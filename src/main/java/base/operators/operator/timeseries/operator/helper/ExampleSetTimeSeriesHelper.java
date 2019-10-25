package base.operators.operator.timeseries.operator.helper;

import base.operators.example.*;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.PolynominalMapping;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.UserError;
import base.operators.operator.error.AttributeNotFoundError;
import base.operators.operator.error.AttributeWrongTypeError;
import base.operators.operator.ports.IncompatibleMDClassException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPassThroughRule;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.tools.AttributeSubsetSelector;
import base.operators.parameter.*;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.*;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IllegalSeriesLengthException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.NotStrictlyMonotonicIncreasingException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.NominalValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.RealValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.TimeValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilders;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealIndexSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ITimeIndexSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsEmptyException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import org.apache.commons.lang3.ArrayUtils;

import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;

public class ExampleSetTimeSeriesHelper<T extends Operator, S extends ISeries<?, ?>> {
   protected final InputPort exampleSetInputPort;
   protected final OutputPort exampleSetOutputPort;
   protected ExampleSet inputExampleSet;
   protected Set timeSeriesAttributes;
   protected Attribute timeSeriesAttribute;
   protected Attribute indicesAttribute = null;
   private IndiceType indiceType;
   protected final Operator operator;
   protected final AttributeSubsetSelector attributeSubsetSelector;
   public static final String PARAMETER_TIME_SERIES_ATTRIBUTE = "time_series_attribute";
   public static final String PARAMETER_HAS_INDICES_ATTRIBUTE = "has_indices";
   public static final String PARAMETER_INDICES_ATTRIBUTE = "indices_attribute";
   public static final String PARAMETER_ADD_NEW_ATTRIBUTE = "add_time_series_as_new_attribute";
   public static final String PARAMETER_NEW_ATTRIBUTE_NAME = "name_of_new_time_series_attribute";
   public static final String PARAMETER_OVERWRITE_ATTRIBUTES = "overwrite_attributes";
   public static final String PARAMETER_NEW_ATTRIBUTES_POSTFIX = "new_attributes_postfix";
   private final IndiceHandling indiceHandling;
   protected SeriesBuilder.ValuesType valuesType;
   protected boolean useISeries;
   private final boolean isInputPortOperator;
   private final boolean isOutputPortOperator;
   private final boolean multivariateInput;
   private final boolean includeSpecialAttributes;
   private final boolean addOverwriteOption;
   private final String defaultNewAttributeName;
   private final String defaultNewAttributesPostfix;
   private final boolean changeOutputAttributesToReal;
   private boolean callProgressStep;
   public static final int PROGRESS_STEP_SIZE = 2000000;

   public ExampleSetTimeSeriesHelper(Operator operator, boolean isInputPortOperator, String inputPortName, boolean isOutputPortOperator, String outputPortName, boolean hasMultivariateInput, boolean includeSpecialAttributes, IndiceHandling indiceHandling, boolean createPorts, boolean addOverwriteOption, String defaultNameOrPostfix, boolean changeOutputAttributesToReal, SeriesBuilder.ValuesType valuesType, boolean useISeries) {
      this.indiceType = IndiceType.DEFAULT;
      this.valuesType = SeriesBuilder.ValuesType.REAL;
      this.useISeries = false;
      this.operator = operator;
      this.isInputPortOperator = isInputPortOperator;
      this.isOutputPortOperator = isOutputPortOperator;
      this.multivariateInput = hasMultivariateInput;
      this.includeSpecialAttributes = includeSpecialAttributes;
      this.valuesType = valuesType;
      this.useISeries = useISeries;
      if (createPorts) {
         if (isInputPortOperator) {
            this.exampleSetInputPort = operator.getInputPorts().createPort(inputPortName, ExampleSet.class);
            if (this.multivariateInput) {
               this.attributeSubsetSelector = new AttributeSubsetSelector(operator, this.exampleSetInputPort, this.getAllowedValuesTypes(valuesType));
            } else {
               this.attributeSubsetSelector = null;
            }
         } else {
            this.exampleSetInputPort = null;
            this.attributeSubsetSelector = null;
         }

         if (isOutputPortOperator) {
            this.exampleSetOutputPort = (OutputPort)operator.getOutputPorts().createPort(outputPortName);
            operator.getTransformer().addGenerationRule(this.exampleSetOutputPort, ExampleSet.class);
         } else {
            this.exampleSetOutputPort = null;
         }
      } else {
         this.exampleSetInputPort = null;
         this.exampleSetOutputPort = null;
         this.attributeSubsetSelector = null;
      }

      this.indiceHandling = indiceHandling;
      this.addOverwriteOption = addOverwriteOption;
      if (this.multivariateInput) {
         this.defaultNewAttributesPostfix = defaultNameOrPostfix;
         this.defaultNewAttributeName = null;
      } else {
         this.defaultNewAttributeName = defaultNameOrPostfix;
         this.defaultNewAttributesPostfix = null;
      }

      this.changeOutputAttributesToReal = changeOutputAttributesToReal;
   }

   public void resetHelper() throws UndefinedParameterError {
      this.inputExampleSet = null;
      this.timeSeriesAttributes = null;
      this.timeSeriesAttribute = null;
      this.indicesAttribute = null;
      this.indiceType = IndiceType.DEFAULT;
   }

