package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;

interface MultivariateSpaceTransformation {
   MultivariateValueSeries compute(TimeSeries var1);

   MultivariateValueSeries compute(ValueSeries var1);
}
