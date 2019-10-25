package base.operators.operator.validation;


import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.set.MappedExampleSet;
import base.operators.example.set.SplittedExampleSet;
import base.operators.operator.*;
import base.operators.operator.concurrency.execution.BackgroundExecutionService;
import base.operators.operator.ports.metadata.*;
import base.operators.operator.tools.ConcurrencyTools;
import base.operators.operator.concurrency.internal.ParallelOperatorChain;
import base.operators.operator.learner.CapabilityProvider;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.parameter.*;
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

public class NewBootstrappingValidation extends NewValidationChain{
    public static final String PARAMETER_NUMBER_OF_VALIDATIONS = "number_of_validations";

    public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

    public static final String PARAMETER_USE_WEIGHTS = "use_weights";

    public static final String PARAMETER_AVERAGE_PERFORMANCES_ONLY = "average_performances_only";

    private int number;

    private int iteration;

    public NewBootstrappingValidation(OperatorDescription description) {
        super(description);
        addValue(new ValueDouble("iteration", "The number of the current iteration.") {

            @Override
            public double getDoubleValue() {
                return iteration;
            }
        });
    }

    @Override
    public void estimatePerformance(ExampleSet inputSet) throws OperatorException {
        boolean useWeights = getParameterAsBoolean(PARAMETER_USE_WEIGHTS);
        number = getParameterAsInt(PARAMETER_NUMBER_OF_VALIDATIONS);
        int size = (int) Math.round(inputSet.size() * getParameterAsDouble(PARAMETER_SAMPLE_RATIO));

        // start bootstrapping loop
        RandomGenerator random = RandomGenerator.getRandomGenerator(this);
        if (modelOutput.isConnected()) {
            getProgress().setTotal(number + 1);
        } else {
            getProgress().setTotal(number);
        }
        getProgress().setCheckForStop(false);

        for (iteration = 0; iteration < number; iteration++) {
            int[] mapping = null;
            if (useWeights && inputSet.getAttributes().getWeight() != null) {
                mapping = MappedExampleSet.createWeightedBootstrappingMapping(inputSet, size, random);
            } else {
                mapping = MappedExampleSet.createBootstrappingMapping(inputSet, size, random);
            }
//            MappedExampleSet trainingSet = new MappedExampleSet(inputSet, mapping, true);
//            learn(trainingSet);
//
//            MappedExampleSet inverseExampleSet = new MappedExampleSet(inputSet, mapping, false);
//            evaluate(inverseExampleSet);
            validate(inputSet, mapping);
            inApplyLoop();
            getProgress().step();
        }
    }

    @Override
    protected MDInteger getTestSetSize(MDInteger originalSize) throws UndefinedParameterError {
        return originalSize.multiply(1d - getParameterAsDouble(PARAMETER_SAMPLE_RATIO));
    }

    @Override
    protected MDInteger getTrainingSetSize(MDInteger originalSize) throws UndefinedParameterError {
        return originalSize.multiply(getParameterAsDouble(PARAMETER_SAMPLE_RATIO));
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_VALIDATIONS,
                "The number of validations that should be executed.", 2, Integer.MAX_VALUE, 10);
        type.setExpert(false);
        types.add(type);
        types.add(new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO,
                "This ratio of examples will be sampled (with replacement) in each iteration.", 0.0d,
                Double.POSITIVE_INFINITY, 1.0d));
        types.add(new ParameterTypeBoolean(PARAMETER_USE_WEIGHTS,
                "If checked, example weights will be used for bootstrapping if such weights are available.", true));
        types.add(new ParameterTypeBoolean(PARAMETER_AVERAGE_PERFORMANCES_ONLY,
                "Indicates if only performance vectors should be averaged or all types of averagable result vectors.",
                true));
        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
        return types;
    }

    @Override
    public boolean supportsCapability(OperatorCapability capability) {
        return true;
    }
}

