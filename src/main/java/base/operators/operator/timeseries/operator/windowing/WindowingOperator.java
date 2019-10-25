package base.operators.operator.timeseries.operator.windowing;

import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WindowingHelper;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.IncompatibleMDClassException;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPassThroughRule;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.parameter.UndefinedParameterError;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.NominalValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.RealValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.TimeValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IDefaultIndexSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.IRealIndexSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ITimeIndexSeries;
import base.operators.operator.timeseries.timeseriesanalysis.window.ArrayIndicesWindow;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WindowingOperator extends ExampleSetTimeSeriesOperator {
   private OutputPort windowedExampleSetOutputPort = (OutputPort)this.getOutputPorts().createPort("windowed example set");
   private OutputPort exampleSetOutputPort = (OutputPort)this.getOutputPorts().createPassThroughPort("original");
   private WindowingHelper windowingHelper;

   public WindowingOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.windowingHelper.addWindowParameterPreconditions();
      this.getTransformer().addRule(new ExampleSetPassThroughRule(this.exampleSetTimeSeriesHelper.getExampleSetInputPort(), this.exampleSetOutputPort, SetRelation.EQUAL));
      this.getTransformer().addRule(new MDTransformationRule() {
         public void transformMD() {
            ExampleSetMetaData md = new ExampleSetMetaData();

            try {
               if (WindowingOperator.this.exampleSetTimeSeriesHelper.getExampleSetInputPort().isConnected()) {
                  WindowingOperator.this.windowingHelper.updateWindowParameterSettings();
                  ExampleSetMetaData inputMD = (ExampleSetMetaData)WindowingOperator.this.exampleSetTimeSeriesHelper.getExampleSetInputPort().getMetaData(ExampleSetMetaData.class);
                  if (inputMD != null) {
                     List amdList = WindowingOperator.this.windowingHelper.createLastIndexInWindowAttributeMetaData(inputMD);
                     if (amdList.isEmpty()) {
                        md.addAttribute(new AttributeMetaData("Window id", 3, "id"));
                     } else {
                        AttributeMetaData amd = (AttributeMetaData)amdList.get(0);
                        amd.setRole("id");
                        md.addAttribute(amd);
                     }

                     if (inputMD.getNumberOfExamples() != null) {
                        MDInteger numberOfExamples = inputMD.getNumberOfExamples().copy();
                        int numberOfTrainingWindows = WindowingOperator.this.windowingHelper.getNumberOfTrainingWindows((Integer)numberOfExamples.getNumber());
                        numberOfExamples.subtract((Integer)numberOfExamples.getNumber() - numberOfTrainingWindows);
                        md.setNumberOfExamples(numberOfExamples);
                     }
                  }

                  md.addAllAttributes(WindowingOperator.this.windowingHelper.createTrainingWindowAttributesMetaData());
                  md.addAllAttributes(WindowingOperator.this.windowingHelper.createHorizonAttributesMetaData());
               }
            } catch (IncompatibleMDClassException | UndefinedParameterError var6) {
               var6.printStackTrace();
            }

            WindowingOperator.this.windowedExampleSetOutputPort.deliverMD(md);
         }
      });
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      builder = builder.asInputPortOperator("example set").enableMultivariateInput().setIndiceHandling(ExampleSetTimeSeriesHelper.IndiceHandling.OPTIONAL_INDICES).asWindowingHelper().setHorizonAttributeSelection(WindowingHelper.HorizonAttributeSelection.SINGLE).setValuesType(SeriesBuilder.ValuesType.MIXED).useISeries();
      this.windowingHelper = (WindowingHelper)builder.build();
      return this.windowingHelper;
   }

   public void doWork() throws OperatorException {
      this.windowingHelper.resetHelper();
      this.windowingHelper.updateWindowParameterSettings();
      this.windowingHelper.readInputData(this.exampleSetTimeSeriesHelper.getExampleSetInputPort());
      int numberOfTrainingWindows = this.windowingHelper.getNumberOfTrainingWindows(this.exampleSetTimeSeriesHelper.getInputExampleSet().size());
      this.windowingHelper.checkWindowParameterSettings();
      int progressStepSize = 2000000 / this.windowingHelper.getWindowSize();
      if (this.exampleSetTimeSeriesHelper.getNumberOfAttributes() != 0) {
         progressStepSize /= this.exampleSetTimeSeriesHelper.getNumberOfAttributes();
      }

      int progressCallsInCreateWindows = this.windowingHelper.progressCallsInCreateWindows() + 1;
      int progressCallsInLoopTrainingWindows = numberOfTrainingWindows / progressStepSize;
      int total = progressCallsInCreateWindows + progressCallsInLoopTrainingWindows + 1;
      this.getProgress().setCheckForStop(true);
      this.getProgress().setTotal(total);
      this.windowingHelper.enableCallProgressStep();
      List windows = this.windowingHelper.createWindowsFromInput();
      this.exampleSetOutputPort.deliver(this.exampleSetTimeSeriesHelper.getInputExampleSet().copy());
      this.getProgress().step();
      List listOfNewAtts = new LinkedList();
      AttributeRole indexAttributeRole = this.windowingHelper.createLastIndexInWindowAttribute();
      Attribute indexAttribute = AttributeFactory.createAttribute("Window id", 3);
      if (indexAttributeRole != null) {
         indexAttribute = indexAttributeRole.getAttribute();
      }

      listOfNewAtts.add(indexAttribute);
      listOfNewAtts.addAll(this.windowingHelper.createTrainingWindowAttributes(this.windowingHelper.getInputNominalMappingMap()));
      List horizonRoles = this.windowingHelper.createHorizonAttributes(this.windowingHelper.getHorizonNominalMappingMap());
      Iterator var11 = horizonRoles.iterator();

      while(var11.hasNext()) {
         AttributeRole role = (AttributeRole)var11.next();
         listOfNewAtts.add(role.getAttribute());
      }

      ExampleSetBuilder builder = ExampleSets.from(listOfNewAtts);
      builder.withRole(indexAttribute, "id");
      Iterator var24 = horizonRoles.iterator();

      while(var24.hasNext()) {
         AttributeRole role = (AttributeRole)var24.next();
         builder.withRole(role.getAttribute(), role.getSpecialName());
      }

      for(int i = 0; i < numberOfTrainingWindows; ++i) {
         double[] row = new double[listOfNewAtts.size()];
         int rowCounter = 0;
         int trainingWindowIndex = i;
         if (this.windowingHelper.isCreateLabels()) {
            trainingWindowIndex = 2 * i;
         }

         ISeries windowedSeries = null;
         if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.DEFAULT) {
            windowedSeries = ((ArrayIndicesWindow)windows.get(trainingWindowIndex)).getWindowedSeries((ISeries)((IDefaultIndexSeries)this.windowingHelper.getInputISeries()));
            row[rowCounter] = (double)i;
            ++rowCounter;
         } else if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.VALUE) {
            windowedSeries = ((ArrayIndicesWindow)windows.get(trainingWindowIndex)).getWindowedSeries((ISeries)((IRealIndexSeries)this.windowingHelper.getInputISeries()));
            row[rowCounter] = (Double)((IRealIndexSeries)windowedSeries).getIndexValue(windowedSeries.getLength() - 1);
            ++rowCounter;
         } else if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.TIME) {
            windowedSeries = ((ArrayIndicesWindow)windows.get(trainingWindowIndex)).getWindowedSeries((ISeries)((ITimeIndexSeries)this.windowingHelper.getInputISeries()));
            row[rowCounter] = (double)((Instant)((ITimeIndexSeries)windowedSeries).getIndexValue(windowedSeries.getLength() - 1)).toEpochMilli();
            ++rowCounter;
         }

         Iterator var17 = windowedSeries.getSeriesValuesList().iterator();

         SeriesValues horizonSeriesValues;
         List realValues;
         Integer[] nominalValues;
         List timeValues;
         int index;
         while(var17.hasNext()) {
            horizonSeriesValues = (SeriesValues)var17.next();
            realValues = null;
            nominalValues = null;
            timeValues = null;
            if (horizonSeriesValues.getValuesType() == SeriesBuilder.ValuesType.REAL) {
               realValues = ((RealValues)horizonSeriesValues).getValues();
            } else if (horizonSeriesValues.getValuesType() == SeriesBuilder.ValuesType.NOMINAL) {
               nominalValues = ((NominalValues)horizonSeriesValues).getNominalIndices();
            } else {
               if (horizonSeriesValues.getValuesType() != SeriesBuilder.ValuesType.TIME) {
                  throw new IllegalArgumentException("not supported valuetype for series: " + horizonSeriesValues.getName());
               }

               timeValues = ((TimeValues)horizonSeriesValues).getValues();
            }

            for(index = 0; index < horizonSeriesValues.getLength(); ++index) {
               if (realValues != null) {
                  row[rowCounter] = realValues.get(index) != null ? (Double)realValues.get(index) : Double.NaN;
               } else if (nominalValues != null) {
                  row[rowCounter] = nominalValues[index] != null ? (double)nominalValues[index] : Double.NaN;
               } else if (timeValues != null) {
                  row[rowCounter] = timeValues.get(index) != null ? (double)((Instant)timeValues.get(index)).toEpochMilli() : Double.NaN;
               }

               ++rowCounter;
            }
         }

         if (this.windowingHelper.isCreateLabels()) {
            ISeries windowedHorizonSeries = null;
            if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.DEFAULT) {
               windowedHorizonSeries = ((ArrayIndicesWindow)windows.get(trainingWindowIndex + 1)).getWindowedSeries((ISeries)((IDefaultIndexSeries)this.windowingHelper.getHorizonISeries()));
            } else if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.VALUE) {
               windowedHorizonSeries = ((ArrayIndicesWindow)windows.get(trainingWindowIndex + 1)).getWindowedSeries((ISeries)((IRealIndexSeries)this.windowingHelper.getHorizonISeries()));
            } else if (this.windowingHelper.getIndiceType() == ExampleSetTimeSeriesHelper.IndiceType.TIME) {
               windowedHorizonSeries = ((ArrayIndicesWindow)windows.get(trainingWindowIndex + 1)).getWindowedSeries((ISeries)((ITimeIndexSeries)this.windowingHelper.getHorizonISeries()));
            }

            horizonSeriesValues = windowedHorizonSeries.getSeriesValues(0);
            realValues = null;
            nominalValues = null;
            timeValues = null;
            if (horizonSeriesValues.getValuesType() == SeriesBuilder.ValuesType.REAL) {
               realValues = ((RealValues)horizonSeriesValues).getValues();
            } else if (horizonSeriesValues.getValuesType() == SeriesBuilder.ValuesType.NOMINAL) {
               nominalValues = ((NominalValues)horizonSeriesValues).getNominalIndices();
            } else {
               if (horizonSeriesValues.getValuesType() != SeriesBuilder.ValuesType.TIME) {
                  throw new IllegalArgumentException("not supported valuetype for series: " + horizonSeriesValues.getName());
               }

               timeValues = ((TimeValues)horizonSeriesValues).getValues();
            }

            for(index = 0; index < horizonSeriesValues.getLength(); ++index) {
               if (realValues != null) {
                  row[rowCounter] = realValues.get(index) != null ? (Double)realValues.get(index) : Double.NaN;
               } else if (nominalValues != null) {
                  row[rowCounter] = nominalValues[index] != null ? (double)nominalValues[index] : Double.NaN;
               } else if (timeValues != null) {
                  row[rowCounter] = timeValues.get(index) != null ? (double)((Instant)timeValues.get(index)).toEpochMilli() : Double.NaN;
               }

               ++rowCounter;
            }
         }

         builder.addRow(row);
         if ((i + 1) % progressStepSize == 0) {
            this.getProgress().step();
         }
      }

      this.windowedExampleSetOutputPort.deliver(builder.build());
      this.getProgress().complete();
   }

   public List getParameterTypes() {
      return this.windowingHelper.getParameterTypes(new ArrayList());
   }
}
