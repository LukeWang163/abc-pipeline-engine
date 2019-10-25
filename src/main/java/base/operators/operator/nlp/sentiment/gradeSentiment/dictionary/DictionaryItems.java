package base.operators.operator.nlp.sentiment.gradeSentiment.dictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DictionaryItems {

    public List<String> notDic;
    public Map<String, Double> sentimentDic;
    public Map<String, Double> degreeDic;
    public List<String> sentimentVocab;
    public List<String> degreeVocab;
    public List<String> symbol;

    public DictionaryItems(){
        this.notDic = DefaultDictionary.dalian_not;
        this.sentimentDic = DefaultDictionary.dalian_senti;
        this.degreeDic = DefaultDictionary.dalian_degree;
        this.sentimentVocab = new ArrayList<String>(DefaultDictionary.dalian_senti.keySet());
        this.degreeVocab = new ArrayList<>(DefaultDictionary.dalian_degree.keySet());
        this.symbol = DefaultDictionary.symbol;
    }

    public DictionaryItems(Map<String, Double> sentimentDic, Map<String, Double> degreeDic, List<String> notDic){
        this.notDic = notDic;
        this.sentimentDic = sentimentDic;
        this.degreeDic = degreeDic;
        this.sentimentVocab = new ArrayList<String>(sentimentDic.keySet());
        this.degreeVocab = new ArrayList<>(degreeDic.keySet());
        this.symbol = DefaultDictionary.symbol;
    }

}
