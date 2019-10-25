package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;

public interface TimeSeriesTransformation {
   TimeSeries compute(TimeSeries var1);

   default TimeSeries computeAndFailOnNonFiniteValues(TimeSeries timeSeries) {
      if (timeSeries.hasNaNValues()) {
         throw new SeriesContainsInvalidValuesException(timeSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      } else if (timeSeries.hasInfiniteValues()) {
         throw new SeriesContainsInvalidValuesException(timeSeries.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
      } else {
         return this.compute(timeSeries);
      }
   }
}
