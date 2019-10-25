package base.operators.h2o.model;

import base.operators.example.ExampleSet;
import base.operators.operator.OperatorException;
import hex.glm.GLMModel;

public class LogisticRegressionModelConverter {
    public static LogisticRegressionModel convert(ExampleSet trainingExampleSet, GLMModel glmModel, boolean useDefaultThreshold) throws OperatorException {
        GeneralizedLinearModelConverter.BasicTable basicTable = GeneralizedLinearModelConverter.convertToBasicTable(glmModel);
        GeneralizedLinearModel.GLMScore glmScore = GeneralizedLinearModelConverter.convertToGLMScore(glmModel, useDefaultThreshold);
        return new LogisticRegressionModel(trainingExampleSet, new H2ONativeModelObject(glmModel), basicTable.multinomialModel, basicTable.coefficientNames, basicTable.coefficients, basicTable.stdCoefficients, basicTable.zValues, basicTable.pValues, basicTable.stdError, basicTable.multinominalCoefficients, basicTable.multinominalStdCoefficients, basicTable.domain, basicTable.modelString, glmModel._warnings, glmScore);
    }
}

