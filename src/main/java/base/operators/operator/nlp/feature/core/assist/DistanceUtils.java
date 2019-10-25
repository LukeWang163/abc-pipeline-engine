package base.operators.operator.nlp.feature.core.assist;

public class DistanceUtils {
	/** 
	 * 计算两个向量的欧式距离，两个向量可以为任意维度，但必须保持维度相同，表示两点 
	 * @param vector1 
	 * @param vector2 
	 * @return 两点间距离 
	 */
	public static double euclideanDistance(double[] vector1, double[] vector2){
		double distance = 0;
		if (vector1.length == vector2.length) { 
			for (int i = 0; i < vector1.length; i++) { 
				double temp = Math.pow((vector1[i] - vector2[i]), 2); 
				distance += temp; 
			}
			distance = Math.sqrt(distance);
		}
		
		return distance;
	}
	/** 
	 * 计算两个向量的余弦距离，两个向量可以为任意维度，但必须保持维度相同，表示两点 
	 * @param vector1 
	 * @param vector2 
	 * @return 两点间距离 
	 */
	public static double cosineDistance(double[] vector1, double[] vector2){
		double distance = 0;
		double denominator_1 = 0;
		double denominator_2 = 0;
		double numerator = 0;
		if (vector1.length == vector2.length) { 
			for (int i = 0; i < vector1.length; i++) { 
				denominator_1 += Math.pow((vector1[i]), 2); 
				denominator_2 += Math.pow((vector2[i]), 2); 
				numerator += vector1[i] * vector2[i]; 
			}
			distance = numerator / Math.sqrt(denominator_1) / Math.sqrt(denominator_2);
		}
		
		return distance;
	}
	/** 
	 * 计算两个向量的曼哈顿距离，两个向量可以为任意维度，但必须保持维度相同，表示两点 
	 * @param vector1 
	 * @param vector2 
	 * @return 两点间距离 
	 */
	public static double manhattanDistance(double[] vector1, double[] vector2){
		double distance = 0;
		if (vector1.length == vector2.length) { 
			for (int i = 0; i < vector1.length; i++) { 
				distance += Math.abs(vector1[i] - vector2[i]); 
			}
		}
		return distance;
	}
	
	/**
	 * 计算两个字符串的编辑距离
	 * @param s1
	 * @param s2
	 * @return 编辑距离
	 */
	public static double levenshteinDistance(String s1, String s2) {
		double distance[][];// 定义距离表
		int s1_len = s1.length();
		int s2_len = s2.length();
 
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
			char s1_i = s1.charAt(i - 1);
			// 逐一比较
			for (int j = 1; j <= s2_len; j++) {
				char s2_j = s2.charAt(j - 1);
				// 若相等，则代价取0；直接取左上方值
				if (s1_i == s2_j) {
					distance[i][j] = distance[i - 1][j - 1];
				} else {
					// 否则代价取1，取左上角、左、上 最小值 + 代价（代价之和便是最终距离）
					distance[i][j] = getMin(distance[i - 1][j], distance[i][j - 1], distance[i - 1][j - 1]) + 1;
				}
			}
		}
		// 取二位数组最后一位便是两个字符串之间的距离
		return distance[s1_len][s2_len];
	}
	/**
	 * 计算两个字符串的编辑距离相似度
	 * @param s1
	 * @param s2
	 * @return 相似度
	 */
	public static double levenshteinSim(String s1, String s2) {
		try {
			double ld = (double)levenshteinDistance(s1, s2);
			return (1-ld/(double)Math.max(s1.length(), s2.length()));
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * 求三个数中的最小值
	 * @param a
	 * @param b
	 * @param c
	 * @return 最小值
	 */
	private static double getMin(double a, double b, double c) {
		double min = a;
		if (b < min) {
			min = b;
		}
		if (c < min) {
			min = c;
		}
		return min;
	}
	/**
	 * 计算两个字符串的最长公共子序列
	 * @param s1
	 * @param s2
	 * @return 最长公共子序列长度
	 */
	public static double longestCommonSubsequence(String s1, String s2) {
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
	public static double longestCommonSubstring(String s1, String s2) {
	    int len1 = s1.length();
	    int len2 = s2.length();
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
	public static double longestCommonSubstringSim(String s1, String s2) {
		try {
			double lcs = (double)longestCommonSubstring(s1, s2);
			return lcs / (double)Math.max(s1.length(), s2.length());
		} catch (Exception e) {
			return 0;
		}
	}
	/**
	 * 计算两个字符串的汉明距离
	 * @author 
	 * @param s1 
	 * @param s2
	 * @return 汉明距离
	 */
	public double simhashHammingDistance(String s1, String s2) {
		
		int distance;
		if (s1.length() != s2.length()) {
			distance = -1;
		} else {
			distance = 0;
			for (int i = 0; i < s1.length(); i++) {
				if (s1.charAt(i) != s2.charAt(i)) {
					distance++;
				}
			}
		}
		return distance;
	}

//	public static void main(String[] args){
//		String s1 = "fhudvfhuv";
//		String s2 = "lllltfhlluvyyyy";
//		System.out.println(longestCommonSubstring(s1,s2));
//		System.out.println(longestCommonSubsequence(s1,s2));
//	}
}
