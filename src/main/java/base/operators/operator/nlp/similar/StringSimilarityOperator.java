package base.operators.operator.nlp.similar;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.similar.core.TextSimilarity;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.tools.Ontology;

import java.util.List;

/**
 * @author wangpanpan
 * create time:  2019.08.02
 * description:
 */

public class StringSimilarityOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String FIRST_DOC_ATTRIBUTE_NAME = "first_doc_attribute_name";
    public static final String SECOND_DOC_ATTRIBUTE_NAME = "second_doc_attribute_name";
    public static final String OUTPUT_ATTRIBUTE_NAME = "output_attribute_name";
    public static final String METHOD = "method";
    public static final String SSK_LAMBDA = "ssk_lambda";
    public static final String SSK_SUBSEQUENCE_LENGTH = "ssk_subsequence_length";

    public static String[] METHODS = {"levenshtein_sim","damerau_levenstein_sim","lcs_sim","ssk","cosine","simhash_hamming_sim","jaro_sim","jaro_winkler_sim","needleman_wunsch","smith_waterman"};

    public StringSimilarityOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, FIRST_DOC_ATTRIBUTE_NAME,SECOND_DOC_ATTRIBUTE_NAME)));
    }

    @Override
    public void doWork() throws OperatorException {
        String first_doc_attribute_name = getParameterAsString(FIRST_DOC_ATTRIBUTE_NAME);
        String second_doc_attribute_name = getParameterAsString(SECOND_DOC_ATTRIBUTE_NAME);
        String output_attribute_name = getParameterAsString(OUTPUT_ATTRIBUTE_NAME);
        int method = getParameterAsInt(METHOD);
        double ssk_lambda = getParameterAsDouble(SSK_LAMBDA);
        int ssk_subsequence_length = getParameterAsInt(SSK_SUBSEQUENCE_LENGTH);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        Attribute first_doc_attribute = attributes.get(first_doc_attribute_name);
        Attribute second_doc_attribute = attributes.get(second_doc_attribute_name);

        ExampleTable exampleTable = exampleSet.getExampleTable();
        Attribute output_attribute = AttributeFactory.createAttribute(output_attribute_name, Ontology.NUMERICAL);
        exampleTable.addAttribute(output_attribute);
        exampleSet.getAttributes().addRegular(output_attribute);

        //获取处理的列的内容
        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            String firstDoc = example.getValueAsString(first_doc_attribute);
            String secondDoc = example.getValueAsString(second_doc_attribute);
            double distance = TextSimilarity.computeDistance(firstDoc, secondDoc, "",METHODS[method], ssk_subsequence_length, ssk_lambda);
            example.setValue(output_attribute, distance);
        }
        ExampleSet exampleSetOut = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSetOut);
    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(FIRST_DOC_ATTRIBUTE_NAME, "The first attribute name of doc.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(SECOND_DOC_ATTRIBUTE_NAME, "The second attribute name of doc.", exampleSetInput,
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

