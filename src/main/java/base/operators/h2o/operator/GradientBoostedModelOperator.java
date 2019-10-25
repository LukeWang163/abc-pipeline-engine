package base.operators.h2o.operator;

import base.operators.h2o.operator.H2OLearner;
import base.operators.example.AttributeWeights;
import base.operators.example.ExampleSet;
import base.operators.h2o.ClusterManager;
import base.operators.h2o.model.GradientBoostedModelConverter;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.UserError;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.MDNumber.Relation;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.AboveOperatorVersionCondition;
import base.operators.parameter.conditions.BelowOrEqualOperatorVersionCondition;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.OrParameterCondition;
import base.operators.parameter.conditions.ParameterCondition;
import base.operators.tools.LogService;
import base.operators.tools.RandomGenerator;
import hex.Model;
import hex.ModelBuilder;
import hex.Distribution.Family;
import hex.Model.Parameters;
import hex.Model.Parameters.FoldAssignmentScheme;
import hex.ScoreKeeper.StoppingMetric;
import hex.tree.gbm.GBM;
import hex.tree.gbm.GBMModel;
import hex.tree.gbm.GBMModel.GBMOutput;
import hex.tree.gbm.GBMModel.GBMParameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import water.Key;
import water.util.TwoDimTable;

public class GradientBoostedModelOperator extends H2OLearner {
    public static final String PARAMETER_NUMBER_OF_TREES = "number_of_trees";
    public static final String PARAMETER_MAXIMAL_DEPTH = "maximal_depth";
    public static final String PARAMETER_MIN_ROWS = "min_rows";
    public static final String PARAMETER_MIN_SPLIT_IMPROVEMENT = "min_split_improvement";
    public static final String PARAMETER_NBINS = "number_of_bins";
    public static final String PARAMETER_LEARNING_RATE = "learning_rate";
    public static final String PARAMETER_DISTRIBUTION = "distribution";
    public static final String PARAMETER_SAMPLE_RATE = "sample_rate";
    public static final String PARAMETER_EARLY_STOPPING = "early_stopping";
    public static final String PARAMETER_STOPPING_ROUNDS = "stopping_rounds";
    public static final String PARAMETER_STOPPING_METRIC = "stopping_metric";
    public static final String PARAMETER_STOPPING_TOLERANCE = "stopping_tolerance";
    public static final String PARAMETER_ADVANCED = "expert_parameters";
    public static final String PARAMETER_REPRODUCIBLE = "reproducible";
    public static final String PARAMETER_MAXIMUM_NUMBER_OF_THREADS = "maximum_number_of_threads";
    private static final OperatorVersion REPRODUCIBLE_VERSION;
    public static final String H2O_ERROR_INDICATOR = "ERRR";
    private static final String RELATIVE_IMPORTANCE_HEADER = "Relative Importance";
    private OutputPort weightOutput = (OutputPort)this.getOutputPorts().createPort("weights");

