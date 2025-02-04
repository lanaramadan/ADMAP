package edu.uci.ics.amber.engine.architecture.controller

import akka.actor.Cancellable
import edu.uci.ics.amber.engine.architecture.common.AkkaActorService
import edu.uci.ics.amber.engine.architecture.rpc.controlcommands.{
  AsyncRPCContext,
  QueryStatisticsRequest
}
import edu.uci.ics.amber.engine.architecture.rpc.controllerservice.ControllerServiceGrpc.METHOD_CONTROLLER_INITIATE_QUERY_STATISTICS
import edu.uci.ics.amber.engine.common.rpc.AsyncRPCClient.ControlInvocation
import edu.uci.ics.amber.engine.common.virtualidentity.util.SELF

import scala.concurrent.duration.{DurationInt, FiniteDuration, MILLISECONDS}

class ControllerTimerService(
    controllerConfig: ControllerConfig,
    akkaActorService: AkkaActorService
) {
  var statusUpdateAskHandle: Option[Cancellable] = None

  def enableStatusUpdate(): Unit = {
    if (controllerConfig.statusUpdateIntervalMs.nonEmpty && statusUpdateAskHandle.isEmpty) {
      statusUpdateAskHandle = Option(
        akkaActorService.sendToSelfWithFixedDelay(
          0.milliseconds,
          FiniteDuration.apply(controllerConfig.statusUpdateIntervalMs.get, MILLISECONDS),
          ControlInvocation(
            METHOD_CONTROLLER_INITIATE_QUERY_STATISTICS,
            QueryStatisticsRequest(Seq.empty),
            AsyncRPCContext(SELF, SELF),
            0
          )
        )
      )
    }
  }

  def disableStatusUpdate(): Unit = {
    if (statusUpdateAskHandle.nonEmpty) {
      statusUpdateAskHandle.get.cancel()
      statusUpdateAskHandle = Option.empty
    }
  }
}
