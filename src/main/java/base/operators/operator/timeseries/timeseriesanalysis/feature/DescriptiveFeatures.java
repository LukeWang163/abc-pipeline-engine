package base.operators.operator.timeseries.timeseriesanalysis.feature;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.WrongExecutionOrderException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Pair;

import java.util.*;

import static java.lang.Double.NaN;


public class DescriptiveFeatures
        extends Object
        implements SeriesValuesFeature<Object, Double>
{
   public static class DescripitveFeaturesBuilder
           extends AbstractFeaturesBuilder<Object, Double>
   {
      public DescripitveFeaturesBuilder reset() {
         this.seriesValuesFeature = new DescriptiveFeatures();
         return this;
      }







      public DescripitveFeaturesBuilder enableAllFeatures() {
         for (DescriptiveFeatures.Feature feature : DescriptiveFeatures.Feature.values()) {
            ((DescriptiveFeatures)this.seriesValuesFeature).enableFeatureCalculation(feature, true);
         }
         return this;
      }









      public DescripitveFeaturesBuilder enableFeature(DescriptiveFeatures.Feature feature) {
         ((DescriptiveFeatures)this.seriesValuesFeature).enableFeatureCalculation(feature, true);
         return this;
      }



      public DescripitveFeaturesBuilder skipInvalidValues() { return (DescripitveFeaturesBuilder)super.skipInvalidValues(); }
   }












   public enum Feature
   {
      SUM, MEAN, GEOMETRIC_MEAN, FIRST_QUARTILE, MEDIAN, THIRD_QUARTILE, MIN, MAX, STD_DEVIATION, KURTOSIS, SKEWNESS;



      public String toString() { return name().toLowerCase().replace("_", " "); }
   }


   private boolean skipInvalidValues = false;


   private Map<Feature, Double> calculatedFeatures;


   private Map<Feature, Boolean> featuresToCalculate = new LinkedHashMap(); protected DescriptiveFeatures() {
   for (Feature feature : Feature.values()) {
      this.featuresToCalculate.put(feature, Boolean.valueOf(false));
   }
   this.calculatedFeatures = null;
}





   public static final String FEATURE_NAME = "descriptive_features";




   public void enableFeatureCalculation(Feature feature, boolean toCalculate) { this.featuresToCalculate.put(feature, Boolean.valueOf(toCalculate)); }




   public String getName() { return "descriptive_features"; }




   public String[] getFeatureNames() { return (String[])Arrays.stream(Feature.class.getEnumConstants()).map(Enum::name).toArray(x$0 -> new String[x$0]); }



   public void compute(IndexDimension<Object> indexDimension, SeriesValues<Double> seriesValues) {
      if (indexDimension == null) {
         throw new ArgumentIsNullException("index dimension");
      }
      compute(seriesValues);
   }








   public void compute(SeriesValues<Double> seriesValues) {
      if (seriesValues == null) {
         throw new ArgumentIsNullException("series values");
      }
      compute(seriesValues.getValues(), seriesValues.getName());
   }

   private void compute(List<Double> values, String seriesName) {
      DescriptiveStatistics statistics = new DescriptiveStatistics();
      for (Double v : values) {
         if (this.skipInvalidValues && (v == null || !Double.isFinite(v.doubleValue()))) {
            continue;
         }
         statistics.addValue((v != null) ? v.doubleValue() : NaN);
      }
      this.calculatedFeatures = new LinkedHashMap();
      for (Feature feature : Feature.values()) {
         if (((Boolean)this.featuresToCalculate.get(feature)).booleanValue()) {
            this.calculatedFeatures.put(feature, Double.valueOf(computeSingleFeature(statistics, feature)));
         }
      }
   }

   private double computeSingleFeature(DescriptiveStatistics statistics, Feature feature) {
      switch (feature) {
         case SUM:
            return statistics.getSum();

         case MEAN:
            return statistics.getMean();

         case GEOMETRIC_MEAN:
            return statistics.getGeometricMean();

         case FIRST_QUARTILE:
            return statistics.getPercentile(25.0D);

         case MEDIAN:
            return statistics.getPercentile(50.0D);

         case THIRD_QUARTILE:
            return statistics.getPercentile(75.0D);

         case MIN:
            return statistics.getMin();

         case MAX:
            return statistics.getMax();

         case STD_DEVIATION:
            return statistics.getStandardDeviation();

         case KURTOSIS:
            return statistics.getKurtosis();

         case SKEWNESS:
            return statistics.getSkewness();
      }

      throw new IllegalArgumentException("Unknown feature: " + feature.toString());
   }



   public List<Pair<String, Double>> getComputedFeatures() {
      if (this.calculatedFeatures == null) {
         throw new WrongExecutionOrderException(getName() + " was not applied on a series.", new String[] { "compute(ValueSeries valueSeries)", "compute(TimeSeries timeSeries)" });
      }

      List<Pair<String, Double>> result = new ArrayList<Pair<String, Double>>();
      for (Feature feature : Feature.values()) {
         if (((Boolean)this.featuresToCalculate.get(feature)).booleanValue()) {
            if (!this.calculatedFeatures.containsKey(feature)) {
               throw new WrongExecutionOrderException("Calculation of " + feature
                       .toString() + " is enabled, but the feature was not yet calculated.", new String[] { "compute(ValueSeries valueSeries)", "compute(TimeSeries timeSeries)" });
            }

            result.add(new Pair(feature.toString(), this.calculatedFeatures.get(feature)));
         }
      }
      return result;
   }



   public void setSkipInvalidValues(boolean skipInvalidValues) { this.skipInvalidValues = skipInvalidValues; }








   public int getNumberOfComputedFeatures() {
      int number = 0;
      for (Feature feature : Feature.values()) {
         if (((Boolean)this.featuresToCalculate.get(feature)).booleanValue()) {
            number++;
         }
      }
      return number;
   }
}
