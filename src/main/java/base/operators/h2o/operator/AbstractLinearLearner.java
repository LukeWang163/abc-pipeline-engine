package base.operators.h2o.operator;

import base.operators.example.Attribute;
import base.operators.example.AttributeWeights;
import base.operators.example.ExampleSet;
import base.operators.example.table.NominalMapping;
import base.operators.h2o.ClusterManager;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.UserError;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.operator.postprocessing.Threshold;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.AboveOperatorVersionCondition;
import base.operators.parameter.conditions.AndParameterCondition;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.EqualTypeCondition;
import base.operators.parameter.conditions.NonEqualTypeCondition;
import base.operators.parameter.conditions.OrParameterCondition;
import base.operators.parameter.conditions.ParameterCondition;
import hex.AUC2;
import hex.Model;
import hex.ModelBuilder;
import hex.ModelMetricsBinomial;
import hex.Model.Parameters;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters.MissingValuesHandling;
import hex.glm.GLM;
import hex.glm.GLMModel;
import hex.glm.GLMModel.GLMOutput;
import hex.glm.GLMModel.GLMParameters;
import hex.glm.GLMModel.GLMParameters.Family;
import hex.glm.GLMModel.GLMParameters.Solver;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang.ArrayUtils;
import water.Key;

public abstract class AbstractLinearLearner extends H2OLearner {
    public static final String PARAMETER_FAMILY = "family";
    public static final String FAMILY_AUTO = "AUTO";
    public static final String PARAMETER_SOLVER = "solver";
    public static final String SOLVER_SUFFIX_EXPERIMENTAL = " (experimental)";
    public static final String PARAMETER_USE_REGULARIZATION = "use_regularization";
    public static final String PARAMETER_LAMBDA = "lambda";
    public static final String PARAMETER_ALPHA = "alpha";
    public static final String PARAMETER_LAMBDA_SEARCH = "lambda_search";
    public static final String PARAMETER_NLAMBDAS = "number_of_lambdas";
    public static final String PARAMETER_LAMBDA_MIN_RATIO = "lambda_min_ratio";
    public static final String PARAMETER_EARLY_STOPPING = "early_stopping";
    public static final String PARAMETER_STOPPING_ROUNDS = "stopping_rounds";
    public static final String PARAMETER_STOPPING_TOLERANCE = "stopping_tolerance";
    public static final String PARAMETER_STANDARDIZE = "standardize";
    public static final String PARAMETER_NON_NEGATIVE = "non-negative_coefficients";
    public static final String PARAMETER_COMPUTE_P_VALUES = "compute_p-values";
    public static final String PARAMETER_REMOVE_COLLINEAR_COLUMNS = "remove_collinear_columns";
    public static final String PARAMETER_INTERCEPT = "add_intercept";
    public static final String PARAMETER_MISSING_VALUES_HANDLING = "missing_values_handling";
    public static final String PARAMETER_MAX_ITERATIONS = "max_iterations";
    public static final String PARAMETER_SPECIFY_BETA_CONSTRAINTS = "specify_beta_constraints";
    public static final String PARAMETER_REPRODUCIBLE = "reproducible";
    public static final String PARAMETER_MAXIMUM_NUMBER_OF_THREADS = "maximum_number_of_threads";
    private static final OperatorVersion REPRODUCIBLE_VERSION;
    protected static final OperatorVersion USE_DEFAULT_THRESHOLD_VERSION;
    public boolean useBetaConstraints;
    protected OutputPort weightOutput = (OutputPort)this.getOutputPorts().createPort("weights");
    protected OutputPort thresholdOutput = (OutputPort)this.getOutputPorts().createPort("threshold");

    public AbstractLinearLearner(OperatorDescription description, boolean useBetaConstraints) {
        super(description, "reproducible");
        this.useBetaConstraints = useBetaConstraints;
        this.getTransformer().addGenerationRule(this.weightOutput, AttributeWeights.class);
        this.getTransformer().addGenerationRule(this.thresholdOutput, Threshold.class);
    }

    @Override
    protected ModelBuilder<? extends Model<?, ?, ?>, ?, ?> createModelBuilder(Parameters params, String modelName) {
        return new GLM((GLMParameters)params, Key.make(modelName));
    }

