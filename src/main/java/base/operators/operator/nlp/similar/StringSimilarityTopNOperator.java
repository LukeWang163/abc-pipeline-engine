package base.operators.operator.nlp.similar;


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
import base.operators.operator.nlp.similar.core.StringSimilarityTopN;
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

public class StringSimilarityTopNOperator extends Operator {

    private InputPort originalExampleSetInput = getInputPorts().createPort("original example set");
    private InputPort mapExampleSetInput = getInputPorts().createPort("map example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String ORIGINAL_ATTRIBUTE_NAME = "original_attribute_name";
    public static final String MAP_ATTRIBUTE_NAME = "map_attribute_name";
    public static final String TOP_N = "top_n";
    public static final String METHOD = "method";
    public static final String SSK_LAMBDA = "ssk_lambda";
    public static final String SSK_SUBSEQUENCE_LENGTH = "ssk_subsequence_length";

    public static String[] METHODS = {"levenshtein_sim","damerau_levenstein_sim","lcs_sim","ssk","cosine","simhash_hamming_sim","jaro_sim","jaro_winkler_sim","needleman_wunsch","smith_waterman"};

    public StringSimilarityTopNOperator(OperatorDescription description){
        super(description);
        originalExampleSetInput.addPrecondition(
                new AttributeSetPrecondition(originalExampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, ORIGINAL_ATTRIBUTE_NAME)));
        mapExampleSetInput.addPrecondition(
                new AttributeSetPrecondition(mapExampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, MAP_ATTRIBUTE_NAME)));
    }

    @Override
    public void doWork() throws OperatorException {
        String original_attribute_name = getParameterAsString(ORIGINAL_ATTRIBUTE_NAME);
        String map_attribute_name = getParameterAsString(MAP_ATTRIBUTE_NAME);
        int method = getParameterAsInt(METHOD);
        int top_n = getParameterAsInt(TOP_N);
        double ssk_lambda = getParameterAsDouble(SSK_LAMBDA);
        int ssk_subsequence_length = getParameterAsInt(SSK_SUBSEQUENCE_LENGTH);

        ExampleSet originalExampleSet = (ExampleSet) originalExampleSetInput.getData(ExampleSet.class).clone();
        Attributes originalAttributes = originalExampleSet.getAttributes();
        Attribute original_attribute = originalAttributes.get(original_attribute_name);

        ExampleSet mapExampleSet = (ExampleSet) mapExampleSetInput.getData(ExampleSet.class).clone();
        Attributes mapAttributes = mapExampleSet.getAttributes();
        Attribute map_attribute = mapAttributes.get(map_attribute_name);

        List<Attribute> attributeList = new ArrayList<>();
        Attribute original_id_attribute = AttributeFactory.createAttribute("original_id", original_attribute.getValueType());
        attributeList.add(original_id_attribute);
        Attribute near_id_attribute = AttributeFactory.createAttribute("near_id", map_attribute.getValueType());
        attributeList.add(near_id_attribute);
        Attribute sim_attribute = AttributeFactory.createAttribute("distance", Ontology.NUMERICAL);
        attributeList.add(sim_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
        //获取处理的列的内容
        List<String> original_list = new ArrayList<>();
        List<String> map_list = new ArrayList<>();
        for (Example example : originalExampleSet) {
            original_list.add(example.getValueAsString(original_attribute));
        }
        for(Example example : originalExampleSet){
            map_list.add(example.getValueAsString(map_attribute));
        }
        StringSimilarityTopN sstn = new StringSimilarityTopN(original_list, map_list, METHODS[method], top_n, ssk_subsequence_length, ssk_lambda);

        for (int j = 0; j < sstn.inputList.size(); j++) {
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(original_id_attribute, original_attribute.isNumerical()? Double.parseDouble(sstn.inputList.get(j)): original_id_attribute.getMapping().mapString(sstn.inputList.get(j)));
            dataRow.set(near_id_attribute, map_attribute.isNumerical()? Double.parseDouble(sstn.mapList.get(j)): near_id_attribute.getMapping().mapString(sstn.mapList.get(j)));
            dataRow.set(sim_attribute, sstn.simList.get(j));
            exampleTable.addDataRow(dataRow);
        }

        ExampleSet exampleSetOut = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSetOut);

    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(ORIGINAL_ATTRIBUTE_NAME, "The name of the original attribute.", originalExampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(MAP_ATTRIBUTE_NAME, "The name of the map attribute.", mapExampleSetInput,
                false));
        types.add(new ParameterTypeCategory(METHOD, "The method of similarity.",
                METHODS, 0, false));
        types.add(new ParameterTypeInt(TOP_N,"The top n number to save.",1, Integer.MAX_VALUE, 2));
        ParameterType type = new ParameterTypeInt(SSK_SUBSEQUENCE_LENGTH,"The length of ssk subsequence.",1, Integer.MAX_VALUE, 2);
        type.registerDependencyCondition(new EqualStringCondition(this, METHOD, false, "ssk"));
        types.add(type);
        type = new ParameterTypeDouble(SSK_LAMBDA,"The lambda of ssk.",0.00001, 1, 0.5);
        type.registerDependencyCondition(new EqualStringCondition(this, METHOD, false, "ssk"));
        types.add(type);
        return types;
    }

}
