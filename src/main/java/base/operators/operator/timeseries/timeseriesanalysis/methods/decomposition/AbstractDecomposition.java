package base.operators.operator.timeseries.timeseriesanalysis.methods.decomposition;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import java.util.ArrayList;

public abstract class AbstractDecomposition implements SeriesDecomposition, MultivariateSeriesDecomposition {
   public static final String TREND_POSTFIX = "_Trend";
   public static final String SEASONAL_POSTFIX = "_Seasonal";
   public static final String REMAINDER_POSTFIX = "_Remainder";

   public MultivariateValueSeries compute(MultivariateValueSeries multivariateValueSeries) {
      if (multivariateValueSeries == null) {
         throw new ArgumentIsNullException("multivariate value series");
      } else {
         ArrayList seriesList = new ArrayList();

         for(int seriesCount = 0; seriesCount < multivariateValueSeries.getSeriesCount(); ++seriesCount) {
            ValueSeries originalSeries = multivariateValueSeries.getValueSeries(seriesCount);
            this.initializeOneSeries(originalSeries);
            seriesList.add(originalSeries);
            ValueSeries trendSeries = this.computeValueSeriesTrend(originalSeries);
            seriesList.add(trendSeries);
            ValueSeries seasonalSeries = this.computeValueSeriesSeasonal(originalSeries, trendSeries);
            seriesList.add(seasonalSeries);
            ValueSeries remainderSeries = this.computeValueSeriesRemainder(originalSeries, trendSeries, seasonalSeries);
            seriesList.add(remainderSeries);
            this.finishOneSeries();
         }

         return MultivariateValueSeries.create(seriesList, multivariateValueSeries.hasDefaultIndices());
      }
   }

   public MultivariateTimeSeries compute(MultivariateTimeSeries multivariateTimeSeries) {
      if (multivariateTimeSeries == null) {
         throw new ArgumentIsNullException("multivariate time series");
      } else {
         ArrayList seriesList = new ArrayList();

         for(int seriesCount = 0; seriesCount < multivariateTimeSeries.getSeriesCount(); ++seriesCount) {
            TimeSeries originalSeries = multivariateTimeSeries.getTimeSeries(seriesCount);
            this.initializeOneSeries(originalSeries);
            seriesList.add(originalSeries);
            TimeSeries trendSeries = this.computeTimeSeriesTrend(originalSeries);
            seriesList.add(trendSeries);
            TimeSeries seasonalSeries = this.computeTimeSeriesSeasonal(originalSeries, trendSeries);
            seriesList.add(seasonalSeries);
            TimeSeries remainderSeries = this.computeTimeSeriesRemainder(originalSeries, trendSeries, seasonalSeries);
            seriesList.add(remainderSeries);
            this.finishOneSeries();
         }

         return MultivariateTimeSeries.create(seriesList);
      }
   }

   public MultivariateValueSeries compute(ValueSeries valueSeries) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         ArrayList seriesList = new ArrayList();
         this.initializeOneSeries(valueSeries);
         seriesList.add(valueSeries);
         ValueSeries trendSeries = this.computeValueSeriesTrend(valueSeries);
         seriesList.add(trendSeries);
         ValueSeries seasonalSeries = this.computeValueSeriesSeasonal(valueSeries, trendSeries);
         seriesList.add(seasonalSeries);
         ValueSeries remainderSeries = this.computeValueSeriesRemainder(valueSeries, trendSeries, seasonalSeries);
         seriesList.add(remainderSeries);
         this.finishOneSeries();
         return MultivariateValueSeries.create(seriesList, valueSeries.hasDefaultIndices());
      }
   }

   public MultivariateTimeSeries compute(TimeSeries timeSeries) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         ArrayList seriesList = new ArrayList();
         this.initializeOneSeries(timeSeries);
         seriesList.add(timeSeries);
         TimeSeries trendSeries = this.computeTimeSeriesTrend(timeSeries);
         seriesList.add(trendSeries);
         TimeSeries seasonalSeries = this.computeTimeSeriesSeasonal(timeSeries, trendSeries);
         seriesList.add(seasonalSeries);
         TimeSeries remainderSeries = this.computeTimeSeriesRemainder(timeSeries, trendSeries, seasonalSeries);
         seriesList.add(remainderSeries);
         this.finishOneSeries();
         return MultivariateTimeSeries.create(seriesList);
      }
   }

   protected abstract ValueSeries computeValueSeriesTrend(ValueSeries var1);

   protected abstract TimeSeries computeTimeSeriesTrend(TimeSeries var1);

   protected abstract ValueSeries computeValueSeriesSeasonal(ValueSeries var1, ValueSeries var2);

   protected abstract TimeSeries computeTimeSeriesSeasonal(TimeSeries var1, TimeSeries var2);

   protected abstract ValueSeries computeValueSeriesRemainder(ValueSeries var1, ValueSeries var2, ValueSeries var3);

   protected abstract TimeSeries computeTimeSeriesRemainder(TimeSeries var1, TimeSeries var2, TimeSeries var3);

   protected void initializeOneSeries(Series series) {
   }

   protected void finishOneSeries() {
   }
}
