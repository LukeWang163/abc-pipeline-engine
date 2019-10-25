package base.operators.operator.timeseries.timeseriesanalysis.datamodel;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IllegalSeriesLengthException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IndicesNotEqualException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NameNotInMultivariateSeriesException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class MultivariateValueSeries extends MultivariateSeries {
   private double[] indices;
   private boolean defaultIndices;

   private MultivariateValueSeries(double[] indices, ArrayList seriesList, boolean defaultIndices) {
      super(seriesList);
      if (defaultIndices) {
         this.indices = null;
         this.defaultIndices = true;
      } else if (indices != null) {
         if (this.getLength() != indices.length) {
            throw new IllegalSeriesLengthException("indices array", "seriesList", indices.length, this.getLength());
         }

         ValueSeries.isValidIndexDimension(indices);
         this.indices = indices;
         this.defaultIndices = false;
      } else {
         ValueSeries firstSeries = (ValueSeries)seriesList.get(0);
         Iterator var5 = seriesList.iterator();

         while(var5.hasNext()) {
            ValueSeries valueSeries = (ValueSeries)var5.next();
            if (!Arrays.equals(firstSeries.getIndices(), valueSeries.getIndices())) {
               throw new IndicesNotEqualException("list of series");
            }
         }

         this.indices = firstSeries.getIndices();
         this.defaultIndices = false;
      }

   }

   public static MultivariateValueSeries create(ArrayList seriesList) {
      return create(seriesList, true);
   }

   public static MultivariateValueSeries create(ArrayList seriesList, boolean useDefaultIndices) {
      return new MultivariateValueSeries((double[])null, seriesList, useDefaultIndices);
   }

   public static MultivariateValueSeries create(double[] indices, ArrayList seriesList) {
      return new MultivariateValueSeries(indices, seriesList, false);
   }

   public static MultivariateValueSeries create(ArrayList valuesArrayList, ArrayList seriesNames) {
      if (valuesArrayList == null) {
         throw new ArgumentIsNullException("values array list");
      } else if (seriesNames == null) {
         throw new ArgumentIsNullException("series names list");
      } else if (valuesArrayList.size() != seriesNames.size()) {
         throw new IllegalSeriesLengthException("valuesArrayList", "seriesNames", valuesArrayList.size(), seriesNames.size());
      } else {
         ArrayList seriesList = new ArrayList();
         Iterator valuesIterator = valuesArrayList.iterator();
         Iterator nameIterator = seriesNames.iterator();

         while(valuesIterator.hasNext() && nameIterator.hasNext()) {
            seriesList.add(ValueSeries.create((double[])valuesIterator.next(), (String)nameIterator.next()));
         }

         return new MultivariateValueSeries((double[])null, seriesList, true);
      }
   }

   public static MultivariateValueSeries create(double[] indices, ArrayList valuesArrayList, ArrayList seriesNames) {
      if (indices == null) {
         throw new ArgumentIsNullException("indices array");
      } else if (valuesArrayList == null) {
         throw new ArgumentIsNullException("values array list");
      } else if (seriesNames == null) {
         throw new ArgumentIsNullException("series names list");
      } else if (valuesArrayList.size() != seriesNames.size()) {
         throw new IllegalSeriesLengthException("valuesArrayList", "seriesNames", valuesArrayList.size(), seriesNames.size());
      } else {
         ArrayList seriesList = new ArrayList();
         Iterator valuesIterator = valuesArrayList.iterator();
         Iterator nameIterator = seriesNames.iterator();

         while(valuesIterator.hasNext() && nameIterator.hasNext()) {
            seriesList.add(ValueSeries.create((double[])valuesIterator.next(), (String)nameIterator.next()));
         }

         return new MultivariateValueSeries(indices, seriesList, false);
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
         double[] indexDimension = new double[this.getLength()];

         for(int i = 0; i < this.getLength(); ++i) {
            indexDimension[i] = (double)i;
         }

         return indexDimension;
      }
   }

   public void setIndices(double[] indices) {
      if (indices == null) {
         throw new ArgumentIsNullException("indices array");
      } else if (indices.length != this.getLength()) {
         throw new IllegalSeriesLengthException("indices array", "multivariate value series", indices.length, this.getLength());
      } else {
         ValueSeries.isValidIndexDimension(indices);
         this.indices = indices;
         this.defaultIndices = false;
      }
   }

   public ValueSeries getValueSeries(String seriesName) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         return this.getValueSeries(index);
      }
   }

   public ValueSeries getValueSeries(int index) {
      return this.defaultIndices ? ValueSeries.create(this.getSeries(index)) : ValueSeries.create(this.getSeries(index), this.getIndices());
   }

   public void setValueSeries(String seriesName, ValueSeries valueSeries) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         this.setValueSeries(index, valueSeries, false);
      }
   }

   public void setValueSeries(int index, ValueSeries valueSeries) {
      this.setValueSeries(index, valueSeries, false);
   }

   public void setValueSeries(String seriesName, ValueSeries valueSeries, boolean ignoreIndices) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         this.setValueSeries(index, valueSeries, ignoreIndices);
      }
   }

   public void setValueSeries(int index, ValueSeries valueSeries, boolean ignoreIndices) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         if (!valueSeries.hasDefaultIndices() && !ignoreIndices) {
            if (!Arrays.equals(this.getIndices(), valueSeries.getIndices())) {
               throw new IndicesNotEqualException("value series", "the multivariate value series indices");
            }

            this.setSeries(index, valueSeries);
         } else {
            this.setSeries(index, valueSeries);
         }

      }
   }

   public void addValueSeries(ValueSeries valueSeries) {
      this.addValueSeries(valueSeries, false);
   }

   public void addValueSeries(ValueSeries valueSeries, boolean ignoreIndices) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         if (!valueSeries.hasDefaultIndices() && !ignoreIndices) {
            if (!Arrays.equals(this.getIndices(), valueSeries.getIndices())) {
               throw new IndicesNotEqualException("value series", "the multivariate value series indices");
            }

            this.addSeries(valueSeries);
         } else {
            this.addSeries(valueSeries);
         }

      }
   }

   public ValueSeries removeValueSeries(String seriesName) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         return this.removeValueSeries(index);
      }
   }

   public ValueSeries removeValueSeries(int index) {
      return this.defaultIndices ? ValueSeries.create(this.removeSeries(index)) : ValueSeries.create(this.removeSeries(index), this.getIndices());
   }

   public String toString() {
      return "Indices: " + this.getIndices() + "\n" + super.toString();
   }

   public boolean equals(MultivariateValueSeries multivariateValueSeries) {
      return this.defaultIndices == multivariateValueSeries.hasDefaultIndices() && (this.defaultIndices || Arrays.equals(this.indices, multivariateValueSeries.getIndices())) && super.equals(multivariateValueSeries);
   }

   public MultivariateValueSeries clone() {
      ArrayList seriesList = new ArrayList();
      Iterator iterator = this.getIterator();

      while(iterator.hasNext()) {
         seriesList.add(ValueSeries.create((Series)iterator.next()));
      }

      return !this.defaultIndices ? new MultivariateValueSeries((double[])this.indices.clone(), seriesList, this.defaultIndices) : new MultivariateValueSeries((double[])null, seriesList, this.defaultIndices);
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

   protected MultivariateSeries getSubMultivariateSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      ArrayList valuesList = new ArrayList();
      ArrayList namesList = new ArrayList();
      Iterator iterator = this.getIterator();

      while(iterator.hasNext()) {
         Series series = (Series)iterator.next();
         valuesList.add(series.getSubSeries(lowerArrayIndex, upperArrayIndex).getValues());
         namesList.add(series.getName());
      }

      if (!this.defaultIndices) {
         double[] subIndices = Arrays.copyOfRange(this.indices, lowerArrayIndex, upperArrayIndex);
         return create(subIndices, valuesList, namesList);
      } else {
         return create(valuesList, namesList);
      }
   }
}
