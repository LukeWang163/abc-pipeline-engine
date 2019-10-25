package base.operators.operator.timeseries.timeseriesanalysis.feature;

public interface FeaturesBuilder<I, V> {
   FeaturesBuilder<I, V> reset();

   FeaturesBuilder<I, V> skipInvalidValues();

   SeriesValuesFeature<I, V> build();
}
