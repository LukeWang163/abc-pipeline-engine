package base.operators.operator.timeseries.timeseriesanalysis.functions;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;

public interface SeriesFunction<I, V> {
   String getName();

   SeriesValues<V> evaluate(IndexDimension<I> paramIndexDimension, String paramString);
}
