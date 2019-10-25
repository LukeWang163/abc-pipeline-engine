package base.operators.operator.nlp.commenttag;

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
import base.operators.parameter.ParameterTypeCategory;
import base.operators.tools.Ontology;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zls
 * create time:  2019.07.23.
 * description:
 */
public class CommenttagOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String PARAMETER_LANGUAGE = "language";
    public static final String PARAMETER_SELECT_COLUMN = "select_column_name";
    public static String[] LANGUAGES = { "Chinese"};

    public CommenttagOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, PARAMETER_SELECT_COLUMN)));

    }

    @Override
    public void doWork() throws OperatorException {

        String contentParserColumn = getParameterAsString(PARAMETER_SELECT_COLUMN);//短语的语法依存结果列
        ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

        Attributes attributes = exampleSet.getAttributes();
        Attribute seleted = attributes.get(contentParserColumn);


        ExampleTable table = exampleSet.getExampleTable();
        Attribute comment = AttributeFactory.createAttribute("comment",  Ontology.STRING);
        table.addAttribute(comment);

        exampleSet.getAttributes().addRegular(comment);

        for(Example row : exampleSet) {


            String parser = String.valueOf(row.getValueAsString(seleted));
            String commentTag = "";

            String pattern = "(?<=\\[)(.+)(?=\\].+)";
            Pattern t = Pattern.compile(pattern);
            Matcher m = t.matcher(parser);
            if (m.find()) {
                ThreeTuples threeTuples = new ThreeTuples(m.group());
                if (threeTuples.analys()) {
                    //System.out.println("---"+threeTuples.getsentenceTag());
                    //String posString = getpos.getData(threeTuples.getsentenceTag(),posURL);
                    //取消分词校验
                    //if(SelfUtil.resultVerify(posString)) {
                    //System.out.println("校验："+threeTuples.getsentenceTag());
                    commentTag = threeTuples.getsentenceTag();
                    //}
                }
            }
            row.setValue(comment, comment.getMapping().mapString(commentTag));
        }

        exampleSetOutput.deliver(exampleSet);
    }

    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(PARAMETER_SELECT_COLUMN, "The name of the attribute for commenttag.", exampleSetInput,
                false));
        types.add(new ParameterTypeCategory(PARAMETER_LANGUAGE, "language of text.",
                LANGUAGES, 0, false));

        return types;
    }

}
