package base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils;

import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.WrongExecutionOrderException;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.Arima;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.stat.StatUtils;

import java.util.Arrays;


public class HannanRissanen
{
   private int maxNumberOfIterations;
   private int maxOrderOfInitialARProcess;
   private int p;
   private int q;
   private boolean estimateConstant;
   private double[] values;
   private double[] arimaParameters;
   private double estimatedSigmaSquare;

   public HannanRissanen(int p, int q, boolean estimateConstant, double[] values) { this(p, q, estimateConstant, values, 1, (int)Math.round(12.0D * Math.pow((values.length / 100.0F), 0.25D))); }













   public HannanRissanen(int p, int q, boolean estimateConstant, double[] values, int maxNumberOfIterations, int maxOrderOfInitialARProcess) {
      if (maxNumberOfIterations <= 0) {
         throw new IllegalIndexArgumentException("max number of iterations", Integer.valueOf(maxNumberOfIterations), IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      }

      if (maxOrderOfInitialARProcess <= 0) {
         throw new IllegalIndexArgumentException("max order of initial AR process", Integer.valueOf(maxOrderOfInitialARProcess), IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      }

      if (p + q <= 0) {
         throw new IllegalArgumentException("At least one AR or MA term has to provided.");
      }
      this.maxNumberOfIterations = maxNumberOfIterations;
      this.p = p;
      this.q = q;
      this.estimateConstant = estimateConstant;

      this.values = values;
      this.arimaParameters = null;
      if (maxOrderOfInitialARProcess >= values.length) {
         this.maxOrderOfInitialARProcess = values.length - 1;
      } else {
         this.maxOrderOfInitialARProcess = maxOrderOfInitialARProcess;
      }
   }







   public Arima trainArima() {
      double mean = 0.0D;
      double[] valuesZeroMean = Arrays.copyOf(this.values, this.values.length);
      if (this.estimateConstant) {

         mean = StatUtils.mean(this.values);
         for (int i = 0; i < this.values.length; i++) {
            valuesZeroMean[i] = valuesZeroMean[i] - mean;
         }
      }

      int startK = 1;

      double bestIc = Double.POSITIVE_INFINITY;
      int bestK = 0;
      for (int k = startK; k <= this.maxOrderOfInitialARProcess; k++) {
         double[] valuesForKEvaluation = Arrays.copyOfRange(valuesZeroMean, this.maxOrderOfInitialARProcess - k, valuesZeroMean.length);

         RealVector tempARCoefficients = estimateARCoefficients(valuesForKEvaluation, k);


         double sigmaSquared = computeSigmaSquared(valuesForKEvaluation, tempARCoefficients.toArray(), k);


         double tempIc = computeARSpecificBIC(sigmaSquared, k, valuesZeroMean.length - this.maxOrderOfInitialARProcess);
         if (tempIc < bestIc) {
            bestIc = tempIc;
            bestK = k;
         }
      }


      RealVector estimatedARCoefficients = estimateARCoefficients(valuesZeroMean, bestK);




      double[] estimatedResiduals = new double[valuesZeroMean.length];
      for (int t = 0; t < estimatedResiduals.length; t++) {
         if (t - bestK < 0) {

            estimatedResiduals[t] = 0.0D;
         } else {

            estimatedResiduals[t] = valuesZeroMean[t];
            for (int j = 0; j < bestK; j++) {
               estimatedResiduals[t] = estimatedResiduals[t] - estimatedARCoefficients.getEntry(j) * valuesZeroMean[t - j - 1];
            }
         }
      }

      double[] arCoefficients = new double[this.p];
      double[] maCoefficients = new double[this.q];




      for (int iteration = 0; iteration < this.maxNumberOfIterations; iteration++) {


         double[] armaParameters = calculateARMACoefficientsByRegression(valuesZeroMean, estimatedResiduals, bestK);

         arCoefficients = ArimaUtils.getArCoefficientsFromParametersArray(armaParameters, this.p, this.q);
         maCoefficients = ArimaUtils.getMaCoefficientsFromParametersArray(armaParameters, this.p, this.q);
         this.estimatedSigmaSquare = ArimaUtils.getSigmaSquareFromParametersArray(armaParameters, this.p, this.q, false);

         Arima arima = Arima.create(0, arCoefficients, maCoefficients);
         estimatedResiduals = new double[valuesZeroMean.length];
         for (int i = 0; i < estimatedResiduals.length; i++) {
            estimatedResiduals[i] = valuesZeroMean[i] - arima.forecastOneValue(i, this.values, null, estimatedResiduals);
         }
      }
      double[] lastResiduals = new double[this.q];
      for (int i = 0; i < this.q; i++) {
         lastResiduals[i] = estimatedResiduals[estimatedResiduals.length - this.q + i];
      }

      if (this.estimateConstant) {
         this.arimaParameters = ArimaUtils.getParametersArray(arCoefficients, maCoefficients, mean, true);
      } else {
         this.arimaParameters = ArimaUtils.getParametersArray(arCoefficients, maCoefficients);
      }

      return Arima.create(this.p, 0, this.q, arCoefficients, maCoefficients, mean, lastResiduals);
   }


   private double[] calculateARMACoefficientsByRegression(double[] values, double[] estimatedResiduals, int k) {
      int xStart = 0;
      int yStart = Math.max(k + this.q, this.p);
      int residStart = 0;
      if (this.p < k + this.q) {
         xStart = k + this.q - this.p;
      } else {
         residStart = this.p - k - this.q;
      }

      double[] y = Arrays.copyOfRange(values, yStart, values.length);
      double[][] x = new double[values.length - yStart][this.p + this.q];

      for (int t = this.p + xStart; t < values.length; t++) {
         for (int i = 0; i < this.p; i++) {
            x[t - this.p - xStart][i] = values[t - i - 1];
         }
      }
      for (int t = this.q + residStart + k; t < values.length; t++) {
         for (int i = 0; i < this.q; i++) {
            x[t - this.q - residStart - k][this.p + i] = estimatedResiduals[t - i - 1];
         }
      }

      RealMatrix xValues = MatrixUtils.createRealMatrix(x);
      RealVector yValues = MatrixUtils.createRealVector(y);
      SingularValueDecomposition decomposition = new SingularValueDecomposition(xValues.transpose().multiply(xValues));
      double[] res = decomposition.getSolver().getInverse().multiply(xValues.transpose()).operate(yValues).toArray();

      double[] result = new double[this.p + this.q + 1];
      int i = 0;
      for (double param : res) {
         result[i] = param;
         i++;
      }
      ArmaLogLikelihood armaLogLikelihood = new ArmaLogLikelihood(this.p, this.q, true, false, ArimaUtils.ArimaLogLikelihoodType.CONDITIONAL, values);

      armaLogLikelihood.value(result);
      result[i] = armaLogLikelihood.getSigmaSquare();
      return result;
   }
















   private RealVector estimateARCoefficients(double[] values, int k) {
      RealMatrix M = MatrixUtils.createRealMatrix(k, k);
      RealVector v = MatrixUtils.createRealVector(new double[k]);
      for (int t = k; t < values.length; t++) {

         double[] x_tValues = new double[k];
         for (int j = 0; j < k; j++) {
            x_tValues[j] = values[t - j - 1];
         }
         RealVector x_t = MatrixUtils.createRealVector(x_tValues);
         M = M.add(x_t.outerProduct(x_t));
         v = v.add(x_t.mapMultiply(values[t]));
      }
      M = MatrixUtils.inverse(M);
      return M.operate(v);
   }









   private double computeSigmaSquared(double[] values, double[] arCoefficients, int k) {
      double[] residuals = new double[values.length];

      for (int i = 0; i < k; i++) {
         residuals[i] = 0.0D;
      }
      double sumTerm = 0.0D;
      for (int t = k; t < values.length; t++) {
         double tempResidual = values[t];
         for (int i = 0; i < k; i++) {
            tempResidual -= values[t - i - 1] * arCoefficients[i];
         }
         residuals[t] = tempResidual;
         sumTerm += Math.pow(residuals[t], 2.0D);
      }
      return sumTerm / (values.length - k);
   }











   private double computeARSpecificBIC(double sigmaSquared, int numberOfParameters, int sampleSize) { return Math.log(sigmaSquared) + (numberOfParameters + 1) * Math.log(sampleSize) / sampleSize; }









   public double[] getParameters(boolean retrieveSigmaSquare) {
      if (this.arimaParameters == null) {
         throw new WrongExecutionOrderException("HannanRissanen Algorithm was only intialized and not yet trained.", new String[] { "trainArima()" });
      }

      if (!retrieveSigmaSquare) {
         return this.arimaParameters;
      }
      double[] parameters = new double[this.arimaParameters.length + 1];
      int i = 0;
      for (; i < this.arimaParameters.length; i++) {
         parameters[i] = this.arimaParameters[i];
      }
      parameters[i] = this.estimatedSigmaSquare;
      return parameters;
   }
}
