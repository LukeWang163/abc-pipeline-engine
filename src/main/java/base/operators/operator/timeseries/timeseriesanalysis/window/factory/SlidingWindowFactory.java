package base.operators.operator.timeseries.timeseriesanalysis.window.factory;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.window.ArrayIndicesWindow;
import java.util.ArrayList;
import java.util.List;

public class SlidingWindowFactory {
   private List fromSeriesLength(int length, int windowSize, int stepSize, int horizonWindow, int horizonOffset) {
      if (windowSize <= 0) {
         throw new IllegalIndexArgumentException("window size", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else if (windowSize + horizonWindow + horizonOffset > length) {
         throw new IllegalArgumentException("Provided windowSize + horizonWindow + horizonOffset is larger than length of series.");
      } else if (stepSize <= 0) {
         throw new IllegalIndexArgumentException("step size", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else if (horizonWindow < 0) {
         throw new IllegalIndexArgumentException("horizon window", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (horizonOffset < 0) {
         throw new IllegalIndexArgumentException("horizon offset", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         ArrayList resultList = new ArrayList();

         for(int i = 0; i <= length - windowSize - horizonOffset - horizonWindow; i += stepSize) {
            resultList.add(new ArrayIndicesWindow(i, i + windowSize, "Training"));
            if (horizonWindow > 0) {
               resultList.add(new ArrayIndicesWindow(i + windowSize + horizonOffset, i + windowSize + horizonOffset + horizonWindow, "Horizon"));
            }
         }

         return resultList;
      }
   }

   public List fromISeries(ISeries series, int windowSize, int stepSize, int horizonWindow, int horizonOffset) {
      if (series == null) {
         throw new ArgumentIsNullException("series");
      } else {
         return this.fromSeriesLength(series.getLength(), windowSize, stepSize, horizonWindow, horizonOffset);
      }
   }

   public List fromSeries(Series series, int windowSize, int stepSize, int horizonWindow, int horizonOffset) {
      if (series == null) {
         throw new ArgumentIsNullException("series");
      } else {
         return this.fromSeriesLength(series.getLength(), windowSize, stepSize, horizonWindow, horizonOffset);
      }
   }

   public List fromISeries(ISeries series, int windowSize, int stepSize) {
      return this.fromISeries(series, windowSize, stepSize, 0, 0);
   }

   public List fromSeries(Series series, int windowSize, int stepSize) {
      return this.fromSeries(series, windowSize, stepSize, 0, 0);
   }

   public List fromISeriesHorizonOne(ISeries series, int windowSize, int stepSize) {
      return this.fromISeries(series, windowSize, stepSize, 1, 0);
   }

   public List fromSeriesHorizonOne(Series series, int windowSize, int stepSize) {
      return this.fromSeries(series, windowSize, stepSize, 1, 0);
   }

   public List fromMultivariateSeries(MultivariateSeries multivariateSeries, int windowSize, int stepSize, int horizonWindow, int horizonOffset) {
      if (multivariateSeries == null) {
         throw new ArgumentIsNullException("multivariate series");
      } else {
         return this.fromSeriesLength(multivariateSeries.getLength(), windowSize, stepSize, horizonWindow, horizonOffset);
      }
   }
}
