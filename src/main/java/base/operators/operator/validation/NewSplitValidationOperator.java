package base.operators.operator.validation;

import java.util.List;

import base.operators.operator.Model;
import base.operators.operator.performance.PerformanceVector;
import base.operators.example.ExampleSet;
import base.operators.example.set.SplittedExampleSet;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.UserError;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.CapabilityPrecondition;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.Precondition;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.operator.visualization.ProcessLogOperator;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.EqualTypeCondition;
import base.operators.tools.RandomGenerator;

/**
 * <p>
 * A FixedSplitValidationChain splits up the example set at a fixed point into a training and test
 * set and evaluates the model (linear sampling). For non-linear sampling methods, i.e. the data is
 * shuffled, the specified amounts of data are used as training and test set. The sum of both must
 * be smaller than the input example set size.
 * </p>
 *
 * <p>
 * At least either the training set size must be specified (rest is used for testing) or the test
 * set size must be specified (rest is used for training). If both are specified, the rest is not
 * used at all.
 * </p>
 *
 * <p>
 * The first inner operator must accept an {@link ExampleSet} while the
 * second must accept an {@link ExampleSet} and the output of the first
 * (which in most cases is a {@link Model}) and must produce a
 * {@link PerformanceVector}.
 * </p>
 *
 * <p>
 * This validation operator provides several values which can be logged by means of a
 * {@link ProcessLogOperator}. All performance estimation operators of RapidMiner provide access to
 * the average values calculated during the estimation. Since the operator cannot ensure the names
 * of the delivered criteria, the ProcessLog operator can access the values via the generic value
 * names:
 * </p>
 * <ul>
 * <li>performance: the value for the main criterion calculated by this validation operator</li>
 * <li>performance1: the value of the first criterion of the performance vector calculated</li>
 * <li>performance2: the value of the second criterion of the performance vector calculated</li>
 * <li>performance3: the value of the third criterion of the performance vector calculated</li>
 * <li>for the main criterion, also the variance and the standard deviation can be accessed where
 * applicable.</li>
 * </ul>
 *
 * @author wangj
 */
public class NewSplitValidationOperator extends NewValidationChain {

    public static final String PARAMETER_SPLIT = "split";

    public static final String[] SPLIT_MODES = { "absolute", "relative" };

    public static final int SPLIT_ABSOLUTE = 0;

    public static final int SPLIT_RELATIVE = 1;

    public static final String PARAMETER_SPLIT_RATIO = "split_ratio";

    public static final String PARAMETER_TRAINING_SET_SIZE = "training_set_size";

    public static final String PARAMETER_TEST_SET_SIZE = "test_set_size";

    public static final String PARAMETER_SAMPLING_TYPE = "sampling_type";

    public NewSplitValidationOperator(OperatorDescription description) {
        super(description);
    }

    @Override
    protected Precondition getCapabilityPrecondition() {
        return new CapabilityPrecondition(this, trainingSetInput) {

            @Override
            protected List<QuickFix> getFixesForRegressionWhenClassificationSupported(AttributeMetaData labelMD) {
                List<QuickFix> fixes = super.getFixesForRegressionWhenClassificationSupported(labelMD);
                fixes.add(0, new ParameterSettingQuickFix(NewSplitValidationOperator.this, PARAMETER_SAMPLING_TYPE,
                        SplittedExampleSet.SHUFFLED_SAMPLING + "", "switch_to_shuffled_sampling"));
                return fixes;
            }
        };
    }