   public void addPassThroughRule() throws WrongConfiguredHelperException {
      if (this.isInputPortOperator && this.isOutputPortOperator) {
         if (this.multivariateInput) {
            this.operator.getTransformer().addRule(new ExampleSetPassThroughRule(this.exampleSetInputPort, this.exampleSetOutputPort, SetRelation.EQUAL) {
               public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
                  boolean newAttributes = true;
                  if (ExampleSetTimeSeriesHelper.this.addOverwriteOption) {
                     newAttributes = !ExampleSetTimeSeriesHelper.this.operator.getParameterAsBoolean("overwrite_attributes");
                  }

                  if (newAttributes) {
                     String postfix = ExampleSetTimeSeriesHelper.this.operator.getParameterAsString("new_attributes_postfix");
                     Iterator var4 = ExampleSetTimeSeriesHelper.this.attributeSubsetSelector.getMetaDataSubset(metaData, ExampleSetTimeSeriesHelper.this.includeSpecialAttributes).getAllAttributes().iterator();

                     while(var4.hasNext()) {
                        AttributeMetaData amd = (AttributeMetaData)var4.next();
                        if (ArrayUtils.contains(ExampleSetTimeSeriesHelper.this.getAllowedValuesTypes(ExampleSetTimeSeriesHelper.this.valuesType), amd.getValueType())) {
                           metaData.addAttribute(new AttributeMetaData(amd.getName() + postfix, amd.getValueType()));
                        }
                     }
                  }

                  return metaData;
               }
            });
         } else {
            this.operator.getTransformer().addRule(new ExampleSetPassThroughRule(this.exampleSetInputPort, this.exampleSetOutputPort, SetRelation.EQUAL) {
               public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
                  String newAttributeName = ExampleSetTimeSeriesHelper.this.operator.getParameterAsString("name_of_new_time_series_attribute");
                  boolean newAttributeToAdd = true;
                  if (ExampleSetTimeSeriesHelper.this.addOverwriteOption) {
                     newAttributeToAdd = ExampleSetTimeSeriesHelper.this.operator.getParameterAsBoolean("add_time_series_as_new_attribute");
                  }

                  if (newAttributeToAdd) {
                     metaData.addAttribute(new AttributeMetaData(newAttributeName, 2));
                  }

                  return metaData;
               }
            });
         }

      } else {
         throw new WrongConfiguredHelperException("addPassThroughRule()", "not initialized as input- and outputPortOperator", "asInputPortOperator(),asOutputPortOperator()");
      }
   }

   public void readInputData(InputPort inputPort) throws UserError {
      this.indicesAttribute = null;
      this.timeSeriesAttribute = null;
      this.timeSeriesAttributes = null;
      this.inputExampleSet = (ExampleSet)inputPort.getData(ExampleSet.class);
      if (this.multivariateInput) {
         Iterator subsetAttributeIterator = this.attributeSubsetSelector.getSubset(this.inputExampleSet, this.isIncludeSpecialAttributes(), true).getAttributes().allAttributes();
         this.timeSeriesAttributes = new LinkedHashSet();

         while(subsetAttributeIterator.hasNext()) {
            this.timeSeriesAttributes.add(subsetAttributeIterator.next());
         }
      } else {
         this.timeSeriesAttribute = this.inputExampleSet.getAttributes().get(this.operator.getParameterAsString("time_series_attribute"));
         if (this.timeSeriesAttribute == null) {
            throw new AttributeNotFoundError(this.operator, "time_series_attribute", this.operator.getParameterAsString("time_series_attribute"));
         }

         if (!ArrayUtils.contains(this.getAllowedValuesTypes(this.valuesType), this.timeSeriesAttribute.getValueType())) {
            throw new AttributeWrongTypeError(this.operator, this.timeSeriesAttribute, this.getAllowedValuesTypes(this.valuesType));
         }

         if (this.useISeries) {
            this.timeSeriesAttributes = new LinkedHashSet();
            this.timeSeriesAttributes.add(this.timeSeriesAttribute);
         }
      }

      boolean hasIndicesAttribute = false;
      switch(this.indiceHandling) {
      case NO_INDICES:
      default:
         break;
      case OPTIONAL_INDICES:
         hasIndicesAttribute = this.operator.getParameterAsBoolean("has_indices");
         break;
      case MANDATORY_INDICES:
         hasIndicesAttribute = true;
      }

      if (hasIndicesAttribute) {
         this.indicesAttribute = this.inputExampleSet.getAttributes().get(this.operator.getParameterAsString("indices_attribute"));
         if (this.indicesAttribute == null) {
            throw new AttributeNotFoundError(this.operator, "indices_attribute", this.operator.getParameterAsString("indices_attribute"));
         }

         if (this.indicesAttribute.isNumerical()) {
            this.indiceType = IndiceType.VALUE;
         } else {
            if (!this.indicesAttribute.isDateTime()) {
               throw new AttributeWrongTypeError(this.operator, this.indicesAttribute, new int[]{2, 9});
            }

            this.indiceType = IndiceType.TIME;
         }
      } else {
         this.indiceType = IndiceType.DEFAULT;
      }

   }

   public boolean checkForTimeIndices() throws OperatorException {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("checkForTimeIndices()", "not initialized as inputPortOperator", "asInputPortOperator()");
      } else {
         return this.checkForTimeIndices(this.exampleSetInputPort);
      }
   }

   public boolean checkForTimeIndices(InputPort inputPort) throws OperatorException {
      switch(this.indiceHandling) {
      case NO_INDICES:
         this.indiceType = IndiceType.DEFAULT;
         return false;
      case OPTIONAL_INDICES:
         if (!this.operator.getParameterAsBoolean("has_indices")) {
            this.indiceType = IndiceType.DEFAULT;
            return false;
         }
      default:
         if (this.inputExampleSet == null) {
            this.readInputData(inputPort);
         }

         return this.indiceType == IndiceType.TIME;
      }
   }

   public void checkForMissingValues() throws UserError {
      if (this.inputExampleSet == null) {
         this.readInputData(this.exampleSetInputPort);
      }

      this.inputExampleSet.recalculateAllAttributeStatistics();
      Iterator var1 = this.timeSeriesAttributes.iterator();

      Attribute att;
      do {
         if (!var1.hasNext()) {
            return;
         }

         att = (Attribute)var1.next();
      } while(this.inputExampleSet.getStatistics(att, "unknown") <= 0.0D);

      throw new UserError(this.operator, "time_series_extension.timeseries.attribute_non_finite_values", new Object[]{"Time Series", "missing", att.getName()});
   }

   public void checkForInfiniteValues() throws UserError {
      if (this.inputExampleSet == null) {
         this.readInputData(this.exampleSetInputPort);
      }

      this.inputExampleSet.recalculateAllAttributeStatistics();
      Iterator var1 = this.timeSeriesAttributes.iterator();

      Attribute att;
      do {
         do {
            if (!var1.hasNext()) {
               return;
            }

            att = (Attribute)var1.next();
         } while(!att.isNumerical());
      } while(this.inputExampleSet.getStatistics(att, "maximum") != Double.POSITIVE_INFINITY && this.inputExampleSet.getStatistics(att, "minimum") != Double.NEGATIVE_INFINITY);

      throw new UserError(this.operator, "time_series_extension.timeseries.attribute_non_finite_values", new Object[]{"Time Series", "infinite", att.getName()});
   }

   public ValueSeries getInputValueSeries() throws OperatorException {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("getInputValueSeries()", "not initialized as inputPortOperator", "asInputPortOperator()");
      } else {
         return this.getInputValueSeries(this.exampleSetInputPort, this.timeSeriesAttribute);
      }
   }

   public ValueSeries getInputValueSeries(InputPort inputPort, Attribute seriesAttribute) throws OperatorException {
      if (this.inputExampleSet == null) {
         this.readInputData(inputPort);
      }

      double[] values = new double[this.inputExampleSet.size()];
      double[] indices = null;
      if (this.indicesAttribute != null) {
         indices = new double[this.inputExampleSet.size()];
         if (!this.indicesAttribute.isNumerical()) {
            throw new AttributeWrongTypeError(this.operator, this.indicesAttribute, new int[]{2});
         }
      }

      for(int i = 0; i < values.length; ++i) {
         values[i] = this.inputExampleSet.getExample(i).getValue(seriesAttribute);
         if (this.indicesAttribute != null) {
            indices[i] = this.inputExampleSet.getExample(i).getValue(this.indicesAttribute);
         }

         this.progressStep(i);
      }

      try {
         if (this.indicesAttribute != null) {
            this.indiceType = IndiceType.VALUE;
            return ValueSeries.create(indices, values, seriesAttribute.getName());
         } else {
            this.indiceType = IndiceType.DEFAULT;
            return ValueSeries.create(values, seriesAttribute.getName());
         }
      } catch (Exception var6) {
         throw this.handleException(var6, seriesAttribute.getName());
      }
   }

   public MultivariateValueSeries getInputMultivariateValueSeries() throws OperatorException {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("getInputMultivariateValueSeries()", "not initialized as inputPortOperator", "asInputPortOperator()");
      } else {
         return this.getInputMultivariateValueSeries(this.exampleSetInputPort, this.timeSeriesAttributes);
      }
   }

   public MultivariateValueSeries getInputMultivariateValueSeries(InputPort inputPort, Set seriesAttributes) throws OperatorException {
      if (this.inputExampleSet == null) {
         this.readInputData(inputPort);
      }

      Map valuesMap = new LinkedHashMap();
      Iterator var4 = seriesAttributes.iterator();

      while(var4.hasNext()) {
         Attribute attribute = (Attribute)var4.next();
         if (attribute.isNumerical()) {
            valuesMap.put(attribute, new double[this.inputExampleSet.size()]);
         }
      }

      if (valuesMap.isEmpty()) {
         throw new UserError(this.operator, "153", new Object[]{1, 0});
      } else {
         double[] indices = null;
         if (this.indicesAttribute != null) {
            indices = new double[this.inputExampleSet.size()];
            if (!this.indicesAttribute.isNumerical()) {
               throw new AttributeWrongTypeError(this.operator, this.indicesAttribute, new int[]{2});
            }
         }

         int exampleCounter = 0;
         int numberOfAttributes = valuesMap.size();

         for(Iterator var7 = this.inputExampleSet.iterator(); var7.hasNext(); ++exampleCounter) {
            Example example = (Example)var7.next();
            int attributeCounter = 0;

            for(Iterator var10 = valuesMap.entrySet().iterator(); var10.hasNext(); ++attributeCounter) {
               Entry entry = (Entry)var10.next();
               ((double[])entry.getValue())[exampleCounter] = example.getValue((Attribute)entry.getKey());
               this.progressStep(exampleCounter * numberOfAttributes + attributeCounter);
            }

            if (this.indicesAttribute != null) {
               indices[exampleCounter] = example.getValue(this.indicesAttribute);
            }
         }

         ArrayList seriesNames = new ArrayList();
         ArrayList valuesList = new ArrayList();
         Iterator var17 = valuesMap.entrySet().iterator();

         while(var17.hasNext()) {
            Entry entry = (Entry)var17.next();
            seriesNames.add(((Attribute)entry.getKey()).getName());
            valuesList.add(entry.getValue());
         }

         try {
            if (this.indicesAttribute != null) {
               this.indiceType = IndiceType.VALUE;
               return MultivariateValueSeries.create(indices, valuesList, seriesNames);
            } else {
               this.indiceType = IndiceType.DEFAULT;
               return MultivariateValueSeries.create(valuesList, seriesNames);
            }
         } catch (Exception var12) {
            throw this.handleException(var12, seriesNames.toString());
         }
      }
   }

   public ISeries getInputISeriesFromPort() throws OperatorException {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("getInputISeriesFromPort()", "not initialized as inputPortOperator", "asInputPortOperator()");
      } else {
         return this.getInputISeriesFromPort(this.exampleSetInputPort, (Attribute[])this.timeSeriesAttributes.toArray(new Attribute[0]));
      }
   }

   public ISeries getInputISeriesFromPort(InputPort inputPort, Attribute... seriesAttributes) throws OperatorException {
      if (this.inputExampleSet == null) {
         this.readInputData(inputPort);
      }

      Map<Attribute, List<Double>> valuesMap = new LinkedHashMap<Attribute, List<Double>>();
      Map<String, List<String>> nominalMappingMap = new LinkedHashMap<String, List<String>>();
      List<String> seriesNames = new ArrayList<String>();
      for (Attribute attribute : seriesAttributes) {
         valuesMap.put(attribute, new ArrayList());
         if (attribute.isNominal()) {
            nominalMappingMap.put(attribute.getName(), attribute.getMapping().getValues());
         }
         seriesNames.add(attribute.getName());
      }

      if (valuesMap.isEmpty()) {
         throw new UserError(this.operator, "153", new Object[] { Integer.valueOf(1), Integer.valueOf(0) });
      }

      List<Double> indexRealValues = null;
      List<Instant> indexTimeValues = null;
      if (this.indicesAttribute != null) {
         if (this.indicesAttribute.isNumerical()) {
            indexRealValues = new ArrayList<Double>();
         } else if (this.indicesAttribute.isDateTime()) {
            indexTimeValues = new ArrayList<Instant>();
         } else {
            throw new AttributeWrongTypeError(this.operator, this.indicesAttribute, new int[] { 2, 9 });
         }
      }
      int exampleCounter = 0;
      int numberOfAttributes = valuesMap.size();
      for (Example example : this.inputExampleSet) {
         int attributeCounter = 0;
         for (Entry<Attribute, List<Double>> entry : valuesMap.entrySet()) {
            ((List)entry.getValue()).add(Double.valueOf(example.getValue((Attribute)entry.getKey())));
            progressStep(exampleCounter * numberOfAttributes + attributeCounter);
            attributeCounter++;
         }
         if (this.indicesAttribute != null) {
            if (this.indicesAttribute.isNumerical()) {
               indexRealValues.add(Double.valueOf(example.getValue(this.indicesAttribute)));
            } else if (this.indicesAttribute.isDateTime()) {
               indexTimeValues.add(example.getDateValue(this.indicesAttribute).toInstant());
            }
         }
         exampleCounter++;
      }

      try {
            SeriesBuilder seriesBuilder = null;
            switch(this.valuesType) {
            case REAL:
               seriesBuilder = SeriesBuilders.realValues();
               break;
            case NOMINAL:
               seriesBuilder = SeriesBuilders.nominalValues();
               break;
            case MIXED:
               seriesBuilder = SeriesBuilders.mixedValues();
               break;
            case TIME:
               seriesBuilder = SeriesBuilders.mixedValues();
               break;
            default:
               seriesBuilder = SeriesBuilders.mixedValues();
            }

            if (this.indicesAttribute != null) {
               if (this.indicesAttribute.isNumerical()) {
                  seriesBuilder.realIndex(indexRealValues);
               } else if (this.indicesAttribute.isDateTime()) {
                  seriesBuilder.timeIndex(indexTimeValues);
               }

               seriesBuilder.indexName(this.indicesAttribute.getName());
            }

            Iterator var25 = valuesMap.entrySet().iterator();

            while(true) {
               while(var25.hasNext()) {
                  Entry entry = (Entry)var25.next();
                  Attribute attribute = (Attribute)entry.getKey();
                  List valuesList = (List)entry.getValue();
                  if (attribute.isNumerical()) {
                     seriesBuilder.addRealValues(valuesList, attribute.getName());
                  } else if (attribute.isNominal()) {
                     Integer[] nominalIndices = new Integer[valuesList.size()];
                     int i = 0;

                     for(Iterator var31 = valuesList.iterator(); var31.hasNext(); ++i) {
                        double value = (Double)var31.next();
                        if (Double.isNaN(value)) {
                           nominalIndices[i] = null;
                        } else {
                           nominalIndices[i] = (int)value;
                        }
                     }

                     seriesBuilder.addNominalValues(nominalIndices, (List)nominalMappingMap.get(attribute.getName()), attribute.getName());
                  } else if (attribute.isDateTime()) {
                     List timeValues = new ArrayList();
                     Iterator var16 = valuesList.iterator();

                     while(var16.hasNext()) {
                        double value = (Double)var16.next();
                        if (Double.isNaN(value)) {
                           timeValues.add((Object)null);
                        } else {
                           timeValues.add(Instant.ofEpochMilli((long)value));
                        }
                     }

                     seriesBuilder.addTimeValues(timeValues, attribute.getName());
                  }
               }

               return seriesBuilder.build();
            }
         } catch (Exception var20) {
            throw this.handleException(var20, seriesNames.toString());
         }
      }


   public TimeSeries getInputTimeSeries() throws OperatorException {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("getInputTimeSeries()", "not initialized as inputPortOperator", "asInputPortOperator()");
      } else {
         return this.getInputTimeSeries(this.exampleSetInputPort, this.timeSeriesAttribute);
      }
   }

   public TimeSeries getInputTimeSeries(InputPort inputPort, Attribute seriesAttribute) throws OperatorException {
      switch(this.indiceHandling) {
      case NO_INDICES:
         throw new WrongConfiguredHelperException("getInputTimeSeries()", "set to have NO_INDICES", "setIndicesHandling(MANDATORY_INDICES) or setIndicesHandling(OPTIONAL_INDICES)");
      case OPTIONAL_INDICES:
         if (!this.operator.getParameterAsBoolean("has_indices")) {
            throw new WrongConfiguredHelperException("getInputTimeSeries()", "set to have OPTIONAL_INDICES and no indices attribute is specified.");
         }
      default:
         if (this.inputExampleSet == null) {
            this.readInputData(inputPort);
         }

         if (!this.indicesAttribute.isDateTime()) {
            throw new AttributeWrongTypeError(this.operator, this.indicesAttribute, new int[]{9});
         } else {
            double[] values = new double[this.inputExampleSet.size()];
            ArrayList indices = new ArrayList();

            for(int i = 0; i < values.length; ++i) {
               values[i] = this.inputExampleSet.getExample(i).getValue(seriesAttribute);
               indices.add(this.inputExampleSet.getExample(i).getDateValue(this.indicesAttribute).toInstant());
               this.progressStep(i);
            }

            try {
               return TimeSeries.create(indices, values, seriesAttribute.getName());
            } catch (Exception var6) {
               throw this.handleException(var6, seriesAttribute.getName());
            }
         }
      }
   }

   public MultivariateTimeSeries getInputMultivariateTimeSeries() throws OperatorException {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("getInputMultivariateTimeSeries()", "not initialized as inputPortOperator", "asInputPortOperator()");
      } else {
         return this.getInputMultivariateTimeSeries(this.exampleSetInputPort, this.timeSeriesAttributes);
      }
   }

   public MultivariateTimeSeries getInputMultivariateTimeSeries(InputPort inputPort, Set seriesAttributes) throws OperatorException {
      switch(this.indiceHandling) {
      case NO_INDICES:
         throw new WrongConfiguredHelperException("getInputMultivariateTimeSeries()", "set to have NO_INDICES", "setIndicesHandling(MANDATORY_INDICES) or setIndicesHandling(OPTIONAL_INDICES)");
      case OPTIONAL_INDICES:
         if (!this.operator.getParameterAsBoolean("has_indices")) {
            throw new WrongConfiguredHelperException("getInputMultivariateTimeSeries()", "set to have OPTIONAL_INDICES and no indices attribute is specified.");
         }
      default:
         if (this.inputExampleSet == null) {
            this.readInputData(inputPort);
         }

         Map valuesMap = new LinkedHashMap();
         Iterator var4 = seriesAttributes.iterator();

         while(var4.hasNext()) {
            Attribute attribute = (Attribute)var4.next();
            if (attribute.isNumerical()) {
               valuesMap.put(attribute, new double[this.inputExampleSet.size()]);
            }
         }

         if (valuesMap.isEmpty()) {
            throw new UserError(this.operator, "153", new Object[]{1, 0});
         } else if (!this.indicesAttribute.isDateTime()) {
            throw new AttributeWrongTypeError(this.operator, this.indicesAttribute, new int[]{9});
         } else {
            int exampleCounter = 0;
            int numberOfAttributes = valuesMap.size();
            ArrayList indices = new ArrayList();

            for(Iterator var7 = this.inputExampleSet.iterator(); var7.hasNext(); ++exampleCounter) {
               Example example = (Example)var7.next();
               int attributeCounter = 0;

               for(Iterator var10 = valuesMap.entrySet().iterator(); var10.hasNext(); ++attributeCounter) {
                  Entry entry = (Entry)var10.next();
                  ((double[])entry.getValue())[exampleCounter] = example.getValue((Attribute)entry.getKey());
                  this.progressStep(exampleCounter * numberOfAttributes + attributeCounter);
               }

               indices.add(example.getDateValue(this.indicesAttribute).toInstant());
            }

            ArrayList seriesNames = new ArrayList();
            ArrayList valuesList = new ArrayList();
            Iterator var17 = valuesMap.entrySet().iterator();

            while(var17.hasNext()) {
               Entry entry = (Entry)var17.next();
               seriesNames.add(((Attribute)entry.getKey()).getName());
               valuesList.add(entry.getValue());
            }

            try {
               return MultivariateTimeSeries.create(indices, valuesList, seriesNames);
            } catch (Exception var12) {
               throw this.handleException(var12, seriesNames.toString());
            }
         }
      }
   }

   public void addSeriesToExampleSetOutputPort(ValueSeries valueSeries, ExampleSet outputExampleSet) throws OperatorException {
      if (!this.isOutputPortOperator) {
         throw new WrongConfiguredHelperException("addSeriesToExampleSetOutputPort()", "not initialized as outputPortOperator", "asOutputPortOperator()");
      } else {
         this.addSeriesToExampleSetPort(valueSeries, outputExampleSet, this.exampleSetOutputPort);
      }
   }

   public void addSeriesToExampleSetPort(ValueSeries valueSeries, ExampleSet outputExampleSet, OutputPort outputPort) throws OperatorException {
      if (this.multivariateInput) {
         throw new WrongConfiguredHelperException("addSeriesToExampleSetPort()", "configured to have multivariateInput and the parameters to add a single value series are not present.");
      } else {
         ExampleSet clonedOutputExampleSet = (ExampleSet)outputExampleSet.clone();
         double[] values = valueSeries.getValues();
         Attribute outputAttribute = null;
         Attributes attributes = clonedOutputExampleSet.getAttributes();
         boolean newAttribute = false;
         String newAttributeName = "";
         if (!this.isOutputPortOperator) {
            newAttribute = true;
            newAttributeName = valueSeries.getName();
         }

         if (this.addOverwriteOption) {
            newAttribute = this.operator.getParameterAsBoolean("add_time_series_as_new_attribute");
            newAttributeName = this.operator.getParameterAsString("name_of_new_time_series_attribute");
         }

         if (newAttribute) {
            outputAttribute = AttributeFactory.createAttribute(newAttributeName, 2);
         } else {
            Attribute originalAttribute = attributes.get(this.operator.getParameterAsString("time_series_attribute"));
            if (originalAttribute == null) {
               throw new OperatorException("Time series attribute shall be overwritten but can't be found in the ExampleSet.");
            }

            if (this.changeOutputAttributesToReal && originalAttribute.getValueType() != 4) {
               outputAttribute = AttributeFactory.createAttribute(originalAttribute.getName(), 2);
            } else {
               outputAttribute = (Attribute)originalAttribute.clone();
            }

            attributes.remove(originalAttribute);
         }

         clonedOutputExampleSet.getExampleTable().addAttribute(outputAttribute);
         attributes.addRegular(outputAttribute);
         int i = 0;

         for(Iterator var11 = clonedOutputExampleSet.iterator(); var11.hasNext(); ++i) {
            Example example = (Example)var11.next();
            double value = Double.NaN;
            if (i < values.length) {
               value = values[i];
            }

            example.setValue(outputAttribute, value);
            this.progressStep(i);
         }

         outputPort.deliver(clonedOutputExampleSet);
      }
   }

   public void addMultivariateValueSeriesToExampleSetOutputPort(MultivariateValueSeries multivariateValueSeries, ExampleSet outputExampleSet) throws OperatorException {
      if (!this.isOutputPortOperator) {
         throw new WrongConfiguredHelperException("addMultivariateValueSeriesToExampleSetOutputPort()", "not initialized as outputPortOperator", "asOutputPortOperator()");
      } else {
         this.addMultivariateValueSeriesToExampleSetOutputPort(multivariateValueSeries, outputExampleSet, this.exampleSetOutputPort);
      }
   }

   public void addMultivariateValueSeriesToExampleSetOutputPort(MultivariateValueSeries multivariateValueSeries, ExampleSet outputExampleSet, OutputPort outputPort) throws OperatorException {
      if (!this.multivariateInput) {
         throw new WrongConfiguredHelperException("addMultivariateValueSeriesToExampleSetOutputPort()", "configured to not have multivariateInput and the parameters to add a multivariate value series are not present.");
      } else {
         ExampleSet clonedOutputExampleSet = (ExampleSet)outputExampleSet.clone();
         Map outputAttributes = new LinkedHashMap();
         boolean newAttribute = false;
         String postFix = "";
         if (!this.isOutputPortOperator) {
            newAttribute = true;
         }

         if (this.addOverwriteOption) {
            newAttribute = !this.operator.getParameterAsBoolean("overwrite_attributes");
            postFix = this.operator.getParameterAsString("new_attributes_postfix");
         }

         int exampleCounter;
         for(exampleCounter = 0; exampleCounter < multivariateValueSeries.getSeriesCount(); ++exampleCounter) {
            String seriesName = multivariateValueSeries.getName(exampleCounter);
            AttributeRole outputAttributeRole = null;
            Attributes attributes = clonedOutputExampleSet.getAttributes();
            if (newAttribute) {
               outputAttributeRole = new AttributeRole(AttributeFactory.createAttribute(seriesName + postFix, 2));
            } else {
               AttributeRole originalAttributeRole = attributes.findRoleByName(seriesName);
               if (originalAttributeRole == null) {
                  throw new OperatorException("Time series attribute shall be overwritten but can't be found in the ExampleSet.");
               }

               if (this.changeOutputAttributesToReal && originalAttributeRole.getAttribute().getValueType() != 4) {
                  outputAttributeRole = new AttributeRole(AttributeFactory.createAttribute(originalAttributeRole.getAttribute().getName(), 2));
                  if (originalAttributeRole.isSpecial()) {
                     outputAttributeRole.setSpecial(originalAttributeRole.getSpecialName());
                  }
               } else {
                  outputAttributeRole = (AttributeRole)originalAttributeRole.clone();
               }

               attributes.remove(originalAttributeRole);
            }

            clonedOutputExampleSet.getExampleTable().addAttribute(outputAttributeRole.getAttribute());
            attributes.add(outputAttributeRole);
            outputAttributes.put(seriesName, outputAttributeRole.getAttribute());
         }

         exampleCounter = 0;
         int numberOfAttributes = outputAttributes.size();

         for(Iterator var15 = clonedOutputExampleSet.iterator(); var15.hasNext(); ++exampleCounter) {
            Example example = (Example)var15.next();

            for(int attributeCounter = 0; attributeCounter < multivariateValueSeries.getSeriesCount(); ++attributeCounter) {
               example.setValue((Attribute)outputAttributes.get(multivariateValueSeries.getName(attributeCounter)), multivariateValueSeries.getValue(attributeCounter, exampleCounter));
               this.progressStep(exampleCounter * numberOfAttributes + attributeCounter);
            }
         }

         outputPort.deliver(clonedOutputExampleSet);
      }
   }

   public void addISeriesToExampleSetOutputPort(ISeries series, ExampleSet outputExampleSet) throws OperatorException {
      if (!this.isOutputPortOperator) {
         throw new WrongConfiguredHelperException("addISeriesToExampleSetOutputPort()", "not initialized as outputPortOperator", "asOutputPortOperator()");
      } else {
         this.addISeriesToExampleSetOutputPort(series, outputExampleSet, this.exampleSetOutputPort);
      }
   }

   public void addISeriesToExampleSetOutputPort(ISeries series, ExampleSet outputExampleSet, OutputPort outputPort) throws OperatorException {
      if (!this.multivariateInput) {
         throw new WrongConfiguredHelperException("addISeriesToExampleSetOutputPort()", "configured to not have multivariateInput and the parameters to add a multivariate value series are not present.");
      } else {
         ExampleSet clonedOutputExampleSet = (ExampleSet)outputExampleSet.clone();
         Map outputAttributes = new LinkedHashMap();
         boolean newAttribute = false;
         String postFix = "";
         if (!this.isOutputPortOperator) {
            newAttribute = true;
         }

         if (this.addOverwriteOption) {
            newAttribute = !this.operator.getParameterAsBoolean("overwrite_attributes");
            postFix = this.operator.getParameterAsString("new_attributes_postfix");
         }

         Map realValuesMap = new LinkedHashMap();
         Map timeValuesMap = new LinkedHashMap();
         Map nominalValuesMap = new LinkedHashMap();
         Iterator var11 = series.getSeriesValuesList().iterator();

         while(var11.hasNext()) {
            SeriesValues seriesValues = (SeriesValues)var11.next();
            String seriesName = seriesValues.getName();
            int newAttributeValuesType = 2;
            PolynominalMapping mapping = null;
            NominalValues nominalValues;
            if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.REAL) {
               newAttributeValuesType = 2;
               realValuesMap.put(seriesName, ((RealValues)seriesValues).getValues());
            } else if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.TIME) {
               newAttributeValuesType = 9;
               timeValuesMap.put(seriesName, ((TimeValues)seriesValues).getValues());
            } else if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.NOMINAL) {
               newAttributeValuesType = 1;
               nominalValues = (NominalValues)seriesValues;
               nominalValuesMap.put(seriesName, nominalValues.getNominalIndices());
               mapping = new PolynominalMapping(nominalValues.getIndexToNominalMapAsHashMap());
            }

            nominalValues = null;
            Attributes attributes = clonedOutputExampleSet.getAttributes();
            AttributeRole outputAttributeRole;
            if (newAttribute) {
               outputAttributeRole = new AttributeRole(AttributeFactory.createAttribute(seriesName + postFix, newAttributeValuesType));
            } else {
               AttributeRole originalAttributeRole = attributes.findRoleByName(seriesName);
               if (originalAttributeRole == null) {
                  throw new OperatorException("Time series attribute shall be overwritten but can't be found in the ExampleSet.");
               }

               if (this.changeOutputAttributesToReal && originalAttributeRole.getAttribute().isNumerical() && originalAttributeRole.getAttribute().getValueType() != 4) {
                  outputAttributeRole = new AttributeRole(AttributeFactory.createAttribute(originalAttributeRole.getAttribute().getName(), 4));
                  if (originalAttributeRole.isSpecial()) {
                     outputAttributeRole.setSpecial(originalAttributeRole.getSpecialName());
                  }
               } else {
                  outputAttributeRole = (AttributeRole)originalAttributeRole.clone();
               }

               attributes.remove(originalAttributeRole);
            }

            if (mapping != null) {
               outputAttributeRole.getAttribute().setMapping(mapping);
            }

            clonedOutputExampleSet.getExampleTable().addAttribute(outputAttributeRole.getAttribute());
            attributes.add(outputAttributeRole);
            outputAttributes.put(seriesName, outputAttributeRole.getAttribute());
         }

         int exampleCounter = 0;
         int numberOfAttributes = outputAttributes.size();

         for(Iterator var22 = clonedOutputExampleSet.iterator(); var22.hasNext(); ++exampleCounter) {
            Example example = (Example)var22.next();

            for(int attributeCounter = 0; attributeCounter < series.getNumberOfSeries(); ++attributeCounter) {
               String seriesName = series.getName(attributeCounter);
               Attribute outputAttribute = (Attribute)outputAttributes.get(seriesName);
               Double value = null;
               if (outputAttribute.isNumerical()) {
                  value = (Double)((List)realValuesMap.get(seriesName)).get(exampleCounter);
               } else if (outputAttribute.isNominal()) {
                  Integer intValue = ((Integer[])nominalValuesMap.get(seriesName))[exampleCounter];
                  value = intValue == null ? null : (double)intValue;
               } else if (outputAttribute.isDateTime()) {
                  Instant instantValue = (Instant)((List)timeValuesMap.get(seriesName)).get(exampleCounter);
                  value = instantValue == null ? null : (double)instantValue.toEpochMilli();
               }

               if (value == null) {
                  value = Double.NaN;
               }

               example.setValue(outputAttribute, value);
               this.progressStep(exampleCounter * numberOfAttributes + attributeCounter);
            }
         }

         outputPort.deliver(clonedOutputExampleSet);
      }
   }

   public ExampleSet convertSeriesToExampleSet(Series series) throws OperatorException {
      List listOfNewAtts = new LinkedList();
      Attribute indexAttribute = null;
      ArrayList instantIndices = null;
      double[] doubleIndices = null;
      if (this.indiceType == IndiceType.TIME) {
         indexAttribute = AttributeFactory.createAttribute(this.operator.getParameterAsString("indices_attribute"), 9);
         listOfNewAtts.add(indexAttribute);
         TimeSeries timeSeries = (TimeSeries)series;
         instantIndices = timeSeries.getIndices();
      } else if (this.indiceType == IndiceType.VALUE) {
         indexAttribute = AttributeFactory.createAttribute(this.operator.getParameterAsString("indices_attribute"), 2);
         listOfNewAtts.add(indexAttribute);
         ValueSeries valueSeries = (ValueSeries)series;
         doubleIndices = valueSeries.getIndices();
      }

      Attribute valueAttribute = AttributeFactory.createAttribute(series.getName(), 2);
      listOfNewAtts.add(valueAttribute);
      double[] values = series.getValues();
      ExampleSetBuilder builder = ExampleSets.from(listOfNewAtts);
      if (indexAttribute != null) {
         AttributeRole role = this.inputExampleSet.getAttributes().getRole(this.operator.getParameterAsString("indices_attribute"));
         if (role.isSpecial()) {
            builder.withRole(indexAttribute, role.getSpecialName());
         }
      }

      builder.withBlankSize(series.getLength());
      ExampleSet exampleSet = builder.build();
      int i = 0;

      for(Iterator var11 = exampleSet.iterator(); var11.hasNext(); ++i) {
         Example example = (Example)var11.next();
         if (this.indiceType == IndiceType.TIME) {
            example.setValue(indexAttribute, (double)((Instant)instantIndices.get(i)).toEpochMilli());
         } else if (this.indiceType == IndiceType.VALUE) {
            example.setValue(indexAttribute, doubleIndices[i]);
         }

         example.setValue(valueAttribute, values[i]);
         this.progressStep(i);
      }

      return exampleSet;
   }

   public ExampleSet convertMultivariateSeriesToExampleSet(MultivariateSeries multivariateSeries) throws OperatorException {
      List listOfNewAtts = new LinkedList();
      Attribute indexAttribute = null;
      ArrayList instantIndices = null;
      double[] doubleIndices = null;
      if (this.indiceType == IndiceType.TIME) {
         indexAttribute = AttributeFactory.createAttribute(this.operator.getParameterAsString("indices_attribute"), 9);
         listOfNewAtts.add(indexAttribute);
         MultivariateTimeSeries timeSeries = (MultivariateTimeSeries)multivariateSeries;
         instantIndices = timeSeries.getIndices();
      } else if (this.indiceType == IndiceType.VALUE) {
         indexAttribute = AttributeFactory.createAttribute(this.operator.getParameterAsString("indices_attribute"), 2);
         listOfNewAtts.add(indexAttribute);
         MultivariateValueSeries valueSeries = (MultivariateValueSeries)multivariateSeries;
         doubleIndices = valueSeries.getIndices();
      }

      Map seriesAttributes = new LinkedHashMap();
      String[] var7 = multivariateSeries.getSeriesNames();
      int var8 = var7.length;

      int exampleCounter;
      for(exampleCounter = 0; exampleCounter < var8; ++exampleCounter) {
         String name = var7[exampleCounter];
         Attribute valueAttribute = AttributeFactory.createAttribute(name, 2);
         listOfNewAtts.add(valueAttribute);
         seriesAttributes.put(name, valueAttribute);
      }

      ExampleSetBuilder builder = ExampleSets.from(listOfNewAtts);
      Iterator var19 = listOfNewAtts.iterator();

      while(var19.hasNext()) {
         Attribute attribute = (Attribute)var19.next();
         AttributeRole role = this.inputExampleSet.getAttributes().getRole(attribute.getName());
         if (role != null && role.isSpecial()) {
            builder.withRole(attribute, role.getSpecialName());
         }
      }

      builder.withBlankSize(multivariateSeries.getLength());
      ExampleSet exampleSet = builder.build();
      exampleCounter = 0;
      int numberOfExamples = exampleSet.size();

      for(Iterator var24 = exampleSet.iterator(); var24.hasNext(); ++exampleCounter) {
         Example example = (Example)var24.next();
         if (this.indiceType == IndiceType.TIME) {
            example.setValue(indexAttribute, (double)((Instant)instantIndices.get(exampleCounter)).toEpochMilli());
         } else if (this.indiceType == IndiceType.VALUE) {
            example.setValue(indexAttribute, doubleIndices[exampleCounter]);
         }
      }

      for(int attributeCounter = 0; attributeCounter < multivariateSeries.getSeriesCount(); ++attributeCounter) {
         String seriesName = multivariateSeries.getName(attributeCounter);
         double[] values = multivariateSeries.getValues(attributeCounter);
         exampleCounter = 0;

         for(Iterator var14 = exampleSet.iterator(); var14.hasNext(); ++exampleCounter) {
            Example example = (Example)var14.next();
            example.setValue((Attribute)seriesAttributes.get(seriesName), values[exampleCounter]);
            this.progressStep(attributeCounter * numberOfExamples + exampleCounter);
         }
      }

      return exampleSet;
   }

   public ExampleSet convertISeriesToExampleSet(ISeries series) throws ProcessStoppedException {
      List<Attribute> listOfNewAtts = new LinkedList();
      Attribute indexAttribute = null;
      List instantIndices = null;
      List doubleIndices = null;
      if (this.indiceType == IndiceType.TIME) {
         ITimeIndexSeries<?> timeSeries = (ITimeIndexSeries)series;
         instantIndices = timeSeries.getIndexValues();
         indexAttribute = AttributeFactory.createAttribute(timeSeries.getIndexName(), 9);
         listOfNewAtts.add(indexAttribute);
      } else if (this.indiceType == IndiceType.VALUE) {
         IRealIndexSeries<?> realSeries = (IRealIndexSeries)series;
         doubleIndices = realSeries.getIndexValues();
         indexAttribute = AttributeFactory.createAttribute(realSeries.getIndexName(), 2);
         listOfNewAtts.add(indexAttribute);
      }

      boolean skipSeries = false;
      if (indexAttribute != null && series.hasSeries(indexAttribute.getName())) {
         if (series.getNumberOfSeries() == 1) {
            skipSeries = true;
         } else {
            series.removeSeries(indexAttribute.getName());
         }
      }

      Map seriesAttributes = new LinkedHashMap();
      if (!skipSeries) {
         Iterator var8 = series.getSeriesValuesList().iterator();

         while(var8.hasNext()) {
            SeriesValues seriesValues = (SeriesValues)var8.next();
            String name = seriesValues.getName();
            PolynominalMapping mapping = null;
            byte newAttributeValuesType;
            if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.REAL) {
               newAttributeValuesType = 2;
            } else if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.TIME) {
               newAttributeValuesType = 9;
            } else {
               if (seriesValues.getValuesType() != SeriesBuilder.ValuesType.NOMINAL) {
                  throw new IllegalArgumentException("not supported valuetype for series: " + name);
               }

               newAttributeValuesType = 1;
               mapping = new PolynominalMapping(((NominalValues)seriesValues).getIndexToNominalMapAsHashMap());
            }

            Attribute valueAttribute = AttributeFactory.createAttribute(name, newAttributeValuesType);
            if (mapping != null) {
               valueAttribute.setMapping(mapping);
            }

            listOfNewAtts.add(valueAttribute);
            seriesAttributes.put(name, valueAttribute);
         }
      }

      ExampleSetBuilder builder = ExampleSets.from(listOfNewAtts);
      Iterator var25 = listOfNewAtts.iterator();

      while(var25.hasNext()) {
         Attribute attribute = (Attribute)var25.next();
         AttributeRole role = this.inputExampleSet.getAttributes().getRole(attribute.getName());
         if (role != null && role.isSpecial()) {
            builder.withRole(attribute, role.getSpecialName());
         }
      }

      builder.withBlankSize(series.getLength());
      ExampleSet exampleSet = builder.build();
      int exampleCounter = 0;
      int numberOfExamples = exampleSet.size();

      for(Iterator var31 = exampleSet.iterator(); var31.hasNext(); ++exampleCounter) {
         Example example = (Example)var31.next();
         if (this.indiceType == IndiceType.TIME) {
            example.setValue(indexAttribute, (double)((Instant)instantIndices.get(exampleCounter)).toEpochMilli());
         } else if (this.indiceType == IndiceType.VALUE) {
            example.setValue(indexAttribute, (Double)doubleIndices.get(exampleCounter));
         }
      }

      if (!skipSeries) {
         for(int attributeCounter = 0; attributeCounter < series.getNumberOfSeries(); ++attributeCounter) {
            String seriesName = series.getName(attributeCounter);
            SeriesValues seriesValues = series.getSeriesValues(attributeCounter);
            List realValues = null;
            List timeValues = null;
            Integer[] nominalValues = null;
            if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.REAL) {
               realValues = ((RealValues)seriesValues).getValues();
            } else if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.NOMINAL) {
               nominalValues = ((NominalValues)seriesValues).getNominalIndices();
            } else {
               if (seriesValues.getValuesType() != SeriesBuilder.ValuesType.TIME) {
                  throw new IllegalArgumentException("not supported valuetype for series: " + seriesName);
               }

               timeValues = ((TimeValues)seriesValues).getValues();
            }

            exampleCounter = 0;
            Attribute seriesAttribute = (Attribute)seriesAttributes.get(seriesName);

            for(Iterator var19 = exampleSet.iterator(); var19.hasNext(); ++exampleCounter) {
               Example example = (Example)var19.next();
               Double value = null;
               if (realValues != null) {
                  value = (Double)realValues.get(exampleCounter);
               } else if (nominalValues != null) {
                  value = nominalValues[exampleCounter] == null ? null : (double)nominalValues[exampleCounter];
               } else if (timeValues != null) {
                  value = timeValues.get(exampleCounter) == null ? null : (double)((Instant)timeValues.get(exampleCounter)).toEpochMilli();
               }

               if (value == null) {
                  value = Double.NaN;
               }

               example.setValue(seriesAttribute, value);
               this.progressStep(attributeCounter * numberOfExamples + exampleCounter);
            }
         }
      }

      return exampleSet;
   }

   public List getParameterTypes(List types) {
      if (this.isInputPortOperator) {
         if (this.multivariateInput) {
            types.addAll(this.attributeSubsetSelector.getParameterTypes());
         } else {
            types.add(new ParameterTypeAttribute("time_series_attribute", "The attribute containing the values of the time series.", this.exampleSetInputPort, this.getAllowedValuesTypes(this.valuesType)));
         }

         switch(this.indiceHandling) {
         case NO_INDICES:
         default:
            break;
         case OPTIONAL_INDICES:
            types.add(new ParameterTypeBoolean("has_indices", "This parameter indicates if there is an index attribute associated with the time series.", false, false));
            ParameterType type = new ParameterTypeAttribute("indices_attribute", "The attribute containing the indices of the time series.", this.exampleSetInputPort, true, false, new int[]{9, 2});
            type.registerDependencyCondition(new BooleanParameterCondition(this.operator, "has_indices", true, true));
            types.add(type);
            break;
         case MANDATORY_INDICES:
            types.add(new ParameterTypeAttribute("indices_attribute", "The attribute containing the indices of the time series.", this.exampleSetInputPort, new int[]{9, 2}));
         }
      }

      if (this.isOutputPortOperator) {
         ParameterTypeString type;
         if (this.multivariateInput) {
            if (this.addOverwriteOption) {
               types.add(new ParameterTypeBoolean("overwrite_attributes", "If selected the original time series will be overwritten with the resulting time series.", true, false));
            }

            type = new ParameterTypeString("new_attributes_postfix", "The resulting time series are added as new attributes to the ExampleSet. Their name is the name of the original time series plus the postfix specified by this parameter.", this.defaultNewAttributesPostfix, false);
            if (this.addOverwriteOption) {
               type.registerDependencyCondition(new BooleanParameterCondition(this.operator, "overwrite_attributes", false, false));
            }

            types.add(type);
         } else {
            if (this.addOverwriteOption) {
               types.add(new ParameterTypeBoolean("add_time_series_as_new_attribute", "This parameter indicates if the resulting time series will be added as new attribute to the ExampleSet or that the original time series will be overwritten.", true, false));
            }

            type = new ParameterTypeString("name_of_new_time_series_attribute", "The name of the new time series attribute.", this.defaultNewAttributeName, false);
            if (this.addOverwriteOption) {
               type.registerDependencyCondition(new BooleanParameterCondition(this.operator, "add_time_series_as_new_attribute", false, true));
            }

            types.add(type);
         }
      }

      return types;
   }

   public List getSelectedTimeSeriesAttributesMetaData(InputPort inputPort) throws WrongConfiguredHelperException, IncompatibleMDClassException, UndefinedParameterError {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("getSelectedAttributesMetaData()", "not initialized as inputPortOperator", "asInputPortOperator()");
      } else {
         List result = new ArrayList();
         if (!inputPort.isConnected()) {
            return result;
         } else {
            ExampleSetMetaData inputMetaData = (ExampleSetMetaData)inputPort.getMetaData(ExampleSetMetaData.class);
            if (inputMetaData == null) {
               return result;
            } else {
               if (this.multivariateInput) {
                  ExampleSetMetaData filteredData = this.attributeSubsetSelector.getMetaDataSubset(inputMetaData, this.includeSpecialAttributes);
                  Iterator var5 = filteredData.getAllAttributes().iterator();

                  while(var5.hasNext()) {
                     AttributeMetaData amd = (AttributeMetaData)var5.next();
                     result.add(amd);
                  }
               } else {
                  result.add(inputMetaData.getAttributeByName(this.operator.getParameterAsString("time_series_attribute")));
               }

               return result;
            }
         }
      }
   }

   public AttributeMetaData getIndicesAttributeMetaData(InputPort inputPort) throws WrongConfiguredHelperException, IncompatibleMDClassException, UndefinedParameterError {
      if (!this.isInputPortOperator) {
         throw new WrongConfiguredHelperException("getSelectedAttributesMetaData()", "not initialized as inputPortOperator", "asInputPortOperator()");
      } else if (inputPort.isConnected() && this.operator.getParameterAsBoolean("has_indices")) {
         ExampleSetMetaData inputMetaData = (ExampleSetMetaData)inputPort.getMetaData(ExampleSetMetaData.class);
         return inputMetaData == null ? null : inputMetaData.getAttributeByName(this.operator.getParameterAsString("indices_attribute"));
      } else {
         return null;
      }
   }

   public void progressStep(int i) throws ProcessStoppedException {
      if (this.callProgressStep && (i == -1 || (i + 1) % 2000000 == 0)) {
         this.operator.getProgress().step();
      }

   }

   public void enableCallProgressStep() {
      this.callProgressStep = true;
   }

   public int progressCallsInGetAddConvertMethods() throws UserError {
      if (this.inputExampleSet == null) {
         this.readInputData(this.exampleSetInputPort);
      }

      return this.inputExampleSet.size() * this.getNumberOfAttributes() / 2000000;
   }

   public int getNumberOfAttributes() throws UserError {
      if (this.multivariateInput) {
         if (this.inputExampleSet == null) {
            this.readInputData(this.exampleSetInputPort);
         }

         return this.timeSeriesAttributes.size();
      } else {
         return 1;
      }
   }

   public OperatorException handleException(Exception e, String timeSeriesNames) throws OperatorException {
      if (e instanceof UserError) {
         throw (UserError)e;
      } else if (e instanceof NotStrictlyMonotonicIncreasingException) {
         throw new UserError(this.operator, "time_series_extension.timeseries.indices_not_strictly_monotonic_increasing", new Object[]{this.operator.getParameterAsString("indices_attribute")});
      } else {
         String attributeString;
         String attributeStringValue;
         if (e instanceof SeriesContainsInvalidValuesException) {
            SeriesContainsInvalidValuesException invalidValuesException = (SeriesContainsInvalidValuesException)e;
            attributeString = "time series";
            attributeStringValue = invalidValuesException.getSeriesName();
            String invalidValueTypeStringForUserError = invalidValuesException.getType().toString();
            if (attributeStringValue.contains("indices") || attributeStringValue.contains("index")) {
               attributeString = "indices";
               attributeStringValue = this.operator.getParameterAsString("indices_attribute");
            }

            if (attributeStringValue.contains("values") || attributeStringValue.isEmpty()) {
               attributeStringValue = timeSeriesNames;
            }

            if (invalidValuesException.getType() == SeriesContainsInvalidValuesException.InvalidValuesType.NAN) {
               invalidValueTypeStringForUserError = "missing";
            }

            throw new UserError(this.operator, "time_series_extension.timeseries.attribute_non_finite_values", new Object[]{attributeString, invalidValueTypeStringForUserError, attributeStringValue});
         } else if (!(e instanceof ArgumentIsEmptyException)) {
            if (e instanceof IllegalIndexArgumentException) {
               IllegalIndexArgumentException indexArgumentException = (IllegalIndexArgumentException)e;
               Number number = indexArgumentException.getNumber();
               throw new UserError(this.operator, 207, new Object[]{number != null ? number.toString() : "", indexArgumentException.getIndexName(), indexArgumentException.getMessage()});
            } else if (e instanceof IllegalSeriesLengthException) {
               return new OperatorException(e.getMessage());
            } else if (e instanceof ArgumentIsNullException) {
               return new OperatorException(e.getMessage());
            } else if (e instanceof RuntimeException) {
               throw (RuntimeException)e;
            } else {
               return new OperatorException(e.getLocalizedMessage(), e.getCause());
            }
         } else {
            ArgumentIsEmptyException isEmptyException = (ArgumentIsEmptyException)e;
            attributeString = "time series";
            attributeStringValue = isEmptyException.getArgumentName();
            if (attributeStringValue.contains("indices") || attributeStringValue.contains("index")) {
               attributeString = "indices";
               attributeStringValue = this.operator.getParameterAsString("indices_attribute");
            }

            if (attributeStringValue.contains("values") || attributeStringValue.isEmpty()) {
               attributeStringValue = timeSeriesNames;
            }

            throw new UserError(this.operator, "time_series_extension.timeseries.attribute_length_0", new Object[]{attributeString, attributeStringValue});
         }
      }
   }

   public InputPort getExampleSetInputPort() {
      return this.exampleSetInputPort;
   }

   public ExampleSet getInputExampleSet() {
      return this.inputExampleSet;
   }

   public void setInputExampleSet(ExampleSet inputExampleSet) {
      this.inputExampleSet = inputExampleSet;
   }

   public OutputPort getExampleSetOutputPort() {
      return this.exampleSetOutputPort;
   }

   public AttributeSubsetSelector getAttributeSubsetSelector() {
      return this.attributeSubsetSelector;
   }

   public IndiceType getIndiceType() {
      return this.indiceType;
   }

   public void setIndiceType(IndiceType indiceType) {
      this.indiceType = indiceType;
   }

   public IndiceHandling getIndiceHandling() {
      return this.indiceHandling;
   }

   public boolean isInputPortOperator() {
      return this.isInputPortOperator;
   }

   public boolean isOutputPortOperator() {
      return this.isOutputPortOperator;
   }

   public boolean isMultivariateInput() {
      return this.multivariateInput;
   }

   public boolean isIncludeSpecialAttributes() {
      return this.includeSpecialAttributes;
   }

   public boolean isChangeOutputAttributesToReal() {
      return this.changeOutputAttributesToReal;
   }

   public boolean isAddOverwriteOption() {
      return this.addOverwriteOption;
   }

   public String getDefaultNewAttributeName() {
      return this.defaultNewAttributeName;
   }

   public String getDefaultNewAttributesPostfix() {
      return this.defaultNewAttributesPostfix;
   }

   protected int[] getAllowedValuesTypes(SeriesBuilder.ValuesType type) {
      switch(type) {
      case REAL:
         return new int[]{2, 4, 3};
      case NOMINAL:
         return new int[]{1, 7, 6, 5};
      case MIXED:
         return new int[]{2, 4, 3, 1, 7, 6, 5, 10, 9, 11};
      default:
         return new int[]{2, 4, 3};
      }
   }

   public static enum IndiceType {
      DEFAULT,
      VALUE,
      TIME;
   }

   public static enum IndiceHandling {
      NO_INDICES,
      OPTIONAL_INDICES,
      MANDATORY_INDICES;
   }
}
