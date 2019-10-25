package base.operators.h2o.operator;

import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.h2o.ClusterManager;
import base.operators.h2o.H2OConverter;
import base.operators.h2o.H2OUtils;
import base.operators.operator.Model;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.UserError;
import base.operators.operator.clustering.Cluster;
import base.operators.operator.learner.AbstractLearner;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.ParameterTypeStringCategory;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.BelowOrEqualOperatorVersionCondition;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.OrParameterCondition;
import base.operators.parameter.conditions.ParameterCondition;
import base.operators.tools.LogService;
import hex.ModelBuilder;
import hex.Model.Parameters;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import water.H2O;
import water.Job;
import water.Keyed;
import water.Job.JobCancelledException;
import water.exceptions.H2OIllegalArgumentException;
import water.fvec.Frame;
import water.util.DistributedException;

public abstract class H2OLearner extends AbstractLearner {
    public static final int PROGRESS_CHECK_INTERVAL = 100;
    public static final int PROGRESS_LOG_INTERVAL = 5000;
    public static final String PARAMETER_MAX_RUNTIME_SECS = "max_runtime_seconds";
    public static final String PARAMETER_ADVANCED = "expert_parameters";
    protected static final OperatorVersion NTHREAD_REBALANCING_MAXRUNTIME_VERSION = new OperatorVersion(7, 2, 0);
    public static final String EMPTY_ADVANCED_PARAM = " ";
    private String reproducibleBooleanParameter = null;

    public H2OLearner(OperatorDescription description, String reproducibleParameter) {
        super(description);
        this.reproducibleBooleanParameter = reproducibleParameter;
    }

    protected boolean isEmptyAdvancedValue(String value) {
        return value != null && " ".trim().equals(value.trim());
    }

    @Override
    public Model learn(ExampleSet exampleSet) throws OperatorException {
        ClusterManager.startCluster();
        Parameters params = this.buildModelSpecificParameters(exampleSet);
        this.buildGenericModelParameters(params);
        Frame frame = null;
        hex.Model<?, ?, ?> model = null;
        String modelName = ClusterManager.generateModelName(this.getName());

        String secondPart;
        try {
            String msg;
            try {
                frame = H2OConverter.toH2OFrame(exampleSet, ClusterManager.generateFrameName(this.getName()), false, true, this);
                Attributes attributes = exampleSet.getAttributes();
                params._train = frame._key;
                params._response_column = attributes.getLabel().getName();
                if (attributes.getWeight() != null) {
                    params._weights_column = attributes.getWeight().getName();
                }

                ModelBuilder<? extends hex.Model<?, ?, ?>, ?, ?> builder = this.createModelBuilder(params, modelName);
                Job<? extends hex.Model<?, ?, ?>> job = builder.trainModel();
                this.displayProgess(job);
                AtomicBoolean stopRequest = new AtomicBoolean(false);
//                H2OUtils.addProcessStopListener(this, stopRequest, job);
//                this.checkForStop();

                try {
                    model = (hex.Model)job.get();
                } catch (DistributedException | JobCancelledException var19) {
                    if (!stopRequest.get()) {
                        Throwable t = var19;
                        if (var19 instanceof DistributedException && var19.getCause() != null) {
                            t = var19.getCause();
                        }

                        throw new OperatorException("Unexpected H2O job stop: " + ((Throwable)t).getMessage(), (Throwable)t);
                    }

                    throw new ProcessStoppedException(this);
                }

                this.checkForStop();
                if (model != null) {
                    this.createWeights(exampleSet, model);
                    this.createThreshold(model);
                    Model var28 = this.createModel(exampleSet, model);
                    return var28;
                }

                secondPart = null;
                job.stop();
            } catch (H2OIllegalArgumentException var20) {
                msg = var20.getMessage();
                if (msg.contains(modelName)) {
                    String firstPart = msg.substring(0, msg.indexOf(modelName));
                    int index = msg.indexOf(": ", msg.indexOf(modelName));
                    if (index != -1 && index + 2 <= msg.length()) {
                        secondPart = msg.substring(index + 2, msg.length());
                        msg = firstPart + secondPart;
                    }
                }

                var20.printStackTrace();
                ClusterManager.getH2OLogger().info("H2O training error: ", var20);
                throw new UserError(this, var20, "h2o.train_error", new Object[]{msg});
            } catch (ProcessStoppedException var21) {
                throw var21;
            } catch (OperatorException var22) {
                throw var22;
            } catch (Throwable var23) {
                var23.printStackTrace();
                ClusterManager.getH2OLogger().info("H2O training error: ", var23);
                msg = var23.getMessage();
                if (msg != null) {
                    if (msg.length() > 230) {
                        msg = msg.substring(0, 230) + ". . .";
                    }

                    throw new UserError(this, var23, "h2o.train_error", new Object[]{msg});
                }

                throw new UserError(this, var23, "h2o.train_error", new Object[]{var23.toString()});
            }
        } finally {
            this.cleanup();
            ClusterManager.removeFromCluster(new Keyed[]{frame});
            H2O.orderlyShutdown();
        }
        return null;
    }

