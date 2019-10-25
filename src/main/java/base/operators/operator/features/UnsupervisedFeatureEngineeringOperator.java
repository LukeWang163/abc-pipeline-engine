package base.operators.operator.features;

import base.operators.example.AttributeRole;
import base.operators.example.ExampleSet;
import base.operators.operator.features.meta.TrainingDataPrecondition;
import base.operators.operator.features.meta.UnsupervisedFeatureEngineeringOutputRule;
import base.operators.operator.features.optimization.AutomaticFeatureEngineering;
import base.operators.operator.features.optimization.ErrorCalculator;
import base.operators.operator.features.optimization.OptimizationResultsCollector;
import base.operators.operator.tools.OptimizationListener;
import base.operators.operator.IOObjectCollection;
import base.operators.operator.OperatorChain;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.clustering.CentroidClusterModel;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.ExampleSetPassThroughRule;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.SubprocessTransformRule;
import base.operators.operator.validation.clustering.CentroidBasedEvaluator;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.NonEqualTypeCondition;
import base.operators.tools.RandomGenerator;
import base.operators.tools.Tools;
import java.util.Iterator;
import java.util.List;

public class UnsupervisedFeatureEngineeringOperator extends OperatorChain implements OptimizationListener {
    public static final String TRAINING_INPUT_PORT_NAME = "example set in";
    private final OutputPort innerTrainingSource = (OutputPort)this.getSubprocess(0).getInnerSources().createPort("example set source");
    private final InputPort innerClusterModelSink = this.getSubprocess(0).getInnerSinks().createPort("cluster model sink", CentroidClusterModel.class);
    private final InputPort innerClusteredDataSink = this.getSubprocess(0).getInnerSinks().createPort("example set sink", ExampleSet.class);
    public static final String FEATURE_SET_OUTPUT_PORT_NAME = "feature set";
    public static final String POPULATION_PORT_NAME = "population";
    public static final String OPTIMIZATION_LOG_PORT_NAME = "optimization log";
    public static final String PARAMETER_MODE = "mode";
    public static final String[] MODES;
    public static final int MODE_KEEP_ALL = 0;
    public static final int MODE_FEATURE_SELECTION = 1;
    public static final String PARAMETER_SIMPLICITY_BALANCE = "balance for simplicity";
    public static final String PARAMETER_SHOW_PROGRESS_DIALOG = "show progress dialog";
    public static final String PARAMETER_USE_OPTIMIZATION_HEURISTICS = "use optimization heuristics";
    public static final String PARAMETER_MAX_GENERATIONS = "maximum generations";
    public static final String PARAMETER_POPULATION_SIZE = "population size";
    public static final String PARAMETER_USE_MULTI_STARTS = "use multi-starts";
    public static final String PARAMETER_NUMBER_OF_MULTI_STARTS = "number of multi-starts";
    public static final String PARAMETER_GENERATIONS_UNTIL_RESTART = "generations until multi-start";
    public static final String PARAMETER_USE_TIME_LIMIT = "use time limit";
    public static final String PARAMETER_TIME_LIMIT = "time limit in seconds";
    private InputPort trainingDataInputPort = this.getInputPorts().createPort("example set in", ExampleSet.class);
    private OutputPort featureSetOutputPort;
    private OutputPort populationOutputPort;
    private OutputPort optimizationLogOutputPort;
    private AutomaticFeatureEngineering optimizer;
    private OptimizationResultsCollector optimizationResultsCollector;

    public UnsupervisedFeatureEngineeringOperator(OperatorDescription description) {
        super(description, new String[]{"Clustering Process"});
        this.trainingDataInputPort.addPrecondition(new TrainingDataPrecondition(this.trainingDataInputPort));
        this.featureSetOutputPort = (OutputPort)this.getOutputPorts().createPort("feature set");
        this.populationOutputPort = (OutputPort)this.getOutputPorts().createPort("population");
        this.optimizationLogOutputPort = (OutputPort)this.getOutputPorts().createPort("optimization log");
        this.getTransformer().addRule(new ExampleSetPassThroughRule(this.trainingDataInputPort, this.innerTrainingSource, SetRelation.SUBSET));
        this.getTransformer().addRule(new SubprocessTransformRule(this.getSubprocess(0)));
        UnsupervisedFeatureEngineeringOutputRule rule = new UnsupervisedFeatureEngineeringOutputRule(this.trainingDataInputPort, this.innerClusteredDataSink, this.innerClusterModelSink, this.featureSetOutputPort, this.populationOutputPort, this.optimizationLogOutputPort, this);
        this.getTransformer().addRule(rule);
    }

