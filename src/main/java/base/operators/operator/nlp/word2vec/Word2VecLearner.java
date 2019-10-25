package base.operators.operator.nlp.word2vec;

import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.word2vec.core.Learn;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Word2VecLearner extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort modelOutput = getOutputPorts().createPort("model");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String IS_CBOW = "is_cbow";
    public static final String LAYER_SIZE = "layer_size";
    public static final String WINDOW = "window";
    public static final String ALPHA = "alpha";
    public static final String SAMPLE = "sample";


    public Word2VecLearner(OperatorDescription description) {
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ATTRIBUTE_NAME)));

    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        String doc_attribute_name = "";
        boolean isCbow = true;
        int layerSize = 0;
        int window = 0;
        double alpha = 0.025;
        double sample = 0.001;
        try {
            doc_attribute_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
            isCbow = getParameterAsBoolean(IS_CBOW);
            layerSize = getParameterAsInt(LAYER_SIZE);
            window = getParameterAsInt(WINDOW);
            alpha = getParameterAsDouble(ALPHA);
            sample = getParameterAsDouble(SAMPLE);
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }

        Attributes attributes = exampleSet.getAttributes();

        List<String> trainText = new ArrayList<>();
        for (Example example: exampleSet){
            trainText.add(example.getValueAsString(attributes.get(doc_attribute_name)));
        }
        Learn learnModel = new Learn(isCbow, layerSize, window, alpha, sample);
        try {
            learnModel.learnFile(trainText);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Word2Vec word2Vec = new Word2Vec(exampleSet, learnModel);
        modelOutput.deliver(word2Vec);
        exampleSetOutput.deliver(exampleSet);
    }

    public boolean supportsCapability(OperatorCapability capability){
        switch (capability) {
            case POLYNOMINAL_ATTRIBUTES:
            case WEIGHTED_EXAMPLES:
            case UPDATABLE:
            case MISSING_VALUES:
                return true;
            default:
                return false;
        }
    }
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the attribute to convert.", exampleSetInput,
                false));
        types.add(new ParameterTypeBoolean(IS_CBOW, "The language model", true, false));
        types.add(new ParameterTypeInt(LAYER_SIZE, "The dimension of word feature.", 1, Integer.MAX_VALUE, 50, false));
        types.add(new ParameterTypeInt(WINDOW, "Sliding window size.", 1, Integer.MAX_VALUE, 5, false));
        types.add(new ParameterTypeDouble(ALPHA, "Start learning rate.", 0, Integer.MAX_VALUE, 0.025, false));
        types.add(new ParameterTypeDouble(SAMPLE, "Threshold of Random Downsampling of High Frequency Words.", 0.00001, 1, 0.001, false));

        return types;
    }
    public Class<? extends Word2Vec> getModelClass() {
        return Word2Vec.class;
    }


}
