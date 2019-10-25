package base.operators.operator.timeseries.operator.filter;

import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.MovingAverageFilter;
import java.util.List;

public class MovingAverageFilterOperator extends ExampleSetTimeSeriesOperator {
   public static final String PARAMETER_FILTER_TYPE = "filter_type";
   public static final String PARAMETER_SIZE_OF_FILTER = "filter_size";

   public MovingAverageFilterOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.exampleSetTimeSeriesHelper.addPassThroughRule();
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      return builder.asInputPortOperator("example set").asOutputPortOperator("example set", "_filtered").enableMultivariateInput().addOverwriteOption().changeOutputAttributesToReal().build();
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
      MovingAverageFilter movingAverageFilter = null;
      int sizeOfFilter;
      switch(FilterTypes.valueOf(this.getParameterAsString("filter_type").toUpperCase())) {
      case SIMPLE:
         sizeOfFilter = this.getParameterAsInt("filter_size");
         if (2 * sizeOfFilter + 1 > inputMultivariateValueSeries.getLength()) {
            throw new UserError(this, "time_series_extension.parameter.timeseries_length_larger_than_parameter", new Object[]{"Size of the filter window", "larger than", "2*(filter_size) + 1 = " + (2 * sizeOfFilter + 1), inputMultivariateValueSeries.getLength()});
         }

         movingAverageFilter = MovingAverageFilter.createSimpleMovingAverage(sizeOfFilter);
         break;
      case BINOM:
         sizeOfFilter = this.getParameterAsInt("filter_size");
         if (2 * sizeOfFilter + 1 > inputMultivariateValueSeries.getLength()) {
            throw new UserError(this, "time_series_extension.parameter.timeseries_length_larger_than_parameter", new Object[]{"Size of the filter window", "larger than", "2*(filter_size) + 1 = " + (2 * sizeOfFilter + 1), inputMultivariateValueSeries.getLength()});
         }

         movingAverageFilter = MovingAverageFilter.createBinomMovingAverage(sizeOfFilter);
         break;
      case SPENCERS_15_POINTS:
         movingAverageFilter = MovingAverageFilter.createSpencers15PointMovingAverage();
         if (movingAverageFilter.getWeights().length > inputMultivariateValueSeries.getLength()) {
            throw new UserError(this, "time_series_extension.parameter.timeseries_length_larger_than_parameter", new Object[]{"Size of the filter window", "larger than", "Spencers 15 Point Moving Average = " + movingAverageFilter.getWeights().length, inputMultivariateValueSeries.getLength()});
         }
      }

      MultivariateValueSeries outputMultivariateValueSeries = movingAverageFilter.compute(inputMultivariateValueSeries);
      this.getProgress().step(progressCallsInGetMethod);
      this.exampleSetTimeSeriesHelper.addMultivariateValueSeriesToExampleSetOutputPort(outputMultivariateValueSeries, this.exampleSetTimeSeriesHelper.getInputExampleSet());
      this.getProgress().complete();
   }

   public List getParameterTypes() {
      List types = this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
      FilterTypes[] filterTypes = FilterTypes.values();
      String[] typeNames = new String[filterTypes.length];
      int i = 0;
      FilterTypes[] var5 = filterTypes;
      int var6 = filterTypes.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         FilterTypes type = var5[var7];
         typeNames[i] = type.name().toLowerCase();
         ++i;
      }

      types.add(new ParameterTypeCategory("filter_type", "The filter type defines the weights of the filter.", typeNames, 0, false));
      ParameterType type = new ParameterTypeInt("filter_size", "This parameter defines the size of the filter window (number of values left and right to the actual value) for the binom and simple filter type.", 1, Integer.MAX_VALUE, 1, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "filter_type", false, new String[]{"simple", "binom"}));
      types.add(type);
      return types;
   }

   private static enum FilterTypes {
      SIMPLE,
      BINOM,
      SPENCERS_15_POINTS;
   }
}
