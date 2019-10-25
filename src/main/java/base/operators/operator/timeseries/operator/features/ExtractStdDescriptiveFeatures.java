package base.operators.operator.timeseries.operator.features;

import base.operators.operator.OperatorDescription;
import base.operators.operator.UserError;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.UndefinedParameterError;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.feature.DescriptiveFeatures;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExtractStdDescriptiveFeatures extends AbstractFeaturesOperator {
   private DescriptiveFeatures descriptiveFeatures = null;

   public ExtractStdDescriptiveFeatures(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
   }

   protected void initFeatureCalculation(ISeries inputSeries) throws UserError {
      DescriptiveFeatures.DescripitveFeaturesBuilder builder = new DescriptiveFeatures.DescripitveFeaturesBuilder();
      if (this.getParameterAsBoolean("ignore_invalid_values")) {
         builder = builder.skipInvalidValues();
      }

      DescriptiveFeatures.Feature[] var3 = DescriptiveFeatures.Feature.values();
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         DescriptiveFeatures.Feature feature = var3[var5];
         if (this.getParameterAsBoolean(feature.toString().replace(" ", "_"))) {
            builder = builder.enableFeature(feature);
         }
      }

      this.descriptiveFeatures = (DescriptiveFeatures)builder.build();
   }

   private List getFeatureList(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      List result = new ArrayList();
      List featureList = this.descriptiveFeatures.getComputedFeatures(indexDimension, seriesValues);
      Iterator var5 = featureList.iterator();

      while(var5.hasNext()) {
         Pair valuePair = (Pair)var5.next();
         FeatureContainer feature = (new FeatureContainer()).setName((String)valuePair.getFirst()).setValueType(2).setSeriesValuesType(SeriesBuilder.ValuesType.REAL).setValue(valuePair.getSecond()).setDoubleValue(valuePair.getSecond() == null ? Double.NaN : (Double)valuePair.getSecond());
         result.add(new Pair(valuePair.getFirst(), feature));
      }

      return result;
   }

   protected List getFeaturesDefaultReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeatureList(indexDimension, seriesValues);
   }

   protected List getFeaturesRealReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeatureList(indexDimension, seriesValues);
   }

   protected List getFeaturesTimeReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeatureList(indexDimension, seriesValues);
   }

   protected List parameterTypesBefore() {
      List types = new ArrayList();
      DescriptiveFeatures.Feature[] var2 = DescriptiveFeatures.Feature.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         DescriptiveFeatures.Feature feature = var2[var4];
         ParameterType type = new ParameterTypeBoolean(feature.toString().replace(" ", "_"), "Enables the calculation of the " + feature.toString() + " of the values of the time series.", true, false);
         types.add(type);
      }

      return types;
   }

   protected List parameterTypesAfter() {
      return new ArrayList();
   }

   protected String[] getFeatureNames() throws UndefinedParameterError {
      List resultList = new ArrayList();
      DescriptiveFeatures.Feature[] var2 = DescriptiveFeatures.Feature.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         DescriptiveFeatures.Feature feature = var2[var4];
         if (this.getParameterAsBoolean(feature.toString())) {
            resultList.add(feature.toString());
         }
      }

      return (String[])resultList.toArray(new String[0]);
   }

   protected int getDefaultValueType() {
      return 2;
   }

   protected SeriesBuilder.ValuesType getValuesType() {
      return SeriesBuilder.ValuesType.REAL;
   }

   protected boolean sortAttributes() {
      return false;
   }
}
