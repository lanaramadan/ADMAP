package edu.uci.ics.texera.web.service

import com.google.protobuf.timestamp.Timestamp
import com.typesafe.scalalogging.LazyLogging
import edu.uci.ics.amber.engine.architecture.controller.{
  ExecutionStatsUpdate,
  FatalError,
  WorkerAssignmentUpdate,
  WorkflowRecoveryStatus
}
import edu.uci.ics.amber.engine.architecture.rpc.controlreturns.WorkflowAggregatedState
import edu.uci.ics.amber.engine.architecture.rpc.controlreturns.WorkflowAggregatedState.FAILED
import edu.uci.ics.amber.engine.architecture.worker.statistics.PortTupleCountMapping
import edu.uci.ics.amber.engine.common.Utils.maptoStatusCode
import edu.uci.ics.amber.engine.common.client.AmberClient
import edu.uci.ics.amber.engine.common.executionruntimestate.{
  OperatorMetrics,
  OperatorStatistics,
  OperatorWorkerMapping
}
import edu.uci.ics.amber.engine.common.{AmberConfig, Utils}
import edu.uci.ics.amber.error.ErrorUtils.{getOperatorFromActorIdOpt, getStackTraceWithAllCauses}
import edu.uci.ics.amber.core.workflowruntimestate.FatalErrorType.EXECUTION_FAILURE
import edu.uci.ics.amber.core.workflowruntimestate.WorkflowFatalError
import edu.uci.ics.texera.web.SubscriptionManager
import edu.uci.ics.texera.dao.jooq.generated.tables.pojos.OperatorRuntimeStatistics
import edu.uci.ics.texera.web.model.websocket.event.{
  ExecutionDurationUpdateEvent,
  OperatorAggregatedMetrics,
  OperatorStatisticsUpdateEvent,
  WorkerAssignmentUpdateEvent
}
import edu.uci.ics.texera.web.resource.dashboard.user.workflow.WorkflowExecutionsResource
import edu.uci.ics.texera.web.storage.ExecutionStateStore
import edu.uci.ics.texera.web.storage.ExecutionStateStore.updateWorkflowState
import org.jooq.types.{UInteger, ULong}

import java.time.Instant
import java.util
import java.util.concurrent.Executors

