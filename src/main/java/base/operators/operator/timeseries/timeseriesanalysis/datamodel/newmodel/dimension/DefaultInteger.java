package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IndexArgumentsDontMatchException;
import java.util.ArrayList;
import java.util.List;

public class DefaultInteger extends AbstractDimension<Integer> {
   private int length;
   public static final String DEFAULT_NAME = "Default integer";

   public DefaultInteger(int length, boolean isIndexDimension) {
      super(isIndexDimension, SeriesBuilder.IndexType.DEFAULT, (SeriesBuilder.ValuesType)null);
      this.setLength(length);
   }

   public DefaultInteger(int length, boolean isIndexDimension, String name) {
      super(isIndexDimension, SeriesBuilder.IndexType.DEFAULT, (SeriesBuilder.ValuesType)null, name);
      this.setLength(length);
   }

   protected Integer getValueImpl(int index) {
      if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         return index;
      }
   }

   protected List getValuesImpl() {
      List values = new ArrayList();

      for(int i = 0; i < this.length; ++i) {
         values.add(i);
      }

      return values;
   }

   protected void setValueImpl(int index, Integer value) {
      if (index < 0) {
         throw new IllegalIndexArgumentException("index", index, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (value == null) {
         throw new ArgumentIsNullException("value");
      } else if (index != value) {
         throw new IllegalArgumentException("Setting the value to another value than the index itself is not allowed for DefaultInteger.");
      }
   }

   protected void setValuesImpl(List values) {
      for(int i = 0; i < this.length; ++i) {
         this.setValueImpl(i, (Integer)values.get(i));
      }

      this.setLength(values.size());
   }

   public void isValidIndexDimension(List indexValues) {
      this.setValuesImpl(indexValues);
   }

   public void isValidIndexValue(int index, Integer indexValue) {
      this.setValueImpl(index, indexValue);
   }

   public int getLowerArrayIndex(Integer indexValue) {
      if (indexValue < 0) {
         return Integer.MIN_VALUE;
      } else {
         return indexValue > this.getLength() - 1 ? this.getLength() - 1 : indexValue;
      }
   }

   public int getUpperArrayIndex(Integer indexValue) {
      if (indexValue < 0) {
         return 0;
      } else {
         return indexValue > this.getLength() - 1 ? Integer.MAX_VALUE : indexValue;
      }
   }

   public IndexDimension getSubIndexDimension(int lowerArrayIndex, int upperArrayIndex) {
      if (lowerArrayIndex < 0) {
         throw new IllegalIndexArgumentException("lower array index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (upperArrayIndex <= lowerArrayIndex) {
         throw new IndexArgumentsDontMatchException("lower array index", "upper array index", IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else {
         return new DefaultInteger(upperArrayIndex - lowerArrayIndex, this.isIndexDimension, this.getName());
      }
   }

   public SeriesValues getSubSeriesValues(int lowerArrayIndex, int upperArrayIndex) {
      if (lowerArrayIndex < 0) {
         throw new IllegalIndexArgumentException("lower array index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (upperArrayIndex <= lowerArrayIndex) {
         throw new IndexArgumentsDontMatchException("lower array index", "upper array index", IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else {
         return new DefaultInteger(upperArrayIndex - lowerArrayIndex, this.isIndexDimension, this.getName());
      }
   }

   public boolean equalIndexDimension(IndexDimension indexDimension) {
      if (!(indexDimension instanceof DefaultInteger)) {
         return false;
      } else {
         DefaultInteger defaultInteger = (DefaultInteger)indexDimension;
         return defaultInteger.getLength() == this.getLength() && defaultInteger.getName().equals(this.getName());
      }
   }

   public boolean equalSeriesValues(SeriesValues seriesValues) {
      if (!(seriesValues instanceof DefaultInteger)) {
         return false;
      } else {
         DefaultInteger defaultInteger = (DefaultInteger)seriesValues;
         return defaultInteger.getLength() == this.getLength() && defaultInteger.getName().equals(this.getName());
      }
   }

   public boolean hasFixLength() {
      return false;
   }

   public int getLength() {
      return this.length;
   }

   public void setLength(int length) {
      if (length <= 0) {
         throw new IllegalIndexArgumentException("length", length, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else {
         this.length = length;
      }
   }

   protected String getDefaultName() {
      return "Default integer";
   }

   public boolean hasMissingValues() {
      return false;
   }

   public boolean isMissing(Integer value) {
      return value == null;
   }

   public double convertValueToDoubleValue(Integer value) {
      return this.isMissing(value) ? Double.NaN : (double)value;
   }

   public Integer convertDoubleValueToValue(double doubleValue) {
      if (Double.isNaN(doubleValue)) {
         return null;
      } else if (doubleValue < -2.147483648E9D) {
         throw new IllegalArgumentException("Provided doubleValue is smaller than Integer.MIN_VALUE. Cannot convert it to Integer.");
      } else if (doubleValue > 2.147483647E9D) {
         throw new IllegalArgumentException("Provided doubleValue is larger than Integer.MAX_VALUE. Cannot convert it to Integer.");
      } else if (Math.floor(doubleValue) != doubleValue) {
         throw new IllegalArgumentException("Provided doubleValue is not a whole number. Cannot convert it to Integer.");
      } else {
         return (int)doubleValue;
      }
   }

   public DefaultInteger copy() {
      return new DefaultInteger(this.getLength(), this.isIndexDimension, this.getName());
   }
}
