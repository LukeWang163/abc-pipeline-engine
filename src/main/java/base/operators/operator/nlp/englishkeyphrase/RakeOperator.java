package base.operators.operator.nlp.englishkeyphrase;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
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
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.*;

public class RakeOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String KEY_WORDS_SIZE = "key_words_size";
    public static final String MINIMUM_CHARACTER_NUMBER_OF_PHRASES = "minimum_character_number_of_phrases";
    public static final String MAXIMUM_NUMBER_OF_WORDS_IN_PHRASES = "maximum_number_of_words_in_phrases";
    public RakeOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, ID_ATTRIBUTE_NAME,DOC_ATTRIBUTE_NAME)));

    }

    @Override
    public void doWork() throws OperatorException {
        String id_name = getParameterAsString(ID_ATTRIBUTE_NAME);
        String doc_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
        int key_size = getParameterAsInt(KEY_WORDS_SIZE);
        int minLength = getParameterAsInt(MINIMUM_CHARACTER_NUMBER_OF_PHRASES);
        int maxCount = getParameterAsInt(MAXIMUM_NUMBER_OF_WORDS_IN_PHRASES);
        String languageCode = RakeLanguages.ENGLISH;
        Rake rake = new Rake(languageCode,minLength,maxCount);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        // 构造第一个输出表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute new_id_attribute = AttributeFactory.createAttribute(id_name, attributes.get(id_name).getValueType());
        attributeList.add(new_id_attribute);
        Attribute keyphrase_attribute = AttributeFactory.createAttribute("keyphrase", Ontology.STRING);
        attributeList.add(keyphrase_attribute);
        Attribute score_attribute = AttributeFactory.createAttribute("score", Ontology.NUMERICAL);
        attributeList.add(score_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        for(Example example : exampleSet){
            String docContent = example.getValueAsString(attributes.get(doc_name));
            double docId = example.getValue(attributes.get(id_name));
            //关键短语提取
            LinkedHashMap<String, Double> results = rake.getKeywordsFromText(docContent);
            if(key_size > results.size()) {
                key_size = results.size();
            }
            Iterator<Map.Entry<String, Double>> iterator= results.entrySet().iterator();
            int i = 0;
            while(iterator.hasNext() && i < key_size) {
                Map.Entry<String, Double> entry = iterator.next();
                DataRowFactory factory = new DataRowFactory(0, '.');
                DataRow dataRow = factory.create(attributeList.size());
                dataRow.set(new_id_attribute, docId);
                dataRow.set(keyphrase_attribute, keyphrase_attribute.getMapping().mapString(entry.getKey().toString()));
                dataRow.set(score_attribute, entry.getValue());
                exampleTable.addDataRow(dataRow);
                i++;
            }
        }
        ExampleSet exampleSetOut = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSetOut);

    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeInt(KEY_WORDS_SIZE, "The number of key words.", 1, Integer.MAX_VALUE, 5));
        types.add(new ParameterTypeInt(MINIMUM_CHARACTER_NUMBER_OF_PHRASES, "The minimum character number of phrases.",1, Integer.MAX_VALUE, 4));
        types.add(new ParameterTypeInt(MAXIMUM_NUMBER_OF_WORDS_IN_PHRASES, "The maximum number of words in phrases.",1, Integer.MAX_VALUE, 6));

        return types;
    }



}