class ExecutionStatsService(
    client: AmberClient,
    stateStore: ExecutionStateStore,
    operatorIdToExecutionId: Map[String, ULong]
) extends SubscriptionManager
    with LazyLogging {
  private val metricsPersistThread = Executors.newSingleThreadExecutor()
  private var lastPersistedMetrics: Map[String, OperatorMetrics] = Map()
  registerCallbacks()

  addSubscription(
    stateStore.statsStore.registerDiffHandler((oldState, newState) => {
      // Update operator stats if any operator updates its stat
      if (newState.operatorInfo.toSet != oldState.operatorInfo.toSet) {
        Iterable(
          OperatorStatisticsUpdateEvent(newState.operatorInfo.collect {
            case x =>
              val metrics = x._2
              val res = OperatorAggregatedMetrics(
                Utils.aggregatedStateToString(metrics.operatorState),
                metrics.operatorStatistics.inputCount.map(_.tupleCount).sum,
                metrics.operatorStatistics.outputCount.map(_.tupleCount).sum,
                metrics.operatorStatistics.numWorkers,
                metrics.operatorStatistics.dataProcessingTime,
                metrics.operatorStatistics.controlProcessingTime,
                metrics.operatorStatistics.idleTime
              )
              (x._1, res)
          })
        )
      } else {
        Iterable.empty
      }
    })
  )

  addSubscription(
    stateStore.statsStore.registerDiffHandler((oldState, newState) => {
      // update operators' workers.
      if (newState.operatorWorkerMapping != oldState.operatorWorkerMapping) {
        newState.operatorWorkerMapping
          .map { opToWorkers =>
            WorkerAssignmentUpdateEvent(opToWorkers.operatorId, opToWorkers.workerIds)
          }
      } else {
        Iterable()
      }
    })
  )

  addSubscription(
    stateStore.statsStore.registerDiffHandler((oldState, newState) => {
      // update execution duration.
      if (
        newState.startTimeStamp != oldState.startTimeStamp || newState.endTimeStamp != oldState.endTimeStamp
      ) {
        if (newState.endTimeStamp != 0) {
          Iterable(
            ExecutionDurationUpdateEvent(
              newState.endTimeStamp - newState.startTimeStamp,
              isRunning = false
            )
          )
        } else {
          val currentTime = System.currentTimeMillis()
          Iterable(
            ExecutionDurationUpdateEvent(currentTime - newState.startTimeStamp, isRunning = true)
          )
        }
      } else {
        Iterable()
      }
    })
  )

  private[this] def registerCallbacks(): Unit = {
    registerCallbackOnWorkflowStatsUpdate()
    registerCallbackOnWorkerAssignedUpdate()
    registerCallbackOnWorkflowRecoveryUpdate()
    registerCallbackOnFatalError()
  }

  private[this] def registerCallbackOnWorkflowStatsUpdate(): Unit = {
    addSubscription(
      client
        .registerCallback[ExecutionStatsUpdate]((evt: ExecutionStatsUpdate) => {
          stateStore.statsStore.updateState { statsStore =>
            statsStore.withOperatorInfo(evt.operatorMetrics)
          }
          if (AmberConfig.isUserSystemEnabled) {
            metricsPersistThread.execute(() => {
              storeRuntimeStatistics(computeStatsDiff(evt.operatorMetrics))
              lastPersistedMetrics = evt.operatorMetrics
            })
          }
        })
    )
  }

  private def computeStatsDiff(
      newMetrics: Map[String, OperatorMetrics]
  ): Map[String, OperatorMetrics] = {
    val defaultMetrics =
      OperatorMetrics(
        WorkflowAggregatedState.UNINITIALIZED,
        OperatorStatistics(Seq(), Seq(), 0, 0, 0, 0)
      )

    var metricsMap = newMetrics

    // Find keys present in newState.operatorInfo but not in oldState.operatorInfo
    val newKeys = newMetrics.keys.toSet diff lastPersistedMetrics.keys.toSet
    for (key <- newKeys) {
      lastPersistedMetrics = lastPersistedMetrics + (key -> defaultMetrics)
    }

    // Find keys present in oldState.operatorInfo but not in newState.operatorInfo
    val oldKeys = lastPersistedMetrics.keys.toSet diff newMetrics.keys.toSet
    for (key <- oldKeys) {
      metricsMap = metricsMap + (key -> lastPersistedMetrics(key))
    }

    metricsMap.keys.map { key =>
      val newMetrics = metricsMap(key)
      val oldMetrics = lastPersistedMetrics(key)
      val res = OperatorMetrics(
        newMetrics.operatorState,
        OperatorStatistics(
          newMetrics.operatorStatistics.inputCount.map {
            case PortTupleCountMapping(k, v) =>
              PortTupleCountMapping(
                k,
                v - oldMetrics.operatorStatistics.inputCount
                  .find(_.portId == k)
                  .map(_.tupleCount)
                  .getOrElse(0L)
              )
          },
          newMetrics.operatorStatistics.outputCount.map {
            case PortTupleCountMapping(k, v) =>
              PortTupleCountMapping(
                k,
                v - oldMetrics.operatorStatistics.outputCount
                  .find(_.portId == k)
                  .map(_.tupleCount)
                  .getOrElse(0L)
              )
          },
          newMetrics.operatorStatistics.numWorkers,
          newMetrics.operatorStatistics.dataProcessingTime - oldMetrics.operatorStatistics.dataProcessingTime,
          newMetrics.operatorStatistics.controlProcessingTime - oldMetrics.operatorStatistics.controlProcessingTime,
          newMetrics.operatorStatistics.idleTime - oldMetrics.operatorStatistics.idleTime
        )
      )
      (key, res)
    }.toMap
  }

  private def storeRuntimeStatistics(
      operatorStatistics: scala.collection.immutable.Map[String, OperatorMetrics]
  ): Unit = {
    try {
      val runtimeStatsList: util.ArrayList[OperatorRuntimeStatistics] =
        new util.ArrayList[OperatorRuntimeStatistics]()

      for ((operatorId, stat) <- operatorStatistics) {
        // Create and populate the operator runtime statistics entry
        val runtimeStats = new OperatorRuntimeStatistics()
        runtimeStats.setOperatorExecutionId(operatorIdToExecutionId(operatorId))
        runtimeStats.setInputTupleCnt(
          ULong.valueOf(stat.operatorStatistics.inputCount.map(_.tupleCount).sum)
        )
        runtimeStats.setOutputTupleCnt(
          ULong.valueOf(stat.operatorStatistics.outputCount.map(_.tupleCount).sum)
        )
        runtimeStats.setStatus(maptoStatusCode(stat.operatorState))
        runtimeStats.setDataProcessingTime(
          ULong.valueOf(stat.operatorStatistics.dataProcessingTime)
        )
        runtimeStats.setControlProcessingTime(
          ULong.valueOf(stat.operatorStatistics.controlProcessingTime)
        )
        runtimeStats.setIdleTime(ULong.valueOf(stat.operatorStatistics.idleTime))
        runtimeStats.setNumWorkers(UInteger.valueOf(stat.operatorStatistics.numWorkers))
        runtimeStatsList.add(runtimeStats)
      }

      // Insert into operator_runtime_statistics table
      WorkflowExecutionsResource.insertOperatorRuntimeStatistics(runtimeStatsList)
    } catch {
      case err: Throwable => logger.error("error occurred when storing runtime statistics", err)
    }
  }

  private[this] def registerCallbackOnWorkerAssignedUpdate(): Unit = {
    addSubscription(
      client
        .registerCallback[WorkerAssignmentUpdate]((evt: WorkerAssignmentUpdate) => {
          stateStore.statsStore.updateState { statsStore =>
            statsStore.withOperatorWorkerMapping(
              evt.workerMapping
                .map({
                  case (opId, workerIds) => OperatorWorkerMapping(opId, workerIds.toSeq)
                })
                .toSeq
            )
          }
        })
    )
  }

  private[this] def registerCallbackOnWorkflowRecoveryUpdate(): Unit = {
    addSubscription(
      client
        .registerCallback[WorkflowRecoveryStatus]((evt: WorkflowRecoveryStatus) => {
          stateStore.metadataStore.updateState { metadataStore =>
            metadataStore.withIsRecovering(evt.isRecovering)
          }
        })
    )
  }

  private[this] def registerCallbackOnFatalError(): Unit = {
    addSubscription(
      client
        .registerCallback[FatalError]((evt: FatalError) => {
          client.shutdown()
          val (operatorId, workerId) = getOperatorFromActorIdOpt(evt.fromActor)
          stateStore.statsStore.updateState(stats =>
            stats.withEndTimeStamp(System.currentTimeMillis())
          )
          stateStore.metadataStore.updateState { metadataStore =>
            logger.error("error occurred in execution", evt.e)
            updateWorkflowState(FAILED, metadataStore).addFatalErrors(
              WorkflowFatalError(
                EXECUTION_FAILURE,
                Timestamp(Instant.now),
                evt.e.toString,
                getStackTraceWithAllCauses(evt.e),
                operatorId,
                workerId
              )
            )
          }
        })
    )
  }
}
