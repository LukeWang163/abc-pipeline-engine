package base.operators.operator.timeseries.timeseriesanalysis.demo;

import com.google.gson.Gson;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.Arima;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.ArimaTrainer;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils.ArimaUtils;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils.HannanRissanen;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Arrays;

public class ArimaTestCase {
   private double[] originalParameters;
   private double[] initialParameters;
   private double[] finParameters;
   private int p;
   private int q;
   private int n;
   private double[] factorRange;
   private int factorNumberPoints;
   private double[] initialTrustRange;
   private int initialTrustNumberPoints;
   private boolean initialTrustLogarithmicSteps;
   private double[] stoppingTrustRange;
   private int stoppingTrustNumberPoints;
   private boolean stoppingTrustLogarithmicSteps;
   private double finFactor;
   private double finInitialTrust;
   private double finStoppingTrust;
   private double finDifference;
   private double finLikelihood;
   private double finAic;
   private double finBic;
   private double finCorrectedAic;
   private Long seed;
   private transient ValueSeries testData;
   private transient ValueSeries dataFromFittedArima;
   private transient ArimaTrainer arimaTrainer;

   public ArimaTestCase(int p, int q, ValueSeries testData) {
      this.p = p;
      this.q = q;
      this.testData = testData;
      this.n = testData.getLength();
      this.arimaTrainer = ArimaTrainer.create(p, 0, q);
   }

