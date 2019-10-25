package base.operators.operator.nlp.sentiment.gradeSentiment;

import base.operators.operator.nlp.sentiment.gradeSentiment.dictionary.DictionaryItems;
import base.operators.operator.nlp.sentiment.gradeSentiment.entity.Entity;
import base.operators.operator.nlp.sentiment.gradeSentiment.entity.SearchEntities;

import java.util.*;

public class SentimentImpl {

    public DictionaryItems dic;

    public SentimentImpl(DictionaryItems dic){
        this.dic = dic;
    }
    /*
    *
   根据筛选出的实体计算情感得分
   *@param text:输入文本
   *@return double:情感得分
   *
   */
    public double computeScoreChinese(String text){
        List<Entity> entityList = SearchEntities.selectEntity(text, dic);
        double score = 0;
        int senti_number = 0;
        for (int j = 0; j < entityList.size(); j++) {
            int index = 0;
            //判断情感词的位置
            if (!"sentiment".equals(entityList.get(j).entityType)){
                continue;
            }else {
                senti_number += 1;
                //寻找离该情感词最近的情感词或者标点符号的位置
                for (int w = j-1; w >= 0 ; w--) {
                    if (("sentiment".equals(entityList.get(w).entityType)) || ("symbol".equals(entityList.get(w).entityType))) {
                        index = w;
                        break;
                    }
                }
            }
            //计算该情感词的情感值
            // 权重初始化为该情感词的分值,往前找否定词和程度副词
            double initial_score = dic.sentimentDic.get(entityList.get(j).entityName);
            List<String> example_pattern = new ArrayList<>(Arrays.asList("sentiment"));
            int index_ = 0;
            index_ = index == 0 ? 0 : index + 1;
            for (int u = index_; u < j; u++) {
                if (example_pattern.size() >= 3){
                    break;
                }
                if ("not".equals(entityList.get(u).entityType)){
                    example_pattern.add(0, entityList.get(u).entityType);
                    initial_score *= (-1);
                }else if("degree".equals(entityList.get(u).entityType)){
                    //去除程度副词*程度副词*情感词的情况
                    if (example_pattern.equals(new ArrayList<>(Arrays.asList("degree", "sentiment")))){
                        initial_score *= (double) 1;
                    }else{
                        example_pattern.add(0, entityList.get(u).entityType);
                        initial_score *= dic.degreeDic.get(entityList.get(u).entityName);
                    }
                }
            }
            if (example_pattern.equals(new ArrayList<>(Arrays.asList("not", "degree", "sentiment")))){
                initial_score *= 0.4;
            }

            score += initial_score;
        }

        score = senti_number == 0 ? 0 : score/senti_number;
        
        score = score / 18 * 100;

        return score;
    }
    //遍历情感词程度词否定词集合，计算情感得分
    public double computeWeightEnglish(Map<Integer,Double> sen_word, Map<Integer,Integer> not_word, Map<Integer,Double> degree_word) {
        float score = 0;
        //情感词的位置下标集合
        List<Integer> sentiment_index_list = new ArrayList<Integer>(sen_word.keySet());
        //遍历分词结果(遍历分词结果是为了定位两个情感词之间的程度副词和否定词)
        for(int i=0;i < sentiment_index_list.size();i++) {
            int w = 1;
            if(i==0) {
                for(int j=0;j<sentiment_index_list.get(i);j++) {
                    //更新权重，如果有否定词，取反
                    if(not_word.keySet().contains(j)) {
                        w*=-1;
                    }else if(degree_word.keySet().contains(j)) {
                        //更新权重，如果有程度副词，分值乘以程度副词的程度分值
                        w *= degree_word.get(j);
                    }
                }
                score+=w*sen_word.get(sentiment_index_list.get(i));
            }else {
                for(int j=sentiment_index_list.get(i-1);j<sentiment_index_list.get(i);j++) {
                    //更新权重，如果有否定词，取反
                    if(not_word.keySet().contains(j)) {
                        w*=-1;
                    }else if(degree_word.keySet().contains(j)) {
                        //更新权重，如果有程度副词，分值乘以程度副词的程度分值
                        w *= degree_word.get(j);
                    }
                }
                score+=w*sen_word.get(sentiment_index_list.get(i));
            }
        }
        return score;
    }
    //计算给定的分词完毕的句子计算情感得分
    public double computeScoreEnglish(String text) {
        List<String> input = Arrays.asList(text.split(" "));
        double score = 0.0;
        //将句子的输入变为字典存储
        Map<String,Integer> inputDic = new HashMap<String,Integer>();
        for(int i = 0 ; i < input.size() ; i++) {
            inputDic.put(input.get(i),i);
        }
        //得到输入句子中的情感词、否定词、程度副词
        Map<Integer,Double> sen_word = new HashMap<Integer,Double>();
        Map<Integer,Integer> not_word = new HashMap<Integer,Integer>();
        Map<Integer,Double> degree_word = new HashMap<Integer,Double>();

        for (Map.Entry<String,Integer> entry : inputDic.entrySet()) {
            if ((dic.sentimentVocab.contains(entry.getKey()))&(!dic.notDic.contains(entry.getKey()))&(!dic.degreeVocab.contains(entry.getKey()))){
                sen_word.put(inputDic.get(entry.getKey()), dic.sentimentDic.get(entry.getKey()));
            }else if((dic.notDic.contains(entry.getKey()))&(!dic.degreeVocab.contains(entry.getKey()))){
                not_word.put(inputDic.get(entry.getKey()),-1);
            }else if(dic.degreeVocab.contains(entry.getKey())){
                degree_word.put(inputDic.get(entry.getKey()), dic.degreeDic.get(entry.getKey()));
            }
        }
        score = computeWeightEnglish(sen_word, not_word, degree_word);

        return score;
    }

//    public static void main(String[] args){
//        String text = "准四星已经正式挂牌为四星了,房间很大很干净,服务也不错,酒店就在火车站旁边,交通很便利,而且大商等大商场立着也不远, 购物也挺方便,不过也正因为在火车站旁边,酒店门口显得比较乱";
//        DictionaryItems dic = new DictionaryItems();
//        SentimentImpl impl = new SentimentImpl(dic);
//        System.out.println(impl.computeScore(text));
//
//        List<Entity> list = SearchEntities.selectEntity(text, dic);
//        for (int i = 0; i < list.size(); i++) {
//            System.out.println(list.get(i).entityName+" "+list.get(i).startIndex+" "+list.get(i).endIndex+" "+list.get(i).entityType);
//        }
//    }
}
