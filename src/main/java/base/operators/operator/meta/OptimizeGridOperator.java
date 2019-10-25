package base.operators.operator.meta;

import base.operators.operator.IOObject;
import base.operators.operator.Model;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.CollectingOrIteratingPortPairExtender;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.PassThroughOrGenerateRule;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.operator.ports.metadata.SubprocessTransformRule;
import base.operators.operator.process_control.loops.AbstractLoopOperator;
import base.operators.operator.process_control.loops.LoopParametersOperator;

import java.util.List;

public class OptimizeGridOperator extends LoopParametersOperator {
    private final Object bestLock = new Object();
    private ParameterSet bestParameterSet;
    private Model bestModel;
    private List<IOObject> bestResults;
    private int bestIteration;
    private InputPort modelInnerSink;
    private OutputPort modelOutput;
    private OutputPort performanceOutput;
    private OutputPort parameterSetOutput;

    public OptimizeGridOperator(OperatorDescription description) {
        this(description, "Subprocess");
    }

    private OptimizeGridOperator(OperatorDescription description, String subprocessName) {
        super(description, subprocessName);
    }

    private void setBest(ParameterSet parameterSet, PerformanceVector performance, List<IOObject> results, Model model, int iteration) {
        this.bestParameterSet = new ParameterSet(parameterSet, performance);
        this.bestModel = model;
        this.bestResults = results;
        this.bestIteration = iteration;
    }

    private void deliverBest() throws OperatorException {
        if (this.bestParameterSet == null) {
            throw new UserError(this, "optimize.parameters.no_results", new Object[] { this, " performance" });
        }
        this.performanceOutput.deliver(this.bestParameterSet.getPerformance());
        this.parameterSetOutput.deliver(this.bestParameterSet);
        if (this.modelOutput.isConnected()) {
            if (this.bestModel == null) {
                throw new UserError(this, "optimize.parameters.no_results", new Object[]{this, " model"});
            }
            this.modelOutput.deliver(this.bestModel);
        }
        if (this.bestResults != null && !this.bestResults.isEmpty()) {
            List<PortPairExtender.PortPair> managedPairs = this.outputPortPairExtender.getManagedPairs();
            int i = 0;
            for (IOObject result : this.bestResults) {
                ((PortPairExtender.PortPair)managedPairs.get(i++)).getOutputPort().deliver(result);
            }
        }
    }

    @Override
    protected void createPerformancePorts() {
        super.createPerformancePorts();

        this.performanceOutput = (OutputPort)getOutputPorts().createPort("performance");
        this.performanceInnerSink.registerMetaDataChangeListener(this.performanceOutput::deliverMD);
        getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
        getTransformer().addRule(new PassThroughOrGenerateRule(this.performanceInnerSink, this.performanceOutput, new MetaData(PerformanceVector.class)));


        this.modelOutput = (OutputPort)getOutputPorts().createPort("model");

        this.modelInnerSink = (InputPort)getSubprocess(0).getInnerSinks().createPort("model");
        this.modelInnerSink.addPrecondition(new SimplePrecondition(this.modelInnerSink, new MetaData(Model.class), false)
        {
            @Override
            protected boolean isMandatory()
            {
                return (OptimizeGridOperator.this.modelOutput.isConnected() && !OptimizeGridOperator.this.modelInnerSink.isConnected());
            }
        });
        this.modelInnerSink.registerMetaDataChangeListener(this.modelOutput::deliverMD);
        getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
        getTransformer().addRule(new PassThroughOrGenerateRule(this.modelInnerSink, this.modelOutput, new MetaData(Model.class)));

        this.parameterSetOutput = (OutputPort)getOutputPorts().createPort("parameter set");
        getTransformer().addGenerationRule(this.parameterSetOutput, ParameterSet.class);
    }

    @Override
    protected boolean isPerformanceRequired() { return true; }

    @Override
    protected void createAndStartExtender() {
        super.createAndStartExtender();
        this.outputPortPairExtender.setOutputMode(CollectingOrIteratingPortPairExtender.PortOutputMode.ITERATING);
    }

    @Override
    protected void prepareRun(AbstractLoopOperator.LoopArguments<ParameterSet> arguments, boolean executeParallely) throws OperatorException {
        this.bestParameterSet = null;
        this.bestModel = null;
        this.bestResults = null;
        this.bestIteration = -1;
    }

    @Override
    protected void processSingleRun(ParameterSet parameterSet, List<IOObject> results, boolean reuseResults, AbstractLoopOperator<ParameterSet> operator) throws OperatorException {
        super.processSingleRun(parameterSet, results, reuseResults, operator);
        OptimizeGridOperator optimizeGrid = (OptimizeGridOperator)operator;
        PerformanceVector performance = (PerformanceVector)optimizeGrid.performanceInnerSink.getDataOrNull(PerformanceVector.class);

        if (performance != null) {
            int iteration = optimizeGrid.getIterationNumber();
            synchronized (this.bestLock) {
                PerformanceVector bestPerformance = (this.bestParameterSet != null) ? this.bestParameterSet.getPerformance() : null;
                int comparison = (bestPerformance != null) ? performance.compareTo(bestPerformance) : 1;
                if (comparison > 0 || (comparison == 0 && iteration < this.bestIteration)) {
                    setBest(parameterSet, performance, results, (Model)optimizeGrid.modelInnerSink.getDataOrNull(Model.class), iteration);
                }
            }
        }
    }

    @Override
    protected void finishRun(AbstractLoopOperator.LoopArguments<ParameterSet> arguments) throws OperatorException {
        super.finishRun(arguments);
        deliverBest();
    }
}
