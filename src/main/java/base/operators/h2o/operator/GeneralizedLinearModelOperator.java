package base.operators.h2o.operator;

import base.operators.example.ExampleSet;
import base.operators.h2o.ClusterManager;
import base.operators.h2o.model.GeneralizedLinearModelConverter;
import base.operators.operator.Model;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.UserError;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.ParameterTypeTupel;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.AndParameterCondition;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.EqualTypeCondition;
import base.operators.parameter.conditions.NonEqualTypeCondition;
import base.operators.parameter.conditions.OrParameterCondition;
import base.operators.parameter.conditions.ParameterCondition;
import base.operators.tools.LogService;
import base.operators.tools.Ontology;
import hex.Model.Parameters;
import hex.Model.Parameters.FoldAssignmentScheme;
import hex.glm.GLMModel;
import hex.glm.GLMModel.GLMParameters;
import hex.glm.GLMModel.GLMParameters.Family;
import hex.glm.GLMModel.GLMParameters.Link;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang.ArrayUtils;
import water.DKV;
import water.Key;
import water.Keyed;
import water.fvec.Frame;
import water.fvec.Vec;

public class GeneralizedLinearModelOperator extends AbstractLinearLearner {
    public static final String PARAMETER_LINK = "link";
    public static final String PARAMETER_BETA_CONSTRAINTS = "beta_constraints";
    public static final String PARAMETER_BETA_NAME = "names";
    public static final String PARAMETER_BETA_LOWER_BOUNDS = "lower_bounds";
    public static final String PARAMETER_BETA_UPPER_BOUNDS = "upper_bounds";
    public static final String PARAMETER_BETA_GIVEN = "beta_given";
    public static final String PARAMETER_BETA_START = "beta_start";
    private Frame betaFrame;

    public GeneralizedLinearModelOperator(OperatorDescription description) {
        super(description, true);
        this.getTransformer().addRule(new MDTransformationRule() {
            @Override
            public void transformMD() {
                MetaData md = GeneralizedLinearModelOperator.this.getExampleSetInputPort().getMetaData();
                if (md != null && md instanceof ExampleSetMetaData) {
                    ExampleSetMetaData emd = (ExampleSetMetaData)md;
                    AttributeMetaData labelMD = emd.getLabelMetaData();
                    if (labelMD != null) {
                        boolean performRegression = !labelMD.isNominal();

                        try {
                            String familyString = GeneralizedLinearModelOperator.this.getParameterAsString("family");
                            if (!"AUTO".equals(familyString)) {
                                Family family = Family.valueOf(familyString);
                                ArrayList fixes;
                                if (!Family.binomial.equals(family) && !Family.multinomial.equals(family)) {
                                    if (!performRegression) {
                                        fixes = new ArrayList();
                                        fixes.add(new ParameterSettingQuickFix(GeneralizedLinearModelOperator.this, "family", (String)null, "set_optional_parameter", new Object[]{"family"}));
                                        GeneralizedLinearModelOperator.this.addError(new SimpleProcessSetupError(Severity.ERROR, GeneralizedLinearModelOperator.this.getPortOwner(), fixes, "param_regression_only", new Object[]{family.toString(), "family"}));
                                    }
                                } else {
                                    fixes = new ArrayList();
                                    fixes.add(new ParameterSettingQuickFix(GeneralizedLinearModelOperator.this, "family", (String)null, "set_optional_parameter", new Object[]{"family"}));
                                    if (performRegression) {
                                        GeneralizedLinearModelOperator.this.addError(new SimpleProcessSetupError(Severity.ERROR, GeneralizedLinearModelOperator.this.getPortOwner(), fixes, "param_classification_only", new Object[]{family.toString(), "family"}));
                                    } else if (Family.binomial.equals(family) && !labelMD.isBinominal() && labelMD.getValueSet().size() != 2) {
                                        GeneralizedLinearModelOperator.this.addError(new SimpleProcessSetupError(Severity.WARNING, GeneralizedLinearModelOperator.this.getPortOwner(), fixes, "param_binominal_only", new Object[]{family.toString(), "family"}));
                                    }
                                }
                            }
                        } catch (UndefinedParameterError var8) {
                        }
                    }
                }

            }
        });
    }

