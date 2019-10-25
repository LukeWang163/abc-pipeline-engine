package base.operators.operator.nlp.similar;


import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.*;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.similar.core.DocSentencesSimilarity;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangpanpan
 * create time:  2019.08.02.
 * description:
 */

public class DocSentencesSimilarityOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String OUTPUT_ATTRIBUTE_NAME = "output_attribute_name";
    public static final String METHOD = "method";
    public static final String SSK_LAMBDA = "ssk_lambda";
    public static final String SSK_SUBSEQUENCE_LENGTH = "ssk_subsequence_length";

    public static String[] METHODS = {"levenshtein_sim","damerau_levenstein_sim","lcs_sim","ssk","cosine","simhash_hamming_sim","jaro_sim","jaro_winkler_sim","needleman_wunsch","smith_waterman"};

    public DocSentencesSimilarityOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ATTRIBUTE_NAME,ID_ATTRIBUTE_NAME)));

    }

    @Override
    public void doWork() throws OperatorException {
        String doc_attribute_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
        String id_attribute_name = getParameterAsString(ID_ATTRIBUTE_NAME);
        String output_attribute_name = getParameterAsString(OUTPUT_ATTRIBUTE_NAME);
        int method = getParameterAsInt(METHOD);
        double ssk_lambda = getParameterAsDouble(SSK_LAMBDA);
        int ssk_subsequence_length = getParameterAsInt(SSK_SUBSEQUENCE_LENGTH);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        Attribute id_attribute = attributes.get(id_attribute_name);
        Attribute doc_attribute = attributes.get(doc_attribute_name);

        List<Attribute> attributeList = new ArrayList<>();
        Attribute new_id_attribute = AttributeFactory.createAttribute(id_attribute_name, id_attribute.getValueType());
        attributeList.add(new_id_attribute);
        Attribute new_sentence_attribute_1 = AttributeFactory.createAttribute("sentence_1", Ontology.STRING);
        attributeList.add(new_sentence_attribute_1);
        Attribute new_sentence_attribute_2 = AttributeFactory.createAttribute("sentence_2", Ontology.STRING);
        attributeList.add(new_sentence_attribute_2);
        Attribute new_sim_attribute = AttributeFactory.createAttribute(output_attribute_name, Ontology.NUMERICAL);
        attributeList.add(new_sim_attribute);
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
        //获取处理的列的内容
        List<String> idList = new ArrayList<>();
        List<String> docList = new ArrayList<>();
        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            idList.add(example.getValueAsString(id_attribute));
            docList.add(example.getValueAsString(doc_attribute));
        }
        DocSentencesSimilarity dss = new DocSentencesSimilarity(idList, docList, METHODS[method], ssk_subsequence_length, ssk_lambda);
        for (int j = 0; j < dss.idList.size(); j++) {
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(new_id_attribute, id_attribute.isNumerical()? Double.parseDouble(dss.idList.get(j)): new_id_attribute.getMapping().mapString(dss.idList.get(j)));
            dataRow.set(new_sentence_attribute_1, new_sentence_attribute_1.getMapping().mapString(dss.inputList.get(j)));
            dataRow.set(new_sentence_attribute_2, new_sentence_attribute_2.getMapping().mapString(dss.mapList.get(j)));
            dataRow.set(new_sim_attribute, dss.simList.get(j));
            exampleTable.addDataRow(dataRow);
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
        types.add(new ParameterTypeString(OUTPUT_ATTRIBUTE_NAME, "The name of the output attribute name.","output"));
        types.add(new ParameterTypeCategory(METHOD, "The method of similarity.",
                METHODS, 0, false));

        ParameterType type = new ParameterTypeInt(SSK_SUBSEQUENCE_LENGTH,"The length of ssk subsequence.",1, Integer.MAX_VALUE, 2);
        type.registerDependencyCondition(new EqualStringCondition(this, METHOD, false, "ssk"));
        types.add(type);
        type = new ParameterTypeDouble(SSK_LAMBDA,"The lambda of ssk.",0.00001, 1, 0.5);
        type.registerDependencyCondition(new EqualStringCondition(this, METHOD, false, "ssk"));
        types.add(type);
        return types;
    }


}
