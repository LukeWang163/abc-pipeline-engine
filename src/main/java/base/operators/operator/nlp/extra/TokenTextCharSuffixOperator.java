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
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.ParameterTypeString;
import base.operators.tools.Ontology;

import java.util.List;

public class TokenTextCharSuffixOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String WORD_ATTRIBUTE_NAME = "word_attribute_name";
    public static final String PREFIX = "prefix";
    public static final String SUFFIX_LENGTH= "suffix_length";

    public TokenTextCharSuffixOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, WORD_ATTRIBUTE_NAME)));
    }

    @Override
    public void doWork() throws OperatorException {
        String word_column = getParameterAsString(WORD_ATTRIBUTE_NAME);
        String prefix = getParameterAsString(PREFIX);
        int suffix_length = getParameterAsInt(SUFFIX_LENGTH);
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();

        ExampleTable exampleTable = exampleSet.getExampleTable();
        Attribute prefix_attribute = AttributeFactory.createAttribute(word_column+"_prefix", Ontology.STRING);
        exampleTable.addAttribute(prefix_attribute);
        attributes.addRegular(prefix_attribute);
        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            String word  = example.getValueAsString(attributes.get(word_column));
            int slen = word.length();
            if (slen > suffix_length)
                example.setValue(prefix_attribute, prefix_attribute.getMapping().mapString(prefix + word.substring (slen - suffix_length, slen)));
        }
        exampleSetOutput.deliver(exampleSet);
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(WORD_ATTRIBUTE_NAME, "The name of the word attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeString(PREFIX,"The word prefixes.", "PREFIX="));

        types.add(new ParameterTypeInt(SUFFIX_LENGTH,"The length of word suffixes.", 1, Integer.MAX_VALUE, 2, false));

        return types;
    }

}