    protected void cleanup() {
    }

    protected abstract ModelBuilder<? extends hex.Model<?, ?, ?>, ?, ?> createModelBuilder(Parameters var1, String var2);

    public abstract Model createModel(ExampleSet var1, hex.Model<?, ?, ?> var2) throws OperatorException;

    protected void createWeights(ExampleSet es, hex.Model<?, ?, ?> model) throws OperatorException {
    }

    protected void createThreshold(hex.Model<?, ?, ?> model) throws OperatorException {
    }

    protected abstract Parameters buildModelSpecificParameters(ExampleSet var1) throws UndefinedParameterError, UserError;

    protected void buildGenericModelParameters(Parameters params) throws UndefinedParameterError {
        boolean setMaxRuntimeSecs = this.reproducibleBooleanParameter != null ? !this.getParameterAsBoolean(this.reproducibleBooleanParameter) : true;
        if (this.getCompatibilityLevel().isAtMost(NTHREAD_REBALANCING_MAXRUNTIME_VERSION) || setMaxRuntimeSecs) {
            params._max_runtime_secs = this.getParameterAsDouble("max_runtime_seconds");
        }

        if (this.getCompatibilityLevel().isAtMost(NTHREAD_REBALANCING_MAXRUNTIME_VERSION)) {
            params._nthread_rebalancing = false;
        }

    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type = new ParameterTypeInt("max_runtime_seconds", "Maximum allowed runtime in seconds for model training. Use 0 to disable.", 0, 2147483647, 0, true);
        if (this.reproducibleBooleanParameter != null) {
            type.registerDependencyCondition(new OrParameterCondition(this, false, new ParameterCondition[]{new BelowOrEqualOperatorVersionCondition(this, NTHREAD_REBALANCING_MAXRUNTIME_VERSION), new BooleanParameterCondition(this, this.reproducibleBooleanParameter, false, false)}));
        }

        types.add(type);
        String[] expertParamsArray = this.getAdvancedParametersArray();
        if (expertParamsArray != null) {
            ParameterType advancedParameters = new ParameterTypeList("expert_parameters", "Advanced parameters that can be set.", new ParameterTypeStringCategory("parameter name", "The name of the parameter", expertParamsArray), new ParameterTypeString("value", "The value of the parameter"), true);
            types.add(advancedParameters);
        }

        this.postProcessParameterTypes(types);
        return types;
    }

    protected void postProcessParameterTypes(List<ParameterType> types) {
    }

    protected abstract String[] getAdvancedParametersArray();

    protected List<String[]> getAdvancedParametersDefaults() {
        return new ArrayList();
    }

    protected void displayProgess(Job<?> job) {
        this.displayProgess(job, 100, 5000);
    }

    protected void displayProgess(final Job<?> job, final int checkInterval, final int logInterval) {
        Objects.requireNonNull(job);
        final NumberFormat percentFormat = NumberFormat.getPercentInstance();

        long jobTotal;
        for(jobTotal = job._work; jobTotal > 2147483647L; jobTotal >>= 1) {
        }

        final int progressTotal = (int)jobTotal;
        this.getProgress().setCheckForStop(false);
        (new Thread(new Runnable() {
            @Override
            public void run() {
                H2OLearner.this.getProgress().setTotal(progressTotal);
                long start = System.currentTimeMillis();

                while(job.isRunning()) {
                    try {
                        Thread.sleep((long)checkInterval);
                    } catch (InterruptedException var8) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    float h2oProgress = job.progress();
                    String h2oMessage = job.progress_msg();
                    if (System.currentTimeMillis() - start > (long)logInterval) {
                        if (h2oMessage != null) {
                            LogService.getRoot().info(String.format("H2O: %s - %s", percentFormat.format((double)h2oProgress), h2oMessage));
                        }

                        start = System.currentTimeMillis();
                    }

                    try {
                        H2OLearner.this.getProgress().setCompleted((int)((float)progressTotal * h2oProgress));
                    } catch (ProcessStoppedException var7) {
                    }
                }

                try {
                    H2OLearner.this.getProgress().setCompleted(progressTotal);
                } catch (ProcessStoppedException var6) {
                }

                Thread.currentThread().interrupt();
            }
        }, "H2O-progress-" + job._result)).start();
    }

    public double getDoubleValue(String key, String value) throws UserError {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException var4) {
            throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, key});
        }
    }

    public float getFloatValue(String key, String value) throws UserError {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException var4) {
            throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, key});
        }
    }

    public long getLongValue(String key, String value) throws UserError {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException var4) {
            throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, key});
        }
    }

    public int getIntegerValue(String key, String value) throws UserError {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException var4) {
            throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, key});
        }
    }

    public boolean getBooleanValue(String key, String value) throws UserError {
        if (value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("false")) {
            return false;
        } else {
            throw new UserError(this, "h2o.wrong_advanced_value", new Object[]{value, key});
        }
    }

    @Override
    public OperatorVersion[] getIncompatibleVersionChanges() {
        return new OperatorVersion[]{NTHREAD_REBALANCING_MAXRUNTIME_VERSION};
    }
}
