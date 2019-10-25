package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IDefaultIndexNominalValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import java.util.ArrayList;
import java.util.List;


public class DefaultIndexNominalValuesSeries
        extends AbstractSeries<Integer, String>
        implements IDefaultIndexNominalValuesSeries
{
   protected DefaultIndexNominalValuesSeries(IndexDimension<Integer> indexDimension, List<SeriesValues<String>> seriesValuesList) { super(indexDimension, seriesValuesList); }



   protected AbstractSeries<Integer, String> getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      List<SeriesValues<String>> subSeriesValuesList = new ArrayList<SeriesValues<String>>();
      for (SeriesValues<String> seriesValues : this.seriesValuesList) {
         subSeriesValuesList.add(seriesValues.getSubSeriesValues(lowerArrayIndex, upperArrayIndex));
      }
      return new DefaultIndexNominalValuesSeries(this.indexDimension.getSubIndexDimension(lowerArrayIndex, upperArrayIndex), subSeriesValuesList);
   }




   public ISeries<Integer, String> copy() { return new DefaultIndexNominalValuesSeries(this.indexDimension.copy(), getSeriesValuesList()); }
}
