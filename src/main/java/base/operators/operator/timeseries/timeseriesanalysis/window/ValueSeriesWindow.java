package base.operators.operator.timeseries.timeseriesanalysis.window;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IndexArgumentsDontMatchException;


public class ValueSeriesWindow
        extends Window<Double>
{
   private double leftEdge;
   private double rightEdge;
   private double windowSize;

   public ValueSeriesWindow(double leftEdge, double rightEdge) { setEdges(leftEdge, rightEdge); }



   public ValueSeriesWindow(double leftEdge, double rightEdge, String name) {
      super(name);
      setEdges(leftEdge, rightEdge);
   }

   private boolean checkEdges(double leftEdge, double rightEdge) {
      if (Double.isNaN(leftEdge) || Double.isNaN(rightEdge)) {
         throw new IllegalArgumentException("Provided leftEdge and/or rightEdge is Double.NaN. This is not supported for a ValueSeriesWindow. Provided leftEdge/rightEdge: " + leftEdge + "/" + rightEdge);
      }


      if (rightEdge < leftEdge) {
         throw new IndexArgumentsDontMatchException("right edge", "left edge", Double.valueOf(rightEdge), Double.valueOf(leftEdge), IndexArgumentsDontMatchException.MisMatchType.SMALLER);
      }
      if (rightEdge == Double.POSITIVE_INFINITY) {
         throw new IllegalArgumentException("Provided rightEdge is equal to Double.POSITIVE_INFINITY. This is not supported.");
      }

      if (leftEdge == Double.NEGATIVE_INFINITY) {
         throw new IllegalArgumentException("Provided leftEdge is equal to Double.NEGATIVE_INFINITY. This is not supported.");
      }

      return true;
   }


   public Series getWindowedSeries(Series wholeSeries) {
      if (!(wholeSeries instanceof ValueSeries)) {
         throw new IllegalArgumentException("Provided series is not a ValueSeries. Class of provided series: " + wholeSeries
                 .getClass().getName());
      }

      ValueSeries wholeValueSeries = (ValueSeries)wholeSeries;
      int arrayIndexOfLeftEdge = wholeValueSeries.getUpperArrayIndex(this.leftEdge);
      int arrayIndexOfRightEdge = wholeValueSeries.getLowerArrayIndex(this.rightEdge);




      if (arrayIndexOfLeftEdge == Integer.MAX_VALUE || arrayIndexOfRightEdge == Integer.MIN_VALUE) {
         return null;
      }


      if (this.rightEdge > wholeValueSeries.getIndex(arrayIndexOfRightEdge)) {
         arrayIndexOfRightEdge++;
      }

      ValueSeries windowedSeries = (ValueSeries)wholeValueSeries.getSubSeries(arrayIndexOfLeftEdge, arrayIndexOfRightEdge);


      this
              .lastWindowedSeriesContainer = new Window.LastWindowedSeriesContainer(windowedSeries, arrayIndexOfLeftEdge, arrayIndexOfRightEdge, wholeValueSeries.getName());

      return windowedSeries;
   }


   public MultivariateSeries getWindowedSeries(MultivariateSeries wholeSeries) {
      if (!(wholeSeries instanceof MultivariateValueSeries)) {
         throw new IllegalArgumentException("Provided multivariateSeries is not a MultivariateValueSeries. Class of provided multivariateSeries: " + wholeSeries

                 .getClass().getName());
      }

      MultivariateValueSeries wholeValueSeries = (MultivariateValueSeries)wholeSeries;
      int arrayIndexOfLeftEdge = wholeValueSeries.getUpperArrayIndex(this.leftEdge);
      int arrayIndexOfRightEdge = wholeValueSeries.getLowerArrayIndex(this.rightEdge);




      if (arrayIndexOfLeftEdge == Integer.MAX_VALUE || arrayIndexOfRightEdge == Integer.MIN_VALUE) {
         return null;
      }


      if (this.rightEdge > wholeValueSeries.getIndices()[arrayIndexOfRightEdge]) {
         arrayIndexOfRightEdge++;
      }

      return wholeValueSeries.getSubMultivariateSeries(arrayIndexOfLeftEdge, arrayIndexOfRightEdge);
   }


   public ISeries<Double, ?> getWindowedSeries(ISeries<Double, ?> wholeSeries) {
      int arrayIndexOfLeftEdge = wholeSeries.getUpperArrayIndex(Double.valueOf(this.leftEdge));
      int arrayIndexOfRightEdge = wholeSeries.getLowerArrayIndex(Double.valueOf(this.rightEdge));




      if (arrayIndexOfLeftEdge == Integer.MAX_VALUE || arrayIndexOfRightEdge == Integer.MIN_VALUE) {
         return null;
      }



      if (this.rightEdge > ((Double)wholeSeries.getIndexValue(arrayIndexOfRightEdge)).doubleValue()) {
         arrayIndexOfRightEdge++;
      }

      return wholeSeries.getSubSeries(arrayIndexOfLeftEdge, arrayIndexOfRightEdge);
   }


   public Window<Double> mergeWindows(Window<Double> otherWindow, boolean checkOverlap) {
      if (!(otherWindow instanceof ValueSeriesWindow)) {
         throw new IllegalArgumentException("Provided window is not a ValueSeriesWindow. Class of provided window: " + otherWindow
                 .getClass().getName());
      }
      ValueSeriesWindow otherArrayIndicesWindow = (ValueSeriesWindow)otherWindow;
      double otherLeftEdge = otherArrayIndicesWindow.getLeftEdge();
      double otherRightEdge = otherArrayIndicesWindow.getRightEdge();
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
         throw new WindowsDontOverlapException(Double.toString(this.leftEdge), Double.toString(this.rightEdge),
                 Double.toString(otherLeftEdge), Double.toString(otherRightEdge));
      }
      return new ValueSeriesWindow(Math.min(otherLeftEdge, this.leftEdge), Math.max(otherRightEdge, this.rightEdge));
   }

   public void setEdges(double leftEdge, double rightEdge) {
      checkEdges(leftEdge, rightEdge);
      this.leftEdge = leftEdge;
      this.rightEdge = rightEdge;
      this.windowSize = rightEdge - leftEdge;
   }






   public double getLeftEdge() { return this.leftEdge; }







   public void setLeftEdge(double leftEdge) { setEdges(leftEdge, this.rightEdge); }







   public double getRightEdge() { return this.rightEdge; }







   public void setRightEdge(double rightEdge) { setEdges(this.leftEdge, rightEdge); }







   public double getWindowSize() { return this.windowSize; }



   public boolean equals(ValueSeriesWindow otherWindow) { return equals(otherWindow, true); }


   public boolean equals(ValueSeriesWindow otherWindow, boolean checkLastWindowedSeries) {
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
