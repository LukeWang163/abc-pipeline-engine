package base.operators.operator.timeseries.timeseriesanalysis.methods.decomposition;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;

public interface SeriesDecomposition {
   MultivariateValueSeries compute(ValueSeries var1);

   MultivariateTimeSeries compute(TimeSeries var1);

   default MultivariateValueSeries computeAndFailOnNonFiniteValues(ValueSeries valueSeries) {
      if (valueSeries.hasNaNValues()) {
         throw new SeriesContainsInvalidValuesException(valueSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      } else if (valueSeries.hasInfiniteValues()) {
         throw new SeriesContainsInvalidValuesException(valueSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
      } else {
         return this.compute(valueSeries);
      }
   }

   default MultivariateTimeSeries computeAndFailOnNonFiniteValues(TimeSeries timeSeries) {
      if (timeSeries.hasNaNValues()) {
         throw new SeriesContainsInvalidValuesException(timeSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      } else if (timeSeries.hasInfiniteValues()) {
         throw new SeriesContainsInvalidValuesException(timeSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
      } else {
         return this.compute(timeSeries);
      }
   }
}
