package base.operators.operator.timeseries.timeseriesanalysis.forecast;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;

public interface ValueSeriesForecastTrainer {
   ValueSeriesForecast trainForecast(ValueSeries var1);

   default ValueSeriesForecast trainForecastAndFailOnNonFiniteValues(ValueSeries valueSeries) {
      if (valueSeries.hasNaNValues()) {
         throw new SeriesContainsInvalidValuesException(valueSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      } else if (valueSeries.hasInfiniteValues()) {
         throw new SeriesContainsInvalidValuesException(valueSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
      } else {
         return this.trainForecast(valueSeries);
      }
   }
}
