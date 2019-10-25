package base.operators.operator.timeseries.operator;

import base.operators.operator.process_control.loops.AbstractLoopOperator;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorChain;
import base.operators.operator.OperatorDescription;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.window.ArrayIndicesWindow;
import org.apache.commons.math3.util.Pair;


public abstract class ExampleSetTimeSeriesLoopOperator
        extends AbstractLoopOperator<Pair<ArrayIndicesWindow<?>, ArrayIndicesWindow<?>>>
{
   protected ExampleSetTimeSeriesHelper<OperatorChain, ISeries<?, ?>> exampleSetTimeSeriesHelper = initExampleSetTimeSeriesOperator();


   protected ExampleSetTimeSeriesLoopOperator(OperatorDescription description, String... subprocessNames) throws WrongConfiguredHelperException { super(description, subprocessNames); }

   protected abstract ExampleSetTimeSeriesHelper<OperatorChain, ISeries<?, ?>> initExampleSetTimeSeriesOperator() throws WrongConfiguredHelperException;
}
