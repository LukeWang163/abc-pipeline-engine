package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;

public class LogTransformation implements ValueSeriesTransformation, TimeSeriesTransformation, MultivariateValueSeriesTransformation, MultivariateTimeSeriesTransformation {
   private LogarithmType logarithmType;

   private LogTransformation(LogarithmType logarithmType) {
      this.logarithmType = logarithmType;
   }

   public static LogTransformation createLnTransformation() {
      return new LogTransformation(LogarithmType.LN);
   }

   public static LogTransformation createLog10Transformation() {
      return new LogTransformation(LogarithmType.LOG10);
   }

   public TimeSeries compute(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         return TimeSeries.create(timeSeries.getIndices(), this.applyLog(timeSeries.getValues()), timeSeries.getName());
      }
   }

   public ValueSeries compute(ValueSeries valueSeries) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         return valueSeries.hasDefaultIndices() ? ValueSeries.create(this.applyLog(valueSeries.getValues())) : ValueSeries.create(valueSeries.getIndices(), this.applyLog(valueSeries.getValues()), valueSeries.getName());
      }
   }

   public MultivariateTimeSeries compute(MultivariateTimeSeries multivariateTimeSeries) {
      if (multivariateTimeSeries == null) {
         throw new ArgumentIsNullException("multivariate time series");
      } else {
         MultivariateTimeSeries newMultivariateTimeSeries = multivariateTimeSeries.clone();

         for(int i = 0; i < multivariateTimeSeries.getSeriesCount(); ++i) {
            newMultivariateTimeSeries.setValues(i, this.applyLog(multivariateTimeSeries.getValues(i)));
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
            newMultivariateValueSeries.setValues(i, this.applyLog(multivariateValueSeries.getValues(i)));
         }

         return newMultivariateValueSeries;
      }
   }

   private double[] applyLog(double[] values) {
      double[] logValues = new double[values.length];

      for(int i = 0; i < values.length; ++i) {
         switch(this.logarithmType) {
         case LN:
            logValues[i] = Math.log(values[i]);
            break;
         case LOG10:
         default:
            logValues[i] = Math.log10(values[i]);
         }
      }

      return logValues;
   }

   public static enum LogarithmType {
      LN,
      LOG10;
   }
}
