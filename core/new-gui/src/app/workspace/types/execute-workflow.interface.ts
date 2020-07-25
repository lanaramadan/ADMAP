/**
 * This file contains some type declaration for the WorkflowGraph interface of the **backend**.
 * The API of the backend is (currently) not the same as the Graph representation in the frontend.
 * These interfaces confronts to the backend API.
*/

export interface LogicalLink extends Readonly<{
  origin: string,
  destination: string
}> { }

export interface LogicalOperator extends Readonly<{
  operatorID: string,
  operatorType: string,
  // reason for not using `any` in this case is to
  //  prevent types such as `undefined` or `null`
  [uniqueAttributes: string]: string | number | boolean | object
}> { }

/**
 * LogicalPlan is the backend interface equivalent of frontend interface WorkflowGraph,
 *  they represent the same thing - the backend term currently used is LogicalPlan.
 * However, the format and content of the backend interface is different.
 */
export interface LogicalPlan extends Readonly<{
  operators: LogicalOperator[],
  links: LogicalLink[]
}> { }

/**
 * The backend interface of the return object of a successful execution
 */
export interface ResultObject extends Readonly<{
  operatorID: string,
  table: ReadonlyArray<object>,
  chartType: string | undefined
}> {

}
export interface SuccessExecutionResult extends Readonly<{
  code: 0,
  result: ReadonlyArray<ResultObject>,
  resultID: string
}> { }

/**
 * The backend interface of the return object of a failed execution
 */
export interface ErrorExecutionResult extends Readonly< {
  code: 1,
  message: string
}> { }

/**
 * Discriminated Union
 * http://www.typescriptlang.org/docs/handbook/advanced-types.html
 *
 * ExecutionResult type can be either SuccessExecutionResult or ErrorExecutionResult.
 *  but cannot contain both structures at the same time.
 * In this case:
 *  if the code value is 0, then the object type must be SuccessExecutionResult
 *  if the code value is 1, then the object type must be ErrorExecutionResult
 */
export type ExecutionResult = SuccessExecutionResult | ErrorExecutionResult;


/**
 * interface for processStatus recieved from the backend via websocket
 *    operatorStates: a dictionary with operator id as key and operator current state as value
 *    operatorStatistics: a dictionary with operator id as key and operator current statistics as value
 */
export interface ProcessStatus extends Readonly< {
  operatorStates: Readonly< {
    [key: string]: OperatorStates
  }>
  operatorStatistics: Readonly< {
    [key: string]: Statistics
  }>
}> {}

export enum OperatorStates {
  Initializing,
  Ready,
  Running,
  Pausing,
  Paused,
  Completed
}

/**
 * inputCount: the number of tuples received by a operator
 * outputCount: the number of tuples outputed by a operator
 * speed: number of tuples outputed by a operator per millisecond
 */
export interface Statistics {
  inputCount: number;
  outputCount: number;
  speed: number;
}
