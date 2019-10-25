package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;

public interface SeriesTransformation {
   ISeries compute(ISeries var1);
}
