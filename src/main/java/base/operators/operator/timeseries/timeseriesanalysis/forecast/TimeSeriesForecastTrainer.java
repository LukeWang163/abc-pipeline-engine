package base.operators.operator.timeseries.timeseriesanalysis.forecast;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;

public interface TimeSeriesForecastTrainer {
   TimeSeriesForecast trainForecast(TimeSeries var1);

   default TimeSeriesForecast trainForecastAndFailOnNonFiniteValues(TimeSeries timeSeries) {
      if (timeSeries.hasNaNValues()) {
         throw new SeriesContainsInvalidValuesException(timeSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      } else if (timeSeries.hasInfiniteValues()) {
         throw new SeriesContainsInvalidValuesException(timeSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
      } else {
         return this.trainForecast(timeSeries);
      }
   }
}
