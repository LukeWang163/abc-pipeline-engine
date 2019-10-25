package base.operators.operator.timeseries.timeseriesanalysis.forecast.holtwinters;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.WrongExecutionOrderException;
import base.operators.operator.timeseries.timeseriesanalysis.forecast.ForecastModel;
import java.util.ArrayList;

public class HoltWinters extends ForecastModel {
   private static final long serialVersionUID = -8153756877396950276L;
   private double alpha;
   private double beta;
   private double gamma;
   private int period;
   private SeasonalModel modelType;
   private double[] levelSmoothing;
   private double[] trendSmoothing;
   private double[] seasonalSmoothing;

   private HoltWinters(double alpha, double beta, double gamma, int period, SeasonalModel modelType, int forecastHorizon, String forecastSeriesName) {
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
                  this.setForecastHorizon(forecastHorizon);
                  this.setForecastSeriesName(forecastSeriesName);
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

   public static HoltWinters create(double alpha, double beta, double gamma, int period, SeasonalModel modelType, int forecastHorizon, String forecastSeriesName) {
      return new HoltWinters(alpha, beta, gamma, period, modelType, forecastHorizon, forecastSeriesName);
   }

   public static HoltWinters create(double alpha, double beta, double gamma, int period, SeasonalModel modelType) {
      return new HoltWinters(alpha, beta, gamma, period, modelType, 0, "Forecast");
   }

   public MultivariateTimeSeries forecast(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         ArrayList valuesList = new ArrayList();
         ArrayList nameList = new ArrayList();
         valuesList.add(this.forecastValues(timeSeries.getValues()));
         nameList.add(this.getForecastSeriesName());
         ArrayList indices = this.getIndicesForForecastedValues(timeSeries, ((double[])valuesList.get(0)).length);
         return MultivariateTimeSeries.create(indices, valuesList, nameList);
      }
   }

   public MultivariateValueSeries forecast(ValueSeries valueSeries) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         ArrayList valuesList = new ArrayList();
         ArrayList nameList = new ArrayList();
         valuesList.add(this.forecastValues(valueSeries.getValues()));
         nameList.add(this.getForecastSeriesName());
         if (valueSeries.hasDefaultIndices()) {
            return MultivariateValueSeries.create(valuesList, nameList);
         } else {
            double[] indices = this.getIndicesForForecastedValues(valueSeries, ((double[])valuesList.get(0)).length);
            return MultivariateValueSeries.create(indices, valuesList, nameList);
         }
      }
   }

   private double[] forecastValues(double[] values) {
      double[] forecast = new double[values.length + this.forecastHorizon];

      for(int i = values.length; i < values.length + this.forecastHorizon; ++i) {
         int m = i - values.length + 1;
         int n = values.length - this.period + (m - 1) % this.period;
         if (this.modelType == SeasonalModel.ADDITIVE) {
            forecast[i] = this.levelSmoothing[values.length - 1] + (double)m * this.trendSmoothing[values.length - 1] + this.seasonalSmoothing[n];
         } else {
            forecast[i] = (this.levelSmoothing[values.length - 1] + (double)m * this.trendSmoothing[values.length - 1]) * this.seasonalSmoothing[n];
         }
      }

      double[] forecastOnly = new double[this.forecastHorizon];
      System.arraycopy(forecast, values.length, forecastOnly, 0, this.forecastHorizon);
      return forecastOnly;
   }

   public void initializeSmoothing(double[] values) {
      if (this.levelSmoothing == null && this.trendSmoothing == null && this.seasonalSmoothing == null) {
         this.levelSmoothing = new double[values.length];
         this.trendSmoothing = new double[values.length];
         this.seasonalSmoothing = new double[values.length];
         int seasons = values.length / this.period;
         this.levelSmoothing[0] = values[0];
         this.trendSmoothing[0] = this.initialTrend(values, this.period);
         double[] initialSeasonalIndices = this.calculateSeasonalIndices(values, this.period, seasons);
         System.arraycopy(initialSeasonalIndices, 0, this.seasonalSmoothing, 0, this.period);

         for(int i = 1; i < values.length; ++i) {
            if (i - this.period >= 0) {
               if (this.modelType == SeasonalModel.ADDITIVE) {
                  this.levelSmoothing[i] = this.alpha * (values[i] - this.seasonalSmoothing[i - this.period]) + (1.0D - this.alpha) * (this.levelSmoothing[i - 1] + this.trendSmoothing[i - 1]);
               } else {
                  this.levelSmoothing[i] = this.alpha * values[i] / this.seasonalSmoothing[i - this.period] + (1.0D - this.alpha) * (this.levelSmoothing[i - 1] + this.trendSmoothing[i - 1]);
               }
            } else {
               this.levelSmoothing[i] = this.alpha * (values[i] - this.seasonalSmoothing[i % this.period]) + (1.0D - this.alpha) * (this.levelSmoothing[i] + this.trendSmoothing[i]);
            }

            this.trendSmoothing[i] = this.beta * (this.levelSmoothing[i] - this.levelSmoothing[i - 1]) + (1.0D - this.beta) * this.trendSmoothing[i - 1];
            if (i - this.period >= 0) {
               if (this.modelType == SeasonalModel.ADDITIVE) {
                  this.seasonalSmoothing[i] = this.gamma * (values[i] - this.levelSmoothing[i]) + (1.0D - this.gamma) * this.seasonalSmoothing[i - this.period];
               } else {
                  this.seasonalSmoothing[i] = this.gamma * (values[i] / this.levelSmoothing[i]) + (1.0D - this.gamma) * this.seasonalSmoothing[i - this.period];
               }
            }
         }

      } else {
         throw new WrongExecutionOrderException("already initialized", new String[0]);
      }
   }

   private double[] calculateSeasonalIndices(double[] values, int period, int seasons) {
      double[] seasonalAverage = new double[seasons];
      double[] seasonalIndices = new double[period];

      int i;
      int j;
      for(i = 0; i < seasons; ++i) {
         for(j = 0; j < period; ++j) {
            seasonalAverage[i] += values[i * period + j];
         }

         seasonalAverage[i] /= (double)period;
      }

      for(i = 0; i < period; ++i) {
         for(j = 0; j < seasons; ++j) {
            seasonalIndices[i] += values[period * j + i] / seasonalAverage[j];
         }

         seasonalIndices[i] /= (double)seasons;
      }

      return seasonalIndices;
   }

   private double initialTrend(double[] values, int period) {
      double betaInit = 0.0D;

      for(int i = 0; i < period; ++i) {
         betaInit += values[period + i] - values[i];
      }

      return betaInit / (double)(period * period);
   }

   public String toString() {
      return "Holt-Winters Model (alpha: " + this.alpha + ", beta: " + this.beta + ", gamma: " + this.gamma + ", period: " + this.period + ")";
   }

   public ForecastModel copy() {
      return create(this.alpha, this.beta, this.gamma, this.period, this.modelType, this.forecastHorizon, this.getForecastSeriesName());
   }

   public double getAlpha() {
      return this.alpha;
   }

   public double getBeta() {
      return this.beta;
   }

   public double getGamma() {
      return this.gamma;
   }

   public int getPeriod() {
      return this.period;
   }

   public static enum SeasonalModel {
      ADDITIVE,
      MULTIPLICATIVE;
   }
}
