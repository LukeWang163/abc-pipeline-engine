package base.operators.operator.nlp.similar.core;

import base.operators.operator.nlp.similar.assist.*;
import base.operators.operator.nlp.similar.assist.sequenceAlignment.NeedlemanWunsch;
import base.operators.operator.nlp.similar.assist.sequenceAlignment.SmithWaterman;

import java.util.Arrays;
import java.util.List;

public class TextSimilarity {
	/**
	 * 计算两个文本的相似度
	 * @param s1
	 * @param s2
     * @param separator:""表示字符串，" "表示文章级别
	 * @param method ：距离计算方法
	 * @return Double：两个字符串距离
	 */
	public static double computeDistance(String s1, String s2, String separator, String method, int k, double lambda){
		double distance = 0;
		if ("levenshtein".equals(method)){
			Levenshtein ls = new Levenshtein(s1, s2, separator);
			distance = ls.levenshteinDistance();
		}else if ("levenshtein_sim".equals(method)){
			Levenshtein ls = new Levenshtein(s1, s2, separator);
			distance = ls.levenshteinSim();
		} else if("damerau_levenstein".equals(method)){
			DamerauLevenstein dls = new DamerauLevenstein( s1, s2,separator);
			distance = dls.damerauLevensteinDistance();
		}else if("damerau_levenstein_sim".equals(method)){
			DamerauLevenstein dls = new DamerauLevenstein( s1, s2,separator);
			distance =dls.damerauLevenshteinSim();
		}else if ("lcs".equals(method)){
			distance = LongestCommonSub.longestCommonSubstringDistance(s1, s2);
		}else if ("lcs_sim".equals(method)){
			distance = LongestCommonSub.longestCommonSubstringSim(s1, s2);
		}else if ("simhash_hamming".equals(method)){
			SimHash hash1 = new SimHash(s1, 64);
			SimHash hash2 = new SimHash(s2, 64);
			distance = (double)hash1.getDistance(hash1.strSimHash, hash2.strSimHash);
		}else if ("simhash_hamming_sim".equals(method)){
			SimHash hash1 = new SimHash(s1, 64);
			SimHash hash2 = new SimHash(s2, 64);
			distance = 1 - (double)hash1.getDistance(hash1.strSimHash, hash2.strSimHash) / 64;
		}else if ("cosine".equals(method)){
			double[][] vec = OneHot.stringCharacterOneHot(s1,s2);
			distance = Util.cosineDistance(vec[0], vec[1]);
		}else if ("ssk".equals(method)){
			List<String> list = Arrays.asList(s1, s2);
			StringSubsequenceKernel ssk = new StringSubsequenceKernel(list, separator, k, lambda);
			double[][] score = ssk.kernel();
			try {
				distance = Util.arrayMultiply(score[0], score[1]) / Math.sqrt(Util.arrayMultiply(score[0], score[0])) / Math.sqrt(Util.arrayMultiply(score[1], score[1]));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else if("jaro_sim".equals(method)){
			distance = JaroWinkler.jaroSim(s1, s2);
		}else if("jaro_winkler_sim".equals(method)){
			distance = JaroWinkler.jaroWinklerSim(s1, s2);
		}else if ("needleman_wunsch".equals(method)){
			int match = 1;
			int mismatch = -1;
			int gap = -2;
			NeedlemanWunsch aligner = new NeedlemanWunsch(s1, s2, match, mismatch, gap);
			String[] alignment = aligner.getAlignment();
			for (int i = 0; i < alignment[0].length(); i++) {
				if (alignment[0].charAt(i) == alignment[1].charAt(i)) {
					distance += 1;
				}
			}
			distance = distance / alignment[0].length();
		}else if ("smith_waterman".equals(method)){
			int match = 1;
			int mismatch = -1;
			int gap = -2;
			SmithWaterman aligner = new SmithWaterman(s1, s2, match, mismatch, gap);
			distance = aligner.getAlignment()[0].length() / (double)Math.max(s1.length(), s2.length());
		}
		return distance;
	}
//	public static void main(String[] args){
//		String s1 = "wohah";
//		String s2 = "dihahAAA";
//		System.out.println(twoStringDistance(s1, s2, "ssk", 2, 0.9));
//
//	}
}
