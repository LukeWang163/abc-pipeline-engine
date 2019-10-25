package base.operators.operator.nlp.extra;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
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
import base.operators.operator.nlp.extra.util.CharSequenceLexer;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CharSequence2TokenSequenceOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");


    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String CONTENT_ATTRIBUTE_NAME = "content_attribute_name";
    public static final String PARAMETER_REGEX_EXPRESSION = "regex_expression";

    public static final String TOKEN_TYPE = "token_type";
    public static String[] TYPE_NAMES = new String[] { "lex_alpha","lex_words","lex_nonwhitespace_together","lex_word_classes","lex_nonwhitespace_classes","unicode_letters","custom_regular_expression"};

    // Some predefined lexing rules
    public static final Pattern LEX_ALPHA = Pattern.compile ("\\p{Alpha}+");//匹配一个或者多个 字母
    public static final Pattern LEX_WORDS = Pattern.compile ("\\w+");//匹配一个或者多个 字母、数字、下划线
    public static final Pattern LEX_NONWHITESPACE_TOGETHER = Pattern.compile ("\\S+");////匹配一个或者多个 非空白字符
    public static final Pattern LEX_WORD_CLASSES	=
            Pattern.compile ("\\p{Alpha}+|\\p{Digit}+");//匹配一个或者多个 字母或者数字
    public static final Pattern LEX_NONWHITESPACE_CLASSES	=
            Pattern.compile ("\\p{Alpha}+|\\p{Digit}+|\\p{Punct}");//匹配一个或者多个 字母、数字、符号

    // Lowercase letters and uppercase letters
    public static final Pattern UNICODE_LETTERS =
            Pattern.compile("[\\p{Ll}&&\\p{Lu}]+");//匹配一个或者多个 小写字母和大写字母

    public CharSequence2TokenSequenceOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, ID_ATTRIBUTE_NAME, CONTENT_ATTRIBUTE_NAME)));
    }

    @Override
    public void doWork() throws OperatorException {
        String id_column = getParameterAsString(ID_ATTRIBUTE_NAME);
        String content_column = getParameterAsString(CONTENT_ATTRIBUTE_NAME);
        String token_type = getParameterAsString(TOKEN_TYPE);
        String regular = getParameterAsString(PARAMETER_REGEX_EXPRESSION);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();

        // 构造输出表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute new_id_attribute = AttributeFactory.createAttribute(id_column, attributes.get(id_column).isNumerical() ? Ontology.NUMERICAL : Ontology.NOMINAL);
        attributeList.add(new_id_attribute);
        Attribute ngram_attribute = AttributeFactory.createAttribute("char_ngram", Ontology.STRING);
        attributeList.add(ngram_attribute);
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            double id = example.getValue(attributes.get(id_column));
            String content  = example.getValueAsString(attributes.get(content_column));
            Pattern regex = null;
            switch(token_type.toUpperCase()){
                case "LEX_ALPHA" :
                    regex = LEX_ALPHA;
                    break;
                case "LEX_WORDS" :
                    regex = LEX_WORDS;
                    break;
                case "LEX_NONWHITESPACE_TOGETHER" :
                    regex =LEX_NONWHITESPACE_TOGETHER;
                    break;
                case "LEX_WORD_CLASSES" :
                    regex = LEX_WORD_CLASSES;
                    break;
                case "LEX_NONWHITESPACE_CLASSES" :
                    regex =LEX_NONWHITESPACE_CLASSES;
                    break;
                case "UNICODE_LETTERS" :
                    regex =UNICODE_LETTERS;
                    break;
                case "CUSTOM_REGULAR_EXPRESSION" :
                    regex = Pattern.compile(regular);
            }
            CharSequenceLexer lexer = new CharSequenceLexer (regex);
            lexer.setCharSequence (content);
            while (lexer.hasNext()) {
                lexer.next();
                String sub = content.substring(lexer.getStartOffset (), lexer.getEndOffset ());
                DataRowFactory factory = new DataRowFactory(0, '.');
                DataRow dataRow = factory.create(attributeList.size());
                dataRow.set(new_id_attribute, id);
                dataRow.set(ngram_attribute, ngram_attribute.getMapping().mapString(sub));
                exampleTable.addDataRow(dataRow);
            }
        }
        ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSet1);

    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(CONTENT_ATTRIBUTE_NAME, "The name of the string attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeCategory(TOKEN_TYPE,"The method of segment text.", TYPE_NAMES, 0, false));

        ParameterType type = new ParameterTypeRegexp(PARAMETER_REGEX_EXPRESSION, "Custom regular expressions.",true);
        type.registerDependencyCondition(new EqualStringCondition(this, TOKEN_TYPE, false, "custom_regular_expression"));
        types.add(type);
        return types;
    }
}
