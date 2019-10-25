package base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils;

public final class ArimaUtils {
   public static double[] getArCoefficientsFromParametersArray(double[] parameters, int p, int q) {
      if (p + q > parameters.length) {
         throw new IllegalArgumentException("The sum of p and q is larger than the parameters array");
      } else {
         double[] arCoefficients = new double[p];

         for(int i = 0; i < p; ++i) {
            arCoefficients[i] = parameters[i];
         }

         return arCoefficients;
      }
   }

   public static double[] getMaCoefficientsFromParametersArray(double[] parameters, int p, int q) {
      if (p + q > parameters.length) {
         throw new IllegalArgumentException("The sum of p and q is larger than the parameters array");
      } else {
         double[] maCoefficients = new double[q];

         for(int i = 0; i < q; ++i) {
            maCoefficients[i] = parameters[i + p];
         }

         return maCoefficients;
      }
   }

   public static double getConstantFromParametersArray(double[] parameters, int p, int q) {
      if (p + q + 1 > parameters.length) {
         if (p + q == parameters.length) {
            throw new IllegalArgumentException("The parameters array does not contain a constant");
         } else {
            throw new IllegalArgumentException("p + q + 1 is larger than the parameters array");
         }
      } else {
         return parameters[p + q];
      }
   }

   public static double getSigmaSquareFromParametersArray(double[] parameters, int p, int q, boolean containsConstant) {
      if (containsConstant) {
         if (p + q + 2 > parameters.length) {
            if (p + q + 1 == parameters.length) {
               throw new IllegalArgumentException("The parameters array does not contain a constant");
            } else {
               throw new IllegalArgumentException("p + q + 2 is larger than the parameters array");
            }
         } else {
            return parameters[p + q + 1];
         }
      } else if (p + q + 1 > parameters.length) {
         if (p + q == parameters.length) {
            throw new IllegalArgumentException("The parameters array does not contain sigmaSquare");
         } else {
            throw new IllegalArgumentException("p + q + 1 is larger than the parameters array");
         }
      } else {
         return parameters[p + q];
      }
   }

   private static double[] getParametersArray(double[] arCoefficients, double[] maCoefficients, Double constant, Double sigmaSquare) {
      int length = arCoefficients.length + maCoefficients.length;
      if (constant != null) {
         ++length;
      }

      if (sigmaSquare != null) {
         ++length;
      }

      double[] parameters = new double[length];
      int i = 0;
      double[] var7 = arCoefficients;
      int var8 = arCoefficients.length;

      int var9;
      double coeff;
      for(var9 = 0; var9 < var8; ++var9) {
         coeff = var7[var9];
         parameters[i] = coeff;
         ++i;
      }

      var7 = maCoefficients;
      var8 = maCoefficients.length;

      for(var9 = 0; var9 < var8; ++var9) {
         coeff = var7[var9];
         parameters[i] = coeff;
         ++i;
      }

      if (constant != null) {
         parameters[i] = constant;
         ++i;
      }

      if (sigmaSquare != null) {
         parameters[i] = sigmaSquare;
         ++i;
      }

      return parameters;
   }

   public static double[] getParametersArray(double[] arCoefficients, double[] maCoefficients, double constant, double sigmaSquare) {
      return getParametersArray(arCoefficients, maCoefficients, new Double(constant), new Double(sigmaSquare));
   }

   public static double[] getParametersArray(double[] arCoefficients, double[] maCoefficients, double singleValue, boolean isConstant) {
      return isConstant ? getParametersArray(arCoefficients, maCoefficients, new Double(singleValue), (Double)null) : getParametersArray(arCoefficients, maCoefficients, (Double)null, new Double(singleValue));
   }

   public static double[] getParametersArray(double[] arCoefficients, double[] maCoefficients) {
      return getParametersArray(arCoefficients, maCoefficients, (Double)null, (Double)null);
   }

   public static double[] transformParams(double[] parameters, int p, int q, boolean estimateConstant) {
      double[] arCoefficients = new double[0];
      double[] maCoefficients = new double[0];
      double constant = 0.0D;
      if (p > 0) {
         arCoefficients = transformCoefficients(getArCoefficientsFromParametersArray(parameters, p, q), false);
      }

      if (q > 0) {
         maCoefficients = transformCoefficients(getMaCoefficientsFromParametersArray(parameters, p, q), true);
      }

      if (estimateConstant) {
         constant = getConstantFromParametersArray(parameters, p, q);
      }

      return getParametersArray(arCoefficients, maCoefficients, constant, true);
   }

   private static double[] transformCoefficients(double[] coeffs, boolean isMa) {
      double f = 1.0D;
      if (!isMa) {
         f = -1.0D;
      }

      double[] newParams = new double[coeffs.length];
      double[] tmp = new double[coeffs.length];

      int i;
      double a;
      for(i = 0; i < coeffs.length; ++i) {
         a = Math.exp(-coeffs[i]);
         a = (1.0D - a) / (1.0D + a);
         newParams[i] = a;
         tmp[i] = a;
      }

      for(i = 1; i < coeffs.length; ++i) {
         a = newParams[i];

         int j;
         for(j = 0; j < i; ++j) {
            tmp[j] += f * a * newParams[i - j - 1];
         }

         for(j = 0; j < i; ++j) {
            newParams[j] = tmp[j];
         }
      }

      return newParams;
   }

   public static double[] inverseTransformParams(double[] invParameters, int p, int q, boolean estimateConstant) {
      double[] arCoefficients = new double[0];
      double[] maCoefficients = new double[0];
      double constant = 0.0D;
      if (p > 0) {
         arCoefficients = inverseTransformCoefficients(getArCoefficientsFromParametersArray(invParameters, p, q), false);
      }

      if (q > 0) {
         maCoefficients = inverseTransformCoefficients(getMaCoefficientsFromParametersArray(invParameters, p, q), true);
      }

      if (estimateConstant) {
         constant = getConstantFromParametersArray(invParameters, p, q);
      }

      return getParametersArray(arCoefficients, maCoefficients, constant, true);
   }

   private static double[] inverseTransformCoefficients(double[] invCoeffs, boolean isMa) {
      double f = 1.0D;
      if (isMa) {
         f = -1.0D;
      }

      double[] newCoeffs = new double[invCoeffs.length];
      double[] oldCoeffs = (double[])invCoeffs.clone();
      double[] tmp = (double[])invCoeffs.clone();

      int i;
      for(i = invCoeffs.length - 1; i > 0; --i) {
         double a = oldCoeffs[i];

         int j;
         for(j = 0; j < i; ++j) {
            tmp[j] = (oldCoeffs[j] + f * a * oldCoeffs[i - j - 1]) / (1.0D - Math.pow(a, 2.0D));
         }

         for(j = 0; j < i; ++j) {
            oldCoeffs[j] = tmp[j];
         }
      }

      for(i = 0; i < oldCoeffs.length; ++i) {
         newCoeffs[i] = -Math.log((1.0D - oldCoeffs[i]) / (1.0D + oldCoeffs[i]));
      }

      return newCoeffs;
   }

   public static enum OptimizationMethod {
      BOBYQA,
      CMAES,
      NELDERMEAD,
      POWELL,
      LBFGS;
   }

   public static enum TrainingAlgorithm {
      EXACT_MAX_LOGLIKELIHOOD,
      CONDITIONAL_MAX_LOGLIKELIHOOD,
      HANNAN_RISSANEN,
      CONDITIONAL_THEN_EXACT_MAX_LOGLIKELIHOOD;
   }

   public static enum ArimaLogLikelihoodType {
      EXACT,
      CONDITIONAL;
   }
}
