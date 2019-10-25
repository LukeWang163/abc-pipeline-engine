package base.operators.operator.timeseries.timeseriesanalysis.datamodel;

import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsEmptyException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IndexArgumentsDontMatchException;
import base.operators.operator.timeseries.timeseriesanalysis.tools.SeriesUtils;
import java.io.Serializable;
import java.util.Arrays;

public abstract class Series implements Serializable {
   private static final long serialVersionUID = 4328648948488292307L;
   private double[] values;
   private String name;
   public static final String DEFAULT_NAME = "series";
   public static final String VALUES_ARRAY_DESCRIPTOR = "values array";
   public static final String INDICES_ARRAY_DESCRIPTOR = "indices array";
   public static final String INDICES_LIST_DESCRIPTOR = "indices list";
   public static final String INDEX_DESCRIPTOR = "index";
   public static final String LENGTH_DESCRIPTOR = "length of series";
   public static final String LOWER_ARRAY_INDEX_DESCRIPTOR = "lowerArrayIndex";
   public static final String UPPER_ARRAY_INDEX_DESCRIPTOR = "upperArrayIndex";

   protected Series(double[] values, String name) {
      if (values == null) {
         throw new ArgumentIsNullException("values array");
      } else if (values.length == 0) {
         throw new ArgumentIsEmptyException("values array");
      } else if (name.isEmpty()) {
         throw new ArgumentIsEmptyException("name");
      } else {
         this.values = values;
         this.name = name;
      }
   }

   public abstract Series clone();

   public double[] getValues() {
      return (double[])this.values.clone();
   }

   public Series getSubSeries(int lowerArrayIndex, int upperArrayIndex) {
      if (lowerArrayIndex < 0) {
         throw new IllegalIndexArgumentException("lowerArrayIndex", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (upperArrayIndex < 0) {
         throw new IllegalIndexArgumentException("upperArrayIndex", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (upperArrayIndex < lowerArrayIndex) {
         throw new IndexArgumentsDontMatchException("upperArrayIndex", "lowerArrayIndex", upperArrayIndex, lowerArrayIndex, IndexArgumentsDontMatchException.MisMatchType.SMALLER);
      } else if (upperArrayIndex > this.getLength()) {
         throw new IndexArgumentsDontMatchException("upperArrayIndex", "length of the series", upperArrayIndex, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER);
      } else {
         return lowerArrayIndex == upperArrayIndex ? null : this.getSubSeriesImplemented(lowerArrayIndex, upperArrayIndex);
      }
   }

   protected abstract Series getSubSeriesImplemented(int var1, int var2);

   protected void setValues(double[] values) {
      if (values == null) {
         throw new ArgumentIsNullException("values array");
      } else if (values.length == 0) {
         throw new ArgumentIsEmptyException("values array");
      } else {
         this.values = values;
      }
   }

   public void setValue(int index, double value) {
      if (index >= this.values.length) {
         throw new IndexArgumentsDontMatchException("index", "length of series", index, this.values.length, IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         this.values[index] = value;
      }
   }

   public double getValue(int index) {
      if (index >= this.values.length) {
         throw new IndexArgumentsDontMatchException("index", "length of series", index, this.values.length, IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         return this.values[index];
      }
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      if (name.isEmpty()) {
         throw new ArgumentIsEmptyException("name");
      } else {
         this.name = name;
      }
   }

   public boolean hasNaNValues() {
      return SeriesUtils.hasNaNValues(this.values);
   }

   public boolean hasInfiniteValues() {
      return SeriesUtils.hasInfiniteValues(this.values);
   }

   protected boolean equals(Series valueSeries) {
      if (this.getLength() != valueSeries.getLength()) {
         return false;
      } else if (!Arrays.equals(this.getValues(), valueSeries.getValues())) {
         return false;
      } else {
         return this.getName().equals(valueSeries.getName());
      }
   }

   public int getLength() {
      return this.values.length;
   }

   public abstract String toString();
}
