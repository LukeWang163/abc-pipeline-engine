package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;

public class ExponentialSmoothing implements ValueSeriesTransformation, TimeSeriesTransformation, MultivariateValueSeriesTransformation, MultivariateTimeSeriesTransformation {
   private double alpha;
   private double[] smoothedValues;

   private ExponentialSmoothing(double alpha) {
      if (alpha > 0.0D && alpha <= 1.0D) {
         this.alpha = alpha;
      } else {
         throw new IllegalArgumentException("Coefficient alpha must be between 0 and 1 (0 < alpha <= 1). Given value was: " + alpha);
      }
   }

   public static ExponentialSmoothing createSmoothing(double alpha) {
      return new ExponentialSmoothing(alpha);
   }

   public TimeSeries compute(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         return TimeSeries.create(timeSeries.getIndices(), this.applySmoothing(timeSeries.getValues()), timeSeries.getName());
      }
   }

   public ValueSeries compute(ValueSeries valueSeries) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         return valueSeries.hasDefaultIndices() ? ValueSeries.create(this.applySmoothing(valueSeries.getValues())) : ValueSeries.create(valueSeries.getIndices(), this.applySmoothing(valueSeries.getValues()), valueSeries.getName());
      }
   }

   public MultivariateTimeSeries compute(MultivariateTimeSeries multivariateTimeSeries) {
      if (multivariateTimeSeries == null) {
         throw new ArgumentIsNullException("multivariate time series");
      } else {
         MultivariateTimeSeries newMultivariateTimeSeries = multivariateTimeSeries.clone();

         for(int i = 0; i < multivariateTimeSeries.getSeriesCount(); ++i) {
            newMultivariateTimeSeries.setValues(i, this.applySmoothing(multivariateTimeSeries.getValues(i)));
         }

         return newMultivariateTimeSeries;
      }
   }

   public MultivariateValueSeries compute(MultivariateValueSeries multivariateValueSeries) {
      if (multivariateValueSeries == null) {
         throw new ArgumentIsNullException("multivariate value series");
      } else {
         MultivariateValueSeries newMultivariateValueSeries = multivariateValueSeries.clone();

         for(int i = 0; i < multivariateValueSeries.getSeriesCount(); ++i) {
            newMultivariateValueSeries.setValues(i, this.applySmoothing(multivariateValueSeries.getValues(i)));
         }

         return newMultivariateValueSeries;
      }
   }

   public double[] applySmoothing(double[] values) {
      this.smoothedValues = new double[values.length];
      this.smoothedValues[0] = values[0];

      for(int i = 1; i < values.length; ++i) {
         this.smoothedValues[i] = this.alpha * values[i] + (1.0D - this.alpha) * this.smoothedValues[i - 1];
      }

      return this.smoothedValues;
   }

   public double[] getSmoothedValues() {
      return this.smoothedValues;
   }
}
