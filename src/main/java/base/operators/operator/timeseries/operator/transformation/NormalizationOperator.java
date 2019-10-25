package base.operators.operator.timeseries.operator.transformation;

import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.Normalization;
import java.util.List;

public class NormalizationOperator extends ExampleSetTimeSeriesOperator {
   public NormalizationOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.exampleSetTimeSeriesHelper.addPassThroughRule();
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      return builder.asInputPortOperator("example set").asOutputPortOperator("example set", "_normalized").enableMultivariateInput().addOverwriteOption().changeOutputAttributesToReal().build();
   }

   public void doWork() throws OperatorException {
      this.exampleSetTimeSeriesHelper.resetHelper();
      this.exampleSetTimeSeriesHelper.readInputData(this.exampleSetTimeSeriesHelper.getExampleSetInputPort());
      int progressCallsInGetMethod = this.exampleSetTimeSeriesHelper.progressCallsInGetAddConvertMethods() + 1;
      this.getProgress().setCheckForStop(true);
      this.getProgress().setTotal(3 * progressCallsInGetMethod);
      this.exampleSetTimeSeriesHelper.enableCallProgressStep();
      MultivariateValueSeries inputMultivariateValueSeries = this.exampleSetTimeSeriesHelper.getInputMultivariateValueSeries();
      this.getProgress().step();
      Normalization normalization = Normalization.create();
      MultivariateValueSeries outputMultivariateValueSeries = normalization.compute(inputMultivariateValueSeries);
      this.getProgress().step(progressCallsInGetMethod);
      this.exampleSetTimeSeriesHelper.addMultivariateValueSeriesToExampleSetOutputPort(outputMultivariateValueSeries, this.exampleSetTimeSeriesHelper.getInputExampleSet());
      this.getProgress().complete();
   }

   public List getParameterTypes() {
      return this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
   }
}
