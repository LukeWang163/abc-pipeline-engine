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

public class RegexDoubleMatchesCountOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String CONTENT_ATTRIBUTE_NAME = "content_attribute_name";
    public static final String REGEX_EXPRESSION = "regex_expression";
    public static final String MORE_SPECIFIC_REGEX_EXPRESSION = "more_specific_regex_expression";
    public static final String NORMALIZE_BY_REGEX_MATCHES = "normalize_by_regex_matches";

    public RegexDoubleMatchesCountOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, CONTENT_ATTRIBUTE_NAME)));
    }

    boolean normalizeByRegexMatches = false;

    @Override
    public void doWork() throws OperatorException {
        String content_col = getParameterAsString(CONTENT_ATTRIBUTE_NAME);
        Pattern regex_1 = Pattern.compile(getParameterAsString(REGEX_EXPRESSION));
        Pattern regex_2 = Pattern.compile(getParameterAsString(MORE_SPECIFIC_REGEX_EXPRESSION));
        normalizeByRegexMatches = getParameterAsBoolean(NORMALIZE_BY_REGEX_MATCHES);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        ExampleTable table = exampleSet.getExampleTable();

        Attribute feature_attribute = AttributeFactory.createAttribute(content_col+"_feature", Ontology.NUMERICAL);
        table.addAttribute(feature_attribute);
        attributes.addRegular(feature_attribute);

        for(Example row : exampleSet){
            String content = row.getValueAsString(attributes.get(content_col));
            int count = 0;
            int moreSpecificCount = 0;
            Matcher matcher = regex_1.matcher (content);
            while (matcher.find()) {
                count++;
                Matcher moreSpecificMatcher = regex_2.matcher (content.substring(matcher.start()));
                if (moreSpecificMatcher.lookingAt ()) {
                    moreSpecificCount++;
                }
            }

            if (moreSpecificCount > 0){
                row.setValue(feature_attribute, normalizeByRegexMatches
                        ? ((double)moreSpecificCount)/count
                        : moreSpecificCount);
            }else {
                row.setValue(feature_attribute, 0);
            }
        }

        exampleSetOutput.deliver(exampleSet);
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(CONTENT_ATTRIBUTE_NAME, "The name of the string attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeRegexp(REGEX_EXPRESSION, "Regular expression.", false, false));
        types.add(new ParameterTypeRegexp(MORE_SPECIFIC_REGEX_EXPRESSION, "More specific regular expression.", false, false));
        types.add(new ParameterTypeBoolean(NORMALIZE_BY_REGEX_MATCHES,"Is the number of matches used for standardization?", false,false));

        return types;
    }

}