    @Override
    public void estimatePerformance(ExampleSet inputSet) throws OperatorException {
        SplittedExampleSet eSet = null;

        switch (getParameterAsInt(PARAMETER_SPLIT)) {
            case SPLIT_RELATIVE:
                double splitRatio = getParameterAsDouble(PARAMETER_SPLIT_RATIO);
                eSet = new SplittedExampleSet(inputSet, splitRatio, getParameterAsInt(PARAMETER_SAMPLING_TYPE),
                        getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
                        getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED),
                        getCompatibilityLevel().isAtMost(SplittedExampleSet.VERSION_SAMPLING_CHANGED));
                break;
            case SPLIT_ABSOLUTE: {
                int trainingSetSize = getParameterAsInt(PARAMETER_TRAINING_SET_SIZE);
                int testSetSize = getParameterAsInt(PARAMETER_TEST_SET_SIZE);
                int inputSetSize = inputSet.size();
                if (inputSetSize < trainingSetSize + testSetSize) {
                    throw new UserError(this, 110, trainingSetSize + testSetSize + " (" + trainingSetSize + " for training, "
                            + testSetSize + " for testing)");
                }

                int rest = inputSetSize - (trainingSetSize + testSetSize);
                if (trainingSetSize < 1 && testSetSize < 1) {
                    throw new UserError(this, 116, "training_set_size / test_set_size",
                            "either training_set_size or test_set_size or both must be greater than 1.");
                } else if (testSetSize < 1) {
                    rest = 0;
                    testSetSize = inputSetSize - trainingSetSize;
                } else if (trainingSetSize < 1) {
                    rest = 0;
                    trainingSetSize = inputSetSize - testSetSize;
                }
                log("Using " + trainingSetSize + " examples for learning and " + testSetSize + " examples for testing. "
                        + rest + " examples are not used.");
                double[] ratios = new double[] { (double) trainingSetSize / (double) inputSetSize,
                        (double) testSetSize / (double) inputSetSize, (double) rest / (double) inputSetSize };
                eSet = new SplittedExampleSet(inputSet, ratios, getParameterAsInt(PARAMETER_SAMPLING_TYPE),
                        getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
                        getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED),
                        getCompatibilityLevel().isAtMost(SplittedExampleSet.VERSION_SAMPLING_CHANGED));
                break;
            }
        }

        validate(eSet);
    }

    @Override
    protected MDInteger getTrainingSetSize(MDInteger originalSize) throws UndefinedParameterError {
        switch (getParameterAsInt(PARAMETER_SPLIT)) {
            case SPLIT_RELATIVE:
                return originalSize.multiply(getParameterAsDouble(PARAMETER_SPLIT_RATIO));
            case SPLIT_ABSOLUTE:
                return new MDInteger(getParameterAsInt(PARAMETER_TRAINING_SET_SIZE));
            default:
                return new MDInteger();
        }
    }

    @Override
    protected MDInteger getTestSetSize(MDInteger originalSize) throws UndefinedParameterError {
        switch (getParameterAsInt(PARAMETER_SPLIT)) {
            case SPLIT_RELATIVE:
                return originalSize.multiply(1.0d - getParameterAsDouble(PARAMETER_SPLIT_RATIO));
            case SPLIT_ABSOLUTE:
                return new MDInteger(getParameterAsInt(PARAMETER_TEST_SET_SIZE));
            default:
                return new MDInteger();
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type = new ParameterTypeCategory(PARAMETER_SPLIT, "Specifies how the example set should be splitted.",
                SPLIT_MODES, SPLIT_RELATIVE);
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeDouble(PARAMETER_SPLIT_RATIO, "Relative size of the training set", 0.0d, 1.0d, 0.7d);
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SPLIT, SPLIT_MODES, true, SPLIT_RELATIVE));
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeInt(PARAMETER_TRAINING_SET_SIZE,
                "Absolute size required for the training set (-1: use rest for training)", -1, Integer.MAX_VALUE, 100);
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SPLIT, SPLIT_MODES, true, SPLIT_ABSOLUTE));
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeInt(PARAMETER_TEST_SET_SIZE,
                "Absolute size required for the test set (-1: use rest for testing)", -1, Integer.MAX_VALUE, -1);
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SPLIT, SPLIT_MODES, true, SPLIT_ABSOLUTE));
        type.setExpert(false);
        types.add(type);
        types.add(new ParameterTypeCategory(PARAMETER_SAMPLING_TYPE,
                "Defines the sampling type of the cross validation (linear = consecutive subsets, shuffled = random subsets, stratified = random subsets with class distribution kept constant, automatic = primary stratified or secondary shuffled)",
                SplittedExampleSet.SAMPLING_NAMES, SplittedExampleSet.AUTOMATIC, false));
        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
        return types;
    }

    @Override
    public OperatorVersion[] getIncompatibleVersionChanges() {
        return new OperatorVersion[] { SplittedExampleSet.VERSION_SAMPLING_CHANGED };
    }

    @Override
    public boolean supportsCapability(OperatorCapability capability) {
        switch (capability) {
            case NO_LABEL:
                return false;
            case NUMERICAL_LABEL:
                try {
                    return getParameterAsInt(PARAMETER_SAMPLING_TYPE) != SplittedExampleSet.STRATIFIED_SAMPLING;
                } catch (UndefinedParameterError e) {
                    return false;
                }
            default:
                return true;
        }
    }
}
