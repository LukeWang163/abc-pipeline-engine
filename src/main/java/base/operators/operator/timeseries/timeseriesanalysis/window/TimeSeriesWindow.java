package base.operators.operator.timeseries.timeseriesanalysis.window;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import java.time.Duration;
import java.time.Instant;

public class TimeSeriesWindow
        extends Window<Instant>
{
   private Instant leftEdge;
   private Instant rightEdge;
   private Duration windowSize;

   public TimeSeriesWindow(Instant leftEdge, Instant rightEdge) { setEdges(leftEdge, rightEdge); }


   public TimeSeriesWindow(Instant leftEdge, Instant rightEdge, String name) {
      super(name);
      setEdges(leftEdge, rightEdge);
   }

   private boolean checkEdges(Instant leftEdge, Instant rightEdge) {
      if (leftEdge == null) {
         throw new ArgumentIsNullException("left edge");
      }
      if (rightEdge == null) {
         throw new ArgumentIsNullException("right edge");
      }
      if (!rightEdge.isAfter(leftEdge)) {
         throw new IllegalArgumentException("Provided rightEdge is earlier than leftEdge. This is not supported for a TimeSeriesWindow. Provided leftEdge/rightEdge: " + leftEdge

                 .toString() + " / " + rightEdge.toString());
      }
      return true;
   }


   public Series getWindowedSeries(Series wholeSeries) {
      if (!(wholeSeries instanceof TimeSeries)) {
         throw new IllegalArgumentException("Provided series is not a TimeSeries. Class of provided series: " + wholeSeries
                 .getClass().getName());
      }

      TimeSeries wholeTimeSeries = (TimeSeries)wholeSeries;
      int arrayIndexOfLeftEdge = wholeTimeSeries.getUpperArrayIndex(this.leftEdge);
      int arrayIndexOfRightEdge = wholeTimeSeries.getLowerArrayIndex(this.rightEdge);




      if (arrayIndexOfLeftEdge == Integer.MAX_VALUE || arrayIndexOfRightEdge == Integer.MIN_VALUE) {
         return null;
      }

      if (this.rightEdge.isAfter(wholeTimeSeries.getIndex(arrayIndexOfRightEdge))) {
         arrayIndexOfRightEdge++;
      }

      TimeSeries windowedSeries = (TimeSeries)wholeTimeSeries.getSubSeries(arrayIndexOfLeftEdge, arrayIndexOfRightEdge);
      this
              .lastWindowedSeriesContainer = new Window.LastWindowedSeriesContainer(windowedSeries, arrayIndexOfLeftEdge, arrayIndexOfRightEdge, wholeTimeSeries.getName());

      return windowedSeries;
   }


   public MultivariateSeries getWindowedSeries(MultivariateSeries wholeSeries) {
      if (!(wholeSeries instanceof MultivariateTimeSeries)) {
         throw new IllegalArgumentException("Provided multivariateSeries is not a MultivariateTimeSeries. Class of provided multivariateSeries: " + wholeSeries

                 .getClass().getName());
      }

      MultivariateTimeSeries wholeTimeSeries = (MultivariateTimeSeries)wholeSeries;
      int arrayIndexOfLeftEdge = wholeTimeSeries.getUpperArrayIndex(this.leftEdge);
      int arrayIndexOfRightEdge = wholeTimeSeries.getLowerArrayIndex(this.rightEdge);




      if (arrayIndexOfLeftEdge == Integer.MAX_VALUE || arrayIndexOfRightEdge == Integer.MIN_VALUE) {
         return null;
      }


      if (this.rightEdge.isAfter((Instant)wholeTimeSeries.getIndices().get(arrayIndexOfRightEdge))) {
         arrayIndexOfRightEdge++;
      }

      return wholeTimeSeries.getSubMultivariateSeries(arrayIndexOfLeftEdge, arrayIndexOfRightEdge);
   }


   public ISeries<Instant, ?> getWindowedSeries(ISeries<Instant, ?> wholeSeries) {
      int arrayIndexOfLeftEdge = wholeSeries.getUpperArrayIndex(this.leftEdge);
      int arrayIndexOfRightEdge = wholeSeries.getLowerArrayIndex(this.rightEdge);




      if (arrayIndexOfLeftEdge == Integer.MAX_VALUE || arrayIndexOfRightEdge == Integer.MIN_VALUE) {
         return null;
      }



      if (this.rightEdge.isAfter((Instant)wholeSeries.getIndexValue(arrayIndexOfRightEdge))) {
         arrayIndexOfRightEdge++;
      }

      return wholeSeries.getSubSeries(arrayIndexOfLeftEdge, arrayIndexOfRightEdge);
   }


   public Window<Instant> mergeWindows(Window<Instant> otherWindow, boolean checkOverlap) {
      if (!(otherWindow instanceof TimeSeriesWindow)) {
         throw new IllegalArgumentException("Provided window is not a TimeSeriesWindow. Class of provided window: " + otherWindow
                 .getClass().getName());
      }
      TimeSeriesWindow otherArrayIndicesWindow = (TimeSeriesWindow)otherWindow;
      Instant otherLeftEdge = otherArrayIndicesWindow.getLeftEdge();
      Instant otherRightEdge = otherArrayIndicesWindow.getRightEdge();
      boolean throwException = false;
      if (checkOverlap) {
         if ((this.leftEdge.isBefore(otherLeftEdge) || this.leftEdge == otherLeftEdge) && this.rightEdge.isBefore(otherLeftEdge)) {
            throwException = true;
         }
         if (otherLeftEdge.isBefore(this.leftEdge) && otherRightEdge.isBefore(this.leftEdge)) {
            throwException = true;
         }
      }
      if (throwException) {
         throw new WindowsDontOverlapException(this.leftEdge.toString(), this.rightEdge.toString(), otherLeftEdge.toString(), otherRightEdge
                 .toString());
      }
      Instant newLeftEdge = this.leftEdge;
      if (otherLeftEdge.isBefore(this.leftEdge)) {
         newLeftEdge = otherLeftEdge;
      }
      Instant newRightEdge = this.rightEdge;
      if (otherRightEdge.isAfter(this.rightEdge)) {
         newRightEdge = otherRightEdge;
      }
      return new TimeSeriesWindow(newLeftEdge, newRightEdge);
   }







   public void setEdges(Instant leftEdge, Instant rightEdge) {
      checkEdges(leftEdge, rightEdge);
      this.leftEdge = leftEdge;
      this.rightEdge = rightEdge;
      this.windowSize = Duration.between(leftEdge, rightEdge);
   }






   public Instant getLeftEdge() { return this.leftEdge; }







   public void setLeftEdge(Instant leftEdge) { setEdges(leftEdge, this.rightEdge); }







   public Instant getRightEdge() { return this.rightEdge; }







   public void setRightEdge(Instant rightEdge) { setEdges(this.leftEdge, rightEdge); }



   public boolean equals(TimeSeriesWindow otherWindow) { return equals(otherWindow, true); }









   public boolean equals(TimeSeriesWindow otherWindow, boolean checkLastWindowedSeries) {
      if (!this.leftEdge.equals(otherWindow.getLeftEdge()) || !this.rightEdge.equals(otherWindow.getRightEdge())) {
         return false;
      }
      if (checkLastWindowedSeries) {
         if (getLastWindowedSeriesContainer() == null && otherWindow.getLastWindowedSeriesContainer() == null)
            return true;
         if (getLastWindowedSeriesContainer() != null && otherWindow.getLastWindowedSeriesContainer() != null) {
            return getLastWindowedSeriesContainer().equals(otherWindow.getLastWindowedSeriesContainer());
         }
         return false;
      }

      return true;
   }


   public Duration getWindowSize() { return this.windowSize; }
}
