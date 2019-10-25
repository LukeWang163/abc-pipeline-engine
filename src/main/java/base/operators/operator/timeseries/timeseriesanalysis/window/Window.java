package base.operators.operator.timeseries.timeseriesanalysis.window;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;

public abstract class Window<I>
        extends Object
{
   public static final String WINDOW_SIZE_DESCRIPTOR = "window size";
   protected LastWindowedSeriesContainer lastWindowedSeriesContainer;
   String name;

   class LastWindowedSeriesContainer
   {
      Series windowedSeries;
      int numberOfValues;
      int lowerArrayIndex;
      int upperArrayIndex;
      String nameOfOriginalSeries;

      protected LastWindowedSeriesContainer(Series windowedSeries, int lowerArrayIndex, int upperArrayIndex, String nameOfOriginalSeries) {
         this.windowedSeries = windowedSeries;
         this.lowerArrayIndex = lowerArrayIndex;
         this.upperArrayIndex = upperArrayIndex;
         this.numberOfValues = upperArrayIndex - lowerArrayIndex;
         this.nameOfOriginalSeries = nameOfOriginalSeries;
      }

      public boolean equals(LastWindowedSeriesContainer other) {
         return (this.lowerArrayIndex == other.lowerArrayIndex && this.upperArrayIndex == other.upperArrayIndex && this.numberOfValues == other.numberOfValues && this.nameOfOriginalSeries == other.nameOfOriginalSeries && this.windowedSeries

                 .equals(other.windowedSeries));
      }
   }


   public Window() {
      this.lastWindowedSeriesContainer = null;
      this.name = "Window";
   }

   public Window(String name) {
      this.lastWindowedSeriesContainer = null;
      this.name = name;
   }

   public abstract Series getWindowedSeries(Series paramSeries);


   public abstract MultivariateSeries getWindowedSeries(MultivariateSeries paramMultivariateSeries);


   public abstract ISeries<I, ?> getWindowedSeries(ISeries<I, ?> paramISeries);


   public abstract Window<I> mergeWindows(Window<I> paramWindow, boolean paramBoolean);


   public String getName() { return this.name; }


   public void setName(String name) { this.name = name; }


   public Series getLastWindowedSeries() {
      if (this.lastWindowedSeriesContainer != null) {
         return this.lastWindowedSeriesContainer.windowedSeries;
      }
      return null;
   }



   public LastWindowedSeriesContainer getLastWindowedSeriesContainer() { return this.lastWindowedSeriesContainer; }
}
