package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IllegalSeriesLengthException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NameAlreadyInMultivariateSeriesException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.DefaultInteger;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.NominalValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.RealValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.TimeValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsEmptyException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.WrongExecutionOrderException;
import base.operators.operator.timeseries.timeseriesanalysis.tools.SeriesUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SeriesBuilder<T extends ISeries<?, ?>>
{
   private IndexType indexType;
   private String indexName;
   private List<?> indexValues;
   private Integer[] indexNominalIndices;
   private List<String> indexIndexToNominalMap;
   private Locale locale;
   private ValuesType valuesType;
   private List<SeriesValues<?>> seriesValuesList;
   private List<String> seriesValuesNames;
   private int seriesCounter;
   private int length;
   private final boolean strict;

   public enum IndexType
   {
      DEFAULT, REAL, TIME, NOMINAL;
   }









   public enum ValuesType
   {
      REAL, TIME, NOMINAL, MIXED;
   }


   protected SeriesBuilder() { this(false); } protected SeriesBuilder(boolean strict) { this.indexType = null; this.indexName = null; this.indexValues = null; this.indexNominalIndices = null; this.indexIndexToNominalMap = null; this.locale = Locale.US; this.valuesType = null; this.seriesValuesList = new ArrayList();
   this.seriesValuesNames = new ArrayList();
   this.seriesCounter = -1;
   this.length = 0;
   this.strict = strict;
   reset(); }







   public void reset() {
      this.indexType = null;
      this.indexName = null;
      this.indexValues = null;
      this.indexNominalIndices = null;
      this.indexIndexToNominalMap = null;

      this.valuesType = null;
      this.seriesValuesList = new ArrayList();
      this.seriesValuesNames = new ArrayList();

      this.seriesCounter = -1;
      this.length = 0;
   }









   public SeriesBuilder<T> changeLocaleForNominalValues(Locale locale) {
      this.locale = locale;
      return this;
   }










   public SeriesBuilder<T> indexName(String name) {
      if (name.isEmpty()) {
         throw new ArgumentIsEmptyException("index name");
      }
      this.indexName = name;
      return this;
   }










   public SeriesBuilder<T> defaultIndex() {
      setIndexType(IndexType.DEFAULT);
      return this;
   }















   public SeriesBuilder<T> realIndex(List<Double> values) {
      indexValues(values, IndexType.REAL);
      return this;
   }















   public SeriesBuilder<T> timeIndex(List<Instant> values) {
      indexValues(values, IndexType.TIME);
      return this;
   }

















   public SeriesBuilder<T> nominalIndex(List<String> values) {
      indexValues(values, IndexType.NOMINAL);
      return this;
   }



















   public SeriesBuilder<T> nominalIndexValues(Integer[] nominalIndices, List<String> indexToNominalMap) {
      checkNominalArguments(nominalIndices, indexToNominalMap, "index values");
      checkLength(nominalIndices.length, "index nominal indices");
      setIndexType(IndexType.NOMINAL);
      this.indexNominalIndices = nominalIndices;
      this.indexIndexToNominalMap = indexToNominalMap;
      return this;
   }














   public SeriesBuilder<T> indexDimension(IndexDimension<?> indexDimension) {
      if (indexDimension == null) {
         throw new ArgumentIsNullException("index dimension");
      }
      this.indexName = indexDimension.getName();
      if (indexDimension.getIndexType() == IndexType.DEFAULT) {
         return defaultIndex();
      }
      if (indexDimension.getIndexType() == IndexType.REAL) {
         return realIndex(((RealValues)indexDimension).getIndexValues());
      }
      if (indexDimension.getIndexType() == IndexType.TIME) {
         return timeIndex(((TimeValues)indexDimension).getIndexValues());
      }
      if (indexDimension.getIndexType() == IndexType.NOMINAL) {
         NominalValues nominalValues = (NominalValues)indexDimension;
         return nominalIndexValues(nominalValues.getNominalIndices(), nominalValues.getIndexToNominalMap());
      }
      throw new IllegalArgumentException("Index type of provided indexDimension is not supported: " + indexDimension
              .getIndexType().toString());
   }
















   public SeriesBuilder<T> addRealValues(List<Double> values) { return addRealValues(values, null); }

















   public SeriesBuilder<T> addRealValues(List<Double> values, String name) {
      name = checkValuesListAndName(values, ValuesType.REAL, name);
      this.seriesValuesList.add(new RealValues(values, false, name));
      this.seriesValuesNames.add(name);
      return this;
   }
















   public SeriesBuilder<T> addTimeValues(List<Instant> values) { return addTimeValues(values, null); }















   public SeriesBuilder<T> addTimeValues(List<Instant> values, String name) {
      name = checkValuesListAndName(values, ValuesType.TIME, name);
      this.seriesValuesList.add(new TimeValues(values, false, name));
      this.seriesValuesNames.add(name);
      return this;
   }

















   public SeriesBuilder<T> addNominalValues(List<String> values) { return addNominalValues(values, null); }
















   public SeriesBuilder<T> addNominalValues(List<String> values, String name) {
      name = checkValuesListAndName(values, ValuesType.NOMINAL, name);
      this.seriesValuesList.add(new NominalValues(values, false, name, this.locale));
      this.seriesValuesNames.add(name);
      return this;
   }
























   public SeriesBuilder<T> addNominalValues(Integer[] nominalIndices, List<String> indexToNominalMap) { return addNominalValues(nominalIndices, indexToNominalMap, null); }

























   public SeriesBuilder<T> addNominalValues(Integer[] nominalIndices, List<String> indexToNominalMap, String name) {
      checkNominalArguments(nominalIndices, indexToNominalMap, "series values");
      checkLength(nominalIndices.length, "series values nominal indices");
      addValuesType(ValuesType.NOMINAL);
      name = checkCreateValuesName(name, ValuesType.NOMINAL);
      this.seriesValuesList.add(new NominalValues(nominalIndices, indexToNominalMap, false, name, this.locale));
      this.seriesValuesNames.add(name);
      return this;
   }
















   public SeriesBuilder<T> addSeriesValues(SeriesValues<?> seriesValues) {
      if (seriesValues == null) {
         throw new ArgumentIsNullException("series values");
      }
      checkLength(seriesValues.getLength(), "series values");
      addValuesType(seriesValues.getValuesType());
      String name = checkCreateValuesName(seriesValues.getName(), seriesValues.getValuesType());
      this.seriesValuesList.add(seriesValues);
      this.seriesValuesNames.add(name);
      return this;
   }













   public T build() {
      validateIndexSettings();
      validateSeriesValuesSettings();
      if (!SeriesFactory.checkSupportedTypes(this.indexType, this.valuesType)) {
         throw new IllegalArgumentException("The combination of index type (" + this.indexType.toString() + ") and valuesType (" + this.valuesType
                 .toString() + ") is not supported.");
      }
      return (T)SeriesFactory.create(createIndexDimension(), createSeriesValuesList(), this.indexType, this.valuesType);
   }

   private void validateIndexSettings() {
      if (this.length == 0)
      {

         throw new WrongExecutionOrderException("No values added to the SeriesBuilder", new String[] { "addRealValues()", "addTimeValues()", "addNominalValues()" });
      }

      if (this.indexType == null)
      {
         this.indexType = IndexType.DEFAULT;
      }
      if (this.indexName == null)
      {
         this.indexName = this.indexType.toString() + " index";
      }
   }

   private void validateSeriesValuesSettings() {
      if (this.seriesValuesList.isEmpty()) {
         throw new WrongExecutionOrderException("No values added to the SeriesBuilder", new String[] { "addRealValues()", "addTimeValues()", "addNominalValues()" });
      }


      if (this.valuesType == ValuesType.TIME) {
         this.valuesType = ValuesType.MIXED;
      }
   }



   private IndexDimension createIndexDimension() {
      switch (this.indexType) {
         case DEFAULT:
            return new DefaultInteger(this.length, true, this.indexName);

         case REAL:
            return new RealValues(this.indexValues, true, this.indexName);

         case TIME:
            return new TimeValues(this.indexValues, true, this.indexName);

         case NOMINAL:
            if (this.indexValues != null)
               return new NominalValues(this.indexValues, true, this.indexName, this.locale);
            if (this.indexNominalIndices != null && this.indexIndexToNominalMap != null) {
               return new NominalValues(this.indexNominalIndices, this.indexIndexToNominalMap, true, this.indexName, this.locale);
            }

            throw new IllegalArgumentException("index Type was set to " + IndexType.NOMINAL.toString() + " but neither indexValues nor nominalIndices and a indexToNominalMap were provided");
      }


      throw new IllegalArgumentException("Wrong index type. Could not create IndexDimension.");
   }


//不确定
   private <V>List<SeriesValues<V>> createSeriesValuesList() {
      List<SeriesValues<V>> resultList = new ArrayList<SeriesValues<V>>();
      for (SeriesValues<?> seriesValues : this.seriesValuesList) {

            resultList.add((SeriesValues<V>) seriesValues);

      }
      return resultList;
   }

   private void indexValues(List<?> values, IndexType type) {
      SeriesUtils.checkList(values, "index values");
      checkLength(values.size(), "index values");
      setIndexType(type);
      this.indexValues = values;
   }

   private void checkLength(int size, String objectName) {
      if (this.length != 0 && size != this.length)
         throw new IllegalSeriesLengthException(objectName, "series", size, this.length);
      if (this.length == 0) {
         this.length = size;
      }
   }

   private void checkNominalArguments(Integer[] nominalIndices, List<String> indexToNominalMap, String objectName) {
      if (nominalIndices == null) {
         throw new ArgumentIsNullException(objectName);
      }
      if (indexToNominalMap == null) {
         throw new ArgumentIsNullException(objectName + " map");
      }
      if (nominalIndices.length == 0) {
         throw new ArgumentIsEmptyException(objectName);
      }
   }

   private String checkValuesListAndName(List<?> values, ValuesType type, String name) {
      SeriesUtils.checkList(values, "series values");
      checkLength(values.size(), "series values");
      addValuesType(type);
      return checkCreateValuesName(name, type);
   }

   private String checkCreateValuesName(String name, ValuesType type) {
      if (name == null)
      { name = getDefaultName(type); }
      else { if (name.isEmpty())
         throw new ArgumentIsEmptyException("series name");
         if (this.seriesValuesNames.contains(name))
            throw new NameAlreadyInMultivariateSeriesException(name);  }

      return name;
   }


















   private String getDefaultName(ValuesType currentType) {
      if (this.seriesCounter == Integer.MAX_VALUE) {
         throw new IllegalArgumentException("seriesCounter for defining default name reached Integer.MAX_VALUE.");
      }
      this.seriesCounter++;

      String name = currentType + " " + "series values" + " " + this.seriesCounter;
      if (this.seriesValuesNames.contains(name)) {
         return getDefaultName(currentType);
      }
      return name;
   }

   protected SeriesBuilder<T> setIndexType(IndexType type) {
      if (this.indexType == null) {
         this.indexType = type;
      } else if (this.indexType != type) {
         throw new IllegalArgumentException("Index type was already set to " + this.indexType.toString());
      }
      return this;
   }

   protected SeriesBuilder<T> addValuesType(ValuesType type) {
      if (this.valuesType == null) {
         this.valuesType = type;
      }
      if (this.valuesType != ValuesType.MIXED && this.valuesType != type) {
         if (this.strict) {
            throw new IllegalArgumentException("ValuesType (" + type
                    .toString() + ") not supported. SeriesBuilder was created to support only " + this.valuesType
                    .toString() + " valuesType.");
         }
         this.valuesType = ValuesType.MIXED;
      }
      return this;
   }
}
