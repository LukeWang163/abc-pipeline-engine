package base.operators.operator.timeseries.operator.transformation;

import base.operators.example.ExampleSet;
import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
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
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.FastFourierTransformation;
import java.util.Iterator;
import java.util.List;

public class FastFourierTransformationOperator extends ExampleSetTimeSeriesOperator {
   private OutputPort fftExampleSetOutputPort = (OutputPort)this.getOutputPorts().createPort("fft transformed example set");
   private OutputPort exampleSetOutputPort = (OutputPort)this.getOutputPorts().createPassThroughPort("original");
   private static final String PARAMETER_ADD_PHASESPECTRUM = "add_phase_spectrum";

   public FastFourierTransformationOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.getTransformer().addGenerationRule(this.fftExampleSetOutputPort, ExampleSet.class);
      this.getTransformer().addRule(new ExampleSetPassThroughRule(this.exampleSetTimeSeriesHelper.getExampleSetInputPort(), this.exampleSetOutputPort, SetRelation.EQUAL));
      this.getTransformer().addRule(new MDTransformationRule() {
         public void transformMD() {
            if (FastFourierTransformationOperator.this.exampleSetTimeSeriesHelper.getExampleSetInputPort().isConnected()) {
               ExampleSetMetaData metaData = new ExampleSetMetaData();

               try {
                  ExampleSetMetaData inputMd = (ExampleSetMetaData)FastFourierTransformationOperator.this.exampleSetTimeSeriesHelper.getExampleSetInputPort().getMetaData(ExampleSetMetaData.class);
                  if (inputMd != null) {
                     Iterator var3 = FastFourierTransformationOperator.this.exampleSetTimeSeriesHelper.getAttributeSubsetSelector().getMetaDataSubset(inputMd, FastFourierTransformationOperator.this.exampleSetTimeSeriesHelper.isIncludeSpecialAttributes()).getAllAttributes().iterator();

                     while(var3.hasNext()) {
                        AttributeMetaData amd = (AttributeMetaData)var3.next();
                        metaData.addAttribute(new AttributeMetaData(amd.getName() + "_Amplitude", 2));
                        if (FastFourierTransformationOperator.this.getParameterAsBoolean("add_phase_spectrum")) {
                           metaData.addAttribute(new AttributeMetaData(amd.getName() + "_Phase", 2));
                        }
                     }

                     MDInteger numberOfExamples = new MDInteger(inputMd.getNumberOfExamples());
                     int diff = (Integer)numberOfExamples.getNumber() - FastFourierTransformationOperator.this.getNumberOfExamplesInFFTOutput((Integer)numberOfExamples.getNumber());
                     numberOfExamples.subtract(diff);
                     metaData.setNumberOfExamples(numberOfExamples);
                  }
               } catch (IncompatibleMDClassException var5) {
                  var5.printStackTrace();
               }

               FastFourierTransformationOperator.this.fftExampleSetOutputPort.deliverMD(metaData);
            }

         }
      });
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      return builder.asInputPortOperator("example set").enableMultivariateInput().build();
   }

   public void doWork() throws OperatorException {
      this.exampleSetTimeSeriesHelper.resetHelper();
      this.exampleSetTimeSeriesHelper.readInputData(this.exampleSetTimeSeriesHelper.getExampleSetInputPort());
      boolean addPhase = this.getParameterAsBoolean("add_phase_spectrum");
      int progressCallsInGetMethod = this.exampleSetTimeSeriesHelper.progressCallsInGetAddConvertMethods() + 1;
      int numberOfExamplesInFFTOutput = this.getNumberOfExamplesInFFTOutput(this.exampleSetTimeSeriesHelper.getInputExampleSet().size());
      int numberOfCellsInFFTOutput = numberOfExamplesInFFTOutput * this.exampleSetTimeSeriesHelper.getNumberOfAttributes();
      if (addPhase) {
         numberOfCellsInFFTOutput *= 2;
      }

      int progressCallsInConvert = numberOfCellsInFFTOutput / 2000000;
      int totalNumber = 2 * progressCallsInGetMethod + progressCallsInConvert + 1;
      this.getProgress().setCheckForStop(true);
      this.getProgress().setTotal(totalNumber);
      this.exampleSetTimeSeriesHelper.enableCallProgressStep();
      this.exampleSetTimeSeriesHelper.checkForMissingValues();
      this.exampleSetTimeSeriesHelper.checkForInfiniteValues();
      MultivariateValueSeries inputMultivariateValueSeries = this.exampleSetTimeSeriesHelper.getInputMultivariateValueSeries();
      this.exampleSetOutputPort.deliver(this.exampleSetTimeSeriesHelper.getInputExampleSet());
      this.getProgress().step();
      FastFourierTransformation fft = new FastFourierTransformation();
      MultivariateValueSeries fftResult = fft.compute(inputMultivariateValueSeries);
      if (!addPhase) {
         String[] resutlNames = fftResult.getSeriesNames();
         String[] var11 = resutlNames;
         int var12 = resutlNames.length;

         for(int var13 = 0; var13 < var12; ++var13) {
            String name = var11[var13];
            if (name.endsWith("_Phase")) {
               fftResult.removeValueSeries(name);
            }
         }
      }

      this.getProgress().step(progressCallsInGetMethod);
      this.fftExampleSetOutputPort.deliver(this.exampleSetTimeSeriesHelper.convertMultivariateSeriesToExampleSet(fftResult));
      this.getProgress().complete();
   }

   private int getNumberOfExamplesInFFTOutput(int numberOfExamplesInInput) {
      int power = (int)(Math.log((double)numberOfExamplesInInput) / Math.log(2.0D));
      return (int)Math.pow(2.0D, (double)power) / 2;
   }

   public List getParameterTypes() {
      List types = this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
      types.add(new ParameterTypeBoolean("add_phase_spectrum", "Add the phase spectrum to the output.", false, false));
      return types;
   }
}
