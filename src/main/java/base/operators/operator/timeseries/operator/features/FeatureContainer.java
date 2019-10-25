package base.operators.operator.timeseries.operator.features;

import base.operators.example.table.NominalMapping;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;

public class FeatureContainer {
   private String name;
   private Object value;
   private double doubleValue;
   private int valueType;
   private SeriesBuilder.ValuesType seriesValuesType;
   private NominalMapping nominalMapping = null;

   public NominalMapping getNominalMapping() {
      return this.nominalMapping;
   }

   public FeatureContainer setNominalMapping(NominalMapping nominalMapping) {
      this.nominalMapping = nominalMapping;
      return this;
   }

   public String getName() {
      return this.name;
   }

   public FeatureContainer setName(String name) {
      this.name = name;
      return this;
   }

   public Object getValue() {
      return this.value;
   }

   public FeatureContainer setValue(Object value) {
      this.value = value;
      return this;
   }

   public double getDoubleValue() {
      return this.doubleValue;
   }

   public FeatureContainer setDoubleValue(double doubleValue) {
      this.doubleValue = doubleValue;
      return this;
   }

   public int getValueType() {
      return this.valueType;
   }

   public FeatureContainer setValueType(int valueType) {
      this.valueType = valueType;
      return this;
   }

   public SeriesBuilder.ValuesType getSeriesValuesType() {
      return this.seriesValuesType;
   }

   public FeatureContainer setSeriesValuesType(SeriesBuilder.ValuesType seriesValuesType) {
      this.seriesValuesType = seriesValuesType;
      return this;
   }
}
