package base.operators.operator.timeseries.operator.helper;

import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.NominalMapping;
import base.operators.example.table.PolynominalMapping;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.UserError;
import base.operators.operator.error.AttributeNotFoundError;
import base.operators.operator.error.AttributeWrongTypeError;
import base.operators.operator.ports.IncompatibleMDClassException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.*;
import base.operators.operator.ports.metadata.MDNumber.Relation;
import base.operators.operator.tools.AttributeSubsetSelector;
import base.operators.parameter.*;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.*;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.NominalValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IDefaultIndexSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealIndexSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ITimeIndexSeries;
import base.operators.operator.timeseries.timeseriesanalysis.window.ArrayIndicesWindow;
import base.operators.operator.timeseries.timeseriesanalysis.window.factory.WindowFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.*;

public class WindowingHelper<T extends Operator, S extends ISeries<?, ?>>
        extends ExampleSetTimeSeriesHelper<T, S> {
   public static final String PARAMETER_WINDOW_SIZE = "window_size";
   public static final String PARAMETER_STEP_SIZE = "step_size";
   public static final String PARAMETER_CREATE_LABELS = "create_horizon_(labels)";
   public static final String PARAMETER_HORIZON_ATTRIBUTE = "horizon_attribute";
   public static final String PARAMETER_HORIZON_SIZE = "horizon_size";
   public static final String PARAMETER_HORIZON_OFFSET = "horizon_offset";
   public static final String PARAMETER_NO_OVERLAPING_WINDOWS = "no_overlapping_windows";
   private final boolean mandatoryHorizon;
   private final HorizonAttributeSelection horizonAttributeSelection;
   protected final AttributeSubsetSelector horizonAttributeSubsetSelector;
   private Series inputSeries = null;
   private MultivariateSeries inputMultivariateSeries = null;
   private Series horizonSeries = null;
   private MultivariateSeries horizonMultivariateSeries = null;
   private ISeries inputISeries = null;
   private ISeries horizonISeries = null;
   private Map inputNominalMappingMap = null;
   protected Set horizonSeriesAttributes;
   protected Attribute horizonSeriesAttribute;
   private Map horizonNominalMappingMap = null;
   private int windowSize;
   private boolean createLabels;
   private int horizonWidth;
   private int horizonOffset;
   private boolean noOverlapingWindows;
   private int stepSize;

   public WindowingHelper(Operator operator, boolean isInputPortOperator, String inputPortName, boolean isOutputPortOperator, String outputPortName, boolean hasMultivariateInput, boolean includeSpecialAttributes, ExampleSetTimeSeriesHelper.IndiceHandling indiceHandling, boolean createPorts, boolean addOverwriteOption, String defaultNameOrPostfix, boolean changeOutputAttributesToReal, SeriesBuilder.ValuesType valuesType, boolean useISeries, boolean mandatoryHorizon, HorizonAttributeSelection horizonAttributeSelection) {
      super(operator, isInputPortOperator, inputPortName, isOutputPortOperator, outputPortName, hasMultivariateInput, includeSpecialAttributes, indiceHandling, createPorts, addOverwriteOption, defaultNameOrPostfix, changeOutputAttributesToReal, valuesType, useISeries);
      this.mandatoryHorizon = mandatoryHorizon;
      this.horizonAttributeSelection = horizonAttributeSelection;
      if (horizonAttributeSelection == HorizonAttributeSelection.MULTI) {
         this.horizonAttributeSubsetSelector = new AttributeSubsetSelector(operator, this.exampleSetInputPort, this.getAllowedValuesTypes(valuesType));
      } else {
         this.horizonAttributeSubsetSelector = null;
      }

   }

   public WindowingHelper clone(Operator operator) throws UndefinedParameterError {
      String inputPortName = "";
      String outputPortName = "";
      if (this.isInputPortOperator()) {
         inputPortName = this.exampleSetInputPort.getName();
      }

      if (this.isOutputPortOperator()) {
         outputPortName = this.exampleSetOutputPort.getName();
      }

      String defaultNewNameOrPostfix = "";
      if (this.isMultivariateInput()) {
         defaultNewNameOrPostfix = this.getDefaultNewAttributesPostfix();
      } else {
         defaultNewNameOrPostfix = this.getDefaultNewAttributeName();
      }

      WindowingHelper clone = new WindowingHelper(operator, this.isInputPortOperator(), inputPortName, this.isOutputPortOperator(), outputPortName, this.isMultivariateInput(), this.isIncludeSpecialAttributes(), this.getIndiceHandling(), false, this.isAddOverwriteOption(), defaultNewNameOrPostfix, this.isChangeOutputAttributesToReal(), this.valuesType, this.useISeries, this.mandatoryHorizon, this.horizonAttributeSelection);
      clone.setIndiceType(this.getIndiceType());
      if (this.inputSeries != null) {
         clone.setInputSeries(this.inputSeries);
      }

      if (this.inputMultivariateSeries != null) {
         clone.setInputMultivariateSeries(this.inputMultivariateSeries);
      }

      if (this.horizonSeries != null) {
         clone.setHorizonSeries(this.horizonSeries);
      }

      if (this.horizonMultivariateSeries != null) {
         clone.setHorizonMultivariateSeries(this.horizonMultivariateSeries);
      }

      if (this.inputISeries != null) {
         clone.setInputISeries(this.inputISeries);
      }

      if (this.horizonISeries != null) {
         clone.setHorizonISeries(this.horizonISeries);
      }

      if (this.inputExampleSet != null) {
         clone.setInputExampleSet(this.inputExampleSet);
      }

      clone.updateWindowParameterSettings();
      return clone;
   }

   public void resetHelper() throws UndefinedParameterError {
      super.resetHelper();
      this.inputSeries = null;
      this.inputMultivariateSeries = null;
      this.horizonSeries = null;
      this.horizonMultivariateSeries = null;
      this.horizonSeriesAttribute = null;
      this.horizonSeriesAttributes = null;
      this.updateWindowParameterSettings();
   }

   public void readInputData(InputPort inputPort) throws UserError {
      super.readInputData(inputPort);
      this.horizonSeriesAttribute = null;
      this.horizonSeriesAttributes = null;
      if (this.createLabels) {
         switch(this.horizonAttributeSelection) {
         case SINGLE:
            this.horizonSeriesAttribute = this.inputExampleSet.getAttributes().get(this.operator.getParameterAsString("horizon_attribute"));
            if (this.horizonSeriesAttribute == null) {
               throw new AttributeNotFoundError(this.operator, "horizon_attribute", this.operator.getParameterAsString("horizon_attribute"));
            }

            if (!ArrayUtils.contains(this.getAllowedValuesTypes(this.valuesType), this.horizonSeriesAttribute.getValueType())) {
               throw new AttributeWrongTypeError(this.operator, this.horizonSeriesAttribute, this.getAllowedValuesTypes(this.valuesType));
            }
            break;
         case MULTI:
            Iterator subsetAttributeIterator = this.horizonAttributeSubsetSelector.getSubset(this.inputExampleSet, this.isIncludeSpecialAttributes(), true).getAttributes().allAttributes();
            this.horizonSeriesAttributes = new LinkedHashSet();

            while(subsetAttributeIterator.hasNext()) {
               this.horizonSeriesAttributes.add(subsetAttributeIterator.next());
            }
         }

         if (this.useISeries) {
            this.horizonSeriesAttributes = new LinkedHashSet();
            this.horizonSeriesAttributes.add(this.horizonSeriesAttribute);
            this.inputNominalMappingMap = null;
            this.horizonNominalMappingMap = null;
         }
      }

   }

   public void updateWindowParameterSettings() throws UndefinedParameterError {
      this.windowSize = this.operator.getParameterAsInt("window_size");
      if (this.mandatoryHorizon) {
         this.createLabels = true;
         this.horizonOffset = 0;
      } else {
         this.createLabels = this.operator.getParameterAsBoolean("create_horizon_(labels)");
      }

      if (this.createLabels) {
         this.horizonWidth = this.operator.getParameterAsInt("horizon_size");
         if (!this.mandatoryHorizon) {
            this.horizonOffset = this.operator.getParameterAsInt("horizon_offset");
         }
      } else {
         this.horizonWidth = 0;
         this.horizonOffset = 0;
      }

      this.noOverlapingWindows = this.operator.getParameterAsBoolean("no_overlapping_windows");
      if (this.noOverlapingWindows) {
         this.stepSize = this.windowSize + this.horizonOffset + this.horizonWidth;
      } else {
         this.stepSize = this.operator.getParameterAsInt("step_size");
      }

   }

   public void addWindowMetaDataRule(final OutputPort outputPort) throws WrongConfiguredHelperException {
      if (!this.isInputPortOperator()) {
         throw new WrongConfiguredHelperException("addWindowMetaDataRule()", "not initialized as inputPortOperator", "asInputPortOperator()");
      } else {
         this.operator.getTransformer().addRule(new MDTransformationRule() {
            public void transformMD() {
               ExampleSetMetaData md = new ExampleSetMetaData();
               if (WindowingHelper.this.exampleSetInputPort.isConnected()) {
                  try {
                     ExampleSetMetaData inputMD = (ExampleSetMetaData)WindowingHelper.this.exampleSetInputPort.getMetaData(ExampleSetMetaData.class);
                     if (inputMD != null) {
                        AttributeMetaData amd;
                        if (WindowingHelper.this.operator.getParameterAsBoolean("has_indices")) {
                           amd = inputMD.getAttributeByName(WindowingHelper.this.operator.getParameterAsString("indices_attribute"));
                           if (amd != null) {
                              md.addAttribute(amd);
                           }
                        }

                        if (WindowingHelper.this.isMultivariateInput()) {
                           ExampleSetMetaData selectedMD = WindowingHelper.this.attributeSubsetSelector.getMetaDataSubset(inputMD, WindowingHelper.this.isIncludeSpecialAttributes());
                           Iterator var4 = selectedMD.getAllAttributes().iterator();

                           while(var4.hasNext()) {
                              AttributeMetaData amdx = (AttributeMetaData)var4.next();
                              if (ArrayUtils.contains(WindowingHelper.this.getAllowedValuesTypes(WindowingHelper.this.valuesType), amdx.getValueType())) {
                                 md.addAttribute(amdx);
                              }
                           }
                        } else {
                           amd = inputMD.getAttributeByName(WindowingHelper.this.operator.getParameterAsString("time_series_attribute"));
                           if (amd != null) {
                              md.addAttribute(amd);
                           }
                        }

                        md.setNumberOfExamples(WindowingHelper.this.operator.getParameterAsInt("window_size"));
                     }
                  } catch (IncompatibleMDClassException | UndefinedParameterError var6) {
                     var6.printStackTrace();
                  }
               }

               outputPort.deliverMD(md);
            }
         });
      }
   }

   public void addWindowParameterPreconditions() {
      this.exampleSetInputPort.addPrecondition(new ExampleSetPrecondition(this.exampleSetInputPort) {
         public void makeAdditionalChecks(ExampleSetMetaData emd) throws UndefinedParameterError {
            WindowingHelper.this.updateWindowParameterSettings();
            MDInteger numberOfExamples = emd.getNumberOfExamples();
            if (numberOfExamples.getRelation() == Relation.EQUAL || numberOfExamples.getRelation() == Relation.AT_MOST) {
               int numberOfTrainingWindows = WindowingHelper.this.getNumberOfTrainingWindows((Integer)numberOfExamples.getNumber());
               if (numberOfTrainingWindows <= 0) {
                  this.createError(Severity.ERROR, "time_series_extension.parameters.windowing.number_of_parameters_too_large", new Object[]{WindowingHelper.this.windowSize, WindowingHelper.this.horizonWidth, WindowingHelper.this.horizonOffset, numberOfExamples.toString()});
               }
            }

         }
      });
   }

   public void checkWindowParameterSettings() throws UserError {
      this.updateWindowParameterSettings();
      if (this.inputExampleSet == null) {
         this.readInputData(this.exampleSetInputPort);
      }

      int numberOfTrainingWindows = this.getNumberOfTrainingWindows(this.inputExampleSet.size());
      if (numberOfTrainingWindows <= 0) {
         int parameterSum = this.windowSize + this.horizonWidth + this.horizonOffset;
         throw new UserError(this.operator, "time_series_extension.parameter.timeseries_length_larger_than_parameter", new Object[]{"Sum of window size, horizon size and horizon offset", "larger", parameterSum, this.inputExampleSet.size()});
      }
   }

   public List createLastIndexInWindowAttributeMetaData(ExampleSetMetaData inputMD) throws UndefinedParameterError {
      List amdList = new ArrayList();
      if (this.operator.getParameterAsBoolean("has_indices")) {
         String indicesAttributeName = this.operator.getParameterAsString("indices_attribute");
         AttributeMetaData indicesAttribute = inputMD.getAttributeByName(indicesAttributeName);
         if (indicesAttribute != null) {
            AttributeMetaData lastIndexAttribute = null;
            if (indicesAttribute.isDateTime()) {
               lastIndexAttribute = new AttributeMetaData("Last " + indicesAttributeName + " in window", 9);
            } else if (indicesAttribute.isNumerical()) {
               lastIndexAttribute = new AttributeMetaData("Last " + indicesAttributeName + " in window", 2);
            }

            if (indicesAttribute.isSpecial() && lastIndexAttribute != null) {
               lastIndexAttribute.setRole(indicesAttribute.getRole());
            }

            amdList.add(lastIndexAttribute);
         }
      }

      return amdList;
   }

   public AttributeRole createLastIndexInWindowAttribute() throws UndefinedParameterError {
      Attribute lastIndexAttribute = null;
      switch(this.getIndiceType()) {
      case DEFAULT:
         return null;
      case TIME:
         lastIndexAttribute = AttributeFactory.createAttribute("Last " + this.operator.getParameterAsString("indices_attribute") + " in window", 9);
         break;
      case VALUE:
         lastIndexAttribute = AttributeFactory.createAttribute("Last " + this.operator.getParameterAsString("indices_attribute") + " in window", 2);
         break;
      default:
         return null;
      }

      AttributeRole newRole = new AttributeRole(lastIndexAttribute);
      AttributeRole role = this.inputExampleSet.getAttributes().getRole(this.operator.getParameterAsString("indices_attribute"));
      if (role.isSpecial()) {
         newRole.setSpecial(role.getSpecialName());
      }

      return newRole;
   }

   public Attribute createAndAddLastIndexAttribute(ExampleSet exampleSet) throws UndefinedParameterError {
      AttributeRole role = this.createLastIndexInWindowAttribute();
      if (role != null) {
         Attributes attributes = exampleSet.getAttributes();
         role.getAttribute().setTableIndex(attributes.size());
         exampleSet.getExampleTable().addAttribute(role.getAttribute());
         if (attributes.getRole(role.getSpecialName()) != null) {
            attributes.addRegular(role.getAttribute());
         } else {
            attributes.add(role);
         }

         return role.getAttribute();
      } else {
         return null;
      }
   }

   public double getLastIndexValue(ArrayIndicesWindow trainingWindow) {
      if (this.useISeries) {
         ISeries windowedSeries;
         if (this.inputISeries.getIndexDimension().getIndexType() == SeriesBuilder.IndexType.DEFAULT) {
            windowedSeries = trainingWindow.getWindowedSeries((ISeries)((IDefaultIndexSeries)this.inputISeries));
            return (double)(Integer)windowedSeries.getIndexValue(windowedSeries.getLength() - 1);
         }

         if (this.inputISeries.getIndexDimension().getIndexType() == SeriesBuilder.IndexType.REAL) {
            windowedSeries = trainingWindow.getWindowedSeries((ISeries)((IRealIndexSeries)this.inputISeries));
            return (Double)windowedSeries.getIndexValue(windowedSeries.getLength() - 1);
         }

         if (this.inputISeries.getIndexDimension().getIndexType() == SeriesBuilder.IndexType.TIME) {
            windowedSeries = trainingWindow.getWindowedSeries((ISeries)((ITimeIndexSeries)this.inputISeries));
            return (double)((Instant)windowedSeries.getIndexValue(windowedSeries.getLength() - 1)).toEpochMilli();
         }
      }

      if (this.isMultivariateInput()) {
         if (this.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.TIME) {
            MultivariateTimeSeries timeSeries = (MultivariateTimeSeries)trainingWindow.getWindowedSeries(this.inputMultivariateSeries);
            return (double)((Instant)timeSeries.getIndices().get(timeSeries.getLength() - 1)).toEpochMilli();
         } else {
            MultivariateValueSeries valueSeries = (MultivariateValueSeries)trainingWindow.getWindowedSeries(this.inputMultivariateSeries);
            return valueSeries.getIndices()[valueSeries.getLength() - 1];
         }
      } else if (this.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.TIME) {
         TimeSeries timeSeries = (TimeSeries)trainingWindow.getWindowedSeries(this.inputSeries);
         return (double)timeSeries.getIndex(timeSeries.getLength() - 1).toEpochMilli();
      } else {
         ValueSeries valueSeries = (ValueSeries)trainingWindow.getWindowedSeries(this.inputSeries);
         return valueSeries.getIndex(valueSeries.getLength() - 1);
      }
   }

   public List createHorizonAttributesMetaData() throws UndefinedParameterError, IncompatibleMDClassException {
      if (!this.createLabels) {
         return new ArrayList();
      } else {
         List horizonAttributeNames = new ArrayList();
         List horizonAttributeTypes = new ArrayList();
         ExampleSetMetaData exampleSetMetaData = null;
         if (this.exampleSetInputPort.isConnected()) {
            exampleSetMetaData = (ExampleSetMetaData)this.exampleSetInputPort.getMetaData(ExampleSetMetaData.class);
         }

         Iterator var4;
         AttributeMetaData amd;
         switch(this.horizonAttributeSelection) {
         case SINGLE:
            horizonAttributeNames.add(this.operator.getParameterAsString("horizon_attribute"));
            if (exampleSetMetaData != null && exampleSetMetaData.getAttributeByName(this.operator.getParameterAsString("horizon_attribute")) != null) {
               horizonAttributeTypes.add(exampleSetMetaData.getAttributeByName(this.operator.getParameterAsString("horizon_attribute")).getValueType());
            } else {
               horizonAttributeTypes.add(0);
            }
            break;
         case MULTI:
            if (exampleSetMetaData != null) {
               var4 = this.horizonAttributeSubsetSelector.getMetaDataSubset(exampleSetMetaData, this.isIncludeSpecialAttributes()).getAllAttributes().iterator();

               while(var4.hasNext()) {
                  amd = (AttributeMetaData)var4.next();
                  horizonAttributeNames.add(amd.getName());
                  horizonAttributeTypes.add(amd.getValueType());
               }
            }
            break;
         case SAME:
            if (this.isMultivariateInput()) {
               if (exampleSetMetaData != null) {
                  var4 = this.attributeSubsetSelector.getMetaDataSubset(exampleSetMetaData, false).getAllAttributes().iterator();

                  while(var4.hasNext()) {
                     amd = (AttributeMetaData)var4.next();
                     horizonAttributeNames.add(amd.getName());
                     horizonAttributeTypes.add(amd.getValueType());
                  }
               }
            } else {
               horizonAttributeNames.add(this.operator.getParameterAsString("time_series_attribute"));
               if (exampleSetMetaData != null && exampleSetMetaData.getAttributeByName("time_series_attribute") != null) {
                  horizonAttributeTypes.add(exampleSetMetaData.getAttributeByName("time_series_attribute").getValueType());
               } else {
                  horizonAttributeTypes.add(0);
               }
            }
         }

         List amdList = new ArrayList();
         String horizonRole = "label";
         boolean addAsLabel = this.horizonWidth == 1 && horizonAttributeNames.size() == 1;

         for(int attributeCounter = 0; attributeCounter < horizonAttributeNames.size(); ++attributeCounter) {
            String horizonAttributeName = (String)horizonAttributeNames.get(attributeCounter);
            int horizonAttributeType = (Integer)horizonAttributeTypes.get(attributeCounter);

            for(int i = 0; i < this.horizonWidth; ++i) {
               if (!addAsLabel) {
                  horizonRole = "horizon ";
                  if (horizonAttributeNames.size() != 1) {
                     horizonRole = horizonRole + "(" + horizonAttributeName + ") ";
                  }

                  horizonRole = horizonRole + "+ " + (i + 1);
               }

               String newAttributeName = horizonAttributeName + " + " + (i + this.horizonOffset + 1) + " (horizon)";
               amdList.add(new AttributeMetaData(newAttributeName, horizonAttributeType, horizonRole));
            }
         }

         return amdList;
      }
   }

   public List createHorizonAttributes(Map mapping) throws UserError {
      List horizonAttributeNames = new ArrayList();
      List horizonAttributeTypes = new ArrayList();
 //     if (this.exampleSetInputPort.isConnected()) {
         if (this.inputExampleSet == null) {
            this.readInputData(this.exampleSetInputPort);
         }

         if (this.createLabels) {
            Iterator var4;
            Attribute horizonAttribute;
            label75:
            switch(this.horizonAttributeSelection) {
            case SINGLE:
               horizonAttributeNames.add(this.horizonSeriesAttribute.getName());
               horizonAttributeTypes.add(this.horizonSeriesAttribute.getValueType());
               break;
            case MULTI:
               var4 = this.horizonSeriesAttributes.iterator();

               while(true) {
                  if (!var4.hasNext()) {
                     break label75;
                  }

                  horizonAttribute = (Attribute)var4.next();
                  horizonAttributeNames.add(horizonAttribute.getName());
                  horizonAttributeTypes.add(this.horizonSeriesAttribute.getValueType());
               }
            case SAME:
               if (this.isMultivariateInput()) {
                  var4 = this.timeSeriesAttributes.iterator();

                  while(var4.hasNext()) {
                     horizonAttribute = (Attribute)var4.next();
                     horizonAttributeNames.add(horizonAttribute.getName());
                     horizonAttributeTypes.add(this.horizonSeriesAttribute.getValueType());
                  }
               } else {
                  horizonAttributeNames.add(this.timeSeriesAttribute.getName());
                  horizonAttributeTypes.add(this.horizonSeriesAttribute.getValueType());
               }
            }
         }
 //     }

      List horizonAttributes = new ArrayList();
      boolean addAsLabel = this.horizonWidth == 1 && horizonAttributeNames.size() == 1;

      for(int attributeCounter = 0; attributeCounter < horizonAttributeNames.size(); ++attributeCounter) {
         String horizonAttributeName = (String)horizonAttributeNames.get(attributeCounter);
         int horizonValueType = (Integer)horizonAttributeTypes.get(attributeCounter);

         for(int i = 0; i < this.horizonWidth; ++i) {
            String newAttributeName = horizonAttributeName + " + " + (i + this.horizonOffset + 1) + " (horizon)";
            Attribute horizonSliceAttribute = AttributeFactory.createAttribute(newAttributeName, horizonValueType);
            if (horizonSliceAttribute.isNominal() && mapping != null && mapping.containsKey(horizonAttributeName)) {
               horizonSliceAttribute.setMapping((NominalMapping)mapping.get(horizonAttributeName));
            }

            AttributeRole role = new AttributeRole(horizonSliceAttribute);
            if (addAsLabel) {
               role.setSpecial("label");
            } else {
               String horizonRole = "horizon ";
               if (horizonAttributeNames.size() != 1) {
                  horizonRole = horizonRole + "(" + horizonAttributeName + ") ";
               }

               horizonRole = horizonRole + "+ " + (i + 1);
               role.setSpecial(horizonRole);
            }

            horizonAttributes.add(role);
         }
      }

      return horizonAttributes;
   }

   public List createAndAddHorizonAttributes(ExampleSet exampleSet, Map mapping) throws UserError {
      List horizonAttributes = null;
      if (this.horizonWidth != 0) {
         List roles = this.createHorizonAttributes(mapping);
         horizonAttributes = new ArrayList();
         Attributes attributes = exampleSet.getAttributes();

         AttributeRole role;
         for(Iterator var6 = roles.iterator(); var6.hasNext(); horizonAttributes.add(role.getAttribute())) {
            role = (AttributeRole)var6.next();
            role.getAttribute().setTableIndex(attributes.size());
            exampleSet.getExampleTable().addAttribute(role.getAttribute());
            if (attributes.getRole(role.getSpecialName()) != null) {
               attributes.addRegular(role.getAttribute());
            } else {
               attributes.add(role);
            }
         }
      }

      return horizonAttributes;
   }

   public List createTrainingWindowAttributesMetaData() throws UndefinedParameterError, IncompatibleMDClassException {
      List attributeNames = new ArrayList();
      List attributeTypes = new ArrayList();
      ExampleSetMetaData exampleSetMetaData = null;
      if (this.exampleSetInputPort.isConnected()) {
         exampleSetMetaData = (ExampleSetMetaData)this.exampleSetInputPort.getMetaData(ExampleSetMetaData.class);
      }

      if (this.isMultivariateInput()) {
         if (exampleSetMetaData != null) {
            Iterator var4 = this.attributeSubsetSelector.getMetaDataSubset(exampleSetMetaData, this.isIncludeSpecialAttributes()).getAllAttributes().iterator();

            while(var4.hasNext()) {
               AttributeMetaData amd = (AttributeMetaData)var4.next();
               if (ArrayUtils.contains(this.getAllowedValuesTypes(this.valuesType), amd.getValueType())) {
                  attributeNames.add(amd.getName());
                  attributeTypes.add(amd.getValueType());
               }
            }
         }
      } else {
         attributeNames.add(this.operator.getParameterAsString("time_series_attribute"));
         if (exampleSetMetaData != null && exampleSetMetaData.getAttributeByName(this.operator.getParameterAsString("horizon_attribute")) != null) {
            attributeTypes.add(exampleSetMetaData.getAttributeByName(this.operator.getParameterAsString("horizon_attribute")).getValueType());
         } else {
            attributeTypes.add(0);
         }
      }

      List amdList = new ArrayList();

      for(int attributeCounter = 0; attributeCounter < attributeNames.size(); ++attributeCounter) {
         String name = (String)attributeNames.get(attributeCounter);
         int type = (Integer)attributeTypes.get(attributeCounter);

         for(int i = 0; i < this.windowSize; ++i) {
            String newAttributeName = name + " - " + (this.windowSize - 1 - i);
            amdList.add(new AttributeMetaData(newAttributeName, type));
         }
      }

      return amdList;
   }

   public List createTrainingWindowAttributes(Map mapping) throws UserError {
      List attributeNames = new ArrayList();
      List attributeTypes = new ArrayList();
//      if (this.exampleSetInputPort.isConnected()) {
         if (this.inputExampleSet == null) {
            this.readInputData(this.exampleSetInputPort);
         }

         if (this.isMultivariateInput()) {
            Iterator var4 = this.timeSeriesAttributes.iterator();

            while(var4.hasNext()) {
               Attribute timeSeriesAttribute = (Attribute)var4.next();
               attributeNames.add(timeSeriesAttribute.getName());
               attributeTypes.add(timeSeriesAttribute.getValueType());
            }
         } else {
            attributeNames.add(this.timeSeriesAttribute.getName());
            attributeTypes.add(this.timeSeriesAttribute.getValueType());
         }
 //     }

      List attributeList = new ArrayList();

      for(int attributeCounter = 0; attributeCounter < attributeNames.size(); ++attributeCounter) {
         String name = (String)attributeNames.get(attributeCounter);
         int valueType = (Integer)attributeTypes.get(attributeCounter);

         for(int i = 0; i < this.windowSize; ++i) {
            String newAttributeName = name + " - " + (this.windowSize - 1 - i);
            Attribute attribute = AttributeFactory.createAttribute(newAttributeName, valueType);
            if (attribute.isNominal() && mapping != null && mapping.containsKey(name)) {
               attribute.setMapping((NominalMapping)mapping.get(name));
            }

            attributeList.add(attribute);
         }
      }

      return attributeList;
   }

   public int getNumberOfTrainingWindows(int lengthOfSeries) {
      return (lengthOfSeries - this.windowSize - this.horizonWidth - this.horizonOffset) / this.stepSize + 1;
   }

   public int getNumberOfTrainingWindows() throws WrongConfiguredHelperException {
      if (this.inputSeries != null) {
         return this.getNumberOfTrainingWindows(this.inputSeries.getLength());
      } else if (this.inputMultivariateSeries != null) {
         return this.getNumberOfTrainingWindows(this.inputMultivariateSeries.getLength());
      } else if (this.inputISeries != null) {
         return this.getNumberOfTrainingWindows(this.inputISeries.getLength());
      } else {
         throw new WrongConfiguredHelperException("getNumberOfTrainingWindows()", "not initialized either with inputSeries nor inputMultivariateSeries");
      }
   }

   public List createWindowsFromInput() throws OperatorException {
      this.updateWindowParameterSettings();
      if (!this.useISeries) {
         boolean timeSeriesInput = this.checkForTimeIndices();
         if (this.isMultivariateInput()) {
            if (timeSeriesInput) {
               this.inputMultivariateSeries = this.getInputMultivariateTimeSeries();
            } else {
               this.inputMultivariateSeries = this.getInputMultivariateValueSeries();
            }
         } else if (timeSeriesInput) {
            this.inputSeries = this.getInputTimeSeries();
         } else {
            this.inputSeries = this.getInputValueSeries();
         }

         if (this.createLabels) {
            switch(this.horizonAttributeSelection) {
            case SINGLE:
               if (timeSeriesInput) {
                  this.horizonSeries = this.getInputTimeSeries(this.exampleSetInputPort, this.horizonSeriesAttribute);
               } else {
                  this.horizonSeries = this.getInputValueSeries(this.exampleSetInputPort, this.horizonSeriesAttribute);
               }
               break;
            case MULTI:
               if (timeSeriesInput) {
                  this.horizonMultivariateSeries = this.getInputMultivariateTimeSeries(this.exampleSetInputPort, this.horizonSeriesAttributes);
               } else {
                  this.horizonMultivariateSeries = this.getInputMultivariateValueSeries(this.exampleSetInputPort, this.horizonSeriesAttributes);
               }
            }
         }

         try {
            return this.isMultivariateInput() ? WindowFactory.slidingWindow.fromMultivariateSeries(this.inputMultivariateSeries, this.windowSize, this.stepSize, this.horizonWidth, this.horizonOffset) : WindowFactory.slidingWindow.fromSeries(this.inputSeries, this.windowSize, this.stepSize, this.horizonWidth, this.horizonOffset);
         } catch (Exception var5) {
            throw this.handleException(var5, this.isMultivariateInput() ? StringUtils.join((Object[])this.inputMultivariateSeries.getSeriesNames(), ",") : this.inputSeries.getName());
         }
      } else {
         this.inputISeries = this.getInputISeriesFromPort();
         this.inputNominalMappingMap = new LinkedHashMap();
         Iterator var1 = this.inputISeries.getSeriesValuesList().iterator();

         SeriesValues seriesValues;
         NominalValues nominalValues;
         while(var1.hasNext()) {
            seriesValues = (SeriesValues)var1.next();
            if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.NOMINAL) {
               nominalValues = (NominalValues)seriesValues;
               this.inputNominalMappingMap.put(nominalValues.getName(), new PolynominalMapping(nominalValues.getIndexToNominalMapAsHashMap()));
            }
         }

         if (this.createLabels) {
            switch(this.horizonAttributeSelection) {
            case SINGLE:
               this.horizonISeries = this.getInputISeriesFromPort(this.exampleSetInputPort, new Attribute[]{this.horizonSeriesAttribute});
               break;
            case MULTI:
               this.horizonISeries = this.getInputISeriesFromPort(this.exampleSetInputPort, (Attribute[])this.horizonSeriesAttributes.toArray(new Attribute[0]));
            }

            this.horizonNominalMappingMap = new LinkedHashMap();
            var1 = this.horizonISeries.getSeriesValuesList().iterator();

            while(var1.hasNext()) {
               seriesValues = (SeriesValues)var1.next();
               if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.NOMINAL) {
                  nominalValues = (NominalValues)seriesValues;
                  this.horizonNominalMappingMap.put(nominalValues.getName(), new PolynominalMapping(nominalValues.getIndexToNominalMapAsHashMap()));
               }
            }
         }

         try {
            return WindowFactory.slidingWindow.fromISeries(this.inputISeries, this.windowSize, this.stepSize, this.horizonWidth, this.horizonOffset);
         } catch (Exception var4) {
            throw this.handleException(var4, StringUtils.join((Object[])this.inputISeries.getSeriesNames(), ","));
         }
      }
   }

   public int progressCallsInCreateWindows() throws UserError {
      int number = this.progressCallsInGetAddConvertMethods();
      if (this.createLabels) {
         switch(this.horizonAttributeSelection) {
         case SINGLE:
            if (this.inputExampleSet == null) {
               this.readInputData(this.exampleSetInputPort);
            }

            number += this.inputExampleSet.size() / 2000000;
            break;
         case MULTI:
            if (this.inputExampleSet == null) {
               this.readInputData(this.exampleSetInputPort);
            }

            int numberHorizonAttributes = this.horizonAttributeSubsetSelector.getSubset(this.inputExampleSet, this.isIncludeSpecialAttributes(), true).getAttributes().allSize();
            number += this.inputExampleSet.size() * numberHorizonAttributes / 2000000;
         case SAME:
         }
      }

      return number;
   }

   public List getParameterTypes(List types) {
      types = super.getParameterTypes(types);
      types.add(new ParameterTypeInt("window_size", "The number of values in one window.", 1, Integer.MAX_VALUE, 20, false));
      types.add(new ParameterTypeBoolean("no_overlapping_windows", "If this parameter is set to true, the parameter stepsize is determined automatically, so that windows and horizons don't overlap.", false, false));
      ParameterType type = new ParameterTypeInt("step_size", "The step size between the first values of two consecutive windows.", 1, Integer.MAX_VALUE, 1, false);
      type.registerDependencyCondition(new BooleanParameterCondition(this.operator, "no_overlapping_windows", false, false));
      types.add(type);
      if (!this.mandatoryHorizon) {
         types.add(new ParameterTypeBoolean("create_horizon_(labels)", "If this parameter is set to true, horizon windows are created.", true, false));
      }

      switch(this.horizonAttributeSelection) {
      case SINGLE:
          type = new ParameterTypeAttribute("horizon_attribute", "The attribute containing the values used for creating the horizon windows.", this.exampleSetInputPort, true, false, this.getAllowedValuesTypes(this.valuesType));
         if (!this.mandatoryHorizon) {
            type.registerDependencyCondition(new BooleanParameterCondition(this.operator, "create_horizon_(labels)", true, true));
         }

         types.add(type);
         break;
      case MULTI:
         List horizonAttributeSubsetSelectorTypes = this.horizonAttributeSubsetSelector.getParameterTypes();

         ParameterType hType;
         for(Iterator var4 = horizonAttributeSubsetSelectorTypes.iterator(); var4.hasNext(); types.add(hType)) {
            hType = (ParameterType)var4.next();
            if (!this.mandatoryHorizon) {
               hType.registerDependencyCondition(new BooleanParameterCondition(this.operator, "create_horizon_(labels)", false, true));
            }
         }
      case SAME:
      }

      type = new ParameterTypeInt("horizon_size", "The number of values taken as the horizon.", 1, Integer.MAX_VALUE, 1, false);
      if (!this.mandatoryHorizon) {
         type.registerDependencyCondition(new BooleanParameterCondition(this.operator, "create_horizon_(labels)", false, true));
      }

      types.add(type);
      if (!this.mandatoryHorizon) {
         type = new ParameterTypeInt("horizon_offset", "The offset between the windows and their corresponding horizons.", 0, Integer.MAX_VALUE, 0, false);
         type.registerDependencyCondition(new BooleanParameterCondition(this.operator, "create_horizon_(labels)", false, true));
         types.add(type);
      }

      return types;
   }

   public OperatorException handleException(Exception e, String timeSeriesNames) throws OperatorException {
      if (e instanceof IllegalArgumentException && e.getMessage().equals("Provided windowSize + horizonWindow + horizonOffset is larger than length of series.")) {
         StringBuilder userErrorString1Builder = new StringBuilder("for the window creation");
         StringBuilder userErrorString2Builder = new StringBuilder("Sum of windowing parameters is larger than length of series.");
         StringBuilder userErrorString3Builder = new StringBuilder("Length of series is: ");
         if (this.inputSeries != null) {
            userErrorString3Builder.append(this.inputSeries.getLength());
         } else if (this.inputISeries != null) {
            userErrorString3Builder.append(this.inputISeries.getLength());
         }

         userErrorString3Builder.append(", sum of window parameters ( ").append("window_size".replace("_", " "));
         if (this.createLabels) {
            userErrorString3Builder.append(" + ").append("horizon_size".replace("_", " "));
            if (!this.mandatoryHorizon) {
               userErrorString3Builder.append(" + ").append("horizon_offset".replace("_", " "));
            }
         }

         userErrorString3Builder.append(" ) is: ").append(this.windowSize + this.horizonWidth + this.horizonOffset);
         throw new UserError(this.operator, "time_series_extension.parameter.parameter_combination_not_allowed", new Object[]{userErrorString1Builder.toString(), userErrorString2Builder.toString(), userErrorString3Builder.toString()});
      } else {
         return super.handleException(e, timeSeriesNames);
      }
   }

   public Series getInputSeries() {
      return this.inputSeries;
   }

   public void setInputSeries(Series inputSeries) {
      this.inputSeries = inputSeries.clone();
   }

   public MultivariateSeries getInputMultivariateSeries() {
      return this.inputMultivariateSeries;
   }

   public void setInputMultivariateSeries(MultivariateSeries inputMultivariateSeries) {
      this.inputMultivariateSeries = inputMultivariateSeries.clone();
   }

   public Series getHorizonSeries() {
      return this.horizonSeries;
   }

   public void setHorizonSeries(Series horizonSeries) {
      this.horizonSeries = horizonSeries;
   }

   public ISeries getInputISeries() {
      return this.inputISeries;
   }

   public void setInputISeries(ISeries inputISeries) {
      this.inputISeries = inputISeries;
   }

   public ISeries getHorizonISeries() {
      return this.horizonISeries;
   }

   public void setHorizonISeries(ISeries horizonISeries) {
      this.horizonISeries = horizonISeries;
   }

   public Map getInputNominalMappingMap() {
      return this.inputNominalMappingMap;
   }

   public Map getHorizonNominalMappingMap() {
      return this.horizonNominalMappingMap;
   }

   public MultivariateSeries getHorizonMultivariateSeries() {
      return this.horizonMultivariateSeries;
   }

   public void setHorizonMultivariateSeries(MultivariateSeries horizonMultivariateSeries) {
      this.horizonMultivariateSeries = horizonMultivariateSeries;
   }

   public int getWindowSize() {
      return this.windowSize;
   }

   public boolean isCreateLabels() {
      return this.createLabels;
   }

   public HorizonAttributeSelection getHorizonAttributeSelection() {
      return this.horizonAttributeSelection;
   }

   public int getHorizonWidth() {
      return this.horizonWidth;
   }

   public int getHorizonOffset() {
      return this.horizonOffset;
   }

   public boolean isNoOverlapingWindows() {
      return this.noOverlapingWindows;
   }

   public int getStepSize() {
      return this.stepSize;
   }

   public boolean isMandatoryHorizon() {
      return this.mandatoryHorizon;
   }

   public static enum HorizonAttributeSelection {
      SAME,
      SINGLE,
      MULTI;
   }
}
