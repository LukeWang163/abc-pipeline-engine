package base.operators.operator.nlp.feature;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.MemoryExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.feature.core.NGramCount;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.Ontology;

import java.util.*;

public class NGramCountOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort vocabExampleSetInput = getInputPorts().createPort("vocabulary example set");
    private InputPort otherExampleSetInput = getInputPorts().createPort("other example set");

    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public NGramCountOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ATTRIBUTE_NAME)));
        try {

            if(getParameterAsString(WEIGHT_ATTRIBUTE_NAME)!=""){
                exampleSetInput.addPrecondition(
                        new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                                this, WEIGHT_ATTRIBUTE_NAME)));
            }
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }
    }

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String WEIGHT_ATTRIBUTE_NAME = "weight_attribute_name";
    public static final String VOCABULARY_ATTRIBUTE_NAME = "vocabulary_attribute_name";
    public static final String WORD_ATTRIBUTE_NAME = "word_attribute_name";
    public static final String COUNT_ATTRIBUTE_NAME = "count_attribute_name";
    public static final String N_GRAM_SIZE = "n_gram_size";


    public void doWork() throws OperatorException {
        String doc_attribute_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
        String weight_attribute_name = getParameterAsString(WEIGHT_ATTRIBUTE_NAME);
        String vocabulary_attribute_name = getParameterAsString(VOCABULARY_ATTRIBUTE_NAME);
        String word_attribute_name = getParameterAsString(WORD_ATTRIBUTE_NAME);
        String count_attribute_name = getParameterAsString(COUNT_ATTRIBUTE_NAME);
        int n_gram_size = getParameterAsInt(N_GRAM_SIZE);
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();

        //读取词典表
        Set<String> vocabulary = new HashSet<>();
        if(vocabExampleSetInput.isConnected()){
            ExampleSet dictExampleSet = vocabExampleSetInput.getData(ExampleSet.class);
            for (Example example : dictExampleSet) {
                vocabulary.add(example.getValueAsString(example.getAttributes().get(vocabulary_attribute_name)));
            }

        }
        Map<String, Double> other = new HashMap<>();
        if(otherExampleSetInput.isConnected()){
            ExampleSet otherExampleSet = otherExampleSetInput.getData(ExampleSet.class);
            for (Example example : otherExampleSet) {
                other.put(example.getValueAsString(example.getAttributes().get(word_attribute_name)), example.getValue(example.getAttributes().get(count_attribute_name)));
            }
        }

        NGramCount nGramCount = new NGramCount(exampleSet, exampleSet.getAttributes().get(doc_attribute_name), exampleSet.getAttributes().get(weight_attribute_name), vocabulary, n_gram_size);
        Map<Integer, Map<String, Double>> result1 = nGramCount.getNgramCount();

        Map<Integer, Map<String, Double>> result = new HashMap<>();
        if(other.size()!=0){
            result = nGramCount.mergeNgramResult(result1, other);
        }else{
            result = result1;
        }

        List<Attribute> attributeList = new ArrayList<>();
        Attribute ngram_attribute = AttributeFactory.createAttribute("ngram", Ontology.NUMERICAL);
        attributeList.add(ngram_attribute);
        Attribute words_attribute = AttributeFactory.createAttribute("words", Ontology.STRING);
        attributeList.add(words_attribute);
        Attribute count_attribute = AttributeFactory.createAttribute("count", Ontology.NUMERICAL);
        attributeList.add(count_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
        for (Map.Entry<Integer, Map<String, Double>> idEntry : result.entrySet()) {
            for (Map.Entry<String, Double> wordEntry : idEntry.getValue().entrySet()){
                DataRowFactory factory = new DataRowFactory(0, '.');
                DataRow dataRow = factory.create(attributeList.size());
                dataRow.set(ngram_attribute, idEntry.getKey());
                dataRow.set(words_attribute, words_attribute.getMapping().mapString(wordEntry.getKey()));
                dataRow.set(count_attribute, wordEntry.getValue());
                exampleTable.addDataRow(dataRow);
            }
        }
        ExampleSet exampleSetOut = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSetOut);

    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the text attribute.", exampleSetInput, false));
        types.add(new ParameterTypeAttribute(WEIGHT_ATTRIBUTE_NAME, "The name of the weight attribute.", exampleSetInput, false));
        types.add(new ParameterTypeAttribute(VOCABULARY_ATTRIBUTE_NAME, "The name of the word attribute.", vocabExampleSetInput, true));

        types.add(new ParameterTypeAttribute(WORD_ATTRIBUTE_NAME, "The name of the word attribute.", otherExampleSetInput, false));
        types.add(new ParameterTypeAttribute(COUNT_ATTRIBUTE_NAME, "The name of the count attribute.", otherExampleSetInput, false));

        types.add(new ParameterTypeInt(N_GRAM_SIZE, "The size of gram.", 1, Integer.MAX_VALUE, 2));

        return types;
    }
}
