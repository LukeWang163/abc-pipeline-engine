package base.operators.operator.timeseries.timeseriesanalysis.forecast.holtwinters;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.TimeSeriesForecast;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.TimeSeriesForecastTrainer;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.ValueSeriesForecast;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.ValueSeriesForecastTrainer;

public class HoltWintersTrainer implements TimeSeriesForecastTrainer, ValueSeriesForecastTrainer {
   private double alpha;
   private double beta;
   private double gamma;
   private int period;
   private HoltWinters.SeasonalModel modelType;

   private HoltWintersTrainer(double alpha, double beta, double gamma, int period, HoltWinters.SeasonalModel modelType) {
      if (alpha > 0.0D && alpha <= 1.0D) {
         if (beta >= 0.0D && beta <= 1.0D) {
            if (gamma >= 0.0D && gamma <= 1.0D) {
               if (period <= 0) {
                  throw new IllegalArgumentException("Period parameter must be greater than 0 (0 <= period). Given value was: " + period);
               } else {
                  this.alpha = alpha;
                  this.beta = beta;
                  this.gamma = gamma;
                  this.period = period;
                  this.modelType = modelType;
               }
            } else {
               throw new IllegalArgumentException("Coefficient gamma must be between 0 and 1 (0 < gamma <= 1). Given value was: " + gamma);
            }
         } else {
            throw new IllegalArgumentException("Coefficient beta must be between 0 and 1 (0 <= beta <= 1). Given value was: " + beta);
         }
      } else {
         throw new IllegalArgumentException("Coefficient alpha must be between 0 and 1 (0 < alpha <= 1). Given value was: " + alpha);
      }
   }

   public static HoltWintersTrainer create(double alpha, double beta, double gamma, int period, HoltWinters.SeasonalModel modelType) {
      return new HoltWintersTrainer(alpha, beta, gamma, period, modelType);
   }

   public TimeSeriesForecast trainForecast(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         return this.trainHoltWinters(timeSeries.getValues());
      }
   }

   public ValueSeriesForecast trainForecast(ValueSeries valueSeries) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         return this.trainHoltWinters(valueSeries.getValues());
      }
   }

   public HoltWinters trainHoltWinters(double[] trainingValues) {
      if (trainingValues.length < 2 * this.period) {
         throw new IllegalArgumentException("The provided period has to be smaller or equal to half of series length. period: " + this.period + " half of series length: " + (double)trainingValues.length / 2.0D);
      } else {
         HoltWinters holtWinters = HoltWinters.create(this.alpha, this.beta, this.gamma, this.period, this.modelType);
         holtWinters.initializeSmoothing(trainingValues);
         return holtWinters;
      }
   }
}
