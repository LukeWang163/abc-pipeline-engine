package base.operators.operator.nlp.similar.assist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DamerauLevenstein {

    public String separator = "";//Hash内容的分隔符，""代表以字符为单位。
    public List<String> s1_list = new ArrayList<>();
    public List<String> s2_list = new ArrayList<>();

    public DamerauLevenstein(String s1, String s2, String separator){
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
    public double damerauLevensteinDistance(){
        double d[][];
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        String s1_i; // ith String of s
        String s2_j; // jth String of t
        int cost; // cost

        n = this.s1_list.size();
        m = this.s2_list.size();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = new double[n+1][m+1];

        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }

        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        for(i = 1; i <= n; i++) {
            s1_i = this.s1_list.get(i - 1);
            for(j = 1; j <= m; j++) {
                s2_j = this.s2_list.get(j - 1);

                if(s1_i.equals(s2_j)){
                    cost = 0;
                }else{
                    cost = 1;
                }
                d[i][j] = Util.getMin(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1] + cost);

                if(i > 1 && j > 1 && s1_i.equals(this.s2_list.get(j - 2)) && this.s1_list.get(i - 2).equals(s2_j)){
                    d[i][j] = Util.getMin(d[i][j], d[i-2][j-2] + cost);
                }
            }
        }
        return d[n][m];
    }

    public double damerauLevenshteinSim() {
        try {
            double ld = (double)damerauLevensteinDistance();
            return (1-ld/(double)Math.max(this.s1_list.size(), this.s2_list.size()));
        } catch (Exception e) {
            return 0;
        }
    }
//    public static double damerauLevensteinDistance(String s1, String s2){
//        double d[][];
//        int n; // length of s
//        int m; // length of t
//        int i; // iterates through s
//        int j; // iterates through t
//        char s1_i; // ith character of s
//        char s2_j; // jth character of t
//        int cost; // cost
//
//        n = s1.length ();
//        m = s2.length ();
//        if (n == 0) {
//            return m;
//        }
//        if (m == 0) {
//            return n;
//        }
//        d = new double[n+1][m+1];
//
//        for (i = 0; i <= n; i++) {
//            d[i][0] = i;
//        }
//
//        for (j = 0; j <= m; j++) {
//            d[0][j] = j;
//        }
//
//        for(i = 1; i <= n; i++) {
//            s1_i = s1.charAt (i - 1);
//            for(j = 1; j <= m; j++) {
//                s2_j = s2.charAt (j - 1);
//
//                if(s1_i == s2_j){
//                    cost = 0;
//                }else{
//                    cost = 1;
//                }
//                d[i][j] = Util.getMin(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1] + cost);
//
//                if(i > 1 && j > 1 && s1_i == s2_j-1 && s1_i-1 == s2_j){
//                    d[i][j] = Util.getMin(d[i][j], d[i-2][j-2] + cost);
//                }
//            }
//        }
//        return d[n][m];
//    }
//
//    public static double damerauLevenshteinSim(String s1, String s2) {
//        try {
//            double ld = (double)damerauLevensteinDistance(s1, s2);
//            return (1-ld/(double)Math.max(s1.length(), s2.length()));
//        } catch (Exception e) {
//            return 0;
//        }
//    }
//    public static void main(String[] args){
//        String s1 = "wo si";
//        String s2 = "wo si ha";
//        DamerauLevenstein dls = new DamerauLevenstein( s1, s2," ");
//        System.out.println(dls.damerauLevensteinDistance());
//        System.out.println(dls.damerauLevenshteinSim());
//    }
}
