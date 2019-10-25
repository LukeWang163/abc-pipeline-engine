package base.operators.operator.timeseries.timeseriesanalysis.forecast.arima;

import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils.ArimaUtils;
import java.util.Arrays;

public class ArimaWronglyConfiguredException extends RuntimeException {
   private static final long serialVersionUID = -3327018292981521024L;

   public ArimaWronglyConfiguredException(WrongConfigurationType type) {
      super(createMessage(type));
   }

   public ArimaWronglyConfiguredException(WrongConfigurationType type, Throwable cause) {
      super(createMessage(type), cause);
   }

   private static String createMessage(WrongConfigurationType type) {
      switch(type) {
      case HANNAN_RISSANEN_ILLEGAL_D:
         return "Hannan-Rissanen algorithm for an ARIMA process with d != 0 is not supported.";
      case HANNAN_RISSANEN_INITIAL_PARAM:
         return "HANNAN_RISSANEN as the training algorithm does not need initial parameters.";
      case HANNAN_RISSANEN_OPTIMIZATION_PARAM:
         return "HANNAN_RISSANEN as the training algorithm does not need optimization parameters.";
      case NO_OPTIMIZATION_PARAMETERS_PROVIDED:
         return "Trainer is configured to not use default parameter for optimization, but no optimization parameters are provided.";
      case RESIDUALS_NOT_LONG_ENOUGH:
         return "The residuals array is not long enough. It has to have at least q+1 entries.";
      case NOT_SUPPORTED_OPTIMIZATION_METHOD:
         return "Provided optimization method is not one of: " + Arrays.toString(ArimaUtils.OptimizationMethod.values());
      case NOT_SUPPORTED_TRAINING_ALGORITHM:
         return "Provided training algorithm is not one of: " + Arrays.toString(ArimaUtils.TrainingAlgorithm.values());
      case NOT_SUPPROTED_ARIMA_LOGLIKELIHOODTYPE:
         return "Provided arima loglikelihood type is not one of: " + Arrays.toString(ArimaUtils.ArimaLogLikelihoodType.values());
      case CONSTANT_NOT_ENABLED:
         return "Constant shall be returned, but estimate constant is false.";
      default:
         return "";
      }
   }

   public static enum WrongConfigurationType {
      HANNAN_RISSANEN_ILLEGAL_D,
      HANNAN_RISSANEN_OPTIMIZATION_PARAM,
      HANNAN_RISSANEN_INITIAL_PARAM,
      RESIDUALS_NOT_LONG_ENOUGH,
      NOT_SUPPORTED_TRAINING_ALGORITHM,
      NO_OPTIMIZATION_PARAMETERS_PROVIDED,
      NOT_SUPPORTED_OPTIMIZATION_METHOD,
      NOT_SUPPROTED_ARIMA_LOGLIKELIHOODTYPE,
      CONSTANT_NOT_ENABLED;
   }
}