    @Override
    protected void createWeights(ExampleSet exampleSet, Model<?, ?, ?> model) throws OperatorException {
        if (this.weightOutput.isConnected()) {
            GLMModel glmModel = (GLMModel)model;
            String[] coefficientNames = ((GLMOutput)glmModel._output).coefficientNames();
            AttributeWeights weights;
            if (!((GLMOutput)glmModel._output)._multinomial) {
                double[] normBetas = ((GLMOutput)glmModel._output).getNormBeta();
                weights = this.generateWeights(exampleSet, coefficientNames, normBetas);
            } else {
                double[][] betaNorm = ((GLMOutput)glmModel._output).getNormBetaMultinomial();
                double[] magnitudes = new double[betaNorm[0].length];
                int i = 0;

                while(true) {
                    if (i >= betaNorm.length) {
                        weights = this.generateWeights(exampleSet, coefficientNames, magnitudes);
                        break;
                    }

                    for(int j = 0; j < betaNorm[i].length; ++j) {
                        double d = betaNorm[i][j];
                        magnitudes[j] += d < 0.0D ? -d : d;
                    }

                    ++i;
                }
            }

            if (weights != null) {
                this.weightOutput.deliver(weights);
            }
        }

    }

    @Override
    protected void createThreshold(Model<?, ?, ?> model) throws OperatorException {
        if (this.thresholdOutput.isConnected()) {
            GLMModel glmModel = (GLMModel)model;
            if (((GLMOutput)glmModel._output)._training_metrics instanceof ModelMetricsBinomial) {
                ModelMetricsBinomial mm = (ModelMetricsBinomial)((ModelMetricsBinomial)((GLMOutput)glmModel._output)._training_metrics);
                AUC2 auc = mm.auc_obj();
                this.thresholdOutput.deliver(new Threshold(auc._ths[auc._max_idx], mm._domain[0], mm._domain[1]));
            }
        }

    }

    private AttributeWeights generateWeights(ExampleSet exampleSet, String[] coefficientNames, double[] values) throws OperatorException {
        if (coefficientNames != null && values != null) {
            AttributeWeights weights = new AttributeWeights();
            boolean[] usedCoefficientNames = new boolean[coefficientNames.length];
            Iterator<Attribute> attrIter = exampleSet.getAttributes().iterator();
            List<Attribute> nominals = new ArrayList();
            ArrayList nonNominals = new ArrayList();

            while(attrIter.hasNext()) {
                Attribute attr = (Attribute)attrIter.next();
                if (attr.isNominal()) {
                    nominals.add(attr);
                } else {
                    nonNominals.add(attr);
                }
            }

            for(int i = 0; i < nominals.size(); ++i) {
                for(int j = i + 1; j < nominals.size(); ++j) {
                    if (((Attribute)nominals.get(i)).getMapping().size() < ((Attribute)nominals.get(j)).getMapping().size()) {
                        Attribute x = (Attribute)nominals.get(i);
                        nominals.set(i, nominals.get(j));
                        nominals.set(j, x);
                    }
                }
            }

            Iterator var19 = nonNominals.iterator();

            Attribute attr;
            while(var19.hasNext()) {
                attr = (Attribute)var19.next();
                String expectedName = attr.getName();
                boolean foundMatch = false;

                for(int i = coefficientNames.length - 1; !foundMatch && i >= 0; --i) {
                    if (!usedCoefficientNames[i] && expectedName.equals(coefficientNames[i])) {
                        weights.setWeight(expectedName, values[i]);
                        usedCoefficientNames[i] = true;
                        foundMatch = true;
                    }
                }

                if (!foundMatch) {
                    weights.setWeight(expectedName, 0.0D);
                }
            }

            var19 = nominals.iterator();

            while(var19.hasNext()) {
                attr = (Attribute)var19.next();
                NominalMapping mapping = attr.getMapping();
                Iterator var23 = mapping.getValues().iterator();

                while(var23.hasNext()) {
                    String nominalValue = (String)var23.next();
                    String expectedName = attr.getName() + "." + nominalValue;
                    String dummyName = attr.getName() + " = " + nominalValue;
                    boolean foundMatch = false;

                    for(int i = 0; !foundMatch && i < values.length; ++i) {
                        if (!usedCoefficientNames[i] && expectedName.equals(coefficientNames[i])) {
                            weights.setWeight(dummyName, values[i]);
                            usedCoefficientNames[i] = true;
                            foundMatch = true;
                        }
                    }

                    if (!foundMatch) {
                        weights.setWeight(dummyName, 0.0D);
                    }
                }
            }

            return weights;
        } else {
            ClusterManager.getH2OLogger().info("Weights could not be obtained from the H2O model.");
            return null;
        }
    }

