package base.operators.operator.timeseries.operator;

import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;


public abstract class ExampleSetTimeSeriesOperator
        extends Operator
{
   protected ExampleSetTimeSeriesHelper<Operator, ISeries<?, ?>> exampleSetTimeSeriesHelper;

   public ExampleSetTimeSeriesOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.exampleSetTimeSeriesHelper = initExampleSetTimeSeriesOperator();
   }

   protected abstract ExampleSetTimeSeriesHelper<Operator, ISeries<?, ?>> initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException;
}
