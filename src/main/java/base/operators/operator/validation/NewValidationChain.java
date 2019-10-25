package base.operators.operator.validation;

import java.util.List;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.set.MappedExampleSet;
import base.operators.example.set.SplittedExampleSet;
import base.operators.operator.OperatorChain;
import base.operators.tools.math.AverageVector;
import base.operators.operator.IOObject;
import base.operators.operator.Model;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ValueDouble;
import base.operators.operator.learner.CapabilityProvider;
import base.operators.operator.learner.PredictionModel;
import base.operators.operator.performance.PerformanceCriterion;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.CapabilityPrecondition;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPassThroughRule;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.PassThroughRule;
import base.operators.operator.ports.metadata.Precondition;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.operator.ports.metadata.SubprocessTransformRule;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.UndefinedParameterError;


/**
 * Abstract superclass of operator chains that split an {@link ExampleSet} into a training and test
 * set and return a performance vector. The two inner operators must be a learner returning a
 * {@link Model} and an operator or operator chain that can apply this model and returns a
 * {@link PerformanceVector}. Hence the second inner operator usually is an operator chain
 * containing a model applier and a performance evaluator.
 *
 * @author wangj
 */
public abstract class NewValidationChain extends OperatorChain implements CapabilityProvider {

    /**
     * The parameter name for &quot;Indicates if a model of the complete data set should be
     * additionally build after estimation.&quot;
     */
    public static final String PARAMETER_CREATE_COMPLETE_MODEL = "create_complete_model";

    // input
    protected final InputPort trainingSetInput = getInputPorts().createPort("training", ExampleSet.class);

    // training
    protected final OutputPort trainingProcessExampleSetOutput = getSubprocess(0).getInnerSources().createPort("training");
    private final OutputPort applyProcessExampleSetOutput = getSubprocess(0).getInnerSources().createPort("testing");

    private final InputPort trainingProcessModelInput = getSubprocess(0).getInnerSinks().createPort("model", Model.class);
    private final PortPairExtender applyProcessPerformancePortExtender = new PortPairExtender("averagable",
            getSubprocess(0).getInnerSinks(), getOutputPorts(), new MetaData(AverageVector.class));

    // output
    protected final OutputPort modelOutput = getOutputPorts().createPort("model");
    private final OutputPort exampleSetOutput = getOutputPorts().createPort("training");

    private double lastMainPerformance = Double.NaN;
    private double lastMainVariance = Double.NaN;
    private double lastMainDeviation = Double.NaN;

    private double lastFirstPerformance = Double.NaN;
    private double lastSecondPerformance = Double.NaN;
    private double lastThirdPerformance = Double.NaN;

