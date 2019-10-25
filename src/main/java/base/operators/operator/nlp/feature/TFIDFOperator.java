package base.operators.operator.nlp.feature;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.*;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.feature.core.TFIDF;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.List;

public class TFIDFOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String DOC_ID_ATTRIBUTE_NAME = "doc_id_attribute_name";
    public static final String WORD_ATTRIBUTE_NAME = "word_attribute_name";
    public static final String FREQUENCY_ATTRIBUTE_NAME = "frequency_attribute_name";

    public TFIDFOperator(OperatorDescription description){
        super(description);

        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ID_ATTRIBUTE_NAME,WORD_ATTRIBUTE_NAME, FREQUENCY_ATTRIBUTE_NAME)));

    }

    @Override
    public void doWork() throws OperatorException {
        String doc_id_column = getParameterAsString(DOC_ID_ATTRIBUTE_NAME);
        String word_column = getParameterAsString(WORD_ATTRIBUTE_NAME);
        String frequency_column = getParameterAsString(FREQUENCY_ATTRIBUTE_NAME);
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();

        Attributes attributes = exampleSet.getAttributes();
        Attribute doc_id_seleted = attributes.get(doc_id_column);
        Attribute word_seleted = attributes.get(word_column);
        Attribute frequency_seleted = attributes.get(frequency_column);

        TFIDF td = new TFIDF(exampleSet, doc_id_seleted, word_seleted, frequency_seleted);
        td.computeTFIDF();

        ExampleTable exampleTable = exampleSet.getExampleTable();
        //total_word_count当前doc中总word数
        Attribute total_word_count_attribute = AttributeFactory.createAttribute("total_word_count", Ontology.NUMERICAL);
        exampleTable.addAttribute(total_word_count_attribute);
        exampleSet.getAttributes().addRegular(total_word_count_attribute);
        //doc_count包含当前word的总doc数
        Attribute doc_count_attribute = AttributeFactory.createAttribute("doc_count", Ontology.NUMERICAL);
        exampleTable.addAttribute(doc_count_attribute);
        exampleSet.getAttributes().addRegular(doc_count_attribute);
        //total_doc_count全部doc数
        Attribute total_doc_count_attribute = AttributeFactory.createAttribute("total_doc_count", Ontology.NUMERICAL);
        exampleTable.addAttribute(total_doc_count_attribute);
        exampleSet.getAttributes().addRegular(total_doc_count_attribute);
        //tf
        Attribute tf_attribute = AttributeFactory.createAttribute("tf", Ontology.NUMERICAL);
        exampleTable.addAttribute(tf_attribute);
        exampleSet.getAttributes().addRegular(tf_attribute);
        //idf
        Attribute idf_attribute = AttributeFactory.createAttribute("idf", Ontology.NUMERICAL);
        exampleTable.addAttribute(idf_attribute);
        exampleSet.getAttributes().addRegular(idf_attribute);
        //tfidf
        Attribute tfidf_attribute = AttributeFactory.createAttribute("tfidf", Ontology.NUMERICAL);
        exampleTable.addAttribute(tfidf_attribute);
        exampleSet.getAttributes().addRegular(tfidf_attribute);
        for (int j = 0; j < exampleSet.size(); j++) {
            Example example = exampleSet.getExample(j);
            String doc_id = example.getValueAsString(doc_id_seleted);
            String word = example.getValueAsString(word_seleted);
            double frequency = example.getValue(frequency_seleted);

            example.setValue(total_word_count_attribute, td.total_word_count.get(doc_id));
            example.setValue(doc_count_attribute, td.doc_count.get(word));
            example.setValue(total_doc_count_attribute, td.total_doc_count);
            double tf = td.total_word_count.get(doc_id) / frequency;
            example.setValue(tf_attribute, tf);
            double idf = Math.log10(td.total_doc_count/(double)td.doc_count.get(word));
            example.setValue(idf_attribute, idf);
            example.setValue(tfidf_attribute, tf*idf);

        }

        exampleSetOutput.deliver(exampleSet);

    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ID_ATTRIBUTE_NAME, "The name of the id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(WORD_ATTRIBUTE_NAME, "The name of the word attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(FREQUENCY_ATTRIBUTE_NAME, "The name of the frequency attribute about word.", exampleSetInput,
                false));
        return types;
    }
}
