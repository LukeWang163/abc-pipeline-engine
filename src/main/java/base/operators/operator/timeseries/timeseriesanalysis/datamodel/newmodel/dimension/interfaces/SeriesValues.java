package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import java.util.List;

public interface SeriesValues<V> {
   V getValue(int var1);

   double getValueAsDouble(int var1);

   List<V> getValues();

   void setValue(int var1, V var2);

   void setDoubleValue(int var1, double var2);

   void setValues(List<V> var1);

   SeriesValues<V> getSubSeriesValues(int var1, int var2);

   boolean equalSeriesValues(SeriesValues<?> var1);

   int getLength();

   String getName();

   void setName(String var1);

   SeriesValues<V> copy();

   SeriesBuilder.ValuesType getValuesType();

   boolean hasMissingValues();

   boolean isMissing(V var1);

   double convertValueToDoubleValue(V var1);

   V convertDoubleValueToValue(double var1);
}
