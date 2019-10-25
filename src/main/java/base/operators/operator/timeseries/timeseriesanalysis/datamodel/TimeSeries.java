package base.operators.operator.timeseries.timeseriesanalysis.datamodel;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IllegalSeriesLengthException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NotStrictlyMonotonicIncreasingException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsEmptyException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IndexArgumentsDontMatchException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

public class TimeSeries extends Series {
   public static final String TIME_SERIES_NAME_DESCRIPTOR = "time series";
   private static final long serialVersionUID = -7265456874513912148L;
   private ArrayList indices;

   private TimeSeries(ArrayList indices, double[] values, String name) {
      super(values, name);
      this.indices = new ArrayList(indices);
   }

   public static TimeSeries create(ArrayList indices, double[] values) {
      return create(indices, values, "series");
   }

   public static TimeSeries create(ArrayList indices, double[] values, String name) {
      if (values == null) {
         throw new ArgumentIsNullException("values array");
      } else if (indices == null) {
         throw new ArgumentIsNullException("indices list");
      } else if (indices.size() != values.length) {
         throw new IllegalSeriesLengthException("indices list", "values array", indices.size(), values.length);
      } else if (!isStrictlyMonotonicIncreasing(indices)) {
         throw new NotStrictlyMonotonicIncreasingException();
      } else {
         return new TimeSeries(indices, values, name);
      }
   }

   public static TimeSeries create(ArrayList indices, Series series) {
      if (series == null) {
         throw new ArgumentIsNullException("series");
      } else {
         return create(indices, series.getValues(), series.getName());
      }
   }

   public void setIndicesAndValues(ArrayList indices, double[] values) {
      if (values == null) {
         throw new ArgumentIsNullException("values array");
      } else if (indices == null) {
         throw new ArgumentIsNullException("indices list");
      } else if (indices.size() != values.length) {
         throw new IllegalSeriesLengthException("indices", "values array", indices.size(), values.length);
      } else if (!isStrictlyMonotonicIncreasing(indices)) {
         throw new NotStrictlyMonotonicIncreasingException();
      } else {
         this.setValues(values);
         this.indices = indices;
      }
   }

   public ArrayList getIndices() {
      return new ArrayList(this.indices);
   }

   public void setIndices(ArrayList indices) {
      if (indices == null) {
         throw new ArgumentIsNullException("indices list");
      } else if (indices.size() != this.getLength()) {
         throw new IllegalSeriesLengthException("indices", "the time series", indices.size(), this.getLength());
      } else if (!isStrictlyMonotonicIncreasing(indices)) {
         throw new NotStrictlyMonotonicIncreasingException();
      } else {
         this.indices = indices;
      }
   }

   public Instant getIndex(int index) {
      if (index >= this.getLength()) {
         throw new IndexArgumentsDontMatchException("index", "length of series", index, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         return (Instant)this.indices.get(index);
      }
   }

   public boolean equals(TimeSeries timeSeries) {
      return this.indices.equals(timeSeries.getIndices()) && super.equals(timeSeries);
   }

   public String toString() {
      return "Name: " + this.getName() + "\n" + "Indices: " + this.getIndices() + "\n" + "Values: " + this.getValues() + "\n" + "Length: " + this.getLength();
   }

   public static boolean isStrictlyMonotonicIncreasing(ArrayList indices) {
      if (indices == null) {
         throw new ArgumentIsNullException("indices list");
      } else if (indices.isEmpty()) {
         throw new ArgumentIsEmptyException("indices list");
      } else {
         for(int i = 0; i < indices.size() - 1; ++i) {
            if (((Instant)indices.get(i)).isAfter((Instant)indices.get(i + 1))) {
               return false;
            }
         }

         return true;
      }
   }

   public TimeSeries clone() {
      return new TimeSeries(new ArrayList(this.indices), this.getValues(), this.getName());
   }

   public int getLowerArrayIndex(Instant instantIndex) {
      if (instantIndex == null) {
         throw new ArgumentIsNullException("instant index");
      } else if (instantIndex.isBefore((Instant)this.indices.get(0))) {
         return Integer.MIN_VALUE;
      } else if (instantIndex.isAfter((Instant)this.indices.get(this.getLength() - 1))) {
         return this.getLength() - 1;
      } else {
         int index = Arrays.binarySearch(this.indices.toArray(), instantIndex);
         return index < 0 ? -(index + 2) : index;
      }
   }

   public int getUpperArrayIndex(Instant instantIndex) {
      if (instantIndex == null) {
         throw new ArgumentIsNullException("instant index");
      } else if (instantIndex.isBefore((Instant)this.indices.get(0))) {
         return 0;
      } else if (instantIndex.isAfter((Instant)this.indices.get(this.getLength() - 1))) {
         return Integer.MAX_VALUE;
      } else {
         int index = Arrays.binarySearch(this.indices.toArray(), instantIndex);
         return index < 0 ? -(index + 1) : index;
      }
   }

   protected Series getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      double[] subValues = Arrays.copyOfRange(this.getValues(), lowerArrayIndex, upperArrayIndex);
      ArrayList subIndices = new ArrayList(this.indices.subList(lowerArrayIndex, upperArrayIndex));
      return new TimeSeries(subIndices, subValues, this.getName());
   }
}
