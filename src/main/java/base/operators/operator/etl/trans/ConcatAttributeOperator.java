package base.operators.operator.etl.trans;


import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.List;

public class ConcatAttributeOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public ConcatAttributeOperator(OperatorDescription description){
        super(description);
        String[] attributes = new String[0];
        try {
            attributes = getParameterAsString(ATTRIBUTES_NAME).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }
        for (int i = 0; i < attributes.length; i++) {
            exampleSetInput.addPrecondition(
                    new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                            this, attributes[i])));
        }
    }

    public static final String ATTRIBUTES_NAME = "attributes_name";
    public static final String OUTPUT_ATTRIBUTE_NAME = "output_attribute_name";
    public static final String SEPARATOR = "separator";
    public static final String ENCLOSURE = "enclosure";
    public static final String WHETHER_TO_REMOVE_SELECTED_ATTRIBUTES = "whether_to_remove_selected_attributes";

    private String separator;
    private String enclosure;
    private boolean is_remove;

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        String[] selected_attributes = getParameterAsString(ATTRIBUTES_NAME).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        for (int i = 1; i < selected_attributes.length; i++) {
            if(attributes.get(selected_attributes[0]).getValueType()!= attributes.get(selected_attributes[i]).getValueType()){
                throw new UserError(this, 116, ATTRIBUTES_NAME, "The type of the value of the input attribute should be the same！");
            }
        }

        String output = getParameterAsString(OUTPUT_ATTRIBUTE_NAME);
        separator = getParameterAsString(SEPARATOR);
        if(getParameterAsString(ENCLOSURE)!=null){
            enclosure = getParameterAsString(ENCLOSURE);
        }
        is_remove = getParameterAsBoolean(WHETHER_TO_REMOVE_SELECTED_ATTRIBUTES);

        ExampleTable exampleTable = exampleSet.getExampleTable();
        Attribute result_attribute = AttributeFactory.createAttribute(output, Ontology.STRING);
        exampleTable.addAttribute(result_attribute);
        attributes.addRegular(result_attribute);

        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            StringBuilder str = new StringBuilder();
            str.append(enclosure);
            for (int j = 0; j < selected_attributes.length; j++) {
                if(j>0){
                    str.append( separator );
                }
                if(attributes.get(selected_attributes[0]).getValueType()==3){
                    str.append((int)example.getValue(attributes.get(selected_attributes[j])));

                }else{
                    str.append(example.getValueAsString(attributes.get(selected_attributes[j])));
                }
            }
            str.append(enclosure);
            example.setValue(result_attribute, result_attribute.getMapping().mapString(str.toString()));
        }

        if(is_remove){
            for (int u = 0; u < selected_attributes.length; u++) {
                exampleTable.removeAttribute(attributes.get(selected_attributes[u]));
                attributes.remove(attributes.get(selected_attributes[u]));
            }
        }
        exampleSetOutput.deliver(exampleSet);

    }

    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttributes(ATTRIBUTES_NAME, "Select the attributes to be used for concat.", exampleSetInput,
                false));
        types.add(new ParameterTypeString(OUTPUT_ATTRIBUTE_NAME,"Attribute name of output results","output", false));
        types.add(new ParameterTypeString(SEPARATOR,"Spacing symbols for concat.",";" ,false));
        types.add(new ParameterTypeString(ENCLOSURE,"Markers added before and after concat results.","", false));
        types.add(new ParameterTypeBoolean(WHETHER_TO_REMOVE_SELECTED_ATTRIBUTES,"whether to remove selected attributes?",false, false));


        return types;
    }

}
