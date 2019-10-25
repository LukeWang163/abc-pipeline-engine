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
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.List;
import java.util.regex.Pattern;

public class RegexMatchesOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String CONTENT_ATTRIBUTE_NAME = "content_attribute_name";
    public static final String EDIT_REGEX_EXPRESSION = "edit_regex_expression";
    public static final String REGEX_FEATURE_NAME = "regex_feature_name";
    public static final String REGEX_EXPRESSION = "regex_expression";

    public RegexMatchesOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, CONTENT_ATTRIBUTE_NAME)));
    }

    @Override
    public void doWork() throws OperatorException {
        String content_col = getParameterAsString(CONTENT_ATTRIBUTE_NAME);
        List<String[]> regex_list = getParameterList(EDIT_REGEX_EXPRESSION);
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        ExampleTable table = exampleSet.getExampleTable();

        for(String[] regexp : regex_list){
            String feature = regexp[0];
            Pattern pattern = Pattern.compile(regexp[1]);

            Attribute feature_attribute = AttributeFactory.createAttribute(content_col+"_"+feature, Ontology.NUMERICAL);
            table.addAttribute(feature_attribute);
            attributes.addRegular(feature_attribute);

            for(Example row : exampleSet){
                String content = row.getValueAsString(attributes.get(content_col));
                if(pattern.matcher(content).matches()){
                    row.setValue(feature_attribute, 1);
                }else {
                    row.setValue(feature_attribute, 0);
                }

            }

        }
        exampleSetOutput.deliver(exampleSet);
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(CONTENT_ATTRIBUTE_NAME, "The name of the string attribute.", exampleSetInput,
                false));

        ParameterType type = new ParameterTypeList(EDIT_REGEX_EXPRESSION, "Regex expression list",
                new ParameterTypeString(REGEX_FEATURE_NAME, "Regex expression name.", false, false),
                new ParameterTypeRegexp(REGEX_EXPRESSION, "Regex expression.", false), false);
        types.add(type);
        return types;
    }


}
