package base.operators.operator.nlp.sentiment.gradeSentiment.entity;

import base.operators.operator.nlp.sentiment.gradeSentiment.dictionary.DictionaryItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchEntities {
    /*
    寻找给定某一类型的词典中的实体
    @param text:待标记文本
    @param vocab:词典
    @param entityType:实体类型
    @return List<Entity>:实体列表
    */
    public static List<Entity> searchOneTypeEntities(String text, List<String> vocab, String entityType){
        List<Entity> entityList = new ArrayList<>();
        for (int i = 0; i < vocab.size(); i++) {
            int index = text.indexOf(vocab.get(i));
            while (index != -1){
                Entity entity = new Entity(vocab.get(i), index, entityType);
                entityList.add(entity);
                index = text.indexOf(vocab.get(i), index + 1);
            }
        }
        return entityList;
    }
    /*
    寻找所有类型的词典中的实体
    @param text:待标记文本
    @return List<Entity>:四种类型的实体列表
    */
    public static List<Entity> searchAllEntities(String text, DictionaryItems senDic){
        List<Entity> allEntityList = new ArrayList<>();

        List<Entity> symbol_entityList = searchOneTypeEntities(text, senDic.symbol, "symbol");
        allEntityList.addAll(symbol_entityList);
        List<Entity> degree_entityList = searchOneTypeEntities(text, senDic.degreeVocab, "degree");
        allEntityList.addAll(degree_entityList);
        List<Entity> not_entityList = searchOneTypeEntities(text, senDic.notDic, "not");
        allEntityList.addAll(not_entityList);
        List<Entity> senti_entityList = searchOneTypeEntities(text, senDic.sentimentVocab, "sentiment");
        allEntityList.addAll(senti_entityList);
        //将实体列表按照起始位置和实体长度进行排序
        EntityComparator comparator = new EntityComparator();
        Collections.sort(allEntityList, comparator);
        return allEntityList;
    }

    /*
    筛选实体
    @param text:待标记文本
    @return List<Entity>:实体列表
    */
    public static List<Entity> selectEntity(String text, DictionaryItems senDic){
        List<Entity> selectEntityList = new ArrayList<>();
        List<Entity> allEntityList = searchAllEntities(text, senDic);
        //选择实体词最长的进行最大匹配
        int finalEntityNumber = 0; //selectEntityList的最终个数
        int lastEntityEndIndex = 0; //上一实体的结束索引
        int i = 0;
        //迭代检索实体词，如果后面的实体词和当前实体词起始索引一致，则找最长的实体，作为当前索引的实体，下一个词的起始索引要大于最长实体的结束索引
        while (i < allEntityList.size() - 1){
            int indexNew = allEntityList.get(i).startIndex;
            //当前实体索引小于上一实体的结束索引，直接略过，判断下一实体
            if (indexNew < lastEntityEndIndex){
                i = i + 1;
                continue;
            }
            int maxIndex = i;
            //训练遍历后面的实体，找到同索引的最长实体，记录实体结束索引和下一个实体的序号
            for (int j = i + 1; j < allEntityList.size(); j++) {
                if (allEntityList.get(j).startIndex == indexNew){
                    maxIndex = j;
                    i = maxIndex + 1;
                }else{
                    lastEntityEndIndex = allEntityList.get(maxIndex).endIndex;
                    i = maxIndex + 1;
                    break;
                }
            }
            selectEntityList.add(allEntityList.get(maxIndex));
            finalEntityNumber += 1;
        }
        //特殊处理allEntityList长度为1的情况
        if (allEntityList.size() == 1) {
            selectEntityList.addAll(allEntityList);
        }else{
            selectEntityList = selectEntityList.subList(0, finalEntityNumber);
        }
        //特殊处理最后一个实体的问题，处理最后一个实体被扔掉的情况
        if ((allEntityList.size() - 1 > 0) && (!selectEntityList.contains(allEntityList.get(allEntityList.size()-1))) && (selectEntityList.get(finalEntityNumber-1).endIndex <= allEntityList.get(allEntityList.size()-1).startIndex)){
            selectEntityList.add(allEntityList.get(allEntityList.size() - 1));
        }
        return selectEntityList;
    }
}
