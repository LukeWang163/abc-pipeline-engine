package base.operators.operator.timeseries.timeseriesanalysis.methods.decomposition;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;
import org.apache.commons.lang3.StringUtils;

public interface MultivariateSeriesDecomposition {
   MultivariateValueSeries compute(MultivariateValueSeries var1);

   MultivariateTimeSeries compute(MultivariateTimeSeries var1);

   default MultivariateValueSeries computeAndFailOnNonFiniteValues(MultivariateValueSeries multivariateValueSeries) {
      if (multivariateValueSeries.hasNaNValues()) {
         throw new SeriesContainsInvalidValuesException(StringUtils.join((Object[])multivariateValueSeries.getSeriesNames(), ","), SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      } else if (multivariateValueSeries.hasInfiniteValues()) {
         throw new SeriesContainsInvalidValuesException(StringUtils.join((Object[])multivariateValueSeries.getSeriesNames(), ","), SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
      } else {
         return this.compute(multivariateValueSeries);
      }
   }

   default MultivariateTimeSeries computeAndFailOnNonFiniteValues(MultivariateTimeSeries multivariateTimeSeries) {
      if (multivariateTimeSeries.hasNaNValues()) {
         throw new SeriesContainsInvalidValuesException(StringUtils.join((Object[])multivariateTimeSeries.getSeriesNames(), ","), SeriesContainsInvalidValuesException.InvalidValuesType.NAN);
      } else if (multivariateTimeSeries.hasInfiniteValues()) {
         throw new SeriesContainsInvalidValuesException(StringUtils.join((Object[])multivariateTimeSeries.getSeriesNames(), ","), SeriesContainsInvalidValuesException.InvalidValuesType.INFINITE);
      } else {
         return this.compute(multivariateTimeSeries);
      }
   }
}
