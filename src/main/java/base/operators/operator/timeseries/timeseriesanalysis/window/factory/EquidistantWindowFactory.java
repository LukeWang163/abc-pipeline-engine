package base.operators.operator.timeseries.timeseriesanalysis.window.factory;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.window.ArrayIndicesWindow;
import base.operators.operator.timeseries.timeseriesanalysis.window.TimeSeriesWindow;
import base.operators.operator.timeseries.timeseriesanalysis.window.ValueSeriesWindow;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class EquidistantWindowFactory {
   public ArrayList valueSeriesWindows(double start, double windowSize, int numberOfWindows) {
      if (numberOfWindows <= 0) {
         throw new IllegalIndexArgumentException("number of windows", numberOfWindows, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else if (!Double.isNaN(start) && !Double.isNaN(windowSize)) {
         if (windowSize <= 0.0D) {
            throw new IllegalIndexArgumentException("window size", windowSize, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
         } else {
            ArrayList resultList = new ArrayList();
            double leftEdge = start;

            for(int i = 0; i < numberOfWindows; ++i) {
               resultList.add(new ValueSeriesWindow(leftEdge, leftEdge + windowSize));
               leftEdge += windowSize;
            }

            return resultList;
         }
      } else {
         throw new IllegalArgumentException("Provided start and/or windowSize value is NaN. start/windowSize: " + start + "/" + windowSize);
      }
   }

   public ArrayList arrayIndicesWindows(int start, int windowSize, int numberOfWindows) {
      if (numberOfWindows <= 0) {
         throw new IllegalIndexArgumentException("number of windows", numberOfWindows, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else if (start != Integer.MAX_VALUE && windowSize != Integer.MAX_VALUE) {
         if (start < 0) {
            throw new IllegalIndexArgumentException("start value", start, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
         } else if (windowSize <= 0) {
            throw new IllegalIndexArgumentException("window size", windowSize, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
         } else {
            ArrayList resultList = new ArrayList();
            int leftEdge = start;

            for(int i = 0; i < numberOfWindows; ++i) {
               resultList.add(new ArrayIndicesWindow(leftEdge, leftEdge + windowSize));
               leftEdge += windowSize;
            }

            return resultList;
         }
      } else {
         throw new IllegalArgumentException("Provided start and/or windowSize value is Integer.MAX_VALUE. start/windowSize: " + start + "/" + windowSize);
      }
   }

   public ArrayList timeSeriesWindows(Instant start, Duration windowSize, int numberOfWindows) {
      if (numberOfWindows <= 0) {
         throw new IllegalIndexArgumentException("number of windows", numberOfWindows, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else if (start == null) {
         throw new ArgumentIsNullException("start value");
      } else if (windowSize == null) {
         throw new ArgumentIsNullException("window size");
      } else if (!windowSize.isZero() && !windowSize.isNegative()) {
         ArrayList resultList = new ArrayList();
         Instant leftEdge = start;

         for(int i = 0; i < numberOfWindows; ++i) {
            resultList.add(new TimeSeriesWindow(leftEdge, leftEdge.plus(windowSize)));
            leftEdge = leftEdge.plus(windowSize);
         }

         return resultList;
      } else {
         throw new IllegalIndexArgumentException("window size", (double)windowSize.getSeconds() + (double)windowSize.getNano() / 1000000.0D, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      }
   }

   public ArrayList fixedNumber(Series series, int numberOfWindows) {
      if (series == null) {
         throw new ArgumentIsNullException("series");
      } else if (series instanceof ValueSeries) {
         ValueSeries valueSeries = (ValueSeries)series;
         return this.fixedNumber(valueSeries.getIndex(0), valueSeries.getIndex(valueSeries.getLength() - 1), numberOfWindows);
      } else if (series instanceof TimeSeries) {
         TimeSeries timeSeries = (TimeSeries)series;
         return this.fixedNumber(timeSeries.getIndex(0), timeSeries.getIndex(timeSeries.getLength() - 1), numberOfWindows);
      } else {
         throw new IllegalArgumentException("equidistantWindowsFixedNumber does not support the provided Series class: " + series.getClass().getName());
      }
   }

   public ArrayList fixedNumber(double start, double stop, int numberOfWindows) {
      if (numberOfWindows <= 0) {
         throw new IllegalIndexArgumentException("number of windows", numberOfWindows, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else {
         double windowSize = (stop - start) / (double)numberOfWindows;
         return this.valueSeriesWindows(start, windowSize, numberOfWindows);
      }
   }

   public ArrayList fixedNumber(int start, int stop, int numberOfWindows, boolean windowsWithin) {
      if (numberOfWindows <= 0) {
         throw new IllegalIndexArgumentException("number of windows", numberOfWindows, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else {
         int windowSize = (stop - start) / numberOfWindows;
         if (!windowsWithin) {
            ++windowSize;
         }

         return this.arrayIndicesWindows(start, windowSize, numberOfWindows);
      }
   }

   public ArrayList fixedNumber(Instant start, Instant stop, int numberOfWindows) {
      if (numberOfWindows <= 0) {
         throw new IllegalIndexArgumentException("number of windows", numberOfWindows, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else {
         Duration windowSize = Duration.between(start, stop).dividedBy((long)numberOfWindows);
         return this.timeSeriesWindows(start, windowSize, numberOfWindows);
      }
   }

   public ArrayList fixedSize(ValueSeries valueSeries, double windowSize, boolean windowsWithin) {
      if (valueSeries == null) {
         throw new ArgumentIsNullException("value series");
      } else {
         return this.fixedSize(valueSeries.getIndex(0), valueSeries.getIndex(valueSeries.getLength() - 1), windowSize, windowsWithin);
      }
   }

   public ArrayList arrayIndicesWindowsFixedSize(Series series, int windowSize, boolean windowsWithin) {
      if (series == null) {
         throw new ArgumentIsNullException("series");
      } else {
         return this.fixedSize(0, series.getLength() - 1, windowSize, windowsWithin);
      }
   }

   public ArrayList fixedSize(TimeSeries timeSeries, Duration windowSize, boolean windowsWithin) {
      if (timeSeries == null) {
         throw new ArgumentIsNullException("time series");
      } else {
         return this.fixedSize(timeSeries.getIndex(0), timeSeries.getIndex(timeSeries.getLength() - 1), windowSize, windowsWithin);
      }
   }

   public ArrayList fixedSize(double start, double stop, double windowSize, boolean windowsWithin) {
      if (windowSize <= 0.0D) {
         throw new IllegalIndexArgumentException("window size", windowSize, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else {
         int numberOfWindows = (int)((stop - start) / windowSize);
         if (!windowsWithin) {
            ++numberOfWindows;
         }

         return this.valueSeriesWindows(start, windowSize, numberOfWindows);
      }
   }

   public ArrayList fixedSize(int start, int stop, int windowSize, boolean windowsWithin) {
      if (windowSize <= 0) {
         throw new IllegalIndexArgumentException("window size", windowSize, IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else {
         int numberOfWindows = (stop - start) / windowSize;
         if (!windowsWithin) {
            ++numberOfWindows;
         }

         return this.arrayIndicesWindows(start, windowSize, numberOfWindows);
      }
   }

   public ArrayList fixedSize(Instant start, Instant stop, Duration windowSize, boolean windowsWithin) {
      if (!windowSize.isZero() && !windowSize.isNegative()) {
         long durationMillis = Duration.between(start, stop).toMillis();
         long windowSizeMillis = windowSize.toMillis();
         int numberOfWindows = (int)(durationMillis / windowSizeMillis);
         if (!windowsWithin) {
            ++numberOfWindows;
         }

         return this.timeSeriesWindows(start, windowSize, numberOfWindows);
      } else {
         throw new IllegalIndexArgumentException("window size", windowSize.getSeconds(), IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      }
   }
}
