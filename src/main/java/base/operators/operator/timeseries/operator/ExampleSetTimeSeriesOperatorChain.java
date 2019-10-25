package base.operators.operator.timeseries.operator;

import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorChain;
import base.operators.operator.OperatorDescription;
import base.operators.operator.concurrency.internal.ParallelOperatorChain;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;


public abstract class ExampleSetTimeSeriesOperatorChain
        extends ParallelOperatorChain
{
   protected ExampleSetTimeSeriesHelper<OperatorChain, ISeries<?, ?>> exampleSetTimeSeriesHelper = initExampleSetTimeSeriesOperator();


   protected ExampleSetTimeSeriesOperatorChain(OperatorDescription description, String... subprocessNames) throws WrongConfiguredHelperException { super(description, subprocessNames); }

   protected abstract ExampleSetTimeSeriesHelper<OperatorChain, ISeries<?, ?>> initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException;
}
