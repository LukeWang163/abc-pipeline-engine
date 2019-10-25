package base.operators.operator.nlp.extra;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.tools.Ontology;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharSequenceRemoveUUEncodedBlocksOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String CONTENT_ATTRIBUTE_NAME = "content_attribute_name";

    public CharSequenceRemoveUUEncodedBlocksOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, CONTENT_ATTRIBUTE_NAME)));
    }

    public static final Pattern UU_ENCODED_LINE= Pattern.compile ("^M.{60}$");


    @Override
    public void doWork() throws OperatorException {
        String content_column = getParameterAsString(CONTENT_ATTRIBUTE_NAME);
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();

        ExampleTable exampleTable = exampleSet.getExampleTable();
        Attribute new_attribute= AttributeFactory.createAttribute(content_column+"_clean", Ontology.STRING);
        exampleTable.addAttribute(new_attribute);
        attributes.addRegular(new_attribute);
        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            String content  = example.getValueAsString(attributes.get(content_column));
            Matcher m = UU_ENCODED_LINE.matcher(content);
            example.setValue(new_attribute,new_attribute.getMapping().mapString(m.replaceAll ("")));

        }
        exampleSetOutput.deliver(exampleSet);
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(CONTENT_ATTRIBUTE_NAME, "The name of the string attribute.", exampleSetInput,
                false));
        return types;
    }

}
