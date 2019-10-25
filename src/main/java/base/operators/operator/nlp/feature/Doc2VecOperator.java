package base.operators.operator.nlp.feature;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.example.Statistics;
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

import java.util.*;

public class Doc2VecOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String VEC_ATTRIBUTE_NAMES = "vec_attribute_names";
    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String MODE = "mode";

    public static String[] MODES = {"sum", "average"};

    public Doc2VecOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, ID_ATTRIBUTE_NAME)));

    }

    @Override
    public void doWork() throws OperatorException {
        String[] vec_attribute_names = getParameterAsString(VEC_ATTRIBUTE_NAMES).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        String id_attribute_name = getParameterAsString(ID_ATTRIBUTE_NAME);
        int mode = getParameterAsInt(MODE);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        Attribute id_attribute = attributes.get(id_attribute_name);
        Set<Double> id_set = new HashSet<>();
        for (int i = 0; i < exampleSet.size(); i++) {
            id_set.add(exampleSet.getExample(i).getValue(id_attribute));
        }

        List<Attribute> attributeList = new ArrayList<>();
        Attribute new_id_attribute = AttributeFactory.createAttribute(id_attribute_name, id_attribute.getValueType());
        attributeList.add(new_id_attribute);
        for (int j = 0; j < vec_attribute_names.length; j++) {
            Attribute new_vec_attribute = AttributeFactory.createAttribute(vec_attribute_names[j], attributes.get(vec_attribute_names[j]).getValueType());
            attributeList.add(new_vec_attribute);
        }
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
        //按照id进行exampleset的筛选
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        OperatorDescription description = null;
        try {
            description = new OperatorDescription(loader, null, null, "com.rapidminer.operator.preprocessing.filter.ExampleFilter", null, null, null, null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Iterator<Double> it = id_set.iterator();
        while (it.hasNext()) {
            double idValue = it.next();
            ExampleFilter examplesFilter = new ExampleFilter(description);
            examplesFilter.setParameter("condition_class", "attribute_value_filter");
            examplesFilter.setParameter("parameter_string", id_attribute_name+"="+idValue);
            ExampleSet filterExampleSet = examplesFilter.apply(exampleSet);
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            filterExampleSet.recalculateAllAttributeStatistics();
            for (int k = 1; k < attributeList.size(); k++) {
                //double value = attributeList.get(k).getStatistics(MODES[mode]);
                double value = 0.0;
                if(mode==0){
                    value = filterExampleSet.getStatistics(attributeList.get(k),Statistics.SUM);
                }else if(mode==1){
                    value = filterExampleSet.getStatistics(attributeList.get(k), Statistics.AVERAGE);
                }
                dataRow.set(attributeList.get(k), value);
            }
            dataRow.set(new_id_attribute, idValue);
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
        types.add(new ParameterTypeCategory(MODE, "The method mode of computing.",
                MODES, 0, false));

        return types;
    }

}
