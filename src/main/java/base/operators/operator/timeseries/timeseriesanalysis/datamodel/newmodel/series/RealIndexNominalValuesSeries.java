package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealIndexNominalValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import java.util.ArrayList;
import java.util.List;


public class RealIndexNominalValuesSeries
        extends AbstractSeries<Double, String>
        implements IRealIndexNominalValuesSeries
{
   protected RealIndexNominalValuesSeries(IndexDimension<Double> indexDimension, List<SeriesValues<String>> seriesValuesList) { super(indexDimension, seriesValuesList); }



   protected AbstractSeries<Double, String> getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      List<SeriesValues<String>> subSeriesValuesList = new ArrayList<SeriesValues<String>>();
      for (SeriesValues<String> seriesValues : this.seriesValuesList) {
         subSeriesValuesList.add(seriesValues.getSubSeriesValues(lowerArrayIndex, upperArrayIndex));
      }
      return new RealIndexNominalValuesSeries(this.indexDimension.getSubIndexDimension(lowerArrayIndex, upperArrayIndex), subSeriesValuesList);
   }




   public ISeries<Double, String> copy() { return new RealIndexNominalValuesSeries(this.indexDimension.copy(), getSeriesValuesList()); }
}
