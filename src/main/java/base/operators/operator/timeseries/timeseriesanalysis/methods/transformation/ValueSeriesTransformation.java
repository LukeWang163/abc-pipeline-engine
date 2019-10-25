package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;

public interface ValueSeriesTransformation {
   ValueSeries compute(ValueSeries var1);

   default ValueSeries computeAndFailOnNonFiniteValues(ValueSeries valueSeries) {
      if (valueSeries.hasNaNValues()) {
         throw new SeriesContainsInvalidValuesException(valueSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      } else if (valueSeries.hasInfiniteValues()) {
         throw new SeriesContainsInvalidValuesException(valueSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
      } else {
         return this.compute(valueSeries);
      }
   }
}
