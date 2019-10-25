package base.operators.operator.nlp.feature.core.assist;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CorpusVocabulary {
//    /*
//    *多篇文档，每篇文档是空格隔开的字符串
//    */
//    public List<String> text = new ArrayList<>();
    public ExampleSet exampleSet;
    public Attribute docAttribute;
    public Attribute docIdAttribute;

    /*
     *ngram大小
     */
    public int ngramSize;
    /*
     *最小词频
     */
    public int minFrequency;
    /*
     *最大文档频率
     */
    public Double maxNgramDocRatio;
    /*
     *语料词频的集合
     */
    public Map<String, Integer> frequency = new HashMap<>();
    /*
     *文档频率
     */
    public Map<String, Integer> df = new HashMap<>();
    /*
     *逆向文档频率
     */
    public Map<String, Double> idf = new HashMap<>();
    /*
     *tf
     */
    public Map<String, Map<String, Long>> tf = new HashMap<>();
    /*
     *tfidf
     */
    public Map<String, Map<String, Double>> tfidf = new HashMap<>();
    /*
     *文档总数
     */
    public int totalNumDocs  = 0;

    public CorpusVocabulary(){
    }

    public CorpusVocabulary(ExampleSet exampleSet,  Attribute docIdAttribute, Attribute docAttribute, int ngramSize, int minFrequency, Double maxNgramDocRatio){
        this.exampleSet = exampleSet;
        this.docIdAttribute = docIdAttribute;
        this.docAttribute = docAttribute;
        this.ngramSize = ngramSize;
        this.minFrequency = minFrequency;
        this.maxNgramDocRatio = maxNgramDocRatio;
        this.totalNumDocs = exampleSet.size();
        getCorpusVocabulary();
    }

    public void getCorpusVocabulary(){
        //临时tf存放
        Map<String, Map<String, Long>> tfTemp = new HashMap<>();

        //所有文档以空格切分，按照ngram的大小生成窗口信息
        for (int i = 0; i < this.exampleSet.size(); i++){
            Example example = this.exampleSet.getExample(i);
            NgramText perNgram = new NgramText(Arrays.asList(example.getValueAsString(docAttribute).split("\\s+")), this.ngramSize);
            for (int j = 0; j < perNgram.ngramText.size(); j++){
                String word = perNgram.ngramText.get(j);
                frequency.put(word, frequency.containsKey(word)? frequency.get(word) + 1 : 1);
            }

            Map<String, Long> counted = perNgram.ngramText.stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            tfTemp.put(example.getValueAsString(docIdAttribute), counted);
            for (String word: counted.keySet()){
                df.put(word, df.containsKey(word)?df.get(word)+1:1);
            }
        }

        //筛选符合条件的单词
        Iterator<String> freIter = frequency.keySet().iterator();
        while(freIter.hasNext()){
            String key = freIter.next();
            if ((frequency.get(key) < this.minFrequency) || (df.get(key)*1.0/this.totalNumDocs > this.maxNgramDocRatio)) {
                freIter.remove();
            }
        }
        Iterator<String> dfIter = df.keySet().iterator();
        while(dfIter.hasNext()){
            String key = dfIter.next();
            if (!frequency.keySet().contains(key)) {
                dfIter.remove();
            }else{
                //将符合条件的单词的idf放入
                idf.put(key, Math.log10(1.0/df.get(key)));
            }
        }

        //按照同样条件筛选tfTemp
        for (String docId : tfTemp.keySet()) {
            Map<String, Long> tfTempValue = tfTemp.get(docId);
            Map<String, Double> tfidfValue = new HashMap<>();
            Iterator<String> tfIter = tfTempValue.keySet().iterator();
            while(tfIter.hasNext()) {
                String word = tfIter.next();
                if (!frequency.keySet().contains(word)) {
                    tfIter.remove();
                }else{
                    tfidfValue.put(word, tfTempValue.get(word)*idf.get(word));
                }
            }
            tf.put(docId, tfTempValue);
            tfidf.put(docId, tfidfValue);
        }
    }
}
