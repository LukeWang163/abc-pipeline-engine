package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealIndexMixedValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import java.util.ArrayList;
import java.util.List;


public class RealIndexMixedValuesSeries
        extends AbstractSeries<Double, Object>
        implements IRealIndexMixedValuesSeries
{
   protected RealIndexMixedValuesSeries(IndexDimension<Double> indexDimension, List<SeriesValues<Object>> seriesValuesList) { super(indexDimension, seriesValuesList); }



   protected AbstractSeries<Double, Object> getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      List<SeriesValues<Object>> subSeriesValuesList = new ArrayList<SeriesValues<Object>>();
      for (SeriesValues<Object> seriesValues : this.seriesValuesList) {
         subSeriesValuesList.add(seriesValues.getSubSeriesValues(lowerArrayIndex, upperArrayIndex));
      }
      return new RealIndexMixedValuesSeries(this.indexDimension.getSubIndexDimension(lowerArrayIndex, upperArrayIndex), subSeriesValuesList);
   }




   public ISeries<Double, Object> copy() { return new RealIndexMixedValuesSeries(this.indexDimension.copy(), getSeriesValuesList()); }
}