    protected List<ParameterType> getCommonParameterTypes(List<String> extendedFamilyValues) {
        List<ParameterType> types = new ArrayList();
        String[] solverValues = (String[])Stream.of(Solver.values()).map((s) -> {
            return s.toString() + (!Solver.COORDINATE_DESCENT_NAIVE.equals(s) && !Solver.COORDINATE_DESCENT.equals(s) ? "" : " (experimental)");
        }).toArray((x$0) -> {
            return new String[x$0];
        });
        types.add(new ParameterTypeCategory("solver", "AUTO will currently always set the solver to IRLSM. IRLSM is fast on on problems with small number of predictors and for lambda-search with L1 penalty, L_BFGS scales better for datasets with many columns. Coordinate descent and Coordinate descent naive are experimental (beta).", solverValues, Solver.AUTO.ordinal(), false));
        ParameterType type = new ParameterTypeBoolean("reproducible", "Reproducible", false, false);
        type.registerDependencyCondition(new AboveOperatorVersionCondition(this, REPRODUCIBLE_VERSION));
        types.add(type);
        type = new ParameterTypeInt("maximum_number_of_threads", "Maximum number of threads", 1, 2147483647, 4, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "reproducible", false, true));
        types.add(type);
        types.add(new ParameterTypeBoolean("use_regularization", "Use regularization. if this parameter is enabled, the lambda, alpha and lambda_search parameters can be set.", true, false));
        type = new ParameterTypeDouble("lambda", "The lambda parameter controls  the amount of regularization applied. If lambda is 0.0, no regularization is applied and the alpha parameter is ignored.The default value for lambda is calculated by H2O using a heuristic based on the training data.", 0.0D, 1.7976931348623157E308D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use_regularization", false, true));
        types.add(type);
        type = new ParameterTypeBoolean("lambda_search", "Use lambda search starting at lambda max, given lambda is then interpreted as lambda min.", false, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use_regularization", false, true));
        types.add(type);
        type = new ParameterTypeInt("number_of_lambdas", "The number of lambda values when lambda search = TRUE. 0 means no preference.", 0, 2147483647, 0, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "lambda_search", false, true));
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeDouble("lambda_min_ratio", "Smallest value for lambda as a fraction of lambda.max, the entry value, which is the smallest value for which all coefficients in the model are zero.  If the number of observations is greater than the number of variables then default lambda_min_ratio = 0.0001; if the number of observations is less than the number of variables then default lambda_min_ratio = 0.01. Default is 0.0.", 0.0D, 1.7976931348623157E308D, 0.0D, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "lambda_search", false, true));
        type.setExpert(true);
        types.add(type);
        type = new ParameterTypeBoolean("early_stopping", "Use early stopping metric for lambda search", true, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "lambda_search", false, true));
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeInt("stopping_rounds", "Early stopping based on convergence of stopping_metric. Stop if simple moving average of length k of the stopping_metric does not improve for k:=stopping_rounds scoring events", 1, 2147483647, 3, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "early_stopping", false, true));
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeDouble("stopping_tolerance", "Relative tolerance for metric-based stopping criterion (stop if relative improvement is not at least this much).", 0.0D, 1.0D, 0.001D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "early_stopping", false, true));
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeDouble("alpha", "The alpha parameter controls the distribution between the L1 (Lasso) and L2(Ridge regression) penalties. A value of 1.0 for alpha represents Lasso, and an alphavalue of 0.0 produces Ridge regression. Default is 0.0 for the L-BFGS solver, else 0.5.", 0.0D, 1.0D, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use_regularization", false, true));
        types.add(type);
        types.add(new ParameterTypeBoolean("standardize", "Standardize numeric columns to have zero mean and unit variance", true, false));
        type = new ParameterTypeBoolean("non-negative_coefficients", "Restrict coefficients (not intercept) to be non-negative.", false, false);
        type.registerDependencyCondition(new OrParameterCondition(this, false, new ParameterCondition[]{new BooleanParameterCondition(this, "use_regularization", false, false), new AndParameterCondition(this, false, new ParameterCondition[]{new BooleanParameterCondition(this, "use_regularization", false, true), new BooleanParameterCondition(this, "lambda_search", false, false)})}));
        types.add(type);
        types.add(new ParameterTypeBoolean("add_intercept", "Include constant term in the model.", true, true));
        type = new ParameterTypeBoolean("compute_p-values", "Request p-values computation, p-values work only with IRLSM solver and no regularization", false, true);
        List<ParameterCondition> pValuesConditions = new ArrayList();
        pValuesConditions.add(new EqualTypeCondition(this, "solver", solverValues, false, new int[]{Solver.IRLSM.ordinal(), Solver.AUTO.ordinal()}));
        if (this.useBetaConstraints) {
            pValuesConditions.add(new BooleanParameterCondition(this, "specify_beta_constraints", false, false));
        }

        pValuesConditions.add(new BooleanParameterCondition(this, "use_regularization", false, false));
        if (extendedFamilyValues != null) {
            pValuesConditions.add(new NonEqualTypeCondition(this, "family", (String[])extendedFamilyValues.toArray(new String[0]), false, new int[]{extendedFamilyValues.indexOf(Family.multinomial.toString())}));
        }

        pValuesConditions.add(new BooleanParameterCondition(this, "non-negative_coefficients", false, false));
        pValuesConditions.add(new BooleanParameterCondition(this, "add_intercept", false, true));
        type.registerDependencyCondition(new AndParameterCondition(this, false, (ParameterCondition[])pValuesConditions.toArray(new ParameterCondition[0])));
        types.add(type);
        type = new ParameterTypeBoolean("remove_collinear_columns", "In case of linearly dependent columns remove some of the dependent columns.", false, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "add_intercept", false, true));
        types.add(type);
        types.add(new ParameterTypeCategory("missing_values_handling", "Handling of missing values. Either Skip or MeanImputation.", (String[])Stream.of(MissingValuesHandling.values()).map(Enum::toString).toArray((x$0) -> {
            return new String[x$0];
        }), MissingValuesHandling.MeanImputation.ordinal(), true));
        types.add(new ParameterTypeInt("max_iterations", "Maximum number of iterations. 0 means no limit.", 0, 2147483647, 0, true));
        return types;
    }

    protected Solver getSolver() throws UndefinedParameterError {
        String solverString = this.getParameterAsString("solver");
        if (solverString.endsWith(" (experimental)")) {
            solverString = solverString.startsWith(Solver.COORDINATE_DESCENT_NAIVE.toString()) ? Solver.COORDINATE_DESCENT_NAIVE.toString() : Solver.COORDINATE_DESCENT.toString();
        }

        return Solver.valueOf(solverString);
    }

    @Override
    protected void performAdditionalChecks() {
        try {
            boolean removeCollinear = this.getParameterAsBoolean("remove_collinear_columns");
            boolean computePValues = this.getParameterAsBoolean("compute_p-values");
            if (computePValues && !removeCollinear) {
                List<QuickFix> fixes = new ArrayList();
                fixes = new ArrayList();
                fixes.add(new ParameterSettingQuickFix(this, "remove_collinear_columns", (String)null, "set_optional_parameter", new Object[]{"remove_collinear_columns"}));
                fixes.add(new ParameterSettingQuickFix(this, "compute_p-values", (String)null, "set_optional_parameter", new Object[]{"compute_p-values"}));
                this.addError(new SimpleProcessSetupError(Severity.WARNING, this.getPortOwner(), fixes, "glm_remove_collinear", new Object[0]));
            }

            if (this.getParameterAsBoolean("use_regularization")) {
                try {
                    if (this.getParameterAsDouble("lambda") == 0.0D) {
                        List<QuickFix> fixes = new ArrayList();
                        fixes = new ArrayList();
                        fixes.add(new ParameterSettingQuickFix(this, "lambda", (String)null, "set_optional_parameter", new Object[]{"lambda"}));
                        this.addError(new SimpleProcessSetupError(Severity.WARNING, this.getPortOwner(), fixes, "no_regularization", new Object[0]));
                    }
                } catch (UndefinedParameterError var5) {
                }
            }

            Solver solver = this.getSolver();
            if (Solver.COORDINATE_DESCENT.equals(solver) || Solver.COORDINATE_DESCENT_NAIVE.equals(solver)) {
                List<QuickFix> fixes = new ArrayList();
                fixes.add(new ParameterSettingQuickFix(this, "solver", (String)null, "set_optional_parameter", new Object[]{"solver"}));
                this.addError(new SimpleProcessSetupError(Severity.WARNING, this.getPortOwner(), fixes, "experimental_solver", new Object[0]));
            }
        } catch (UndefinedParameterError var6) {
        }

    }

    protected Double getLambda() {
        if (!this.getParameterAsBoolean("use_regularization")) {
            return 0.0D;
        } else {
            Double lambda = null;

            try {
                lambda = this.getParameterAsDouble("lambda");
            } catch (UndefinedParameterError var3) {
            }

            return lambda;
        }
    }

    protected Double getAlpha(Double lambda) {
        if (!this.getParameterAsBoolean("use_regularization")) {
            return null;
        } else {
            Double alpha = null;
            if (lambda == null || lambda != 0.0D) {
                try {
                    alpha = this.getParameterAsDouble("alpha");
                } catch (UndefinedParameterError var4) {
                }
            }

            return alpha;
        }
    }

    protected void addCommonParameters(GLMParameters glmParameters, Family family) throws UserError {
        Solver solver = this.getSolver();
        glmParameters._solver = solver;
        boolean useReg = this.getParameterAsBoolean("use_regularization");
        boolean nonNegative;
        if (useReg) {
            nonNegative = this.getParameterAsBoolean("lambda_search");
            glmParameters._lambda_search = nonNegative;
            if (nonNegative) {
                int nlambdas = this.getParameterAsInt("number_of_lambdas");
                glmParameters._nlambdas = nlambdas == 0 ? -1 : nlambdas;
                double minRatio = this.getParameterAsDouble("lambda_min_ratio");
                glmParameters._lambda_min_ratio = minRatio == 0.0D ? -1.0D : minRatio;
                boolean earlyStopping = this.getParameterAsBoolean("early_stopping");
                glmParameters._early_stopping = earlyStopping;
                if (earlyStopping) {
                    glmParameters._stopping_rounds = this.getParameterAsInt("stopping_rounds");
                    glmParameters._stopping_tolerance = this.getParameterAsDouble("stopping_tolerance");
                } else {
                    glmParameters._stopping_rounds = 0;
                }
            }
        }

        glmParameters._standardize = this.getParameterAsBoolean("standardize");
        nonNegative = false;
        if (!this.getParameterAsBoolean("lambda_search") || !this.getParameterAsBoolean("use_regularization")) {
            nonNegative = this.getParameterAsBoolean("non-negative_coefficients");
        }

        glmParameters._non_negative = nonNegative;
        boolean specifyBetaConstraints = false;
        if (this.useBetaConstraints && (!this.getParameterAsBoolean("lambda_search") || !this.getParameterAsBoolean("use_regularization"))) {
            specifyBetaConstraints = this.getParameterAsBoolean("specify_beta_constraints");
        }

        boolean addIntercept = this.getParameterAsBoolean("add_intercept");
        glmParameters._intercept = addIntercept;
        if (!Family.multinomial.equals(family) && (Solver.AUTO.equals(solver) || Solver.IRLSM.equals(solver)) && !specifyBetaConstraints && !nonNegative && !useReg && addIntercept) {
            glmParameters._compute_p_values = this.getParameterAsBoolean("compute_p-values");
        }

        if (addIntercept) {
            glmParameters._remove_collinear_columns = this.getParameterAsBoolean("remove_collinear_columns");
        }

        int maxIter = this.getParameterAsInt("max_iterations");
        glmParameters._max_iterations = maxIter == 0 ? -1 : maxIter;
        glmParameters._missing_values_handling = MissingValuesHandling.valueOf(this.getParameterAsString("missing_values_handling"));
        if (this.getCompatibilityLevel().isAbove(REPRODUCIBLE_VERSION)) {
            glmParameters._fixed_parallelism = this.getParameterAsBoolean("reproducible") ? this.getParameterAsInt("maximum_number_of_threads") : 0;
        }

    }

    @Override
    public base.operators.operator.Model createModel(ExampleSet es, Model<?, ?, ?> model) throws OperatorException {
        return this.createModel(es, (GLMModel)model, this.getCompatibilityLevel().isAbove(USE_DEFAULT_THRESHOLD_VERSION));
    }

    public abstract base.operators.operator.Model createModel(ExampleSet var1, Model<?, ?, ?> var2, boolean var3) throws OperatorException;

    @Override
    public OperatorVersion[] getIncompatibleVersionChanges() {
        return (OperatorVersion[])((OperatorVersion[])ArrayUtils.add(super.getIncompatibleVersionChanges(), USE_DEFAULT_THRESHOLD_VERSION));
    }

    static {
        REPRODUCIBLE_VERSION = NTHREAD_REBALANCING_MAXRUNTIME_VERSION;
        USE_DEFAULT_THRESHOLD_VERSION = new OperatorVersion(7, 5, 0);
    }
}
