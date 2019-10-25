package base.operators.h2o.model;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.set.HeaderExampleSet;
import base.operators.h2o.ClusterManager;
import base.operators.h2o.H2OConverter;
import base.operators.h2o.H2ONativeObject;
import base.operators.h2o.H2OUtils;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import hex.Model;
import hex.tree.SharedTreeModel;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import water.DKV;
import water.Job;
import water.Key;
import water.Keyed;
import water.fvec.Frame;

public class H2ONativeModelObject extends H2ONativeObject {
    private static final long serialVersionUID = 1L;

    public H2ONativeModelObject(Keyed<?> h2oObject) throws OperatorException {
        super(h2oObject);
    }

    public H2ONativeModelObject(String version, String key, byte[] binary) {
        super(version, key, binary);
    }

    public H2ONativeModelObject(String version, String key, List<byte[]> compressedJsons, List<String> jsonClasses, Operator operator, HeaderExampleSet trainingHeader) {
        super(version, key, compressedJsons, jsonClasses);
    }

    public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel, Operator operator, HeaderExampleSet trainingHeader) throws OperatorException {
        Frame exampleFrame = null;
        Frame predictionFrame = null;

        try {
            ClusterManager.startCluster();
            Model<?, ?, ?> model = (Model)this.getH2OObject();
            String name = this.getClass().getSimpleName();
            if (operator != null) {
                name = operator.getName();
            }

            exampleFrame = H2OConverter.toH2OFrame(exampleSet, ClusterManager.generateFrameName(name), false, false, operator, trainingHeader);
            String predictionFrameName = ClusterManager.generateFrameName(name);
            Job<Frame> job = new Job(Key.make(predictionFrameName), Frame.class.getName(), (String)null);
            DKV.put(job);
            AtomicBoolean stopRequest = new AtomicBoolean(false);
            H2OUtils.addProcessStopListener(operator, stopRequest, job);

            try {
                predictionFrame = model.score(exampleFrame, predictionFrameName, job);
                H2OConverter.fillPredictionAttributes(exampleSet, predictionFrame, trainingHeader);
            } catch (Job.JobCancelledException var17) {
                if (!stopRequest.get()) {
                    throw new OperatorException("Unexpected H2O job stop: ", var17);
                }
            }

            this.postProcessModel(model);
        } finally {
            ClusterManager.removeFromCluster(new Keyed[]{exampleFrame, predictionFrame});
        }

        return exampleSet;
    }

    protected void postProcessModel(Model<?, ?, ?> model) {
    }

    public String[] getFeatureNames() throws OperatorException {
        String[] allNames = ((Model)this.getH2OObject())._output._names;
        int numberOfFeatures = ((Model)this.getH2OObject())._output.nfeatures();
        return (String[])Arrays.copyOf(allNames, numberOfFeatures);
    }

    @Override
    protected void computeCompressedJsonsAndJsonClasses() throws OperatorException {
        super.computeCompressedJsonsAndJsonClasses();

        try {
            int var3;
            if (this.h2oObject instanceof Model) {
                Field _model_metrics = Model.Output.class.getDeclaredField("_model_metrics");
                _model_metrics.setAccessible(true);
                Key[] var2 = (Key[])((Key[])_model_metrics.get(((Model)this.h2oObject)._output));
                var3 = var2.length;

                for(int var4 = 0; var4 < var3; ++var4) {
                    Key<?> modelMetricsKey = var2[var4];
                    Keyed<?> modelMetrics = modelMetricsKey.get();
                    String json = ClusterManager.toJson(modelMetrics);
                    this.compressedJsons.add(ClusterManager.compress(json));
                    this.jsonClasses.add(modelMetrics.getClass().getName());
                }
            }

            if (this.h2oObject instanceof SharedTreeModel) {
                Key[][] var12 = ((SharedTreeModel.SharedTreeOutput)((SharedTreeModel)this.h2oObject)._output)._treeKeys;
                int var13 = var12.length;

                for(var3 = 0; var3 < var13; ++var3) {
                    Key<?>[] trees = var12[var3];
                    if (trees != null) {
                        Key[] var15 = trees;
                        int var16 = trees.length;

                        for(int var17 = 0; var17 < var16; ++var17) {
                            Key<?> treeKey = var15[var17];
                            if (treeKey != null) {
                                Keyed<?> tree = treeKey.get();
                                String json = ClusterManager.toJson(tree);
                                this.compressedJsons.add(ClusterManager.compress(json));
                                this.jsonClasses.add(tree.getClass().getName());
                            }
                        }
                    }
                }
            }

        } catch (Exception var11) {
            throw new OperatorException("H2O object can't be serialized to JSON", var11);
        }
    }
}
