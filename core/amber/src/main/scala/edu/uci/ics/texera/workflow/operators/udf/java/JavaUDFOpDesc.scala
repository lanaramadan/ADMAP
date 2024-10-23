package edu.uci.ics.texera.workflow.operators.udf.java

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyDescription}
import com.google.common.base.Preconditions
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle
import edu.uci.ics.amber.engine.architecture.deploysemantics.layer.OpExecInitInfo
import edu.uci.ics.amber.engine.common.model.{PhysicalOp, SchemaPropagationFunc}
import edu.uci.ics.amber.engine.common.model.tuple.{Attribute, Schema}
import edu.uci.ics.amber.engine.common.virtualidentity.{ExecutionIdentity, WorkflowIdentity}
import edu.uci.ics.amber.engine.common.workflow.{InputPort, OutputPort, PortIdentity}
import edu.uci.ics.texera.workflow.common.metadata.{OperatorGroupConstants, OperatorInfo}
import edu.uci.ics.texera.workflow.common.operators.{LogicalOp, PortDescription, StateTransferFunc}
import edu.uci.ics.texera.workflow.common.workflow.{PartitionInfo, UnknownPartition}

import scala.util.{Success, Try}

class JavaUDFOpDesc extends LogicalOp {
  @JsonProperty(
    required = true,
    defaultValue =
      "import edu.uci.ics.texera.workflow.common.operators.map.MapOpExec;\n" +
        "import edu.uci.ics.amber.engine.common.model.tuple.Tuple;\n" +
        "import edu.uci.ics.amber.engine.common.model.tuple.TupleLike;\n" +
        "import scala.Function1;\n" +
        "import java.io.Serializable;\n" +
        "\n" +
        "public class JavaUDFOpExec extends MapOpExec {\n" +
        "    public JavaUDFOpExec () {\n" +
        "        this.setMapFunc((Function1<Tuple, TupleLike> & Serializable) this::processTuple);\n" +
        "    }\n" +
        "    \n" +
        "    public TupleLike processTuple(Tuple tuple) {\n" +
        "        return tuple;\n" +
        "    }\n" +
        "}"
  )
  @JsonSchemaTitle("Java UDF script")
  @JsonPropertyDescription("Input your code here")
  var code: String = ""

  @JsonProperty(required = true, defaultValue = "1")
  @JsonSchemaTitle("Worker count")
  @JsonPropertyDescription("Specify how many parallel workers to lunch")
  var workers: Int = Int.box(1)

  @JsonProperty(required = true, defaultValue = "true")
  @JsonSchemaTitle("Retain input columns")
  @JsonPropertyDescription("Keep the original input columns?")
  var retainInputColumns: Boolean = Boolean.box(false)

  @JsonProperty
  @JsonSchemaTitle("Extra output column(s)")
  @JsonPropertyDescription(
    "Name of the newly added output columns that the UDF will produce, if any"
  )
  var outputColumns: List[Attribute] = List()

