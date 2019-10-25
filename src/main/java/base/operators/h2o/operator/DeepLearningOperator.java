package base.operators.h2o.operator;

import base.operators.example.ExampleSet;
import base.operators.h2o.ClusterManager;
import base.operators.h2o.model.DeepLearningModelConverter;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.UserError;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeEnumeration;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeLong;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.ParameterTypeStringCategory;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.AboveOperatorVersionCondition;
import base.operators.parameter.conditions.BelowOrEqualOperatorVersionCondition;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.EqualTypeCondition;
import base.operators.tools.LogService;
import base.operators.tools.RandomGenerator;
import hex.Model;
import hex.ModelBuilder;
import hex.Distribution.Family;
import hex.Model.Parameters;
import hex.Model.Parameters.FoldAssignmentScheme;
import hex.ModelBuilder.ValidationMessage;
import hex.ScoreKeeper.StoppingMetric;
import hex.deeplearning.DeepLearning;
import hex.deeplearning.DeepLearningModel;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters.Activation;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters.ClassSamplingMethod;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters.InitialWeightDistribution;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters.Loss;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters.MissingValuesHandling;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import water.Key;

public class DeepLearningOperator extends H2OLearner {
    public static final String VALIDATION_MESSAGE_POSTFIX = "For more information visit:";
    public static final String VALIDATION_MESSAGE_ERR_PREFIX = "ERRR on field:";
    public static final String VALIDATION_MESSAGE_WARN_PREFIX = "WARN on field:";
    public static final String PARAMETER_ACTIVATION = "activation";
    public static final String PARAMETER_HIDDEN_DROPOUT_RATIOS = "hidden_dropout_ratios";
    public static final String PARAMETER_HIDDEN = "hidden_layer_sizes";
    public static final int HIDDEN_DEFAULT = 50;
    public static final String PARAMETER_EPOCHS = "epochs";
    public static final String PARAMETER_VARIABLE_IMPORTANCES = "compute_variable_importances";
    public static final String PARAMETER_TRAIN_SAMPLES_PER_ITERATION = "train_samples_per_iteration";
    public static final String PARAMETER_ADAPTIVE_RATE = "adaptive_rate";
    public static final String PARAMETER_RHO = "rho";
    public static final String PARAMETER_EPSILON = "epsilon";
    public static final String PARAMETER_RATE = "learning_rate";
    public static final String PARAMETER_RATE_ANNEALING = "learning_rate_annealing";
    public static final String PARAMETER_RATE_DECAY = "learning_rate_decay";
    public static final String PARAMETER_MOMENTUM_START = "momentum_start";
    public static final String PARAMETER_MOMENTUM_RAMP = "momentum_ramp";
    public static final String PARAMETER_MOMENTUM_STABLE = "momentum_stable";
    public static final String PARAMETER_NESTEROV_ACCELERATED_GRADIENT = "nesterov_accelerated_gradient";
    public static final String PARAMETER_L1 = "L1";
    public static final String PARAMETER_L2 = "L2";
    public static final String PARAMETER_MAX_W2 = "max_w2";
    public static final String PARAMETER_STANDARDIZE = "standardize";
    public static final String PARAMETER_LOSS = "loss_function";
    public static final String PARAMETER_DISTRIBUTION = "distribution_function";
    public static final String PARAMETER_EARLY_STOPPING = "early_stopping";
    public static final String PARAMETER_STOPPING_ROUNDS = "stopping_rounds";
    public static final String PARAMETER_STOPPING_METRIC = "stopping_metric";
    public static final String PARAMETER_STOPPING_TOLERANCE = "stopping_tolerance";
    public static final String PARAMETER_MISSING_VALUES_HANDLING = "missing_values_handling";
    public static final String PARAMETER_REPRODUCIBLE = "reproducible_(uses_1_thread)";
    public static final String PARAMETER_ADVANCED_NEW = "expert_parameters_";
    private static final OperatorVersion MAX_W2_EXPERT_VERSION;

