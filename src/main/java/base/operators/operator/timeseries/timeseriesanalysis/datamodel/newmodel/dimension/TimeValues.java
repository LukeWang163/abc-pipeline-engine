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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimeValues extends AbstractValuesListDimension<Instant> {
   public static final String DEFAULT_NAME = "Time";

   public TimeValues(List values, boolean isIndexDimension) {
      super(values, isIndexDimension, SeriesBuilder.IndexType.TIME, SeriesBuilder.ValuesType.TIME);
   }

   public TimeValues(List values, boolean isIndexDimension, String name) {
      super(values, isIndexDimension, SeriesBuilder.IndexType.TIME, SeriesBuilder.ValuesType.TIME, name);
   }

   public void isValidIndexDimension(List indexValues) {
      SeriesUtils.checkList(indexValues, this.getDimensionTypeName() + " list");

      for(int i = 0; i < indexValues.size() - 1; ++i) {
         if (this.isMissing((Instant)indexValues.get(i)) || this.isMissing((Instant)indexValues.get(i + 1))) {
            throw new SeriesContainsInvalidValuesException(this.getDimensionTypeName() + " list", SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
         }

         if (((Instant)indexValues.get(i)).isAfter((Instant)indexValues.get(i + 1))) {
            throw new NotStrictlyMonotonicIncreasingException();
         }
      }

   }

   public void isValidIndexValue(int index, Instant indexValue) {
      if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (index >= this.getLength()) {
         throw new IndexArgumentsDontMatchException("index", "series", index, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (this.isMissing(indexValue)) {
         throw new IllegalArgumentException("Provided index value is NaN.");
      } else if (index > 0 && ((Instant)this.valuesList.get(index - 1)).isAfter(indexValue)) {
         throw new NotStrictlyMonotonicIncreasingException();
      } else if (index < this.getLength() - 1 && indexValue.isAfter((Instant)this.valuesList.get(index + 1))) {
         throw new NotStrictlyMonotonicIncreasingException();
      }
   }

   public int getLowerArrayIndex(Instant indexValue) {
      if (indexValue == null) {
         throw new ArgumentIsNullException("indexValue");
      } else if (indexValue.isBefore((Instant)this.valuesList.get(0))) {
         return Integer.MIN_VALUE;
      } else if (indexValue.isAfter((Instant)this.valuesList.get(this.getLength() - 1))) {
         return this.getLength() - 1;
      } else {
         int index = Arrays.binarySearch(this.valuesList.toArray(), indexValue);
         return index < 0 ? -(index + 2) : index;
      }
   }

   public int getUpperArrayIndex(Instant indexValue) {
      if (indexValue == null) {
         throw new ArgumentIsNullException("instant index");
      } else if (indexValue.isBefore((Instant)this.valuesList.get(0))) {
         return 0;
      } else if (indexValue.isAfter((Instant)this.valuesList.get(this.getLength() - 1))) {
         return Integer.MAX_VALUE;
      } else {
         int index = Arrays.binarySearch(this.valuesList.toArray(), indexValue);
         return index < 0 ? -(index + 1) : index;
      }
   }

   public IndexDimension getSubIndexDimension(int lowerArrayIndex, int upperArrayIndex) {
      return new TimeValues(new ArrayList(this.valuesList.subList(lowerArrayIndex, upperArrayIndex)), this.isIndexDimension, this.getName());
   }

   public SeriesValues getSubSeriesValues(int lowerArrayIndex, int upperArrayIndex) {
      return new TimeValues(new ArrayList(this.valuesList.subList(lowerArrayIndex, upperArrayIndex)), this.isIndexDimension, this.getName());
   }

   public boolean isMissing(Instant value) {
      return value == null;
   }

   public double convertValueToDoubleValue(Instant value) {
      return this.isMissing(value) ? Double.NaN : (double)value.toEpochMilli();
   }

   public Instant convertDoubleValueToValue(double doubleValue) {
      if (Double.isNaN(doubleValue)) {
         return null;
      } else if (doubleValue < -9.223372036854776E18D) {
         throw new IllegalArgumentException("Provided doubleValue is smaller than Long.MIN_VALUE. Cannot convert it to Instant.");
      } else if (doubleValue > 9.223372036854776E18D) {
         throw new IllegalArgumentException("Provided doubleValue is larger than Long.MAX_VALUE. Cannot convert it to Instant.");
      } else if (Math.floor(doubleValue) != doubleValue) {
         throw new IllegalArgumentException("Provided doubleValue is not a whole number (long). Cannot convert it to Instant.");
      } else {
         return Instant.ofEpochMilli((long)doubleValue);
      }
   }

   protected String getDefaultName() {
      return "Time";
   }

   public TimeValues copy() {
      return new TimeValues(new ArrayList(this.valuesList), this.isIndexDimension, this.getName());
   }
}
