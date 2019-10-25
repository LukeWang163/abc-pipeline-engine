package base.operators.operator.nlp.feature;

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
import base.operators.operator.nlp.feature.core.PMI;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PMIOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String MIN_FREQUENCY = "min_frequency";//迭代结束误差
    public static final String WINDOWS = "windows";

    public PMIOperator(OperatorDescription description){
        super(description);

        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ATTRIBUTE_NAME)));

    }
    public void doWork() throws OperatorException {
        String doc_column = getParameterAsString(DOC_ATTRIBUTE_NAME);
        int windows = getParameterAsInt(WINDOWS);
        int min_frequency = getParameterAsInt(MIN_FREQUENCY);
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        Attribute doc_seleted = attributes.get(doc_column);
        List<String> doc_list = new ArrayList<>();
        for(Example example : exampleSet){
            doc_list.add(example.getValueAsString(doc_seleted));
        }
        PMI pmi = new PMI(doc_list, min_frequency, windows);
        pmi.computePMI();
        //构造输出表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute word1_attribute = AttributeFactory.createAttribute("word1", Ontology.STRING);
        attributeList.add(word1_attribute);
        Attribute word2_attribute = AttributeFactory.createAttribute("word2", Ontology.STRING);
        attributeList.add(word2_attribute);
        Attribute count1_attribute = AttributeFactory.createAttribute("count1", Ontology.NUMERICAL);
        attributeList.add(count1_attribute);
        Attribute count2_attribute = AttributeFactory.createAttribute("count2", Ontology.NUMERICAL);
        attributeList.add(count2_attribute);
        Attribute occurrences_attribute = AttributeFactory.createAttribute("co_occurrences_count", Ontology.NUMERICAL);
        attributeList.add(occurrences_attribute);
        Attribute pmi_attribute = AttributeFactory.createAttribute("pmi", Ontology.NUMERICAL);
        attributeList.add(pmi_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        for (Map.Entry<String, Double> pairEntry : pmi.fullWordPairPMI.entrySet()) {
            String[] pairword = pairEntry.getKey().split("\\s+");
            if(pairword.length==2){
                DataRowFactory factory = new DataRowFactory(0, '.');
                DataRow dataRow = factory.create(attributeList.size());
                dataRow.set(word1_attribute, word1_attribute.getMapping().mapString(pairword[0]));
                dataRow.set(word2_attribute, word2_attribute.getMapping().mapString(pairword[1]));
                dataRow.set(count1_attribute, pmi.singleWordFrequency.get(pairword[0]));
                dataRow.set(count2_attribute, pmi.singleWordFrequency.get(pairword[1]));
                dataRow.set(occurrences_attribute, pmi.fullWordPairFrequency.get(pairEntry.getKey()));
                dataRow.set(pmi_attribute, pairEntry.getValue());

                exampleTable.addDataRow(dataRow);
            }
        }
        ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSet1);

    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the text attribute.", exampleSetInput, false));
        types.add(new ParameterTypeInt(MIN_FREQUENCY, "The min frequency of ngram words to save", 1, Integer.MAX_VALUE,5, false));
        types.add(new ParameterTypeInt(WINDOWS, "The size of window.", 1, Integer.MAX_VALUE, 2));

        return types;
    }


}
