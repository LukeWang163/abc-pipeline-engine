package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import java.util.List;

public interface IndexDimension<I> {
   I getIndexValue(int paramInt);

   double getIndexValueAsDouble(int var1);

   List<I> getIndexValues();

   void setIndexValue(int var1, I var2);

   void setDoubleIndexValue(int var1, double var2);

   void setIndexValues(List<I> var1);

   void isValidIndexDimension(List<I> var1);

   void isValidIndexValue(int var1, I var2);

   int getLowerArrayIndex(I var1);

   int getUpperArrayIndex(I var1);

   IndexDimension<I> getSubIndexDimension(int var1, int var2);

   boolean equalIndexDimension(IndexDimension<?> var1);

   boolean hasFixLength();

   int getLength();

   String getName();

   void setName(String var1);

   IndexDimension<I> copy();

   SeriesBuilder.IndexType getIndexType();

   boolean hasMissingValues();

   boolean isMissing(I var1);

   double convertValueToDoubleValue(I var1);

   I convertDoubleValueToValue(double var1);
}
