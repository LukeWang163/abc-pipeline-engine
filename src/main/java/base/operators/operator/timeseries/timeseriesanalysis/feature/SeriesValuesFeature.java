package base.operators.operator.timeseries.timeseriesanalysis.feature;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import org.apache.commons.math3.util.Pair;

import java.util.List;


public interface SeriesValuesFeature<I, V>
{
   String getName();

   String[] getFeatureNames();

   void compute(IndexDimension<I> paramIndexDimension, SeriesValues<V> paramSeriesValues);

   List<Pair<String, V>> getComputedFeatures();

   default List<Pair<String, V>> getComputedFeatures(IndexDimension<I> indexDimension, SeriesValues<V> seriesValues) {
      compute(indexDimension, seriesValues);
      return getComputedFeatures();
   }

   void setSkipInvalidValues(boolean paramBoolean);
}
