package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ITimeIndexNominalValuesSeries;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class TimeIndexNominalValuesSeries
        extends AbstractSeries<Instant, String>
        implements ITimeIndexNominalValuesSeries
{
   protected TimeIndexNominalValuesSeries(IndexDimension<Instant> indexDimension, List<SeriesValues<String>> seriesValuesList) { super(indexDimension, seriesValuesList); }



   protected AbstractSeries<Instant, String> getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      List<SeriesValues<String>> subSeriesValuesList = new ArrayList<SeriesValues<String>>();
      for (SeriesValues<String> seriesValues : this.seriesValuesList) {
         subSeriesValuesList.add(seriesValues.getSubSeriesValues(lowerArrayIndex, upperArrayIndex));
      }
      return new TimeIndexNominalValuesSeries(this.indexDimension.getSubIndexDimension(lowerArrayIndex, upperArrayIndex), subSeriesValuesList);
   }




   public ISeries<Instant, String> copy() { return new TimeIndexNominalValuesSeries(this.indexDimension.copy(), getSeriesValuesList()); }
}
