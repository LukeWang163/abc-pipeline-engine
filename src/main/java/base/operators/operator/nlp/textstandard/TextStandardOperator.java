package base.operators.operator.nlp.textstandard;

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

public class TextStandardOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String SELECTED_ATTRIBUTE_NAME = "attribute_name";
    public static final String UPPER_LOWER_CASE = "upper_lower_case";
    public static String[] CASES_CH = {"不转换大小写","大写转小写","小写转大写"};
    public static String[] CASES_EN = {"no case conversion","uppercase to lowercase","lowercase to uppercase"};

    public static final String FULL_HALF_ANGLE_CONVERSION = "full_half_angle_conversion";
    public static String[] CONVERSIONS_CH = {"不进行转换","全角转半角","半角转全角"};
    public static String[] CONVERSIONS_EN = {"no case conversion","full-angle turn half-angle","half-angle turn full-angle"};

    public static final String DELETE_NUMBER = "delete_number";
    public static final String DELETE_EMAIL = "delete_email";
    public static final String DELETE_URL = "delete_url";

    public static final String SPECIAL_SYMBOL_REPLACE = "special_symbol_replace";
    public static final String REPLACE_REGEXP = "replace_regex";
    public static final String REPLACE_WITH = "replace_with";


    public TextStandardOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, SELECTED_ATTRIBUTE_NAME)));

    }

    public void doWork() throws OperatorException {
        String select_name = getParameterAsString(SELECTED_ATTRIBUTE_NAME);
        int upper_lower_case = getParameterAsInt(UPPER_LOWER_CASE);
        int full_half_conversion = getParameterAsInt(FULL_HALF_ANGLE_CONVERSION);
        boolean delete_number = getParameterAsBoolean(DELETE_NUMBER);
        boolean delete_email = getParameterAsBoolean(DELETE_EMAIL);
        boolean delete_url = getParameterAsBoolean(DELETE_URL);
        List<String[]> special = null;
        try{
            special = getParameterList(SPECIAL_SYMBOL_REPLACE);

        }catch (NullPointerException e){
            e.printStackTrace();
        }

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        ExampleTable table = exampleSet.getExampleTable();
        Attributes attributes = exampleSet.getAttributes();
        Attribute newAttribute = AttributeFactory.createAttribute(select_name+"_standard", Ontology.STRING);
        table.addAttribute(newAttribute);
        attributes.addRegular(newAttribute);

        for (Example example : exampleSet) {
            // 获取原字段所对应的数据
            String text = example.getValueAsString(attributes.get(select_name));
            if(text != null && !"".equals(text)) {
                if(special != null) {
                    for(int i=0; i < special.size(); i++) {
                        Pattern pattern = Pattern.compile(special.get(i)[0]);
                        Matcher matcher = pattern.matcher(text);
                        text = matcher.replaceAll(special.get(i)[1]);
                    }
                }

                if(delete_number == true) {
                    Pattern pattern = Pattern.compile("[\\d]");
                    Matcher matcher = pattern.matcher(text);
                    text = matcher.replaceAll("").trim();
                }
            	/*if(delspecial == 1) {

            	}
            	if(deldup == 1) {

            	}*/
                if(delete_email == true) {
                    String regex_code = "([a-zA-Z_]{1,}[0-9]{0,}@(([a-zA-z0-9]-*){1,}\\.){1,3}[a-zA-z\\-]{1,})|([1-9]\\d{4,10}@qq.com)";
                    Pattern pattern = Pattern.compile(regex_code);
                    Matcher matcher = pattern.matcher(text);
                    text = matcher.replaceAll("").trim();
                }
            	/*if(lexicon == 1) {

            	}*/
                if(delete_url == true) {
                    String regex_code = "(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]";
                    Pattern pattern = Pattern.compile(regex_code);
                    Matcher matcher = pattern.matcher(text);
                    text = matcher.replaceAll("").trim();
                }
                if(upper_lower_case == 2) {
                    text = text.toUpperCase();
                } else if(upper_lower_case == 1) {
                    text = text.toLowerCase();
                }
                if(full_half_conversion == 1) {
                    text = qj2bj(text);
                } else if(full_half_conversion == 2) {
                    text = bj2qj(text);
                }
            	/*if(exv == 1) {

            	}
            	if(seguniq == 1) {

            	}
            	if(slash == 1) {

            	}*/
            }
            example.setValue(newAttribute, newAttribute.getMapping().mapString(text));
        }
        exampleSetOutput.deliver(exampleSet);
    }

    /**
     * ASCII表中可见字符从!开始，偏移位值为33(Decimal)
     */
    static final char DBC_CHAR_START = 33; // 半角!

    /**
     * ASCII表中可见字符到~结束，偏移位值为126(Decimal)
     */
    static final char DBC_CHAR_END = 126; // 半角~

    /**
     * 全角对应于ASCII表的可见字符从！开始，偏移值为65281
     */
    static final char SBC_CHAR_START = 65281; // 全角！

    /**
     * 全角对应于ASCII表的可见字符到～结束，偏移值为65374
     */
    static final char SBC_CHAR_END = 65374; // 全角～

    /**
     * ASCII表中除空格外的可见字符与对应的全角字符的相对偏移
     */
    static final int CONVERT_STEP = 65248; // 全角半角转换间隔

    /**
     * 全角空格的值，它没有遵从与ASCII的相对偏移，必须单独处理
     */
    static final char SBC_SPACE = 12288; // 全角空格 12288

    /**
     * 半角空格的值，在ASCII中为32(Decimal)
     */
    static final char DBC_SPACE = ' '; // 半角空格

    /**
     * <PRE>
     * 全角字符->半角字符转换
     * 只处理全角的空格，全角！到全角～之间的字符，忽略其他
     * </PRE>
     */
    private static String qj2bj(String src) {
        if (src == null) {
            return src;
        }
        StringBuilder buf = new StringBuilder(src.length());
        char[] ca = src.toCharArray();
        for (int i = 0; i < src.length(); i++) {
            if (ca[i] >= SBC_CHAR_START && ca[i] <= SBC_CHAR_END) { // 如果位于全角！到全角～区间内
                buf.append((char) (ca[i] - CONVERT_STEP));
            } else if (ca[i] == SBC_SPACE) { // 如果是全角空格
                buf.append(DBC_SPACE);
            } else { // 不处理全角空格，全角！到全角～区间外的字符
                buf.append(ca[i]);
            }
        }
        return buf.toString();
    }

    /**
     * <PRE>
     * 半角字符->全角字符转换
     * 只处理空格，!到˜之间的字符，忽略其他
     * </PRE>
     */
    private static String bj2qj(String src) {
        if (src == null) {
            return src;
        }
        StringBuilder buf = new StringBuilder(src.length());
        char[] ca = src.toCharArray();
        for (int i = 0; i < ca.length; i++) {
            if (ca[i] == DBC_SPACE) { // 如果是半角空格，直接用全角空格替代
                buf.append(SBC_SPACE);
            } else if ((ca[i] >= DBC_CHAR_START) && (ca[i] <= DBC_CHAR_END)) { // 字符是!到~之间的可见字符
                buf.append((char) (ca[i] + CONVERT_STEP));
            } else { // 不对空格以及ascii表中其他可见字符之外的字符做任何处理
                buf.append(ca[i]);
            }
        }
        return buf.toString();
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(SELECTED_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput));
        types.add(new ParameterTypeCategory(UPPER_LOWER_CASE, "The type of mode.",
                CASES_EN, 0, false));
        types.add(new ParameterTypeCategory(FULL_HALF_ANGLE_CONVERSION, "The type of mode.",
                CONVERSIONS_EN, 0, false));
        types.add(new ParameterTypeBoolean(DELETE_NUMBER, "Whether delete number", false));
        types.add(new ParameterTypeBoolean(DELETE_EMAIL, "Whether delete email", false));
        types.add(new ParameterTypeBoolean(DELETE_URL, "Whether delete URL", false));
        types.add(new ParameterTypeList(SPECIAL_SYMBOL_REPLACE, "Special symbol replace.", new ParameterTypeRegexp(REPLACE_REGEXP, "What you want to replace."), new ParameterTypeString(REPLACE_WITH,"Replacement target content.")));

        return types;
    }

}