    @Override
    public boolean supportsCapability(OperatorCapability capability) {
        switch(capability) {
            case BINOMINAL_ATTRIBUTES:
            case POLYNOMINAL_ATTRIBUTES:
            case NUMERICAL_ATTRIBUTES:
            case POLYNOMINAL_LABEL:
            case BINOMINAL_LABEL:
            case NUMERICAL_LABEL:
            case MISSING_VALUES:
            case WEIGHTED_EXAMPLES:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected Parameters buildModelSpecificParameters(ExampleSet es) throws UserError {
        GLMParameters glmParameters = new GLMParameters();

        try {
            String familyString = this.getParameterAsString("family");
            Family family;
            if ("AUTO".equals(familyString)) {
                int labelType = es.getAttributes().getLabel().getValueType();
                if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(labelType, 1)) {
                    if (labelType != 6 && es.getAttributes().getLabel().getMapping().size() != 2) {
                        family = Family.multinomial;
                    } else {
                        family = Family.binomial;
                    }
                } else {
                    family = Family.gaussian;
                }
            } else {
                family = Family.valueOf(familyString);
                if (Family.gaussian.equals(family) && es.getAttributes().getLabel().isNominal()) {
                    throw new UserError(this, "h2o.param_regression_only", new Object[]{family.toString(), "family"});
                }
            }

            glmParameters._family = family;
            Link link = null;
            switch(family) {
                case binomial:
                case multinomial:
                case tweedie:
                    link = Link.family_default;
                    break;
                default:
                    link = Link.valueOf(this.getParameterAsString("link"));
            }

            glmParameters._link = link;
            this.addCommonParameters(glmParameters, family);
            List parameterList;
            String[] vals;
            if (!Family.multinomial.equals(family) && this.getParameterAsBoolean("specify_beta_constraints") && (!this.getParameterAsBoolean("lambda_search") || !this.getParameterAsBoolean("use_regularization"))) {
                parameterList = ParameterTypeList.transformString2List(this.getParameterAsString("beta_constraints"));
                List<Double> lowerBounds = new ArrayList();
                List<Double> upperBounds = new ArrayList();
                List<Double> givenBetas = new ArrayList();
                List<Double> startBetas = new ArrayList();
                List<String> namesWithCategories = new ArrayList();
                Iterator var12 = parameterList.iterator();

                while(var12.hasNext()) {
                    vals = (String[])var12.next();
                    if (vals.length == 2) {
                        String[] names = ParameterTypeTupel.transformString2Tupel(vals[0]);
                        if (names.length == 2) {
                            namesWithCategories.add(names[0] + (names[1].isEmpty() ? "" : "." + names[1]));
                            String[] bounds = ParameterTypeTupel.transformString2Tupel(vals[1]);
                            if (bounds.length == 4) {
                                try {
                                    lowerBounds.add(Double.valueOf(bounds[0]));
                                    upperBounds.add(Double.valueOf(bounds[1]));
                                    givenBetas.add(Double.valueOf(bounds[2]));
                                    startBetas.add(Double.valueOf(bounds[3]));
                                } catch (NumberFormatException var21) {
                                    LogService.getRoot().fine("Can't parse beta constraints: " + var21.getMessage());
                                }
                            }
                        }
                    }
                }

                Vec[] vecs = new Vec[5];
                vecs[0] = Vec.makeVec(IntStream.rangeClosed(0, namesWithCategories.size() - 1).asDoubleStream().toArray(), (String[])namesWithCategories.toArray(new String[0]), Vec.newKey());
                vecs[1] = Vec.makeVec(ArrayUtils.toPrimitive((Double[])lowerBounds.toArray(new Double[0])), vecs[0].group().addVec());
                vecs[2] = Vec.makeVec(ArrayUtils.toPrimitive((Double[])upperBounds.toArray(new Double[0])), vecs[0].group().addVec());
                vecs[3] = Vec.makeVec(ArrayUtils.toPrimitive((Double[])givenBetas.toArray(new Double[0])), vecs[0].group().addVec());
                vecs[4] = Vec.makeVec(ArrayUtils.toPrimitive((Double[])startBetas.toArray(new Double[0])), vecs[0].group().addVec());
                this.betaFrame = new Frame(Key.make(ClusterManager.generateFrameName(this.getName())), new String[]{"names", "lower_bounds", "upper_bounds", "beta_given", "beta_start"}, vecs);
                DKV.put(this.betaFrame);
                glmParameters._beta_constraints = this.betaFrame._key;
            }

            parameterList = this.getParameterList("expert_parameters");
            double[] additionalAlphas = null;
            double[] additionalLambdas = null;
            Iterator var26 = parameterList.iterator();

            while(var26.hasNext()) {
                String[] kv = (String[])var26.next();
                if (kv.length == 2) {
                    String value = kv[1];

                    GeneralizedLinearModelOperator.AdvancedGLMParameter glmParam;
                    try {
                        glmParam = GeneralizedLinearModelOperator.AdvancedGLMParameter.valueOf(kv[0].toUpperCase());
                    } catch (IllegalArgumentException var20) {
                        throw new UserError(this, "h2o.wrong_advanced_key", new Object[]{kv[0]});
                    }

                    switch(glmParam) {
                        case TWEEDIE_VARIANCE_POWER:
                            glmParameters._tweedie_variance_power = this.getDoubleValue(kv[0], value);
                            break;
                        case TWEEDIE_LINK_POWER:
                            glmParameters._tweedie_link_power = this.getDoubleValue(kv[0], value);
                            break;
                        case PRIOR:
                            glmParameters._prior = this.getDoubleValue(kv[0], value);
                            break;
                        case BETA_EPSILON:
                            glmParameters._beta_epsilon = this.getDoubleValue(kv[0], value);
                            break;
                        case OBJECTIVE_EPSILON:
                            glmParameters._objective_epsilon = this.getDoubleValue(kv[0], value);
                            break;
                        case GRADIENT_EPSILON:
                            glmParameters._gradient_epsilon = this.getDoubleValue(kv[0], value);
                            break;
                        case MAX_ACTIVE_PREDICTORS:
                            glmParameters._max_active_predictors = this.getIntegerValue(kv[0], value);
                            break;
                        case OBJ_REG:
                            if (!this.isEmptyAdvancedValue(value)) {
                                glmParameters._obj_reg = this.getDoubleValue(kv[0], value);
                            }
                            break;
                        case MAX_CONFUSION_MATRIX_SIZE:
                            glmParameters._max_confusion_matrix_size = this.getIntegerValue(kv[0], value);
                            break;
                        case FOLD_ASSIGNMENT:
                            try {
                                FoldAssignmentScheme scheme = FoldAssignmentScheme.valueOf(value);
                                glmParameters._fold_assignment = scheme;
                                break;
                            } catch (IllegalArgumentException var19) {
                                throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, kv[0]});
                            }
                        case FOLD_COLUMN:
                            if (!this.isEmptyAdvancedValue(value)) {
                                glmParameters._fold_column = value;
                            }
                            break;
                        case OFFSET_COLUMN:
                            if (!this.isEmptyAdvancedValue(value)) {
                                glmParameters._offset_column = value;
                            }
                            break;
                        case SCORE_EACH_ITERATION:
                            glmParameters._score_each_iteration = this.getBooleanValue(kv[0], value);
                            break;
                        case KEEP_CROSS_VALIDATION_PREDICTIONS:
                            glmParameters._keep_cross_validation_predictions = this.getBooleanValue(kv[0], value);
                            break;
                        case KEEP_CROSS_VALIDATION_FOLD_ASSIGNMENT:
                            glmParameters._keep_cross_validation_fold_assignment = this.getBooleanValue(kv[0], value);
                            break;
                        case ADDITIONAL_ALPHAS:
                            if (!this.isEmptyAdvancedValue(value)) {
                                vals = value.split(",");
                                if (vals.length > 0) {
                                    try {
                                        additionalAlphas = Stream.of(vals).mapToDouble(Double::valueOf).toArray();
                                    } catch (NumberFormatException var18) {
                                        throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, kv[0]});
                                    }
                                }
                            }
                            break;
                        case ADDITIONAL_LAMBDAS:
                            if (!this.isEmptyAdvancedValue(value)) {
                                vals = value.split(",");
                                if (vals.length > 0) {
                                    try {
                                        additionalLambdas = Stream.of(vals).mapToDouble(Double::valueOf).toArray();
                                    } catch (NumberFormatException var17) {
                                        throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, kv[0]});
                                    }
                                }
                            }
                            break;
                        case NFOLDS:
                            glmParameters._nfolds = this.getIntegerValue(kv[0], value);
                            break;
                        default:
                            LogService.getRoot().warning("The invalid parameter '" + kv[0] + "' is skipped.");
                    }
                }
            }

            Double lambda = this.getLambda();
            Double alpha = this.getAlpha(lambda);
            double[] lambdas;
            int i;
            if (alpha != null) {
                if (additionalAlphas != null) {
                    lambdas = new double[1 + additionalAlphas.length];
                    lambdas[0] = alpha;

                    for(i = 0; i < additionalAlphas.length; ++i) {
                        lambdas[i + 1] = additionalAlphas[i];
                    }

                    glmParameters._alpha = lambdas;
                } else {
                    glmParameters._alpha = new double[]{alpha};
                }
            }

            if (lambda != null) {
                if (additionalLambdas != null) {
                    lambdas = new double[1 + additionalLambdas.length];
                    lambdas[0] = lambda;

                    for(i = 0; i < additionalLambdas.length; ++i) {
                        lambdas[i + 1] = additionalLambdas[i];
                    }

                    glmParameters._lambda = lambdas;
                } else {
                    glmParameters._lambda = new double[]{lambda};
                }
            }

            return glmParameters;
        } catch (UndefinedParameterError var22) {
            throw new RuntimeException(var22);
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        if (this.betaFrame != null) {
            ClusterManager.removeFromCluster(new Keyed[]{this.betaFrame});
        }

    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new ArrayList();
        List<Family> familyValues = new ArrayList(Arrays.asList(Family.values()));
        List<String> extendedFamilyValues = new ArrayList();
        extendedFamilyValues.add("AUTO");
        extendedFamilyValues.addAll((Collection)familyValues.stream().map(Enum::name).collect(Collectors.toList()));
        types.add(new ParameterTypeCategory("family", "Family. Use binomial for classification with logistic regression, others are for regression problems.", (String[])extendedFamilyValues.toArray(new String[0]), 0, false));
        ParameterType type = new ParameterTypeCategory("link", "The link function relates the linear predictor to the distribution function.  The default is the canonical link for the specified family. ", (String[])Stream.of(Link.values()).filter((link) -> {
            return !Link.tweedie.equals(link) && !Link.multinomial.equals(link) && !Link.logit.equals(link);
        }).map(Enum::toString).toArray((x$0) -> {
            return new String[x$0];
        }), Link.family_default.ordinal(), false);
        type.registerDependencyCondition(new EqualTypeCondition(this, "family", (String[])extendedFamilyValues.toArray(new String[0]), false, new int[]{extendedFamilyValues.indexOf(Family.gaussian.toString()), extendedFamilyValues.indexOf(Family.gamma.toString()), extendedFamilyValues.indexOf(Family.poisson.toString())}));
        types.add(type);
        types.addAll(this.getCommonParameterTypes(extendedFamilyValues));
        type = new ParameterTypeBoolean("specify_beta_constraints", "If enabled, beta constraints for the regular attributes can be provided.", false, true);
        type.registerDependencyCondition(new OrParameterCondition(this, false, new ParameterCondition[]{new BooleanParameterCondition(this, "use_regularization", false, false), new AndParameterCondition(this, false, new ParameterCondition[]{new BooleanParameterCondition(this, "use_regularization", false, true), new BooleanParameterCondition(this, "lambda_search", false, false)})}));
        if (extendedFamilyValues != null) {
            type.registerDependencyCondition(new NonEqualTypeCondition(this, "family", (String[])extendedFamilyValues.toArray(new String[0]), false, new int[]{extendedFamilyValues.indexOf(Family.multinomial.toString())}));
        }

        types.add(type);
        ParameterType attrType = new ParameterTypeAttribute("Attribute name", "Make constraint for this attribute.", this.getExampleSetInputPort()) {
            private static final long serialVersionUID = 6299169751638627561L;

            @Override
            protected boolean isFilteredOut(AttributeMetaData amd) {
                return amd.isSpecial();
            }
        };
        ParameterType nominalValueType = new ParameterTypeString("Category", "Value from the attribute's domain (if nominal)", true);
        ParameterType lower = new ParameterTypeDouble("lower_bounds", "The lower bound for the beta constraint.", -1.7976931348623157E308D, 1.7976931348623157E308D, 0.0D);
        ParameterType upper = new ParameterTypeDouble("upper_bounds", "The upper bound for the beta constraint.", -1.7976931348623157E308D, 1.7976931348623157E308D, 0.0D);
        ParameterType givenVal = new ParameterTypeDouble("beta_given", "The user-specified starting value for the beta.", -1.7976931348623157E308D, 1.7976931348623157E308D, 0.0D);
        ParameterType startVal = new ParameterTypeDouble("beta_start", "The user-specified starting value for the beta.", -1.7976931348623157E308D, 1.7976931348623157E308D, 0.0D);
        type = new ParameterTypeList("beta_constraints", "Beta constraints", new ParameterTypeTupel("Names", "Make constraint for the given category of the given attribute.", new ParameterType[]{attrType, nominalValueType}), new ParameterTypeTupel("Constraints", "Constraints for the attribute's beta.", new ParameterType[]{lower, upper, givenVal, startVal}));
        type.registerDependencyCondition(new BooleanParameterCondition(this, "specify_beta_constraints", false, true));
        types.add(type);
        types.addAll(super.getParameterTypes());
        return types;
    }

    @Override
    protected void performAdditionalChecks() {
        super.performAdditionalChecks();

        try {
            String familyString = this.getParameterAsString("family");
            Link link = Link.valueOf(this.getParameterAsString("link"));
            ArrayList fixes;
            if (!"AUTO".equals(familyString) && !Link.family_default.equals(link)) {
                Family family = Family.valueOf(familyString);
                switch(family) {
                    case poisson:
                        if (!link.equals(Link.identity) && !link.equals(Link.log)) {
                            fixes = new ArrayList();
                            fixes.add(new ParameterSettingQuickFix(this, "link", (String)null, "set_optional_parameter", new Object[]{"link"}));
                            fixes.add(new ParameterSettingQuickFix(this, "family", (String)null, "set_optional_parameter", new Object[]{"family"}));
                            this.addError(new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(), fixes, "glm_incompatible_link", new Object[]{family.toString(), link.toString()}));
                        }
                }
            }

            List<String[]> parameterList = this.getParameterList("expert_parameters");
            fixes = new ArrayList();
            fixes.add(new ParameterSettingQuickFix(this, "expert_parameters", (String)null, "set_optional_parameter", new Object[]{"expert_parameters"}));
            Iterator var5 = parameterList.iterator();

            while(true) {
                String[] kv;
                do {
                    if (!var5.hasNext()) {
                        return;
                    }

                    kv = (String[])var5.next();
                } while(kv.length != 2);

                try {
                    GeneralizedLinearModelOperator.AdvancedGLMParameter paramKey = GeneralizedLinearModelOperator.AdvancedGLMParameter.valueOf(kv[0].toUpperCase());
                    if (GeneralizedLinearModelOperator.AdvancedGLMParameter.PRIOR.equals(paramKey)) {
                        try {
                            double prior = Double.valueOf(kv[1]);
                            if ((prior >= 1.0D || prior <= 0.0D) && prior != -1.0D) {
                                this.addError(new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(), fixes, "param_not_in_range", new Object[]{kv[0], "(0,1) exclusive", prior}));
                            }
                        } catch (NumberFormatException var13) {
                        }
                    }

                    if ((GeneralizedLinearModelOperator.AdvancedGLMParameter.TWEEDIE_LINK_POWER.equals(paramKey) && !GeneralizedLinearModelOperator.AdvancedGLMParameter.TWEEDIE_LINK_POWER.isDefault(kv[1]) || GeneralizedLinearModelOperator.AdvancedGLMParameter.TWEEDIE_VARIANCE_POWER.equals(paramKey) && !GeneralizedLinearModelOperator.AdvancedGLMParameter.TWEEDIE_VARIANCE_POWER.isDefault(kv[1])) && (familyString.equals("AUTO") || !Family.tweedie.equals(Family.valueOf(familyString)))) {
                        List<QuickFix> tweedieFixes = new ArrayList(fixes);
                        tweedieFixes.add(new ParameterSettingQuickFix(this, "family", (String)null, "set_optional_parameter", new Object[]{"family"}));
                        this.addError(new SimpleProcessSetupError(Severity.WARNING, this.getPortOwner(), tweedieFixes, "incompatible_parameters", new Object[]{paramKey.toString().toLowerCase() + " will be used only if Family=tweedie"}));
                    }

                    boolean useRegularization = this.getParameterAsBoolean("use_regularization");
                    boolean undefinedLambda;
                    if (GeneralizedLinearModelOperator.AdvancedGLMParameter.ADDITIONAL_ALPHAS.equals(paramKey) && !this.isEmptyAdvancedValue(kv[1])) {
                        undefinedLambda = false;

                        try {
                            if (useRegularization) {
                                this.getParameterAsDouble("alpha");
                            }
                        } catch (UndefinedParameterError var12) {
                            undefinedLambda = true;
                        }

                        if (!useRegularization || undefinedLambda) {
                            this.addError(new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(), fixes, "incompatible_parameters", new Object[]{paramKey.toString().toLowerCase() + " is ignored in the current parameter combination"}));
                        }
                    }

                    if (GeneralizedLinearModelOperator.AdvancedGLMParameter.ADDITIONAL_LAMBDAS.equals(paramKey) && !this.isEmptyAdvancedValue(kv[1])) {
                        undefinedLambda = false;

                        try {
                            if (useRegularization) {
                                this.getParameterAsDouble("lambda");
                            }
                        } catch (UndefinedParameterError var11) {
                            undefinedLambda = true;
                        }

                        if (!useRegularization || undefinedLambda) {
                            this.addError(new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(), fixes, "incompatible_parameters", new Object[]{paramKey.toString().toLowerCase() + " is ignored in the current parameter combination"}));
                        }
                    }

                    if (GeneralizedLinearModelOperator.AdvancedGLMParameter.NFOLDS.equals(paramKey) && Integer.parseInt(kv[1]) == 1) {
                        this.addError(new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(), fixes, "invalid_parameter", new Object[]{1, GeneralizedLinearModelOperator.AdvancedGLMParameter.NFOLDS.toString(), "nfolds must be either 0 or >1"}));
                    }
                } catch (IllegalArgumentException var14) {
                    this.addError(new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(), fixes, "invalid_param_key", new Object[]{kv[0]}));
                }
            }
        } catch (UndefinedParameterError var15) {
        }
    }

    @Override
    public Model createModel(ExampleSet es, hex.Model<?, ?, ?> model, boolean useDefaultThreshold) throws OperatorException {
        return GeneralizedLinearModelConverter.convert(es, (GLMModel)model, useDefaultThreshold);
    }

    @Override
    protected String[] getAdvancedParametersArray() {
        return GeneralizedLinearModelOperator.AdvancedGLMParameter.lowerCaseValues();
    }

    public static enum AdvancedGLMParameter {
        TWEEDIE_VARIANCE_POWER("0"),
        TWEEDIE_LINK_POWER("1"),
        PRIOR("-1"),
        SCORE_EACH_ITERATION("false"),
        FOLD_ASSIGNMENT("AUTO"),
        FOLD_COLUMN(" "),
        OFFSET_COLUMN(" "),
        MAX_CONFUSION_MATRIX_SIZE("20"),
        KEEP_CROSS_VALIDATION_PREDICTIONS("false"),
        KEEP_CROSS_VALIDATION_FOLD_ASSIGNMENT("false"),
        BETA_EPSILON("0.0001"),
        OBJECTIVE_EPSILON("-1"),
        GRADIENT_EPSILON("0.0001"),
        MAX_ACTIVE_PREDICTORS("-1"),
        OBJ_REG(" "),
        ADDITIONAL_ALPHAS(" "),
        ADDITIONAL_LAMBDAS(" "),
        NFOLDS("0");

        private String defaultValue;

        private AdvancedGLMParameter(String defaultVal) {
            this.defaultValue = defaultVal;
        }

        public String getDefaultValue() {
            return this.defaultValue;
        }

        public boolean isDefault(String def) {
            return this.defaultValue.equals(def);
        }

        public static String[] lowerCaseValues() {
            return (String[])Stream.of(values()).map((val) -> {
                return val.name().toLowerCase();
            }).toArray((x$0) -> {
                return new String[x$0];
            });
        }

        public static String[] getDefaultValues() {
            return (String[])Stream.of(values()).map((val) -> {
                return val.getDefaultValue();
            }).toArray((x$0) -> {
                return new String[x$0];
            });
        }
    }
}
