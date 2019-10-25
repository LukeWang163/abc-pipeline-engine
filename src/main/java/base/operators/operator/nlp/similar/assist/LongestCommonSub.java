package base.operators.operator.nlp.similar.assist;

public class LongestCommonSub {
    /**
     * 计算两个字符串的最长公共子序列
     * @param s1
     * @param s2
     * @return 最长公共子序列长度
     */
    public static double longestCommonSubsequenceDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int c[][] = new int[len1+1][len2+1];
        for (int i = 0; i <= len1; i++) {
            for( int j = 0; j <= len2; j++) {
                if(i == 0 || j == 0) {
                    c[i][j] = 0;
                } else if (s1.charAt(i-1) == s2.charAt(j-1)) {
                    c[i][j] = c[i-1][j-1] + 1;
                } else {
                    c[i][j] = Math.max(c[i - 1][j], c[i][j - 1]);
                }
            }
        }
        return c[len1][len2];
    }
    /**
     * 计算两个字符串的最长公共子串
     * @param s1
     * @param s2
     * @return 最长公共子串长度
     */
    public static double longestCommonSubstringDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        if(s1.equals(s2)){
            return (double)len1;
        }
        int result = 0;     //记录最长公共子串长度
        int c[][] = new int[len1+1][len2+1];
        for (int i = 0; i <= len1; i++) {
            for( int j = 0; j <= len2; j++) {
                if(i == 0 || j == 0) {
                    c[i][j] = 0;
                } else if (s1.charAt(i-1) == s2.charAt(j-1)) {
                    c[i][j] = c[i-1][j-1] + 1;
                    result = Math.max(c[i][j], result);
                } else {
                    c[i][j] = 0;
                }
            }
        }
        return result;
    }
    /**
     * 计算两个字符串的最长公共子串相似度
     * @param s1
     * @param s2
     * @return 相似度
     */
    public static double longestCommonSubsequenceSim(String s1, String s2) {
        try {
            double lcs = longestCommonSubsequenceDistance(s1, s2);
            return lcs / (double)Math.max(s1.length(), s2.length());
        } catch (Exception e) {
            return 0;
        }
    }
    /**
     * 计算两个字符串的最长公共子串相似度
     * @param s1
     * @param s2
     * @return 相似度
     */
    public static double longestCommonSubstringSim(String s1, String s2) {
        try {
            double lcs = (double)longestCommonSubstringDistance(s1, s2);
            return lcs / (double)Math.max(s1.length(), s2.length());
        } catch (Exception e) {
            return 0;
        }
    }
}
