package base.operators.operator.timeseries.timeseriesanalysis.forecast;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsEmptyException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public abstract class ForecastModel implements TimeSeriesForecast, ValueSeriesForecast, Serializable {
   private static final long serialVersionUID = -4079359995698080829L;
   public static final String DEFAULT_FORECAST_SERIES_NAME = "Forecast";
   protected int forecastHorizon;
   String forecastSeriesName;

   public int getForecastHorizon() {
      return this.forecastHorizon;
   }

   public void setForecastHorizon(int forecastHorizon) {
      if (forecastHorizon < 0) {
         throw new IllegalIndexArgumentException("forecast horizon", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else {
         this.forecastHorizon = forecastHorizon;
      }
   }

   public void setForecastSeriesName(String forecastSeriesName) {
      if (forecastSeriesName == null) {
         throw new ArgumentIsNullException("forecast series name");
      } else if (forecastSeriesName.isEmpty()) {
         throw new ArgumentIsEmptyException("forecast series name");
      } else {
         this.forecastSeriesName = forecastSeriesName;
      }
   }

   protected double[] getIndicesForForecastedValues(ValueSeries valueSeries, int length) {
      double lastIndex = valueSeries.getIndex(valueSeries.getLength() - 1);
      double secondLastIndex = valueSeries.getIndex(valueSeries.getLength() - 2);
      double stepSize = lastIndex - secondLastIndex;
      double[] result = new double[length];

      for(int i = 0; i < length; ++i) {
         result[i] = lastIndex + (double)(i + 1) * stepSize;
      }

      return result;
   }

   protected ArrayList getIndicesForForecastedValues(TimeSeries timeSeries, int length) {
      Instant lastIndex = timeSeries.getIndex(timeSeries.getLength() - 1);
      Instant secondLastIndex = timeSeries.getIndex(timeSeries.getLength() - 2);
      Duration stepSize = Duration.between(secondLastIndex, lastIndex);
      ArrayList result = new ArrayList();

      for(int i = 0; i < length; ++i) {
         result.add(lastIndex.plus(stepSize.multipliedBy((long)i + 1L)));
      }

      return result;
   }

   public String getForecastSeriesName() {
      return this.forecastSeriesName;
   }

   public abstract ForecastModel copy();
}
