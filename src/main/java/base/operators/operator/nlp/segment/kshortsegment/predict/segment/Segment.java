package base.operators.operator.nlp.segment.kshortsegment.predict.segment;

import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.nlp.segment.kshortsegment.predict.dictionary.DictManager;
import base.operators.operator.nlp.segment.kshortsegment.predict.utils.BCConvert;
import base.operators.operator.nlp.segment.kshortsegment.training.Item;
import base.operators.operator.nlp.segment.kshortsegment.training.NatureDictionaryMaker;

import java.util.*;


/**
 * @author zls
 * create time:  2019.03.19.
 * description:
 */
public abstract class Segment {

    static DictManager dictManager;
    static WordGraphFactory graphFactory;
    static int SEN_OFF_SET = 0;//句子在文本中的偏移量

    static NatureDictionaryMaker userModel = null;
    static ExampleSet dictExampleSetInput = null;
    static String wordColName = null;
    static String posColName = null;
    Segment(NatureDictionaryMaker userModel){
        this.userModel = userModel;
        if(dictManager == null){
            loadDict();
        }
        if(graphFactory == null){
            loadModel();
        }
    }
    Segment(NatureDictionaryMaker userModel, ExampleSet dictExampleSetInput, String wordColName, String posColName){
         this.userModel = userModel;
         this.dictExampleSetInput = dictExampleSetInput;
         this.wordColName = wordColName;
         this.posColName = posColName;
         if(dictManager == null){
            loadDict();
        }
        if(graphFactory == null){
            loadModel();
        }
    }

    //加载字典，包括外部词典和内部词典、模型词典
    private void loadDict(){
        List<String> data1 = new ArrayList<>();
        List<String> data2 = new ArrayList<>();
        Map<String, List<String>> data = new HashMap<>();
        if(userModel!=null){
            System.out.println("读取用户模型中词典...");
            Set<Map.Entry<String, Item>> entries = userModel.dictionaryMaker.trie.entrySet();
            for (Map.Entry<String, Item> entry : entries) {
                data1.add(entry.getValue().toString());
            }
        }

        if(dictExampleSetInput!=null && wordColName!=null && posColName!=null) {
            System.out.println("读取附加词典...");
            for (Example example : dictExampleSetInput){
                StringBuilder builder = new StringBuilder();
                builder.append(example.getValueAsString(dictExampleSetInput.getAttributes().get(wordColName)));
                builder.append(" ");
                builder.append(example.getValueAsString(dictExampleSetInput.getAttributes().get(posColName)));
                data2.add(builder.toString());
            }
        }

        data.put("tf", data1);
        data.put("custom", data2);
        System.out.println("词典加载...");
        dictManager = DictManager.getInstance(data);
    }

    //加载模型
    private void loadModel(){
        if(userModel!=null){
            System.out.println("读取用户模型...");

            List<String> data1 = userModel.nGramDictionaryMaker.tmDictionaryMaker.toList();

            List<String> data2 = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : userModel.nGramDictionaryMaker.trie.entrySet()) {
                data2.add(entry.getKey() + "\t" + entry.getValue()+userModel.nGramDictionaryMaker.trie.getPosTransResult(entry.getKey()));
            }
            System.out.println("构建用户模型...");
            graphFactory = new WordGraphFactory(data1, data2);
        }
        else {
            System.out.println("构建系统模型...");
            graphFactory = new WordGraphFactory();
        }

    }

    private String normalize(String text) {
        return BCConvert.qj2bj(text.trim()).toUpperCase();
    }

    private List<String> toSentenceList(char[] chars) {

        StringBuilder sb = new StringBuilder();

        List<String> sentences = new LinkedList<String>();

        for (int i = 0; i < chars.length; ++i) {
            if (sb.length() == 0 && (Character.isWhitespace(chars[i]) || chars[i] == ' ')) {
                continue;
            }

            sb.append(chars[i]);
            switch (chars[i]) {
                case '.':
                    if (i < chars.length - 1 && chars[i + 1] > 128) {
                        insertIntoList(sb, sentences);
                        sb = new StringBuilder();
                    }
                    break;
                case '…': {
                    if (i < chars.length - 1 && chars[i + 1] == '…') {
                        sb.append('…');
                        ++i;
                        insertIntoList(sb, sentences);
                        sb = new StringBuilder();
                    }
                }
                break;
                case '。':
//			case '，':
//			case ',':
//				insertIntoList(sb, sentences);
//				sb = new StringBuilder();
//				break;
//			case ';':
//			case '；':
//				insertIntoList(sb, sentences);
//				sb = new StringBuilder();
//				break;
                case '!':
                case '！':
                    insertIntoList(sb, sentences);
                    sb = new StringBuilder();
                    break;
                case '?':
                case '？':
                    insertIntoList(sb, sentences);
                    sb = new StringBuilder();
                    break;
                case '\n':
                    String content = sb.toString().trim();
                    if (content.length() > 0) {
                        sentences.add(content+"\n");
                    }
                    sb = new StringBuilder();
                    break;
                case '\r':
                    insertIntoList(sb, sentences);
                    sb = new StringBuilder();
                    break;
            }
        }

        if (sb.length() > 0) {
            insertIntoList(sb, sentences);
        }

        return sentences;
    }
    private void insertIntoList(StringBuilder sb, List<String> sentences) {
        String content = sb.toString().trim();
        if (content.length() > 0) {
            sentences.add(content);
        }
    }

    abstract List<Word> segSentence(String sentence);

    private String wordlist2String(List<Word> sentence,boolean posflag){
        StringBuffer result = new StringBuffer();
        if(sentence.size()>=1){
            for(Word w:sentence){
                if(w.getRealWord()!=null && !" ".equals(w.getRealWord())){//去掉begin和end
                    result.append(w.getRealWord());
                    if(true==posflag){
                        result.append("/"+w.getPosTag().toString());
                    }
                    result.append(" ");
                }
                if (w.getRealWord() == "\n"){
                    result.append("\n");
                }
            }
        }
        return result.toString();
    }

    /**
     * 给一个文本分词
     *
     * @param text
     *            待分词文本
     * @param posflag
     *            是否带词性标注
     * @return 分词后的词列表
     */
    public String segment(String text, boolean posflag){
        if (text == null)
            return null;
        String normalText = normalize(text);
        String[] paraArray=normalText.split("\r\n");

        StringBuilder sb=new StringBuilder();
        for(int i=0;i<paraArray.length;i++){
            String paraText=paraArray[i];
            List<String> sentenceList = toSentenceList(paraText.toCharArray());
            List<Word> wordList = new LinkedList<Word>();
            SEN_OFF_SET = 0;
            for (int j = 0; j < sentenceList.size(); j++) {
                String sentence = sentenceList.get(j);
                wordList.addAll(segSentence(sentence.trim()));
                SEN_OFF_SET+=sentence.length();
            }
            String paraPosTag = wordlist2String(wordList,posflag);
            sb.append(paraPosTag);
            sb.append("\r\n");
        }
        String temp = sb.toString();
        return temp.substring(0, temp.length()-2);
    }
}
