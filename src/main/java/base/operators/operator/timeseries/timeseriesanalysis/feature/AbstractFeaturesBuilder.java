package base.operators.operator.timeseries.timeseriesanalysis.feature;


public abstract class AbstractFeaturesBuilder<I, V>
        extends Object
        implements FeaturesBuilder<I, V>
{
   protected SeriesValuesFeature<I, V> seriesValuesFeature;

   public AbstractFeaturesBuilder() {
      this.seriesValuesFeature = null;

      reset();
   }


   public FeaturesBuilder<I, V> skipInvalidValues() {
      this.seriesValuesFeature.setSkipInvalidValues(true);
      return this;
   }

   public SeriesValuesFeature<I, V> build() { return this.seriesValuesFeature; }
}
