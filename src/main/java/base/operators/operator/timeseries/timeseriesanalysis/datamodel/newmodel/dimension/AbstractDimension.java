package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsEmptyException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;

import java.util.List;

public abstract class AbstractDimension<T> implements IndexDimension<T>, SeriesValues<T> {
   protected String name;
   protected final boolean isIndexDimension;
   protected final SeriesBuilder.IndexType indexType;
   protected final SeriesBuilder.ValuesType valuesType;

   protected AbstractDimension(boolean isIndexDimension, SeriesBuilder.IndexType indexType, SeriesBuilder.ValuesType valuesType) {
      this.isIndexDimension = isIndexDimension;
      this.indexType = indexType;
      this.valuesType = valuesType;
      this.name = this.getDefaultName() + " " + this.getDimensionTypeName();
   }

   protected AbstractDimension(boolean isIndexDimension, SeriesBuilder.IndexType indexType, SeriesBuilder.ValuesType valuesType, String name) {
      if (name == null) {
         throw new ArgumentIsNullException("name");
      } else if (name.isEmpty()) {
         throw new ArgumentIsEmptyException("name");
      } else {
         this.isIndexDimension = isIndexDimension;
         this.indexType = indexType;
         this.valuesType = valuesType;
         this.name = name;
      }
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      if (name == null) {
         throw new ArgumentIsNullException("name");
      } else if (name.isEmpty()) {
         throw new ArgumentIsEmptyException("name");
      } else {
         this.name = name;
      }
   }

   public SeriesBuilder.IndexType getIndexType() {
      return this.indexType;
   }

   public SeriesBuilder.ValuesType getValuesType() {
      return this.valuesType;
   }

   public String toString() {
      return this.getName() + "\n" + this.getValuesImpl();
   }

   protected String getDimensionTypeName() {
      return this.isIndexDimension ? "index" : "values";
   }

   public boolean hasMissingValues() {
      for (T value : getValuesImpl()) {
         if (isMissing(value)) {
            return true;
         }
      }
      return false;
   }

   public T getIndexValue(int index) {
      return this.getValueImpl(index);
   }

   public double getIndexValueAsDouble(int index) {
      return this.convertValueToDoubleValue(this.getValueImpl(index));
   }

   public List getIndexValues() {
      return this.getValuesImpl();
   }

   public T getValue(int index) {
      return this.getValueImpl(index);
   }

   public double getValueAsDouble(int index) {
      return this.convertValueToDoubleValue(this.getValueImpl(index));
   }

   public List getValues() {
      return this.getValuesImpl();
   }

   public void setIndexValue(int index, T indexValue) {
      this.isValidIndexValue(index, indexValue);
      this.setValueImpl(index, indexValue);
   }

   public void setDoubleIndexValue(int index, double doubleIndexValue) {
      this.setIndexValue(index, this.convertDoubleValueToValue(doubleIndexValue));
   }

   public void setIndexValues(List<T> indexValues) {
      this.isValidIndexDimension(indexValues);
      this.setValues(indexValues);
   }

   public void setValue(int index, T value) {
      this.setValueImpl(index, value);
   }

   public void setDoubleValue(int index, double doubleValue) {
      this.setValue(index, this.convertDoubleValueToValue(doubleValue));
   }

   public void setValues(List<T> values) {
      this.setValuesImpl(values);
   }

   protected abstract T getValueImpl(int var1);

   protected abstract List<T> getValuesImpl();

   protected abstract void setValueImpl(int var1, T var2);

   protected abstract void setValuesImpl(List<T> var1);

   protected abstract String getDefaultName();

   public abstract AbstractDimension copy();
}
