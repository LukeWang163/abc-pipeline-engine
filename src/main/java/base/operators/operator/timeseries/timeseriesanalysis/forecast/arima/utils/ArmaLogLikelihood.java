package base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils;

import com.github.lbfgs4j.liblbfgs.Function;
import base.operators.operator.timeseries.timeseriesanalysis.exception.WrongExecutionOrderException;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.ArimaWronglyConfiguredException;
import java.util.Arrays;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class ArmaLogLikelihood implements MultivariateFunction, Function {
   public static final String NOT_INITIALIZED_MESSAGE = "The ArmaLogLikelihood was only initialized and not yet evaluated.";
   public static final String NOT_INITIALIZED_METHOD_DESCRIPTOR = "value(double[] parameters)";
   private int p;
   private int q;
   private boolean estimateConstant;
   private boolean transParams;
   private ArimaUtils.ArimaLogLikelihoodType type;
   private double[] arCoefficients;
   private double[] maCoefficients;
   private double constant;
   private double sigmaSquare;
   private double[] values;
   private double[] valuesForCalculation;
   private int T;
   private double[] residuals;
   int calls;

   public ArmaLogLikelihood(int p, int q, boolean estimateConstant, boolean transParams, ArimaUtils.ArimaLogLikelihoodType type, double[] values) {
      this.p = p;
      this.q = q;
      this.estimateConstant = estimateConstant;
      this.transParams = transParams;
      this.type = type;
      this.values = values;
      this.arCoefficients = null;
      this.maCoefficients = null;
      this.calls = 0;
      this.residuals = null;
   }

   public double value(double[] coeffs) {
      double[] coefficients = coeffs;
      if (this.transParams) {
         coefficients = ArimaUtils.transformParams(coeffs, this.p, this.q, this.estimateConstant);
      }

      this.residuals = new double[this.values.length];
      if (this.getNumberOfParameters() != coefficients.length) {
         throw new IllegalArgumentException("Provided coefficients array does not have the correct length. Length of provided array: " + coefficients.length + ", neccessary length: " + this.getNumberOfParameters());
      } else {
         ++this.calls;
         this.arCoefficients = ArimaUtils.getArCoefficientsFromParametersArray(coefficients, this.p, this.q);
         this.maCoefficients = ArimaUtils.getMaCoefficientsFromParametersArray(coefficients, this.p, this.q);
         this.valuesForCalculation = Arrays.copyOf(this.values, this.values.length);
         if (this.estimateConstant) {
            this.constant = ArimaUtils.getConstantFromParametersArray(coefficients, this.p, this.q);

            for(int i = 0; i < this.values.length; ++i) {
               this.valuesForCalculation[i] -= this.constant;
            }
         }

         if (this.type == ArimaUtils.ArimaLogLikelihoodType.EXACT) {
            this.sigmaSquare = ArimaUtils.getSigmaSquareFromParametersArray(coefficients, this.p, this.q, this.estimateConstant);
            return this.calculateExactLogLikelihood();
         } else if (this.type == ArimaUtils.ArimaLogLikelihoodType.CONDITIONAL) {
            return this.calculateConditionalLogLikelihood();
         } else {
            throw new ArimaWronglyConfiguredException(ArimaWronglyConfiguredException.WrongConfigurationType.NOT_SUPPROTED_ARIMA_LOGLIKELIHOODTYPE);
         }
      }
   }

   private double calculateConditionalLogLikelihood() {
      for(int i = 0; i < this.p; ++i) {
         this.residuals[i] = 0.0D;
      }

      this.T = this.values.length;
      double sumTerm = 0.0D;

      for(int t = this.p; t < this.T; ++t) {
         this.residuals[t] = this.calculateResidualForConditionalLikelihood(t);
         sumTerm += Math.pow(this.residuals[t], 2.0D);
      }

      this.sigmaSquare = sumTerm / (double)this.T;
      return this.sigmaSquare != 0.0D && Math.log(6.283185307179586D) + Math.log(this.sigmaSquare) != 0.0D ? (double)(-this.T) / 2.0D * (Math.log(6.283185307179586D) + Math.log(this.sigmaSquare)) - sumTerm / (2.0D * this.sigmaSquare) : Double.NEGATIVE_INFINITY;
   }

   private double calculateResidualForConditionalLikelihood(int t) {
      double residual = this.valuesForCalculation[t];

      int localQ;
      for(localQ = 0; localQ < this.p; ++localQ) {
         residual -= this.valuesForCalculation[t - localQ - 1] * this.arCoefficients[localQ];
      }

      localQ = this.q;
      if (t < this.q) {
         localQ = t;
      }

      for(int i = 0; i < localQ; ++i) {
         residual -= this.residuals[t - i - 1] * this.maCoefficients[i];
      }

      return residual;
   }

   private double calculateExactLogLikelihood() {
      this.T = this.values.length;
      double[] vectorElements = new double[Math.max(this.p, this.q + 1)];
      RealVector stateVector_0 = MatrixUtils.createRealVector(vectorElements);
      RealMatrix S_0 = MatrixUtils.createRealIdentityMatrix(Math.max(this.p, this.q + 1));
      KalmanFilter kalmanFilter = new KalmanFilter(this.p, this.q, this.arCoefficients, this.maCoefficients, this.sigmaSquare, stateVector_0, S_0);
      double logLikelihood = (double)(-this.T) / 2.0D * Math.log(this.sigmaSquare);

      for(int t = 0; t < this.T; ++t) {
         kalmanFilter.calculateFilter(this.valuesForCalculation[t]);
         double v_t = kalmanFilter.getOneStepError();
         double e_t = kalmanFilter.getResidual();
         kalmanFilter.updateFilter();
         this.residuals[t] = e_t;
         logLikelihood -= Math.log(v_t) / 2.0D;
         logLikelihood -= Math.pow(e_t, 2.0D) / (2.0D * this.sigmaSquare * v_t);
      }

      return logLikelihood;
   }

   public int getNumberOfParameters() {
      int numberOfParameters = this.p + this.q;
      if (this.estimateConstant) {
         ++numberOfParameters;
      }

      if (this.type == ArimaUtils.ArimaLogLikelihoodType.EXACT) {
         ++numberOfParameters;
      }

      return numberOfParameters;
   }

   public double[] getArCoefficients() {
      if (this.arCoefficients == null) {
         throw new WrongExecutionOrderException("The ArmaLogLikelihood was only initialized and not yet evaluated.", new String[]{"value(double[] parameters)"});
      } else {
         return this.arCoefficients;
      }
   }

   public double[] getMaCoefficients() {
      if (this.maCoefficients == null) {
         throw new WrongExecutionOrderException("The ArmaLogLikelihood was only initialized and not yet evaluated.", new String[]{"value(double[] parameters)"});
      } else {
         return this.maCoefficients;
      }
   }

   public double getConstant() {
      if (!this.estimateConstant) {
         throw new ArimaWronglyConfiguredException(ArimaWronglyConfiguredException.WrongConfigurationType.CONSTANT_NOT_ENABLED);
      } else if (this.arCoefficients == null) {
         throw new WrongExecutionOrderException("The ArmaLogLikelihood was only initialized and not yet evaluated.", new String[]{"value(double[] parameters)"});
      } else {
         return this.constant;
      }
   }

   public double getSigmaSquare() {
      if (this.arCoefficients == null) {
         throw new WrongExecutionOrderException("The ArmaLogLikelihood was only initialized and not yet evaluated.", new String[]{"value(double[] parameters)"});
      } else {
         return this.sigmaSquare;
      }
   }

   public double[] getResiduals() {
      if (this.residuals == null) {
         throw new WrongExecutionOrderException("The ArmaLogLikelihood was only initialized and not yet evaluated.", new String[]{"value(double[] parameters)"});
      } else {
         return this.residuals;
      }
   }

   public int getDimension() {
      return this.getNumberOfParameters();
   }

   public double valueAt(double[] coefficients) {
      return -this.value(coefficients);
   }

   public double[] gradientAt(double[] x) {
      double[] gradient = new double[x.length];
      double[] xNew = (double[])x.clone();
      double epsilon = 1.0E-8D;

      for(int i = 0; i < x.length; ++i) {
         xNew[i] += epsilon;
         double newVal = this.valueAt(xNew);
         double oldVal = this.valueAt(x);
         gradient[i] = (newVal - oldVal) / epsilon;
         xNew[i] -= epsilon;
      }

      return gradient;
   }

   public void setTransParams(boolean transParams) {
      this.transParams = transParams;
   }
}
