package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ITimeIndexMixedValuesSeries;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class TimeIndexMixedValuesSeries
        extends AbstractSeries<Instant, Object>
        implements ITimeIndexMixedValuesSeries
{
   protected TimeIndexMixedValuesSeries(IndexDimension<Instant> indexDimension, List<SeriesValues<Object>> seriesValuesList) { super(indexDimension, seriesValuesList); }



   protected AbstractSeries<Instant, Object> getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      List<SeriesValues<Object>> subSeriesValuesList = new ArrayList<SeriesValues<Object>>();
      for (SeriesValues<Object> seriesValues : this.seriesValuesList) {
         subSeriesValuesList.add(seriesValues.getSubSeriesValues(lowerArrayIndex, upperArrayIndex));
      }
      return new TimeIndexMixedValuesSeries(this.indexDimension.getSubIndexDimension(lowerArrayIndex, upperArrayIndex), subSeriesValuesList);
   }




   public ISeries<Instant, Object> copy() { return new TimeIndexMixedValuesSeries(this.indexDimension.copy(), getSeriesValuesList()); }
}
