package base.operators.operator.nlp.dependency.model;

import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ModelMetaData;
import com.alibaba.fastjson.JSONObject;
import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.ExampleSetUtilities;
import base.operators.example.set.HeaderExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.*;
import base.operators.tools.Ontology;
import nlp.core.parser.parser.shiftreduce.ShiftReduceParser;
import nlp.core.parser.process.*;
import nlp.core.parser.tagger.maxent.MaxentTagger;
import nlp.core.parser.trees.*;

import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

public class ShiftReduceModel extends ResultObjectAdapter implements Model {

    public MaxentTagger tagger;
    public ShiftReduceParser parser;
    public ExampleSet trainExampleSet;
    public String language;
    private HeaderExampleSet headerExampleSet;
    private Operator operator = null;
    private ModelMetaData modelMetaData;


    public ShiftReduceModel(ShiftReduceParser parser, MaxentTagger tagger,ExampleSet trainExampleSet, String language) {
        this.tagger = tagger;
        this.parser = parser;
        this.trainExampleSet = trainExampleSet;
        this.language = language;
        this.modelMetaData = null;
        if (this.trainExampleSet != null) {
            this.headerExampleSet = new HeaderExampleSet(this.trainExampleSet);
            this.modelMetaData = new ModelMetaData(new ExampleSetMetaData(this.trainExampleSet));
        }
    }

