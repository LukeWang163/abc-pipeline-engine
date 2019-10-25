package base.operators.operator.nlp.similar.assist;

import java.util.*;

public class OneHot {
    /**
     * 生成两个字符串以字为单位的one-hot向量
     * @param s1:第一个字符串；
     * @param s2:第一个字符串；
     * @return int[][]：两个字符串的字向量。
     */
    public static double[][] stringCharacterOneHot(String s1, String s2){
        //生成两个字符串中单个字的词典
        Set<String> vocab_set = new HashSet<>();
        for (int i = 0; i < s1.length(); i++){
            vocab_set.add(s1.substring(i, i + 1));
        }
        for (int j = 0; j < s2.length(); j++){
            vocab_set.add(s2.substring(j, j + 1));
        }
        List<String> vocab_list = new ArrayList<String>(vocab_set);
        //生成两个字符串的one-hot向量
        double[][] vector = new double[2][vocab_list.size()];
        for (int i = 0; i < s1.length(); i++){
            vector[0][vocab_list.indexOf(s1.substring(i, i + 1))] = (double)1;
        }
        for (int j = 0; j < s2.length(); j++){
            vector[1][vocab_list.indexOf(s2.substring(j, j + 1))] = (double)1;
        }
        return vector;
    }
    /**
     * 生成两篇文章以词为单位的one-hot向量
     * @param s1:第一篇，以空格隔开；
     * @param s2:第一篇，以空格隔开；
     * @return int[][]：两篇文章的向量。
     */
    public static double[][] docWordOneHot(String s1, String s2){
        //生成两篇文章的单词词典
        String[] s1_split = s1.split(" ");
        String[] s2_split = s2.split(" ");
        Set<String> vocab_set = new HashSet<String>();
        Arrays.asList(s1_split).stream().forEach(item -> {vocab_set.add(item);});
        Arrays.asList(s2_split).stream().forEach(item -> {vocab_set.add(item);});
        List<String> vocab_list = new ArrayList<String>(vocab_set);
        //生成两个文章的one-hot向量
        double[][] vector = new double[2][vocab_list.size()];
        for (int i = 0; i < s1_split.length; i++){
            vector[0][vocab_list.indexOf(s1_split[i])] = (double)1;
        }
        for (int j = 0; j < s2_split.length; j++){
            vector[1][vocab_list.indexOf(s2_split[j])] = (double)1;
        }
        return vector;
    }
}
