package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.CombinatoricsUtils;

public class MovingAverageFilter implements ValueSeriesTransformation, TimeSeriesTransformation, MultivariateValueSeriesTransformation, MultivariateTimeSeriesTransformation {
   private int lowerEdge;
   private int upperEdge;
   private double[] weights;

   private MovingAverageFilter(int lowerEdge, int upperEdge, double[] weights) {
      if (lowerEdge + upperEdge + 1 != weights.length) {
         throw new IllegalArgumentException("lowerEdge + UpperEdge + 1 is not equal length of provided weights array.");
      } else if (lowerEdge < 0) {
         throw new IllegalIndexArgumentException("lower edge", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (upperEdge < 0) {
         throw new IllegalIndexArgumentException("upper edge", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (!weightsEqualOne(weights)) {
         throw new IllegalArgumentException("Sum of weights array is not 1.");
      } else {
         this.lowerEdge = lowerEdge;
         this.upperEdge = upperEdge;
         this.weights = weights;
      }
   }

   public static MovingAverageFilter create(int lowerEdge, int upperEdge, double[] weights) {
      if (weights == null) {
         throw new ArgumentIsNullException("weights array");
      } else {
         return new MovingAverageFilter(lowerEdge, upperEdge, weights);
      }
   }

   public static MovingAverageFilter createFromNotNormalizedWeights(int lowerEdge, int upperEdge, double[] notNormalizedWeights) {
      if (notNormalizedWeights == null) {
         throw new ArgumentIsNullException("weights array");
      } else {
         return new MovingAverageFilter(lowerEdge, upperEdge, normalizeWeights(notNormalizedWeights));
      }
   }

   public static MovingAverageFilter createSimpleMovingAverage(int sizeOfFilter) {
      if (sizeOfFilter < 0) {
         throw new IllegalIndexArgumentException("size of filter", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         double[] weightsArray = new double[2 * sizeOfFilter + 1];

         for(int j = 0; j < weightsArray.length; ++j) {
            weightsArray[j] = 1.0D / (2.0D * (double)sizeOfFilter + 1.0D);
         }

         return new MovingAverageFilter(sizeOfFilter, sizeOfFilter, weightsArray);
      }
   }

   public static MovingAverageFilter createBinomMovingAverage(int q) {
      if (q < 0) {
         throw new IllegalIndexArgumentException("q", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         double[] weightsArray = new double[2 * q + 1];
         double constant = 1.0D / Math.pow(2.0D, 2.0D * (double)q);

         for(int j = 0; j < weightsArray.length; ++j) {
            weightsArray[j] = constant * CombinatoricsUtils.binomialCoefficientDouble(2 * q, j);
         }

         return new MovingAverageFilter(q, q, weightsArray);
      }
   }

   public static MovingAverageFilter createSpencers15PointMovingAverage() {
      double[] weights1 = new double[]{0.25D, 0.25D, 0.25D, 0.25D};
      double[] weights2 = new double[]{-0.75D, 0.75D, 1.0D, 0.75D, -0.75D};
      MovingAverageFilter filter1 = create(2, 1, weights1);
      MovingAverageFilter filter2 = create(1, 2, weights1);
      MovingAverageFilter filter3 = createSimpleMovingAverage(2);
      MovingAverageFilter filter4 = create(2, 2, weights2);
      return convoluteFilters(convoluteFilters(convoluteFilters(filter1, filter2), filter3), filter4);
   }

   public static MovingAverageFilter convoluteFilters(MovingAverageFilter filter1, MovingAverageFilter filter2) {
      double[] weights1 = filter1.getWeights();
      double[] weights2 = filter2.getWeights();
      int newLowerEdge = filter1.getLowerEdge() + filter2.getLowerEdge();
      int newUpperEdge = filter1.getUpperEdge() + filter2.getUpperEdge();
      int newWeightsLength = newUpperEdge + newLowerEdge + 1;
      double[] newWeights = new double[newWeightsLength];

      for(int i = 0; i < newWeightsLength; ++i) {
         double weight = 0.0D;

         for(int j = Math.max(0, i - weights2.length + 1); j < weights1.length && i - j >= 0; ++j) {
            weight += weights1[j] * weights2[i - j];
         }

         newWeights[i] = weight;
      }

      return create(newLowerEdge, newLowerEdge, newWeights);
   }

   public TimeSeries compute(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         double[] filteredValues = this.applyFilter(timeSeries.getValues());
         return TimeSeries.create(timeSeries.getIndices(), filteredValues, timeSeries.getName());
      }
   }

   public ValueSeries compute(ValueSeries valueSeries) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         double[] filteredValues = this.applyFilter(valueSeries.getValues());
         return valueSeries.hasDefaultIndices() ? ValueSeries.create(filteredValues) : ValueSeries.create(valueSeries.getIndices(), filteredValues, valueSeries.getName());
      }
   }

   public MultivariateValueSeries compute(MultivariateValueSeries multivariateValueSeries) {
      if (multivariateValueSeries == null) {
         throw new ArgumentIsNullException("multivariate value series");
      } else {
         MultivariateValueSeries newMultivariateValueSeries = multivariateValueSeries.clone();

         for(int i = 0; i < multivariateValueSeries.getSeriesCount(); ++i) {
            newMultivariateValueSeries.setValues(i, this.applyFilter(multivariateValueSeries.getValues(i)));
         }

         return newMultivariateValueSeries;
      }
   }

   public MultivariateTimeSeries compute(MultivariateTimeSeries multivariateTimeSeries) {
      if (multivariateTimeSeries == null) {
         throw new ArgumentIsNullException("multivariate time series");
      } else {
         MultivariateTimeSeries newMultivariateTimeSeries = multivariateTimeSeries.clone();

         for(int i = 0; i < multivariateTimeSeries.getSeriesCount(); ++i) {
            newMultivariateTimeSeries.setValues(i, this.applyFilter(multivariateTimeSeries.getValues(i)));
         }

         return newMultivariateTimeSeries;
      }
   }

   private double[] applyFilter(double[] values) {
      double[] result = new double[values.length];

      for(int i = 0; i < values.length; ++i) {
         result[i] = this.getFilteredValue(i, values);
      }

      return result;
   }

   private double getFilteredValue(int i, double[] values) {
      double result = 0.0D;
      if (i - this.lowerEdge < 0) {
         result = Double.NaN;
      } else if (i + this.upperEdge >= values.length) {
         result = Double.NaN;
      } else {
         for(int j = -this.lowerEdge; j <= this.upperEdge; ++j) {
            result += this.weights[j + this.lowerEdge] * values[i + j];
         }
      }

      return result;
   }

   private static double[] normalizeWeights(double[] weights) {
      double sumOfWeights = StatUtils.sum(weights);
      double[] normalizedWeights = weights;
      if (!weightsEqualOne(weights)) {
         for(int i = 0; i < normalizedWeights.length; ++i) {
            normalizedWeights[i] /= sumOfWeights;
         }
      }

      return normalizedWeights;
   }

   private static boolean weightsEqualOne(double[] weights) {
      return Math.abs(StatUtils.sum(weights) - 1.0D) <= (double)weights.length * Math.ulp(1.0D);
   }

   public int getLowerEdge() {
      return this.lowerEdge;
   }

   public int getUpperEdge() {
      return this.upperEdge;
   }

   public double[] getWeights() {
      return this.weights;
   }
}
