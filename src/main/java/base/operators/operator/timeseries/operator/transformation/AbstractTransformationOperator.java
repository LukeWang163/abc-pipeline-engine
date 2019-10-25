package base.operators.operator.timeseries.operator.transformation;

import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.parameter.UndefinedParameterError;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.SeriesTransformation;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public abstract class AbstractTransformationOperator extends ExampleSetTimeSeriesOperator {
   public AbstractTransformationOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.exampleSetTimeSeriesHelper.addPassThroughRule();
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      return builder.asInputPortOperator("example set").asOutputPortOperator("example set", this.getDefaultPostfix()).enableMultivariateInput().addOverwriteOption().changeOutputAttributesToReal().setIndiceHandling(this.getIndicesHandling()).setValuesType(SeriesBuilder.ValuesType.MIXED).useISeries().build();
   }

   public void doWork() throws OperatorException {
      this.exampleSetTimeSeriesHelper.resetHelper();
      this.exampleSetTimeSeriesHelper.readInputData(this.exampleSetTimeSeriesHelper.getExampleSetInputPort());
      int progressCallsInGetMethod = this.exampleSetTimeSeriesHelper.progressCallsInGetAddConvertMethods() + 1;
      this.getProgress().setCheckForStop(true);
      this.getProgress().setTotal(3 * progressCallsInGetMethod);
      this.exampleSetTimeSeriesHelper.enableCallProgressStep();
      ISeries inputSeries = this.exampleSetTimeSeriesHelper.getInputISeriesFromPort();
      this.getProgress().step();
      SeriesTransformation transformation = this.createTransformation();
      ISeries outputSeries = null;

      try {
         outputSeries = transformation.compute(inputSeries);
      } catch (Exception var6) {
         throw this.exampleSetTimeSeriesHelper.handleException(var6, StringUtils.join((Object[])inputSeries.getSeriesNames(), ","));
      }

      this.getProgress().step(progressCallsInGetMethod);
      this.exampleSetTimeSeriesHelper.addISeriesToExampleSetOutputPort(outputSeries, this.exampleSetTimeSeriesHelper.getInputExampleSet());
      this.getProgress().complete();
   }

   public List getParameterTypes() {
      List types = this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
      types.addAll(this.getAdditionalParameterTypes());
      return types;
   }

   protected abstract SeriesTransformation createTransformation() throws UndefinedParameterError, UserError;

   protected abstract List getAdditionalParameterTypes();

   protected abstract String getDefaultPostfix();

   protected ExampleSetTimeSeriesHelper.IndiceHandling getIndicesHandling() {
      return ExampleSetTimeSeriesHelper.IndiceHandling.NO_INDICES;
   }
}
