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
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.operator.preprocessing.filter.ExampleFilter;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.*;

public class TripleToKVOperator extends Operator{
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort indexExampleSetInput = getInputPorts().createPort("index example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
    private OutputPort indexExampleSetOutput = getOutputPorts().createPort("index example set");

    public static final String KEEP_ATTRIBUTE_NAME = "keep_attribute_name";
    public static final String KEY_ATTRIBUTE_NAME = "key_attribute_name";
    public static final String VALUE_ATTRIBUTE_NAME = "value_attribute_name";
    public static final String INDEX_KEY_ATTRIBUTE_NAME = "index_key_attribute_name";
    public static final String INDEX_KEY_ID_ATTRIBUTE_NAME = "index_key_id_attribute_name";
    public static final String KV_DELIMITER = "kv_delimiter";
    public static final String PAIR_DELIMITER = "pair_delimiter";

    public TripleToKVOperator(OperatorDescription description){
        super(description);

        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, KEEP_ATTRIBUTE_NAME,KEY_ATTRIBUTE_NAME, VALUE_ATTRIBUTE_NAME)));
        if(indexExampleSetInput.isConnected()){
            indexExampleSetInput.addPrecondition(
                    new AttributeSetPrecondition(indexExampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                            this, INDEX_KEY_ATTRIBUTE_NAME,INDEX_KEY_ID_ATTRIBUTE_NAME)));
        }

    }

    @Override
    public void doWork() throws OperatorException {
        String keep_attribute_name = getParameterAsString(KEEP_ATTRIBUTE_NAME);
        String key_attribute_name = getParameterAsString(KEY_ATTRIBUTE_NAME);
        String value_attribute_name = getParameterAsString(VALUE_ATTRIBUTE_NAME);
        String index_key_attribute_name = getParameterAsString(INDEX_KEY_ATTRIBUTE_NAME);
        String index_key_id_attribute_name = getParameterAsString(INDEX_KEY_ID_ATTRIBUTE_NAME);
        String kv_delimiter = getParameterAsString(KV_DELIMITER);
        String pair_delimiter = getParameterAsString(PAIR_DELIMITER);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attribute keep_attribute = exampleSet.getAttributes().get(keep_attribute_name);
        Attribute key_attribute = exampleSet.getAttributes().get(key_attribute_name);
        Attribute value_attribute = exampleSet.getAttributes().get(value_attribute_name);
        Set<Double> keep_set = new HashSet<>();
        Map<String, String> indexMap = new HashMap<>();
        int index = 0;
        for (Example example : exampleSet) {
            keep_set.add(example.getValue(keep_attribute));
            if (!indexExampleSetInput.isConnected()&&!indexMap.containsKey(example.getValueAsString(keep_attribute))){
                indexMap.put(example.getValueAsString(key_attribute), ""+index);
                index++;
            }
        }
        ExampleSet indexExampleSet = null;
        Attribute index_key_attribute = null;
        Attribute index_key_id_attribute = null;
        if(indexExampleSetInput.isConnected()){
            indexExampleSet = (ExampleSet) indexExampleSetInput.getData(ExampleSet.class).clone();
            index_key_attribute = indexExampleSet.getAttributes().get(index_key_attribute_name);
            index_key_id_attribute = indexExampleSet.getAttributes().get(index_key_id_attribute_name);
            indexMap = new HashMap<>();
            for(Example example : indexExampleSet){
                indexMap.put(example.getValueAsString(index_key_attribute), example.getValueAsString(index_key_id_attribute));
            }
            indexExampleSetOutput.deliver(indexExampleSet);
        }
        //创建新的表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute new_keep_attribute = AttributeFactory.createAttribute(keep_attribute_name, keep_attribute.getValueType());
        attributeList.add(new_keep_attribute);
        Attribute key_value_attribute = AttributeFactory.createAttribute("key_value", Ontology.STRING);
        attributeList.add(key_value_attribute);
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        //按照id进行exampleset的筛选
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        OperatorDescription description = null;
        try {
            description = new OperatorDescription(loader, null, null, "com.rapidminer.operator.preprocessing.filter.ExampleFilter", null, null, null, null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Iterator<Double> it = keep_set.iterator();
        while (it.hasNext()) {
            double keepValue = it.next();
            ExampleFilter examplesFilter = new ExampleFilter(description);
            examplesFilter.setParameter("condition_class", "attribute_value_filter");
            examplesFilter.setParameter("parameter_string", keep_attribute_name+"="+keepValue);
            ExampleSet filterExampleSet = examplesFilter.apply(exampleSet);
            StringBuffer key_value_string = new StringBuffer();
            for (int j = 0; j < filterExampleSet.size(); j++) {
                key_value_string.append(indexMap.get(filterExampleSet.getExample(j).getValueAsString(key_attribute))+kv_delimiter+filterExampleSet.getExample(j).getValueAsString(value_attribute)+pair_delimiter);
            }
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(new_keep_attribute, keepValue);
            dataRow.set(key_value_attribute, key_value_attribute.getMapping().mapString(key_value_string.toString()));
            exampleTable.addDataRow(dataRow);

        }
        ExampleSet exampleSetOut = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSetOut);

        if (!indexExampleSetInput.isConnected()){
            List<Attribute> indexAttributeList = new ArrayList<>();
            Attribute new_key_attribute = AttributeFactory.createAttribute("key", Ontology.STRING);
            indexAttributeList.add(new_key_attribute);
            Attribute new_index_attribute = AttributeFactory.createAttribute("key_id", Ontology.NUMERICAL);
            indexAttributeList.add(new_index_attribute);
            MemoryExampleTable indexExampleTable = new MemoryExampleTable(indexAttributeList);
            for(Map.Entry<String, String> entry : indexMap.entrySet()){
                DataRowFactory factory = new DataRowFactory(0, '.');
                DataRow dataRow = factory.create(indexAttributeList.size());
                dataRow.set(new_key_attribute, new_key_attribute.getMapping().mapString(entry.getKey()));
                dataRow.set(new_index_attribute, Integer.valueOf(entry.getValue()));
                indexExampleTable.addDataRow(dataRow);
            }
            ExampleSet indexExampleSetOut = new SimpleExampleSet(indexExampleTable);
            indexExampleSetOutput.deliver(indexExampleSetOut);
        }



    }
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(KEEP_ATTRIBUTE_NAME, "The name of remain unchanged attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(KEY_ATTRIBUTE_NAME, "The name of the key attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(VALUE_ATTRIBUTE_NAME, "The name of the value attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(INDEX_KEY_ATTRIBUTE_NAME, "The name of the key attribute in index table.", indexExampleSetInput,
                true));
        types.add(new ParameterTypeAttribute(INDEX_KEY_ID_ATTRIBUTE_NAME, "The name of the key id attribute in index table.", indexExampleSetInput,
                true));

        types.add(new ParameterTypeString(KV_DELIMITER, "The delimiter of key and value", ":", false));
        types.add(new ParameterTypeString(PAIR_DELIMITER, "The delimiter of pair about key and value", ",", false));

        return types;
    }
}