    @Override
    public void doWork() throws OperatorException {
        final ExampleSet trainingData = (ExampleSet)this.trainingDataInputPort.getData(ExampleSet.class);
        this.optimizationResultsCollector = new OptimizationResultsCollector(AutomaticFeatureEngineering.LearningType.UNSUPERVISED);
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
            int maxGenerations;
            if (this.getParameterAsBoolean("use_local_random_seed")) {
                maxGenerations = this.getParameterAsInt("local_random_seed");
                rng = RandomGenerator.getRandomGenerator(true, maxGenerations);
            } else {
                rng = RandomGenerator.getGlobalRandomGenerator();
            }

            maxGenerations = -1;
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

            this.optimizer = new AutomaticFeatureEngineering();
            this.optimizer.addListener(this);

            class PerformanceCalculator implements ErrorCalculator {
                PerformanceCalculator() {
                }

                @Override
                public void calculateError(base.operators.operator.features.optimization.Individual individual) throws OperatorException {
                    UnsupervisedFeatureEngineeringOperator.this.evaluate(individual, trainingData);
                }
            }

            base.operators.operator.features.optimization.Population result = this.optimizer.run(trainingData, new PerformanceCalculator(), mode == 1, (List)null, 0, (String)null, maxGenerations, populationSize, numberOfMultiStarts, generationsUntilMultiStart, timeLimit, AutomaticFeatureEngineering.LearningType.UNSUPERVISED, rng);
            double balance = this.getParameterAsDouble("balance for simplicity");
            base.operators.operator.features.optimization.Individual individual = result.getIndividualForBalance(false, balance);
            FeatureSet resultingFeatureSet = individual.getFeatureSet();
            resultingFeatureSet.storeStatistics(trainingData);
            this.featureSetOutputPort.deliver(new FeatureSetIOObject(resultingFeatureSet, individual.isOriginal()));
            IOObjectCollection<FeatureSetIOObject> paretoFrontFeatureSets = new IOObjectCollection();
            Iterator var16 = result.getParetoFront(false).iterator();

            while(var16.hasNext()) {
                base.operators.operator.features.optimization.Individual ind = (base.operators.operator.features.optimization.Individual)var16.next();
                FeatureSetIOObject featureSetIOObject = new FeatureSetIOObject(ind.getFeatureSet(), ind.isOriginal());
                featureSetIOObject.setLastKnownFitness(ind.getError(), ind.isPercent());
                paretoFrontFeatureSets.add(featureSetIOObject);
            }

            this.populationOutputPort.deliver(paretoFrontFeatureSets);
            this.optimizationLogOutputPort.deliver(this.optimizationResultsCollector.createExampleSet());
        }

    }

    private void evaluate(base.operators.operator.features.optimization.Individual individual, ExampleSet trainingData) throws OperatorException {
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
                this.innerTrainingSource.deliver(transformedTrainingData);
                this.getSubprocess(0).execute();
                CentroidClusterModel clusterModel = (CentroidClusterModel)this.innerClusterModelSink.getData(CentroidClusterModel.class);
                ExampleSet clusteredData = (ExampleSet)this.innerClusteredDataSink.getData(ExampleSet.class);
                double daviesBouldin = CentroidBasedEvaluator.getDaviesBouldin(clusterModel, clusteredData);
                if (!Double.isInfinite(daviesBouldin) && !Double.isNaN(daviesBouldin)) {
                    individual.setError(daviesBouldin, false);
                } else {
                    individual.setError(1.0D / 0.0, false);
                }
            }
        }

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

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type = new ParameterTypeCategory("mode", "The working mode of this operator: keep all original features, feature selection, or feature engineering.", MODES, 1, false);
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeDouble("balance for simplicity", "Defines a balance between 0 (most simple feature set) and 1 (most accurate feature set) to pick the final solution.", 0.0D, 1.0D, 1.0D);
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
        return types;
    }

    static {
        MODES = new String[]{"keep all original features", "feature selection"};
    }
}
