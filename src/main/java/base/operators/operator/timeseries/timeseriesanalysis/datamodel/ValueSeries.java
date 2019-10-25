package base.operators.operator.timeseries.timeseriesanalysis.datamodel;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IllegalSeriesLengthException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NotStrictlyMonotonicIncreasingException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsEmptyException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IndexArgumentsDontMatchException;
import java.util.Arrays;

public class ValueSeries extends Series {
   private static final long serialVersionUID = 5891849987837594761L;
   private double[] indices;
   private boolean defaultIndices;
   public static final String VALUE_SERIES_NAME_DESCRIPTOR = "value series";

   private ValueSeries(double[] indices, double[] values, boolean defaultIndices, String name) {
      super(values, name);
      this.indices = indices;
      this.defaultIndices = defaultIndices;
   }

   public static ValueSeries create(double[] values) {
      return new ValueSeries((double[])null, values, true, "series");
   }

   public static ValueSeries create(double[] values, String name) {
      return new ValueSeries((double[])null, values, true, name);
   }

   public static ValueSeries create(double[] indices, double[] values) {
      return create(indices, values, "series");
   }

   public static ValueSeries create(double[] indices, double[] values, String name) {
      if (values == null) {
         throw new ArgumentIsNullException("values array");
      } else if (indices == null) {
         throw new ArgumentIsNullException("indices array");
      } else if (indices.length != values.length) {
         throw new IllegalSeriesLengthException("indices", "values array", indices.length, values.length);
      } else {
         isValidIndexDimension(indices);
         return new ValueSeries(indices, values, false, name);
      }
   }

   public static ValueSeries create(Series series) {
      if (series == null) {
         throw new ArgumentIsNullException("series");
      } else {
         return new ValueSeries((double[])null, series.getValues(), true, series.getName());
      }
   }

   public static ValueSeries create(Series series, double[] indices) {
      if (series == null) {
         throw new ArgumentIsNullException("series");
      } else {
         return create(indices, series.getValues(), series.getName());
      }
   }

   public void setIndicesAndValues(double[] indices, double[] values) {
      if (values == null) {
         throw new ArgumentIsNullException("values array");
      } else if (indices == null) {
         throw new ArgumentIsNullException("indices array");
      } else if (indices.length != values.length) {
         throw new IllegalSeriesLengthException("indices", "values array", indices.length, values.length);
      } else {
         isValidIndexDimension(indices);
         super.setValues(values);
         this.indices = indices;
         this.defaultIndices = false;
      }
   }

   public void setValues(double[] values) {
      if (values == null) {
         throw new ArgumentIsNullException("values array");
      } else if (values.length != this.getLength() && !this.hasDefaultIndices()) {
         throw new IllegalSeriesLengthException("values array", "the values series", values.length, this.getLength());
      } else {
         super.setValues(values);
      }
   }

   public boolean hasDefaultIndices() {
      return this.defaultIndices;
   }

   public void enableDefaultIndices() {
      this.defaultIndices = true;
      this.indices = null;
   }

   public double[] getIndices() {
      if (!this.defaultIndices) {
         return (double[])this.indices.clone();
      } else {
         double[] result = new double[this.getLength()];

         for(int i = 0; i < this.getLength(); ++i) {
            result[i] = (double)i;
         }

         return result;
      }
   }

   public void setIndices(double[] indices) {
      if (indices == null) {
         throw new ArgumentIsNullException("indices array");
      } else if (indices.length != this.getLength()) {
         throw new IllegalSeriesLengthException("indices array", "the values series", indices.length, this.getLength());
      } else {
         isValidIndexDimension(indices);
         this.indices = indices;
         this.defaultIndices = false;
      }
   }

   public double getIndex(int index) {
      if (index >= this.getLength()) {
         throw new IndexArgumentsDontMatchException("index", "length of series", index, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         return this.defaultIndices ? (double)index : this.indices[index];
      }
   }

   public boolean equals(ValueSeries valueSeries) {
      return this.defaultIndices == valueSeries.hasDefaultIndices() && (this.defaultIndices || Arrays.equals(this.indices, valueSeries.getIndices())) && super.equals(valueSeries);
   }

   public String toString() {
      return "Name: " + this.getName() + "\n" + "Indices: " + this.getIndices() + "\n" + "Values: " + this.getValues() + "\n" + "Length: " + this.getLength();
   }

   public static void isValidIndexDimension(double[] indices) {
      if (indices == null) {
         throw new ArgumentIsNullException("indices array");
      } else if (indices.length == 0) {
         throw new ArgumentIsEmptyException("indices array");
      } else {
         for(int i = 0; i < indices.length - 1; ++i) {
            if (Double.isNaN(indices[i])) {
               throw new SeriesContainsInvalidValuesException("indices array", SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
            }

            if (Double.isInfinite(indices[i])) {
               throw new SeriesContainsInvalidValuesException("indices array", SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
            }

            if (indices[i] >= indices[i + 1]) {
               throw new NotStrictlyMonotonicIncreasingException();
            }
         }

      }
   }

   public ValueSeries clone() {
      return !this.defaultIndices ? new ValueSeries((double[])this.indices.clone(), this.getValues(), this.defaultIndices, this.getName()) : new ValueSeries((double[])null, (double[])this.getValues().clone(), this.defaultIndices, this.getName());
   }

   public int getLowerArrayIndex(double doubleIndex) {
      if (Double.isNaN(doubleIndex)) {
         throw new IllegalArgumentException("Provided double index is NaN.");
      } else if (this.defaultIndices) {
         if (doubleIndex < 0.0D) {
            return Integer.MIN_VALUE;
         } else {
            return doubleIndex > (double)(this.getLength() - 1) ? this.getLength() - 1 : (int)Math.floor(doubleIndex);
         }
      } else if (doubleIndex < this.indices[0]) {
         return Integer.MIN_VALUE;
      } else if (doubleIndex > this.indices[this.getLength() - 1]) {
         return this.getLength() - 1;
      } else {
         int index = Arrays.binarySearch(this.indices, doubleIndex);
         return index < 0 ? -(index + 2) : index;
      }
   }

   public int getUpperArrayIndex(double doubleIndex) {
      if (Double.isNaN(doubleIndex)) {
         throw new IllegalArgumentException("Provided double index is NaN.");
      } else if (this.defaultIndices) {
         if (doubleIndex < 0.0D) {
            return 0;
         } else {
            return doubleIndex > (double)(this.getLength() - 1) ? Integer.MAX_VALUE : (int)Math.ceil(doubleIndex);
         }
      } else if (doubleIndex < this.indices[0]) {
         return 0;
      } else if (doubleIndex > this.indices[this.getLength() - 1]) {
         return Integer.MAX_VALUE;
      } else {
         int index = Arrays.binarySearch(this.indices, doubleIndex);
         return index < 0 ? -(index + 1) : index;
      }
   }

   protected Series getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      double[] subValues = Arrays.copyOfRange(this.getValues(), lowerArrayIndex, upperArrayIndex);
      if (!this.defaultIndices) {
         double[] subIndices = Arrays.copyOfRange(this.indices, lowerArrayIndex, upperArrayIndex);
         return new ValueSeries(subIndices, subValues, this.defaultIndices, this.getName());
      } else {
         return new ValueSeries((double[])null, subValues, this.defaultIndices, this.getName());
      }
   }
}
