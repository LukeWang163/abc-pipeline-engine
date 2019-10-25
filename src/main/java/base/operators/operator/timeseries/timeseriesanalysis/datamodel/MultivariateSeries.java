package base.operators.operator.timeseries.timeseriesanalysis.datamodel;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IllegalSeriesLengthException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NameAlreadyInMultivariateSeriesException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NameNotInMultivariateSeriesException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsEmptyException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IndexArgumentsDontMatchException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public abstract class MultivariateSeries {
   private ArrayList<Series> seriesList;
   public static final String SERIES_LIST_DESCRIPTOR = "seriesList";
   public static final String MULTIVARIATE_SERIES_DESCRIPTOR = "multivariate series";
   public static final String MULTIVARIATE_SERIES_RANGE_DESCRIPTOR = "multivariate series range";
   public static final String MULTIVARIATE_SERIES_INDEX_DESCRIPTOR = "seriesIndex";

   protected MultivariateSeries(ArrayList newSeriesList) {
      if (newSeriesList == null) {
         throw new ArgumentIsNullException("seriesList");
      } else if (newSeriesList.isEmpty()) {
         throw new ArgumentIsEmptyException("seriesList");
      } else {
         this.seriesList = new ArrayList();
         this.seriesList.add(CollectibleSeries.create((Series)newSeriesList.get(0)));

         for(int i = 1; i < newSeriesList.size(); ++i) {
            this.addSeries((Series)newSeriesList.get(i));
         }

      }
   }

   public abstract MultivariateSeries clone();

   public double[] getValues(String seriesName) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         return this.getValues(index);
      }
   }

   public double[] getValues(int index) {
      return (double[])this.getSeries(index).getValues().clone();
   }

   public void setValues(String seriesName, double[] values) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         this.setValues(index, values);
      }
   }

   public void setValues(int index, double[] values) {
      if (values == null) {
         throw new ArgumentIsNullException("values array");
      } else if (this.getLength() != values.length) {
         throw new IllegalSeriesLengthException("values array", "multivariate series", values.length, this.getLength());
      } else if (index >= this.getSeriesCount()) {
         throw new IndexArgumentsDontMatchException("index", "multivariate series range", index, this.getSeriesCount(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         ((Series)this.seriesList.get(index)).setValues(values);
      }
   }

   public double getValue(String seriesName, int seriesIndex) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         return this.getValue(index, seriesIndex);
      }
   }

   public double getValue(int index, int seriesIndex) {
      if (index >= this.getSeriesCount()) {
         throw new IndexArgumentsDontMatchException("index", "multivariate series range", index, this.getSeriesCount(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (seriesIndex >= this.getLength()) {
         throw new IndexArgumentsDontMatchException("seriesIndex", "length of series", seriesIndex, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (seriesIndex < 0) {
         throw new IllegalIndexArgumentException("seriesIndex", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         return ((Series)this.seriesList.get(index)).getValue(seriesIndex);
      }
   }

   public void setValue(String seriesName, int seriesIndex, double value) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         this.setValue(index, seriesIndex, value);
      }
   }

   public void setValue(int index, int seriesIndex, double value) {
      if (index >= this.getSeriesCount()) {
         throw new IndexArgumentsDontMatchException("index", "multivariate series range", index, this.getSeriesCount(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (seriesIndex >= this.getLength()) {
         throw new IndexArgumentsDontMatchException("seriesIndex", "length of series", seriesIndex, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (seriesIndex < 0) {
         throw new IllegalIndexArgumentException("seriesIndex", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         ((Series)this.seriesList.get(index)).setValue(seriesIndex, value);
      }
   }

   public void setName(String oldName, String newName) {
      int index = this.getIndex(oldName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(oldName);
      } else {
         this.setName(index, newName);
      }
   }

   public void setName(int index, String name) {
      if (index >= this.getSeriesCount()) {
         throw new IndexArgumentsDontMatchException("index", "multivariate series range", index, this.getSeriesCount(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (index < 0) {
         throw new NameNotInMultivariateSeriesException("index: " + index);
      } else if (this.hasSeries(name)) {
         throw new NameAlreadyInMultivariateSeriesException(name);
      } else {
         ((Series)this.seriesList.get(index)).setName(name);
      }
   }

   public String getName(int index) {
      return this.getSeries(index).getName();
   }

   protected Series getSeries(int index) {
      if (index >= this.getSeriesCount()) {
         throw new IndexArgumentsDontMatchException("index", "multivariate series range", index, this.getSeriesCount(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         return (Series)this.seriesList.get(index);
      }
   }

   protected void setSeries(int index, Series series) {
      if (series == null) {
         throw new ArgumentIsNullException("series");
      } else if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (index >= this.getSeriesCount()) {
         throw new IndexArgumentsDontMatchException("index", "multivariate series range", index, this.getSeriesCount(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (this.getLength() != series.getLength()) {
         throw new IllegalSeriesLengthException(series.getName(), "multivariate series", series.getLength(), this.getLength());
      } else {
         int indexOfSeriesNameInSeriesList = this.getIndex(series.getName());
         if (indexOfSeriesNameInSeriesList != -1 && indexOfSeriesNameInSeriesList != index) {
            throw new NameAlreadyInMultivariateSeriesException(series.getName());
         } else {
            this.seriesList.set(index, CollectibleSeries.create(series));
         }
      }
   }

   protected void addSeries(Series series) {
      if (series == null) {
         throw new ArgumentIsNullException("series");
      } else if (this.hasSeries(series.getName())) {
         throw new NameAlreadyInMultivariateSeriesException(series.getName());
      } else if (series.getLength() != this.getLength()) {
         throw new IllegalSeriesLengthException("series", "multivariate series", series.getLength(), this.getLength());
      } else {
         this.seriesList.add(CollectibleSeries.create(series));
      }
   }

   protected Series removeSeries(int index) {
      if (index >= this.getSeriesCount()) {
         throw new IndexArgumentsDontMatchException("index", "multivariate series range", index, this.getSeriesCount(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (this.getSeriesCount() <= 1) {
         throw new IllegalArgumentException("Can not remove last series from MultivariateSeries.");
      } else {
         return (Series)this.seriesList.remove(index);
      }
   }

   protected Iterator getIterator() {
      return this.seriesList.iterator();
   }

   public int getSeriesCount() {
      return this.seriesList.size();
   }

   public int getLength() {
      return ((Series)this.seriesList.get(0)).getLength();
   }

   public boolean hasNaNValues() {
      Iterator var1 = this.seriesList.iterator();

      Series series;
      do {
         if (!var1.hasNext()) {
            return false;
         }

         series = (Series)var1.next();
      } while(!series.hasNaNValues());

      return true;
   }

   protected boolean equals(MultivariateSeries multivariateSeries) {
      if (this.getLength() != multivariateSeries.getLength()) {
         return false;
      } else {
         Iterator iterator1 = this.getIterator();
         Iterator iterator2 = multivariateSeries.getIterator();

         while(iterator1.hasNext() && iterator2.hasNext()) {
            if (!((Series)iterator1.next()).equals((Series)iterator2.next())) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean hasInfiniteValues() {
      Iterator var1 = this.seriesList.iterator();

      Series series;
      do {
         if (!var1.hasNext()) {
            return false;
         }

         series = (Series)var1.next();
      } while(!series.hasInfiniteValues());

      return true;
   }

   public String[] getSeriesNames() {
      String[] seriesNames = new String[this.getSeriesCount()];

      for(int i = 0; i < this.getSeriesCount(); ++i) {
         seriesNames[i] = ((Series)this.seriesList.get(i)).getName();
      }

      return seriesNames;
   }

   public boolean hasSeries(String seriesName) {
      for(int i = 0; i < this.getSeriesCount(); ++i) {
         if (((Series)this.seriesList.get(i)).getName().equals(seriesName)) {
            return true;
         }
      }

      return false;
   }

   protected int getIndex(String seriesName) {
      for(int i = 0; i < this.getSeriesCount(); ++i) {
         if (((Series)this.seriesList.get(i)).getName().equals(seriesName)) {
            return i;
         }
      }

      return -1;
   }

   public String toString() {
      return this.seriesList.toString() + "\n" + "Length: " + this.getLength();
   }

   public MultivariateSeries getSubMultivariateSeries(int lowerArrayIndex, int upperArrayIndex) {
      if (lowerArrayIndex < 0) {
         throw new IllegalIndexArgumentException("lowerArrayIndex", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (upperArrayIndex < 0) {
         throw new IllegalIndexArgumentException("upperArrayIndex", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (upperArrayIndex < lowerArrayIndex) {
         throw new IndexArgumentsDontMatchException("upperArrayIndex", "lowerArrayIndex", upperArrayIndex, lowerArrayIndex, IndexArgumentsDontMatchException.MisMatchType.SMALLER);
      } else if (upperArrayIndex > this.getLength()) {
         throw new IndexArgumentsDontMatchException("upperArrayIndex", "length of series", upperArrayIndex, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER);
      } else {
         return lowerArrayIndex == upperArrayIndex ? null : this.getSubMultivariateSeriesImplemented(lowerArrayIndex, upperArrayIndex);
      }
   }

   protected abstract MultivariateSeries getSubMultivariateSeriesImplemented(int var1, int var2);

   private static class CollectibleSeries extends Series {
      private static final long serialVersionUID = 3587135678955090865L;

      protected CollectibleSeries(Series series, String seriesName) {
         super(series.getValues(), seriesName);
      }

      public static Series create(Series series) {
         return new CollectibleSeries(series, series.getName());
      }

      public String toString() {
         return "Name: " + super.getName() + "\n" + "Values: " + super.getValues() + "\n" + "Length: " + super.getLength();
      }

      protected Series getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
         double[] subValues = Arrays.copyOfRange(super.getValues(), lowerArrayIndex, upperArrayIndex);
         return new CollectibleSeries(ValueSeries.create(subValues), super.getName());
      }

      public Series clone() {
         return new CollectibleSeries(this, super.getName());
      }
   }
}
