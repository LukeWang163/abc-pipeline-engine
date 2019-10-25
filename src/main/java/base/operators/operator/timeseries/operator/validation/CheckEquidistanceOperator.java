package base.operators.operator.timeseries.operator.validation;

import base.operators.operator.timeseries.operator.ExampleSetTimeSeriesOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.TimeSeriesHelperBuilder;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.timeseries.timeseriesanalysis.tools.CheckEquidistantIndices;
import java.util.List;

public class CheckEquidistanceOperator extends ExampleSetTimeSeriesOperator {
   private OutputPort exampleSetOutputPort = (OutputPort)this.getOutputPorts().createPassThroughPort("original");

   public CheckEquidistanceOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
      this.getTransformer().addPassThroughRule(this.exampleSetTimeSeriesHelper.getExampleSetInputPort(), this.exampleSetOutputPort);
   }

   protected ExampleSetTimeSeriesHelper initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException {
      TimeSeriesHelperBuilder builder = new TimeSeriesHelperBuilder(this);
      return builder.asInputPortOperator("example set").setIndiceHandling(ExampleSetTimeSeriesHelper.IndiceHandling.MANDATORY_INDICES).build();
   }

   public void doWork() throws OperatorException {
      this.exampleSetTimeSeriesHelper.resetHelper();
      boolean equidistantCheck = false;
      if (this.exampleSetTimeSeriesHelper.checkForTimeIndices()) {
         if (CheckEquidistantIndices.isEquidistant(this.exampleSetTimeSeriesHelper.getInputTimeSeries())) {
            equidistantCheck = true;
         }
      } else if (CheckEquidistantIndices.isEquidistant(this.exampleSetTimeSeriesHelper.getInputValueSeries())) {
         equidistantCheck = true;
      }

      if (!equidistantCheck) {
         throw new UserError(this, "time_series_extension.timeseries.indices_non_equidistant", new Object[]{this.getParameterAsString("indices_attribute")});
      } else {
         this.exampleSetOutputPort.deliver(this.exampleSetTimeSeriesHelper.getInputExampleSet());
      }
   }

   public List getParameterTypes() {
      return this.exampleSetTimeSeriesHelper.getParameterTypes(super.getParameterTypes());
   }
}
