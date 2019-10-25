package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NotStrictlyMonotonicIncreasingException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IndexArgumentsDontMatchException;
import base.operators.operator.timeseries.timeseriesanalysis.tools.SeriesUtils;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NominalValues extends AbstractDimension<String> {
   private Integer[] nominalIndices;
   private final Map nominalToIndexMap;
   private final List indexToNominalMap;
   private final Locale locale;
   public static final String DEFAULT_NAME = "Nominal";

   public NominalValues(List values, boolean isIndexDimension) {
      super(isIndexDimension, SeriesBuilder.IndexType.NOMINAL, SeriesBuilder.ValuesType.NOMINAL);
      this.indexToNominalMap = new ArrayList();
      this.nominalToIndexMap = new LinkedHashMap();
      this.locale = Locale.US;
      this.setValues(values);
      if (isIndexDimension) {
         this.isValidIndexDimension(values);
      }

   }

   public NominalValues(List values, boolean isIndexDimension, String name, Locale locale) {
      super(isIndexDimension, SeriesBuilder.IndexType.NOMINAL, SeriesBuilder.ValuesType.NOMINAL, name);
      this.indexToNominalMap = new ArrayList();
      this.nominalToIndexMap = new LinkedHashMap();
      this.locale = locale;
      this.setValues(values);
      if (isIndexDimension) {
         this.isValidIndexDimension(values);
      }

   }

   public NominalValues(Integer[] nominalIndices, List indexToNominalMap, boolean isIndexDimension) {
      super(isIndexDimension, SeriesBuilder.IndexType.NOMINAL, SeriesBuilder.ValuesType.NOMINAL);
      if (indexToNominalMap == null) {
         throw new ArgumentIsNullException("index to value map");
      } else {
         this.nominalToIndexMap = new LinkedHashMap();

         for(int i = 0; i < indexToNominalMap.size(); ++i) {
            this.nominalToIndexMap.put(indexToNominalMap.get(i), i);
         }

         this.indexToNominalMap = new ArrayList(indexToNominalMap);
         this.locale = Locale.US;
         this.setNominalIndices(nominalIndices);
         if (isIndexDimension) {
            this.isValidIndexDimension(this.getValues());
         }

      }
   }

   public NominalValues(Integer[] nominalIndices, List indexToNominalMap, boolean isIndexDimension, String name, Locale locale) {
      super(isIndexDimension, SeriesBuilder.IndexType.NOMINAL, SeriesBuilder.ValuesType.NOMINAL, name);
      if (indexToNominalMap == null) {
         throw new ArgumentIsNullException("index to nominal map");
      } else {
         this.nominalToIndexMap = new LinkedHashMap();

         for(int i = 0; i < indexToNominalMap.size(); ++i) {
            this.nominalToIndexMap.put(indexToNominalMap.get(i), i);
         }

         this.indexToNominalMap = new ArrayList(indexToNominalMap);
         this.locale = locale;
         this.setNominalIndices(nominalIndices);
         if (isIndexDimension) {
            this.isValidIndexDimension(this.getValues());
         }

      }
   }

   public NominalValues(Integer[] nominalIndices, Map nominalToIndexMap, List indexToNominalMap, boolean isIndexDimension) {
      super(isIndexDimension, SeriesBuilder.IndexType.NOMINAL, SeriesBuilder.ValuesType.NOMINAL);
      this.nominalToIndexMap = new HashMap(nominalToIndexMap);
      this.indexToNominalMap = new ArrayList(indexToNominalMap);
      this.locale = Locale.US;
      this.setNominalIndices(nominalIndices);
      if (isIndexDimension) {
         this.isValidIndexDimension(this.getValues());
      }

   }

   public NominalValues(Integer[] nominalIndices, Map nominalToIndexMap, List indexToNominalMap, boolean isIndexDimension, String name, Locale locale) {
      super(isIndexDimension, SeriesBuilder.IndexType.NOMINAL, SeriesBuilder.ValuesType.NOMINAL, name);
      this.nominalToIndexMap = new HashMap(nominalToIndexMap);
      this.indexToNominalMap = new ArrayList(indexToNominalMap);
      this.locale = locale;
      this.setNominalIndices(nominalIndices);
      if (isIndexDimension) {
         this.isValidIndexDimension(this.getValues());
      }

   }

   public double getIndexValueAsDouble(int index) {
      return this.getValueAsDouble(index);
   }

   public double getValueAsDouble(int index) {
      if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (index >= this.nominalIndices.length) {
         throw new IndexArgumentsDontMatchException("index", "series", index, this.nominalIndices.length, IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else {
         return this.nominalIndices[index] == null ? Double.NaN : (double)this.nominalIndices[index];
      }
   }

   protected String getValueImpl(int index) {
      if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (index >= this.nominalIndices.length) {
         throw new IndexArgumentsDontMatchException("index", "series", index, this.nominalIndices.length, IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else {
         return this.mapIndex(this.nominalIndices[index]);
      }
   }

   protected List getValuesImpl() {
      List values = new ArrayList();

      for(int i = 0; i < this.nominalIndices.length; ++i) {
         values.add(this.getValue(i));
      }

      return values;
   }

   protected void setValueImpl(int index, String value) {
      if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (index >= this.nominalIndices.length) {
         throw new IndexArgumentsDontMatchException("index", "series", index, this.nominalIndices.length, IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else {
         this.nominalIndices[index] = this.mapString(value);
      }
   }

   protected void setValuesImpl(List values) {
      SeriesUtils.checkList(values, this.getDimensionTypeName() + " list");
      this.nominalIndices = new Integer[values.size()];

      for(int i = 0; i < values.size(); ++i) {
         this.nominalIndices[i] = this.mapString((String)values.get(i));
      }

   }

   public void isValidIndexDimension(List indexValues) {
      SeriesUtils.checkList(indexValues, this.getDimensionTypeName());
      Collator collator = Collator.getInstance(this.locale);

      for(int i = 0; i < indexValues.size() - 1; ++i) {
         if (this.isMissing((String)indexValues.get(i)) || this.isMissing((String)indexValues.get(i + 1))) {
            throw new SeriesContainsInvalidValuesException(this.getDimensionTypeName() + " list", SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
         }

         if (collator.compare((String)indexValues.get(i), (String)indexValues.get(i + 1)) >= 0) {
            throw new NotStrictlyMonotonicIncreasingException();
         }
      }

   }

   public void isValidIndexValue(int index, String indexValue) {
      if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (index >= this.getLength()) {
         throw new IndexArgumentsDontMatchException("index", "series", index, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else {
         Collator collator = Collator.getInstance(this.locale);
         if (this.isMissing(indexValue)) {
            throw new IllegalArgumentException("Provided index value is NaN.");
         } else if (index > 0 && collator.compare(this.mapIndex(index - 1), indexValue) >= 0) {
            throw new NotStrictlyMonotonicIncreasingException();
         } else if (index < this.getLength() - 1 && collator.compare(indexValue, this.mapIndex(index + 1)) >= 0) {
            throw new NotStrictlyMonotonicIncreasingException();
         }
      }
   }

   private void setNominalIndices(Integer[] nominalIndices) {
      if (nominalIndices == null) {
         throw new ArgumentIsNullException("nominal indices array");
      } else {
         Integer[] var2 = nominalIndices;
         int var3 = nominalIndices.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Integer index = var2[var4];
            if (index != null && index >= this.indexToNominalMap.size()) {
               throw new IllegalArgumentException("nominal indices arrays contains index values which are not in the index to nominal map.");
            }
         }

         this.nominalIndices = (Integer[])nominalIndices.clone();
      }
   }

   public Integer[] getNominalIndices() {
      return (Integer[])this.nominalIndices.clone();
   }

   public SeriesValues getSubSeriesValues(int lowerArrayIndex, int upperArrayIndex) {
      if (upperArrayIndex > this.getLength()) {
         throw new IndexArgumentsDontMatchException("upperArrayIndex", "series", upperArrayIndex, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else {
         return new NominalValues((Integer[])Arrays.copyOfRange(this.nominalIndices, lowerArrayIndex, upperArrayIndex), this.getNominalToIndexMap(), this.getIndexToNominalMap(), this.isIndexDimension, this.getName(), this.locale);
      }
   }

   public IndexDimension getSubIndexDimension(int lowerArrayIndex, int upperArrayIndex) {
      if (upperArrayIndex > this.getLength()) {
         throw new IndexArgumentsDontMatchException("upperArrayIndex", "series", upperArrayIndex, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else {
         return new NominalValues((Integer[])Arrays.copyOfRange(this.nominalIndices, lowerArrayIndex, upperArrayIndex), this.getNominalToIndexMap(), this.getIndexToNominalMap(), this.isIndexDimension, this.getName(), this.locale);
      }
   }

   public boolean equalSeriesValues(SeriesValues seriesValues) {
      if (!(seriesValues instanceof NominalValues)) {
         return false;
      } else {
         NominalValues nominalValues = (NominalValues)seriesValues;
         return nominalValues.getLength() == this.getLength() && nominalValues.getIndexToNominalMap().equals(this.indexToNominalMap) && nominalValues.getNominalToIndexMap().equals(this.nominalToIndexMap) && nominalValues.getValues().equals(this.getValues());
      }
   }

   public boolean equalIndexDimension(IndexDimension indexDimension) {
      if (!(indexDimension instanceof NominalValues)) {
         return false;
      } else {
         NominalValues nominalValues = (NominalValues)indexDimension;
         return nominalValues.getLength() == this.getLength() && nominalValues.getIndexToNominalMap().equals(this.indexToNominalMap) && nominalValues.getNominalToIndexMap().equals(this.nominalToIndexMap) && nominalValues.getValues().equals(this.getValues());
      }
   }

   private Integer mapString(String str) {
      if (str == null) {
         return null;
      } else {
         Integer index = this.getIndex(str);
         if (index < 0) {
            this.indexToNominalMap.add(str);
            index = this.indexToNominalMap.size() - 1;
            this.nominalToIndexMap.put(str, index);
         }

         return index;
      }
   }

   private String mapIndex(Integer index) {
      if (index == null) {
         return null;
      } else if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (index >= this.indexToNominalMap.size()) {
         throw new IllegalArgumentException("Provided index is larger than the size of the indexToValueMap.");
      } else {
         return (String)this.indexToNominalMap.get(index);
      }
   }

   private int getIndex(String str) {
      Integer index = (Integer)this.nominalToIndexMap.get(str);
      return index == null ? -1 : index;
   }

   public int getLength() {
      return this.nominalIndices.length;
   }

   public Map getNominalToIndexMap() {
      return new LinkedHashMap(this.nominalToIndexMap);
   }

   public List getIndexToNominalMap() {
      return new ArrayList(this.indexToNominalMap);
   }

   public Map getIndexToNominalMapAsHashMap() {
      Map resultMap = new LinkedHashMap();

      for(int i = 0; i < this.indexToNominalMap.size(); ++i) {
         resultMap.put(i, this.indexToNominalMap.get(i));
      }

      return resultMap;
   }

   public int getLowerArrayIndex(String indexValue) {
      Collator collator = Collator.getInstance(this.locale);
      if (collator.compare(indexValue, (String)this.getValue(0)) < 0) {
         return Integer.MIN_VALUE;
      } else if (collator.compare(indexValue, (String)this.getValue(this.getLength() - 1)) > 0) {
         return this.getLength() - 1;
      } else {
         int index = Arrays.binarySearch(this.getValues().toArray(), indexValue, collator);
         return index < 0 ? -(index + 2) : index;
      }
   }

   public int getUpperArrayIndex(String indexValue) {
      Collator collator = Collator.getInstance(this.locale);
      if (collator.compare(indexValue, (String)this.getValue(0)) < 0) {
         return 0;
      } else if (collator.compare(indexValue, (String)this.getValue(this.getLength() - 1)) > 0) {
         return Integer.MAX_VALUE;
      } else {
         int index = Arrays.binarySearch(this.getValues().toArray(), indexValue, collator);
         return index < 0 ? -(index + 1) : index;
      }
   }

   public boolean hasFixLength() {
      return true;
   }

   public boolean isMissing(String value) {
      return value == null;
   }

   public double convertValueToDoubleValue(String value) {
      return this.isMissing(value) ? Double.NaN : (double)this.mapString(value);
   }

   public String convertDoubleValueToValue(double doubleValue) {
      if (Double.isNaN(doubleValue)) {
         return null;
      } else if (Double.isInfinite(doubleValue)) {
         throw new IllegalArgumentException("Provided doubleValue is infinite. Cannot convert it to String.");
      } else if (Math.floor(doubleValue) != doubleValue) {
         throw new IllegalArgumentException("Provided doubleValue is not a whole number and cannot be a nominal index. Cannot convert it to String.");
      } else {
         return this.mapIndex((int)doubleValue);
      }
   }

   public boolean hasEmptyValues() {
      int emptyKeyIndex = this.getIndex("");
      if (emptyKeyIndex == -1) {
         return false;
      } else {
         Integer[] var2 = this.nominalIndices;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            int nominalIndex = var2[var4];
            if (nominalIndex == emptyKeyIndex) {
               return true;
            }
         }

         return false;
      }
   }

   protected String getDefaultName() {
      return "Nominal";
   }

   public NominalValues copy() {
      return new NominalValues((Integer[])this.nominalIndices.clone(), new LinkedHashMap(this.nominalToIndexMap), new ArrayList(this.indexToNominalMap), this.isIndexDimension, this.getName(), this.locale);
   }
}
