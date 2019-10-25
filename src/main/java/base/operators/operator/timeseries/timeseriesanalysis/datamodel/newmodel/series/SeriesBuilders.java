package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IDefaultIndexMixedValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IDefaultIndexNominalValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IDefaultIndexRealValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealIndexMixedValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealIndexNominalValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealIndexRealValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ITimeIndexMixedValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ITimeIndexNominalValuesSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ITimeIndexRealValuesSeries;


public class SeriesBuilders
{
   public static SeriesBuilder<ISeries<?, ?>> builder() { return new SeriesBuilder(); }












   public static <T extends base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealValuesSeries<?>> SeriesBuilder<T> realValues() { return create(null, SeriesBuilder.ValuesType.REAL); }












   public static <T extends base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.INominalValuesSeries<?>> SeriesBuilder<T> nominalValues() { return create(null, SeriesBuilder.ValuesType.NOMINAL); }











   public static <T extends base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IMixedValuesSeries<?>> SeriesBuilder<T> mixedValues() { return create(null, SeriesBuilder.ValuesType.MIXED); }












   public static <T extends base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IDefaultIndexSeries<?>> SeriesBuilder<T> defaultIndex() { return create(SeriesBuilder.IndexType.DEFAULT, null); }












   public static <T extends base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealIndexSeries<?>> SeriesBuilder<T> realIndex() { return create(SeriesBuilder.IndexType.REAL, null); }












   public static <T extends base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ITimeIndexSeries<?>> SeriesBuilder<T> timeIndex() { return create(SeriesBuilder.IndexType.TIME, null); }













   public static SeriesBuilder<IDefaultIndexRealValuesSeries> defaultIndexRealValues() { return create(SeriesBuilder.IndexType.DEFAULT, SeriesBuilder.ValuesType.REAL); }













   public static SeriesBuilder<IRealIndexRealValuesSeries> realIndexRealValues() { return create(SeriesBuilder.IndexType.REAL, SeriesBuilder.ValuesType.REAL); }













   public static SeriesBuilder<ITimeIndexRealValuesSeries> timeIndexRealValues() { return create(SeriesBuilder.IndexType.TIME, SeriesBuilder.ValuesType.REAL); }















   public static SeriesBuilder<IDefaultIndexNominalValuesSeries> defaultIndexNominalValues() { return create(SeriesBuilder.IndexType.DEFAULT, SeriesBuilder.ValuesType.NOMINAL); }













   public static SeriesBuilder<IRealIndexNominalValuesSeries> realIndexNominalValues() { return create(SeriesBuilder.IndexType.REAL, SeriesBuilder.ValuesType.NOMINAL); }













   public static SeriesBuilder<ITimeIndexNominalValuesSeries> timeIndexNominalValues() { return create(SeriesBuilder.IndexType.TIME, SeriesBuilder.ValuesType.NOMINAL); }












   public static SeriesBuilder<IDefaultIndexMixedValuesSeries> defaultIndexMixedValues() { return create(SeriesBuilder.IndexType.DEFAULT, SeriesBuilder.ValuesType.MIXED); }












   public static SeriesBuilder<IRealIndexMixedValuesSeries> realIndexMixedValues() { return create(SeriesBuilder.IndexType.REAL, SeriesBuilder.ValuesType.MIXED); }












   public static SeriesBuilder<ITimeIndexMixedValuesSeries> timeIndexMixedValues() { return create(SeriesBuilder.IndexType.TIME, SeriesBuilder.ValuesType.MIXED); }














   public static <S extends ISeries<?, ?>> SeriesBuilder<S> fromSeries(S series) { return create(series.getIndexType(), series.getValuesType()); }


   private static <T extends ISeries<?, ?>> SeriesBuilder<T> create(SeriesBuilder.IndexType indexType, SeriesBuilder.ValuesType valuesType) {
      return (new SeriesBuilder((valuesType != null && valuesType != SeriesBuilder.ValuesType.MIXED) )).setIndexType(indexType)
              .addValuesType(valuesType);
   }
}
