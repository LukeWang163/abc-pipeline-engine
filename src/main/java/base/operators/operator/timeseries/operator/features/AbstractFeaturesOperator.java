package base.operators.operator.timeseries.operator.features;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.NominalMapping;
import base.operators.example.table.PolynominalMapping;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.UserError;
import base.operators.operator.ports.IncompatibleMDClassException;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.DefaultInteger;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.NominalValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.RealValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.TimeValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.container.Triple;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;

import java.text.Collator;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;

public abstract class AbstractFeaturesOperator extends ExampleSetTimeSeriesOperator {
   protected OutputPort featureOutputPort = (OutputPort)this.getOutputPorts().createPort("features");
   protected OutputPort exampleSetOutputPort = (OutputPort)this.getOutputPorts().createPort("original");
   public static final String PARAMETER_ADD_TIME_SERIES_NAME = "add_time_series_name";
   public static final String PARAMETER_IGNORE_INVALID_VALUES = "ignore_invalid_values";

   public AbstractFeaturesOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.getTransformer().addPassThroughRule(this.exampleSetTimeSeriesHelper.getExampleSetInputPort(), this.exampleSetOutputPort);
      this.getTransformer().addRule(new MDTransformationRule() {
         public void transformMD() {
            ExampleSetMetaData md = new ExampleSetMetaData();
            boolean addTimeSeriesName = AbstractFeaturesOperator.this.getParameterAsBoolean("add_time_series_name");
            Collection selectedAttributes = null;
            if (AbstractFeaturesOperator.this.exampleSetTimeSeriesHelper.getExampleSetInputPort().isConnected()) {
               try {
                  ExampleSetMetaData inputMD = (ExampleSetMetaData)AbstractFeaturesOperator.this.exampleSetTimeSeriesHelper.getExampleSetInputPort().getMetaData(ExampleSetMetaData.class);
                  if (inputMD != null) {
                     selectedAttributes = AbstractFeaturesOperator.this.exampleSetTimeSeriesHelper.getAttributeSubsetSelector().getMetaDataSubset(inputMD, AbstractFeaturesOperator.this.exampleSetTimeSeriesHelper.isIncludeSpecialAttributes()).getAllAttributes();
                  }
               } catch (IncompatibleMDClassException var10) {
                  var10.printStackTrace();
               }
            }

            if (!addTimeSeriesName) {
               md.addAttribute(new AttributeMetaData("time series", 1, "id"));
               if (selectedAttributes != null) {
                  md.setNumberOfExamples(selectedAttributes.size());
               }
            } else {
               md.setNumberOfExamples(1);
            }

            try {
               String[] var12 = AbstractFeaturesOperator.this.getFeatureNames();
               int var5 = var12.length;

               for(int var6 = 0; var6 < var5; ++var6) {
                  String featureName = var12[var6];
                  if (addTimeSeriesName && selectedAttributes != null) {
                     Iterator var8 = selectedAttributes.iterator();

                     while(var8.hasNext()) {
                        AttributeMetaData amd = (AttributeMetaData)var8.next();
                        md.addAttribute(new AttributeMetaData(amd.getName() + "." + featureName, amd.getValueType()));
                     }
                  } else {
                     md.addAttribute(new AttributeMetaData(featureName, AbstractFeaturesOperator.this.getDefaultValueType()));
                  }
               }
            } catch (UndefinedParameterError var11) {
               var11.printStackTrace();
            }

            AbstractFeaturesOperator.this.featureOutputPort.deliverMD(md);
         }
      });
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      return builder.asInputPortOperator("example set").enableMultivariateInput().setIndiceHandling(this.getIndicesHandling()).setValuesType(this.getValuesType()).useISeries().build();
   }

   public void doWork() throws OperatorException {
      this.exampleSetTimeSeriesHelper.resetHelper();
      this.exampleSetTimeSeriesHelper.readInputData(this.exampleSetTimeSeriesHelper.getExampleSetInputPort());
      if (this.failOnInvalidValues() && (!this.addIgnoreInvalidValuesParameter() || !this.getParameterAsBoolean("ignore_invalid_values"))) {
         this.exampleSetTimeSeriesHelper.checkForMissingValues();
         this.exampleSetTimeSeriesHelper.checkForInfiniteValues();
      }

      this.exampleSetOutputPort.deliver(this.exampleSetTimeSeriesHelper.getInputExampleSet());
      int totalProgressSteps = this.exampleSetTimeSeriesHelper.progressCallsInGetAddConvertMethods() + this.exampleSetTimeSeriesHelper.getNumberOfAttributes() + 1;
      this.getProgress().setCheckForStop(true);
      this.getProgress().setTotal(totalProgressSteps);
      this.exampleSetTimeSeriesHelper.enableCallProgressStep();
      ISeries inputSeries = this.exampleSetTimeSeriesHelper.getInputISeriesFromPort();
      this.getProgress().step();
      boolean addTimeSeriesName = this.getParameterAsBoolean("add_time_series_name");
      List listOfNewAtts = new LinkedList();
      Attribute timeSeriesNameAttribute = null;
      List seriesNames = null;
      List singleRowValues = null;
      Map multiRowValues = null;
      if (addTimeSeriesName) {
         singleRowValues = new ArrayList();
      } else {
         timeSeriesNameAttribute = AttributeFactory.createAttribute("time series", 1);
         listOfNewAtts.add(timeSeriesNameAttribute);
         seriesNames = new ArrayList();
         multiRowValues = new LinkedHashMap();
      }

      IndexDimension indexDimension = inputSeries.getIndexDimension();

      SeriesValues seriesValues;
      try {
         this.initFeatureCalculation(inputSeries);

         label151:
         for(Iterator var10 = inputSeries.getSeriesValuesList().iterator(); var10.hasNext(); this.getProgress().step()) {
            seriesValues = (SeriesValues)var10.next();
            String seriesName = seriesValues.getName();
            if (!addTimeSeriesName) {
               seriesNames.add(seriesValues.getName());
            }

            List featureList = this.getFeatureList(indexDimension, seriesValues);
            Iterator var14 = featureList.iterator();

            while(true) {
               while(true) {
                  if (!var14.hasNext()) {
                     continue label151;
                  }

                  Pair feature = (Pair)var14.next();
                  String featureName = ((FeatureContainer)feature.getSecond()).getName();
                  Integer featureType = ((FeatureContainer)feature.getSecond()).getValueType();
                  Double featureDoubleValue = ((FeatureContainer)feature.getSecond()).getDoubleValue();
                  NominalMapping mapping = ((FeatureContainer)feature.getSecond()).getNominalMapping();
                  if (addTimeSeriesName) {
                     String featureAttributeName = seriesName + "." + featureName;
                     Attribute attribute = AttributeFactory.createAttribute(featureAttributeName, featureType);
                     if (mapping != null) {
                        attribute.setMapping(mapping);
                     }

                     listOfNewAtts.add(attribute);
                     singleRowValues.add(new Pair(attribute, featureDoubleValue));
                  } else {
                     Triple currentValues = null;
                     if (!multiRowValues.containsKey(featureName)) {
                        Map map = new LinkedHashMap();
                        map.put(seriesName, featureDoubleValue);
                        currentValues = new Triple(featureType, mapping, map);
                     } else {
                        currentValues = (Triple)multiRowValues.get(featureName);
                        Integer oldFeatureType = (Integer)currentValues.getFirst();
                        NominalMapping oldMapping = (NominalMapping)currentValues.getSecond();
                        Map valuesMap = (Map)currentValues.getThird();
                        if (mapping != null && oldMapping != null && !mapping.equals((NominalMapping)oldMapping) && !Double.isNaN(featureDoubleValue)) {
                           featureDoubleValue = (double)((NominalMapping)oldMapping).mapString(mapping.mapIndex(featureDoubleValue.intValue()));
                        }

                        if (oldFeatureType != featureType) {
                           if (oldMapping == null) {
                              oldMapping = mapping != null ? mapping : new PolynominalMapping();
                              currentValues.setSecond(oldMapping);
                              Iterator var24 = valuesMap.entrySet().iterator();

                              while(var24.hasNext()) {
                                 Entry valueEntry = (Entry)var24.next();
                                 if (!Double.isNaN((Double)valueEntry.getValue())) {
                                    valueEntry.setValue(this.mapNonNominalValue((NominalMapping)oldMapping, oldFeatureType, (Double)valueEntry.getValue()));
                                 }
                              }
                           }

                           if (featureType != 1 && !Double.isNaN(featureDoubleValue)) {
                              featureDoubleValue = this.mapNonNominalValue((NominalMapping)oldMapping, featureType, featureDoubleValue);
                           }

                           currentValues.setFirst(1);
                        }

                        valuesMap.put(seriesName, featureDoubleValue);
                        currentValues.setThird(valuesMap);
                     }

                     multiRowValues.put(featureName, currentValues);
                  }
               }
            }
         }
      } catch (Exception var26) {
         this.exampleSetTimeSeriesHelper.handleException(var26, StringUtils.join((Object[])inputSeries.getSeriesNames(), ","));
      }

      if (!addTimeSeriesName) {
         List list = new ArrayList(multiRowValues.entrySet());
         if (this.sortAttributes()) {
            list.sort(Entry.comparingByKey(Collator.getInstance(Locale.US)));
         }

         Attribute attribute;
         for(Iterator var29 = list.iterator(); var29.hasNext(); listOfNewAtts.add(attribute)) {
            Entry entry = (Entry)var29.next();
            String attributeName = (String)entry.getKey();
            int attributeType = (Integer)((Triple)entry.getValue()).getFirst();
            NominalMapping mapping = (NominalMapping)((Triple)entry.getValue()).getSecond();
            attribute = AttributeFactory.createAttribute(attributeName, attributeType);
            if (mapping != null) {
               attribute.setMapping(mapping);
            }
         }
      }

      ExampleSetBuilder builder = ExampleSets.from(listOfNewAtts);
      seriesValues = null;
      ExampleSet result;
      Iterator var35;
      if (addTimeSeriesName) {
         result = builder.withBlankSize(1).build();
         Example example = result.getExample(0);
         var35 = singleRowValues.iterator();

         while(var35.hasNext()) {
            Pair valuePair = (Pair)var35.next();
            example.setValue((Attribute)valuePair.getFirst(), (Double)valuePair.getSecond());
         }
      } else {
         result = builder.withRole(timeSeriesNameAttribute, "id").withBlankSize(seriesNames.size()).build();
         int i = 0;

         for(var35 = result.iterator(); var35.hasNext(); ++i) {
            Example example = (Example)var35.next();
            String seriesName = (String)seriesNames.get(i);
            Iterator var42 = listOfNewAtts.iterator();

            while(var42.hasNext()) {
               Attribute attribute = (Attribute)var42.next();
               if (attribute == timeSeriesNameAttribute) {
                  example.setValue(timeSeriesNameAttribute, seriesName);
               } else if (!((Map)((Triple)multiRowValues.get(attribute.getName())).getThird()).containsKey(seriesName)) {
                  example.setValue(attribute, Double.NaN);
               } else {
                  example.setValue(attribute, (Double)((Map)((Triple)multiRowValues.get(attribute.getName())).getThird()).get(seriesName));
               }
            }
         }
      }

      this.finishFeatureCalculation(inputSeries);
      this.featureOutputPort.deliver(result);
      this.getProgress().complete();
   }

   protected void finishFeatureCalculation(ISeries inputSeries) throws ProcessStoppedException {
   }

   private List getFeatureList(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      SeriesBuilder.IndexType indexType = indexDimension.getIndexType();
      SeriesBuilder.ValuesType valuesType = seriesValues.getValuesType();
      if (indexType == SeriesBuilder.IndexType.DEFAULT) {
         if (valuesType == SeriesBuilder.ValuesType.REAL) {
            return this.getFeaturesDefaultReal((DefaultInteger)indexDimension, (RealValues)seriesValues);
         }

         if (valuesType == SeriesBuilder.ValuesType.NOMINAL) {
            return this.getFeaturesDefaultNominal((DefaultInteger)indexDimension, (NominalValues)seriesValues);
         }

         if (valuesType == SeriesBuilder.ValuesType.TIME) {
            return this.getFeaturesDefaultTime((DefaultInteger)indexDimension, (TimeValues)seriesValues);
         }
      }

      if (indexType == SeriesBuilder.IndexType.REAL) {
         if (valuesType == SeriesBuilder.ValuesType.REAL) {
            return this.getFeaturesRealReal((RealValues)indexDimension, (RealValues)seriesValues);
         }

         if (valuesType == SeriesBuilder.ValuesType.NOMINAL) {
            return this.getFeaturesRealNominal((RealValues)indexDimension, (NominalValues)seriesValues);
         }

         if (valuesType == SeriesBuilder.ValuesType.TIME) {
            return this.getFeaturesRealTime((RealValues)indexDimension, (TimeValues)seriesValues);
         }
      }

      if (indexType == SeriesBuilder.IndexType.TIME) {
         if (valuesType == SeriesBuilder.ValuesType.REAL) {
            return this.getFeaturesTimeReal((TimeValues)indexDimension, (RealValues)seriesValues);
         }

         if (valuesType == SeriesBuilder.ValuesType.NOMINAL) {
            return this.getFeaturesTimeNominal((TimeValues)indexDimension, (NominalValues)seriesValues);
         }

         if (valuesType == SeriesBuilder.ValuesType.TIME) {
            return this.getFeaturesTimeTime((TimeValues)indexDimension, (TimeValues)seriesValues);
         }
      }

      return null;
   }

   private Double mapNonNominalValue(NominalMapping mapping, Integer featureType, Double oldDoubleValue) {
      if (featureType != 4 && featureType != 2 && featureType != 3) {
         return featureType != 11 && featureType != 9 && featureType != 10 ? Double.NaN : (double)mapping.mapString(Instant.ofEpochMilli(oldDoubleValue.longValue()).toString());
      } else {
         return (double)mapping.mapString(oldDoubleValue.toString());
      }
   }

   protected abstract void initFeatureCalculation(ISeries var1) throws UserError;

   protected List getFeaturesDefaultReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      throw new IllegalArgumentException("Features operator is not configured to handle default integer index and real values.");
   }

   protected List getFeaturesDefaultNominal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      throw new IllegalArgumentException("Features operator is not configured to handle default integer index and nominal values.");
   }

   protected List getFeaturesDefaultTime(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      throw new IllegalArgumentException("Features operator is not configured to handle default integer index and date time values.");
   }

   protected List getFeaturesRealReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      throw new IllegalArgumentException("Features operator is not configured to handle real index and real values.");
   }

   protected List getFeaturesRealNominal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      throw new IllegalArgumentException("Features operator is not configured to handle real index and nominal values.");
   }

   protected List getFeaturesRealTime(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      throw new IllegalArgumentException("Features operator is not configured to handle real index and date time values.");
   }

   protected List getFeaturesTimeReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      throw new IllegalArgumentException("Features operator is not configured to handle date time index and real values.");
   }

   protected List getFeaturesTimeNominal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      throw new IllegalArgumentException("Features operator is not configured to handle date time index and nominal values.");
   }

   protected List getFeaturesTimeTime(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      throw new IllegalArgumentException("Features operator is not configured to handle date time index and date time values.");
   }

   public List getParameterTypes() {
      List types = this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
      types.addAll(this.parameterTypesBefore());
      types.add(new ParameterTypeBoolean("add_time_series_name", "If this parameter is set to true the name of the time series attributes are added as a prefix to the name of the feature attributes.", false, false));
      if (this.addIgnoreInvalidValuesParameter()) {
         types.add(new ParameterTypeBoolean("ignore_invalid_values", "If this parameter is set to true invalid values (missing, positive and negative infinity for numeric series and emtpy strings for nominal series) in the time series are ignored for the calculation.", false, false));
      }

      types.addAll(this.parameterTypesAfter());
      return types;
   }

   protected abstract List parameterTypesBefore();

   protected abstract List parameterTypesAfter();

   protected ExampleSetTimeSeriesHelper.IndiceHandling getIndicesHandling() {
      return ExampleSetTimeSeriesHelper.IndiceHandling.NO_INDICES;
   }

   protected boolean addIgnoreInvalidValuesParameter() {
      return true;
   }

   protected boolean failOnInvalidValues() {
      return false;
   }

   protected abstract String[] getFeatureNames() throws UndefinedParameterError;

   protected abstract int getDefaultValueType();

   protected abstract SeriesBuilder.ValuesType getValuesType();

   protected abstract boolean sortAttributes();
}
