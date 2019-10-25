package base.operators.operator.process_control.loops;

import base.operators.MacroHandler;
import base.operators.Process;
import base.operators.operator.concurrency.execution.BackgroundExecutionService;
import base.operators.operator.tools.ConcurrencyTools;
import base.operators.operator.IOObject;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ValueDouble;
import base.operators.operator.concurrency.internal.ParallelOperatorChain;
import base.operators.operator.ports.CollectingOrIteratingPortPairExtender;
import base.operators.operator.ports.OrderPreservingPortPairExtender;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.SubprocessTransformRule;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.studio.concurrency.internal.ConcurrencyExecutionService;
import base.operators.tools.RandomGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class AbstractLoopOperator<D>
        extends ParallelOperatorChain
{
    protected static final String PARAMETER_REUSE_RESULTS = "reuse_results";
    protected OrderPreservingPortPairExtender inputPortPairExtender;
    protected CollectingOrIteratingPortPairExtender outputPortPairExtender;
    private int currentIterationNumber;

    public static class LoopArguments<D>
            extends Object
    {
        private int numberOfIterations;
        private Map<String, String> macros;
        private List<D> dataForIteration;

        public int getNumberOfIterations() { return this.numberOfIterations; }

        public void setNumberOfIterations(int numberOfIterations) { this.numberOfIterations = numberOfIterations; }

        public Map<String, String> getMacros() { return this.macros; }

        public void setMacros(Map<String, String> macros) { this.macros = macros; }

        public List<D> getDataForIteration() { return this.dataForIteration; }

        public void setDataForIteration(List<D> dataForIteration) { this.dataForIteration = dataForIteration; }
    }

    protected AbstractLoopOperator(OperatorDescription description, String... subprocessNames) {
        super(description, subprocessNames);
        this.createPorts();
        this.addValue(new ValueDouble("iteration_number", "The number of the current iteration, independent of parallelization.")
        {
            @Override
            public double getDoubleValue()
            {
                return (AbstractLoopOperator.this.currentIterationNumber + 1);
            }
        });
    }

    private void updateOutputExtender() {
        this.outputPortPairExtender.setOutputMode(
                getParameterAsBoolean("reuse_results") ? CollectingOrIteratingPortPairExtender.PortOutputMode.ITERATING : CollectingOrIteratingPortPairExtender.PortOutputMode.COLLECTING);
    }

    private void setMacros(LoopArguments<D> arguments) throws OperatorException { setMacros(arguments, getProcess().getMacroHandler(), this.currentIterationNumber); }

    protected int getIterationNumber() { return this.currentIterationNumber; }

    protected void createPorts() {
        createAndStartExtender();
        init();
        if (canReuseResults()) {
            getParameters().addObserver((observable, arg) -> updateOutputExtender(), false);
        }
    }

    protected void createAndStartExtender() {
        this.inputPortPairExtender = new OrderPreservingPortPairExtender("input", getInputPorts(), getSubprocess(0).getInnerSources());
        this.outputPortPairExtender = new CollectingOrIteratingPortPairExtender("output", getSubprocess(0).getInnerSinks(), getOutputPorts());
        this.inputPortPairExtender.start();
        this.outputPortPairExtender.start();
        getTransformer().addRule(this.inputPortPairExtender.makePassThroughRule());
        getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
        getTransformer().addRule(this.outputPortPairExtender.makePassThroughRule());
    }

    protected void init() {}

    protected void prepareRun(LoopArguments<D> arguments, boolean executeParallely) throws OperatorException {}

    protected void finishRun(LoopArguments<D> arguments) throws OperatorException {}

    protected void performParallelLoop(LoopArguments<D> arguments) throws OperatorException {
        List<IOObject> inputData = getDataCopy(this.inputPortPairExtender.getDataOrNull(IOObject.class), true);
        int numberOfIterations = arguments.getNumberOfIterations();

        List<List<IOObject>> resultSet = new ArrayList<List<IOObject>>(numberOfIterations);

        for (int i = 0; i < numberOfIterations; ) {
            int batchSize = Math.min(numberOfIterations - i,
                    ConcurrencyExecutionService.getRecommendedConcurrencyBatchSize(this));
            List<Callable<List<IOObject>>> taskSet = new ArrayList<Callable<List<IOObject>>>(batchSize);

            BackgroundExecutionService backgroundExecutionService = new BackgroundExecutionService();
            for (int j = 1; j <= batchSize; i++, j++) {

                AbstractLoopOperator<D> copy = (AbstractLoopOperator) ConcurrencyTools.clone(this);
                copy.currentIterationNumber = i;
                D dataForIteration = (D)((arguments.getDataForIteration() == null) ? null : arguments.getDataForIteration().get(i));

//                Callable<List<IOObject>> singleTask = ConcurrencyExecutionServiceProvider.INSTANCE.getService().prepareOperatorTask(getProcess(), copy, i + 1, (i + 1 == numberOfIterations), () -> {
                Callable<List<IOObject>> singleTask = backgroundExecutionService.prepareOperatorTask(getProcess(), copy, i + 1, (i + 1 == numberOfIterations), () -> {
                    copy.checkForStop();
                    copy.setMacros(arguments);
                    copy.inputPortPairExtender.deliver(getDataCopy(inputData, false));
                    prepareSingleRun(dataForIteration, copy);
                    List<IOObject> result = copy.doIteration();
                    processSingleRun(dataForIteration, result, false, copy);
                    getProgress().step();
                    return result;
                });

                taskSet.add(singleTask);
            }
            resultSet.addAll(backgroundExecutionService.executeOperatorTasks(this, taskSet));
        }

        List<PortPairExtender.PortPair> managedPairs = this.outputPortPairExtender.getManagedPairs();
        for (List<IOObject> singleResult : resultSet) {
            int i = 0;
            for (IOObject result : singleResult) {
                ((PortPairExtender.PortPair)managedPairs.get(i++)).getInputPort().receive(result);
            }
            this.outputPortPairExtender.collect();
        }
    }

    protected void prepareSingleRun(D dataForIteration, AbstractLoopOperator<D> operator) throws OperatorException {}

    protected void processSingleRun(D dataForIteration, List<IOObject> results, boolean reuseResults, AbstractLoopOperator<D> operator) throws OperatorException {
        if (reuseResults) {
            this.inputPortPairExtender.deliver(results);
        } else if (operator == this) {

            this.inputPortPairExtender.passDataThrough();
            this.outputPortPairExtender.collect();
        }
    }

    protected void performSynchronizedLoop(LoopArguments<D> arguments) throws OperatorException {
        this.inputPortPairExtender.passDataThrough();
        int numberOfIterations = arguments.getNumberOfIterations();
        boolean reuseResults = getParameterAsBoolean("reuse_results");

        Process process = getProcess();
        RandomGenerator base = RandomGenerator.stash(process);
        MacroHandler macroHandler = process.getMacroHandler();
        List<IOObject> results = null;
        for (int i = 0; i < numberOfIterations; i++) {

            RandomGenerator.init(process, Long.valueOf(base.nextLongInRange(1L, 2147483648L)));
            this.currentIterationNumber = i;
            setMacros(arguments, macroHandler, i);
            D dataForIteration = (D)((arguments.getDataForIteration() == null) ? null : arguments.getDataForIteration().get(i));
            prepareSingleRun(dataForIteration, this);
            results = doIteration();
            processSingleRun(dataForIteration, results, reuseResults, this);
            getProgress().step();
        }

        if (reuseResults && results != null) {
            List<PortPairExtender.PortPair> managedPairs = this.outputPortPairExtender.getManagedPairs();
            int i = 0;
            for (IOObject singleResult : results) {
                ((PortPairExtender.PortPair)managedPairs.get(i++)).getOutputPort().deliver(singleResult);
            }
        }
        RandomGenerator.restore(process);
    }

    protected List<IOObject> doIteration() throws OperatorException {
        inApplyLoop();

        getSubprocess(0).execute();

        return collectResults();
    }

    protected List<IOObject> collectResults() throws OperatorException { return this.outputPortPairExtender.getData(IOObject.class); }

    protected boolean canReuseResults() { return true; }

    @Override
    protected boolean checkParallelizability() { return (super.checkParallelizability() && !getParameterAsBoolean("reuse_results")); }

    @Override
    public void doWork() throws OperatorException {
        this.outputPortPairExtender.reset();

        boolean executeParallely = checkParallelizability();
        LoopArguments<D> arguments = this.prepareArguments(executeParallely);

        if (arguments.getNumberOfIterations() < 1) {
            throw new UserError(this, "abstract_loop.not_enough_iterations", new Object[] { Integer.valueOf(arguments.getNumberOfIterations()) });
        }

        getProgress().setCheckForStop(false);
        getProgress().setTotal(arguments.getNumberOfIterations());

        prepareRun(arguments, executeParallely);

        if (executeParallely && arguments.getNumberOfIterations() > 1) {
            performParallelLoop(arguments);
        } else {
            performSynchronizedLoop(arguments);
        }

        finishRun(arguments);
        getProgress().complete();
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new ArrayList<ParameterType>();
        if (canReuseResults()) {
            types.add(new ParameterTypeBoolean("reuse_results", "Set whether to reuse the results of each iteration as the input of the next iteration. If set to true, the output of each iteration is used as input for the next iteration. Enabling this parameter will force the operator to NOT run in a parallel fashion. If set to false, the input of each iteration will be the original input.", false, false));
        }

        List<ParameterType> superTypes = super.getParameterTypes();
        types.addAll(superTypes);
        ParameterType enableParallelExecutionType = (ParameterType)superTypes.get(superTypes.size() - 1);
        if (canReuseResults()) {
            enableParallelExecutionType
                    .registerDependencyCondition(new BooleanParameterCondition(this, "reuse_results", false, false));
        }
        return types;
    }

    protected abstract LoopArguments<D> prepareArguments(boolean paramBoolean) throws OperatorException;

    protected abstract void setMacros(LoopArguments<D> paramLoopArguments, MacroHandler paramMacroHandler, int paramInt) throws OperatorException;
}