  override def getPhysicalOp(
      workflowId: WorkflowIdentity,
      executionId: ExecutionIdentity
  ): PhysicalOp = {
    Preconditions.checkArgument(workers >= 1, "Need at least 1 worker.", Array())
    val opInfo = this.operatorInfo
    val partitionRequirement: List[Option[PartitionInfo]] = if (inputPorts != null) {
      inputPorts.map(p => Option(p.partitionRequirement))
    } else {
      opInfo.inputPorts.map(_ => None)
    }

    val propagateSchema = (inputSchemas: Map[PortIdentity, Schema]) => {
      val inputSchema = inputSchemas(operatorInfo.inputPorts.head.id)
      val outputSchemaBuilder = Schema.builder()
      // keep the same schema from input
      if (retainInputColumns) outputSchemaBuilder.add(inputSchema)
      // for any javaUDFType, it can add custom output columns (attributes).
      if (outputColumns != null) {
        if (retainInputColumns) { // check if columns are duplicated

          for (column <- outputColumns) {
            if (inputSchema.containsAttribute(column.getName))
              throw new RuntimeException("Column name " + column.getName + " already exists!")
          }
        }
        outputSchemaBuilder.add(outputColumns).build()
      }
      Map(operatorInfo.outputPorts.head.id -> outputSchemaBuilder.build())
    }

    if (workers > 1)
      PhysicalOp
        .oneToOnePhysicalOp(
          workflowId,
          executionId,
          operatorIdentifier,
          OpExecInitInfo(code, "java")
        )
        .withDerivePartition(_ => UnknownPartition())
        .withInputPorts(operatorInfo.inputPorts)
        .withOutputPorts(operatorInfo.outputPorts)
        .withPartitionRequirement(partitionRequirement)
        .withIsOneToManyOp(true)
        .withParallelizable(true)
        .withSuggestedWorkerNum(workers)
        .withPropagateSchema(SchemaPropagationFunc(propagateSchema))
    else
      PhysicalOp
        .manyToOnePhysicalOp(
          workflowId,
          executionId,
          operatorIdentifier,
          OpExecInitInfo(code, "java")
        )
        .withDerivePartition(_ => UnknownPartition())
        .withInputPorts(operatorInfo.inputPorts)
        .withOutputPorts(operatorInfo.outputPorts)
        .withPartitionRequirement(partitionRequirement)
        .withIsOneToManyOp(true)
        .withParallelizable(false)
        .withPropagateSchema(SchemaPropagationFunc(propagateSchema))
  }

  override def operatorInfo: OperatorInfo = {
    val inputPortInfo = if (inputPorts != null) {
      inputPorts.zipWithIndex.map {
        case (portDesc: PortDescription, idx) =>
          InputPort(
            PortIdentity(idx),
            displayName = portDesc.displayName,
            allowMultiLinks = portDesc.allowMultiInputs,
            dependencies = portDesc.dependencies.map(idx => PortIdentity(idx))
          )
      }
    } else {
      List(InputPort(PortIdentity(), allowMultiLinks = true))
    }
    val outputPortInfo = if (outputPorts != null) {
      outputPorts.zipWithIndex.map {
        case (portDesc, idx) => OutputPort(PortIdentity(idx), displayName = portDesc.displayName)
      }
    } else {
      List(OutputPort())
    }

    OperatorInfo(
      "Java UDF",
      "User-defined function operator in Java script",
      OperatorGroupConstants.JAVA_GROUP,
      inputPortInfo,
      outputPortInfo,
      dynamicInputPorts = true,
      dynamicOutputPorts = true,
      supportReconfiguration = true,
      allowPortCustomization = true
    )
  }

  override def getOutputSchema(schemas: Array[Schema]): Schema = {
    //    Preconditions.checkArgument(schemas.length == 1)
    val inputSchema = schemas(0)
    val outputSchemaBuilder = Schema.Builder()
    // keep the same schema from input
    if (retainInputColumns) outputSchemaBuilder.add(inputSchema)
    // for any javaUDFType, it can add custom output columns (attributes).
    if (outputColumns != null) {
      if (retainInputColumns) { // check if columns are duplicated

        for (column <- outputColumns) {
          if (inputSchema.containsAttribute(column.getName))
            throw new RuntimeException("Column name " + column.getName + " already exists!")
        }
      }
      outputSchemaBuilder.add(outputColumns)
    }
    outputSchemaBuilder.build()
  }

  override def runtimeReconfiguration(
      workflowId: WorkflowIdentity,
      executionId: ExecutionIdentity,
      oldLogicalOp: LogicalOp,
      newLogicalOp: LogicalOp
  ): Try[(PhysicalOp, Option[StateTransferFunc])] = {
    Success(newLogicalOp.getPhysicalOp(workflowId, executionId), None)
  }
}
