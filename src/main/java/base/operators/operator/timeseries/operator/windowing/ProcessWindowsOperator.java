package base.operators.operator.timeseries.operator.windowing;

import base.operators.MacroHandler;
import base.operators.example.*;
import base.operators.example.table.AttributeFactory;
import base.operators.operator.*;
import base.operators.operator.process_control.loops.AbstractLoopOperator;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.*;
import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesLoopOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WindowingHelper;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.UndefinedParameterError;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.NominalValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.RealValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.TimeValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IDefaultIndexSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealIndexSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ITimeIndexSeries;
import base.operators.operator.timeseries.timeseriesanalysis.window.ArrayIndicesWindow;
import org.apache.commons.math3.util.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProcessWindowsOperator extends ExampleSetTimeSeriesLoopOperator {
   private WindowingHelper<OperatorChain, ISeries<?, ?>> windowingHelper;
   public static final String PARAMETER_ADD_LAST_ID_ATTRIBUTE = "add_last_index_in_window_attribute";
   private OutputPort windowedExampleSetInnerSource = (OutputPort)this.getSubprocess(0).getInnerSources().createPort("windowed example set");
   public static final String WINDOW_ID_ATTRIBUTE_NAME = "Window id";

   public ProcessWindowsOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description, "Process Windows");
      this.windowingHelper.addWindowParameterPreconditions();
      this.windowingHelper.addWindowMetaDataRule(this.windowedExampleSetInnerSource);
      this.getTransformer().addRule(new SubprocessTransformRule(this.getSubprocess(0)));
      this.getTransformer().addRule(new MDTransformationRule()
      {
         public void transformMD()
         {
            try {
               ProcessWindowsOperator.this.windowingHelper.updateWindowParameterSettings();
               for (PortPairExtender.PortPair pair : ProcessWindowsOperator.this.outputPortPairExtender.getManagedPairs()) {
                  MetaData inData = pair.getInputPort().getMetaData();
                  MetaData outData = null;
                  if (inData != null) {
                     outData = inData.clone();
                     if (outData instanceof ExampleSetMetaData) {
                        ExampleSetMetaData emd = (ExampleSetMetaData)outData;

                        ExampleSetMetaData inputMD = (ExampleSetMetaData)ProcessWindowsOperator.this.windowingHelper.getExampleSetInputPort().getMetaData(ExampleSetMetaData.class);
                        if (ProcessWindowsOperator.this.getParameterAsBoolean("add_last_index_in_window_attribute") && inputMD != null) {

                           List<AttributeMetaData> lastIndexInWindowAMDList = ProcessWindowsOperator.this.windowingHelper.createLastIndexInWindowAttributeMetaData(inputMD);
                           if (lastIndexInWindowAMDList.isEmpty()) {
                              AttributeMetaData lastIndexInWindowAMD = new AttributeMetaData("Window id", 3);

                              if (inputMD.hasSpecial("id") != MetaDataInfo.YES) {
                                 lastIndexInWindowAMD.setRole("id");
                              }
                              lastIndexInWindowAMDList.add(lastIndexInWindowAMD);
                           }
                           emd.addAllAttributes(lastIndexInWindowAMDList);
                        }
                        emd.addAllAttributes(ProcessWindowsOperator.this.windowingHelper.createHorizonAttributesMetaData());
                        ExampleSetMetaData exampleSetMetaData = emd;
                     } else {
                        outData = inData.clone();
                     }
                     outData.addToHistory(pair.getOutputPort());
                  }
                  pair.getOutputPort().deliverMD(outData);
               }
            } catch (UndefinedParameterError|base.operators.operator.ports.IncompatibleMDClassException e) {

               e.printStackTrace();
            }
         }
      });
   }

   protected ExampleSetTimeSeriesHelper<OperatorChain, ISeries<?, ?>> initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder<OperatorChain> builder = new TimeSeriesHelperBuilder<OperatorChain>(this);


      builder = builder.asInputPortOperator("example set").setIndiceHandling(ExampleSetTimeSeriesHelper.IndiceHandling.OPTIONAL_INDICES).enableMultivariateInput().includeSpecialAttributes().asWindowingHelper().setHorizonAttributeSelection(WindowingHelper.HorizonAttributeSelection.SINGLE).setValuesType(SeriesBuilder.ValuesType.MIXED).useISeries();
      this.windowingHelper = (WindowingHelper)builder.build();
      return this.windowingHelper;
   }

   public List<ParameterType> getParameterTypes() {
      List<ParameterType> types = this.windowingHelper.getParameterTypes(new ArrayList());
      ParameterType type = new ParameterTypeBoolean("add_last_index_in_window_attribute", "If this parameter is set to true an additional attribute, containing the last index value in the corresponding window in casehas_indices is true, a default integer otherwise, is added to all ExampleSets which are provided at the output port of the inner subprocess.", true, false);
      types.add(type);
      types.addAll(super.getParameterTypes());
      return types;
   }

   protected boolean canReuseResults() {
      return false;
   }

   protected AbstractLoopOperator.LoopArguments<Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>>> prepareArguments(boolean executeParallely) throws OperatorException {
      this.windowingHelper.resetHelper();
      this.windowingHelper.checkWindowParameterSettings();
      List<ArrayIndicesWindow<?>> windows = this.windowingHelper.createWindowsFromInput();
      List<Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>>> dataForIteration = new ArrayList<Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>>>();

      for(int i = 0; i < this.windowingHelper.getNumberOfTrainingWindows(); ++i) {
         ArrayIndicesWindow<?> trainingWindow = null;
         ArrayIndicesWindow<?> horizonWindow = null;
         if (this.getParameterAsBoolean("create_horizon_(labels)")) {
            trainingWindow = (ArrayIndicesWindow)windows.get(2 * i);
            horizonWindow = (ArrayIndicesWindow)windows.get(2 * i + 1);
         } else {
            trainingWindow = (ArrayIndicesWindow)windows.get(i);
         }

         dataForIteration.add(new Pair(trainingWindow, horizonWindow));
      }

      AbstractLoopOperator.LoopArguments<Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>>> arguments = new AbstractLoopOperator.LoopArguments<Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>>>();
      arguments.setDataForIteration(dataForIteration);
      arguments.setNumberOfIterations(this.windowingHelper.getNumberOfTrainingWindows());
      return arguments;
   }

   protected void setMacros(LoopArguments<Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>>> arguments, MacroHandler macroHandler, int iteration) throws OperatorException {
      macroHandler.addMacro("current_window", String.valueOf(iteration));
   }

   @Override
   protected void prepareSingleRun(Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>> dataForIteration, AbstractLoopOperator<Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>>> operator) throws OperatorException {
      ProcessWindowsOperator castOperator = (ProcessWindowsOperator)operator;
      ISeries<?, ?> windowedSeries = null;
      if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.DEFAULT) {
         windowedSeries = ((ArrayIndicesWindow)dataForIteration.getFirst()).getWindowedSeries((ISeries)((IDefaultIndexSeries)this.windowingHelper.getInputISeries()));
      } else if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.VALUE) {
         windowedSeries = ((ArrayIndicesWindow)dataForIteration.getFirst()).getWindowedSeries((ISeries)((IRealIndexSeries)this.windowingHelper.getInputISeries()));
      } else if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.TIME) {
         windowedSeries = ((ArrayIndicesWindow)dataForIteration.getFirst()).getWindowedSeries((ISeries)((ITimeIndexSeries)this.windowingHelper.getInputISeries()));
      }

      castOperator.windowedExampleSetInnerSource.deliver(this.windowingHelper.convertISeriesToExampleSet(windowedSeries));
   }

   //@Override
   protected void processSingleRun2(Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>> dataForIteration, List<IOObject> results, boolean reuseResults, AbstractLoopOperator<Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>>> operator) throws OperatorException {
      Iterator var5 = results.iterator();

      label109:
      while(true) {
         ExampleSet exampleSet;
         Attribute indexOfLastValueInWindowAttribute;
         List<Attribute> horizonAttributes;
         Object horizonValues;
         double indexOfLastValueInWindowValue;
         do {
            IOObject object;
            do {
               if (!var5.hasNext()) {
                  super.processSingleRun(dataForIteration, results, reuseResults, operator);
                  return;
               }

               object = (IOObject)var5.next();
            } while(!(object instanceof ExampleSet));

            exampleSet = (ExampleSet)object;
            indexOfLastValueInWindowAttribute = null;
            horizonAttributes = null;
            horizonValues = null;
            indexOfLastValueInWindowValue = 0.0D;
            if (this.getParameterAsBoolean("add_last_index_in_window_attribute")) {
               if (this.getParameterAsBoolean("has_indices")) {
                  indexOfLastValueInWindowAttribute = this.windowingHelper.createAndAddLastIndexAttribute(exampleSet);
                  indexOfLastValueInWindowValue = this.windowingHelper.getLastIndexValue((ArrayIndicesWindow)dataForIteration.getFirst());
               } else {
                  Attributes attributes = exampleSet.getAttributes();
                  indexOfLastValueInWindowAttribute = AttributeFactory.createAttribute("Window id", 3);
                  AttributeRole role = new AttributeRole(indexOfLastValueInWindowAttribute);
                  if (attributes.findRoleBySpecialName("id") == null) {
                     role.setSpecial("id");
                  }

                  exampleSet.getExampleTable().addAttribute(indexOfLastValueInWindowAttribute);
                  attributes.add(role);
                  indexOfLastValueInWindowValue = ((ValueDouble)operator.getValue("iteration_number")).getDoubleValue() - 1.0D;
               }
            }

            if (dataForIteration.getSecond() != null) {
               ISeries windowedHorizonSeries = null;
               if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.DEFAULT) {
                  windowedHorizonSeries = ((ArrayIndicesWindow)dataForIteration.getSecond()).getWindowedSeries((ISeries)((IDefaultIndexSeries)this.windowingHelper.getHorizonISeries()));
               } else if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.VALUE) {
                  windowedHorizonSeries = ((ArrayIndicesWindow)dataForIteration.getSecond()).getWindowedSeries((ISeries)((IRealIndexSeries)this.windowingHelper.getHorizonISeries()));
               } else if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.TIME) {
                  windowedHorizonSeries = ((ArrayIndicesWindow)dataForIteration.getSecond()).getWindowedSeries((ISeries)((ITimeIndexSeries)this.windowingHelper.getHorizonISeries()));
               }

               if (windowedHorizonSeries.getSeriesValues(0).getValuesType() == SeriesBuilder.ValuesType.REAL) {
                  horizonValues = ((RealValues)windowedHorizonSeries.getSeriesValues(0)).getValues();
               } else if (windowedHorizonSeries.getSeriesValues(0).getValuesType() == SeriesBuilder.ValuesType.NOMINAL) {
                  Integer[] nominalIndices = ((NominalValues)windowedHorizonSeries.getSeriesValues(0)).getNominalIndices();
                  horizonValues = new ArrayList();
                  Integer[] var15 = nominalIndices;
                  int var16 = nominalIndices.length;

                  for(int var17 = 0; var17 < var16; ++var17) {
                     Integer value = var15[var17];
                     if (value == null) {
                        ((List)horizonValues).add(Double.NaN);
                     } else {
                        ((List)horizonValues).add((double)value);
                     }
                  }
               } else if (windowedHorizonSeries.getSeriesValues(0).getValuesType() == SeriesBuilder.ValuesType.TIME) {
                  List values = ((TimeValues)windowedHorizonSeries.getSeriesValues(0)).getValues();
                  horizonValues = new ArrayList();
                  Iterator var24 = values.iterator();

                  while(var24.hasNext()) {
                     Instant value = (Instant)var24.next();
                     if (value == null) {
                        ((List)horizonValues).add(Double.NaN);
                     } else {
                        ((List)horizonValues).add((double)value.toEpochMilli());
                     }
                  }
               }

               horizonAttributes = this.windowingHelper.createAndAddHorizonAttributes(exampleSet, this.windowingHelper.getHorizonNominalMappingMap());
            }
         } while(indexOfLastValueInWindowAttribute == null && horizonAttributes == null);

         Iterator var22 = exampleSet.iterator();

         while(true) {
            Example example;
            do {
               if (!var22.hasNext()) {
                  continue label109;
               }

               example = (Example)var22.next();
               if (indexOfLastValueInWindowAttribute != null) {
                  example.setValue(indexOfLastValueInWindowAttribute, indexOfLastValueInWindowValue);
               }
            } while(horizonAttributes == null);

            int i = 0;

            for(Iterator var27 = horizonAttributes.iterator(); var27.hasNext(); ++i) {
               Attribute horizonAttribute = (Attribute)var27.next();
               example.setValue(horizonAttribute, (Double)((List)horizonValues).get(i));
            }
         }
      }
   }


   @Override
   protected void processSingleRun(Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>> dataForIteration, List<IOObject> results, boolean reuseResults, AbstractLoopOperator<Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>>> operator) throws OperatorException {
      for (IOObject object : results) {
         if (object instanceof ExampleSet) {
            ExampleSet exampleSet = (ExampleSet)object;
            Attribute indexOfLastValueInWindowAttribute = null;
            List<Attribute> horizonAttributes = null;
            List<Double> horizonValues = null;
            double indexOfLastValueInWindowValue = 0.0D;
            if (getParameterAsBoolean("add_last_index_in_window_attribute")) {
               if (getParameterAsBoolean("has_indices")) {
                  indexOfLastValueInWindowAttribute = this.windowingHelper.createAndAddLastIndexAttribute(exampleSet);
                  indexOfLastValueInWindowValue = this.windowingHelper.getLastIndexValue((ArrayIndicesWindow)dataForIteration.getFirst());
               } else {
                  Attributes attributes = exampleSet.getAttributes();
                  indexOfLastValueInWindowAttribute = AttributeFactory.createAttribute("Window id", 3);

                  AttributeRole role = new AttributeRole(indexOfLastValueInWindowAttribute);
                  if (attributes.findRoleBySpecialName("id") == null) {
                     role.setSpecial("id");
                  }
                  exampleSet.getExampleTable().addAttribute(indexOfLastValueInWindowAttribute);
                  attributes.add(role);

                  indexOfLastValueInWindowValue = ((ValueDouble)operator.getValue("iteration_number")).getDoubleValue() - 1.0D;
               }
            }
            if (dataForIteration.getSecond() != null) {
               ISeries<?, ?> windowedHorizonSeries = null;
               if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.DEFAULT) {

                  windowedHorizonSeries = ((ArrayIndicesWindow)dataForIteration.getSecond()).getWindowedSeries((IDefaultIndexSeries)this.windowingHelper.getHorizonISeries());
               } else if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.VALUE) {

                  windowedHorizonSeries = ((ArrayIndicesWindow)dataForIteration.getSecond()).getWindowedSeries((IRealIndexSeries)this.windowingHelper.getHorizonISeries());
               } else if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.TIME) {

                  windowedHorizonSeries = ((ArrayIndicesWindow)dataForIteration.getSecond()).getWindowedSeries((ITimeIndexSeries)this.windowingHelper.getHorizonISeries());
               }
               if (windowedHorizonSeries.getSeriesValues(0).getValuesType() == SeriesBuilder.ValuesType.REAL) {
                  horizonValues = ((RealValues)windowedHorizonSeries.getSeriesValues(0)).getValues();
               } else if (windowedHorizonSeries.getSeriesValues(0).getValuesType() == SeriesBuilder.ValuesType.NOMINAL) {

                  Integer[] nominalIndices = ((NominalValues)windowedHorizonSeries.getSeriesValues(0)).getNominalIndices();
                  horizonValues = new ArrayList<Double>();
                  for (Integer value : nominalIndices) {
                     if (value == null) {
                        horizonValues.add(Double.valueOf(Double.NaN));
                     } else {
                        horizonValues.add(Double.valueOf(value.intValue()));
                     }
                  }
               } else if (windowedHorizonSeries.getSeriesValues(0).getValuesType() == SeriesBuilder.ValuesType.TIME) {
                  List<Instant> values = ((TimeValues)windowedHorizonSeries.getSeriesValues(0)).getValues();
                  horizonValues = new ArrayList<Double>();
                  for (Instant value : values) {
                     if (value == null) {
                        horizonValues.add(Double.valueOf(Double.NaN)); continue;
                     }
                     horizonValues.add(Double.valueOf(value.toEpochMilli()));
                  }
               }

               horizonAttributes = this.windowingHelper.createAndAddHorizonAttributes(exampleSet, this.windowingHelper
                       .getHorizonNominalMappingMap());
            }
            if (indexOfLastValueInWindowAttribute != null || horizonAttributes != null) {
               for (Example example : exampleSet) {
                  if (indexOfLastValueInWindowAttribute != null) {
                     example.setValue(indexOfLastValueInWindowAttribute, indexOfLastValueInWindowValue);
                  }
                  if (horizonAttributes != null) {
                     int i = 0;
                     for (Attribute horizonAttribute : horizonAttributes) {
                        example.setValue(horizonAttribute, ((Double)horizonValues.get(i)).doubleValue());
                        i++;
                     }
                  }
               }
            }
         }
      }
      super.processSingleRun(dataForIteration, results, reuseResults, operator);
   }
}
