package base.operators.h2o.model;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.set.HeaderExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;


public class LogisticRegressionModel extends GeneralizedLinearModel {
    private static final long serialVersionUID = -7168682331856782669L;

    public LogisticRegressionModel(ExampleSet trainingExampleSet, H2ONativeModelObject h2oNativeModel, boolean multinomialModel, String[] coefficientNames, double[] coefficients, double[] stdCoefficients, double[] zValues, double[] pValues, double[] stdError, double[][] multinominalCoefficients, double[][] multinominalStdCoefficients, String[] domain, String modelString, String[] warnings, GeneralizedLinearModel.GLMScore glmScore) throws OperatorException {
        super(trainingExampleSet, h2oNativeModel, multinomialModel, coefficientNames, coefficients, stdCoefficients, zValues, pValues, stdError, multinominalCoefficients, multinominalStdCoefficients, domain, modelString, warnings, glmScore);
    }

    @Override
    public ExampleSet performCustomPrediction(ExampleSet exampleSet, Attribute predictedLabel, Operator operator, HeaderExampleSet trainingHeader) throws OperatorException {
        return super.performCustomPrediction(exampleSet, predictedLabel, operator, trainingHeader);
    }
}
