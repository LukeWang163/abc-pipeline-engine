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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexMatchesCountOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String CONTENT_ATTRIBUTE_NAME = "content_attribute_name";
    public static final String COUNT_TYPE = "count_type";
    public static String[] TYPE_NAMES = new String[] { "integer_count","binary_count","normalized_count"};

    public static final String EDIT_REGEX_EXPRESSION = "edit_regex_expression";
    public static final String REGEX_FEATURE_NAME = "regex_feature_name";
    public static final String REGEX_EXPRESSION = "regex_expression";

    public RegexMatchesCountOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, CONTENT_ATTRIBUTE_NAME)));
    }

    boolean normalizeByCharLength = false;
    boolean countIsBinary = false;

    @Override
    public void doWork() throws OperatorException {
        String content_col = getParameterAsString(CONTENT_ATTRIBUTE_NAME);
        int countType = getParameterAsInt(COUNT_TYPE);
        if (countType == 1)
            countIsBinary = true;
        else if (countType == 2)
            normalizeByCharLength = true;
        List<String[]> regex_list = getParameterList(EDIT_REGEX_EXPRESSION);
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        ExampleTable table = exampleSet.getExampleTable();

        for(String[] regex : regex_list){
            String feature = regex[0];
            Pattern pattern = Pattern.compile(regex[1]);

            Attribute feature_attribute = AttributeFactory.createAttribute(content_col+"_"+feature, Ontology.NUMERICAL);
            table.addAttribute(feature_attribute);
            attributes.addRegular(feature_attribute);

            for(Example row : exampleSet){
                String content = row.getValueAsString(attributes.get(content_col));
                int count = 0;
                Matcher matcher = pattern.matcher (content);
                while (matcher.find ()) {
                    count++;
                    if (countIsBinary) break;
                }
                if (count > 0){
                    row.setValue(feature_attribute, normalizeByCharLength ? ((double)count)/content.length() : (double)count);
                }else if(count == 0){
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
        types.add(new ParameterTypeCategory(COUNT_TYPE, "Setting method of feature value", TYPE_NAMES, 0,false));
        ParameterType type = new ParameterTypeList(EDIT_REGEX_EXPRESSION, "Regex expression list",
                new ParameterTypeString(REGEX_FEATURE_NAME, "Regex expression name.", false, false),
                new ParameterTypeRegexp(REGEX_EXPRESSION, "Regex expression.", false), false);
        types.add(type);
        return types;
    }


}
