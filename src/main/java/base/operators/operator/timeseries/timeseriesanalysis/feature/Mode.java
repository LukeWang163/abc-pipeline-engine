package base.operators.operator.timeseries.timeseriesanalysis.feature;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.WrongExecutionOrderException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.math3.util.Pair;


public class Mode<V>
        extends Object
        implements SeriesValuesFeature<Object, V>
{
   public static final String FEATURE_NAME = "mode";
   private int maxModeOrder;
   private int maxK;
   private MultiModalMode multiModalMode;
   private List<Map.Entry<V, Integer>> modeList;
   private boolean skipInvalidValues;
   private Random random;

   public static class ModeBuilder<T>
           extends AbstractFeaturesBuilder<Object, T>
   {
      public FeaturesBuilder<Object, T> reset() {
         this.seriesValuesFeature = new Mode();
         return this;
      }










      public ModeBuilder<T> random(Random random) {
         ((Mode)this.seriesValuesFeature).setRandom(random);
         return this;
      }








      public ModeBuilder<T> maxModeOrder(int maxModeOrder) {
         ((Mode)this.seriesValuesFeature).setMaxModeOrder(maxModeOrder);
         return this;
      }










      public ModeBuilder<T> maxK(int maxK) {
         ((Mode)this.seriesValuesFeature).setMaxK(maxK);
         return this;
      }








      public ModeBuilder<T> multiModalMode(Mode.MultiModalMode multiModalMode) {
         ((Mode)this.seriesValuesFeature).setMultiModalMode(multiModalMode);
         return this;
      }



      public ModeBuilder<T> skipInvalidValues() { return (ModeBuilder)super.skipInvalidValues(); }
   }

















   public enum MultiModalMode
   {
      FIRST_K_OCCURENCE, RANDOM_K, ALL;



      public String toString() { return name().toLowerCase().replace("_", " "); }
   }

   protected Mode() {
      this.maxModeOrder = 1;
      this.maxK = 1;
      this.multiModalMode = MultiModalMode.FIRST_K_OCCURENCE;

      this.modeList = null;

      this.skipInvalidValues = false;





      this.random = null;


      this.random = new Random();
   }



   public String getName() { return "mode"; }



   public String[] getFeatureNames() {
      List<String> resultList = new ArrayList<String>();
      for (int order = 1; order <= this.maxModeOrder; order++) {
         String featureName = getName() + "_order_" + order;


         int currentK = (this.multiModalMode == MultiModalMode.ALL) ? 3 : this.maxK;
         if (currentK == 1) {
            resultList.add(featureName);
         } else {
            for (int i = 0; i < currentK; i++) {
               resultList.add(featureName + "_k_" + i);
            }
         }
      }
      return (String[])resultList.toArray(new String[0]);
   }


   public void compute(IndexDimension<Object> indexDimension, SeriesValues<V> seriesValues) {
      if (indexDimension == null) {
         throw new ArgumentIsNullException("index dimension");
      }
      compute(seriesValues);
   }

   public void compute(SeriesValues<V> seriesValues) {
      if (seriesValues == null) {
         throw new ArgumentIsNullException("series values");
      }
      Map<V, Integer> map = new LinkedHashMap<V, Integer>();
      for (V value : seriesValues.getValues()) {
         if (this.skipInvalidValues && isInfinite(value)) {
            continue;
         }
         Integer counter = Integer.valueOf(1);
         if (map.containsKey(value)) {
            counter = Integer.valueOf(counter.intValue() + ((Integer)map.get(value)).intValue());
         }
         map.put(value, counter);
      }
      this.modeList = new ArrayList(map.entrySet());
      this.modeList.sort(Map.Entry.comparingByValue());
      Collections.reverse(this.modeList);
   }


   public List<Pair<String, V>> getComputedFeatures() {
      if (this.modeList == null) {
         throw new WrongExecutionOrderException(getName() + " was not applied on seriesValues.", new String[] { "compute(IndexDimension indexDimension, SeriesValues seriesValues)" });
      }

      List<Pair<String, V>> result = new ArrayList<Pair<String, V>>();
      if (this.modeList.isEmpty()) {



         result.add(new Pair(getName() + "_order_1", null));
         return result;
      }
      Iterator<Map.Entry<V, Integer>> entriesIterator = this.modeList.iterator();
      Map.Entry<V, Integer> currentEntry = (Map.Entry)entriesIterator.next();
      boolean entriesLeft = true;

      for (int order = 1; order <= this.maxModeOrder && entriesLeft; order++) {
         String featureName = getName() + "_order_" + order;

         List<Map.Entry<V, Integer>> subList = new ArrayList<Map.Entry<V, Integer>>();
         subList.add(currentEntry);
         boolean checkEntries = true;
         while (entriesIterator.hasNext() && checkEntries) {
            Map.Entry<V, Integer> nextEntry = (Map.Entry)entriesIterator.next();
            if (currentEntry.getValue() == nextEntry.getValue()) {
               subList.add(nextEntry);
            } else {
               checkEntries = false;
            }
            currentEntry = nextEntry;
         }


         Collections.reverse(subList);
         entriesLeft = (entriesIterator.hasNext() || !checkEntries);


         if (this.multiModalMode != MultiModalMode.ALL) {
            int numberToRemove = Math.max(0, subList.size() - this.maxK);
            for (int i = 0; i < numberToRemove; i++) {
               if (this.multiModalMode == MultiModalMode.RANDOM_K) {
                  subList.remove(this.random.nextInt(subList.size()));
               } else if (this.multiModalMode == MultiModalMode.FIRST_K_OCCURENCE) {
                  subList.remove(subList.size() - 1);
               }
            }
         }
         if (subList.size() == 1) {


            result.add(new Pair(featureName, ((Map.Entry)subList.get(0)).getKey()));
         }
         else {

            int i = 0;
            for (Map.Entry<V, Integer> entry : subList) {
               result.add(new Pair(featureName + "_k_" + i, entry.getKey()));
               i++;
            }
         }
      }
      return result;
   }

   private boolean isInfinite(V value) {
      if (value == null) {
         return true;
      }
      if (value instanceof Double && !Double.isFinite(((Double)value).doubleValue())) {
         return true;
      }
      if (value instanceof String && ((String)value).isEmpty()) {
         return true;
      }
      return false;
   }









   public void setRandom(Random random) {
      if (random == null) {
         throw new ArgumentIsNullException("random number generator");
      }
      this.random = random;
   }







   public void setMaxModeOrder(int maxModeOrder) {
      if (maxModeOrder < 1) {
         throw new IllegalIndexArgumentException("maxModeOrder", Integer.valueOf(maxModeOrder), IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      }
      this.maxModeOrder = maxModeOrder;
   }









   public void setMaxK(int maxK) {
      if (maxK < 1) {
         throw new IllegalIndexArgumentException("max k", Integer.valueOf(maxK), IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      }
      this.maxK = maxK;
   }








   public void setMultiModalMode(MultiModalMode multiModalMode) { this.multiModalMode = multiModalMode; }














   public void setSkipInvalidValues(boolean skipInvalidValues) { this.skipInvalidValues = skipInvalidValues; }
}
