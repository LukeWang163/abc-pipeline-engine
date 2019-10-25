package base.operators.operator.etl.trans;


import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.MemoryExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClosureGeneratorOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public ClosureGeneratorOperator(OperatorDescription description){
        super(description);
        try {
            exampleSetInput.addPrecondition(
                    new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                            this, getParameterAsString(PARENT_ID_ATTRIBUTE_NAME), getParameterAsString(CHILD_ID_ATTRIBUTE_NAME))));
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }

    }

    public static final String PARENT_ID_ATTRIBUTE_NAME = "parent_id_attribute_name";
    public static final String CHILD_ID_ATTRIBUTE_NAME = "child_id_attribute_name";
    public static final String DISTANCE_ATTRIBUTE_NAME = "distance_attribute_name";
    public static final String ROOT_IS_ZERO = "root_is_zero";

    private Map<Object, Long> parents;
    private List<Map<Object, Object>> child_parent_map;
    private Object topLevel = null;

    @Override
    public void doWork() throws OperatorException {
        String parentIDName = getParameterAsString(PARENT_ID_ATTRIBUTE_NAME);
        String childIDName = getParameterAsString(CHILD_ID_ATTRIBUTE_NAME);
        String output = getParameterAsString(DISTANCE_ATTRIBUTE_NAME);
        boolean isRootIdZero = getParameterAsBoolean(ROOT_IS_ZERO);

        ExampleSet inputSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = inputSet.getAttributes();
        if(!attributes.get(childIDName).isNumerical()){
            throw new UserError(this, 120, CHILD_ID_ATTRIBUTE_NAME, "The type of child id attribute must be numerical.");
        }
        child_parent_map = new ArrayList<>();
        for (int i = 0; i < inputSet.size(); i++) {
            Map <Object,Object> per = new HashMap<>();
            per.put(inputSet.getExample(i).getValue(attributes.get(childIDName)),inputSet.getExample(i).getValue(attributes.get(parentIDName)));
            child_parent_map.add(per);
        }

        if ( isRootIdZero) {
            topLevel = new Long( 0 );
        }
        //构造第一个输出
        List<Attribute> attributeList = new ArrayList<>();
        Attribute parent_id_attribute = AttributeFactory.createAttribute(parentIDName, attributes.get(parentIDName).getValueType());
        attributeList.add(parent_id_attribute);
        Attribute child_id_attribute = AttributeFactory.createAttribute(childIDName, attributes.get(childIDName).getValueType());
        attributeList.add(child_id_attribute);
        Attribute distance_attribute = AttributeFactory.createAttribute(output, Ontology.NUMERICAL);
        attributeList.add(distance_attribute);
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);


        for ( Map<Object, Object> per : child_parent_map ) {
            for ( Object current : per.keySet() ) {
                parents = new HashMap<Object, Long>();

                // add self as distance 0
                //
                parents.put(current, 0L);

                recurseParents(per, current, 1);
                for (Object parent : parents.keySet()) {
                    DataRowFactory factory = new DataRowFactory(0, '.');
                    DataRow dataRow = factory.create(attributeList.size());
                    dataRow.set(parent_id_attribute, (double) parent);
                    dataRow.set(child_id_attribute, (double) current);
                    dataRow.set(distance_attribute, parents.get(parent));
                    exampleTable.addDataRow(dataRow);
                }
            }
        }
        ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSet1);

    }

    private void recurseParents( Map<Object, Object> per, Object key, long distance ) {
        // catch infinite loop - change at will
        if ( distance > 50 ) {
            throw new RuntimeException( "infinite loop detected:" + key );
        }
        Object parent = per.get( key );

        if ( parent == null || parent == topLevel || parent.equals( topLevel ) ) {
            return;
        } else {
            parents.put( parent, distance );
            recurseParents( per, parent, distance + 1 );
            return;
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(PARENT_ID_ATTRIBUTE_NAME, "Select the parent id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(CHILD_ID_ATTRIBUTE_NAME, "Select the child id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeString(DISTANCE_ATTRIBUTE_NAME, "The name of the distance attribute to output.", "distance",
                false));
        types.add(new ParameterTypeBoolean(ROOT_IS_ZERO, "Is the root zero?", false,
                false));

        return types;

    }
}
