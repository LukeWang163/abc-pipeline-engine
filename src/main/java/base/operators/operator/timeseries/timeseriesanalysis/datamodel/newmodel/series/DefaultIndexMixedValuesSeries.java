package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IDefaultIndexMixedValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import java.util.ArrayList;
import java.util.List;


public class DefaultIndexMixedValuesSeries
        extends AbstractSeries<Integer, Object>
        implements IDefaultIndexMixedValuesSeries
{
   protected DefaultIndexMixedValuesSeries(IndexDimension<Integer> indexDimension, List<SeriesValues<Object>> seriesValuesList) { super(indexDimension, seriesValuesList); }



   protected AbstractSeries<Integer, Object> getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      List<SeriesValues<Object>> subSeriesValuesList = new ArrayList<SeriesValues<Object>>();
      for (SeriesValues<Object> seriesValues : this.seriesValuesList) {
         subSeriesValuesList.add(seriesValues.getSubSeriesValues(lowerArrayIndex, upperArrayIndex));
      }
      return new DefaultIndexMixedValuesSeries(this.indexDimension.getSubIndexDimension(lowerArrayIndex, upperArrayIndex), subSeriesValuesList);
   }




   public ISeries<Integer, Object> copy() { return new DefaultIndexMixedValuesSeries(this.indexDimension.copy(), getSeriesValuesList()); }
}
