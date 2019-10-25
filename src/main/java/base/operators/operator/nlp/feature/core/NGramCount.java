package base.operators.operator.nlp.feature.core;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;

import java.util.*;

/**
 * 对于给定文档列，产生n-gram文本信息。
 */
public class NGramCount {

    public ExampleSet examples;
    public Attribute docAttribute;
    public Attribute weightAttribute;
    public Set<String> vocabulary;
    public int ngramSize;
    /**
     * 生成文档的ngram-count信息
     * @param examples:待分析的文档，分好词的文档，以空格隔开；
     * @param weightAttribute：文档权重；
     * @param vocabulary：词袋；
     * @param ngramSize:ngram长度。
     *
     */
    public NGramCount(ExampleSet examples, Attribute docAttribute, Attribute weightAttribute, Set<String> vocabulary, int ngramSize){
        this.examples = examples;
        this.docAttribute = docAttribute;
        this.weightAttribute = weightAttribute;
        this.vocabulary = vocabulary;
        this.ngramSize = ngramSize;
    }

    /**
     * 在句子开头结尾处添加相应标识
     * @param text:文档内容；
     * @param identity1:句子开头标识；
     * @param identity2:句子结尾标识。
     * @return List<String> text:修改后文档内容；
     */

    public static String addIdentity(String text, String identity1, String identity2){
            text = identity1 + " " + text + " " + identity2;
        return text;
    }
    /**
     * 生成文档的ngram-count信息
     * @return Map<Integer, Map<String, Integer>>:第一个键为ngram值；
     *                                       第二个键为ngram文本；
     *                                       值为count。
     */
    public Map<Integer, Map<String, Double>> getNgramCount(){
        //默认文本开始结束标志符是必须的。
        if(!vocabulary.isEmpty()){
            vocabulary.add("<begin>");
            vocabulary.add("<end>");
        }
        //最终的结果集
        Map<Integer, Map<String, Double>> result = new HashMap<Integer, Map<String, Double>>();
        for (int i = 0; i < examples.size(); i++) {
            Example example = examples.getExample(i);
            String text = example.getValueAsString(docAttribute);
            double weight = 1.0;
            if(weightAttribute!=null){
                weight = example.getValue(weightAttribute);
            }
            text = addIdentity(text, "<begin>", "<end>");
            List<String> text_split = Arrays.asList(text.split("\\s+"));
            for (int w = 0; w < text_split.size(); w++){
                //根据文本长度和ngram的值确定遍历的右边界，针对(当前值下标+ngram)的大小进行分别赋值遍历的右边界。
                Integer rightBoundary = w + ngramSize > text_split.size()? text_split.size() - w : ngramSize;
                for (int u = 1; u <= rightBoundary; u++){
                    List<String> sublist = text_split.subList(w, w + u);
                    //将不在词典中的单词替换为<unk>标识。
                    if(!vocabulary.isEmpty()){
                        for (int d = 0; d < sublist.size(); d++){
                            if (!vocabulary.contains(sublist.get(d))){
                                sublist.set(d, "<unk>");
                            }
                        }
                    }
                    //根据该ngram的值，去分别处理，如果结果集中存在键ngram，则叠加，否则赋值新值。
                    if (result.keySet().contains(u)){
                        double frequency = result.get(u).keySet().contains(String.join(" ", sublist)) ? result.get(u).get(String.join(" ", sublist)) + weight : weight;
                        result.get(u).put(String.join(" ", sublist), frequency);
                    }else{
                        Map<String, Double> word_fre = new HashMap<String, Double>();
                        word_fre.put(String.join(" ", sublist), weight);
                        result.put(u, word_fre);
                    }
                }
            }
        }
        return result;
    }
    /**
     * 进行两个ngram结果表的合并
     *
     */
    public static Map<Integer, Map<String, Double>> mergeNgramResult(Map<Integer, Map<String, Double>> subset1, Map<String, Double> subset2){
        for (Map.Entry<String, Double> entry: subset2.entrySet()){
            Double frequency = subset1.get(entry.getKey().split(" ").length).containsKey(entry.getKey()) ? subset1.get(entry.getKey().split(" ").length).get(entry.getKey()) + entry.getValue() : entry.getValue();
            subset1.get(entry.getKey().split(" ").length).put(entry.getKey(), frequency);
        }
        return subset1;
    }

//	public static void main( String[] args ){
//        ArrayList<String> test = new ArrayList<String>();
//        test.add("我 很 开心 啊 开心 是 啊 我");
//        test.add("我 开心");
//        Set<String> vocabulary = new HashSet<String>();
//        vocabulary.add("我");
//        vocabulary.add("开心");
//        vocabulary.add("啊");
//        ArrayList<Double> weight = new ArrayList<Double>();
//        weight.add(0.1);
//        weight.add(0.2);
//        NGramCount ngramCount = new NGramCount();
//        Map<Integer, Map<String, Double>> subset1 = ngramCount.getNgramCount(test, weight, vocabulary, 3);
//        System.out.println(subset1);
//
//        Map<String, Double> subset2 = new HashMap<String, Double>();
//        subset2.put("哈哈哈", (double) 30);
//        subset2.put("哈哈哈 哈哈", (double) 20);
//
//        Map<Integer, Map<String, Double>> result = ngramCount.mergeNgramResult(subset1, subset2);
//        System.out.println(result);
//    }
}
