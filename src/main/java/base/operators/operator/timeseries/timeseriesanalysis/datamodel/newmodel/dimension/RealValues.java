package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NotStrictlyMonotonicIncreasingException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IndexArgumentsDontMatchException;
import base.operators.operator.timeseries.timeseriesanalysis.tools.SeriesUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class RealValues extends AbstractValuesListDimension<Double> {
   public static final String DEFAULT_NAME = "Real";

   public RealValues(List values, boolean isIndexDimension) {
      super(values, isIndexDimension, SeriesBuilder.IndexType.REAL, SeriesBuilder.ValuesType.REAL);
   }

   public RealValues(List values, boolean isIndexDimension, String name) {
      super(values, isIndexDimension, SeriesBuilder.IndexType.REAL, SeriesBuilder.ValuesType.REAL, name);
   }

   public void isValidIndexDimension(List indexValues) {
      SeriesUtils.checkList(indexValues, this.getDimensionTypeName() + " list");
      int i = 0;

      while(i < indexValues.size() - 1) {
         if (!this.isMissing((Double)indexValues.get(i)) && !this.isMissing((Double)indexValues.get(i + 1))) {
            if (!((Double)indexValues.get(i)).isInfinite() && !((Double)indexValues.get(i + 1)).isInfinite()) {
               if ((Double)indexValues.get(i) >= (Double)indexValues.get(i + 1)) {
                  throw new NotStrictlyMonotonicIncreasingException();
               }

               ++i;
               continue;
            }

            throw new SeriesContainsInvalidValuesException(this.getDimensionTypeName() + " list", SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
         }

         throw new SeriesContainsInvalidValuesException(this.getDimensionTypeName() + " list", SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      }

   }

   public void isValidIndexValue(int index, Double indexValue) {
      if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (index >= this.getLength()) {
         throw new IndexArgumentsDontMatchException("index", "series", index, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (this.isMissing(indexValue)) {
         throw new IllegalArgumentException("Provided index value is NaN.");
      } else if (index > 0 && (Double)this.valuesList.get(index - 1) >= indexValue) {
         throw new NotStrictlyMonotonicIncreasingException();
      } else if (index < this.getLength() - 1 && indexValue >= (Double)this.valuesList.get(index + 1)) {
         throw new NotStrictlyMonotonicIncreasingException();
      }
   }

   public int getLowerArrayIndex(Double indexValue) {
      if (indexValue.isNaN()) {
         throw new IllegalArgumentException("Provided index value is NaN.");
      } else if (indexValue < (Double)this.valuesList.get(0)) {
         return Integer.MIN_VALUE;
      } else if (indexValue > (Double)this.valuesList.get(this.getLength() - 1)) {
         return this.getLength() - 1;
      } else {
         int index = Arrays.binarySearch(this.valuesList.toArray(), indexValue);
         return index < 0 ? -(index + 2) : index;
      }
   }

   public int getUpperArrayIndex(Double indexValue) {
      if (indexValue.isNaN()) {
         throw new IllegalArgumentException("Provided index value is NaN.");
      } else if (indexValue < (Double)this.valuesList.get(0)) {
         return 0;
      } else if (indexValue > (Double)this.valuesList.get(this.getLength() - 1)) {
         return Integer.MAX_VALUE;
      } else {
         int index = Arrays.binarySearch(this.valuesList.toArray(), indexValue);
         return index < 0 ? -(index + 1) : index;
      }
   }

   public IndexDimension getSubIndexDimension(int lowerArrayIndex, int upperArrayIndex) {
      return new RealValues(new ArrayList(this.valuesList.subList(lowerArrayIndex, upperArrayIndex)), this.isIndexDimension, this.getName());
   }

   public SeriesValues getSubSeriesValues(int lowerArrayIndex, int upperArrayIndex) {
      return new RealValues(new ArrayList(this.valuesList.subList(lowerArrayIndex, upperArrayIndex)), this.isIndexDimension, this.getName());
   }

   public boolean isMissing(Double value) {
      return value == null || value.isNaN();
   }

   public double convertValueToDoubleValue(Double value) {
      return this.isMissing(value) ? Double.NaN : value;
   }

   public Double convertDoubleValueToValue(double doubleValue) {
      return doubleValue;
   }

   public boolean hasInfiniteValues() {
      Iterator var1 = this.getValuesImpl().iterator();

      Double value;
      do {
         if (!var1.hasNext()) {
            return false;
         }

         value = (Double)var1.next();
      } while(!value.isInfinite());

      return true;
   }

   protected String getDefaultName() {
      return "Real";
   }

   public RealValues copy() {
      return new RealValues(new ArrayList(this.valuesList), this.isIndexDimension, this.getName());
   }
}
