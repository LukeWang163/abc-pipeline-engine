package base.operators.operator.timeseries.timeseriesanalysis.forecast;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;

public interface ValueSeriesForecast {
   MultivariateValueSeries forecast(ValueSeries var1);

   default MultivariateValueSeries forecastAndFailOnNonFiniteValues(ValueSeries valueSeries) {
      if (valueSeries.hasNaNValues()) {
         throw new SeriesContainsInvalidValuesException(valueSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      } else if (valueSeries.hasInfiniteValues()) {
         throw new SeriesContainsInvalidValuesException(valueSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
      } else {
         return this.forecast(valueSeries);
      }
   }
}
