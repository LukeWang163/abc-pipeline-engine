package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;

public interface SpaceTransformationOnMultivariateSeries {
   MultivariateValueSeries compute(MultivariateValueSeries var1);

   MultivariateValueSeries compute(MultivariateTimeSeries var1);
}