    public GradientBoostedModelOperator(OperatorDescription description) {
        super(description, "reproducible");
        this.getTransformer().addRule(new MDTransformationRule() {
            @Override
            public void transformMD() {
                MetaData md = GradientBoostedModelOperator.this.getExampleSetInputPort().getMetaData();
                if (md != null && md instanceof ExampleSetMetaData) {
//                    try {
//                        if (GradientBoostedModelOperator.this.getParameterAsBoolean("reproducible") && !GradientBoostedModelOperator.this.getParameterAsBoolean("use_local_random_seed") && GradientBoostedModelOperator.this.getProcess().getRootOperator().getParameterAsInt("random_seed") == -1) {
//                            List<QuickFix> fixes = new ArrayList();
//                            fixes.add(new ParameterSettingQuickFix(GradientBoostedModelOperator.this.getProcess().getRootOperator(), "random_seed", (String)null, "set_optional_parameter", new Object[]{"random_seed"}));
//                            fixes.add(new ParameterSettingQuickFix(GradientBoostedModelOperator.this, "use_local_random_seed", "true"));
//                            fixes.add(new ParameterSettingQuickFix(GradientBoostedModelOperator.this, "reproducible", "false"));
//                            GradientBoostedModelOperator.this.addError(new SimpleProcessSetupError(Severity.WARNING, GradientBoostedModelOperator.this.getPortOwner(), fixes, "param_reproducible_without_seed", new Object[0]));
//                        }
//                    } catch (UndefinedParameterError var9) {
//                    }

                    ExampleSetMetaData emd = (ExampleSetMetaData)md;
                    AttributeMetaData labelMD = emd.getLabelMetaData();
                    if (labelMD != null) {
                        boolean performRegression = labelMD.isNumerical();

                        try {
                            Family distribution = Family.valueOf(GradientBoostedModelOperator.this.getParameterAsString("distribution"));
                            if (!Family.AUTO.equals(distribution)) {
                                ArrayList fixesx;
                                if (!Family.bernoulli.equals(distribution) && !Family.multinomial.equals(distribution)) {
                                    if (!performRegression) {
                                        fixesx = new ArrayList();
                                        fixesx.add(new ParameterSettingQuickFix(GradientBoostedModelOperator.this, "distribution", (String)null, "set_optional_parameter", new Object[]{"distribution"}));
                                        GradientBoostedModelOperator.this.addError(new SimpleProcessSetupError(Severity.ERROR, GradientBoostedModelOperator.this.getPortOwner(), fixesx, "param_regression_only", new Object[]{distribution.toString(), "distribution"}));
                                    }
                                } else {
                                    fixesx = new ArrayList();
                                    fixesx.add(new ParameterSettingQuickFix(GradientBoostedModelOperator.this, "distribution", (String)null, "set_optional_parameter", new Object[]{"distribution"}));
                                    if (performRegression) {
                                        GradientBoostedModelOperator.this.addError(new SimpleProcessSetupError(Severity.ERROR, GradientBoostedModelOperator.this.getPortOwner(), fixesx, "param_classification_only", new Object[]{distribution.toString(), "distribution"}));
                                    } else if (Family.bernoulli.equals(distribution) && !labelMD.isBinominal() && labelMD.getValueSet().size() != 2) {
                                        GradientBoostedModelOperator.this.addError(new SimpleProcessSetupError(Severity.WARNING, GradientBoostedModelOperator.this.getPortOwner(), fixesx, "param_binominal_only", new Object[]{distribution.toString(), "distribution"}));
                                    }
                                }
                            }
                        } catch (UndefinedParameterError var10) {
                        }
                    }

                    if (emd.getAttributeByRole("weight") == null) {
                        try {
                            double minRows = GradientBoostedModelOperator.this.getParameterAsDouble("min_rows");
                            MDInteger size = emd.getNumberOfExamples();
                            if (!Relation.AT_LEAST.equals(size.getRelation()) && size.getNumber() != null && (Integer)size.getNumber() >= 0 && (double)(Integer)size.getNumber() <= minRows * 2.0D && size.isKnown()) {
                                List<QuickFix> fixesxx = new ArrayList();
                                fixesxx.add(new ParameterSettingQuickFix(GradientBoostedModelOperator.this, "min_rows", (String)null, "set_optional_parameter", new Object[]{"min_rows"}));
                                GradientBoostedModelOperator.this.addError(new SimpleProcessSetupError(Severity.ERROR, GradientBoostedModelOperator.this.getPortOwner(), fixesxx, "param_smaller", new Object[]{"min_rows", "the (size of the input data) * 2"}));
                            }
                        } catch (UndefinedParameterError var8) {
                        }
                    }
                }

            }
        });
        this.getTransformer().addGenerationRule(this.weightOutput, AttributeWeights.class);
    }

