package base.operators.operator.timeseries.operator.forecast.arima;

import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.UserError;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.IncompatibleMDClassException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.*;
import base.operators.operator.ports.metadata.MDNumber.Relation;
import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.Arima;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.ArimaTrainer;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils.ArimaUtils;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import org.apache.commons.math3.util.Pair;

import java.util.Arrays;
import java.util.List;

public class ArimaTrainerOperator extends ExampleSetTimeSeriesOperator {
   private OutputPort arimaOutputPort = (OutputPort)this.getOutputPorts().createPort("forecast model");
   private OutputPort performanceOutputPort = (OutputPort)this.getOutputPorts().createPort("performance");
   OutputPort exampleSetOutputPort = (OutputPort)this.getOutputPorts().createPort("original");
   public static final String PARAMETER_P = "p:_order_of_the_autoregressive_model";
   public static final String PARAMETER_D = "d:_degree_of_differencing";
   public static final String PARAMETER_Q = "q:_order_of_the_moving-average_model";
   public static final String PARAMETER_ESTIMATE_CONSTANT = "estimate_constant";
   public static final String PARAMETER_MAIN_CRITERION = "main_criterion";

   public ArimaTrainerOperator(OperatorDescription description) throws UndefinedParameterError, WrongConfiguredHelperException {
      super(description);
      final InputPort exampleSetInputPort = this.exampleSetTimeSeriesHelper.getExampleSetInputPort();
      this.getTransformer().addGenerationRule(this.arimaOutputPort, ArimaModel.class);
      this.getTransformer().addRule(new GenerateNewMDRule(this.arimaOutputPort, new MetaData(ArimaModel.class)) {
         public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
            try {
               if (exampleSetInputPort.isConnected()) {
                  unmodifiedMetaData.putMetaData("Series name", ArimaTrainerOperator.this.getParameterAsString("time_series_attribute"));
                  if (ArimaTrainerOperator.this.getParameterAsBoolean("has_indices")) {
                     unmodifiedMetaData.putMetaData("Indices name", ArimaTrainerOperator.this.getParameterAsString("indices_attribute"));
                  }

                  ExampleSetMetaData exampleSetMetaData = (ExampleSetMetaData)exampleSetInputPort.getMetaData(ExampleSetMetaData.class);
                  if (exampleSetMetaData != null) {
                     unmodifiedMetaData.putMetaData("Length of series", exampleSetMetaData.getNumberOfExamples());
                     if (ArimaTrainerOperator.this.getParameterAsBoolean("has_indices")) {
                        AttributeMetaData indicesAttributeMetaData = exampleSetMetaData.getAttributeByName(ArimaTrainerOperator.this.getParameterAsString("indices_attribute"));
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
      this.getTransformer().addGenerationRule(this.performanceOutputPort, PerformanceVector.class);
      this.getTransformer().addPassThroughRule(exampleSetInputPort, this.exampleSetOutputPort);
      exampleSetInputPort.addPrecondition(new ExampleSetPrecondition(exampleSetInputPort) {
         public void makeAdditionalChecks(ExampleSetMetaData emd) throws UndefinedParameterError {
            int numberOfParameter = ArimaTrainerOperator.this.getParameterAsInt("p:_order_of_the_autoregressive_model") + ArimaTrainerOperator.this.getParameterAsInt("d:_degree_of_differencing") + ArimaTrainerOperator.this.getParameterAsInt("q:_order_of_the_moving-average_model");
            if (ArimaTrainerOperator.this.getParameterAsBoolean("estimate_constant")) {
               ++numberOfParameter;
            }

            MDInteger numberOfExamples = emd.getNumberOfExamples();
            if (numberOfExamples.getRelation() == Relation.EQUAL || numberOfExamples.getRelation() == Relation.AT_MOST) {
               int lengthOfTimeSeries = (Integer)emd.getNumberOfExamples().getNumber();
               if (numberOfParameter > lengthOfTimeSeries / 4) {
                  this.createError(Severity.WARNING, "time_series_extension.parameters.arima.number_of_parameters_too_large", new Object[]{numberOfParameter, lengthOfTimeSeries, lengthOfTimeSeries / 4});
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
      Series inputSeries = null;
      if (timeSeriesInput) {
         inputSeries = this.exampleSetTimeSeriesHelper.getInputTimeSeries();
      } else {
         inputSeries = this.exampleSetTimeSeriesHelper.getInputValueSeries();
      }

      this.getProgress().step();
      int p = this.getParameterAsInt("p:_order_of_the_autoregressive_model");
      int d = this.getParameterAsInt("d:_degree_of_differencing");
      int q = this.getParameterAsInt("q:_order_of_the_moving-average_model");
      if (p == 0 && q == 0) {
         throw new UserError(this, "time_series_extension.parameter.2_parameter_value_combination_not_allowed", new Object[]{"p:_order_of_the_autoregressive_model", "q:_order_of_the_moving-average_model", 0, 0, "Please change at least one of them to another value."});
      } else {
         boolean estimateConstant = this.getParameterAsBoolean("estimate_constant");
         boolean transformParams = false;
         ArimaUtils.TrainingAlgorithm trainingAlgorithm = ArimaUtils.TrainingAlgorithm.CONDITIONAL_MAX_LOGLIKELIHOOD;
         ArimaUtils.OptimizationMethod optimizationMethod = ArimaUtils.OptimizationMethod.LBFGS;
         int maxNumberOfIterations = 1;
         boolean useRegressionForBOBYQAParameters = false;
         ArimaTrainer arimaTrainer = ArimaTrainer.create(p, d, q, estimateConstant, transformParams, trainingAlgorithm, optimizationMethod, maxNumberOfIterations, useRegressionForBOBYQAParameters);
         Pair validation = arimaTrainer.validateNumberOfParameters(((Series)inputSeries).getLength());
         if (!(Boolean)validation.getFirst()) {
            throw new UserError(this, "time_series_extension.parameter.parameter_combination_not_allowed", new Object[]{"p,d,q,estimateConstant dependent to the length of the series", "p: " + p + ", d: " + d + ", q: " + q + ", estimateConstant: " + estimateConstant + ", length: " + ((Series)inputSeries).getLength() + ". ", validation.getSecond()});
         } else {
            validation = arimaTrainer.validateAllowedParametersForHannanRissanen(((Series)inputSeries).getLength());
            if (!(Boolean)validation.getFirst()) {
               int maxOrderOfInitialARProcess = (int)Math.round(12.0D * Math.pow((double)((float)((Series)inputSeries).getLength() / 100.0F), 0.25D));
               StringBuilder builder = (new StringBuilder("For a time series with length ")).append(((Series)inputSeries).getLength()).append(" the following condition has to be fulfilled for estimating start parameters for the ARIMA fit:").append("\n").append("length > d + Maximum(p,q+").append(maxOrderOfInitialARProcess).append(").");
               throw new UserError(this, "time_series_extension.parameter.parameter_combination_not_allowed", new Object[]{"p,d,q dependent to the length of the series", "p: " + p + ", d: " + d + ", q: " + q + ", length: " + ((Series)inputSeries).getLength(), builder.toString()});
            } else {
               Arima arima = null;
               String indicesAttributeName = "";

               try {
                  if (timeSeriesInput) {
                     arima = (Arima)arimaTrainer.trainForecast((TimeSeries)inputSeries);
                     indicesAttributeName = this.getParameterAsString("indices_attribute");
                  } else {
                     ValueSeries valueSeries = (ValueSeries)inputSeries;
                     arima = (Arima)arimaTrainer.trainForecast(valueSeries);
                     if (!valueSeries.hasDefaultIndices()) {
                        indicesAttributeName = this.getParameterAsString("indices_attribute");
                     }
                  }
               } catch (Exception var22) {
                  throw this.exampleSetTimeSeriesHelper.handleException(var22, this.getParameterAsString("time_series_attribute"));
               }

               this.getProgress().step(progressCallsInGetMethods);
               PerformanceVector performanceVector = new PerformanceVector();
               InformationCriterion.AkaikesInformationCriterion aic = new InformationCriterion.AkaikesInformationCriterion(arimaTrainer.getFinalAicValue());
               InformationCriterion.BayesianInformationCriterion bic = new InformationCriterion.BayesianInformationCriterion(arimaTrainer.getFinalBicValue());
               InformationCriterion.CorrectedAkaikesInformationCriterion corrAic = new InformationCriterion.CorrectedAkaikesInformationCriterion(arimaTrainer.getFinalCorrectedAicValue());
               performanceVector.addCriterion(aic);
               performanceVector.addCriterion(bic);
               performanceVector.addCriterion(corrAic);
               performanceVector.setMainCriterionName(this.getParameterAsString("main_criterion"));
               ArimaModel arimaModel = new ArimaModel(arima, (Series)inputSeries, indicesAttributeName);
               this.arimaOutputPort.deliver(arimaModel);
               this.performanceOutputPort.deliver(performanceVector);
               this.getProgress().complete();
            }
         }
      }
   }

   public List getParameterTypes() {
      List types = this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
      types.add(new ParameterTypeInt("p:_order_of_the_autoregressive_model", "The parameter p specifies the number of lags used by the autoregressive part of the ARIMA model.", 0, Integer.MAX_VALUE, 1, false));
      types.add(new ParameterTypeInt("d:_degree_of_differencing", "The parameter d specifies how often the time series values are differentiated. ", 0, Integer.MAX_VALUE, 0, false));
      types.add(new ParameterTypeInt("q:_order_of_the_moving-average_model", "The parameter q specifies the order of the moving-average part of the model.", 0, Integer.MAX_VALUE, 1, false));
      types.add(new ParameterTypeBoolean("estimate_constant", "This parameter indicates if the constant of the ARIMA process should be estimated or not.", true, false));
      types.add(new ParameterTypeCategory("main_criterion", "The performance measure which is used as the main criterion in the Performance Vector.", (String[])Arrays.stream(InformationCriterion.CRITERION.class.getEnumConstants()).map(Enum::name).toArray((x$0) -> {
         return new String[x$0];
      }), 0, false));
      return types;
   }
}
