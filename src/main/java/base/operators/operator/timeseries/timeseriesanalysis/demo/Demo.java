package base.operators.operator.timeseries.timeseriesanalysis.demo;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.LogTransformation;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.MovingAverageFilter;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.Normalization;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class Demo {
   private static final String INDEX_STRING = "index";
   private static final String AMOUNT_STRING = "amount";

   public static void main(String[] args) throws IOException {
      ValueSeries valueSeries = SeriesIO.readValueSeriesFromCSV("testdata/milk_int_index.csv", ',', 1);
      LogTransformation logTransformation = LogTransformation.createLnTransformation();
      ValueSeries logValueSeries = logTransformation.compute(valueSeries);
      Normalization normalization = Normalization.create();
      MovingAverageFilter simpleFilter = MovingAverageFilter.createSimpleMovingAverage(10);
      ValueSeries normalizedValueSeries = normalization.compute(valueSeries.clone());
      ValueSeries filteredValueSeries = simpleFilter.compute(valueSeries);
      SeriesIO.writeValueSeriesToCSV(normalizedValueSeries, "bin/milk_int_index_normalized.csv", ',');
      TimeSeries timeSeries = SeriesIO.readTimeSeriesFromCSV("testdata/milk_date_index.csv", ',', 1, DateTimeFormatter.ISO_DATE);
      TimeSeries normalizedTimeSeries = normalization.compute(timeSeries);
      SeriesIO.writeTimeSeriesToCSV(normalizedTimeSeries, "bin/milk_date_index_normalized.csv", ',', DateTimeFormatter.ISO_DATE);
      SeriesChart valueSeriesChart = SeriesChart.create("Value Series", "Milk Data", "index", "amount", valueSeries);
      valueSeriesChart.plot();
      SeriesChart logValueSeriesChart = SeriesChart.create("Log Value Series", "Log Milk Data", "index", "amount", logValueSeries);
      logValueSeriesChart.plot();
      SeriesChart normalizedValueSeriesChart = SeriesChart.create("Normalized Value Series", "Normalized Milk Data", "index", "amount", normalizedValueSeries);
      normalizedValueSeriesChart.plot();
      SeriesChart filteredValueSeriesChart = SeriesChart.create("Filtered Value Series", "Filtered Milk Data", "index", "amount", filteredValueSeries);
      filteredValueSeriesChart.plot();
      ValueSeries nonFiniteValueSeries = ValueSeries.create(GenerateData.generateRandomValues(0.0D, 1000.0D, 100, false));
      nonFiniteValueSeries.setValue(7, Double.NaN);
      SeriesChart nonFiniteValueSeriesChart = SeriesChart.create("nonFinite Value Series", "Values", "index", "amount", nonFiniteValueSeries);
      nonFiniteValueSeriesChart.plot();
      TimeSeries defaultTimeSeries = TimeSeries.create(GenerateData.generateRandomSortedTimeIndices(0, 1000, 100), GenerateData.generateRandomValues(0.0D, 1000.0D, 100, false));
      SeriesChart defaultTimeSeriesChart = SeriesChart.create("Default Time Series", "Values", "time", "amount", defaultTimeSeries);
      defaultTimeSeriesChart.plot();
   }
}
