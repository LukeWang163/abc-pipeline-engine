package base.operators.operator.nlp.similar.assist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Levenshtein {

    public String separator = "";//Hash内容的分隔符，""代表以字符为单位。
    public List<String> s1_list = new ArrayList<>();
    public List<String> s2_list = new ArrayList<>();

    public Levenshtein(String s1, String s2, String separator){
        this.separator = separator;
        if("".equals(separator)){
            this.s1_list= Stream.iterate(0, i -> ++i).limit(s1.length())
                    .map(i -> "" + s1.charAt(i))
                    .collect(Collectors.toList());
            this.s2_list= Stream.iterate(0, i -> ++i).limit(s2.length())
                    .map(i -> "" + s2.charAt(i))
                    .collect(Collectors.toList());
        }else {
            this.s1_list= Arrays.asList(s1.split( separator));
            this.s2_list= Arrays.asList(s2.split( separator));
        }
    }
    /**
     * 计算两个字符串的编辑距离
     * @return 编辑距离
     */
    public double levenshteinDistance() {
        double distance[][];// 定义距离表
        int s1_len = this.s1_list.size();
        int s2_len = this.s2_list.size();

        if (s1_len == 0) {
            return s2_len;
        }
        if (s2_len == 0) {
            return s1_len;
        }
        distance = new double[s1_len + 1][s2_len + 1];
        // 二维数组第一行和第一列放置自然数
        for (int i = 0; i <= s1_len; i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= s2_len; j++) {
            distance[0][j] = j;
        }
        // 比较，若行列相同，则代价为0，否则代价为1；
        for (int i = 1; i <= s1_len; i++) {
            String s1_i = this.s1_list.get(i-1);
            // 逐一比较
            for (int j = 1; j <= s2_len; j++) {
                String s2_j = this.s2_list.get(j-1);
                // 若相等，则代价取0；直接取左上方值
                if (s1_i.equals(s2_j)) {
                    distance[i][j] = distance[i - 1][j - 1];
                } else {
                    // 否则代价取1，取左上角、左、上 最小值 + 代价（代价之和便是最终距离）
                    distance[i][j] = Util.getMin(distance[i - 1][j], distance[i][j - 1], distance[i - 1][j - 1]) + 1;
                }
            }
        }
        // 取二位数组最后一位便是两个字符串之间的距离
        return distance[s1_len][s2_len];
    }
    /**
     * 计算两个字符串的编辑距离相似度
     * @return 相似度
     */
    public double levenshteinSim() {
        try {
            double ld = (double)levenshteinDistance();
            return (1-ld/(double)Math.max(this.s1_list.size(), this.s2_list.size()));
        } catch (Exception e) {
            return 0;
        }
    }
//    /**
//     * 计算两个字符串的编辑距离
//     * @param s1
//     * @param s2
//     * @return 编辑距离
//     */
//    public static double levenshteinDistance(String s1, String s2) {
//        double distance[][];// 定义距离表
//        int s1_len = s1.length();
//        int s2_len = s2.length();
//
//        if (s1_len == 0) {
//            return s2_len;
//        }
//        if (s2_len == 0) {
//            return s1_len;
//        }
//        distance = new double[s1_len + 1][s2_len + 1];
//        // 二维数组第一行和第一列放置自然数
//        for (int i = 0; i <= s1_len; i++) {
//            distance[i][0] = i;
//        }
//        for (int j = 0; j <= s2_len; j++) {
//            distance[0][j] = j;
//        }
//        // 比较，若行列相同，则代价为0，否则代价为1；
//        for (int i = 1; i <= s1_len; i++) {
//            char s1_i = s1.charAt(i - 1);
//            // 逐一比较
//            for (int j = 1; j <= s2_len; j++) {
//                char s2_j = s2.charAt(j - 1);
//                // 若相等，则代价取0；直接取左上方值
//                if (s1_i == s2_j) {
//                    distance[i][j] = distance[i - 1][j - 1];
//                } else {
//                    // 否则代价取1，取左上角、左、上 最小值 + 代价（代价之和便是最终距离）
//                    distance[i][j] = Util.getMin(distance[i - 1][j], distance[i][j - 1], distance[i - 1][j - 1]) + 1;
//                }
//            }
//        }
//        // 取二位数组最后一位便是两个字符串之间的距离
//        return distance[s1_len][s2_len];
//    }
//    /**
//     * 计算两个字符串的编辑距离相似度
//     * @param s1
//     * @param s2
//     * @return 相似度
//     */
//    public static double levenshteinSim(String s1, String s2) {
//        try {
//            double ld = (double)levenshteinDistance(s1, s2);
//            return (1-ld/(double)Math.max(s1.length(), s2.length()));
//        } catch (Exception e) {
//            return 0;
//        }
//    }
//    public static void main(String[] args){
//        String s1 = "wo si";
//        String s2 = "ws o";
//        Levenshtein ls = new Levenshtein(s1,s2," ");
//        System.out.println(ls.levenshteinDistance());
//        System.out.println(ls.levenshteinSim());
//    }
}