    public NewValidationChain(OperatorDescription description) {
        super(description, "Validation");

        trainingSetInput.addPrecondition(getCapabilityPrecondition());

        applyProcessPerformancePortExtender.ensureMinimumNumberOfPorts(1);

        InputPort inputPort = applyProcessPerformancePortExtender.getManagedPairs().iterator().next().getInputPort();
        inputPort.addPrecondition(new SimplePrecondition(inputPort, new MetaData(PerformanceVector.class)));
        applyProcessPerformancePortExtender.start();

        getTransformer().addRule(
                new ExampleSetPassThroughRule(trainingSetInput, trainingProcessExampleSetOutput, SetRelation.EQUAL) {

                    @Override
                    public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
                        try {
                            metaData.setNumberOfExamples(getTrainingSetSize(metaData.getNumberOfExamples()));
                        } catch (UndefinedParameterError e) {
                        }
                        return super.modifyExampleSet(metaData);
                    }
                });
        getTransformer()
                .addRule(new ExampleSetPassThroughRule(trainingSetInput, applyProcessExampleSetOutput, SetRelation.EQUAL) {

                    @Override
                    public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
                        try {
                            metaData.setNumberOfExamples(getTestSetSize(metaData.getNumberOfExamples()));
                        } catch (UndefinedParameterError e) {
                        }
                        return super.modifyExampleSet(metaData);
                    }
                });
        getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
        getTransformer().addRule(new PassThroughRule(trainingProcessModelInput, modelOutput, true));
        getTransformer().addRule(applyProcessPerformancePortExtender.makePassThroughRule());
        getTransformer().addPassThroughRule(trainingSetInput, exampleSetOutput);

        addValue(new ValueDouble("performance", "The last performance average (main criterion).") {

            @Override
            public double getDoubleValue() {
                return lastMainPerformance;
            }
        });
        addValue(new ValueDouble("variance", "The variance of the last performance (main criterion).") {

            @Override
            public double getDoubleValue() {
                return lastMainVariance;
            }
        });
        addValue(new ValueDouble("deviation", "The standard deviation of the last performance (main criterion).") {

            @Override
            public double getDoubleValue() {
                return lastMainDeviation;
            }
        });

        addValue(new ValueDouble("performance1", "The last performance average (first criterion).") {

            @Override
            public double getDoubleValue() {
                return NewValidationChain.this.lastFirstPerformance;
            }
        });
        addValue(new ValueDouble("performance2", "The last performance average (second criterion).") {

            @Override
            public double getDoubleValue() {
                return NewValidationChain.this.lastSecondPerformance;
            }
        });
        addValue(new ValueDouble("performance3", "The last performance average (third criterion).") {

            @Override
            public double getDoubleValue() {
                return NewValidationChain.this.lastThirdPerformance;
            }
        });
    }

    /**
     * This method can be overwritten in order to give a more senseful quickfix.
     */
    protected Precondition getCapabilityPrecondition() {
        return new CapabilityPrecondition(this, trainingSetInput);
    }

    protected abstract MDInteger getTrainingSetSize(MDInteger originalSize) throws UndefinedParameterError;

    protected abstract MDInteger getTestSetSize(MDInteger originalSize) throws UndefinedParameterError;

    @Override
    public boolean shouldAutoConnect(OutputPort outputPort) {
        if (outputPort == modelOutput) {
            return getParameterAsBoolean(PARAMETER_CREATE_COMPLETE_MODEL);
        } else if (outputPort == exampleSetOutput) {
            return getParameterAsBoolean("keep_example_set");
        } else {
            return super.shouldAutoConnect(outputPort);
        }
    }

    /**
     * This is the main method of the validation chain and must be implemented to estimate a
     * performance of inner operators on the given example set. The implementation can make use of
     * the provided helper methods in this class.
     */
    public abstract void estimatePerformance(ExampleSet inputSet) throws OperatorException;

    /**
     * Returns the subprocess (or operator chain), i.e. the learning, application and evaluation operator (chain).
     *
     * @throws OperatorException
     */
    protected void executeValidator() throws OperatorException {
        getSubprocess(0).execute();
    }

    /** Can be used by subclasses to set the performance of the example set. */
    private final void setResult(PerformanceVector pv) {
        this.lastMainPerformance = Double.NaN;
        this.lastMainVariance = Double.NaN;
        this.lastMainDeviation = Double.NaN;
        this.lastFirstPerformance = Double.NaN;
        this.lastSecondPerformance = Double.NaN;
        this.lastThirdPerformance = Double.NaN;

        if (pv != null) {
            // main result
            PerformanceCriterion mainCriterion = pv.getMainCriterion();
            if (mainCriterion == null && pv.size() > 0) { // use first if no main criterion was
                // defined
                mainCriterion = pv.getCriterion(0);
            }
            if (mainCriterion != null) {
                this.lastMainPerformance = mainCriterion.getAverage();
                this.lastMainVariance = mainCriterion.getVariance();
                this.lastMainDeviation = mainCriterion.getStandardDeviation();
            }

            if (pv.size() >= 1) {
                PerformanceCriterion criterion = pv.getCriterion(0);
                if (criterion != null) {
                    this.lastFirstPerformance = criterion.getAverage();
                }
            }

            if (pv.size() >= 2) {
                PerformanceCriterion criterion = pv.getCriterion(1);
                if (criterion != null) {
                    this.lastSecondPerformance = criterion.getAverage();
                }
            }

            if (pv.size() >= 3) {
                PerformanceCriterion criterion = pv.getCriterion(2);
                if (criterion != null) {
                    this.lastThirdPerformance = criterion.getAverage();
                }
            }
        }
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet eSet = trainingSetInput.getData(ExampleSet.class);
        estimatePerformance(eSet);

        // Generate complete model
        learnFinalModel(eSet);
        getProgress().complete();
        modelOutput.deliver(trainingProcessModelInput.getData(IOObject.class));

        exampleSetOutput.deliver(eSet);

        // set last result for plotting purposes. This is an average value and
        // actually not the last performance value!
        boolean success = false;
        for (IOObject result : applyProcessPerformancePortExtender.getOutputData(IOObject.class)) {
            if (result instanceof PerformanceVector) {
                setResult((PerformanceVector) result);
                success = true;
                break;
            }
        }
        if (!success) {
            getLogger().warning("No performance vector found among averagable results. Performance will not be loggable.");
        }
    }

    /** building the final model. */
    protected void learnFinalModel(ExampleSet trainingSet) throws OperatorException {
        trainingProcessExampleSetOutput.deliver(trainingSet);
        getSubprocess(0).execute();
    }

    /**
     * Performs training, applying and evaluation operator chain
     * @param eSet
     * @throws OperatorException
     */
    protected final void validate(SplittedExampleSet eSet) throws OperatorException {
        SplittedExampleSet clonedSplittedSet = new SplittedExampleSet(eSet);
        SplittedExampleSet trainSet = clonedSplittedSet;
        trainSet.selectSingleSubset(0);
        trainingProcessExampleSetOutput.deliver(trainSet);
        trainSet = null;

        SplittedExampleSet testSet = clonedSplittedSet;
        testSet.selectSingleSubset(1);
        Attribute predictedBefore = testSet.getAttributes().getPredictedLabel();
        applyProcessExampleSetOutput.deliver(testSet);
        getSubprocess(0).execute();
        Tools.buildAverages(applyProcessPerformancePortExtender);

        Attribute predictedAfter = testSet.getAttributes().getPredictedLabel();
        // remove predicted label and confidence attributes if there is a new prediction which is
        // not equal to an old one
        if (predictedAfter != null
                && (predictedBefore == null || predictedBefore.getTableIndex() != predictedAfter.getTableIndex())) {
            PredictionModel.removePredictedLabel(testSet);
        }
    }

    /**
     * Performs training, applying and evaluation operator chain
     * @param inputSet
     * @param mapping
     * @throws OperatorException
     */
    protected final void validate(ExampleSet inputSet, int[] mapping) throws OperatorException {
        MappedExampleSet trainSet = new MappedExampleSet(inputSet, mapping, true);
        trainingProcessExampleSetOutput.deliver(trainSet);
        trainSet = null;

        MappedExampleSet inverseExampleSet = new MappedExampleSet(inputSet, mapping, false);
        Attribute predictedBefore = inverseExampleSet.getAttributes().getPredictedLabel();
        applyProcessExampleSetOutput.deliver(inverseExampleSet);
        getSubprocess(0).execute();
        Tools.buildAverages(applyProcessPerformancePortExtender);

        Attribute predictedAfter = inverseExampleSet.getAttributes().getPredictedLabel();
        // remove predicted label and confidence attributes if there is a new prediction which is
        // not equal to an old one
        if (predictedAfter != null
                && (predictedBefore == null || predictedBefore.getTableIndex() != predictedAfter.getTableIndex())) {
            PredictionModel.removePredictedLabel(inverseExampleSet);
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type = new ParameterTypeBoolean(PARAMETER_CREATE_COMPLETE_MODEL,
                "Indicates if a model of the complete data set should be additionally build after estimation.", false);
        type.setDeprecated();
        type.setExpert(false);
        types.add(type);
        return types;
    }
}
