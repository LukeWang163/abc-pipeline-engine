package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class Normalization implements ValueSeriesTransformation, TimeSeriesTransformation, MultivariateValueSeriesTransformation, MultivariateTimeSeriesTransformation {
   public static Normalization create() {
      return new Normalization();
   }

   public ValueSeries compute(ValueSeries valueSeries) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         return valueSeries.hasDefaultIndices() ? ValueSeries.create(this.normalize(valueSeries.getValues())) : ValueSeries.create(valueSeries.getIndices(), this.normalize(valueSeries.getValues()), valueSeries.getName());
      }
   }

   public TimeSeries compute(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         return TimeSeries.create(timeSeries.getIndices(), this.normalize(timeSeries.getValues()), timeSeries.getName());
      }
   }

   public MultivariateValueSeries compute(MultivariateValueSeries multivariateValueSeries) {
      if (multivariateValueSeries == null) {
         throw new ArgumentIsNullException("multivariate value series");
      } else {
         MultivariateValueSeries newMultivariateValueSeries = multivariateValueSeries.clone();

         for(int i = 0; i < multivariateValueSeries.getSeriesCount(); ++i) {
            newMultivariateValueSeries.setValues(i, this.normalize(multivariateValueSeries.getValues(i)));
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
            newMultivariateTimeSeries.setValues(i, this.normalize(multivariateTimeSeries.getValues(i)));
         }

         return newMultivariateTimeSeries;
      }
   }

   private double[] normalize(double[] values) {
      DescriptiveStatistics stats = new DescriptiveStatistics();
      double[] var3 = values;
      int var4 = values.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         double value = var3[var5];
         if (Double.isFinite(value)) {
            stats.addValue(value);
         }
      }

      double mean = stats.getMean();
      double standardDeviation = stats.getStandardDeviation();
      double[] normalizedValues = new double[values.length];

      for(int i = 0; i < normalizedValues.length; ++i) {
         if (Double.isFinite(values[i])) {
            normalizedValues[i] = (values[i] - mean) / standardDeviation;
         } else {
            normalizedValues[i] = values[i];
         }
      }

      return normalizedValues;
   }
}
