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
import base.operators.operator.nlp.similar.core.SemanticVectorDistance;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wangpanpan
 * create time:  2019.08.02.
 * description:
 */

public class SemanticVectorDistanceOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String VEC_ATTRIBUTE_NAMES = "vec_attribute_names";
    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String METHOD = "method";
    public static final String TOP_N = "top_n";
    public static final String DISTANCE_THRESHOLD = "distance_threshold";

    public static String[] METHODS = {"euclidean", "cosine", "manhattan"};

    public SemanticVectorDistanceOperator(OperatorDescription description){
        super(description);

        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, ID_ATTRIBUTE_NAME)));

    }

    @Override
    public void doWork() throws OperatorException {
        String[] vec_attribute_names = getParameterAsString(VEC_ATTRIBUTE_NAMES).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        String id_attribute_name = getParameterAsString(ID_ATTRIBUTE_NAME);
        int method = getParameterAsInt(METHOD);
        int top_n = getParameterAsInt(TOP_N);
        double distance_threshold = getParameterAsDouble(DISTANCE_THRESHOLD);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        Attribute id_attribute = attributes.get(id_attribute_name);

        List<Attribute> attributeList = new ArrayList<>();
        Attribute original_id_attribute = AttributeFactory.createAttribute("original_id", id_attribute.getValueType());
        attributeList.add(original_id_attribute);
        Attribute near_id_attribute = AttributeFactory.createAttribute("near_id", id_attribute.getValueType());
        attributeList.add(near_id_attribute);
        Attribute distance_attribute = AttributeFactory.createAttribute("distance", Ontology.NUMERICAL);
        attributeList.add(distance_attribute);
        Attribute rank_attribute = AttributeFactory.createAttribute("rank", Ontology.NUMERICAL);
        attributeList.add(rank_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
        //获取处理的列的内容
        Map<String, double[]> input = new HashMap<>();
        for(Example example : exampleSet){
            List<Double> per_id_vec = new ArrayList<>();
            for (int i = 0; i < vec_attribute_names.length; i++){
                per_id_vec.add(example.getNumericalValue(attributes.get(vec_attribute_names[i])));
            }
            input.put(example.getValueAsString(id_attribute), per_id_vec.stream().mapToDouble(Double::doubleValue).toArray());
        }
        SemanticVectorDistance result = new SemanticVectorDistance(input, METHODS[method], top_n, distance_threshold);
        for (int j = 0; j < result.word1_list.size(); j++) {
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(original_id_attribute, id_attribute.isNumerical()? Double.parseDouble(result.word1_list.get(j)): original_id_attribute.getMapping().mapString(result.word1_list.get(j)));
            dataRow.set(near_id_attribute, id_attribute.isNumerical()? Double.parseDouble(result.word2_list.get(j)): near_id_attribute.getMapping().mapString(result.word2_list.get(j)));
            dataRow.set(distance_attribute, result.distance_list.get(j));
            dataRow.set(rank_attribute, (double)result.rank_list.get(j));

            exampleTable.addDataRow(dataRow);
        }
        ExampleSet exampleSetOut = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSetOut);

    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttributes(VEC_ATTRIBUTE_NAMES, "The name of the vector attribute.", exampleSetInput));
        types.add(new ParameterTypeCategory(METHOD, "The method of similarity.",
                METHODS, 0, false));
        types.add(new ParameterTypeInt(TOP_N,"The top n number to save.",1, Integer.MAX_VALUE, 5));
        types.add(new ParameterTypeDouble(DISTANCE_THRESHOLD,"The Distance threshold.",0, Double.MAX_VALUE, Double.MAX_VALUE));

        return types;
    }


}
