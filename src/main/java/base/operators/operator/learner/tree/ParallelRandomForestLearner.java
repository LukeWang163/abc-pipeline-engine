package base.operators.operator.learner.tree;


import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.*;
import base.operators.operator.learner.PredictionModel;
import base.operators.parameter.*;
import base.operators.parameter.conditions.AboveOperatorVersionCondition;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.NonEqualStringCondition;
import base.operators.studio.concurrency.internal.BackgroudOperatorConcurrencyContext;
import base.operators.studio.concurrency.internal.StudioConcurrencyContext;
import base.operators.studio.internal.Resources;
import base.operators.tools.RandomGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ParallelRandomForestLearner extends ParallelDecisionTreeLearner {
    public static final String PARAMETER_USE_HEURISTIC_SUBSET_RATION = "guess_subset_ratio";
    public static final String PARAMETER_SUBSET_RATIO = "subset_ratio";
    public static final String PARAMETER_NUMBER_OF_TREES = "number_of_trees";
    public static final String PARAMETER_VOTING_STRATEGY = "voting_strategy";
    public static final String[] VOTING_STRATEGIES = { ConfigurableRandomForestModel.VotingStrategy.CONFIDENCE_VOTE.toString(), ConfigurableRandomForestModel.VotingStrategy.MAJORITY_VOTE
            .toString() };

    public static final OperatorVersion ONLY_MAJORITY_VOTING = new OperatorVersion(6, 5, 0);

    private static final String PARAMETER_ENABLE_PARALLEL_EXECUTION = "enable_parallel_execution";

    private static final String PARAMETER_RANDOM_SPLITS = "random_splits";

    public ParallelRandomForestLearner(OperatorDescription description) { super(description); }

    @Override
    public Class<? extends PredictionModel> getModelClass() { return ConfigurableRandomForestModel.class; }

    @Override
    public Model learn(ExampleSet exampleSet) throws OperatorException {
        checkLabelCriterionDependency(exampleSet);

        Attribute labelAtt = exampleSet.getAttributes().getLabel();
        exampleSet.recalculateAttributeStatistics(labelAtt);
        if (exampleSet.getStatistics(labelAtt, "unknown") > 0.0D) {
            throw new UserError(this, '?', new Object[] { labelAtt.getName() });
        }

        ExampleSet weightlessSet = exampleSet;
        if (exampleSet.getAttributes().getWeight() != null) {
            weightlessSet = (ExampleSet)exampleSet.clone();
            weightlessSet.getAttributes().remove(weightlessSet.getAttributes().getWeight());
        }

        int numberOfTrees = getParameterAsInt("number_of_trees");
        List<TreePredictionModel> baseModels = new ArrayList<TreePredictionModel>(numberOfTrees);

        boolean executeInParallel = getParameterAsBoolean("enable_parallel_execution");

        ColumnExampleTable parentTable = new ColumnExampleTable(weightlessSet, this, executeInParallel);

        RandomGenerator random = RandomGenerator.getRandomGenerator(this);

        List<Callable<TreePredictionModel>> tasks = new ArrayList<Callable<TreePredictionModel>>(numberOfTrees);
        for (int i = 0; i < numberOfTrees; i++) {
            tasks.add(new TreeCallable(weightlessSet, parentTable, random.nextInt()));
        }

        if (Runtime.getRuntime().availableProcessors() -1 >1 && tasks.size() > 1 && executeInParallel) {

            List<TreePredictionModel> results = null;
            try {
                BackgroudOperatorConcurrencyContext context = new BackgroudOperatorConcurrencyContext(this);
                Resources.ContextUserData data = new Resources.ContextUserData(context);
                this.setUserData(Resources.CONTEXT_KEY, data);
                results = context.call(tasks);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof OperatorException) {
                    throw (OperatorException) cause;
                }
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                if (cause instanceof Error) {
                    throw (Error)cause;
                }
                throw new OperatorException(cause.getMessage(), cause);
            }

            baseModels.addAll(results);
        } else {

            for (Callable<TreePredictionModel> task : tasks) {
                try {
                    baseModels.add(task.call());
                } catch (OperatorException|RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new OperatorException(e.getMessage(), e);
                }
            }
        }

        ConfigurableRandomForestModel.VotingStrategy strategy = ConfigurableRandomForestModel.VotingStrategy.MAJORITY_VOTE;
        if (getCompatibilityLevel().isAbove(ONLY_MAJORITY_VOTING) &&
                !CRITERIA_NAMES[4].equals(getParameterAsString("criterion"))) {
            String strategyParameter = getParameterAsString("voting_strategy");
            if (ConfigurableRandomForestModel.VotingStrategy.CONFIDENCE_VOTE.toString().equals(strategyParameter)) {
                strategy = ConfigurableRandomForestModel.VotingStrategy.CONFIDENCE_VOTE;
            }
        }

        checkCalculateWeights(baseModels);

        return new ConfigurableRandomForestModel(weightlessSet, baseModels, strategy);
    }

    private class TreeCallable extends Object implements Callable<TreePredictionModel> {
        private final Random callableRandom;
        private final ExampleSet exampleSet;
        private final ColumnExampleTable parentTable;

        private TreeCallable(ExampleSet exampleSet, ColumnExampleTable parentTable, int seed) {
            this.callableRandom = new Random(seed);
            this.parentTable = parentTable;
            this.exampleSet = exampleSet;
        }

        @Override
        public TreePredictionModel call() throws OperatorException {
            AbstractParallelTreeBuilder treeBuilder = ParallelRandomForestLearner.this.getTreeBuilder(this.parentTable, this.exampleSet, this.callableRandom);
            Tree tree = treeBuilder.learnTree(this.exampleSet);
            return generateModel(this.exampleSet, tree);
        }

        private TreePredictionModel generateModel(ExampleSet exampleSet, Tree tree) {
            TreePredictionModel treeModel;
            if (tree instanceof RegressionTree) {
                treeModel = new RegressionTreeModel(exampleSet, (RegressionTree)tree);
            } else {
                treeModel = new TreeModel(exampleSet, tree);
            }
            treeModel.setSource(ParallelRandomForestLearner.this.getName());
            return treeModel;
        }
    }

    protected AbstractParallelTreeBuilder getTreeBuilder(ColumnExampleTable table, ExampleSet exampleSet, Random random) throws OperatorException {
        return new NonParallelBootstrappingTreeBuilder(this, createCriterion(), getTerminationCriteria(exampleSet),
                getPruner(), getSplitPreprocessing(random.nextInt(2147483647)),
                getParameterAsBoolean("apply_prepruning"), getParameterAsInt("number_of_prepruning_alternatives"),
                getParameterAsInt("minimal_size_for_split"), getParameterAsInt("minimal_leaf_size"), random, table,
                getParameterAsBoolean("random_splits"));
    }

    @Override
    public AttributePreprocessing getSplitPreprocessing(int seed) {
        RandomAttributeSubsetPreprocessing randomAttributeSubsetPreprocessing = null;


        try {
            randomAttributeSubsetPreprocessing = new RandomAttributeSubsetPreprocessing(getParameterAsBoolean("guess_subset_ratio"), getParameterAsDouble("subset_ratio"), RandomGenerator.getRandomGenerator(true, seed));
        }
        catch (UndefinedParameterError undefinedParameterError) {}


        return randomAttributeSubsetPreprocessing;
    }

    @Override
    public boolean supportsCapability(OperatorCapability capability) {
        switch (capability) {
            case BINOMINAL_ATTRIBUTES:
            case POLYNOMINAL_ATTRIBUTES:
            case NUMERICAL_ATTRIBUTES:
            case POLYNOMINAL_LABEL:
            case BINOMINAL_LABEL:
            case NUMERICAL_LABEL:
            case MISSING_VALUES:
                return true;
        }

        return false;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new ArrayList<ParameterType>();

        ParameterTypeInt parameterTypeInt = new ParameterTypeInt("number_of_trees", "The number of learned random trees.", 1, 2147483647, 100);

        parameterTypeInt.setExpert(false);
        types.add(parameterTypeInt);

        types.addAll(super.getParameterTypes());

        for (ParameterType t : types) {
            if ("apply_prepruning".equals(t.getKey())) {
                t.setDefaultValue(Boolean.valueOf(false)); continue;
            }  if ("apply_pruning".equals(t.getKey())) {
                t.setDefaultValue(Boolean.valueOf(false));
            }
        }

        ParameterTypeBoolean parameterTypeBoolean = new ParameterTypeBoolean("random_splits", "Split numerical attributes randomly.", false, true);

        types.add(parameterTypeBoolean);

        parameterTypeBoolean = new ParameterTypeBoolean("guess_subset_ratio", "Indicates that log(m) + 1 features are used, otherwise a ratio has to be specified.", true);

        parameterTypeBoolean.setExpert(false);
        types.add(parameterTypeBoolean);

        ParameterTypeDouble parameterTypeDouble = new ParameterTypeDouble("subset_ratio", "Ratio of randomly chosen attributes to test", 0.0D, 1.0D, 0.2D);

        parameterTypeDouble.registerDependencyCondition(new BooleanParameterCondition(this, "guess_subset_ratio", false, false));

        parameterTypeDouble.setExpert(false);
        types.add(parameterTypeDouble);

        ParameterTypeCategory parameterTypeCategory = new ParameterTypeCategory("voting_strategy", "Voting strategy used to determine prediction.", VOTING_STRATEGIES, 0);

        parameterTypeCategory.registerDependencyCondition(new AboveOperatorVersionCondition(this, ONLY_MAJORITY_VOTING));
        parameterTypeCategory.registerDependencyCondition(new NonEqualStringCondition(this, "criterion", false, new String[] { CRITERIA_NAMES[4] }));

        parameterTypeCategory.setExpert(false);
        types.add(parameterTypeCategory);

        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

        types.add(new ParameterTypeBoolean("enable_parallel_execution", "This parameter enables the parallel execution of this operator. Please disable the parallel execution if you run into memory problems.", true, true));



        return types;
    }

    @Override
    public OperatorVersion[] getIncompatibleVersionChanges() {
        OperatorVersion[] incompatibleVersions = super.getIncompatibleVersionChanges();
        OperatorVersion[] extendedIncompatibleVersions = (OperatorVersion[]) Arrays.copyOf(incompatibleVersions, incompatibleVersions.length + 1);

        extendedIncompatibleVersions[incompatibleVersions.length] = ONLY_MAJORITY_VOTING;
        return extendedIncompatibleVersions;
    }
}
