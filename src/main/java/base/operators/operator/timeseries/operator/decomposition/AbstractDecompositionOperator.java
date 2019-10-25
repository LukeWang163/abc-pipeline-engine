package base.operators.operator.timeseries.operator.decomposition;

import base.operators.example.ExampleSet;
import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.IncompatibleMDClassException;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPassThroughRule;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.MDNumber.Relation;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateTimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.MultivariateValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.methods.decomposition.MultivariateSeriesDecomposition;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractDecompositionOperator extends ExampleSetTimeSeriesOperator {
   private OutputPort decompositionExampleSetOutputPort = (OutputPort)this.getOutputPorts().createPort("decomposition");
   private OutputPort exampleSetOutputPort = (OutputPort)this.getOutputPorts().createPassThroughPort("original");
   public static final String PARAMETER_SEASONALITY = "seasonality";

   public AbstractDecompositionOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.getTransformer().addGenerationRule(this.decompositionExampleSetOutputPort, ExampleSet.class);
      this.getTransformer().addRule(new ExampleSetPassThroughRule(this.exampleSetTimeSeriesHelper.getExampleSetInputPort(), this.exampleSetOutputPort, SetRelation.EQUAL));
      this.getTransformer().addRule(new MDTransformationRule() {
         public void transformMD() {
            if (AbstractDecompositionOperator.this.exampleSetTimeSeriesHelper.getExampleSetInputPort().isConnected()) {
               ExampleSetMetaData metaData = new ExampleSetMetaData();

               try {
                  ExampleSetMetaData inputMd = (ExampleSetMetaData)AbstractDecompositionOperator.this.exampleSetTimeSeriesHelper.getExampleSetInputPort().getMetaData(ExampleSetMetaData.class);
                  if (inputMd == null) {
                     AbstractDecompositionOperator.this.decompositionExampleSetOutputPort.deliverMD(metaData);
                     return;
                  }

                  if (AbstractDecompositionOperator.this.getParameterAsBoolean("has_indices")) {
                     AttributeMetaData amd = inputMd.getAttributeByName(AbstractDecompositionOperator.this.getParameterAsString("indices_attribute"));
                     if (amd != null) {
                        metaData.addAttribute(amd.clone());
                     }
                  }

                  Iterator var6 = AbstractDecompositionOperator.this.exampleSetTimeSeriesHelper.getAttributeSubsetSelector().getMetaDataSubset(inputMd, AbstractDecompositionOperator.this.exampleSetTimeSeriesHelper.isIncludeSpecialAttributes()).getAllAttributes().iterator();

                  while(var6.hasNext()) {
                     AttributeMetaData amdx = (AttributeMetaData)var6.next();
                     if (amdx != null) {
                        metaData.addAttribute(amdx.clone());
                        metaData.addAttribute(new AttributeMetaData(amdx.getName() + "_Trend", 2));
                        metaData.addAttribute(new AttributeMetaData(amdx.getName() + "_Seasonal", 2));
                        metaData.addAttribute(new AttributeMetaData(amdx.getName() + "_Remainder", 2));
                     }
                  }

                  metaData.setNumberOfExamples(inputMd.getNumberOfExamples());
               } catch (UndefinedParameterError | IncompatibleMDClassException var5) {
                  var5.printStackTrace();
               }

               AbstractDecompositionOperator.this.decompositionExampleSetOutputPort.deliverMD(metaData);
            }
         }
      });
      this.exampleSetTimeSeriesHelper.getExampleSetInputPort().addPrecondition(new ExampleSetPrecondition(this.exampleSetTimeSeriesHelper.getExampleSetInputPort()) {
         public void makeAdditionalChecks(ExampleSetMetaData emd) throws UndefinedParameterError {
            int seasonality = AbstractDecompositionOperator.this.getParameterAsInt("seasonality");
            MDInteger numberOfExamples = emd.getNumberOfExamples();
            if (numberOfExamples.getRelation() == Relation.EQUAL || numberOfExamples.getRelation() == Relation.AT_MOST) {
               int lengthOfTimeSeries = (Integer)emd.getNumberOfExamples().getNumber();
               if (2 * seasonality >= lengthOfTimeSeries) {
                  this.createError(Severity.ERROR, "time_series_extension.parameters.parameter_too_large", new Object[]{"seasonality", seasonality, "larger than or equal to the half of the time series length", lengthOfTimeSeries / 2});
               }
            }

         }
      });
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      return builder.asInputPortOperator("example set").enableMultivariateInput().setIndiceHandling(ExampleSetTimeSeriesHelper.IndiceHandling.OPTIONAL_INDICES).build();
   }

   public void doWork() throws OperatorException {
      this.exampleSetTimeSeriesHelper.readInputData(this.exampleSetTimeSeriesHelper.getExampleSetInputPort());
      int progressCallsInGetMethod = this.exampleSetTimeSeriesHelper.progressCallsInGetAddConvertMethods() + 1;
      this.getProgress().setCheckForStop(true);
      this.getProgress().setTotal(3 * progressCallsInGetMethod);
      this.exampleSetTimeSeriesHelper.enableCallProgressStep();
      int seasonality = this.getParameterAsInt("seasonality");
      MultivariateSeriesDecomposition decomposition = this.createDecomposition(seasonality);
      boolean timeSeriesInput = this.exampleSetTimeSeriesHelper.checkForTimeIndices();
      MultivariateSeries transformatedMultivariateSeries = null;
      if (timeSeriesInput) {
         MultivariateTimeSeries inputMultivariateTimeSeries = this.exampleSetTimeSeriesHelper.getInputMultivariateTimeSeries();
         this.getProgress().step();
         if (2 * seasonality >= inputMultivariateTimeSeries.getLength()) {
            throw new UserError(this, "time_series_extension.parameter.timeseries_length_larger_than_parameter", new Object[]{"Parameter seasonality", "larger than or equal to half of", seasonality, inputMultivariateTimeSeries.getLength()});
         }

         this.exampleSetOutputPort.deliver(this.exampleSetTimeSeriesHelper.getInputExampleSet());
         transformatedMultivariateSeries = decomposition.compute(inputMultivariateTimeSeries);
      } else {
         MultivariateValueSeries inputMultivariateValueSeries = this.exampleSetTimeSeriesHelper.getInputMultivariateValueSeries();
         if (2 * seasonality >= inputMultivariateValueSeries.getLength()) {
            throw new UserError(this, "time_series_extension.parameter.timeseries_length_larger_than_parameter", new Object[]{"Parameter seasonality", "larger than or equal to half of", seasonality, inputMultivariateValueSeries.getLength()});
         }

         this.getProgress().step();
         this.exampleSetOutputPort.deliver(this.exampleSetTimeSeriesHelper.getInputExampleSet());
         transformatedMultivariateSeries = decomposition.compute(inputMultivariateValueSeries);
      }

      this.getProgress().step(progressCallsInGetMethod);
      this.decompositionExampleSetOutputPort.deliver(this.exampleSetTimeSeriesHelper.convertMultivariateSeriesToExampleSet((MultivariateSeries)transformatedMultivariateSeries));
      this.getProgress().complete();
   }

   public List getParameterTypes() {
      List types = this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
      types.addAll(this.parameterTypesBeforeSeasonality());
      types.add(new ParameterTypeInt("seasonality", "This parameter defines the seasonality for the decomposition.", 1, Integer.MAX_VALUE, 12, false));
      types.addAll(this.parameterTypesAfterSeasonality());
      return types;
   }

   protected abstract MultivariateSeriesDecomposition createDecomposition(int var1) throws UndefinedParameterError;

   protected abstract List parameterTypesBeforeSeasonality();

   protected abstract List parameterTypesAfterSeasonality();
}