    public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
        ExampleSet exampleSetCopy = (ExampleSet) exampleSet.clone();
        Attribute predictedLabel = createPredictionAttributes(exampleSetCopy);
        ExampleSet result = performPrediction(exampleSetCopy, predictedLabel);
        return result;
    }

    public ExampleSet performPrediction (ExampleSet examples, Attribute predictedAttribute){
        if(examples.getAttributes().createRegularAttributeArray().length!=1){
            throw new IllegalArgumentException("Only allowed one regular attribute!");
        }
        for (int jj = 0; jj < examples.size(); jj++) {
            Example example = examples.getExample(jj);
            for (Attribute attribute : example.getAttributes()) {
                String docContent = example.getValueAsString(attribute);
                //判空处理
                String sreal = docContent.replaceAll("\r\n","").replaceAll("\\s+", " ");
                if("".equals(sreal) || docContent==null){//如果s为空或者
                    example.setPredictedLabel(predictedAttribute.getMapping().mapString(sreal));
                    continue;
                }
                JSONObject result = predictImpl(docContent);
                example.setPredictedLabel(predictedAttribute.getMapping().mapString(result.toString()));
            }
        }
        // Create and return a Classification object
        return examples;
    }


    public JSONObject predictImpl(String text) {
        JSONObject result = new JSONObject();
        DocumentPreprocessor tokenizer;
        Iterator var5;
        List sentence;
        List tagged;
        Tree tree;
        TreebankLanguagePack tlp;
        GrammaticalStructureFactory gsf;
        GrammaticalStructure gs;
        List tdl;
        tokenizer = new DocumentPreprocessor(new StringReader(text));
        var5 = tokenizer.iterator();
        while(var5.hasNext()) {
            sentence = (List) var5.next();
            tagged = tagger.tagSentence(sentence);
            tree = parser.apply(tagged);
            result.put("syntactic", tree.pennString().replaceAll("\r\n","").replaceAll("\\s+", " "));
            tlp = parser.treebankLanguagePack();
            gsf = tlp.grammaticalStructureFactory();
            gs = gsf.newGrammaticalStructure(tree);
            tdl = gs.typedDependenciesCCprocessed();
            result.put("dependence", tdl.toString());
        }
        return result;
    }

    protected void checkCompatibility(ExampleSet exampleSet) throws OperatorException {
        ExampleSet trainingHeaderSet = getTrainingHeader();
        // check given constraints (might throw an UserError)
        ExampleSetUtilities.checkAttributesMatching(getOperator(), trainingHeaderSet.getAttributes(),
                exampleSet.getAttributes(), compareSetSize, compareDataType);
        // check number of attributes
        if (exampleSet.getAttributes().size() != trainingHeaderSet.getAttributes().size()) {
            logWarning("The number of regular attributes of the given example set does not fit the number of attributes of the training example set, training: "
                    + trainingHeaderSet.getAttributes().size() + ", application: " + exampleSet.getAttributes().size());
        } else {
            // check order of attributes
            Iterator<Attribute> trainingIt = trainingHeaderSet.getAttributes().iterator();
            Iterator<Attribute> applyIt = exampleSet.getAttributes().iterator();
            while (trainingIt.hasNext() && applyIt.hasNext()) {
                if (!trainingIt.next().getName().equals(applyIt.next().getName())) {
                    logWarning("The order of attributes is not equal for the training and the application example set. This might lead to problems for some models.");
                    break;
                }
            }
        }

        // check if all training attributes are part of the example set and have the same value
        // types and values
        for (Attribute trainingAttribute : trainingHeaderSet.getAttributes()) {
            String name = trainingAttribute.getName();
            Attribute attribute = exampleSet.getAttributes().getRegular(name);
            if (attribute == null) {
                logWarning("The given example set does not contain a regular attribute with name '" + name
                        + "'. This might cause problems for some models depending on this particular attribute.");
            } else {
                if (trainingAttribute.getValueType() != attribute.getValueType()) {
                    logWarning("The value types between training and application differ for attribute '" + name
                            + "', training: " + Ontology.VALUE_TYPE_NAMES[trainingAttribute.getValueType()]
                            + ", application: " + Ontology.VALUE_TYPE_NAMES[attribute.getValueType()]);
                } else {
                    // check nominal values
                    if (trainingAttribute.isNominal()) {
                        if (trainingAttribute.getMapping().size() != attribute.getMapping().size()) {
                            logWarning("The number of nominal values is not the same for training and application for attribute '"
                                    + name
                                    + "', training: "
                                    + trainingAttribute.getMapping().size()
                                    + ", application: "
                                    + attribute.getMapping().size());
                        } else {
                            for (String v : trainingAttribute.getMapping().getValues()) {
                                int trainingIndex = trainingAttribute.getMapping().getIndex(v);
                                int applicationIndex = attribute.getMapping().getIndex(v);
                                if (trainingIndex != applicationIndex) {
                                    logWarning("The internal nominal mappings are not the same between training and application for attribute '"
                                            + name + "'. This will probably lead to wrong results during model application.");
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * This method creates prediction attributes like the predicted label and confidences if needed.
     */
    protected Attribute createPredictionAttributes(ExampleSet exampleSet) {
        // create and add prediction attribute
        Attribute predictedLabel = AttributeFactory.createAttribute(Attributes.PREDICTION_NAME, Ontology.STRING);
        predictedLabel.clearTransformations();
        ExampleTable table = exampleSet.getExampleTable();
        table.addAttribute(predictedLabel);
        exampleSet.getAttributes().setPredictedLabel(predictedLabel);

        return predictedLabel;
    }


    /**
     * This parameter specifies the data types at which the model can be applied on.
     */
    private ExampleSetUtilities.TypesCompareOption compareDataType;

    /**
     * This parameter specifies the relation between the training {@link ExampleSet} and the input
     * {@link ExampleSet} which is needed to apply the model on the input {@link ExampleSet}.
     */
    private ExampleSetUtilities.SetsCompareOption compareSetSize;


    @Override
    public HeaderExampleSet getTrainingHeader() {
        return this.headerExampleSet;
    }

    @Override
    public ModelMetaData getModelMetaData() {
        return modelMetaData;
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public void setParameter(String key, Object value) throws OperatorException {
        throw new UnsupportedApplicationParameterError(null, getName(), key);
    }

    @Override
    public boolean isUpdatable() {
        return false;
    }

    @Override
    public void updateModel(ExampleSet updateExampleSet) throws OperatorException {
        throw new UserError(null, 135, getClass().getName());
    }

    @Override
    public boolean isInTargetEncoding() {
        return false;
    }

    @Override
    public String getName() {
        return "ShiftReduce Parser Model";
    }

}
