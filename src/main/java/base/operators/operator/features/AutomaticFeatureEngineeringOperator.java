package base.operators.operator.features;

import base.operators.example.AttributeRole;
import base.operators.example.ExampleSet;
import base.operators.operator.features.optimization.AutomaticFeatureEngineering;
import base.operators.operator.features.optimization.ErrorCalculator;
import base.operators.operator.features.optimization.Function;
import base.operators.operator.features.optimization.Individual;
import base.operators.operator.features.optimization.OptimizationResultsCollector;
import base.operators.operator.features.optimization.Population;
import base.operators.operator.features.optimization.AutomaticFeatureEngineering.LearningType;
import base.operators.operator.features.meta.AutomaticFeatureEngineeringOutputRule;
import base.operators.operator.features.meta.TrainingDataPrecondition;
import base.operators.operator.tools.OptimizationListener;
import base.operators.operator.IOObjectCollection;
import base.operators.operator.OperatorChain;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.UserError;
import base.operators.operator.ValueDouble;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.ExampleSetPassThroughRule;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.SubprocessTransformRule;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.ParameterTypeRegexp;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.EqualTypeCondition;
import base.operators.parameter.conditions.NonEqualTypeCondition;
import base.operators.tools.RandomGenerator;
import base.operators.tools.Tools;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class AutomaticFeatureEngineeringOperator extends OperatorChain implements OptimizationListener {
    public static final String TRAINING_INPUT_PORT_NAME = "example set in";
    private final OutputPort innerTrainingSource = (OutputPort)this.getSubprocess(0).getInnerSources().createPort("example set source");
    private final InputPort innerPerformanceSink = this.getSubprocess(0).getInnerSinks().createPort("performance sink", PerformanceVector.class);
    public static final String FEATURE_SET_OUTPUT_PORT_NAME = "feature set";
    public static final String POPULATION_PORT_NAME = "population";
    public static final String OPTIMIZATION_LOG_PORT_NAME = "optimization log";
    public static final String PARAMETER_MODE = "mode";
    public static final String[] MODES;
    public static final int MODE_KEEP_ALL = 0;
    public static final int MODE_FEATURE_SELECTION = 1;
    public static final int MODE_FEATURE_ENGINEERING = 2;
    public static final String PARAMETER_COMPLEXITY_ACCURACY_BALANCE = "balance for accuracy";
    public static final String PARAMETER_SHOW_PROGRESS_DIALOG = "show progress dialog";
    public static final String PARAMETER_USE_OPTIMIZATION_HEURISTICS = "use optimization heuristics";
    public static final String PARAMETER_MAX_GENERATIONS = "maximum generations";
    public static final String PARAMETER_POPULATION_SIZE = "population size";
    public static final String PARAMETER_USE_MULTI_STARTS = "use multi-starts";
    public static final String PARAMETER_NUMBER_OF_MULTI_STARTS = "number of multi-starts";
    public static final String PARAMETER_GENERATIONS_UNTIL_RESTART = "generations until multi-start";
    public static final String PARAMETER_USE_TIME_LIMIT = "use time limit";
    public static final String PARAMETER_TIME_LIMIT = "time limit in seconds";
    public static final String PARAMETER_MAX_FUNCTION_COMPLEXITY = "maximum function complexity";
    public static final String PARAMETER_USE_SUBSET_FOR_GENERATION = "use subset for generation";
    public static final String PARAMETER_GENERATION_SUBSET_REG_EXP = "subset for generation";
    public static final String PARAMETER_USE_PLUS = "use_plus";
    public static final String PARAMETER_USE_DIFF = "use_diff";
    public static final String PARAMETER_USE_MULT = "use_mult";
    public static final String PARAMETER_USE_DIV = "use_div";
    public static final String PARAMETER_USE_RECIPROCAL_VALUE = "reciprocal_value";
    public static final String PARAMETER_USE_SQUARE_ROOTS = "use_square_roots";
    public static final String PARAMETER_USE_EXP = "use_exp";
    public static final String PARAMETER_USE_LOG = "use_log";
    public static final String PARAMETER_USE_ABSOLUTE_VALUES = "use_absolute_values";
    public static final String PARAMETER_USE_SGN = "use_sgn";
    public static final String PARAMETER_USE_MIN = "use_min";
    public static final String PARAMETER_USE_MAX = "use_max";
    private InputPort trainingDataInputPort = this.getInputPorts().createPort("example set in", ExampleSet.class);
    private OutputPort featureSetOutputPort;
    private OutputPort populationOutputPort;
    private OutputPort optimizationLogOutputPort;
    private AutomaticFeatureEngineering optimizer;
    private OptimizationResultsCollector optimizationResultsCollector;
    private Set<String> evaluatedFeatureSetStrings = new HashSet();
    private Set<String> generatedFeatureStrings = new HashSet();

    public AutomaticFeatureEngineeringOperator(OperatorDescription description) {
        super(description, new String[]{"Evaluation Process"});
        this.trainingDataInputPort.addPrecondition(new TrainingDataPrecondition(this.trainingDataInputPort));
        this.featureSetOutputPort = (OutputPort)this.getOutputPorts().createPort("feature set");
        this.populationOutputPort = (OutputPort)this.getOutputPorts().createPort("population");
        this.optimizationLogOutputPort = (OutputPort)this.getOutputPorts().createPort("optimization log");
        this.getTransformer().addRule(new ExampleSetPassThroughRule(this.trainingDataInputPort, this.innerTrainingSource, SetRelation.SUBSET));
        this.getTransformer().addRule(new SubprocessTransformRule(this.getSubprocess(0)));
        AutomaticFeatureEngineeringOutputRule rule = new AutomaticFeatureEngineeringOutputRule(this.trainingDataInputPort, this.innerPerformanceSink, this.featureSetOutputPort, this.populationOutputPort, this.optimizationLogOutputPort, this);
        this.getTransformer().addRule(rule);
        this.addValue(new ValueDouble("number_feature_sets", "The number of different feature sets evaluated by this operator.") {
            @Override
            public double getDoubleValue() {
                return (double)AutomaticFeatureEngineeringOperator.this.evaluatedFeatureSetStrings.size();
            }
        });
        this.addValue(new ValueDouble("number_generated_features", "The number of different features generated by this operator.") {
            @Override
            public double getDoubleValue() {
                return (double)AutomaticFeatureEngineeringOperator.this.generatedFeatureStrings.size();
            }
        });
    }

    @Override
    public void doWork() throws OperatorException {
        this.evaluatedFeatureSetStrings.clear();
        this.generatedFeatureStrings.clear();
        final ExampleSet trainingData = (ExampleSet)this.trainingDataInputPort.getData(ExampleSet.class);
        this.optimizationResultsCollector = new OptimizationResultsCollector(LearningType.SUPERVISED);
        int mode = this.getParameterAsInt("mode");
        if (mode == 0) {
            FeatureSet featureSet = new FeatureSet(trainingData);
            featureSet.storeStatistics(trainingData);
            this.featureSetOutputPort.deliver(new FeatureSetIOObject(featureSet, true));
            IOObjectCollection<FeatureSetIOObject> paretoFrontFeatureSets = new IOObjectCollection();
            FeatureSetIOObject featureSetIOObject = new FeatureSetIOObject(new FeatureSet(featureSet), true);
            paretoFrontFeatureSets.add(featureSetIOObject);
            this.populationOutputPort.deliver(paretoFrontFeatureSets);
            this.optimizationLogOutputPort.deliver(this.optimizationResultsCollector.createExampleSet());
        } else {

            RandomGenerator rng;
            if (this.getParameterAsBoolean("use_local_random_seed")) {
                int localRandomSeed = this.getParameterAsInt("local_random_seed");
                rng = RandomGenerator.getRandomGenerator(true, localRandomSeed);
            } else {
                rng = RandomGenerator.getGlobalRandomGenerator();
            }

            List<Function> functions = null;
            if (mode == 2) {
                functions = this.getFunctions();
            }

            int maxGenerations = -1;
            int populationSize = -1;
            if (!this.getParameterAsBoolean("use optimization heuristics")) {
                maxGenerations = this.getParameterAsInt("maximum generations");
                populationSize = this.getParameterAsInt("population size");
            }

            int numberOfMultiStarts = 0;
            int generationsUntilMultiStart = maxGenerations;
            boolean useMultiStarts = this.getParameterAsBoolean("use multi-starts");
            if (useMultiStarts) {
                numberOfMultiStarts = this.getParameterAsInt("number of multi-starts");
                generationsUntilMultiStart = this.getParameterAsInt("generations until multi-start");
            }

            int timeLimit = -1;
            if (this.getParameterAsBoolean("use time limit")) {
                timeLimit = this.getParameterAsInt("time limit in seconds");
            }

            int maxFunctionComplexity = this.getParameterAsInt("maximum function complexity");
            String whiteListRegExp = null;
            if (this.getParameterAsBoolean("use subset for generation")) {
                whiteListRegExp = this.getParameterAsString("subset for generation");
            }

            this.optimizer = new AutomaticFeatureEngineering();
            this.optimizer.addListener(this);

            class PerformanceCalculator implements ErrorCalculator {
                PerformanceCalculator() {
                }

                @Override
                public void calculateError(Individual individual) throws OperatorException {
                    AutomaticFeatureEngineeringOperator.this.evaluate(individual, trainingData);
                }
            }

            Population result = this.optimizer.run(trainingData, new PerformanceCalculator(), mode == 1, functions, maxFunctionComplexity, whiteListRegExp, maxGenerations, populationSize, numberOfMultiStarts, generationsUntilMultiStart, timeLimit, LearningType.SUPERVISED, rng);
            double balance = this.getParameterAsDouble("balance for accuracy");
            Individual individual = result.getIndividualForBalance(true, balance);
            FeatureSet resultingFeatureSet = individual.getFeatureSet();
            resultingFeatureSet.storeStatistics(trainingData);
            this.featureSetOutputPort.deliver(new FeatureSetIOObject(resultingFeatureSet, individual.isOriginal()));
            IOObjectCollection<FeatureSetIOObject> paretoFrontFeatureSets = new IOObjectCollection();
            Iterator var19 = result.getParetoFront(true).iterator();

            while(var19.hasNext()) {
                Individual ind = (Individual)var19.next();
                FeatureSetIOObject featureSetIOObject = new FeatureSetIOObject(ind.getFeatureSet(), ind.isOriginal());
                featureSetIOObject.setLastKnownFitness(ind.getError(), ind.isPercent());
                paretoFrontFeatureSets.add(featureSetIOObject);
            }

            this.populationOutputPort.deliver(paretoFrontFeatureSets);
            this.optimizationLogOutputPort.deliver(this.optimizationResultsCollector.createExampleSet());
        }

    }

    private void evaluate(Individual individual, ExampleSet trainingData) throws OperatorException {
        FeatureSet featureSet = individual.getFeatureSet();
        if (featureSet.getNumberOfFeatures() < 1) {
            individual.setError(1.0D / 0.0, false);
        } else {
            ExampleSet transformedTrainingData = featureSet.apply(trainingData, true, false, false, false);
            transformedTrainingData.recalculateAllAttributeStatistics();
            Iterator a = transformedTrainingData.getAttributes().regularAttributes();

            while(a.hasNext()) {
                AttributeRole attributeRole = (AttributeRole)a.next();
                if (Tools.isEqual(transformedTrainingData.getStatistics(attributeRole.getAttribute(), "minimum"), transformedTrainingData.getStatistics(attributeRole.getAttribute(), "maximum"))) {
                    a.remove();
                }
            }

            if (transformedTrainingData.getAttributes().size() == 0) {
                individual.setError(1.0D / 0.0, false);
            } else {
                this.collectLoggingValues(featureSet);
                this.innerTrainingSource.deliver(transformedTrainingData);
                this.getSubprocess(0).execute();
                PerformanceVector performanceVector = (PerformanceVector)this.innerPerformanceSink.getData(PerformanceVector.class);
                double result = performanceVector.getMainCriterion().getAverage();
                double fitness = performanceVector.getMainCriterion().getFitness();
                double halfMaxFitness = 0.0D / 0.0;
                if (!Double.isInfinite(performanceVector.getMainCriterion().getMaxFitness())) {
                    halfMaxFitness = performanceVector.getMainCriterion().getMaxFitness() / 2.0D;
                }

                if (!Double.isInfinite(fitness) && !Double.isInfinite(result) && !Double.isNaN(result) && !Double.isNaN(fitness) && !Tools.isEqual(result, 0.0D) && (Double.isNaN(halfMaxFitness) || !Tools.isEqual(fitness, halfMaxFitness)) && Tools.isEqual(result, fitness)) {
                    throw new UserError(this, "model_simulator.wrong_performance_type", new Object[]{performanceVector.getMainCriterion().getName()});
                }

                boolean isPercent = performanceVector.getMainCriterion().formatPercent();
                if (!Double.isInfinite(result) && !Double.isNaN(result)) {
                    individual.setError(result, isPercent);
                } else {
                    individual.setError(1.0D / 0.0, isPercent);
                }
            }
        }

    }

    public List<Function> getFunctions() {
        List<Function> functions = new LinkedList();
        if (this.getParameterAsBoolean("use_plus")) {
            functions.add(new Function("+", 2, true, true, 0));
        }

        if (this.getParameterAsBoolean("use_diff")) {
            functions.add(new Function("-", 2, true, true, 0));
        }

        if (this.getParameterAsBoolean("use_mult")) {
            functions.add(new Function("*", 2, false, true, 0));
        }

        if (this.getParameterAsBoolean("use_div")) {
            functions.add(new Function("/", 2, true, true, 0));
        }

        if (this.getParameterAsBoolean("reciprocal_value")) {
            functions.add(new Function("1/", 1, true, true, 0));
        }

        if (this.getParameterAsBoolean("use_square_roots")) {
            functions.add(new Function("sqrt", 1, true, false, 1));
        }

        if (this.getParameterAsBoolean("use_exp")) {
            functions.add(new Function("exp", 1, true, false, 1));
        }

        if (this.getParameterAsBoolean("use_log")) {
            functions.add(new Function("log", 1, true, false, 1));
        }

        if (this.getParameterAsBoolean("use_absolute_values")) {
            functions.add(new Function("abs", 1, true, false, 1));
        }

        if (this.getParameterAsBoolean("use_sgn")) {
            functions.add(new Function("sgn", 1, true, false, 1));
        }

        if (this.getParameterAsBoolean("use_min")) {
            functions.add(new Function("min", 2, true, false, 1));
        }

        if (this.getParameterAsBoolean("use_max")) {
            functions.add(new Function("max", 2, true, false, 1));
        }

        return functions;
    }

    @Override
    public void optimizationStarted(int maxGenerations) {
        this.getProgress().setIndeterminate(true);
    }

    @Override
    public void nextGeneration(int generation) {

        if (this.optimizationResultsCollector != null) {
            this.optimizationResultsCollector.nextGeneration(this.optimizer.getCurrentPopulation(), generation);
        }

        try {
            this.checkForStop();
        } catch (ProcessStoppedException var3) {
            this.optimizer.shouldStop();
        }

    }

    @Override
    public void optimizationFinished() {
        this.getProgress().complete();
    }

    private void collectLoggingValues(FeatureSet featureSet) {
        this.evaluatedFeatureSetStrings.add(featureSet.getNormalizedString());
        Iterator var2 = featureSet.iterator();

        while(var2.hasNext()) {
            Feature feature = (Feature)var2.next();
            if (feature.getComplexity() > 1) {
                this.generatedFeatureStrings.add(feature.getExpression());
            }
        }

    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type;
        type = new ParameterTypeCategory("mode", "The working mode of this operator: keep all original features, feature selection, or feature engineering.", MODES, 1, false);
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeDouble("balance for accuracy", "Defines a balance between 0 (most simple feature set) and 1 (most accurate feature set) to pick the final solution.", 0.0D, 1.0D, 1.0D);
        type.setExpert(false);
        type.registerDependencyCondition(new NonEqualTypeCondition(this, "mode", MODES, false, new int[]{0}));
        types.add(type);
        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
        type = new ParameterTypeBoolean("use optimization heuristics", "Indicates if heuristics should be used to determine a good population size and maximum number of generations.", true);
        type.setExpert(true);
        type.registerDependencyCondition(new NonEqualTypeCondition(this, "mode", MODES, false, new int[]{0}));
        types.add(type);
        type = new ParameterTypeInt("maximum generations", "The maximum number of generations.", 1, 2147483647, 30);
        type.setExpert(true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use optimization heuristics", false, false));
        types.add(type);
        type = new ParameterTypeInt("population size", "The population size.", 1, 2147483647, 10);
        type.setExpert(true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use optimization heuristics", false, false));
        types.add(type);
        type = new ParameterTypeBoolean("use multi-starts", "Indicates if the optimization should be restarted if there is no early improvement.", true);
        type.setExpert(true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use optimization heuristics", false, false));
        types.add(type);
        type = new ParameterTypeInt("number of multi-starts", "The maximum number of multi-starts.", 0, 2147483647, 5);
        type.setExpert(true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use multi-starts", false, true));
        types.add(type);
        type = new ParameterTypeInt("generations until multi-start", "The number of early generations without improvement before a restart is triggered.", 1, 2147483647, 10);
        type.setExpert(true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use multi-starts", false, true));
        types.add(type);
        type = new ParameterTypeBoolean("use time limit", "Indicates if a time limit should be used to stop the optimization.", false);
        type.setExpert(true);
        type.registerDependencyCondition(new NonEqualTypeCondition(this, "mode", MODES, false, new int[]{0}));
        types.add(type);
        type = new ParameterTypeInt("time limit in seconds", "The number of seconds after the optimization will be stopped.", 1, 2147483647, 60);
        type.setExpert(true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use time limit", false, true));
        type.registerDependencyCondition(new NonEqualTypeCondition(this, "mode", MODES, false, new int[]{0}));
        types.add(type);
        type = new ParameterTypeBoolean("use subset for generation", "Indicates if only a subset of numerical attributes should be allowed for generation.", false);
        type.setExpert(true);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeRegexp("subset for generation", "A regular expression describing the subset of features available for generation.");
        type.setExpert(true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use subset for generation", true, true));
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeInt("maximum function complexity", "The maximum complexity allowed for generated functions.", 1, 2147483647, 10);
        type.setExpert(true);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("use_plus", "Generate sums.", false);
        type.setExpert(false);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("use_diff", "Generate differences.", false);
        type.setExpert(false);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("use_mult", "Generate products.", true);
        type.setExpert(false);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("use_div", "Generate quotients.", true);
        type.setExpert(false);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("reciprocal_value", "Generate reciprocal values.", true);
        type.setExpert(false);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("use_square_roots", "Generate square root values.", false);
        type.setExpert(true);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("use_exp", "Generate exponential functions.", false);
        type.setExpert(true);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("use_log", "Generate logarithmic functions.", false);
        type.setExpert(true);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("use_absolute_values", "Generate absolute values.", false);
        type.setExpert(true);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("use_sgn", "Generate signum values.", false);
        type.setExpert(true);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("use_min", "Generate minimum values.", false);
        type.setExpert(true);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        type = new ParameterTypeBoolean("use_max", "Generate maximum values.", false);
        type.setExpert(true);
        type.registerDependencyCondition(new EqualTypeCondition(this, "mode", MODES, false, new int[]{2}));
        types.add(type);
        return types;
    }

    static {
        MODES = new String[]{"keep all original features", "feature selection", "feature selection and generation"};
    }
}

