package base.operators.operator.nlp.similar.core;

import base.operators.operator.nlp.similar.assist.Util;

import java.util.*;

public class SemanticVectorDistance {

	public static List<String> word1_list = new ArrayList<>();
	public static List<String> word2_list = new ArrayList<>();
	public static List<Double> distance_list = new ArrayList<>();
	public static List<Integer> rank_list = new ArrayList<>();

	public SemanticVectorDistance(Map<String, double[]> word_vector, String distanceType, Integer topN, Double distanceThreshold){
		semanticVectorDistance(word_vector, distanceType, topN, distanceThreshold);
	}

	/** 
	 * 计算列表中元素的两两组合,以及该组合的距离
	 * @param Map<String, Double[]> vector:
	 * @param distanceType:距离计算方法
	 * @return 两两组合的组合对，以及距离
	 */
	public Map<Set<String>, Double> pairwiseDistance(Map<String, double[]> word_vector, String distanceType){
		Map<Set<String>, Double> result = new HashMap<Set<String>, Double>();
		List<String> wordSet = new ArrayList<>(word_vector.keySet());
		for (int i = 0; i < wordSet.size(); i++){
			for (int j = i + 1; j < wordSet.size(); j++){
				Set<String> element = new HashSet<String>();
				element.add(wordSet.get(i));
				element.add(wordSet.get(j));
				double distance = 0;
				if ("euclidean".equals(distanceType)){
					distance = Util.euclideanDistance(word_vector.get(wordSet.get(i)), word_vector.get(wordSet.get(j)));
					distance = (double)Math.round(distance * 1000) / 1000;
				}else if ("cosine".equals(distanceType)){
					distance = Util.cosineDistance(word_vector.get(wordSet.get(i)), word_vector.get(wordSet.get(j)));
					distance = (double)Math.round(distance * 1000) / 1000;
				}else if ("manhattan".equals(distanceType)){
					distance = Util.manhattanDistance(word_vector.get(wordSet.get(i)), word_vector.get(wordSet.get(j)));
					distance = (double)Math.round(distance * 1000) / 1000;
				}
				result.put(element, distance);
			}
		}
		return result;
	}
	/** 
	 * 计算两两单词的距离，并按规定输出
	 * @param Map<String, Double[]> vector:
	 * @param distanceType:距离计算方法
	 * @param topN:输出的距离最近的向量的数目
	 * @param distanceThreshold:距离的阈值
	 * @return 两两单词的距离以及等级
	 */
	public void semanticVectorDistance(Map<String, double[]> word_vector, String distanceType, Integer topN, Double distanceThreshold){
		//topN的取值
		int topN_new = Math.min(word_vector.size()-1, topN);
		//计算得到每个单词对的距离
		Map<Set<String>, Double> pairwise_distance = pairwiseDistance(word_vector, distanceType);
		//根据topN和距离的阈值对每个单词的相近词进行筛选
		for (Map.Entry<String, double[]> wordEntry : word_vector.entrySet()){
			//遍历无重复单词集合，存放与该单词word满足条件的单词，及其距离
			TreeMap<String, Double> specify_word = new TreeMap<String, Double>();
			for (Map.Entry<Set<String>, Double> pairEntry : pairwise_distance.entrySet()){
				//只考虑包含该单词并且距离满足条件的单词对
				if(distanceThreshold < 0){
					distanceThreshold = Double.MAX_VALUE;
				}
				if ((pairEntry.getKey().contains(wordEntry.getKey())) && (pairEntry.getValue() < distanceThreshold)){
					//取出除了该单词之外的另一个单词
					String other_word = "";
					for (String item : pairEntry.getKey()){
						if (!item.equals(wordEntry.getKey())){
							other_word = item;
						}
					}
					//存放所有与该单词，有关的单词以及距离
					specify_word.put(other_word, pairEntry.getValue());
				}
			}
			//只考虑过滤后结果大于0的情况
			if (specify_word.size() > 0){
				List<Map.Entry<String, Double>> sort_list = Util.treeMapSortByValue(specify_word, true);
				for (int i = 0; i < Math.min(topN_new, sort_list.size()); i++){
					//依次存放该单词、相近单词、距离、等级
					word1_list.add(wordEntry.getKey());
					word2_list.add(sort_list.get(i).getKey());
					distance_list.add(sort_list.get(i).getValue());
					rank_list.add(i + 1);
				}
			}

		}
	}
	
//	public static void main(String[] args){
//		double[] vec1 = new double[]{1.0,1.2,1.3};
//		double[] vec2 = new double[]{0.9,1.1,0.7};
//		double[] vec3 = new double[]{0.5,0.8,0.1};
//		double[] vec4 = new double[]{0.3,1.0,0.7};
//		double[] vec5 = new double[]{0.9,1.1,0.7};
//		Map<String, double[]> test = new HashMap<String, double[]>();
//		test.put("wo", vec1);
//		test.put("ni", vec2);
//		test.put("ta", vec3);
//		test.put("ha", vec4);
//		test.put("wa", vec5);
//		SemanticVectorDistance res = new SemanticVectorDistance(test, "euclidean", 3, 0.7);
//		System.out.println(res.word1_list);
//		System.out.println(res.word2_list);
//		System.out.println(res.distance_list);
//		System.out.println(res.rank_list);
//	}
}
