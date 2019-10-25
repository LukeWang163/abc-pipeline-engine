package base.operators.operator.timeseries.timeseriesanalysis.methods.decomposition;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.methods.arithmetik.SeriesArithmetik;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.MovingAverageFilter;
import base.operators.operator.timeseries.timeseriesanalysis.tools.SeriesUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Arrays;


public class ClassicDecomposition
        extends AbstractDecomposition
{
   private DecompositionMode mode;
   private int seasonality;

   public enum DecompositionMode
   {
      ADDITIVE, MULTIPLICATIVE; }

   private ClassicDecomposition(DecompositionMode mode, int seasonality) {
      this.mode = DecompositionMode.ADDITIVE;
      this.seasonality = 12;







      this.mode = mode;
      this.seasonality = seasonality;
   }


   public static ClassicDecomposition create(DecompositionMode mode, int seasonality) { return new ClassicDecomposition(mode, seasonality); }



   protected ValueSeries computeValueSeriesTrend(ValueSeries originalSeries) {
      String trendSeriesName = originalSeries.getName() + "_Trend";
      if (originalSeries.hasDefaultIndices()) {
         return ValueSeries.create(computeTrendValues(originalSeries), trendSeriesName);
      }
      return ValueSeries.create(originalSeries.getIndices(), computeTrendValues(originalSeries), trendSeriesName);
   }



   protected TimeSeries computeTimeSeriesTrend(TimeSeries originalSeries) {
      return TimeSeries.create(originalSeries.getIndices(), computeTrendValues(originalSeries), originalSeries
              .getName() + "_Trend");
   }







   private double[] computeTrendValues(Series series) {
      int lowerEdge = (this.seasonality - 1) / 2;
      int upperEdge = this.seasonality / 2;
      double[] weights = new double[this.seasonality];
      Arrays.fill(weights, 1.0D / this.seasonality);
      MovingAverageFilter trendFilter = MovingAverageFilter.create(lowerEdge, upperEdge, weights);
      if (this.seasonality % 2 == 0) {
         trendFilter = MovingAverageFilter.convoluteFilters(MovingAverageFilter.create(1, 0, new double[] { 0.5D, 0.5D }), trendFilter);
      }

      if (series instanceof ValueSeries)
         return trendFilter.compute((ValueSeries)series).getValues();
      if (series instanceof TimeSeries) {
         return trendFilter.compute((TimeSeries)series).getValues();
      }
      throw new IllegalArgumentException("Class of provided series is not supported. Class: " + series
              .getClass().getName());
   }



   protected ValueSeries computeValueSeriesSeasonal(ValueSeries originalSeries, ValueSeries trendSeries) {
      String seasonalSeriesName = originalSeries.getName() + "_Seasonal";
      if (originalSeries.hasDefaultIndices()) {
         return ValueSeries.create(computeSeasonalValues(originalSeries, trendSeries), seasonalSeriesName);
      }
      return ValueSeries.create(originalSeries.getIndices(), computeSeasonalValues(originalSeries, trendSeries), seasonalSeriesName);
   }




   protected TimeSeries computeTimeSeriesSeasonal(TimeSeries originalSeries, TimeSeries trendSeries) {
      return TimeSeries.create(originalSeries.getIndices(), computeSeasonalValues(originalSeries, trendSeries), originalSeries
              .getName() + "_Seasonal");
   }






   private double[] computeSeasonalValues(Series series, Series trendComponent) {
      SeriesUtils.checkSeriesClasses(new Series[] { series, trendComponent });

      Series detrendedSeries = null;
      if (this.mode == DecompositionMode.ADDITIVE) {
         detrendedSeries = SeriesArithmetik.substract(series, trendComponent);
      } else {
         detrendedSeries = SeriesArithmetik.divide(series, trendComponent);
      }


      DescriptiveStatistics[] stats = new DescriptiveStatistics[this.seasonality];
      for (int j = 0; j < stats.length; j++) {
         stats[j] = new DescriptiveStatistics();
      }
      for (int i = 0; i < detrendedSeries.getLength(); i++) {
         double detrendedValue = detrendedSeries.getValue(i);
         if (Double.isFinite(detrendedValue)) {
            stats[i % this.seasonality].addValue(detrendedValue);
         }
      }

      DescriptiveStatistics seasonalityComponentsStat = new DescriptiveStatistics();
      for (DescriptiveStatistics stat : stats) {
         seasonalityComponentsStat.addValue(stat.getMean());
      }
      double sum = seasonalityComponentsStat.getSum();
      double[] seasonalityComponents = seasonalityComponentsStat.getValues();
      for (int j = 0; j < this.seasonality; j++) {
         if (this.mode == DecompositionMode.ADDITIVE) {
            seasonalityComponents[j] = seasonalityComponents[j] - sum / this.seasonality;
         } else {
            seasonalityComponents[j] = seasonalityComponents[j] / sum / this.seasonality;
         }
      }

      double[] result = new double[series.getLength()];
      for (int i = 0; i < result.length; i++) {
         int seasonalityIndex = i % this.seasonality;
         result[i] = seasonalityComponents[seasonalityIndex];
      }
      return result;
   }



   protected ValueSeries computeValueSeriesRemainder(ValueSeries originalSeries, ValueSeries trendSeries, ValueSeries seasonalSeries) {
      String seasonalSeriesName = originalSeries.getName() + "_Remainder";
      if (originalSeries.hasDefaultIndices()) {
         return ValueSeries.create(computeRemainderValues(originalSeries, trendSeries, seasonalSeries), seasonalSeriesName);
      }

      return ValueSeries.create(originalSeries.getIndices(),
              computeRemainderValues(originalSeries, trendSeries, seasonalSeries), seasonalSeriesName);
   }




   protected TimeSeries computeTimeSeriesRemainder(TimeSeries originalSeries, TimeSeries trendSeries, TimeSeries seasonalSeries) {
      return TimeSeries.create(originalSeries.getIndices(),
              computeRemainderValues(originalSeries, trendSeries, seasonalSeries), originalSeries
                      .getName() + "_Remainder");
   }







   private double[] computeRemainderValues(Series series, Series trendComponent, Series seasonalComponent) {
      SeriesUtils.checkSeriesClasses(new Series[] { series, trendComponent, seasonalComponent });
      if (this.mode == DecompositionMode.ADDITIVE) {
         return SeriesArithmetik.substract(SeriesArithmetik.substract(series, trendComponent), seasonalComponent)
                 .getValues();
      }
      return SeriesArithmetik.divide(SeriesArithmetik.divide(series, trendComponent), seasonalComponent).getValues();
   }
}
