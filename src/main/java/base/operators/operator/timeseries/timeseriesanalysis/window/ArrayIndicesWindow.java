package base.operators.operator.timeseries.timeseriesanalysis.window;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IndexArgumentsDontMatchException;


public class ArrayIndicesWindow<I>
        extends Window<I>
{
   private int leftEdge;
   private int rightEdge;
   private int windowSize;

   public ArrayIndicesWindow(int leftEdge, int rightEdge) { setEdges(leftEdge, rightEdge); }


   public ArrayIndicesWindow(int leftEdge, int rightEdge, String name) {
      super(name);
      setEdges(leftEdge, rightEdge);
   }

   private boolean checkEdges(int leftEdge, int rightEdge) {
      if (leftEdge < 0) {
         throw new IllegalIndexArgumentException("left edge", Integer.valueOf(leftEdge), IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      }
      if (rightEdge < 0) {
         throw new IllegalIndexArgumentException("right edge", Integer.valueOf(rightEdge), IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      }
      if (rightEdge < leftEdge) {
         throw new IndexArgumentsDontMatchException("right edge", "left edge", Integer.valueOf(rightEdge), Integer.valueOf(leftEdge), IndexArgumentsDontMatchException.MisMatchType.SMALLER);
      }
      if (rightEdge == Integer.MAX_VALUE) {
         throw new IllegalArgumentException("Provided rightEdge is equal to Integer.MAX_VALUE. This is not supported.");
      }
      return true;
   }

   public Series getWindowedSeries(Series wholeSeries) {
      if (this.leftEdge > wholeSeries.getLength() - 1) {
         return null;
      }
      int rightEdgeForCurrentSeries = this.rightEdge;



      if (this.rightEdge > wholeSeries.getLength()) {
         rightEdgeForCurrentSeries = wholeSeries.getLength();
      }
      Series windowedSeries = wholeSeries.getSubSeries(this.leftEdge, rightEdgeForCurrentSeries);
      this
              .lastWindowedSeriesContainer = new Window.LastWindowedSeriesContainer(windowedSeries, this.leftEdge, rightEdgeForCurrentSeries, wholeSeries.getName());
      return windowedSeries;
   }

   public MultivariateSeries getWindowedSeries(MultivariateSeries wholeSeries) {
      if (this.leftEdge > wholeSeries.getLength() - 1) {
         return null;
      }
      int rightEdgeForCurrentSeries = this.rightEdge;



      if (this.rightEdge > wholeSeries.getLength()) {
         rightEdgeForCurrentSeries = wholeSeries.getLength();
      }
      return wholeSeries.getSubMultivariateSeries(this.leftEdge, rightEdgeForCurrentSeries);
   }

   public ISeries<I, ?> getWindowedSeries(ISeries<I, ?> wholeSeries) {
      if (this.leftEdge > wholeSeries.getLength() - 1) {
         return null;
      }
      int rightEdgeForCurrentSeries = this.rightEdge;



      if (this.rightEdge > wholeSeries.getLength()) {
         rightEdgeForCurrentSeries = wholeSeries.getLength();
      }
      return wholeSeries.getSubSeries(this.leftEdge, rightEdgeForCurrentSeries);
   }


   public Window<I> mergeWindows(Window<I> otherWindow, boolean checkOverlap) {
      if (!(otherWindow instanceof ArrayIndicesWindow)) {
         throw new IllegalArgumentException("Provided window is not a ArrayIndicesWindow. Class of provided window: " + otherWindow
                 .getClass().getName());
      }
      ArrayIndicesWindow<I> otherArrayIndicesWindow = (ArrayIndicesWindow)otherWindow;
      int otherLeftEdge = otherArrayIndicesWindow.getLeftEdge();
      int otherRightEdge = otherArrayIndicesWindow.getRightEdge();
      boolean throwException = false;
      if (checkOverlap) {
         if (this.leftEdge <= otherLeftEdge && this.rightEdge < otherLeftEdge) {
            throwException = true;
         }
         if (otherLeftEdge < this.leftEdge && otherRightEdge < this.leftEdge) {
            throwException = true;
         }
      }
      if (throwException) {
         throw new WindowsDontOverlapException(Integer.toString(this.leftEdge), Integer.toString(this.rightEdge),
                 Integer.toString(otherLeftEdge), Integer.toString(otherRightEdge));
      }
      return new ArrayIndicesWindow(Math.min(otherLeftEdge, this.leftEdge), Math.max(otherRightEdge, this.rightEdge));
   }



   public void setLeftEdge(int leftEdge) { setEdges(leftEdge, this.rightEdge); }

   public void setRightEdge(int rightEdge) { setEdges(this.leftEdge, rightEdge); }


   public void setEdges(int leftEdge, int rightEdge) {
      checkEdges(leftEdge, rightEdge);
      this.leftEdge = leftEdge;
      this.rightEdge = rightEdge;
      this.windowSize = rightEdge - leftEdge;
   }



   public int getRightEdge() { return this.rightEdge; }



   public int getLeftEdge() { return this.leftEdge; }


   public int getWindowSize() { return this.windowSize; }



   public boolean equals(ArrayIndicesWindow<I> otherWindow) { return equals(otherWindow, true); }


   public boolean equals(ArrayIndicesWindow<I> otherWindow, boolean checkLastWindowedSeries) {
      if (this.leftEdge != otherWindow.getLeftEdge() || this.rightEdge != otherWindow.getRightEdge()) {
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
}