package base.operators.operator.timeseries.operator.forecast.holt_winters;

import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.forecast.arima.ArimaModel;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.IncompatibleMDClassException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;
import base.operators.operator.ports.metadata.GenerateNewMDRule;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.MDNumber.Relation;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.holtwinters.HoltWinters;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.holtwinters.HoltWintersTrainer;
import java.util.Arrays;
import java.util.List;

public class HoltWintersTrainerOperator extends ExampleSetTimeSeriesOperator {
   private OutputPort holtWintersPort = (OutputPort)this.getOutputPorts().createPort("forecast model");
   private OutputPort exampleSetOutputPort = (OutputPort)this.getOutputPorts().createPort("original");
   private static final String PARAMETER_ALPHA = "alpha:_coefficient_for_level_smoothing";
   private static final String PARAMETER_BETA = "beta:_coefficient_for_trend_smoothing";
   private static final String PARAMETER_GAMMA = "gamma:_coefficient_for_seasonality_smoothing";
   private static final String PARAMETER_PERIOD = "period:_length_of_one_period";
   private static final String PARAMETER_SEASONAL_MODEL = "seasonality_model";

   public HoltWintersTrainerOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      final InputPort exampleSetInputPort = this.exampleSetTimeSeriesHelper.getExampleSetInputPort();
      this.getTransformer().addGenerationRule(this.holtWintersPort, ArimaModel.class);
      this.getTransformer().addRule(new GenerateNewMDRule(this.holtWintersPort, new MetaData(ArimaModel.class)) {
         public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
            try {
               if (exampleSetInputPort.isConnected()) {
                  unmodifiedMetaData.putMetaData("Series name", HoltWintersTrainerOperator.this.getParameterAsString("time_series_attribute"));
                  if (HoltWintersTrainerOperator.this.getParameterAsBoolean("has_indices")) {
                     unmodifiedMetaData.putMetaData("Indices name", HoltWintersTrainerOperator.this.getParameterAsString("indices_attribute"));
                  }

                  ExampleSetMetaData exampleSetMetaData = (ExampleSetMetaData)exampleSetInputPort.getMetaData(ExampleSetMetaData.class);
                  if (exampleSetMetaData != null) {
                     unmodifiedMetaData.putMetaData("Length of series", exampleSetMetaData.getNumberOfExamples());
                     if (HoltWintersTrainerOperator.this.getParameterAsBoolean("has_indices")) {
                        AttributeMetaData indicesAttributeMetaData = exampleSetMetaData.getAttributeByName(HoltWintersTrainerOperator.this.getParameterAsString("indices_attribute"));
                        if (indicesAttributeMetaData != null) {
                           if (indicesAttributeMetaData.isNumerical()) {
                              unmodifiedMetaData.putMetaData("Indices type", 2);
                           } else if (indicesAttributeMetaData.isDateTime()) {
                              unmodifiedMetaData.putMetaData("Indices type", 9);
                           }
                        }
                     }
                  }
               }
            } catch (IncompatibleMDClassException | UndefinedParameterError var4) {
               var4.printStackTrace();
            }

            return unmodifiedMetaData;
         }
      });
      this.getTransformer().addPassThroughRule(exampleSetInputPort, this.exampleSetOutputPort);
      exampleSetInputPort.addPrecondition(new ExampleSetPrecondition(exampleSetInputPort) {
         public void makeAdditionalChecks(ExampleSetMetaData emd) throws UndefinedParameterError {
            int period = HoltWintersTrainerOperator.this.getParameterAsInt("period:_length_of_one_period");
            MDInteger numberOfExamples = emd.getNumberOfExamples();
            if (numberOfExamples.getRelation() == Relation.EQUAL || numberOfExamples.getRelation() == Relation.AT_MOST) {
               int lengthOfTimeSeries = (Integer)emd.getNumberOfExamples().getNumber();
               if (lengthOfTimeSeries < 2 * period) {
                  this.createError(Severity.ERROR, "time_series_extension.parameters.parameter_too_large", new Object[]{"period: length of one period", period, " larger than half the time series length", lengthOfTimeSeries});
               }
            }

         }
      });
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      return builder.asInputPortOperator("example set").setIndiceHandling(ExampleSetTimeSeriesHelper.IndiceHandling.OPTIONAL_INDICES).build();
   }

   public void doWork() throws OperatorException {
      this.exampleSetTimeSeriesHelper.resetHelper();
      this.exampleSetTimeSeriesHelper.readInputData(this.exampleSetTimeSeriesHelper.getExampleSetInputPort());
      this.exampleSetOutputPort.deliver(this.exampleSetTimeSeriesHelper.getInputExampleSet());
      int progressCallsInGetMethods = this.exampleSetTimeSeriesHelper.progressCallsInGetAddConvertMethods() + 1;
      this.getProgress().setCheckForStop(true);
      this.getProgress().setTotal(2 * progressCallsInGetMethods + 1);
      this.exampleSetTimeSeriesHelper.enableCallProgressStep();
      boolean timeSeriesInput = this.exampleSetTimeSeriesHelper.checkForTimeIndices();
      Object inputSeries;
      if (timeSeriesInput) {
         inputSeries = this.exampleSetTimeSeriesHelper.getInputTimeSeries();
      } else {
         inputSeries = this.exampleSetTimeSeriesHelper.getInputValueSeries();
      }

      this.getProgress().step();
      String indicesAttributeName = this.getParameterAsString("indices_attribute");
      double alpha = this.getParameterAsDouble("alpha:_coefficient_for_level_smoothing");
      double beta = this.getParameterAsDouble("beta:_coefficient_for_trend_smoothing");
      double gamma = this.getParameterAsDouble("gamma:_coefficient_for_seasonality_smoothing");
      int period = this.getParameterAsInt("period:_length_of_one_period");
      if (((Series)inputSeries).getLength() < 2 * period) {
         throw new UserError(this, "time_series_extension.parameter.timeseries_length_larger_than_parameter", new Object[]{"period: length of one period", "smaller than half of", period, ((Series)inputSeries).getLength()});
      } else {
         String seasonalModelName = this.getParameterAsString("seasonality_model");
         HoltWinters.SeasonalModel seasonalModel = HoltWinters.SeasonalModel.valueOf(seasonalModelName.toUpperCase());
         HoltWintersTrainer holtWintersTrainer = HoltWintersTrainer.create(alpha, beta, gamma, period, seasonalModel);

         HoltWinters holtWinters;
         try {
            if (timeSeriesInput) {
               holtWinters = (HoltWinters)holtWintersTrainer.trainForecast((TimeSeries)inputSeries);
               indicesAttributeName = this.getParameterAsString("indices_attribute");
            } else {
               ValueSeries valueSeries = (ValueSeries)inputSeries;
               holtWinters = (HoltWinters)holtWintersTrainer.trainForecast(valueSeries);
               if (!valueSeries.hasDefaultIndices()) {
                  indicesAttributeName = this.getParameterAsString("indices_attribute");
               }
            }
         } catch (Exception var17) {
            throw this.exampleSetTimeSeriesHelper.handleException(var17, this.getParameterAsString("time_series_attribute"));
         }

         HoltWintersModel holtWintersModel = new HoltWintersModel(holtWinters, (Series)inputSeries, indicesAttributeName);
         this.holtWintersPort.deliver(holtWintersModel);
      }
   }

   public List getParameterTypes() {
      List types = this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
      types.add(new ParameterTypeDouble("alpha:_coefficient_for_level_smoothing", "The parameter alpha specifies the smoothing coefficient for the level component of the Holt-Winters model.", 1.0E-9D, 1.0D, 0.5D, false));
      types.add(new ParameterTypeDouble("beta:_coefficient_for_trend_smoothing", "The parameter beta specifies the smoothing coefficient for the trend component of the Holt-Winters model. ", 0.0D, 1.0D, 0.1D, false));
      types.add(new ParameterTypeDouble("gamma:_coefficient_for_seasonality_smoothing", "The parameter gamma specifies the smoothing coefficient for the seasonal component of the Holt-Winters model", 0.0D, 1.0D, 0.5D, false));
      types.add(new ParameterTypeInt("period:_length_of_one_period", "This parameter specifies the length of the period of the input time series", 0, Integer.MAX_VALUE, 4, false));
      types.add(new ParameterTypeCategory("seasonality_model", "The parameter specifies the type of seasonal model used..", (String[])Arrays.stream(HoltWinters.SeasonalModel.class.getEnumConstants()).map(Enum::name).map(String::toLowerCase).toArray((x$0) -> {
         return new String[x$0];
      }), 1, false));
      return types;
   }
}
