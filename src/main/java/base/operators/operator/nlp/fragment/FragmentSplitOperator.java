package base.operators.operator.nlp.fragment;

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
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FragmentSplitOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    private static final String P_REGEX = ".+?(\\n|\\r)";//默认段落分割正则表达式
    //private static final String S_REGEX = ".+?(。|\\.|！|!|？|\\?|\\n|\\r)";//默认句子分割正则表达式
    private static final String S_REGEX = "(?!\\s).+?(?:[。！？\\n\\r!?]|$|(?:(?<!\\d)\\.(?!\\d)))";//默认句子分割正则表达式,去除了小数被切分的情况

    public static final String DOC_ID_ATTRIBUTE_NAME = "doc_id_attribute_name";
    public static final String DOC_CONTENT_ATTRIBUTE_NAME = "doc_content_attribute_name";
    public static final String SPLIT_TYPE = "split_type";//0表示分段,1表示分句
    public static final String SPLIT_REGEX = "split_regex";//分割符，多个分割符用英文逗号隔开

    public static String[] SPLIT_TYPES = {"paragraph segmentation","sentence segmentation"};

    public FragmentSplitOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ID_ATTRIBUTE_NAME, DOC_CONTENT_ATTRIBUTE_NAME)));

    }

    @Override
    public void doWork() throws OperatorException {
        String id_column = getParameterAsString(DOC_ID_ATTRIBUTE_NAME);
        String doc_column = getParameterAsString(DOC_CONTENT_ATTRIBUTE_NAME);
        int split_type = getParameterAsInt(SPLIT_TYPE);

        String splitRegex = "";
        try{
            splitRegex = getParameterAsString(SPLIT_REGEX);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        if(("".equals(splitRegex) || splitRegex==null)){
            if(1==split_type){//1表示分句
                splitRegex = S_REGEX;
            }else if(0==split_type){//0表示分段
                splitRegex = P_REGEX;
            }
        }
//        else{
//            String[] splitSymbolsArray = symbols.split(",");//可写多个符号（每个符号为正则表达），多个符号用逗号隔开
//            splitRegex = ".+?(";
//            int sNum = 0;
//            for(String s : splitSymbolsArray){//组装分割的正则表达式
//                sNum++;
//                if(isEscapeCharacter(s)){//判断是否转义字符
//                    s += "\\"+s;
//                }
//                splitRegex += s;
//                if(sNum<splitSymbolsArray.length){
//                    splitRegex += "|";
//                }
//            }
//            splitRegex += ")";
//        }

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();

        //构造输出
        List<Attribute> attributeList = new ArrayList<>();
        //序号列
        Attribute index_attribute = AttributeFactory.createAttribute("index", Ontology.NUMERICAL);
        attributeList.add(index_attribute);
        //篇章id
        Attribute new_id_attribute = AttributeFactory.createAttribute(id_column, attributes.get(id_column).isNumerical() ? Ontology.NUMERICAL : Ontology.NOMINAL);
        attributeList.add(new_id_attribute);
        //片段id
        Attribute frag_id_attribute = AttributeFactory.createAttribute("frag_id", Ontology.NUMERICAL);
        attributeList.add(frag_id_attribute);
        //片段内容
        Attribute frag_content_attribute = AttributeFactory.createAttribute("frag_content", Ontology.STRING);
        attributeList.add(frag_content_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
        int index_out = 0;
        for(int i = 0; i < exampleSet.size(); i++){
            Example example = exampleSet.getExample(i);
            double docId = example.getValue(attributes.get(id_column));
            String docContent = example.getValueAsString(attributes.get(doc_column));
            List<String> matchResult = matchSymbols(docContent, splitRegex);
            int fragIndex_out = 0;
            if(matchResult!=null && matchResult.size()>0){
                for(String f : matchResult){
                    DataRowFactory factory = new DataRowFactory(0, '.');
                    DataRow dataRow = factory.create(attributeList.size());
                    dataRow.set(index_attribute, index_out);
                    dataRow.set(new_id_attribute, docId);
                    dataRow.set(frag_id_attribute, fragIndex_out);
                    dataRow.set(frag_content_attribute, frag_content_attribute.getMapping().mapString(f));
                    exampleTable.addDataRow(dataRow);
                    index_out++;
                    fragIndex_out++;
                }
            }
        }

        ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSet1);

    }

    /**
     * 判断是否转义字符
     * @param symbol 待判断的符号
     * @return 是否为转移字符
     */
    private static boolean isEscapeCharacter(String symbol){
        boolean flag = false;
        String[] escapeCharacter = {"$", "(", ")", "*", "+", ".", "[", "]", "?", "\\", "^", "{", "}", "|"};
        for(String s:escapeCharacter){
            if(s.equals(symbol)){
                flag = true;
            }
        }
        return flag;
    }

    /**
     *
     * @param content 待切分的字符串
     * @param regex 分割符（正则表达式）
     * @return 切分后的字符片段
     */
    private static List<String> matchSymbols(String content,String regex){
        List<String> matchResult = new ArrayList<String>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        int lastEndIndex = 0;
        while (matcher.find()) {
            String sentence = matcher.group();
            if(!"".equals(sentence.trim())){
                matchResult.add(sentence);
            }
            lastEndIndex=matcher.end();
        }
        if(lastEndIndex!=content.length()-1){
            String restSentence = content.substring(lastEndIndex);
            if(!"".equals(restSentence.trim())){
                matchResult.add(restSentence);
            }
        }
        return matchResult;
    }

    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ID_ATTRIBUTE_NAME, "The name of the doc id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(DOC_CONTENT_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeCategory(SPLIT_TYPE, "The split type.",
                SPLIT_TYPES, 0, false));
        types.add(new ParameterTypeRegexp(SPLIT_REGEX, "The split regex.", true, false));
        return types;
    }

}
