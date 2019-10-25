package base.operators.operator.nlp.feature.core;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.Statistics;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.preprocessing.filter.ExampleFilter;

import java.util.*;

/**
 * 对于给定文档列，计算文档单词的TF-IDF；
 * 该组件的输入，默认为词频统计的输出Map<Integer, Map<String, Integer>> wordFrequencyResult；
 *                               docid（文档ID）      word（单词） word_count（当前单词在该文档中的出现次数）
 * TF = 单词在该本文中出现的频数/该文本的单词总数
 * IDF = log(文档总数/(包含该单词的文档总数+1))
 * TF-IDF = TF * IDF
 *
 * @return
 * count(word在当前doc中频数)
 * total_word_count（当前doc中总word数）
 * doc_count（包含当前word的总doc数）
 * total_doc_count（全部doc数）
 * tf
 * idf
 * tfidf
 */
public class TFIDF {

    public ExampleSet exampleSet;
    public Attribute wordAttribute;
    public Attribute frequencyAttribute;
    public Attribute docIdAttribute;

    public static Map<String, Integer> doc_count = new HashMap<>();//键为word,值为该word出现的文档数
    public static Map<String, Integer> total_word_count = new HashMap<>();//键为docid,值为该doc的所有单词个数
    public Integer total_doc_count;//总的文档数


    public TFIDF(ExampleSet exampleSet, Attribute docIdAttribute, Attribute wordAttribute, Attribute frequencyAttribute){
        this.exampleSet = exampleSet;
        this.docIdAttribute = docIdAttribute;
        this.wordAttribute = wordAttribute;
        this.frequencyAttribute = frequencyAttribute;
    }

    public void computeTFIDF(){
        Set<String> id_set = new HashSet<>();
        for (int i = 0; i < exampleSet.size(); i++) {
            id_set.add(exampleSet.getExample(i).getValueAsString(docIdAttribute));
        }
        //按照id进行exampleset的筛选
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        OperatorDescription description = null;
        try {
            description = new OperatorDescription(loader, null, null, "com.rapidminer.operator.preprocessing.filter.ExampleFilter", null, null, null, null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Iterator<String> it = id_set.iterator();
        while (it.hasNext()) {
            String idValue = it.next();
            ExampleFilter examplesFilter = new ExampleFilter(description);
            examplesFilter.setParameter("condition_class", "attribute_value_filter");
            examplesFilter.setParameter("parameter_string", docIdAttribute.getName()+"="+idValue);
            ExampleSet filterExampleSet = null;
            try {
                filterExampleSet = examplesFilter.apply(exampleSet);
            } catch (OperatorException e) {
                e.printStackTrace();
            }
            filterExampleSet.recalculateAllAttributeStatistics();
            for (int j = 0; j < filterExampleSet.size(); j++) {
                Example example = filterExampleSet.getExample(j);
                String word = example.getValueAsString(wordAttribute);
                doc_count.put(word, doc_count.keySet().contains(word)?doc_count.get(word)+1:1);
            }
            total_word_count.put(idValue, (int)filterExampleSet.getStatistics(frequencyAttribute, Statistics.SUM));
        }
        total_doc_count = total_word_count.size();

    }
//	public static void main( String[] args ){
//    	Map<Integer, String> testMap = new HashMap<Integer, String>();
//        testMap.put(1, "我 很 开心 呀 我 开心");
//        testMap.put(2, "我 有点 忧伤 呀");
//        testMap.put(3, "哈哈");
//        DocWordStat test = new DocWordStat();
//        Map<Integer, Map<String, Integer>> wordFrequencyResult = test.getWordFrequencyResult(testMap);
//        System.out.println(wordFrequencyResult);
//        TFIDF tfidf = new TFIDF();
//        tfidf.getTFIDF(wordFrequencyResult);
//        System.out.println(tfidf.tfidf);
//	}

}



