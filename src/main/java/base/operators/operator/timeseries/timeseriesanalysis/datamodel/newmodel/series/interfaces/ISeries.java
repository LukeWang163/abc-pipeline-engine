package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import java.util.List;

public interface ISeries<I, V> {
   I getIndexValue(int paramInt);

   List<I> getIndexValues();

   IndexDimension<I> getIndexDimension();

   SeriesBuilder.IndexType getIndexType();

   SeriesBuilder.ValuesType getValuesType();

   void setIndexValue(int paramInt, I paramI);

   void setIndexValues(List<I> paramList);

   V getValue(int paramInt1, int paramInt2);

   List<V> getValues(int paramInt);

   SeriesValues<V> getSeriesValues(int paramInt);

   void setValue(int paramInt1, int paramInt2, V paramV);

   void setValues(int paramInt, List<V> paramList);

   V getValue(String paramString, int paramInt);

   List<V> getValues(String paramString);

   SeriesValues<V> getSeriesValues(String paramString);

   List<SeriesValues<V>> getSeriesValuesList();

   void setValue(String paramString, int paramInt, V paramV);

   void setValues(String paramString, List<V> paramList);

   void setIndexAndValues(List<I> paramList1, List<List<V>> paramList2);

   void addSeriesValues(SeriesValues<V> paramSeriesValues);

   void addSeries(ISeries<I, V> paramISeries);

   SeriesValues<V> removeSeries(int paramInt);

   SeriesValues<V> removeSeries(String paramString);

   void setName(String paramString1, String paramString2);

   void setName(int paramInt, String paramString);

   String getName(int paramInt);

   void setIndexName(String paramString);

   String getIndexName();

   String[] getSeriesNames();

   boolean hasSeries(String paramString);

   int getLength();

   int getNumberOfSeries();

   int getUpperArrayIndex(I paramI);

   int getLowerArrayIndex(I paramI);

   ISeries<I, V> getSubSeries(int paramInt1, int paramInt2);

   boolean equals(ISeries<?, ?> paramISeries);

   ISeries<I, V> copy();
}
