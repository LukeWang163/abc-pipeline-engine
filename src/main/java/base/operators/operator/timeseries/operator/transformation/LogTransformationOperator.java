package base.operators.operator.timeseries.operator.transformation;

import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.LogTransformation;

import java.util.Arrays;
import java.util.List;

public class LogTransformationOperator extends ExampleSetTimeSeriesOperator {
   public static final String PARAMETER_LOGARITHM_TYPE = "logarithm_type";

   public LogTransformationOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.exampleSetTimeSeriesHelper.addPassThroughRule();
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      return builder.asInputPortOperator("example set").asOutputPortOperator("example set", "_logarithm").enableMultivariateInput().addOverwriteOption().changeOutputAttributesToReal().build();
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
      LogTransformation logTransformation = null;
      switch(LogTransformation.LogarithmType.valueOf(this.getParameterAsString("logarithm_type").toUpperCase())) {
      case LN:
         logTransformation = LogTransformation.createLnTransformation();
         break;
      case LOG10:
         logTransformation = LogTransformation.createLog10Transformation();
      }

      MultivariateValueSeries outputMultivariateValueSeries = logTransformation.compute(inputMultivariateValueSeries);
      this.getProgress().step(progressCallsInGetMethod);
      this.exampleSetTimeSeriesHelper.addMultivariateValueSeriesToExampleSetOutputPort(outputMultivariateValueSeries, this.exampleSetTimeSeriesHelper.getInputExampleSet());
      this.getProgress().complete();
   }

   public List getParameterTypes() {
      List types = this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
      types.add(new ParameterTypeCategory("logarithm_type", "Base of the applied logarithm.", (String[])Arrays.stream(LogTransformation.LogarithmType.class.getEnumConstants()).map(Enum::name).map(String::toLowerCase).toArray((x$0) -> {
         return new String[x$0];
      }), 0, false));
      return types;
   }
}
