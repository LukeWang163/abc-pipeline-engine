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
import base.operators.operator.UserError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.tools.Ontology;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by wangpanpan on 2019/9/06.
 */
public class SimpleTokenizerOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort stopDictExampleSetInput = getInputPorts().createPort("stop dicttionary example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String CONTENT_ATTRIBUTE_NAME = "content_attribute_name";
    public static final String TOKEN_TYPE = "token_type";
    public static String[] TYPE_NAMES_EN = new String[] { "use default English stop words","use user-defined stop words"};
    public static String[] TYPE_NAMES_CH = new String[] { "使用默认的英文停用词","使用用户自定义停用词"};
    public static final String STOP_WORD_ATTRIBUTE_NAME = "stop_word_attribute_name";

    public SimpleTokenizerOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, ID_ATTRIBUTE_NAME, CONTENT_ATTRIBUTE_NAME)));

        if(stopDictExampleSetInput.isConnected()){
            stopDictExampleSetInput.addPrecondition(
                    new AttributeSetPrecondition(stopDictExampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                            this, STOP_WORD_ATTRIBUTE_NAME)));
        }
    }

    HashSet<String> stoplist = new HashSet<String>();

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        String id_column = getParameterAsString(ID_ATTRIBUTE_NAME);
        String content_column = getParameterAsString(CONTENT_ATTRIBUTE_NAME);
        int token_type = getParameterAsInt(TOKEN_TYPE);

        if(token_type==0){
            defaultStopInitialize();
        }else if(token_type==1){
            if(!stopDictExampleSetInput.isConnected()){
                throw new UserError(this, -1,"The mode is user-defined, but there is no user dictionary input.");
            }else{
                ExampleSet stopDictExampleSet = (ExampleSet) stopDictExampleSetInput.getData(ExampleSet.class).clone();
                String stop_word_col = getParameterAsString(STOP_WORD_ATTRIBUTE_NAME);
                for (Example example : stopDictExampleSet) {
                    stop(example.getValueAsString(stopDictExampleSet.getAttributes().get(stop_word_col)));
                }
            }
        }

        // 构造输出表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute new_id_attribute = AttributeFactory.createAttribute(id_column, attributes.get(id_column).isNumerical() ? Ontology.NUMERICAL : Ontology.NOMINAL);
        attributeList.add(new_id_attribute);
        Attribute token_attribute = AttributeFactory.createAttribute(content_column+"_token", Ontology.STRING);
        attributeList.add(token_attribute);
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        int underscoreCodePoint = Character.codePointAt("_", 0);

        for(int i = 0; i < exampleSet.size(); i++){
            Example example = exampleSet.getExample(i);
            double id = example.getValue(attributes.get(id_column));
            String content  = example.getValueAsString(attributes.get(content_column));

            int[] tokenBuffer = new int[1000];
            int length = -1;

            // Using code points instead of chars allows us
            //  to support extended Unicode, and has no significant
            //  efficiency costs.

            int totalCodePoints = Character.codePointCount(content, 0, content.length());

            for (int j=0; j < totalCodePoints; j++) {

                int codePoint = Character.codePointAt(content, j);
                int codePointType = Character.getType(codePoint);

                if (codePointType == Character.LOWERCASE_LETTER ||
                        codePointType == Character.UPPERCASE_LETTER ||
                        codePoint == underscoreCodePoint) {
                    length++;
                    tokenBuffer[length] = codePoint;
                }
                else if (codePointType == Character.SPACE_SEPARATOR ||
                        codePointType == Character.LINE_SEPARATOR ||
                        codePointType == Character.PARAGRAPH_SEPARATOR ||
                        codePointType == Character.END_PUNCTUATION ||
                        codePointType == Character.DASH_PUNCTUATION ||
                        codePointType == Character.CONNECTOR_PUNCTUATION ||
                        codePointType == Character.START_PUNCTUATION ||
                        codePointType == Character.INITIAL_QUOTE_PUNCTUATION ||
                        codePointType == Character.FINAL_QUOTE_PUNCTUATION ||
                        codePointType == Character.OTHER_PUNCTUATION) {

                    // Things that delimit words
                    if (length != -1) {
                        String token = new String(tokenBuffer, 0, length + 1);
                        if (! stoplist.contains(token)) {
                            DataRowFactory factory = new DataRowFactory(0, '.');
                            DataRow dataRow = factory.create(attributeList.size());
                            dataRow.set(new_id_attribute, id);
                            dataRow.set(token_attribute, token_attribute.getMapping().mapString(token));
                            exampleTable.addDataRow(dataRow);
                        }
                        length = -1;
                    }
                }
                else if (codePointType == Character.COMBINING_SPACING_MARK ||
                        codePointType == Character.ENCLOSING_MARK ||
                        codePointType == Character.NON_SPACING_MARK ||
                        codePointType == Character.TITLECASE_LETTER ||
                        codePointType == Character.MODIFIER_LETTER ||
                        codePointType == Character.OTHER_LETTER) {
                    // Obscure things that are technically part of words.
                    //  Marks are especially useful for Indic scripts.

                    length++;
                    tokenBuffer[length] = codePoint;
                }
                else {
                    // Character.DECIMAL_DIGIT_NUMBER
                    // Character.CONTROL
                    // Character.MATH_SYMBOL
                    //System.out.println("type " + codePointType);
                }

                // Avoid buffer overflows
                if (length + 1 == tokenBuffer.length) {
                    String token = new String(tokenBuffer, 0, length + 1);
                    if (! stoplist.contains(token)) {
                        DataRowFactory factory = new DataRowFactory(0, '.');
                        DataRow dataRow = factory.create(attributeList.size());
                        dataRow.set(new_id_attribute, id);
                        dataRow.set(token_attribute, token_attribute.getMapping().mapString(token));
                        exampleTable.addDataRow(dataRow);                    }
                    length = -1;
                }


            }

            if (length != -1) {
                String token = new String(tokenBuffer, 0, length + 1);
                if (! stoplist.contains(token)) {
                    DataRowFactory factory = new DataRowFactory(0, '.');
                    DataRow dataRow = factory.create(attributeList.size());
                    dataRow.set(new_id_attribute, id);
                    dataRow.set(token_attribute, token_attribute.getMapping().mapString(token));
                    exampleTable.addDataRow(dataRow);
                }
            }

        }
        ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSet1);
    }

    public void defaultStopInitialize(){
        // articles
        stop("the"); stop("a");	stop("an");

        // conjunctions
        stop("and"); stop("or");

        // prepositions
        stop("of");	stop("for"); stop("in");
        stop("on");	stop("to");	stop("with");
        stop("by");

        // definite pronouns
        stop("this"); stop("that"); stop("these");
        stop("those"); stop("some"); stop("other");

        // personal pronouns
        stop("it");	stop("its"); stop("we");
        stop("our");

        // conjuctions
        stop("as"); stop("but"); stop("not");

        // verbs
        stop("do"); stop("does"); stop("is");
        stop("be"); stop("are"); stop("can");
        stop("was"); stop("were");
    }
    public void stop(String word) {
        stoplist.add(word);
    }


    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(CONTENT_ATTRIBUTE_NAME, "The name of the string attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeCategory(TOKEN_TYPE,"The method of segment text.", TYPE_NAMES_EN, 0, false));

        ParameterType type = new ParameterTypeAttribute(STOP_WORD_ATTRIBUTE_NAME, "Stop word attribute name.", stopDictExampleSetInput, true);
        type.registerDependencyCondition(new EqualStringCondition(this, TOKEN_TYPE, false, TYPE_NAMES_EN[1]));
        types.add(type);
        return types;
    }

}
