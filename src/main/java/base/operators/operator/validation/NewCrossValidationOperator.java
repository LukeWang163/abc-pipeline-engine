package base.operators.operator.validation;


import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.set.SplittedExampleSet;
import base.operators.operator.*;
import base.operators.operator.concurrency.execution.BackgroundExecutionService;
import base.operators.operator.tools.ConcurrencyTools;
import base.operators.operator.concurrency.internal.ParallelOperatorChain;
import base.operators.operator.learner.CapabilityProvider;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.CapabilityPrecondition;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.operator.ports.metadata.SubprocessTransformRule;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.EqualTypeCondition;
import base.operators.studio.concurrency.internal.ConcurrencyExecutionService;
import base.operators.studio.concurrency.internal.ConcurrencyExecutionServiceProvider;
import base.operators.studio.concurrency.internal.util.ExampleSetAppender;
import base.operators.tools.RandomGenerator;
import base.operators.tools.container.Pair;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class NewCrossValidationOperator extends ParallelOperatorChain implements CapabilityProvider {
    private static final String PARAMETER_SPLIT_ON_BATCH_ATTRIBUTE = "split_on_batch_attribute";
    private static final String PARAMETER_NUMBER_OF_FOLDS = "number_of_folds";
    private static final String PARAMETER_LEAVE_ONE_OUT = "leave_one_out";
    private static final String PARAMETER_SAMPLING_TYPE = "sampling_type";
    private static final int PROGRESS_ADDITIONAL_TASKS = 4;
    private static final int PROGRESS_STEP_FACTOR_FOLD = 5;

    private static class CrossValidationResult {
        private Pair<List<PerformanceVector>, ExampleSet> partialResult;
        private IOObject modelResult;
        private boolean isPartialResult;

        public CrossValidationResult(Pair<List<PerformanceVector>, ExampleSet> partialResult) {
            this.partialResult = partialResult;
            this.isPartialResult = true;
        }

        public CrossValidationResult(IOObject model) {
            this.modelResult = model;
            this.isPartialResult = false;
        }

        public IOObject getModelResult() { return this.modelResult; }

        public Pair<List<PerformanceVector>, ExampleSet> getPartialResult() { return this.partialResult; }

        public boolean isPartialResult() { return this.isPartialResult; }
    }

    private final InputPort modelInnerInput = getSubprocess(0).getInnerSinks().createPort("model", Model.class);
    private final OutputPort modelOutput = (OutputPort)getOutputPorts().createPort("model");

    private final PortPairExtender performanceOutputPortExtender = new PortPairExtender("performance",
            getSubprocess(0).getInnerSinks(), getOutputPorts(), new MetaData(PerformanceVector.class));

    private final InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
    private final OutputPort trainingSetInnerOutput = (OutputPort)getSubprocess(0).getInnerSources().createPort("training set");
    private final OutputPort testSetInnerOutput = (OutputPort)getSubprocess(0).getInnerSources().createPort("test set");
    private final InputPort testResultSetInnerInput = (InputPort)getSubprocess(0).getInnerSinks().createPort("test set results");
    private final OutputPort exampleSetOutput = (OutputPort)getOutputPorts().createPort("example set");
    private final OutputPort testResultSetOutput = (OutputPort)getOutputPorts().createPort("test result set");

    private double[] loggingValuesPerformance = new double[4];
    private double[] loggingValuesStandardDeviation = new double[4];

    public NewCrossValidationOperator(OperatorDescription description) {
        super(description, "CrossValidation");
        this.performanceOutputPortExtender.start();

        InputPort inputPort = ((PortPairExtender.PortPair)this.performanceOutputPortExtender.getManagedPairs().get(0)).getInputPort();
        inputPort.addPrecondition(new SimplePrecondition(inputPort, new MetaData(PerformanceVector.class)));

        getTransformer().addPassThroughRule(this.exampleSetInput, this.trainingSetInnerOutput);

        getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));

        getTransformer().addPassThroughRule(this.exampleSetInput, this.testSetInnerOutput);

        this.exampleSetInput.addPrecondition(new CapabilityPrecondition(this, this.exampleSetInput) {
            @Override
            protected List<QuickFix> getFixesForRegressionWhenClassificationSupported(AttributeMetaData labelMD) {
                List<QuickFix> fixes = super.getFixesForRegressionWhenClassificationSupported(labelMD);
                fixes.add(0, new ParameterSettingQuickFix(NewCrossValidationOperator.this, "sampling_type", "1", "switch_to_shuffled_sampling", new Object[0]));

                return fixes;
            }
        });

        getTransformer().addPassThroughRule(this.modelInnerInput, this.modelOutput);
        getTransformer().addRule(this.performanceOutputPortExtender.makePassThroughRule());
        getTransformer().addPassThroughRule(this.exampleSetInput, this.exampleSetOutput);
        getTransformer().addPassThroughRule(this.testResultSetInnerInput, this.testResultSetOutput);
        this.testResultSetInnerInput.addPrecondition(new SimplePrecondition(this.testResultSetInnerInput, new ExampleSetMetaData()) {
            @Override
            protected boolean isMandatory()
            {
                return false;
            }
        });

        addValue(new ValueDouble("performance main criterion", "The micro average of the main criterion of the performance vector delivered by the testing subprocess. Available only after the entire Operator is executed completely.") {
            @Override
            public double getDoubleValue() {
                return loggingValuesPerformance[0];
            }
        });
        addValue(new ValueDouble("std deviation main criterion", "The standard deviation over all folds of the main criterion of the performance vector delivered by the testing subprocess. Available only after the entire Operator is executed completely.") {
            @Override
            public double getDoubleValue() {
                return loggingValuesStandardDeviation[0];
            }
        });

        for (int i = 1; i < 4; i++) {
            final int index = i;
            addValue(new ValueDouble("performance " + i, "The micro average of the " + i + ". main criterion of the performance vector delivered by the testing subprocess. Available only after the entire Operator is executed completely.") {
                @Override
                public double getDoubleValue() {
                    return loggingValuesPerformance[index];
                }
            });
            addValue(new ValueDouble("std deviation " + i, "The standard deviation over all folds of the " + i + ". criterion of the performance vector delivered by the testing subprocess. Available only after the entire Operator is executed completely.") {
                @Override
                public double getDoubleValue() {
                    return loggingValuesStandardDeviation[index]; }
            });
        }
    }

    @Override
    public void doWork() throws OperatorException {
        SplittedExampleSet splittedSet;
        ExampleSet materializedSet;
        if (this.performanceOutputPortExtender.getManagedPairs().isEmpty() ||
                !((PortPairExtender.PortPair)this.performanceOutputPortExtender.getManagedPairs().get(0)).getInputPort().isConnected()) {
            throw new PortUserError(((PortPairExtender.PortPair)this.performanceOutputPortExtender.getManagedPairs().get(0)).getInputPort(), "cross_validation.missing_performance", new Object[0]);
        }

        ExampleSet set = (ExampleSet)this.exampleSetInput.getData(ExampleSet.class);
        boolean executeParallely = checkParallelizability();


        int numberOfFolds = getParameterAsInt("number_of_folds");
        if (getParameterAsBoolean("leave_one_out")) {
            numberOfFolds = set.size();
        }

        boolean deliverTestSet = (this.testResultSetOutput.isConnected() && this.testResultSetInnerInput.isConnected());
        int taskSize = this.modelOutput.isConnected() ? (numberOfFolds + 1) : numberOfFolds;

        taskSize *= 5;
        taskSize += (deliverTestSet ? 1 : 0);

        taskSize += 4;
        getProgress().setTotal(taskSize);
        getProgress().step();

        if (executeParallely) {
            materializedSet = (ExampleSet)getDataCopy(set, true);
        } else {
            materializedSet = set;
        }
        getProgress().step();

        boolean splitOnBatch = getParameterAsBoolean("split_on_batch_attribute");
        Attribute batchAttribute = materializedSet.getAttributes().getSpecial("batch");
        if (splitOnBatch &&
                batchAttribute == null) {
            throw new UserError(this, 113, new Object[] { "batch" });
        }

        boolean useLocalRandomSeed = getParameterAsBoolean("use_local_random_seed");
        int localRandomSeed = getParameterAsInt("local_random_seed");

        if (splitOnBatch) {
            splittedSet = SplittedExampleSet.splitByAttribute(materializedSet, batchAttribute);
            numberOfFolds = splittedSet.getNumberOfSubsets();
            int validationProgress = numberOfFolds * 5;

            getProgress().setTotal(validationProgress + 4 + (deliverTestSet ? 1 : 0));
        } else {
            int samplingType = getParameterAsInt("sampling_type");
            splittedSet = new SplittedExampleSet(materializedSet, numberOfFolds, samplingType, useLocalRandomSeed, localRandomSeed, false);
        }
        getProgress().step();

        List<Pair<List<PerformanceVector>, ExampleSet>> results = null;
        if (executeParallely) {
            results = performParallelValidation(numberOfFolds, splittedSet);
        } else {
            results = performSynchronizedValidation(numberOfFolds, splittedSet);
        }

        Pair<List<PerformanceVector>, ExampleSet> firstResult = (Pair)results.remove(numberOfFolds - 1);
        List<ExampleSet> resultSets = new LinkedList<ExampleSet>();
        if (firstResult.getSecond() != null) {
            resultSets.add(firstResult.getSecond());
        }
        List<PerformanceVector> vectors = (List)firstResult.getFirst();
        for (Pair<List<PerformanceVector>, ExampleSet> otherResult : results) {
            for (int i = 0; i < vectors.size(); i++) {
                PerformanceVector vector = (PerformanceVector)vectors.get(i);
                PerformanceVector otherVector = (PerformanceVector)((List)otherResult.getFirst()).get(i);
                vector.buildAverages(otherVector);
            }
            if (otherResult.getSecond() != null) {
                resultSets.add(otherResult.getSecond());
            }
        }


        int i = 0;
        for (PortPairExtender.PortPair pair : this.performanceOutputPortExtender.getManagedPairs()) {
            if (vectors.size() > i) { //pair.getOutputPort().isConnected() &&
                pair.getOutputPort().deliver((IOObject)vectors.get(i++));
            }
        }

        rememberLoggingValues((PerformanceVector)vectors.get(0));
        getProgress().step();

        if (deliverTestSet) {
            this.testResultSetOutput.deliver(ExampleSetAppender.merge(this, resultSets));
        }

        getProgress().complete();
        this.exampleSetOutput.deliver(materializedSet);
    }

    private List<Pair<List<PerformanceVector>, ExampleSet>> performSynchronizedValidation(int numberOfValidations, SplittedExampleSet splittedSet) throws UndefinedParameterError, OperatorException {
        List<Pair<List<PerformanceVector>, ExampleSet>> results = new ArrayList<Pair<List<PerformanceVector>, ExampleSet>>(numberOfValidations);

        boolean computeFullModel = true;//this.modelOutput.isConnected();
        for (int iteration = 0; iteration < numberOfValidations; iteration++) {

            results.add(validate(splittedSet, iteration));
            getProgress().step(5);
        }

        IOObject fullResults = train(splittedSet);
        this.modelOutput.deliver(fullResults);
        return results;
    }

    private List<Pair<List<PerformanceVector>, ExampleSet>> performParallelValidation(int numberOfValidations, final SplittedExampleSet splittedSet) throws UndefinedParameterError, OperatorException {
        ConcurrencyExecutionService concurrencyExecutionService = new BackgroundExecutionService();
        boolean computeFullModel = true; // this.modelOutput.isConnected();
        List<CrossValidationResult> resultSet = new ArrayList<CrossValidationResult>(numberOfValidations + 1);

        int batchSize = ConcurrencyExecutionService.getRecommendedConcurrencyBatchSize(this);
        List<Callable<CrossValidationResult>> taskSet = new ArrayList<Callable<CrossValidationResult>>(batchSize + 1);

        for (int iteration = 0; iteration < numberOfValidations; ) {

            batchSize = Math.min(numberOfValidations - iteration,
                    ConcurrencyExecutionService.getRecommendedConcurrencyBatchSize(this));

            for (int j = 1; j <= batchSize; iteration++, j++) {
                final int currentIteration = iteration;

                final NewCrossValidationOperator copy = (NewCrossValidationOperator)ConcurrencyTools.clone(this);

                Callable<CrossValidationResult> singleTask = concurrencyExecutionService.prepareOperatorTask(getProcess(), copy, iteration + 1, (iteration + 1 == numberOfValidations), new Callable<CrossValidationResult>() {
                    @Override
                    public NewCrossValidationOperator.CrossValidationResult call() throws Exception
                    {
                        SplittedExampleSet clonedSplittedSet = new SplittedExampleSet(splittedSet);
                        Pair<List<PerformanceVector>, ExampleSet> result = copy.validateForParallel(clonedSplittedSet, currentIteration);
                        NewCrossValidationOperator.this.getProgress().step(5);
                        return new NewCrossValidationOperator.CrossValidationResult(result);
                    }
                });
                taskSet.add(singleTask);
            }
            resultSet.addAll(concurrencyExecutionService.executeOperatorTasks(this, taskSet));
            taskSet.clear();
        }

        List<Pair<List<PerformanceVector>, ExampleSet>> result = new ArrayList<Pair<List<PerformanceVector>, ExampleSet>>(numberOfValidations);
        for (CrossValidationResult singleResult : resultSet) {
            if (singleResult.isPartialResult()) {
                result.add(singleResult.getPartialResult()); continue;
            }
        }

        if (computeFullModel) {
            SplittedExampleSet trainSet = new SplittedExampleSet(splittedSet);
            IOObject fullResults = trainForParallel(trainSet);
            this.modelOutput.deliver(fullResults);
        }
        return result;
    }


    private void rememberLoggingValues(PerformanceVector vector) {
        if (vector.getMainCriterion() != null) {
            this.loggingValuesPerformance[0] = vector.getMainCriterion().getMikroAverage();
            this.loggingValuesStandardDeviation[0] = vector.getMainCriterion().getMakroStandardDeviation();
        }
        for (int i = 0; i < 3; i++) {
            if (vector.getSize() > i) {
                this.loggingValuesPerformance[i + 1] = vector.getCriterion(i).getMikroAverage();
                this.loggingValuesStandardDeviation[i + 1] = vector.getCriterion(i).getMakroStandardDeviation();
            }
        }
    }

    private Pair<List<PerformanceVector>, ExampleSet> validate(SplittedExampleSet splittedSet, int iteration) throws OperatorException {
        splittedSet.selectAllSubsetsBut(iteration);
        this.trainingSetInnerOutput.deliver(splittedSet);
        splittedSet.selectSingleSubset(iteration);
        this.testSetInnerOutput.deliver(splittedSet);
        getSubprocess(0).execute();
        List<PerformanceVector> perfVectors = new ArrayList<PerformanceVector>(this.performanceOutputPortExtender.getManagedPairs().size());
        for (PortPairExtender.PortPair pair : this.performanceOutputPortExtender.getManagedPairs()) {
            if (pair.getInputPort().isConnected()) {
                perfVectors.add(pair.getInputPort().getData(PerformanceVector.class));
            }
        }
        return new Pair(perfVectors, this.testResultSetInnerInput.getDataOrNull(ExampleSet.class));

    }

    private Pair<List<PerformanceVector>, ExampleSet> validateForParallel(SplittedExampleSet clonedSplittedSet, int iteration) throws OperatorException {
        SplittedExampleSet trainSet = clonedSplittedSet;
        trainSet.selectAllSubsetsBut(iteration);
        this.trainingSetInnerOutput.deliver(trainSet);
        trainSet = null;
        SplittedExampleSet testSet = clonedSplittedSet;
        testSet.selectSingleSubset(iteration);
        this.testSetInnerOutput.deliver(testSet);
        testSet = null;
        getSubprocess(0).execute();
        List<PerformanceVector> perfVectors = new ArrayList<PerformanceVector>(this.performanceOutputPortExtender.getManagedPairs().size());
        for (PortPairExtender.PortPair pair : this.performanceOutputPortExtender.getManagedPairs()) {
            if (pair.getInputPort().isConnected()) {
                perfVectors.add(pair.getInputPort().getData(PerformanceVector.class));
            }
        }
        return new Pair(perfVectors, this.testResultSetInnerInput.getDataOrNull(ExampleSet.class));

    }

    private IOObject train(ExampleSet trainSet) throws OperatorException {
        this.trainingSetInnerOutput.deliver(trainSet);
        getSubprocess(0).execute();
        return this.modelInnerInput.getData(IOObject.class);
    }

    private IOObject trainForParallel(SplittedExampleSet splittedExampleSet) throws OperatorException {
        SplittedExampleSet trainSet = splittedExampleSet;
        trainSet.selectAllSubsets();
        this.trainingSetInnerOutput.deliver(trainSet);
        trainSet = null;
        SplittedExampleSet testSet = splittedExampleSet;
        testSet.selectSingleSubset(1);
        this.testSetInnerOutput.deliver(testSet);
        getSubprocess(0).execute();
        return this.modelInnerInput.getData(IOObject.class);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();

        types.add(new ParameterTypeBoolean("split_on_batch_attribute", "Use the special attribute 'batch' to partition the data instead of randomly splitting the data. This gives you control over the exact examples which are used to train the model each fold.", false, true));



        ParameterTypeBoolean parameterTypeBoolean = new ParameterTypeBoolean("leave_one_out", "Set the number of validations to the number of examples. If set to true, number_of_folds is ignored", false, false);


        parameterTypeBoolean.registerDependencyCondition(new BooleanParameterCondition(this, "split_on_batch_attribute", false, false));

        types.add(parameterTypeBoolean);

        ParameterTypeInt parameterTypeInt = new ParameterTypeInt("number_of_folds", "Number of folds (aka number of subsets) for the cross validation.", 2, 2147483647, 10);

        parameterTypeInt.registerDependencyCondition(new BooleanParameterCondition(this, "leave_one_out", false, false));
        parameterTypeInt.registerDependencyCondition(new BooleanParameterCondition(this, "split_on_batch_attribute", false, false));

        parameterTypeInt.setExpert(false);
        types.add(parameterTypeInt);

        ParameterTypeCategory parameterTypeCategory = new ParameterTypeCategory("sampling_type", "Defines the sampling type of the cross validation (linear = consecutive subsets, shuffled = random subsets, stratified = random subsets with class distribution kept constant)", SplittedExampleSet.SAMPLING_NAMES, 3);


        parameterTypeCategory.setExpert(false);
        parameterTypeCategory.registerDependencyCondition(new BooleanParameterCondition(this, "leave_one_out", false, false));
        parameterTypeCategory.registerDependencyCondition(new BooleanParameterCondition(this, "split_on_batch_attribute", false, false));

        types.add(parameterTypeCategory);

        for (ParameterType addType : RandomGenerator.getRandomGeneratorParameters(this)) {
            addType.registerDependencyCondition(new BooleanParameterCondition(this, "leave_one_out", false, false));
            addType.registerDependencyCondition(new BooleanParameterCondition(this, "split_on_batch_attribute", false, false));

            addType.registerDependencyCondition(new EqualTypeCondition(this, "sampling_type", SplittedExampleSet.SAMPLING_NAMES, false, new int[] { 1, 2, 3 }));


            types.add(addType);
        }
        List<ParameterType> superTypes = super.getParameterTypes();
        types.addAll(superTypes);

        return types;
    }


    @Override
    public boolean supportsCapability(OperatorCapability capability) {
        switch (capability) {
            case NO_LABEL:
                return false;
            case NUMERICAL_LABEL:
                try {
                    return (getParameterAsInt("sampling_type") != 2);
                } catch (UndefinedParameterError e) {
                    return false;
                }
        }

        return true;
    }
}
