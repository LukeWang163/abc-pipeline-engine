package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ITimeIndexRealValuesSeries;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class TimeIndexRealValuesSeries
        extends AbstractSeries<Instant, Double>
        implements ITimeIndexRealValuesSeries
{
   protected TimeIndexRealValuesSeries(IndexDimension<Instant> indexDimension, List<SeriesValues<Double>> seriesValuesList) { super(indexDimension, seriesValuesList); }



   protected AbstractSeries<Instant, Double> getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      List<SeriesValues<Double>> subSeriesValuesList = new ArrayList<SeriesValues<Double>>();
      for (SeriesValues<Double> seriesValues : this.seriesValuesList) {
         subSeriesValuesList.add(seriesValues.getSubSeriesValues(lowerArrayIndex, upperArrayIndex));
      }
      return new TimeIndexRealValuesSeries(this.indexDimension.getSubIndexDimension(lowerArrayIndex, upperArrayIndex), subSeriesValuesList);
   }




   public ISeries<Instant, Double> copy() { return new TimeIndexRealValuesSeries(this.indexDimension.copy(), getSeriesValuesList()); }
}
