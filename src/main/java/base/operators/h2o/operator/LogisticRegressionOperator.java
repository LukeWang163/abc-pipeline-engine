package base.operators.h2o.operator;

import base.operators.example.ExampleSet;
import base.operators.h2o.model.LogisticRegressionModelConverter;
import base.operators.operator.Model;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.BelowOrEqualOperatorVersionCondition;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.OrParameterCondition;
import base.operators.parameter.conditions.ParameterCondition;
import hex.Model.Parameters;
import hex.glm.GLMModel;
import hex.glm.GLMModel.GLMParameters;
import hex.glm.GLMModel.GLMParameters.Family;
import hex.glm.GLMModel.GLMParameters.Link;
import java.util.List;

public class LogisticRegressionOperator extends AbstractLinearLearner {
    private final Family logregFamily;
    private final Link logregLink;

    public LogisticRegressionOperator(OperatorDescription description) {
        super(description, false);
        this.logregFamily = Family.binomial;
        this.logregLink = Link.logit;
    }

    @Override
    public boolean supportsCapability(OperatorCapability capability) {
        switch(capability) {
            case NUMERICAL_ATTRIBUTES:
            case BINOMINAL_ATTRIBUTES:
            case POLYNOMINAL_ATTRIBUTES:
            case BINOMINAL_LABEL:
            case MISSING_VALUES:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected Parameters buildModelSpecificParameters(ExampleSet es) throws UndefinedParameterError, UserError {
        GLMParameters glmParameters = new GLMParameters();

        try {
            glmParameters._family = this.logregFamily;
            glmParameters._link = this.logregLink;
            this.addCommonParameters(glmParameters, this.logregFamily);
            Double lambda = this.getLambda();
            if (lambda != null) {
                glmParameters._lambda = new double[]{lambda};
            }

            Double alpha = this.getAlpha(lambda);
            if (alpha != null) {
                glmParameters._alpha = new double[]{alpha};
            }

            return glmParameters;
        } catch (UndefinedParameterError var5) {
            throw new RuntimeException(var5);
        }
    }

    @Override
    public Model createModel(ExampleSet es, hex.Model<?, ?, ?> model, boolean useDefaultThreshold) throws OperatorException {
        return LogisticRegressionModelConverter.convert(es, (GLMModel)model, useDefaultThreshold);
    }

    @Override
    protected String[] getAdvancedParametersArray() {
        return null;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = this.getCommonParameterTypes((List)null);
        ParameterType type = new ParameterTypeInt("max_runtime_seconds", "Maximum allowed runtime in seconds for model training. Use 0 to disable.", 0, 2147483647, 0, true);
        type.registerDependencyCondition(new OrParameterCondition(this, false, new ParameterCondition[]{new BelowOrEqualOperatorVersionCondition(this, NTHREAD_REBALANCING_MAXRUNTIME_VERSION), new BooleanParameterCondition(this, "reproducible", false, false)}));
        types.add(type);
        ParameterType useRegularization = (ParameterType)types.stream().filter((p) -> {
            return "use_regularization".equals(p.getKey());
        }).findFirst().get();
        useRegularization.setDefaultValue(Boolean.FALSE);
        ParameterType computePValues = (ParameterType)types.stream().filter((p) -> {
            return "compute_p-values".equals(p.getKey());
        }).findFirst().get();
        computePValues.setDefaultValue(Boolean.TRUE);
        ParameterType removeCollinear = (ParameterType)types.stream().filter((p) -> {
            return "remove_collinear_columns".equals(p.getKey());
        }).findFirst().get();
        removeCollinear.setDefaultValue(Boolean.TRUE);
        return types;
    }
}
