package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;

public interface SpaceTransformation {
   ValueSeries compute(TimeSeries var1);

   ValueSeries compute(ValueSeries var1);
}
