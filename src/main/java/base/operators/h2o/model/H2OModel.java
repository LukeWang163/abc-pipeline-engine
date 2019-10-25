package base.operators.h2o.model;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.ExampleSetUtilities;
import base.operators.example.set.HeaderExampleSet;
import base.operators.example.table.NominalMapping;
import base.operators.h2o.H2ONativeObject;
import base.operators.h2o.H2OUtils;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.learner.PredictionModel;
import base.operators.tools.LogService;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class H2OModel extends PredictionModel {
    private static final long serialVersionUID = 1L;
    private String version;
    private final String key;
    private final byte[] binary;
    private final List<byte[]> compressedJsons;
    private final List<String> jsonClasses;
    private String[] names;
    private String modelString;
    private final String[] warnings;
    private transient volatile H2ONativeModelObject h2oNativeModel;
    private boolean preferClusterScoring = false;

    protected H2OModel(ExampleSet trainingExampleSet, H2ONativeModelObject h2oNativeModel, String modelString, String[] warnings, ExampleSetUtilities.SetsCompareOption sizeCompareOperator, ExampleSetUtilities.TypesCompareOption typeCompareOperator) throws OperatorException {
        super(trainingExampleSet, sizeCompareOperator, typeCompareOperator);
        this.h2oNativeModel = h2oNativeModel;
        this.version = h2oNativeModel.version;
        this.key = h2oNativeModel.key;
        this.binary = h2oNativeModel.getBinary();
        this.compressedJsons = h2oNativeModel.getCompressedJsons();
        this.jsonClasses = h2oNativeModel.getJsonClasses();
        this.names = h2oNativeModel.getFeatureNames();
        this.modelString = modelString;
        this.warnings = warnings;
    }

    @Override
    public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
        H2ONativeModelObject h2oNativeModel;
        return this.preferClusterScoring && (h2oNativeModel = this.getH2ONativeModelIfPossible()) != null ? h2oNativeModel.performPrediction(exampleSet, predictedLabel, this.getOperator(), this.getTrainingHeader()) : this.performCustomPrediction(exampleSet, predictedLabel, this.getOperator(), this.getTrainingHeader());
    }

    public ExampleSet performCustomPrediction(ExampleSet exampleSet, Attribute predictedLabel, Operator operator, HeaderExampleSet trainingHeader) throws OperatorException {
        List<Attribute> dataAttributes = prepareDataAttributes(exampleSet, this.names);
        Attribute[] predictionAttributes = this.preparePredictionAttributes(exampleSet, trainingHeader);
        Iterator r = exampleSet.iterator();

        while(r.hasNext()) {
            Example example = (Example)r.next();
            double[] data = toH2ORow(example, dataAttributes);
            double[] predictions = new double[predictionAttributes.length];
            this.score0(data, predictions);
            fillPredictionAttributes(example, predictions, predictionAttributes);
        }

        return exampleSet;
    }

    public abstract void score0(double[] var1, double[] var2) throws OperatorException;

    public static List<Attribute> prepareDataAttributes(ExampleSet exampleSet, String[] names) {
        List<Attribute> dataAtributes = new ArrayList();
        Attributes attributes = exampleSet.getAttributes();
        String[] var4 = names;
        int var5 = names.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            String name = var4[var6];
            Attribute attribute = attributes.get(name);
            dataAtributes.add(attribute);
        }

        return dataAtributes;
    }

    public static double[] toH2ORow(Example example, List<Attribute> dataAttributes) throws UserError {
        double[] data = new double[dataAttributes.size()];

        for(int i = 0; i < dataAttributes.size(); ++i) {
            data[i] = example.getValue((Attribute)dataAttributes.get(i));
        }

        return data;
    }

    public Attribute[] preparePredictionAttributes(ExampleSet exampleSet, HeaderExampleSet trainingHeader) throws OperatorException {
        Attribute trainingLabel = trainingHeader.getAttributes().getLabel();
        int predictionAttributeSize = trainingLabel.isNominal() ? trainingLabel.getMapping().size() + 1 : 1;
        Attribute[] predictionAttributes = new Attribute[predictionAttributeSize];
        Attributes attributes = exampleSet.getAttributes();
        predictionAttributes[0] = attributes.getPredictedLabel();
        NominalMapping mapping = null;
        if (predictionAttributes[0].isNominal()) {
            mapping = trainingLabel.getMapping();
            predictionAttributes[0].setMapping((NominalMapping)mapping.clone());
        }

        for(int col = 1; col < predictionAttributeSize; ++col) {
            String confidenceAttributeName = "confidence(" + (String)mapping.getValues().get(col - 1) + ")";
            Attribute attribute = attributes.get(confidenceAttributeName);
            if (attribute == null) {
                throw new OperatorException("Confidence attribute not found! Expected name: " + confidenceAttributeName);
            }

            predictionAttributes[col] = attribute;
        }

        return predictionAttributes;
    }

    public static void fillPredictionAttributes(Example example, double[] prediction, Attribute[] predictionAttributes) throws OperatorException {
        for(int col = 0; col < prediction.length; ++col) {
            Attribute predictionAttribute = predictionAttributes[col];
            example.setValue(predictionAttribute, prediction[col]);
        }

    }

    public H2ONativeModelObject getH2ONativeModelIfPossible() {
        if (this.h2oNativeModel == null) {
            synchronized(this) {
                if (this.h2oNativeModel == null) {
                    String currentVersion = null;

                    try {
                        Class<?> buildVersionClass = Class.forName("water.init.BuildVersion");
                        Object buildVersion = buildVersionClass.newInstance();
                        Method projectVersionMethod = buildVersionClass.getDeclaredMethod("projectVersion");
                        currentVersion = (String)projectVersionMethod.invoke(buildVersion);
                    } catch (Throwable var7) {
                        var7.printStackTrace();
                        LogService.getRoot().info("H2O is not available, native H2O model cannot be restored");
                        return null;
                    }

                    if (H2OUtils.isVersionCompatible(this.version, currentVersion)) {
                        this.h2oNativeModel = new H2ONativeModelObject(this.version, this.key, this.binary);
                    } else {
                        LogService.getRoot().info("Native H2O model was built by a different H2O version: " + this.version + " that cannot be restored to the current version: " + currentVersion);
                    }
                }
            }
        }

        return this.h2oNativeModel;
    }

    public String getH2OName() {
        H2ONativeObject o = this.getH2ONativeModelIfPossible();
        if (o == null) {
            return null;
        } else {
            try {
                o.getH2OObject();
            } catch (OperatorException var3) {
                return null;
            }

            return this.key;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.warnings != null && this.warnings.length > 0) {
            sb.append("Warning:\n");
            String[] var2 = this.warnings;
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                String warning = var2[var4];
                sb.append(warning).append("\n");
            }

            sb.append("\n");
        }

        return sb.toString() + this.modelString;// + "\nH2O version: " + this.version;
    }

    @Override
    public void setParameter(String name, Object object) throws OperatorException {
        if (name.equals("prefer_cluster_scoring")) {
            String value = (String)object;
            this.preferClusterScoring = Boolean.parseBoolean(value);
        }

    }

    public String[] getWarnings() {
        return this.warnings;
    }
}
