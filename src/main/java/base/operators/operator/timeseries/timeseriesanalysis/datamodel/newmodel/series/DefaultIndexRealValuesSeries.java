package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IDefaultIndexRealValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import java.util.ArrayList;
import java.util.List;


public class DefaultIndexRealValuesSeries
        extends AbstractSeries<Integer, Double>
        implements IDefaultIndexRealValuesSeries
{
   protected DefaultIndexRealValuesSeries(IndexDimension<Integer> indexDimension, List<SeriesValues<Double>> seriesValuesList) { super(indexDimension, seriesValuesList); }



   protected AbstractSeries<Integer, Double> getSubSeriesImplemented(int lowerArrayIndex, int upperArrayIndex) {
      List<SeriesValues<Double>> subSeriesValuesList = new ArrayList<SeriesValues<Double>>();
      for (SeriesValues<Double> seriesValues : this.seriesValuesList) {
         subSeriesValuesList.add(seriesValues.getSubSeriesValues(lowerArrayIndex, upperArrayIndex));
      }
      return new DefaultIndexRealValuesSeries(this.indexDimension.getSubIndexDimension(lowerArrayIndex, upperArrayIndex), subSeriesValuesList);
   }




   public ISeries<Integer, Double> copy() { return new DefaultIndexRealValuesSeries(this.indexDimension.copy(), getSeriesValuesList()); }
}
