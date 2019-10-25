package base.operators.operator.nlp.sentiment;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.sentiment.gradeSentiment.SentimentImpl;
import base.operators.operator.nlp.sentiment.gradeSentiment.dictionary.DictionaryItems;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SentimentOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort sentimentExampleSetInput = getInputPorts().createPort("sentiment dictionary");
    private InputPort degreeExampleSetInput = getInputPorts().createPort("degree dictionary");
    private InputPort negatorExampleSetInput = getInputPorts().createPort("negator dictionary");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String LANGUAGE = "language";
    public static String[] LANGUAGES = {"Chinese","English"};

    public static final String DICTIONARY = "dictionary";
    public static String[] DICTIONARYS = {"system dictionary","user dictionary"};

    public static final String SENTIMENT_WORD_ATTRIBUTE_NAME = "sentiment_word_attribute_name";
    public static final String SENTIMENT_WEIGHT_ATTRIBUTE_NAME = "sentiment_weight_attribute_name";

    public static final String DEGREE_WORD_ATTRIBUTE_NAME = "degree_word_attribute_name";
    public static final String DEGREE_WEIGHT_ATTRIBUTE_NAME = "degree_weight_attribute_name";

    public static final String NEGATOR_ATTRIBUTE_NAME = "negator_attribute_name";

    public SentimentOperator(OperatorDescription description){
        super(description);
        try {
            String[] selectedColumnArray = getParameterAsString(DOC_ATTRIBUTE_NAME).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
            for(String per : selectedColumnArray){
                if(!"".equals(per)){
                    exampleSetInput.addPrecondition(
                            new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                                    this, per)));
                }

            }
            if(sentimentExampleSetInput.isConnected()){
                sentimentExampleSetInput.addPrecondition(
                        new AttributeSetPrecondition(sentimentExampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                                this, SENTIMENT_WORD_ATTRIBUTE_NAME,SENTIMENT_WEIGHT_ATTRIBUTE_NAME)));
            }
            if(degreeExampleSetInput.isConnected()){
                degreeExampleSetInput.addPrecondition(
                        new AttributeSetPrecondition(degreeExampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                                this, DEGREE_WORD_ATTRIBUTE_NAME,DEGREE_WEIGHT_ATTRIBUTE_NAME)));
            }

            if(negatorExampleSetInput.isConnected()){
                negatorExampleSetInput.addPrecondition(
                        new AttributeSetPrecondition(negatorExampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                                this, NEGATOR_ATTRIBUTE_NAME)));
            }

        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }
    }

    @Override
    public void doWork() throws OperatorException {
        String doc_attribute_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
        int language = getParameterAsInt(LANGUAGE);
        int dictionary = getParameterAsInt(DICTIONARY);
        String senti_name = getParameterAsString(SENTIMENT_WORD_ATTRIBUTE_NAME);
        String senti_weight_name = getParameterAsString(SENTIMENT_WEIGHT_ATTRIBUTE_NAME);
        String degree_name  =  getParameterAsString(DEGREE_WORD_ATTRIBUTE_NAME);
        String degree_weight_name = getParameterAsString(DEGREE_WEIGHT_ATTRIBUTE_NAME);
        String negator_name = getParameterAsString(NEGATOR_ATTRIBUTE_NAME);

        DictionaryItems dt;
        if(dictionary==0&&!sentimentExampleSetInput.isConnected()&&!degreeExampleSetInput.isConnected()&&!negatorExampleSetInput.isConnected()){
            dt = new DictionaryItems();
        }else if(dictionary==1&&sentimentExampleSetInput.isConnected()&&degreeExampleSetInput.isConnected()&&negatorExampleSetInput.isConnected()){
            ExampleSet sentiExampleSet = (ExampleSet) sentimentExampleSetInput.getData(ExampleSet.class).clone();
            ExampleSet degreeExampleSet = (ExampleSet) degreeExampleSetInput.getData(ExampleSet.class).clone();
            ExampleSet negatorExampleSet = (ExampleSet) negatorExampleSetInput.getData(ExampleSet.class).clone();
            Map<String, Double> sentimentDic = new HashMap<>();
            Map<String, Double> degreeDic = new HashMap<>();
            List<String> negatorDic = new ArrayList<>();
            for(Example example : sentiExampleSet){
                sentimentDic.put(example.getValueAsString(sentiExampleSet.getAttributes().get(senti_name)), example.getValue(sentiExampleSet.getAttributes().get(senti_weight_name)));
            }
            for(Example example : degreeExampleSet){
                degreeDic.put(example.getValueAsString(degreeExampleSet.getAttributes().get(degree_name)), example.getValue(degreeExampleSet.getAttributes().get(degree_weight_name)));
            }
            for(Example example : negatorExampleSet){
                negatorDic.add(example.getValueAsString(negatorExampleSet.getAttributes().get(negator_name)));
            }
            dt = new DictionaryItems(sentimentDic, degreeDic, negatorDic);
        }else{
            throw new OperatorException("Input inconsistency");
        }
        SentimentImpl simpl = new SentimentImpl(dt);
        String[] selectedColumnArray = doc_attribute_name.split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        ExampleTable table = exampleSet.getExampleTable();
        Attributes attributes = exampleSet.getAttributes();
        for (int j = 0; j < selectedColumnArray.length; j++) {
            if(!"".equals(selectedColumnArray[j])){
                Attribute result = AttributeFactory.createAttribute(selectedColumnArray[j]+"_score", Ontology.NUMERICAL);
                table.addAttribute(result);
                attributes.addRegular(result);
            }

        }
        for(Example example : exampleSet){
            for(String col : selectedColumnArray){
                if(!"".equals(col)){
                    String text = example.getValueAsString(attributes.get(col));
                    //情感得分计算
                    Double score = 0.0;
                    if(0==language){
                        score = simpl.computeScoreChinese(String.valueOf(text));
                    }else if(1==language){
                        score = simpl.computeScoreEnglish(String.valueOf(text));
                    }
                    example.setValue(attributes.get(col+"_score"), score);
                }
            }
        }
        exampleSetOutput.deliver(exampleSet);
    }


    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttributes(DOC_ATTRIBUTE_NAME, "The name of the attribute to train.",
                exampleSetInput, false));
        types.add(new ParameterTypeCategory(LANGUAGE, "The language of text.",
                LANGUAGES, 0, false));

        types.add(new ParameterTypeCategory(DICTIONARY, "The source of dictionary.",
                DICTIONARYS, 0, false));
        ParameterType type = new ParameterTypeAttribute(SENTIMENT_WORD_ATTRIBUTE_NAME, "The name of the attribute of sentiment word.", sentimentExampleSetInput,
                true);
        type.registerDependencyCondition(new EqualStringCondition(this, DICTIONARY, false, "user dictionary"));
        types.add(type);
        type = new ParameterTypeAttribute(SENTIMENT_WEIGHT_ATTRIBUTE_NAME, "The name of the attribute of sentiment word.", sentimentExampleSetInput,
                true);
        type.registerDependencyCondition(new EqualStringCondition(this, DICTIONARY, false, "user dictionary"));
        types.add(type);
        type = new ParameterTypeAttribute(DEGREE_WORD_ATTRIBUTE_NAME, "The name of the attribute of sentiment word.", sentimentExampleSetInput,
                true);
        type.registerDependencyCondition(new EqualStringCondition(this, DICTIONARY, false, "user dictionary"));
        types.add(type);
        type = new ParameterTypeAttribute(DEGREE_WEIGHT_ATTRIBUTE_NAME, "The name of the attribute of sentiment word.", sentimentExampleSetInput,
                true);
        type.registerDependencyCondition(new EqualStringCondition(this, DICTIONARY, false, "user dictionary"));
        types.add(type);
        type = new ParameterTypeAttribute(NEGATOR_ATTRIBUTE_NAME, "The name of the attribute of sentiment word.", sentimentExampleSetInput,
                true);
        type.registerDependencyCondition(new EqualStringCondition(this, DICTIONARY, false, "user dictionary"));
        types.add(type);

        return types;
    }

}