    public DeepLearningOperator(OperatorDescription description) {
        super(description, "reproducible_(uses_1_thread)");
        this.getTransformer().addRule(new MDTransformationRule() {
            @Override
            public void transformMD() {
                MetaData md = DeepLearningOperator.this.getExampleSetInputPort().getMetaData();
                if (md != null && md instanceof ExampleSetMetaData) {
                    ExampleSetMetaData esmd = (ExampleSetMetaData)md;
                    AttributeMetaData labelMD = esmd.getAttributeByRole("label");
                    if (labelMD == null) {
                        return;
                    }

                    try {
                        if (DeepLearningOperator.this.getParameterAsBoolean("reproducible_(uses_1_thread)") && !DeepLearningOperator.this.getParameterAsBoolean("use_local_random_seed") && DeepLearningOperator.this.getProcess().getRootOperator().getParameterAsInt("random_seed") == -1) {
                            List<QuickFix> fixes = new ArrayList();
                            fixes.add(new ParameterSettingQuickFix(DeepLearningOperator.this.getProcess().getRootOperator(), "random_seed", (String)null, "set_optional_parameter", new Object[]{"random_seed"}));
                            fixes.add(new ParameterSettingQuickFix(DeepLearningOperator.this, "use_local_random_seed", "true"));
                            fixes.add(new ParameterSettingQuickFix(DeepLearningOperator.this, "reproducible_(uses_1_thread)", "false"));
                            DeepLearningOperator.this.addError(new SimpleProcessSetupError(Severity.WARNING, DeepLearningOperator.this.getPortOwner(), fixes, "param_reproducible_without_seed", new Object[0]));
                        }
                    } catch (UndefinedParameterError var5) {
                    }

                    this.checkParametersPrivileged(esmd, labelMD);
                }

            }

            private void checkParametersPrivileged(final ExampleSetMetaData esmd, final AttributeMetaData labelMD) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        checkParameters(esmd, labelMD);
                        return null;
                    }
                });
            }

            private void checkParameters(final ExampleSetMetaData esmd, AttributeMetaData labelMD) {
                ClusterManager.startCluster();
                DeepLearningParameters params = null;

                try {
                    params = (DeepLearningParameters)DeepLearningOperator.this.buildModelSpecificParameters((ExampleSet)null);
                    DeepLearningOperator.this.buildGenericModelParameters(params);
                } catch (UserError var7) {
                    DeepLearningOperator.this.addError(new SimpleProcessSetupError(Severity.ERROR, DeepLearningOperator.this.getPortOwner(), new ArrayList(), "build_parameters", new Object[]{var7.getMessage()}));
                    return;
                }

                params._response_column = labelMD.getName();
                if (esmd.getAttributeByRole("weight") != null) {
                    params._weights_column = esmd.getAttributeByRole("weight").getName();
                }

                final int nClasses = labelMD.isNumerical() ? 1 : Math.max(labelMD.getValueSet().size(), 2);
                DeepLearning dl = new DeepLearning(params) {
                    @Override
                    public void init(boolean expensive) {
                        this._nclass = nClasses;
                        super.init(expensive);
                        MDInteger mdSize = esmd.getNumberOfExamples();
                        int size = 2147483647;
                        if (mdSize.isKnown()) {
                            size = (Integer)mdSize.getValue();
                        }

                        DeepLearningOperator.this.runModelBuilderValidations(this, size, nClasses);
                    }
                };
                ValidationMessage[] msgs = dl._messages;
                Stream.of(msgs).filter((msg) -> {
                    return msg.log_level() <= 2;
                }).forEach((msg) -> {
                    String message = msg.toString();
                    if (message.contains("For more information visit:")) {
                        message = message.substring(0, message.indexOf("For more information visit:"));
                    }

                    if (message.startsWith("ERRR on field:")) {
                        message = message.substring("ERRR on field:".length());
                    } else if (message.startsWith("WARN on field:")) {
                        message = message.substring("WARN on field:".length());
                    }

                    DeepLearningOperator.this.addError(new SimpleProcessSetupError(msg.log_level() == 2 ? Severity.WARNING : Severity.ERROR, DeepLearningOperator.this.getPortOwner(), new ArrayList(), "validation", new Object[]{message}));
                });
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
    protected ModelBuilder<? extends Model<?, ?, ?>, ?, ?> createModelBuilder(Parameters params, String modelName) {
        return new DeepLearning((DeepLearningParameters)params, Key.make(modelName));
    }

    @Override
    public base.operators.operator.Model createModel(ExampleSet es, Model<?, ?, ?> model) throws OperatorException {
        return DeepLearningModelConverter.convert(es, (DeepLearningModel)model);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new ArrayList();
        String[] activationOptions = (String[])Stream.of(Activation.values()).map((val) -> {
            return val.toString();
        }).toArray((x$0) -> {
            return new String[x$0];
        });
        types.add(new ParameterTypeCategory("activation", "The activation function (non-linearity) to be used the neurons in the hidden layers.\nTanh: Hyperbolic tangent function (same as scaled and shifted sigmoid).\nRectifier: Chooses the maximum of (0, x) where x is the input value.\nMaxout: Choose the maximum coordinate of the input vector.\nWith Dropout: Zero out a random user-given fraction of the\nincoming weights to each hidden layer during training, for each\ntraining row. This effectively trains exponentially many models at\nonce, and can improve generalization.", activationOptions, Activation.Rectifier.ordinal(), false));
        ParameterType type = new ParameterTypeEnumeration("hidden_layer_sizes", "Describes the size of all hidden layers.", new ParameterTypeInt("hidden_layer_sizes", "The size of the hidden layer.", 0, 2147483647, 50)) {
            private static final long serialVersionUID = -1308246374575771174L;

            @Override
            public Element getXML(String key, String value, boolean hideDefault, Document doc) {
                Element element = doc.createElement("enumeration");
                element.setAttribute("key", key);
                String[] list = null;
                if (value != null) {
                    list = transformString2Enumeration(value);
                } else {
                    list = transformString2Enumeration((String)this.getDefaultValue());
                }

                if (list != null) {
                    String[] var7 = list;
                    int var8 = list.length;

                    for(int var9 = 0; var9 < var8; ++var9) {
                        String string = var7[var9];
                        element.appendChild(this.getValueType().getXML(this.getValueType().getKey(), string, false, doc));
                    }
                }

                return element;
            }
        };
        type.setExpert(false);
        type.setDefaultValue(ParameterTypeEnumeration.transformEnumeration2String(Arrays.asList(String.valueOf(50), String.valueOf(50))));
        types.add(type);
        type = new ParameterTypeEnumeration("hidden_dropout_ratios", "A fraction of the inputs for each hidden layer to be omitted from training in order to improve generalization. Defaults to 0.5 for each hidden layer if omitted.", new ParameterTypeDouble("hidden_dropout_ratio", "The dropout ratio of the hidden layer.", 0.0D, 1.0D, 0.5D));
        type.registerDependencyCondition(new EqualTypeCondition(this, "activation", activationOptions, false, new int[]{Activation.RectifierWithDropout.ordinal(), Activation.ExpRectifierWithDropout.ordinal(), Activation.MaxoutWithDropout.ordinal(), Activation.TanhWithDropout.ordinal()}));
        type.setExpert(true);
        types.add(type);
        types.add(new ParameterTypeBoolean("reproducible_(uses_1_thread)", "Force reproducibility on small data (WARNING: will be slow - only uses 1 thread).", false, false));
        List<ParameterType> randomTypes = RandomGenerator.getRandomGeneratorParameters(this);
        Iterator var5 = randomTypes.iterator();

        while(var5.hasNext()) {
            ParameterType pt = (ParameterType)var5.next();
            if ("use_local_random_seed".equals(pt.getKey())) {
                pt.registerDependencyCondition(new BooleanParameterCondition(this, "reproducible_(uses_1_thread)", false, true));
            }
        }

        types.addAll(randomTypes);
        type = new ParameterTypeDouble("epochs", "How many times the dataset should be iterated (streamed), can be fractional", 0.0D, 1.7976931348623157E308D, 10.0D);
        type.setExpert(false);
        types.add(type);
        types.add(new ParameterTypeBoolean("compute_variable_importances", "Compute variable importances for input features (Gedeon method) - can be slow for large networks", false, false));
        types.add(new ParameterTypeLong("train_samples_per_iteration", "The number of training data rows to be processed per iteration. Note that independent of this parameter, each row is used immediately to update the model with (online) stochastic gradient descent. This parameter controls the frequency at which scoring and model cancellation can happen. Special values are 0 for one epoch per iteration, -1 for processing the maximum amount of data per iteration. Special value of -2 turns on automatic mode (auto-tuning).", -2L, 9223372036854775807L, -2L, true));
        types.add(new ParameterTypeBoolean("adaptive_rate", "Adaptive learning rate", true, true));
        type = new ParameterTypeDouble("epsilon", "The optimization is stopped if the training error gets below this epsilon value.", 0.0D, 1.0D / 0.0, 1.0E-8D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "adaptive_rate", false, true));
        types.add(type);
        type = new ParameterTypeDouble("rho", "It is similar to momentum and relates to the memory to prior weight updates.\nTypical values are between 0.9 and 0.999.", 0.0D, 1.0D / 0.0, 0.99D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "adaptive_rate", false, true));
        types.add(type);
        type = new ParameterTypeDouble("learning_rate", "The learning rate, alpha. Higher values lead to less stable models, while lower values lead to slower convergence. Default is 0.005", 0.0D, 1.0D, 0.005D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "adaptive_rate", false, false));
        types.add(type);
        type = new ParameterTypeDouble("learning_rate_annealing", "Learning rate annealing reduces the learning rate to \"freeze\" into local minima in the optimization landscape. The annealing rate is the inverse of the number of training samples it takes to cut the learning rate in half (e.g., 1e-6 means that it takes 1e6 training samples to halve the learning rate). This parameter is only active if adaptive learning rate is disabled.", 0.0D, 1.0D, 1.0E-6D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "adaptive_rate", false, false));
        types.add(type);
        type = new ParameterTypeDouble("learning_rate_decay", "The learning rate decay parameter controls the change of learning rate across layers. For example, assume the rate parameter is set to 0.01, and the rate_decay parameter is set to 0.5. Then the learning rate for the weights connecting the input and first hidden layer will be 0.01, the learning rate for the weights connecting the first and the second hidden layer will be 0.005, and the learning rate for the weights connecting the second and third hidden layer will be 0.0025, etc. This parameter is only active if adaptive learning rate is disabled.", 0.0D, 1.0D, 1.0D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "adaptive_rate", false, false));
        types.add(type);
        type = new ParameterTypeDouble("momentum_start", "The momentum_start parameter controls the amount of momentum at the beginning of training. This parameter is only active if adaptive learning rate is disabled.", 0.0D, 1.7976931348623157E308D, 0.0D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "adaptive_rate", false, false));
        types.add(type);
        type = new ParameterTypeDouble("momentum_ramp", "The momentum_ramp parameter controls the amount of learning for which momentum increases (assuming momentum_stable is larger than momentum_start). The ramp is measured in the number of training samples. This parameter is only active if adaptive learning rate is disabled.", 0.0D, 1.7976931348623157E308D, 1000000.0D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "adaptive_rate", false, false));
        types.add(type);
        type = new ParameterTypeDouble("momentum_stable", "The momentum_stable parameter controls the final momentum value reached after momentum_ramp training samples. The momentum used for training will remain the same for training beyond reaching that point. This parameter is only active if adaptive learning rate is disabled. ", 0.0D, 1.7976931348623157E308D, 0.0D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "adaptive_rate", false, false));
        types.add(type);
        type = new ParameterTypeBoolean("nesterov_accelerated_gradient", "The Nesterov accelerated gradient descent method is a modification to traditional gradient descent for convex functions. The method relies on gradient information at various points to build a polynomial approximation that minimizes the residuals in fewer iterations of the descent. This parameter is only active if adaptive learning rate is disabled. ", true, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "adaptive_rate", false, false));
        types.add(type);
        types.add(new ParameterTypeBoolean("standardize", "If enabled, automatically standardize the data. If disabled, the user must provide properly scaled input data.", true, true));
        types.add(new ParameterTypeDouble("L1", "L1 regularization (can add stability and improve generalization, causes many weights to become 0)", 0.0D, 1.0D, 1.0E-5D, true));
        types.add(new ParameterTypeDouble("L2", "L2 regularization (can add stability and improve generalization, causes many weights to be small", 0.0D, 1.0D, 0.0D, true));
        type = new ParameterTypeDouble("max_w2", "Constraint for squared sum of incoming weights per unit", 0.0D, 3.4028234663852886E38D, 10.0D, true);
        type.setExpert(true);
        type.registerDependencyCondition(new AboveOperatorVersionCondition(this, MAX_W2_EXPERT_VERSION) {
        });
        types.add(type);
        types.add(new ParameterTypeCategory("loss_function", "Loss function.", (String[])Stream.of(Loss.values()).map((val) -> {
            return val.toString();
        }).toArray((x$0) -> {
            return new String[x$0];
        }), Loss.Automatic.ordinal(), true));
        List<Family> distributionValues = new ArrayList(Arrays.asList(Family.values()));
        types.add(new ParameterTypeCategory("distribution_function", "Distribution function.", (String[])distributionValues.stream().map((val) -> {
            return val.toString();
        }).toArray((x$0) -> {
            return new String[x$0];
        }), Family.AUTO.ordinal(), true));
        types.add(new ParameterTypeBoolean("early_stopping", "If true, parameters for early stopping needs to be specified.", false, true));
        type = new ParameterTypeInt("stopping_rounds", "Early stopping based on convergence of stopping_metric. Stop if simple moving average of length k of the stopping_metric does not improve for k:=stopping_rounds scoring events.", 1, 2147483647, 1, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "early_stopping", false, true));
        types.add(type);
        type = new ParameterTypeCategory("stopping_metric", "Metric to use for early stopping (AUTO: logloss for classification, deviance for regression)", (String[])Stream.of(StoppingMetric.values()).map((val) -> {
            return val.toString();
        }).toArray((x$0) -> {
            return new String[x$0];
        }), StoppingMetric.AUTO.ordinal(), true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "early_stopping", false, true));
        types.add(type);
        type = new ParameterTypeDouble("stopping_tolerance", "Relative tolerance for metric-based stopping criterion (stop if relative improvement is not at least this much).", 0.0D, 1.0D, 0.001D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "early_stopping", false, true));
        types.add(type);
        types.add(new ParameterTypeCategory("missing_values_handling", "Handling of missing values. Either Skip or MeanImputation.", (String[])Stream.of(MissingValuesHandling.values()).map(Enum::toString).toArray((x$0) -> {
            return new String[x$0];
        }), MissingValuesHandling.MeanImputation.ordinal(), true));
        types.addAll(super.getParameterTypes());
        String[] expertParamsArray = DeepLearningOperator.AdvancedDeepLearningParameter.lowerCaseValues(this.getCompatibilityLevel());
        ParameterType advancedParametersNew = new ParameterTypeList("expert_parameters_", "Advanced parameters that can be set.", new ParameterTypeStringCategory("parameter name", "The name of the parameter", expertParamsArray), new ParameterTypeString("value", "The value of the parameter"), true);
        advancedParametersNew.registerDependencyCondition(new AboveOperatorVersionCondition(this, MAX_W2_EXPERT_VERSION) {
            @Override
            public boolean isConditionFullfilled() {
                boolean b = super.isConditionFullfilled();
                return b;
            }
        });
        types.add(advancedParametersNew);
        return types;
    }

    @Override
    protected void postProcessParameterTypes(List<ParameterType> types) {
        super.postProcessParameterTypes(types);
        ParameterType advancedParametersOld = (ParameterType)types.get(types.size() - 1);
        advancedParametersOld.registerDependencyCondition(new BelowOrEqualOperatorVersionCondition(this, MAX_W2_EXPERT_VERSION) {
            @Override
            public boolean isConditionFullfilled() {
                boolean b = super.isConditionFullfilled();
                return b;
            }
        });
    }

    @Override
    protected Parameters buildModelSpecificParameters(ExampleSet exampleSet) throws UndefinedParameterError, UserError {
        DeepLearningParameters params = new DeepLearningParameters();
        Activation act = Activation.valueOf(this.getParameterAsString("activation"));
        params._activation = act;
        String[] hiddenArray;
        if (Activation.ExpRectifierWithDropout.equals(act) || Activation.RectifierWithDropout.equals(act) || Activation.TanhWithDropout.equals(act) || Activation.MaxoutWithDropout.equals(act)) {
            hiddenArray = ParameterTypeEnumeration.transformString2Enumeration(this.getParameterAsString("hidden_dropout_ratios"));
            params._hidden_dropout_ratios = Stream.of(hiddenArray).mapToDouble(Double::parseDouble).toArray();
        }

        hiddenArray = ParameterTypeEnumeration.transformString2Enumeration(this.getParameterAsString("hidden_layer_sizes"));
        params._hidden = Stream.of(hiddenArray).mapToInt(Integer::parseInt).toArray();
        params._epochs = this.getParameterAsDouble("epochs");
        params._variable_importances = this.getParameterAsBoolean("compute_variable_importances");
        params._train_samples_per_iteration = this.getParameterAsLong("train_samples_per_iteration");
        boolean adaptiveRate = this.getParameterAsBoolean("adaptive_rate");
        params._adaptive_rate = adaptiveRate;
        if (adaptiveRate) {
            params._epsilon = this.getParameterAsDouble("epsilon");
            params._rho = this.getParameterAsDouble("rho");
        } else {
            params._rate = this.getParameterAsDouble("learning_rate");
            params._rate_annealing = this.getParameterAsDouble("learning_rate_annealing");
            params._rate_decay = this.getParameterAsDouble("learning_rate_decay");
            params._momentum_start = this.getParameterAsDouble("momentum_start");
            params._momentum_ramp = this.getParameterAsDouble("momentum_ramp");
            params._momentum_stable = this.getParameterAsDouble("momentum_stable");
            params._nesterov_accelerated_gradient = this.getParameterAsBoolean("nesterov_accelerated_gradient");
        }

        params._standardize = this.getParameterAsBoolean("standardize");
        params._l1 = this.getParameterAsDouble("L1");
        params._l2 = this.getParameterAsDouble("L2");
        if (this.getCompatibilityLevel().isAbove(MAX_W2_EXPERT_VERSION)) {
            float f = (float)this.getParameterAsDouble("max_w2");
//            params._max_w2 = (float)f == 0.0F ? 1.0F / 0.0F : f;
            params._max_w2 = (f == 0.0D) ? Float.POSITIVE_INFINITY : f;
        }

        params._loss = Loss.valueOf(this.getParameterAsString("loss_function"));
        params._distribution = Family.valueOf(this.getParameterAsString("distribution_function"));
        if (this.getParameterAsBoolean("early_stopping")) {
            params._stopping_rounds = this.getParameterAsInt("stopping_rounds");
            params._stopping_metric = StoppingMetric.valueOf(this.getParameterAsString("stopping_metric"));
            params._stopping_tolerance = this.getParameterAsDouble("stopping_tolerance");
        }

        params._missing_values_handling = MissingValuesHandling.valueOf(this.getParameterAsString("missing_values_handling"));
        boolean reproducible = this.getParameterAsBoolean("reproducible_(uses_1_thread)");
        params._reproducible = reproducible;
        if (reproducible) {
            boolean useSeed = this.getParameterAsBoolean("use_local_random_seed");
            if (useSeed) {
                params._seed = (long)this.getParameterAsInt("local_random_seed");
            } else {
                int seed = this.getProcess().getRootOperator().getParameterAsInt("random_seed");
                if (seed != -1) {
                    params._seed = (long)seed;
                }
            }
        }

        List<String[]> parameterList = this.getCompatibilityLevel().isAtMost(MAX_W2_EXPERT_VERSION) ? this.getParameterList("expert_parameters") : this.getParameterList("expert_parameters_");
        Iterator var22 = parameterList.iterator();

        while(true) {
            while(true) {
                String[] kv;
                do {
                    if (!var22.hasNext()) {
                        return params;
                    }

                    kv = (String[])var22.next();
                } while(kv.length != 2);

                String value = kv[1];

                DeepLearningOperator.AdvancedDeepLearningParameter param;
                try {
                    param = DeepLearningOperator.AdvancedDeepLearningParameter.valueOf(kv[0].toUpperCase());
                } catch (IllegalArgumentException var15) {
                    throw new UserError(this, "h2o.wrong_advanced_key", new Object[]{kv[0]});
                }

                switch(param) {
                    case AVERAGE_ACTIVATION:
                        params._average_activation = this.getDoubleValue(kv[0], value);
                        break;
                    case BALANCE_CLASSES:
                        params._balance_classes = this.getBooleanValue(kv[0], value);
                        break;
                    case CLASSIFICATION_STOP:
                        params._classification_stop = this.getDoubleValue(kv[0], value);
                        break;
                    case CLASS_SAMPLING_FACTORS:
                        if (this.isEmptyAdvancedValue(value)) {
                            break;
                        }

                        String[] vals = value.split(",");
                        float[] factors = new float[vals.length];

                        try {
                            for(int i = 0; i < vals.length; ++i) {
                                factors[i] = Float.valueOf(vals[i].trim());
                            }
                        } catch (NumberFormatException var19) {
                            throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, kv[0]});
                        }

                        params._class_sampling_factors = factors;
                        break;
                    case ELASTIC_AVERAGING:
                        params._elastic_averaging = this.getBooleanValue(kv[0], value);
                        break;
                    case EXPORT_WEIGHTS_AND_BIASES:
                        params._export_weights_and_biases = this.getBooleanValue(kv[0], value);
                        break;
                    case FAST_MODE:
                        params._fast_mode = this.getBooleanValue(kv[0], value);
                        break;
                    case FOLD_ASSIGNMENT:
                        try {
                            FoldAssignmentScheme scheme = FoldAssignmentScheme.valueOf(value);
                            params._fold_assignment = scheme;
                            break;
                        } catch (IllegalArgumentException var18) {
                            throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, kv[0]});
                        }
                    case FOLD_COLUMN:
                        if (!this.isEmptyAdvancedValue(value)) {
                            params._fold_column = value;
                        }
                        break;
                    case OFFSET_COLUMN:
                        if (!this.isEmptyAdvancedValue(value)) {
                            params._offset_column = value;
                        }
                        break;
                    case FORCE_LOAD_BALANCE:
                        params._force_load_balance = this.getBooleanValue(kv[0], value);
                        break;
                    case INITIAL_WEIGHT_DISTRIBUTION:
                        try {
                            InitialWeightDistribution distr = InitialWeightDistribution.valueOf(value);
                            params._initial_weight_distribution = distr;
                            break;
                        } catch (IllegalArgumentException var17) {
                            throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, kv[0]});
                        }
                    case INITIAL_WEIGHT_SCALE:
                        params._initial_weight_scale = this.getDoubleValue(kv[0], value);
                        break;
                    case INPUT_DROPOUT_RATIO:
                        params._input_dropout_ratio = this.getDoubleValue(kv[0], value);
                        break;
                    case KEEP_CROSS_VALIDATION_PREDICTIONS:
                        params._keep_cross_validation_predictions = this.getBooleanValue(kv[0], value);
                        break;
                    case KEEP_CROSS_VALIDATION_FOLD_ASSIGNMENT:
                        params._keep_cross_validation_fold_assignment = this.getBooleanValue(kv[0], value);
                        break;
                    case MAX_AFTER_BALANCE_SIZE:
                        params._max_after_balance_size = this.getFloatValue(kv[0], value);
                        break;
                    case MAX_CATEGORICAL_FEATURES:
                        params._max_categorical_features = this.getIntegerValue(kv[0], value);
                        break;
                    case MAX_CONFUSION_MATRIX_SIZE:
                        params._max_confusion_matrix_size = this.getIntegerValue(kv[0], value);
                        break;
                    case MAX_W2:
                        if (this.getCompatibilityLevel().isAtMost(MAX_W2_EXPERT_VERSION)) {
                            params._max_w2 = this.getFloatValue(kv[0], value);
                        }
                        break;
                    case MINI_BATCH_SIZE:
                        params._mini_batch_size = this.getIntegerValue(kv[0], value);
                        break;
                    case OVERWRITE_WITH_BEST_MODEL:
                        params._overwrite_with_best_model = this.getBooleanValue(kv[0], value);
                        break;
                    case QUANTILE_ALPHA:
                        params._quantile_alpha = this.getDoubleValue(kv[0], value);
                        break;
                    case QUIET_MODE:
                        params._quiet_mode = this.getBooleanValue(kv[0], value);
                        break;
                    case REGRESSION_STOP:
                        params._regression_stop = this.getDoubleValue(kv[0], value);
                        break;
                    case SCORE_DUTY_CYCLE:
                        params._score_duty_cycle = this.getDoubleValue(kv[0], value);
                        break;
                    case SCORE_EACH_ITERATION:
                        params._score_each_iteration = this.getBooleanValue(kv[0], value);
                        break;
                    case SCORE_TRAINING_SAMPLES:
                        params._score_training_samples = this.getLongValue(kv[0], value);
                        break;
                    case SCORE_INTERVAL:
                        params._score_interval = this.getDoubleValue(kv[0], value);
                        break;
                    case SCORE_VALIDATION_SAMPLES:
                        params._score_validation_samples = this.getLongValue(kv[0], value);
                        break;
                    case SCORE_VALIDATION_SAMPLING:
                        try {
                            ClassSamplingMethod method = ClassSamplingMethod.valueOf(value);
                            params._score_validation_sampling = method;
                            break;
                        } catch (IllegalArgumentException var16) {
                            throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, kv[0]});
                        }
                    case SHUFFLE_TRAINING_DATA:
                        params._shuffle_training_data = this.getBooleanValue(kv[0], value);
                        break;
                    case SPARSE:
                        params._sparse = this.getBooleanValue(kv[0], value);
                        break;
                    case SPARSITY_BETA:
                        params._sparsity_beta = this.getDoubleValue(kv[0], value);
                        break;
                    case TWEEDIE_POWER:
                        params._tweedie_power = this.getDoubleValue(kv[0], value);
                        break;
                    case USE_ALL_FACTOR_LEVELS:
                        params._use_all_factor_levels = this.getBooleanValue(kv[0], value);
                        break;
                    case NFOLDS:
                        params._nfolds = this.getIntegerValue(kv[0], value);
                        break;
                    default:
                        LogService.getRoot().warning("The invalid parameter '" + kv[0] + "' is skipped.");
                }
            }
        }
    }

    @Override
    protected String[] getAdvancedParametersArray() {
        return DeepLearningOperator.AdvancedDeepLearningParameter.lowerCaseValues();
    }

    @Override
    protected void performAdditionalChecks() {
        super.performAdditionalChecks();
    }

    private void runModelBuilderValidations(ModelBuilder builder, int size, int _nclass) {
        Parameters _parms = builder._parms;
        if (_parms._nfolds < 0 || _parms._nfolds == 1) {
            builder.error("_nfolds", "nfolds must be either 0 or >1.");
        }

        if (_parms._nfolds > 1 && _parms._nfolds > size) {
            builder.error("_nfolds", "nfolds cannot be larger than the number of rows (" + size + ").");
        }

        if (_parms._fold_column != null) {
            if (_parms._nfolds > 1) {
                builder.error("_nfolds", "nfolds cannot be specified at the same time as a fold column.");
            }

            if (_parms._fold_assignment != FoldAssignmentScheme.AUTO) {
                builder.error("_fold_assignment", "Fold assignment is not allowed in conjunction with a fold column.");
            }
        }

        if (_parms._tweedie_power <= 1.0D || _parms._tweedie_power >= 2.0D) {
            builder.error("_tweedie_power", "Tweedie power must be between 1 and 2 (exclusive).");
        }

        if ((double)_parms._max_after_balance_size <= 0.0D) {
            builder.error("_max_after_balance_size", "Max size after balancing needs to be positive, suggest 1.0f");
        }

        if (_nclass > 1000) {
            builder.error("_nclass", "Too many levels in response column: " + _nclass + ", maximum supported number of classes is " + 1000 + ".");
        }

        if (_parms._stopping_tolerance < 0.0D) {
            builder.error("_stopping_tolerance", "Stopping tolerance must be >= 0.");
        }

        if (_parms._stopping_tolerance >= 1.0D) {
            builder.error("_stopping_tolerance", "Stopping tolerance must be < 1.");
        }

        if (_parms._stopping_rounds == 0) {
            if (_parms._stopping_metric != StoppingMetric.AUTO) {
                builder.warn("_stopping_metric", "Stopping metric is ignored for _stopping_rounds=0.");
            }

            if (_parms._stopping_tolerance != 0.0D) {
                builder.warn("_stopping_tolerance", "Stopping tolerance is ignored for _stopping_rounds=0.");
            }
        } else if (_parms._stopping_rounds < 0) {
            builder.error("_stopping_rounds", "Stopping rounds must be >= 0.");
        } else if (builder.isClassifier()) {
            if (_parms._stopping_metric == StoppingMetric.deviance) {
                builder.error("_stopping_metric", "Stopping metric cannot be deviance for classification.");
            }

            if (builder.nclasses() != 2 && _parms._stopping_metric == StoppingMetric.AUC) {
                builder.error("_stopping_metric", "Stopping metric cannot be AUC for multinomial classification.");
            }
        } else if (_parms._stopping_metric == StoppingMetric.misclassification || _parms._stopping_metric == StoppingMetric.AUC || _parms._stopping_metric == StoppingMetric.logloss) {
            builder.error("_stopping_metric", "Stopping metric cannot be " + _parms._stopping_metric.toString() + " for regression.");
        }

        if (_parms._max_runtime_secs < 0.0D) {
            builder.error("_max_runtime_secs", "Max runtime (in seconds) must be greater than 0 (or 0 for unlimited).");
        }

    }

    static {
        MAX_W2_EXPERT_VERSION = NTHREAD_REBALANCING_MAXRUNTIME_VERSION;
    }

    public static enum AdvancedDeepLearningParameter {
        INPUT_DROPOUT_RATIO("0"),
        QUANTILE_ALPHA("0.5"),
        TWEEDIE_POWER("1.5"),
        SCORE_INTERVAL("5"),
        SCORE_TRAINING_SAMPLES("10000"),
        SCORE_VALIDATION_SAMPLES("0"),
        SCORE_DUTY_CYCLE("0.1"),
        OVERWRITE_WITH_BEST_MODEL("true"),
        MAX_W2("10"),
        INITIAL_WEIGHT_DISTRIBUTION("UniformAdaptive"),
        INITIAL_WEIGHT_SCALE("1"),
        CLASSIFICATION_STOP("0"),
        REGRESSION_STOP("1e-6"),
        SCORE_VALIDATION_SAMPLING("Uniform"),
        FAST_MODE("true"),
        FORCE_LOAD_BALANCE("true"),
        SHUFFLE_TRAINING_DATA("false"),
        QUIET_MODE("false"),
        SPARSE("false"),
        AVERAGE_ACTIVATION("0.0"),
        SPARSITY_BETA("0.0"),
        MAX_CATEGORICAL_FEATURES("2147483647"),
        EXPORT_WEIGHTS_AND_BIASES("false"),
        MINI_BATCH_SIZE("1"),
        ELASTIC_AVERAGING("false"),
        USE_ALL_FACTOR_LEVELS("true"),
        SCORE_EACH_ITERATION("false"),
        FOLD_ASSIGNMENT("AUTO"),
        FOLD_COLUMN(" "),
        OFFSET_COLUMN(" "),
        BALANCE_CLASSES("false"),
        MAX_CONFUSION_MATRIX_SIZE("20"),
        KEEP_CROSS_VALIDATION_PREDICTIONS("false"),
        KEEP_CROSS_VALIDATION_FOLD_ASSIGNMENT("false"),
        MAX_AFTER_BALANCE_SIZE("5.0"),
        CLASS_SAMPLING_FACTORS(" "),
        NFOLDS("0");

        private String defaultValue;

        private AdvancedDeepLearningParameter(String defaultVal) {
            this.defaultValue = defaultVal;
        }

        public String getDefaultValue() {
            return this.defaultValue;
        }

        public static String[] lowerCaseValues() {
            return (String[])Stream.of(values()).map((val) -> {
                return val.name().toLowerCase();
            }).toArray((x$0) -> {
                return new String[x$0];
            });
        }

        public static String[] lowerCaseValues(OperatorVersion operatorVersion) {
            return (String[])Stream.of(values()).filter((val) -> {
                return val != MAX_W2 || operatorVersion.isAtMost(DeepLearningOperator.MAX_W2_EXPERT_VERSION);
            }).map((val) -> {
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
