package base.operators.operator.nlp.segment;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.segment.mechanicalsegment.DictManager;
import base.operators.operator.nlp.segment.mechanicalsegment.IsCharType;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.tools.Ontology;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MechanicalSegmentOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort dictExampleSetInput = getInputPorts().createPort("dictionary example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String WORD_ATTRIBUTE_NAME = "word_attribute_name";
    public static final String POS_ATTRIBUTE_NAME = "part_of_speech_attribute_name";

    public static final String IS_IDENTIFY = "identification_item";//"识别项";
    public static final String IS_NUMBER = "identify_number";
    public static final String IS_LETTER = "identify_letter";

    public static final String LANGUAGE = "language";
    public static String[] LANGUAGES = {"Chinese"};

    public static final String SEPARATION_METHOD = "separation_method";
    public static String[] METHODS_EN = {"forward maximum matching","backward maximum matching","bidirectional maximum matching"};
    public static String[] METHODS_CH = {"前向最大匹配","后向最大匹配","双向最大匹配"};

    public static final String WILDCARD_REPLACE = "wildcard_replace";//"通配符替换";
    public static final String REPLACE_STRAT = "replace the beginning of the substitution sentence with '始##始'";//"句子的开始替换为\"始##始\"";
    public static final String REPLACE_END = "replace the end of the substitution sentence with '末##末'";//"结束替换为\"末##末\"";
    public static final String REPLACE_NUMERAL = "replace numerals with '未##数'";//"数词替换为\"未##数\"";
    public static final String REPLACE_TIME = "replace time with '未##时'";//"时间替换为\"未##时\"";
    public static final String REPLACE_STRING = "replace string with '未##串'";//"字符串替换为\"未##串\"";
    public static final String REPLACE_LOCATION = "replace location with '未##地'";//"地名替换为\"未##地\"";
    public static final String REPLACE_PERSON = "replace person with '未##人'";//"人名替换为\"未##人\"";

    public static final String PART_OF_SPEECH ="part_of_speech";//"词性标注";

    public static final String FILTER_NUMBER ="words that filter word segmentation results into Numbers";//"过滤分词结果为数字的词";
    public static final String FILTER_ENGLISH ="words that filter word segmentation results into English";//"过滤分词结果为全英文的词";
    public static final String FILTER_PUNCTUATION ="words that filter word segmentation results into Punctuation";//"过滤分词结果为标点符号的词";

    public MechanicalSegmentOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ATTRIBUTE_NAME)));
        dictExampleSetInput.addPrecondition(
                new AttributeSetPrecondition(dictExampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, WORD_ATTRIBUTE_NAME, POS_ATTRIBUTE_NAME)));
    }

    public void doWork() throws OperatorException {
        String selected_column = getParameterAsString(DOC_ATTRIBUTE_NAME);
        String word_column = getParameterAsString(WORD_ATTRIBUTE_NAME);
        String pos_column = getParameterAsString(POS_ATTRIBUTE_NAME);
        //获取词典
        ExampleSet dictExampleSet = (ExampleSet) dictExampleSetInput.getData(ExampleSet.class).clone();
        DictManager.initDict(dictExampleSet, word_column, pos_column);
        //获取数据表
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        ExampleTable table = exampleSet.getExampleTable();
        Attribute newAttribute = AttributeFactory.createAttribute(selected_column+"_segment", Ontology.STRING);
        table.addAttribute(newAttribute);
        exampleSet.getAttributes().addRegular(newAttribute);
        //是否识别数字和字母
        boolean isIdentify = getParameterAsBoolean(IS_IDENTIFY);
        boolean isNum = getParameterAsBoolean(IS_NUMBER);
        boolean isLetter = getParameterAsBoolean(IS_LETTER);

        // 是否过滤数字
        boolean filterNum = getParameterAsBoolean(FILTER_NUMBER);
        // 是否过滤英文字母
        boolean filterEnWord = getParameterAsBoolean(FILTER_ENGLISH);
        // 是否过滤标点符号
        boolean filterPunctuation = getParameterAsBoolean(FILTER_PUNCTUATION);

        // 是否词性标注
        boolean posTag = getParameterAsBoolean(PART_OF_SPEECH);
        // 切分方式
        int segType = getParameterAsInt(SEPARATION_METHOD);

        // 替换
        boolean start = getParameterAsBoolean(REPLACE_STRAT);
        boolean end = getParameterAsBoolean(REPLACE_END);
        boolean loc = getParameterAsBoolean(REPLACE_LOCATION);
        boolean num = getParameterAsBoolean(REPLACE_NUMERAL);
        boolean time = getParameterAsBoolean(REPLACE_TIME);
        boolean string = getParameterAsBoolean(REPLACE_STRING);
        boolean person = getParameterAsBoolean(REPLACE_PERSON);
        String P_REGEX = ".+?(\\r\\n|\\n|\\r)";
        for (Example example : exampleSet) {
            String text = example.getValueAsString(exampleSet.getAttributes().get(selected_column));
            if(text != null && !"".equals(text)) {
                Pattern pattern = Pattern.compile(P_REGEX);
                Matcher matcher = pattern.matcher(text);
                int lastEndIndex = 0;
                List<String> matchResult = new ArrayList<String>();
                while (matcher.find()) {
                    String sentence = matcher.group();
                    if(!"".equals(sentence.trim())){
                        matchResult.add(sentence);
                    }
                    lastEndIndex=matcher.end();
                }
                if(lastEndIndex!=text.length()-1){
                    String restSentence = text.substring(lastEndIndex);
                    matchResult.add(restSentence);
                }
                List<String> segList = new ArrayList<String>();
                for(String sentence : matchResult) {
                    List<Map<String, String>> list = new ArrayList<Map<String, String>>();
                    if(segType == 0) {
                        list = maxsegSentence(sentence, true, isNum, isLetter);
                    } else if(segType == 1) {
                        list = maxsegSentence(sentence, false, isNum, isLetter);
                    } else if(segType == 2) {
                        list = biMaxsegSentence(sentence, isNum, isLetter);
                    }
                    StringBuffer sb = new StringBuffer();
                    if(start == true) {
                        sb.append("始##始 ");
                    }
                    for (int i = 0; i < list.size(); i++) {
                        Map<String, String> item = list.get(i);
                        String wordItem = item.get("word");
                        String pos = item.get("nature");
                        if(num == true && pos.startsWith("m")) {
                            pos = "未##数";
                        }
                        if(time == true && pos.equals("nt")) {
                            pos = "未##时";
                        }
                        if(string == true && pos.equals("ws")) {
                            pos = "未##串";
                        }
                        if(loc == true && pos.equals("ns")) {
                            pos = "未##地";
                        }
                        if(person == true && pos.startsWith("nh")) {
                            pos = "未##人";
                        }
                        if(posTag == true) {
                            if(!(filterNum == true && "m".equals(pos) || filterEnWord == true && "ws".equals(pos) || filterPunctuation == true && "wp".equals(pos))) {
                                sb.append(wordItem + "/" + item.get("nature") + " ");
                            }

                        } else {
                            if(!(filterNum == true && "m".equals(pos) || filterEnWord == true && "ws".equals(pos) || filterPunctuation == true && "wp".equals(pos))) {
                                sb.append(wordItem + " ");
                            }
                        }
                    }
                    if(end == true) {
                        sb.append("末##末");
                    }
                    segList.add(sb.toString());
                }
                example.setValue(newAttribute, newAttribute.getMapping().mapString(String.join("\r\n", segList)));
            } else {
                example.setValue(newAttribute, newAttribute.getMapping().mapString(""));
            }
        }
        exampleSetOutput.deliver(exampleSet);

    }

    private static List<Map<String, String>> beforeCombineWord(List<Map<String, String>> seg_list, String comPos) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        int i = 0;
        while (i < seg_list.size()) {
            if (!(comPos.equals(seg_list.get(i).get("nature")))) {
                result.add(seg_list.get(i));
                i++;
            } else {
                String pos = seg_list.get(i).get("nature");
                String word = seg_list.get(i).get("word");
                int j = i + 1;
                while (j < seg_list.size()) {
                    if (pos.equals(seg_list.get(j).get("nature"))) {
                        word += seg_list.get(j).get("word");
                        j++;
                        i = j;
                    } else {
                        i = j;
                        break;
                    }
                }
                Map<String, String> word_map = new HashMap<>();
                word_map.put("word", word);
                word_map.put("nature", pos);
                result.add(word_map);
                if (j == seg_list.size()) {
                    break;
                }
            }
        }
        return result;
    }

    private static List<Map<String, String>> afterCombineWord(List<Map<String, String>> seg_list, String comPos) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        int i = seg_list.size() - 1;
        while (i >= 0) {
            if (!("ws".equals(seg_list.get(i).get("nature")) || "m".equals(seg_list.get(i).get("nature")))) {
                result.add(seg_list.get(i));
                i--;
            } else {
                String pos = seg_list.get(i).get("nature");
                String word = seg_list.get(i).get("word");
                int j = i - 1;
                while (j >= 0) {
                    if (pos.equals(seg_list.get(j).get("nature"))) {
                        word += seg_list.get(j).get("word");
                        j--;
                        i = j;
                    } else {
                        i = j;
                        break;
                    }
                }
                Map<String, String> word_map = new HashMap<>();
                word_map.put("word", word);
                word_map.put("nature", pos);
                result.add(word_map);
                if (j < 0) {
                    break;
                }
            }
        }
        return result;
    }

    /*
     * 前向最大分词/后向最大分词
     *
     * @param sentence 待分词的句子
     *
     * @param isForward 如果是true代表前向最大分词，false代表后向最大分词
     */
    private static List<Map<String, String>> maxsegSentence(String sentence, boolean isForward, boolean isNum, boolean isLetter) {
        // sentence += "一台" + new
        // String(Character.toChars(Integer.parseInt("1D306", 16)))+"33d打印机";
        // sentence = "aas7研究生命的奇迹4a/aaaa";
        // sentence = "a/a奇迹4a/a";
        // sentence =
        // "在国科管系统基础数据资源中要对系统中的科技项目、科技人员、科技成果等数据按照《中国图书资料分类法（第四版）》和《中华人民共和国学科分类与代码（国家标准GB/T
        // 13745-2009）》等分类标准进行分类标引和关键词标引，达到标引规范科学，标引结果准确，标引复用性高的要求。";
        // sentence = "一个鞋店,皮鞋匠。";
        List<Map<String, String>> seg_list = new ArrayList<Map<String, String>>();
        List<Map<String, String>> seg_list_new = new ArrayList<Map<String, String>>();
        // 词库匹配分词
        String sentenceSP = "";
        String sentenceLC = "";
        // System.out.println("原句："+sentence);
        // ===============去首尾空格===========================
        sentenceSP = sentence.trim();// 去首尾空格
        // System.out.println("去首尾空格后："+sentenceSP);
        // ================转小写==========================
        sentenceLC = sentenceSP.toLowerCase();

        int senLength = sentenceLC.length();
        // 考虑代码点位扩展
        int senCodeLength = sentenceLC.codePointCount(0, senLength);// 考虑扩展unicode字符情况，需要检测代码点位
        if (isForward) {
            List<Integer> commonPrefixList = new ArrayList<Integer>();// 相同前缀的单词集合
            int index = 0;
            String subStr = null;
            String subFirst = null;
            while (index < senCodeLength) {
                if (senLength == senCodeLength) {
                    subStr = sentenceLC.substring(index);// 从index索引点取到句子末尾
                    subFirst = subStr.substring(0, 1);
                } else {
                    subStr = getSubString(sentence, senCodeLength, index);
                    subFirst = getSubString(subStr, 1, 0);
                }
                commonPrefixList = DictManager.dat.commonPrefixSearch(subStr);
                Map<String, String> word_map = new HashMap<String, String>();
                if (commonPrefixList.size() > 0) {
                    String word = DictManager.words.get(commonPrefixList.get(commonPrefixList.size() - 1));
                    TreeMap<String, Double> posMap = DictManager.dictMap.get(word);
                    Iterator<String> iter = posMap.keySet().iterator();
                    Double max = 0.0;
                    String pos = null;
                    while (iter.hasNext()) {
                        String poTemp = iter.next();
                        Double value = posMap.get(poTemp);
                        if (max < value) {
                            max = value;
                            pos = poTemp;
                        }
                    }
                    word_map.put("word", word);
                    word_map.put("nature", pos);
                    seg_list.add(word_map);
                    index += word.length();
                } else if (commonPrefixList.size() == 0) {
                    word_map.put("word", subFirst);
                    if (IsCharType.isLetter(subFirst)) {
                        word_map.put("nature", "ws");
                    } else if (IsCharType.isNum(subFirst)) {
                        word_map.put("nature", "m");
                    } else if (IsCharType.isPunctuation(subFirst)) {
                        word_map.put("nature", "wp");
                    }

                    seg_list.add(word_map);
                    index++;
                }
            }
            seg_list_new = seg_list;
            if (isNum == true) {
                seg_list_new = beforeCombineWord(seg_list, "m");
            }
            if(isLetter == true) {
                seg_list_new = beforeCombineWord(seg_list_new, "ws");
            }

        } else {
            int index = senCodeLength;
            String subStr = null;
            String subEnd = null;
            while (index > 0) {
                Map<String, String> word_map = new HashMap<String, String>();
                if (senLength == senCodeLength) {
                    subStr = sentenceLC.substring(0, index);// 从句子开头到index
                    subEnd = subStr.substring(subStr.length() - 1);
                    int notMatchCount = 0;
                    for (int i = 0; i < subStr.length(); i++) {
                        if (DictManager.dat.exactMatchSearch(subStr.substring(i)) != -1) {
                            int exactMatch = DictManager.dat.exactMatchSearch(subStr.substring(i));
                            String word = DictManager.words.get(exactMatch);
                            TreeMap<String, Double> posMap = DictManager.dictMap.get(word);
                            Iterator<String> iter = posMap.keySet().iterator();
                            Double max = 0.0;
                            String pos = null;
                            while (iter.hasNext()) {
                                String poTemp = iter.next();
                                Double value = posMap.get(poTemp);
                                if (max < value) {
                                    max = value;
                                    pos = poTemp;
                                }
                            }
                            word_map.put("word", word);
                            word_map.put("nature", pos);
                            seg_list.add(word_map);
                            index -= word.length();
                            break;
                        } else {
                            notMatchCount++;
                        }
                    }
                    if (notMatchCount == subStr.length()) {
                        word_map.put("word", subEnd);
                        if (IsCharType.isLetter(subEnd)) {
                            word_map.put("nature", "ws");
                        } else if (IsCharType.isNum(subEnd)) {
                            word_map.put("nature", "m");
                        } else if (IsCharType.isPunctuation(subEnd)) {
                            word_map.put("nature", "wp");
                        }
                        seg_list.add(word_map);
                        index--;
                    }
                } else {
                    subStr = getSubString(sentence, index, 0);
                    int subStrCodeLength = subStr.codePointCount(0, subStr.length());
                    subEnd = getSubString(subStr, subStrCodeLength, subStrCodeLength - 1);
                    int notMatchCount = 0;
                    for (int i = 0; i < subStrCodeLength; i++) {
                        if (DictManager.dat.exactMatchSearch(getSubString(subStr, subStrCodeLength, i)) != -1) {
                            int exactMatch = DictManager.dat
                                    .exactMatchSearch(getSubString(subStr, subStrCodeLength, i));
                            String word = DictManager.words.get(exactMatch);
                            TreeMap<String, Double> posMap = DictManager.dictMap.get(word);
                            Iterator<String> iter = posMap.keySet().iterator();
                            Double max = 0.0;
                            String pos = null;
                            while (iter.hasNext()) {
                                String poTemp = iter.next();
                                Double value = posMap.get(poTemp);
                                if (max < value) {
                                    max = value;
                                    pos = poTemp;
                                }
                            }
                            word_map.put("word", word);
                            word_map.put("nature", pos);
                            seg_list.add(word_map);
                            index -= word.length();
                            break;
                        } else {
                            notMatchCount++;
                        }
                    }
                    if (notMatchCount == subStrCodeLength) {
                        word_map.put("word", subEnd);
                        if (IsCharType.isLetter(subEnd)) {
                            word_map.put("nature", "ws");
                        } else if (IsCharType.isNum(subEnd)) {
                            word_map.put("nature", "m");
                        } else if (IsCharType.isPunctuation(subEnd)) {
                            word_map.put("nature", "wp");
                        }
                        seg_list.add(word_map);
                        index--;
                    }
                }
            }
            seg_list_new = seg_list;
            if (isNum ==true) {
                seg_list_new = afterCombineWord(seg_list, "m");
            }
            if(isLetter == true) {
                seg_list_new = afterCombineWord(seg_list_new, "ws");
            }
            Collections.reverse(seg_list_new);
        }
        return seg_list_new;
    }

    private static List<Map<String, String>> biMaxsegSentence(String sentence, boolean isNum, boolean isStr) {
        List<Map<String, String>> forwordResult = maxsegSentence(sentence, true, isNum, isStr);
        List<Map<String, String>> backwordResult = maxsegSentence(sentence, false, isNum, isStr);
        if (forwordResult.size() > backwordResult.size()) {
            return backwordResult;
        } else if (forwordResult.size() < backwordResult.size()) {
            return forwordResult;
        } else {
            int forwordSingleCount = 0;
            for (Map<String, String> word_map : forwordResult) {
                forwordSingleCount += (word_map.get("word").length() == 1 ? 1 : 0);
            }
            int backwordSingleCount = 0;
            for (Map<String, String> word_map : backwordResult) {
                backwordSingleCount += (word_map.get("word").length() == 1 ? 1 : 0);
            }
            return backwordSingleCount < forwordSingleCount ? backwordResult : forwordResult;
        }
    }

    // 按照代码点数，取子串，从offset位置到codePointCount位置的字符串
    private static String getSubString(String sentence, int codePointCount, int offset) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < codePointCount; i++) {
            int index = sentence.offsetByCodePoints(0, i);
            // public int offsetByCodePoints(int index,int
            // codePointOffset)返回此String中从给定的index处偏移codePointOffset个代码点的索引。
            int cpp = sentence.codePointAt(index);
            sb.appendCodePoint(cpp);
        }
        return sb.toString();
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput, false));
        types.add(new ParameterTypeAttribute(WORD_ATTRIBUTE_NAME, "The name of the word attribute in dictionary.", dictExampleSetInput, false));
        types.add(new ParameterTypeAttribute(POS_ATTRIBUTE_NAME, "The name of the part of speech attribute in dictionary.", dictExampleSetInput, false));

        types.add(new ParameterTypeCategory(LANGUAGE,"The language of text.",LANGUAGES, 0, false));
        types.add(new ParameterTypeCategory(SEPARATION_METHOD,"The method of segment text.",METHODS_EN, 0, false));

        types.add(new ParameterTypeBoolean(IS_IDENTIFY, "Is there an identifier?",false, false));
        ParameterType type = new ParameterTypeBoolean(IS_NUMBER, "Whether to identify number.", false, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
        types.add(type);
        type = new ParameterTypeBoolean(IS_LETTER, "Whether to identify letter.", false, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
        types.add(type);

        types.add(new ParameterTypeBoolean(WILDCARD_REPLACE, "Is there an wildcard replacement?",false, false));
        type = new ParameterTypeBoolean(REPLACE_STRAT, "Whether to replace start.", false, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
        types.add(type);
        type = new ParameterTypeBoolean(REPLACE_END, "Whether to merge replace ending.", false, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
        types.add(type);
        type = new ParameterTypeBoolean(REPLACE_TIME, "Whether to replace time.", false, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
        types.add(type);
        type = new ParameterTypeBoolean(REPLACE_NUMERAL, "Whether to replace number.", false, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
        types.add(type);
        type = new ParameterTypeBoolean(REPLACE_STRING, "Whether to replace string.", false, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
        types.add(type);
        type = new ParameterTypeBoolean(REPLACE_LOCATION, "Whether to replcae location.", false, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
        types.add(type);
        type = new ParameterTypeBoolean(REPLACE_PERSON, "Whether to replace person.", false, false);
        type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
        types.add(type);

        types.add(new ParameterTypeBoolean(PART_OF_SPEECH, "Whether to save part of speech?",false, false));
        types.add(new ParameterTypeBoolean(FILTER_ENGLISH, "Whether to filter english or not?",false, false));
        types.add(new ParameterTypeBoolean(FILTER_NUMBER, "Whether to filter number or not?",false, false));
        types.add(new ParameterTypeBoolean(FILTER_PUNCTUATION, "Whether to filter punctuation or not?",false, false));

        return types;
    }



}
