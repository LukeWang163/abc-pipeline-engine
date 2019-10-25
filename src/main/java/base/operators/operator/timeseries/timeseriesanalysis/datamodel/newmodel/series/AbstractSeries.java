package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IllegalSeriesLengthException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IndicesNotEqualException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NameAlreadyInMultivariateSeriesException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NameNotInMultivariateSeriesException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IndexArgumentsDontMatchException;
import base.operators.operator.timeseries.timeseriesanalysis.tools.SeriesUtils;
import java.util.ArrayList;
import java.util.List;


public abstract class AbstractSeries<I, V>
        extends Object
        implements ISeries<I, V>
{
   protected IndexDimension<I> indexDimension;
   protected List<SeriesValues<V>> seriesValuesList;
   protected int length;
   public static final String SERIES_INDEX_DESCRIPTOR = "index dimension";
   public static final String INDEX_VALUES_DESCRIPTOR = "index values";
   public static final String VALUES_DESCRIPTOR = "series values";
   public static final String SERIES_VALUES_LIST_DESCRIPTOR = "series values list";
   public static final String LENGTH_OF_SERIES_DESCRIPTOR = "series";
   public static final String NUMBER_OF_SERIES_DESCRIPTOR = "number of series";

   protected AbstractSeries(IndexDimension<I> indexDimension, List<SeriesValues<V>> seriesValuesList) {
      if (indexDimension == null) {
         throw new ArgumentIsNullException("index dimension");
      }
      SeriesUtils.checkList(seriesValuesList, "series values list");
      this.length = ((SeriesValues)seriesValuesList.get(0)).getLength();
      if (indexDimension.hasFixLength() && indexDimension.getLength() != getLength()) {
         throw new IllegalSeriesLengthException("index dimension", "series values", indexDimension.getLength(),
                 getLength());
      }
      List<String> seriesNames = new ArrayList<String>();
      for (SeriesValues<V> seriesValues : seriesValuesList) {
         if (seriesValues.getLength() != getLength()) {
            throw new IllegalSeriesLengthException("series values list", "to each other", seriesValues
                    .getLength(), getLength());
         }
         if (seriesNames.contains(seriesValues.getName())) {
            throw new NameAlreadyInMultivariateSeriesException(seriesValues.getName());
         }
         seriesNames.add(seriesValues.getName());
      }
      this.indexDimension = indexDimension;
      this.seriesValuesList = seriesValuesList;
   }


   public AbstractSeries<I, V> getSubSeries(int lowerArrayIndex, int upperArrayIndex) {
      if (lowerArrayIndex < 0) {
         throw new IllegalIndexArgumentException("lowerArrayIndex", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      }
      if (upperArrayIndex < 0) {
         throw new IllegalIndexArgumentException("upperArrayIndex", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      }
      if (upperArrayIndex < lowerArrayIndex) {
         throw new IndexArgumentsDontMatchException("upperArrayIndex", "lowerArrayIndex", Integer.valueOf(upperArrayIndex),
                 Integer.valueOf(lowerArrayIndex), IndexArgumentsDontMatchException.MisMatchType.SMALLER);
      }
      if (upperArrayIndex > getLength()) {
         throw new IndexArgumentsDontMatchException("upperArrayIndex", "series", Integer.valueOf(upperArrayIndex),
                 Integer.valueOf(getLength()), IndexArgumentsDontMatchException.MisMatchType.LARGER);
      }
      if (lowerArrayIndex == upperArrayIndex) {
         return null;
      }
      return getSubSeriesImplemented(lowerArrayIndex, upperArrayIndex);
   }



















   public int getLowerArrayIndex(I indexValue) {
      if (indexValue == null) {
         throw new ArgumentIsNullException("index value");
      }
      return this.indexDimension.getLowerArrayIndex(indexValue);
   }


   public int getUpperArrayIndex(I indexValue) {
      if (indexValue == null) {
         throw new ArgumentIsNullException("index value");
      }
      return this.indexDimension.getUpperArrayIndex(indexValue);
   }



   public I getIndexValue(int index) { return (I)this.indexDimension.getIndexValue(index); }




   public List<I> getIndexValues() { return this.indexDimension.getIndexValues(); }




   public IndexDimension<I> getIndexDimension() { return this.indexDimension.copy(); }




   public SeriesBuilder.IndexType getIndexType() { return this.indexDimension.getIndexType(); }



   public SeriesBuilder.ValuesType getValuesType() {
      SeriesBuilder.ValuesType type = ((SeriesValues)this.seriesValuesList.get(0)).getValuesType();
      for (int seriesIndex = 1; seriesIndex < getNumberOfSeries(); seriesIndex++) {
         if (!type.equals(((SeriesValues)this.seriesValuesList.get(seriesIndex)).getValuesType())) {
            return SeriesBuilder.ValuesType.MIXED;
         }
      }
      return type;
   }



   public void setIndexValue(int index, I indexValue) { this.indexDimension.setIndexValue(index, indexValue); }



   public void setIndexValues(List<I> indexValues) {
      if (this.indexDimension.hasFixLength() && this.length != indexValues.size()) {
         throw new IllegalSeriesLengthException("index values", "series", indexValues.size(), this.length);
      }

      this.indexDimension.setIndexValues(indexValues);
   }


   public V getValue(int seriesIndex, int index) {
      checkSeriesIndex(seriesIndex);
      return (V)((SeriesValues)this.seriesValuesList.get(seriesIndex)).getValue(index);
   }


   public List<V> getValues(int seriesIndex) {
      checkSeriesIndex(seriesIndex);
      return ((SeriesValues)this.seriesValuesList.get(seriesIndex)).getValues();
   }


   public V getValue(String seriesName, int index) {
      int seriesIndex = getSeriesIndex(seriesName);
      if (seriesIndex == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      }
      return (V)getValue(seriesIndex, index);
   }


   public List<V> getValues(String seriesName) {
      int seriesIndex = getSeriesIndex(seriesName);
      if (seriesIndex == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      }
      return getValues(seriesIndex);
   }


   public SeriesValues<V> getSeriesValues(String seriesName) {
      int seriesIndex = getSeriesIndex(seriesName);
      if (seriesIndex == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      }
      return getSeriesValues(seriesIndex);
   }


   public SeriesValues<V> getSeriesValues(int seriesIndex) {
      checkSeriesIndex(seriesIndex);
      return ((SeriesValues)this.seriesValuesList.get(seriesIndex)).copy();
   }


   public List<SeriesValues<V>> getSeriesValuesList() {
      List<SeriesValues<V>> list = new ArrayList<SeriesValues<V>>();
      for (SeriesValues<V> seriesValues : this.seriesValuesList) {
         list.add(seriesValues.copy());
      }
      return list;
   }


   public void setValue(int seriesIndex, int index, V value) {
      checkSeriesIndex(seriesIndex);
      ((SeriesValues)this.seriesValuesList.get(seriesIndex)).setValue(index, value);
   }


   public void setValues(int seriesIndex, List<V> values) {
      checkSeriesIndex(seriesIndex);
      if (getLength() != values.size()) {
         throw new IllegalSeriesLengthException("series values", "series", values.size(),
                 getLength());
      }
      ((SeriesValues)this.seriesValuesList.get(seriesIndex)).setValues(values);
   }


   public void setValue(String seriesName, int index, V value) {
      int seriesIndex = getSeriesIndex(seriesName);
      if (seriesIndex == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      }
      setValue(seriesIndex, index, value);
   }


   public void setValues(String seriesName, List<V> values) {
      int seriesIndex = getSeriesIndex(seriesName);
      if (seriesIndex == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      }
      setValues(seriesIndex, values);
   }


   public void setIndexAndValues(List<I> indexValues, List<List<V>> valuesList) {
      SeriesUtils.checkList(indexValues, "index values");
      SeriesUtils.checkList(valuesList, "series values list");
      this.length = ((List)valuesList.get(0)).size();
      if (this.indexDimension.hasFixLength() && indexValues.size() != getLength()) {
         throw new IllegalSeriesLengthException("index values", "series values", indexValues.size(),
                 getLength());
      }
      if (valuesList.size() != getNumberOfSeries()) {
         throw new IllegalSeriesLengthException("series values list", "number of series", valuesList
                 .size(), getNumberOfSeries());
      }
      for (List<V> values : valuesList) {
         if (values.size() != getLength()) {
            throw new IllegalSeriesLengthException("series values list", "to each other", values.size(),
                    getLength());
         }
      }
      setIndexValues(indexValues);
      for (int i = 0; i < getNumberOfSeries(); i++) {
         setValues(i, (List)valuesList.get(i));
      }
   }


   public void addSeriesValues(SeriesValues<V> seriesValues) {
      if (hasSeries(seriesValues.getName())) {
         throw new NameAlreadyInMultivariateSeriesException(seriesValues.getName());
      }
      if (seriesValues.getLength() != getLength()) {
         throw new IllegalSeriesLengthException(seriesValues.getName(), "series values", seriesValues.getLength(),
                 getLength());
      }
      this.seriesValuesList.add(seriesValues);
   }


   public void addSeries(ISeries<I, V> series) {
      if (series.getLength() != getLength()) {
         throw new IllegalSeriesLengthException("series to add", "series values", series.getLength(), getLength());
      }
      if (!series.getIndexValues().equals(getIndexValues())) {
         throw new IndicesNotEqualException("series to add", "index values");
      }
      for (SeriesValues<V> seriesValues : series.getSeriesValuesList()) {
         addSeriesValues(seriesValues);
      }
   }


   public SeriesValues<V> removeSeries(int seriesIndex) {
      checkSeriesIndex(seriesIndex);
      if (getNumberOfSeries() <= 1) {
         throw new IllegalArgumentException("Can not remove last series from MultivariateSeries.");
      }
      return (SeriesValues)this.seriesValuesList.remove(seriesIndex);
   }


   public SeriesValues<V> removeSeries(String seriesName) {
      if (!hasSeries(seriesName)) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      }
      return removeSeries(getSeriesIndex(seriesName));
   }


   public void setName(String oldName, String newName) {
      int seriesIndex = getSeriesIndex(oldName);
      if (seriesIndex == -1) {
         throw new NameNotInMultivariateSeriesException(oldName);
      }
      setName(seriesIndex, newName);
   }


   public void setName(int seriesIndex, String newName) {
      checkSeriesIndex(seriesIndex);
      if (hasSeries(newName)) {
         throw new NameAlreadyInMultivariateSeriesException(newName);
      }
      ((SeriesValues)this.seriesValuesList.get(seriesIndex)).setName(newName);
   }


   public String getName(int seriesIndex) {
      checkSeriesIndex(seriesIndex);
      return ((SeriesValues)this.seriesValuesList.get(seriesIndex)).getName();
   }



   public void setIndexName(String newName) { this.indexDimension.setName(newName); }




   public String getIndexName() { return this.indexDimension.getName(); }



   public String[] getSeriesNames() {
      String[] seriesNames = new String[getNumberOfSeries()];
      for (int i = 0; i < getNumberOfSeries(); i++) {
         seriesNames[i] = ((SeriesValues)this.seriesValuesList.get(i)).getName();
      }
      return seriesNames;
   }


   public boolean hasSeries(String name) {
      for (SeriesValues<V> seriesValues : this.seriesValuesList) {
         if (seriesValues.getName().equals(name)) {
            return true;
         }
      }
      return false;
   }



   public int getNumberOfSeries() { return this.seriesValuesList.size(); }




   public int getLength() { return this.length; }



   public boolean equals(ISeries<?, ?> series) {
      if (series == null) {
         return false;
      }

      boolean equals = (getLength() == series.getLength() && getNumberOfSeries() == series.getNumberOfSeries() && this.indexDimension.equalIndexDimension(series.getIndexDimension()));
      if (equals) {
         for (int i = 0; i < getNumberOfSeries(); i++) {
            equals = (equals && ((SeriesValues)this.seriesValuesList.get(i)).equalSeriesValues(series.getSeriesValues(i)));
         }
      }
      return equals;
   }


   public String toString() {
      return "Multiseries (Class: " + getClass().getName() + ", number of series: " +
              getNumberOfSeries() + ", length: " + getLength() + "" + "\nIndex:\n" + this.indexDimension +
              "\nSeries Values List:\n" + this.seriesValuesList;
   }


   protected int getSeriesIndex(String seriesName) {
      for (int i = 0; i < getNumberOfSeries(); i++) {
         if (((SeriesValues)this.seriesValuesList.get(i)).getName().equals(seriesName)) {
            return i;
         }
      }
      return -1;
   }


   protected void checkSeriesIndex(int seriesIndex) {
      if (seriesIndex >= getNumberOfSeries()) {
         throw new IndexArgumentsDontMatchException("series index", "number of series", Integer.valueOf(seriesIndex),
                 Integer.valueOf(getNumberOfSeries()), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      }
      if (seriesIndex < 0)
         throw new IllegalIndexArgumentException("series index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
   }

   protected abstract AbstractSeries<I, V> getSubSeriesImplemented(int paramInt1, int paramInt2);
}
