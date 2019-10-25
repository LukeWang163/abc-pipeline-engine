package base.operators.operator;

import base.operators.example.ExampleSet;
import base.operators.operator.learner.PredictionModel;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.tools.CostMatrix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeEnumeration;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.ParameterTypeMatrix;
import base.operators.parameter.ParameterTypeString;
import base.operators.tools.Tools;
import java.util.Arrays;
import java.util.List;

public class CostSensitiveScoring extends Operator {
    public static final String PARAMETER_CLASSES = "classes";
    public static final String PARAMETER_COST_MATRIX = "cost_matrix";
    public static final String PARAMETER_NUMBER_OF_VARIANTS = "number_of_variants";
    private InputPort modelInput = this.getInputPorts().createPort("model", PredictionModel.class);
    private InputPort exampleSetInput = this.getInputPorts().createPort("training set", ExampleSet.class);
    private OutputPort modelOutput = (OutputPort)this.getOutputPorts().createPort("model");
    private OutputPort exampleSetOutput = (OutputPort)this.getOutputPorts().createPort("training set");

    public CostSensitiveScoring(OperatorDescription description) {
        super(description);
        this.getTransformer().addGenerationRule(this.modelOutput, CostSensitiveModel.class);
        this.getTransformer().addPassThroughRule(this.exampleSetInput, this.exampleSetOutput);
    }

    @Override
    public void doWork() throws OperatorException {
        PredictionModel model = (PredictionModel)this.modelInput.getData(PredictionModel.class);
        ExampleSet exampleSet = (ExampleSet)this.exampleSetInput.getData(ExampleSet.class);
        String[] classNames = ParameterTypeEnumeration.transformString2Enumeration(this.getParameterAsString("classes"));
        double[][] costMatrixValues = this.getParameterAsMatrix("cost_matrix");
        if (classNames.length != costMatrixValues.length) {
            throw new OperatorException("Class names do not match cost matrix.");
        } else {
            CostMatrix costMatrix = new CostMatrix(Arrays.asList(classNames));

            int p;
            for(p = 0; p < classNames.length; ++p) {
                for(int t = 0; t < classNames.length; ++t) {
                    String predictedClass = classNames[p];
                    String trueClass = classNames[t];
                    if (Tools.isZero(costMatrixValues[p][t])) {
                        costMatrix.setCost(predictedClass, trueClass, 0.0D);
                    } else {
                        costMatrix.setCost(predictedClass, trueClass, -1.0D * costMatrixValues[p][t]);
                    }
                }
            }

            p = this.getParameterAsInt("number_of_variants");
            CostSensitiveModel costSensitiveModel = new CostSensitiveModel(exampleSet, model, costMatrix, p);
            this.modelOutput.deliver(costSensitiveModel);
            this.exampleSetOutput.deliver(exampleSet);
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type = new ParameterTypeEnumeration("classes", "The classes for this cost matrix.", new ParameterTypeString("classes", "The classes of this cost matrix.", false));
        type.setOptional(false);
        types.add(type);
        type = new ParameterTypeMatrix("cost_matrix", "The cost matrix for wrong and correct predictions. Costs should be positive numbers.", "Cost Matrix", "Predicted Class", "True Class", true, false);
        types.add(type);
        type = new ParameterTypeInt("number_of_variants", "The number of variants for each data point.", 0, 2147483647, 10);
        types.add(type);
        return types;
    }
}