   public ArimaTestCase(double[] originalParameters, double[] initialParameters, int p, int q, int n, long seed) throws IOException {
      if (p + q + 1 != originalParameters.length) {
         throw new IllegalArgumentException("Length of originalParameters array is not p + q + 1");
      } else if (originalParameters.length != initialParameters.length) {
         throw new IllegalArgumentException("Length of initialParameters is not equal to length of originalParameters.");
      } else if (n <= 0) {
         throw new IllegalIndexArgumentException("number of values", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else {
         this.factorRange = new double[]{1.0D, 1.8D};
         this.factorNumberPoints = 4;
         this.initialTrustRange = new double[]{1.0E-5D, 0.5D};
         this.initialTrustNumberPoints = 8;
         this.initialTrustLogarithmicSteps = true;
         this.stoppingTrustRange = new double[]{1.0E-7D, 0.5D};
         this.stoppingTrustNumberPoints = 8;
         this.stoppingTrustLogarithmicSteps = true;
         this.originalParameters = originalParameters;
         this.initialParameters = initialParameters;
         this.p = p;
         this.q = q;
         this.n = n;
         this.seed = seed;
         double[] currentAR = new double[p];
         double[] currentMA = new double[q];
         int j = 0;

         int i;
         for(i = 0; i < p; ++i) {
            currentAR[i] = originalParameters[j];
            ++j;
         }

         for(i = 0; i < q; ++i) {
            currentMA[i] = originalParameters[j];
            ++j;
         }

         double sigmaSquare = originalParameters[j];
         this.testData = GenerateData.generateArimaSeries(p, 0, q, currentAR, currentMA, 0.0D, Math.sqrt(sigmaSquare), n, (Long)this.seed);
         SeriesIO.writeValueSeriesToJSON("./arimaTestData.json", this.testData);
      }
   }

   public void performGridOptimization() {
      this.finDifference = Double.MAX_VALUE;
      this.arimaTrainer = ArimaTrainer.create(this.p, 0, this.q, true, true, ArimaUtils.TrainingAlgorithm.CONDITIONAL_THEN_EXACT_MAX_LOGLIKELIHOOD, ArimaUtils.OptimizationMethod.BOBYQA, 1000, false);
      this.arimaTrainer.setInitialParameters(this.initialParameters);
      double[] factorPoints = this.getGridParametersFromRange(this.factorNumberPoints, this.factorRange, false);
      double[] initialTrustPoints = this.getGridParametersFromRange(this.initialTrustNumberPoints, this.initialTrustRange, this.initialTrustLogarithmicSteps);
      double[] stoppingTrustPoints = this.getGridParametersFromRange(this.stoppingTrustNumberPoints, this.stoppingTrustRange, this.stoppingTrustLogarithmicSteps);
      double[] var4 = factorPoints;
      int var5 = factorPoints.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         double factor = var4[var6];
         double[] var9 = initialTrustPoints;
         int var10 = initialTrustPoints.length;

         for(int var11 = 0; var11 < var10; ++var11) {
            double initial = var9[var11];
            double[] var14 = stoppingTrustPoints;
            int var15 = stoppingTrustPoints.length;

            for(int var16 = 0; var16 < var15; ++var16) {
               double stopping = var14[var16];
               this.arimaTrainer.setOptimizationParameters(new double[]{factor, initial, stopping});

               try {
                  this.arimaTrainer.trainForecast(this.testData);
               } catch (Exception var21) {
                  continue;
               }

               double difference = this.calculateDifference(this.originalParameters, this.arimaTrainer.getFinalParameters());
               if (difference < this.finDifference) {
                  this.finDifference = difference;
                  this.finParameters = this.arimaTrainer.getFinalParameters();
                  this.finFactor = factor;
                  this.finInitialTrust = initial;
                  this.finStoppingTrust = stopping;
                  this.finLikelihood = this.arimaTrainer.getFinalLogLikelihood();
                  this.finAic = this.arimaTrainer.getFinalAicValue();
                  this.finBic = this.arimaTrainer.getFinalBicValue();
                  this.finCorrectedAic = this.arimaTrainer.getFinalCorrectedAicValue();
               }
            }
         }
      }

   }

   public void applyLinearRegressionForOptimizationParameter() {
      this.arimaTrainer = ArimaTrainer.create(this.p, 0, this.q, true, true, ArimaUtils.TrainingAlgorithm.CONDITIONAL_THEN_EXACT_MAX_LOGLIKELIHOOD, ArimaUtils.OptimizationMethod.BOBYQA, 1000, true);
      this.arimaTrainer.setInitialParameters(this.initialParameters);
      this.arimaTrainer.trainForecast(this.testData);
      this.finParameters = this.arimaTrainer.getFinalParameters();
      this.finDifference = this.calculateChiSquareTest();
      this.finLikelihood = this.arimaTrainer.getFinalLogLikelihood();
      this.finAic = this.arimaTrainer.getFinalAicValue();
      this.finBic = this.arimaTrainer.getFinalBicValue();
      this.finCorrectedAic = this.arimaTrainer.getFinalCorrectedAicValue();
   }

   public void applyHannanRissanenAlgorithm() {
      HannanRissanen hannanRissanen = new HannanRissanen(this.p, this.q, true, this.testData.getValues());
      hannanRissanen.trainArima();
      this.finParameters = hannanRissanen.getParameters(true);
      this.finDifference = this.calculateChiSquareTest();
   }

   public void applyArimaTrainer() {
      this.arimaTrainer = ArimaTrainer.create(this.p, 0, this.q, true, false, ArimaUtils.TrainingAlgorithm.CONDITIONAL_MAX_LOGLIKELIHOOD, ArimaUtils.OptimizationMethod.BOBYQA, 1000, false);
      Arima trainedArima = (Arima)this.arimaTrainer.trainForecast(this.testData);
      System.out.println(trainedArima.toString());
   }

   private double calculateChiSquareTest() {
      double[] estimatedAR = new double[this.p];
      double[] estimatedMA = new double[this.q];
      int j = 0;

      int i;
      for(i = 0; i < this.p; ++i) {
         estimatedAR[i] = this.finParameters[j];
         ++j;
      }

      for(i = 0; i < this.q; ++i) {
         estimatedMA[i] = this.finParameters[j];
         ++j;
      }

      double estimatedSigmaSquare = this.finParameters[j];
      this.dataFromFittedArima = GenerateData.generateArimaSeries(this.p, 0, this.q, estimatedAR, estimatedMA, 0.0D, Math.sqrt(estimatedSigmaSquare), this.n, (Long)this.seed);
      double sum = 0.0D;

      for( i = 0; i < this.n; ++i) {
         if (this.testData.getValue(i) == 0.0D) {
            System.out.println("i: " + i);
         }

         sum += Math.pow(this.testData.getValue(i) - this.dataFromFittedArima.getValue(i), 2.0D) / Math.abs(this.testData.getValue(i));
      }

      sum = Math.sqrt(sum) / (double)(this.n - this.finParameters.length);
      return sum;
   }

   private double calculateDifference(double[] goalValues, double[] fittedValues) {
      if (goalValues.length != fittedValues.length) {
         throw new InvalidParameterException("Length of goalValues and fittedValues is not equal.");
      } else {
         double difference = 0.0D;

         for(int i = 0; i < goalValues.length; ++i) {
            difference += Math.pow((goalValues[i] - fittedValues[i]) / goalValues[i], 2.0D);
         }

         difference = Math.sqrt(difference) / (double)goalValues.length;
         return difference;
      }
   }

   private double[] getGridParametersFromRange(int numberPoints, double[] range, boolean logarithmic) {
      double[] result = new double[numberPoints];
      double start = range[0];
      double stop = range[1];
      if (logarithmic) {
         start = Math.log10(start);
         stop = Math.log10(stop);
      }

      double step = (stop - start) / (double)(numberPoints - 1);

      for(int i = 0; i < numberPoints; ++i) {
         result[i] = start + step * (double)i;
         if (logarithmic) {
            result[i] = Math.pow(10.0D, result[i]);
         }
      }

      return result;
   }

   public String toString() {
      return "ArimaTestCase [originalParameters=" + Arrays.toString(this.originalParameters) + ", initialParameters=" + Arrays.toString(this.initialParameters) + ", finParameters=" + Arrays.toString(this.finParameters) + ", p=" + this.p + ", q=" + this.q + ", n=" + this.n + ", factorRange=" + Arrays.toString(this.factorRange) + ", factorNumberPoints=" + this.factorNumberPoints + ", initialTrustRange=" + Arrays.toString(this.initialTrustRange) + ", initialTrustNumberPoints=" + this.initialTrustNumberPoints + ", initialTrustLogarithmicSteps=" + this.initialTrustLogarithmicSteps + ", stoppingTrustRange=" + Arrays.toString(this.stoppingTrustRange) + ", stoppingTrustNumberPoints=" + this.stoppingTrustNumberPoints + ", stoppingTrustLogarithmicSteps=" + this.stoppingTrustLogarithmicSteps + ", finFactor=" + this.finFactor + ", finInitialTrust=" + this.finInitialTrust + ", finStoppingTrust=" + this.finStoppingTrust + ", finDifference=" + this.finDifference + "]";
   }

   public String getTestResultAsString() {
      StringBuilder builder = new StringBuilder();
      if (this.originalParameters != null) {
         builder.append("originalParameters: ").append(this.originalParameters).append("\n");
      }

      builder.append("finParameters: ").append(this.finParameters).append("\n");
      return builder.append("finDifference (ChiSquareTest): ").append(this.finDifference).toString();
   }

   public String getJsonString() {
      Gson gson = new Gson();
      return gson.toJson((Object)this);
   }

   public void setOriginalParameters(double[] originalParameters) {
      this.originalParameters = originalParameters;
   }

   public void setTestData(ValueSeries testData) {
      this.testData = testData;
   }

   public void setFactorRange(double[] factorRange) {
      this.factorRange = factorRange;
   }

   public void setFactorNumberPoints(int factorNumberPoints) {
      this.factorNumberPoints = factorNumberPoints;
   }

   public void setInitialTrustRange(double[] initialTrustRange) {
      this.initialTrustRange = initialTrustRange;
   }

   public void setInitialTrustNumberPoints(int initialTrustNumberPoints) {
      this.initialTrustNumberPoints = initialTrustNumberPoints;
   }

   public void setInitialTrustLogarithmicSteps(boolean initialTrustLogarithmicSteps) {
      this.initialTrustLogarithmicSteps = initialTrustLogarithmicSteps;
   }

   public void setStoppingTrustRange(double[] stoppingTrustRange) {
      this.stoppingTrustRange = stoppingTrustRange;
   }

   public void setStoppingTrustNumberPoints(int stoppingTrustNumberPoints) {
      this.stoppingTrustNumberPoints = stoppingTrustNumberPoints;
   }

   public void setStoppingTrustLogarithmicSteps(boolean stoppingTrustLogarithmicSteps) {
      this.stoppingTrustLogarithmicSteps = stoppingTrustLogarithmicSteps;
   }
}
