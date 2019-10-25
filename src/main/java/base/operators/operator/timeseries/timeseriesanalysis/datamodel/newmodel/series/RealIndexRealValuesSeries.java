package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealIndexRealValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import java.util.ArrayList;
import java.util.List;


public class RealIndexRealValuesSeries
        extends AbstractSeries<Double, Double>
        implements IRealIndexRealValuesSeries
{
   protected RealIndexRealValuesSeries(IndexDimension<Double> indexDimension, List<SeriesValues<Double>> seriesValuesList) { super(indexDimension, seriesValuesList); }



   protected AbstractSeries<Double, Double> getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      List<SeriesValues<Double>> subSeriesValuesList = new ArrayList<SeriesValues<Double>>();
      for (SeriesValues<Double> seriesValues : this.seriesValuesList) {
         subSeriesValuesList.add(seriesValues.getSubSeriesValues(lowerArrayIndex, upperArrayIndex));
      }
      return new RealIndexRealValuesSeries(this.indexDimension.getSubIndexDimension(lowerArrayIndex, upperArrayIndex), subSeriesValuesList);
   }




   public ISeries<Double, Double> copy() { return new RealIndexRealValuesSeries(this.indexDimension.copy(), getSeriesValuesList()); }
}
