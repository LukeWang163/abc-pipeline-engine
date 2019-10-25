package base.operators.operator.timeseries.operator.transformation;

import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeInt;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.Differentiation;
import java.util.Arrays;
import java.util.List;

public class DifferentiationOperator extends ExampleSetTimeSeriesOperator {
   public static final String PARAMETER_LAG = "lag";
   public static final String PARAMETER_DIFFERENTIATION_METHOD = "differentiation_method";

   public DifferentiationOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.exampleSetTimeSeriesHelper.addPassThroughRule();
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      return builder.asInputPortOperator("example set").asOutputPortOperator("example set", "_differentiated").enableMultivariateInput().addOverwriteOption().changeOutputAttributesToReal().build();
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
      int lag = this.getParameterAsInt("lag");
      if (lag >= inputMultivariateValueSeries.getLength()) {
         throw new UserError(this, "time_series_extension.parameter.timeseries_length_larger_than_parameter", new Object[]{"Parameter lag", "larger than or equal to", lag, inputMultivariateValueSeries.getLength()});
      } else {
         Differentiation.DifferentiationMethod differentationMethod = Differentiation.DifferentiationMethod.valueOf(this.getParameterAsString("differentiation_method").toUpperCase());
         Differentiation differentation = Differentiation.createDifferentiation(lag, differentationMethod);
         MultivariateValueSeries outputMultivariateValueSeries = differentation.compute(inputMultivariateValueSeries);
         this.getProgress().step(progressCallsInGetMethod);
         this.exampleSetTimeSeriesHelper.addMultivariateValueSeriesToExampleSetOutputPort(outputMultivariateValueSeries, this.exampleSetTimeSeriesHelper.getInputExampleSet());
         this.getProgress().complete();
      }
   }

   public List getParameterTypes() {
      List types = this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
      types.add(new ParameterTypeInt("lag", "This parameter defines amount of lag that is used when calculating the differentiated values.", 1, Integer.MAX_VALUE, 1, false));
      types.add(new ParameterTypeCategory("differentiation_method", "With this parameter the used differentiation method can be selected.", (String[])Arrays.stream(Differentiation.DifferentiationMethod.class.getEnumConstants()).map(Enum::name).map(String::toLowerCase).toArray((x$0) -> {
         return new String[x$0];
      }), 0, false));
      return types;
   }
}