    @Override
    protected void performAdditionalChecks() {
        try {
            List<String[]> parameterList = this.getParameterList("expert_parameters");
            Iterator var2 = parameterList.iterator();

            while(var2.hasNext()) {
                String[] kv = (String[])var2.next();
                if (kv.length == 2) {
                    try {
                        GradientBoostedModelOperator.AdvancedGBMParameter param = GradientBoostedModelOperator.AdvancedGBMParameter.valueOf(kv[0].toUpperCase());
                        if (GradientBoostedModelOperator.AdvancedGBMParameter.NBINS_TOP_LEVEL.equals(param)) {
                            try {
                                int nbins_top_level = Integer.parseInt(kv[1]);
                                if (nbins_top_level < this.getParameterAsInt("number_of_bins")) {
                                    List<QuickFix> fixes = new ArrayList();
                                    fixes.add(new ParameterSettingQuickFix(this, "expert_parameters", (String)null, "set_optional_parameter", new Object[]{"expert_parameters"}));
                                    fixes.add(new ParameterSettingQuickFix(this, "number_of_bins", (String)null, "set_optional_parameter", new Object[]{"number_of_bins"}));
                                    this.addError(new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(), fixes, "param_greater", new Object[]{kv[0], "number_of_bins"}));
                                }
                            } catch (NumberFormatException var7) {
                            }
                        }

                        if (GradientBoostedModelOperator.AdvancedGBMParameter.NFOLDS.equals(param) && Integer.parseInt(kv[1]) == 1) {
                            List<QuickFix> fixes = new ArrayList();
                            fixes.add(new ParameterSettingQuickFix(this, "expert_parameters", (String)null, "set_optional_parameter", new Object[]{"expert_parameters"}));
                            this.addError(new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(), fixes, "invalid_parameter", new Object[]{1, GradientBoostedModelOperator.AdvancedGBMParameter.NFOLDS.toString(), "nfolds must be either 0 or >1"}));
                        }
                    } catch (IllegalArgumentException var8) {
                        List<QuickFix> fixes = new ArrayList();
                        fixes.add(new ParameterSettingQuickFix(this, "expert_parameters", (String)null, "set_optional_parameter", new Object[]{"expert_parameters"}));
                        this.addError(new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(), fixes, "invalid_param_key", new Object[]{kv[0]}));
                    }
                }
            }
        } catch (UndefinedParameterError var9) {
        }

        super.performAdditionalChecks();
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new ArrayList();
        types.add(new ParameterTypeInt("number_of_trees", "The number of learned trees.", 1, 10000, 100, false));
        ParameterType type = new ParameterTypeBoolean("reproducible", "Reproducible", false, false);
        type.registerDependencyCondition(new AboveOperatorVersionCondition(this, REPRODUCIBLE_VERSION));
        types.add(type);
        type = new ParameterTypeInt("maximum_number_of_threads", "Maximum number of threads", 1, 2147483647, 4, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "reproducible", false, true));
        types.add(type);
        List<ParameterType> randomTypes = RandomGenerator.getRandomGeneratorParameters(this);
        Iterator var4 = randomTypes.iterator();

        while(var4.hasNext()) {
            ParameterType pt = (ParameterType)var4.next();
            if ("use_local_random_seed".equals(pt.getKey())) {
                pt.registerDependencyCondition(new OrParameterCondition(this, false, new ParameterCondition[]{new BelowOrEqualOperatorVersionCondition(this, REPRODUCIBLE_VERSION), new BooleanParameterCondition(this, "reproducible", false, true)}));
            }
        }

        types.addAll(randomTypes);
        types.add(new ParameterTypeInt("maximal_depth", "Maximum depth of a tree. Deeper trees are more expressive (potentially allowing higher accuracy), but they are also more costly to train and are more likely to overfit.", 0, 2147483647, 10, false));
        types.add(new ParameterTypeDouble("min_rows", "Fewest allowed (weighted) observations in a leaf (in R called 'nodesize').", 4.9E-324D, 1.7976931348623157E308D, 10.0D, false));
        types.add(new ParameterTypeDouble("min_split_improvement", "Minimum relative improvement in squared error reduction for a split to happen.", 0.0D, 1.0D, 0.0D, false));
        types.add(new ParameterTypeInt("number_of_bins", "For numerical columns (real/int), build a histogram of (at least) this many bins, then split at the best point", 1, 65536, 20, false));
        types.add(new ParameterTypeDouble("learning_rate", "Learning rate.", 0.0D, 1.0D, 0.01D, false));
        types.add(new ParameterTypeDouble("sample_rate", "Row sample rate per tree.", 4.9E-324D, 1.0D, 1.0D, false));
        List<Family> distributionValues = new ArrayList(Arrays.asList(Family.values()));
        distributionValues.remove(Family.huber);
        types.add(new ParameterTypeCategory("distribution", "Distribution function.", (String[])distributionValues.stream().map((val) -> {
            return val.toString();
        }).toArray((x$0) -> {
            return new String[x$0];
        }), Family.AUTO.ordinal(), false));
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
        types.addAll(super.getParameterTypes());
        return types;
    }

    @Override
    protected void createWeights(ExampleSet exampleSet, Model<?, ?, ?> model) throws OperatorException {
        if (this.weightOutput.isConnected()) {
            TwoDimTable importancesTable = ((GBMOutput)((GBMModel)model)._output)._variable_importances;
            if (importancesTable == null || importancesTable.getColHeaders() == null || importancesTable.getRowHeaders() == null) {
                ClusterManager.getH2OLogger().info("Weights could not be obtained from the H2O model.");
                return;
            }

            String[] colHeaders = importancesTable.getColHeaders();
            int relativeImportanceIndex = 0;
            String[] var6 = colHeaders;
            int row = colHeaders.length;

            int rowDim;
            for(rowDim = 0; rowDim < row; ++rowDim) {
                String header = var6[rowDim];
                if ("Relative Importance".equalsIgnoreCase(header.trim())) {
                    break;
                }

                ++relativeImportanceIndex;
            }

            AttributeWeights weights = new AttributeWeights(exampleSet);
            row = 0;
            rowDim = importancesTable.getRowDim();
            String[] var14 = importancesTable.getRowHeaders();
            int var10 = var14.length;

            for(int var11 = 0; var11 < var10; ++var11) {
                String attributeName = var14[var11];
                if (row < rowDim) {
                    weights.setWeight(attributeName, (Double)importancesTable.get(row++, relativeImportanceIndex));
                } else {
                    weights.setWeight(attributeName, 0.0D);
                }
            }

            this.weightOutput.deliver(weights);
        }

    }

    @Override
    public base.operators.operator.Model createModel(ExampleSet exampleSet, Model<?, ?, ?> model) throws OperatorException {
        return GradientBoostedModelConverter.convert(exampleSet, (GBMModel)model);
    }

    @Override
    protected ModelBuilder<? extends Model<?, ?, ?>, ?, ?> createModelBuilder(Parameters params, String modelName) {
        return new GBM((GBMParameters)params, Key.make(modelName));
    }

    @Override
    protected Parameters buildModelSpecificParameters(ExampleSet es) throws UndefinedParameterError, UserError {
        GBMParameters gbmParameters = new GBMParameters();
        gbmParameters._ntrees = this.getParameterAsInt("number_of_trees");
        gbmParameters._max_depth = this.getParameterAsInt("maximal_depth");
        gbmParameters._min_rows = this.getParameterAsDouble("min_rows");
        gbmParameters._nbins = this.getParameterAsInt("number_of_bins");
        gbmParameters._learn_rate = this.getParameterAsDouble("learning_rate");
        gbmParameters._distribution = Family.valueOf(this.getParameterAsString("distribution"));
        if (this.getParameterAsBoolean("early_stopping")) {
            gbmParameters._stopping_rounds = this.getParameterAsInt("stopping_rounds");
            gbmParameters._stopping_metric = StoppingMetric.valueOf(this.getParameterAsString("stopping_metric"));
            gbmParameters._stopping_tolerance = this.getParameterAsDouble("stopping_tolerance");
        }

        gbmParameters._min_split_improvement = this.getParameterAsDouble("min_split_improvement");
        gbmParameters._sample_rate = this.getParameterAsDouble("sample_rate");
        boolean reproducible;
        if (this.getCompatibilityLevel().isAbove(REPRODUCIBLE_VERSION)) {
            reproducible = this.getParameterAsBoolean("reproducible");
            if (reproducible) {
                gbmParameters._fixed_parallelism = this.getParameterAsInt("maximum_number_of_threads");
                boolean useSeed = this.getParameterAsBoolean("use_local_random_seed");
                if (useSeed) {
                    gbmParameters._seed = (long)this.getParameterAsInt("local_random_seed");
                } else {
                    int seed = 2001;
                    if (seed != -1) {
                        gbmParameters._seed = (long)seed;
                    }
                }
            } else {
                gbmParameters._fixed_parallelism = 0;
            }
        } else {
            reproducible = this.getParameterAsBoolean("use_local_random_seed");
            if (reproducible) {
                gbmParameters._seed = (long)this.getParameterAsInt("local_random_seed");
            } else {
                int seed = 2001;
                if (seed != -1) {
                    gbmParameters._seed = (long)seed;
                }
            }
        }

        List<String[]> parameterList = this.getParameterList("expert_parameters");
        Iterator var17 = parameterList.iterator();

        while(true) {
            while(true) {
                String[] kv;
                do {
                    if (!var17.hasNext()) {
                        return gbmParameters;
                    }

                    kv = (String[])var17.next();
                } while(kv.length != 2);

                String value = kv[1];

                GradientBoostedModelOperator.AdvancedGBMParameter gbmParam;
                try {
                    gbmParam = GradientBoostedModelOperator.AdvancedGBMParameter.valueOf(kv[0].toUpperCase());
                } catch (IllegalArgumentException var11) {
                    throw new UserError(this, "h2o.wrong_advanced_key", new Object[]{kv[0]});
                }

                String[] vals;
                switch(gbmParam) {
                    case BALANCE_CLASSES:
                        gbmParameters._balance_classes = this.getBooleanValue(kv[0], value);
                        break;
                    case CLASS_SAMPLING_FACTORS:
                        if (this.isEmptyAdvancedValue(value)) {
                            break;
                        }

                        vals = value.split(",");
                        float[] factors = new float[vals.length];

                        try {
                            for(int i = 0; i < vals.length; ++i) {
                                factors[i] = Float.valueOf(vals[i].trim());
                            }
                        } catch (NumberFormatException var14) {
                            throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, kv[0]});
                        }

                        gbmParameters._class_sampling_factors = factors;
                        break;
                    case COL_SAMPLE_RATE:
                        gbmParameters._col_sample_rate = this.getDoubleValue(kv[0], value);
                        break;
                    case COL_SAMPLE_RATE_CHANGE_PER_LEVEL:
                        gbmParameters._col_sample_rate_change_per_level = this.getDoubleValue(kv[0], value);
                        break;
                    case COL_SAMPLE_RATE_PER_TREE:
                        gbmParameters._col_sample_rate_per_tree = this.getDoubleValue(kv[0], value);
                        break;
                    case FOLD_ASSIGNMENT:
                        try {
                            FoldAssignmentScheme scheme = FoldAssignmentScheme.valueOf(value);
                            gbmParameters._fold_assignment = scheme;
                            break;
                        } catch (IllegalArgumentException var13) {
                            throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, kv[0]});
                        }
                    case FOLD_COLUMN:
                        if (!this.isEmptyAdvancedValue(value)) {
                            gbmParameters._fold_column = value;
                        }
                        break;
                    case OFFSET_COLUMN:
                        if (!this.isEmptyAdvancedValue(value)) {
                            gbmParameters._offset_column = value;
                        }
                        break;
                    case LEARN_RATE_ANNEALING:
                        gbmParameters._learn_rate_annealing = this.getDoubleValue(kv[0], value);
                        break;
                    case MAX_ABS_LEAFNODE_PRED:
                        gbmParameters._max_abs_leafnode_pred = this.getDoubleValue(kv[0], value);
                        break;
                    case MAX_AFTER_BALANCE_SIZE:
                        gbmParameters._max_after_balance_size = this.getFloatValue(kv[0], value);
                        break;
                    case MAX_CONFUSION_MATRIX_SIZE:
                        gbmParameters._max_confusion_matrix_size = this.getIntegerValue(kv[0], value);
                        break;
                    case NBINS_CATS:
                        gbmParameters._nbins_cats = this.getIntegerValue(kv[0], value);
                        break;
                    case NBINS_TOP_LEVEL:
                        gbmParameters._nbins_top_level = this.getIntegerValue(kv[0], value);
                        break;
                    case QUANTILE_ALPHA:
                        gbmParameters._quantile_alpha = this.getDoubleValue(kv[0], value);
                        break;
                    case R2_STOPPING:
                        gbmParameters._r2_stopping = this.getDoubleValue(kv[0], value);
                        break;
                    case SAMPLE_RATE_PER_CLASS:
                        if (!this.isEmptyAdvancedValue(value)) {
                            vals = value.split(",");

                            try {
                                gbmParameters._sample_rate_per_class = Stream.of(vals).mapToDouble((v) -> {
                                    return Double.parseDouble(v.trim());
                                }).toArray();
                            } catch (NumberFormatException var12) {
                                throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, kv[0]});
                            }
                        }
                        break;
                    case SCORE_EACH_ITERATION:
                        gbmParameters._score_each_iteration = this.getBooleanValue(kv[0], value);
                        break;
                    case SCORE_TREE_INTERVAL:
                        gbmParameters._score_tree_interval = this.getIntegerValue(kv[0], value);
                        break;
                    case TWEEDIE_POWER:
                        gbmParameters._tweedie_power = this.getDoubleValue(kv[0], value);
                        break;
                    case KEEP_CROSS_VALIDATION_PREDICTIONS:
                        gbmParameters._keep_cross_validation_predictions = this.getBooleanValue(kv[0], value);
                        break;
                    case KEEP_CROSS_VALIDATION_FOLD_ASSIGNMENT:
                        gbmParameters._keep_cross_validation_fold_assignment = this.getBooleanValue(kv[0], value);
                        break;
                    case NFOLDS:
                        gbmParameters._nfolds = this.getIntegerValue(kv[0], value);
                        break;
                    default:
                        LogService.getRoot().warning("The invalid parameter '" + kv[0] + "' is skipped.");
                }
            }
        }
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
    protected String[] getAdvancedParametersArray() {
        return GradientBoostedModelOperator.AdvancedGBMParameter.lowerCaseValues();
    }

    static {
        REPRODUCIBLE_VERSION = NTHREAD_REBALANCING_MAXRUNTIME_VERSION;
    }

    public static enum AdvancedGBMParameter {
        SCORE_EACH_ITERATION("false"),
        SCORE_TREE_INTERVAL("0"),
        FOLD_ASSIGNMENT("AUTO"),
        FOLD_COLUMN(" "),
        OFFSET_COLUMN(" "),
        BALANCE_CLASSES("false"),
        MAX_AFTER_BALANCE_SIZE("5.0"),
        MAX_CONFUSION_MATRIX_SIZE("20"),
        NBINS_TOP_LEVEL("1024"),
        NBINS_CATS("1024"),
        R2_STOPPING("0.999999"),
        QUANTILE_ALPHA("0.5"),
        TWEEDIE_POWER("1.5"),
        COL_SAMPLE_RATE("1.0"),
        COL_SAMPLE_RATE_PER_TREE("1.0"),
        KEEP_CROSS_VALIDATION_PREDICTIONS("false"),
        KEEP_CROSS_VALIDATION_FOLD_ASSIGNMENT("false"),
        CLASS_SAMPLING_FACTORS(" "),
        LEARN_RATE_ANNEALING("1.0"),
        SAMPLE_RATE_PER_CLASS(" "),
        COL_SAMPLE_RATE_CHANGE_PER_LEVEL("1.0"),
        MAX_ABS_LEAFNODE_PRED("Infinity"),
        NFOLDS("0");

        private String defaultValue;

        private AdvancedGBMParameter(String defaultVal) {
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

        public static String[] getDefaultValues() {
            return (String[])Stream.of(values()).map((val) -> {
                return val.getDefaultValue();
            }).toArray((x$0) -> {
                return new String[x$0];
            });
        }
    }
}
