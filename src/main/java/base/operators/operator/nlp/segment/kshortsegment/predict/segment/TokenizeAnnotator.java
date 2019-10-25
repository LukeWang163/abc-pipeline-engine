package base.operators.operator.nlp.segment.kshortsegment.predict.segment;

import base.operators.example.ExampleSet;
import base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.YenTopKShortestPathsAlg;
import base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils.BaseVertex;
import base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils.Graph;
import base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils.Path;
import base.operators.operator.nlp.segment.kshortsegment.predict.tagger.POS;
import base.operators.operator.nlp.segment.kshortsegment.training.NatureDictionaryMaker;

import java.util.*;

/**
 * @author zls
 * create time:  2019.03.16.
 * description:
 */
public class TokenizeAnnotator extends Segment {
    public TokenizeAnnotator(NatureDictionaryMaker userModel){
        super(userModel);
    }
    public TokenizeAnnotator(NatureDictionaryMaker userModel, ExampleSet dictExampleSetInput, String wordColName, String posColName){
        super(userModel, dictExampleSetInput, wordColName, posColName);
    }
    @Override
    public List<Word> segSentence(String sentence){
        //原子分词
        WordNet wordNet = atomSegment(sentence);
        //机械分词
        generateWordNet(wordNet);
        // 添加命名实体识别
        annotateNER(wordNet);

        List<Word> result = getShortestPath(wordNet);

        return result;

    }

    /**
     * 原子分词
     *
     * @param sentence
     * @return
     */
    public WordNet atomSegment(String sentence) {
        WordNet wordNet = new WordNet(sentence);
        // 识别字符串
        Map<Integer, String> wordMap = IDExtract.getLetters(sentence);
        if (wordMap.size() != 0) {
            pushWord(wordMap, POS.ws, wordNet);
            wordNet.setHasLetter(true);
        }
        // 识别数字
        wordMap = IDExtract.getNumbers(sentence);
        if (wordMap.size() != 0) {
            pushWord(wordMap, POS.m, wordNet);
            wordNet.setHasNumber(true);
        }


        pushAtomWord(wordNet);
        return wordNet;
    }

    private void pushAtomWord(WordNet wordNet) {
        List<Word> wordList = new LinkedList<Word>();
        LinkedList<Word> words[] = wordNet.getWords();
        String sentence = wordNet.sentence;
        if (!wordNet.hasExtendedCharset()) {

            for (int i = 1; i < words.length;) {
                if (words[i].isEmpty()) {
                    int j = i;
                    for (; j < words.length - 1; ++j) {
                        if (!words[j].isEmpty())
                            break;
                        char c = sentence.charAt(j - 1);
                        String realWord = c + "";
                        POS posTag = TextTools.testCharType(c);
                        Word word = new Word(realWord,posTag, j, 1, 0);
                        wordList.add(word);
                    }

                    i = j;
                } else
                    i += words[i].getLast().getLength();
            }

        } else {
            for (int i = 1; i < words.length;) {
                if (words[i].isEmpty()) {
                    int j = i;
                    for (; j < words.length - 1; ++j) {
                        if (!words[j].isEmpty())
                            break;
                        int index = sentence.offsetByCodePoints(0, j - 1);
                        int cpp = sentence.codePointAt(index);
                        POS posTag = TextTools.testCharType(cpp);
                        String realWord = new String(Character.toChars(cpp));
                        Word wordItem = new Word(realWord, posTag, j, 1, 0);
                        wordList.add(wordItem);
                    }

                    i = j;
                } else
                    i += words[i].getLast().getLength();
            }
        }
        wordNet.addAll(wordList);
    }

    private void pushWord(Map<Integer, String> wordMap, POS posTag, WordNet wordNet) {
        Iterator it = wordMap.keySet().iterator();
        List<Word> wordList = new LinkedList<Word>();
        while (it.hasNext()) {
            Integer offset = (Integer) it.next();
            String realWord = wordMap.get(offset);
            double frquency=200;
            Word wordItem = new Word(realWord, posTag, offset+1, frquency, 0);
            wordList.add(wordItem);
        }
        wordNet.addAll(wordList);
    }

    /**
     * 基于词典初步分词
     *
     * @param wordNet
     */
    protected void generateWordNet(final WordNet wordNet) {
        String sentence = wordNet.sentence;
        int length = wordNet.getCodePointCount();
        List<Integer> commonPrefixList = new ArrayList<Integer>();
        List<Word> wordList = new LinkedList<Word>();
        String subString = null;
        for (int i = 0; i < length; i++) {
            if (wordNet.hasExtendedCharset()) {
                subString = getSubString(sentence, length, i);
            } else {
                subString = sentence.substring(i);
            }
            commonPrefixList = dictManager.getTrie().commonPrefixSearch(
                    subString);// 判断是否有前缀（判断该字符串是否有根节点）
            int sizePrefix = commonPrefixList.size();// 前缀个数
            if (sizePrefix > 0) {
                for (int indexPrefix = 0; indexPrefix < sizePrefix; indexPrefix++) {// 取出所有前缀词。
                    int wordIndex = commonPrefixList.get(indexPrefix);
                    String realWord = dictManager.getWords().get(wordIndex);
                    TreeMap<POS, Double> posMap = dictManager.getDictMap().get(
                            realWord);
                    Word newWord = new Word(realWord, posMap, i + 1, 0);
                    wordList.add(newWord);
                }
            }
        }
        wordNet.addAll(wordList);
    }

    public static String getSubString(String sentence, int codePointCount,
                                      int offset) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < codePointCount; i++) {
            int index = sentence.offsetByCodePoints(0, i);
            int cpp = sentence.codePointAt(index);
            sb.appendCodePoint(cpp);
        }
        return sb.toString();
    }

    private void annotateNER(WordNet wordNet) {
        NERAnnotation.annotate(wordNet);
    }

    /**
     * 最短路径分词
     *
     * @param wordNet
     * @return
     */
    private List<Word> getShortestPath(WordNet wordNet) {
        Graph graph = graphFactory.generateGraph(wordNet);
        BaseVertex sourceVetex = graph.getSourceVertex();
        BaseVertex targetVetex = graph.getSinkVertex();
        // System.out.println("==== graph="+graph);
        YenTopKShortestPathsAlg alg = new YenTopKShortestPathsAlg(graph);
        Path path = alg.getShortestPath(sourceVetex, targetVetex);
        // System.out.println("====== Graph resultPaths:"+resultPaths);
        List<Word> wordList = new LinkedList<Word>();
        if (path != null) {
            // System.out.println("path"+i+":"+path);
            List<BaseVertex> vertexList = path.getVertexList();
            for (int j = 0; j < vertexList.size(); j++) {
                Word word = (Word) vertexList.get(j).getObject();
                word.confirmPOS((POS) vertexList.get(j).getRefObject());
                wordList.add(word);
            }
        }
        return wordList;
    }



}
