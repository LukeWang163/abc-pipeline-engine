package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.tools.SeriesUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class SeriesFactory
{
   private static final Map<Pair<SeriesBuilder.IndexType, SeriesBuilder.ValuesType>, SeriesCreator<?, ?>> creatorMap = new LinkedHashMap();



   static  {
      register(SeriesBuilder.IndexType.DEFAULT, SeriesBuilder.ValuesType.REAL, new SeriesCreator<Integer, Double>()
      {

         public ISeries<Integer, Double> create(IndexDimension<Integer> indexDimension, List<SeriesValues<Double>> seriesValuesList)
         {
            return new DefaultIndexRealValuesSeries(indexDimension, seriesValuesList);
         }
      });
      register(SeriesBuilder.IndexType.DEFAULT, SeriesBuilder.ValuesType.NOMINAL, new SeriesCreator<Integer, String>()
      {

         public ISeries<Integer, String> create(IndexDimension<Integer> indexDimension, List<SeriesValues<String>> seriesValuesList)
         {
            return new DefaultIndexNominalValuesSeries(indexDimension, seriesValuesList);
         }
      });
      register(SeriesBuilder.IndexType.DEFAULT, SeriesBuilder.ValuesType.MIXED, new SeriesCreator<Integer, Object>()
      {

         public ISeries<Integer, Object> create(IndexDimension<Integer> indexDimension, List<SeriesValues<Object>> seriesValuesList)
         {
            return new DefaultIndexMixedValuesSeries(indexDimension, seriesValuesList);
         }
      });
      register(SeriesBuilder.IndexType.REAL, SeriesBuilder.ValuesType.REAL, new SeriesCreator<Double, Double>()
      {

         public ISeries<Double, Double> create(IndexDimension<Double> indexDimension, List<SeriesValues<Double>> seriesValuesList)
         {
            return new RealIndexRealValuesSeries(indexDimension, seriesValuesList);
         }
      });
      register(SeriesBuilder.IndexType.REAL, SeriesBuilder.ValuesType.NOMINAL, new SeriesCreator<Double, String>()
      {

         public ISeries<Double, String> create(IndexDimension<Double> indexDimension, List<SeriesValues<String>> seriesValuesList)
         {
            return new RealIndexNominalValuesSeries(indexDimension, seriesValuesList);
         }
      });
      register(SeriesBuilder.IndexType.REAL, SeriesBuilder.ValuesType.MIXED, new SeriesCreator<Double, Object>()
      {

         public ISeries<Double, Object> create(IndexDimension<Double> indexDimension, List<SeriesValues<Object>> seriesValuesList)
         {
            return new RealIndexMixedValuesSeries(indexDimension, seriesValuesList);
         }
      });
      register(SeriesBuilder.IndexType.TIME, SeriesBuilder.ValuesType.REAL, new SeriesCreator<Instant, Double>()
      {

         public ISeries<Instant, Double> create(IndexDimension<Instant> indexDimension, List<SeriesValues<Double>> seriesValuesList)
         {
            return new TimeIndexRealValuesSeries(indexDimension, seriesValuesList);
         }
      });
      register(SeriesBuilder.IndexType.TIME, SeriesBuilder.ValuesType.NOMINAL, new SeriesCreator<Instant, String>()
      {

         public ISeries<Instant, String> create(IndexDimension<Instant> indexDimension, List<SeriesValues<String>> seriesValuesList)
         {
            return new TimeIndexNominalValuesSeries(indexDimension, seriesValuesList);
         }
      });
      register(SeriesBuilder.IndexType.TIME, SeriesBuilder.ValuesType.MIXED, new SeriesCreator<Instant, Object>()
      {

         public ISeries<Instant, Object> create(IndexDimension<Instant> indexDimension, List<SeriesValues<Object>> seriesValuesList)
         {
            return new TimeIndexMixedValuesSeries(indexDimension, seriesValuesList);
         }
      });
   }






























   protected static <I, V> ISeries<I, V> create(IndexDimension<I> indexDimension, List<SeriesValues<V>> seriesValuesList, SeriesBuilder.IndexType indexType, SeriesBuilder.ValuesType valuesType) {
      if (indexDimension == null) {
         throw new ArgumentIsNullException("index dimension");
      }
      SeriesUtils.checkList(seriesValuesList, "series values list");
      SeriesCreator<I, V> creator = (SeriesCreator)creatorMap.get(Pair.of(indexType, valuesType));
      if (creator == null) {
         throw new IllegalArgumentException("The combination of index type (" + indexType.toString() + ") and valuesType (" + valuesType
                 .toString() + ") is not supported.");
      }
      return creator.create(indexDimension, seriesValuesList);
   }


   private static <I, V> void register(SeriesBuilder.IndexType indexType, SeriesBuilder.ValuesType valuesType, SeriesCreator<I, V> creator) { creatorMap.put(Pair.of(indexType, valuesType), creator); }



































   public static boolean checkSupportedTypes(SeriesBuilder.IndexType indexType, SeriesBuilder.ValuesType valuesType) { return creatorMap.containsKey(Pair.of(indexType, valuesType)); }

   protected static interface SeriesCreator<I, V> {
      ISeries<I, V> create(IndexDimension<I> param1IndexDimension, List<SeriesValues<V>> param1List);
   }
}
