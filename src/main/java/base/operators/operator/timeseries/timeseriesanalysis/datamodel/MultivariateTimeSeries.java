package base.operators.operator.timeseries.timeseriesanalysis.datamodel;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IllegalSeriesLengthException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IndicesNotEqualException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NameNotInMultivariateSeriesException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NotStrictlyMonotonicIncreasingException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class MultivariateTimeSeries extends MultivariateSeries {
   private ArrayList<Instant> indices;

   private MultivariateTimeSeries(ArrayList<Instant> indices, ArrayList<TimeSeries> seriesList) {
      super(seriesList);
      if (indices != null) {
         if (this.getLength() != indices.size()) {
            throw new IllegalSeriesLengthException("indices list", "seriesList", indices.size(), this.getLength());
         }

         if (!TimeSeries.isStrictlyMonotonicIncreasing(indices)) {
            throw new NotStrictlyMonotonicIncreasingException();
         }

         this.indices = indices;
      } else {
         TimeSeries firstSeries = (TimeSeries)seriesList.get(0);
         Iterator var4 = seriesList.iterator();

         while(var4.hasNext()) {
            TimeSeries timeSeries = (TimeSeries)var4.next();
            if (!timeSeries.getIndices().equals(firstSeries.getIndices())) {
               throw new IndicesNotEqualException("seriesList");
            }
         }

         this.indices = firstSeries.getIndices();
      }

   }

   public static MultivariateTimeSeries create(ArrayList seriesList) {
      return new MultivariateTimeSeries((ArrayList)null, seriesList);
   }

   public static MultivariateTimeSeries create(ArrayList indices, ArrayList seriesList) {
      return new MultivariateTimeSeries(indices, seriesList);
   }

   public static MultivariateTimeSeries create(ArrayList indices, ArrayList valuesArrayList, ArrayList seriesNames) {
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
            seriesList.add(TimeSeries.create(indices, (double[])valuesIterator.next(), (String)nameIterator.next()));
         }

         return new MultivariateTimeSeries(indices, seriesList);
      }
   }

   public ArrayList getIndices() {
      return new ArrayList(this.indices);
   }

   public void setIndices(ArrayList indices) {
      if (indices == null) {
         throw new ArgumentIsNullException("indices list");
      } else if (indices.size() != this.getLength()) {
         throw new IllegalSeriesLengthException("indices list", "multivariate time series", indices.size(), this.getLength());
      } else if (!TimeSeries.isStrictlyMonotonicIncreasing(indices)) {
         throw new NotStrictlyMonotonicIncreasingException();
      } else {
         this.indices = indices;
      }
   }

   public TimeSeries getTimeSeries(String seriesName) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         return TimeSeries.create(this.getIndices(), this.getSeries(index));
      }
   }

   public TimeSeries getTimeSeries(int index) {
      return TimeSeries.create(this.getIndices(), this.getSeries(index));
   }

   public void setTimeSeries(String seriesName, TimeSeries timeSeries) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         this.setTimeSeries(index, timeSeries);
      }
   }

   public void setTimeSeries(int index, TimeSeries timeSeries) {
      this.setTimeSeries(index, timeSeries, false);
   }

   public void setTimeSeries(String seriesName, TimeSeries timeSeries, boolean ignoreIndices) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         this.setTimeSeries(index, timeSeries, ignoreIndices);
      }
   }

   public void setTimeSeries(int index, TimeSeries timeSeries, boolean ignoreIndices) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         if (ignoreIndices) {
            this.setSeries(index, timeSeries);
         } else {
            if (!this.getIndices().equals(timeSeries.getIndices())) {
               throw new IndicesNotEqualException("time series", "the multivariate time series indices");
            }

            this.setSeries(index, timeSeries);
         }

      }
   }

   public void addTimeSeries(TimeSeries timeSeries) {
      this.addTimeSeries(timeSeries, false);
   }

   public void addTimeSeries(TimeSeries timeSeries, boolean ignoreIndices) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         if (ignoreIndices) {
            this.addSeries(timeSeries);
         } else {
            if (!this.getIndices().equals(timeSeries.getIndices())) {
               throw new IndicesNotEqualException("time series", "the multivariate time series indices");
            }

            this.addSeries(timeSeries);
         }

      }
   }

   public TimeSeries removeTimeSeries(String seriesName) {
      int index = this.getIndex(seriesName);
      if (index == -1) {
         throw new NameNotInMultivariateSeriesException(seriesName);
      } else {
         return this.removeTimeSeries(index);
      }
   }

   public TimeSeries removeTimeSeries(int index) {
      return TimeSeries.create(this.getIndices(), this.removeSeries(index));
   }

   public boolean equals(MultivariateTimeSeries multivariateTimeSeries) {
      return this.indices.equals(multivariateTimeSeries.getIndices()) && super.equals(multivariateTimeSeries);
   }

   public String toString() {
      return "Indices: " + this.getIndices() + "\n" + super.toString();
   }

   public MultivariateTimeSeries clone() {
      ArrayList seriesList = new ArrayList();
      Iterator iterator = this.getIterator();

      while(iterator.hasNext()) {
         seriesList.add(TimeSeries.create(new ArrayList(this.indices), (Series)iterator.next()));
      }

      return new MultivariateTimeSeries(new ArrayList(this.indices), seriesList);
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

      ArrayList subIndices = new ArrayList(this.indices.subList(lowerArrayIndex, upperArrayIndex));
      return create(subIndices, valuesList, namesList);
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
}
