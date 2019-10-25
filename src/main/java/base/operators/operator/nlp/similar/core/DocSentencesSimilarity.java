package base.operators.operator.nlp.similar.core;

import base.operators.operator.nlp.similar.assist.Util;

import java.util.*;

public class DocSentencesSimilarity {

    public static List<String> idList = new ArrayList<>();
    public static List<String> inputList = new ArrayList<>();
    public static List<String> mapList = new ArrayList<>();
    public static List<Double> simList = new ArrayList<>();
    public DocSentencesSimilarity(List<String> idCol, List<String> inputCol, String method, int k, double lambda){
        sentencesSimilarity(idCol, inputCol, method, k, lambda);
    }
    /**
     * 计算两列字符串的相似度最高的topN
     * @param idCol:文档id列
     * @param inputCol:输入表字符串；
     * @param method ：距离计算方法
     */
    public void sentencesSimilarity(List<String> idCol, List<String> inputCol, String method, int k, double lambda){
        List<String> unDuplicateId = new ArrayList<>(new HashSet<>(idCol));
        for(String id : unDuplicateId){
            Map<String[], Double> string_distance = new HashMap<>();
            List<String> inputCol_id = new ArrayList<>();
            for (int i = 0; i < idCol.size(); i++) {
                if(id.equals(idCol.get(i))){
                    inputCol_id.add(inputCol.get(i));
                }
            }

            for (int u = 0; u < inputCol_id.size(); u++) {
                for (int w = u + 1; w < inputCol_id.size(); w++) {
                    String[] pair = new String[2];
                    pair[0] = inputCol_id.get(u);
                    pair[1] = inputCol_id.get(w);
                    double distance = TextSimilarity.computeDistance(pair[0], pair[1], "",method,k, lambda);
                    string_distance.put(pair, distance);
                }
            }
            if(string_distance.size()==0){
                continue;
            }
            //排序
            Map<String[], Double> sort_list = Util.top(string_distance.size(), string_distance);
            //与该字符串最相似的前topN个结果
            for (Map.Entry<String[], Double> keyEntry : sort_list.entrySet()){
                idList.add(id);
                inputList.add(keyEntry.getKey()[0]);
                mapList.add(keyEntry.getKey()[1]);
                simList.add(keyEntry.getValue());
            }
        }
    }
//    public static void main(String[] args){
//        List<String> inputCol = new ArrayList<String>();
//        inputCol.add("今天很开心有没有");
//        inputCol.add("哈哈哈哈唉哈哈");
//        inputCol.add("今天开心");
//        inputCol.add("元旦护士VB发半句发布的");
//        inputCol.add("哈哈吃饭方法哈哈唉哈哈");
//        inputCol.add("发乎电话费是地方");
//        List<String> idCol = Arrays.asList("1", "1", "1","2","2","2");
//        DocSentencesSimilarity dss = new DocSentencesSimilarity(idCol, inputCol,"cosine", 1,0.9);
//        System.out.println(dss.idList);
//        System.out.println(dss.inputList);
//        System.out.println(dss.mapList);
//        System.out.println(dss.simList);
//    }
}
