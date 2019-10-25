package base.operators.operator.nlp.similar.core;

import base.operators.operator.nlp.similar.assist.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringSimilarityTopN {

	public static List<String> inputList = new ArrayList<>();
	public static List<String> mapList = new ArrayList<>();
	public static List<Double> simList = new ArrayList<>();

	public StringSimilarityTopN(List<String> inputCol, List<String> mapCol, String method, int topN, int k, double lambda){
        stringSimilarityTopN( inputCol,  mapCol, method, topN, k, lambda);
    }
	/**
	 * 计算两列字符串的相似度最高的topN
	 * @param inputCol:输入表字符串；
	 * @param mapCol:映射表字符串；
	 * @param method ：距离计算方法
	 * @param topN:最终给出的相似度最大值的个数
	 */
	public void stringSimilarityTopN(List<String> inputCol, List<String> mapCol, String method, int topN, int k, double lambda){
		for (int u = 0; u < inputCol.size(); u++) {
			Map<String[], Double> string_distance = new HashMap<>();
			for (int w = 0; w < mapCol.size(); w++) {
				String[] pair = new String[2];
				pair[0] = inputCol.get(u);
				pair[1] = mapCol.get(w);
				double distance = TextSimilarity.computeDistance(pair[0], pair[1], "", method, k, lambda);
				string_distance.put(pair, distance);
			}
            if(string_distance.size()==0){
                continue;
            }
			//排序
			Map<String[], Double> sort_list = Util.top(Math.min(topN, string_distance.size()), string_distance);
			//与该字符串最相似的前topN个结果
			for (Map.Entry<String[], Double> keyEntry : sort_list.entrySet()){
				inputList.add(keyEntry.getKey()[0]);
				mapList.add(keyEntry.getKey()[1]);
				simList.add(keyEntry.getValue());
			}
		}
	}
//	public static void main(String[] args){
//		List<String> inputCol = new ArrayList<String>();
//		inputCol.add("今天很开心有没有");
//		inputCol.add("哈哈哈哈唉哈哈");
//		inputCol.add("今天开心");
//		inputCol.add("元旦护士VB发半句发布的");
//		inputCol.add("哈哈吃饭方法哈哈唉哈哈");
//		List<String> mapCol = new ArrayList<String>();
//		mapCol.add("内存虚仙女小女警逆风局");
//		mapCol.add("今天开心");
//		mapCol.add("元旦护士VB发半句发布的");
//		mapCol.add("哈哈吃饭方法哈哈唉哈哈");
//		mapCol.add("内存虚仙女小女警逆风局");
//		StringSimilarityTopN sstn = new StringSimilarityTopN(inputCol, mapCol, "cosine", 2,2,0.9);
//		System.out.println(sstn.inputList);
//		System.out.println(sstn.mapList);
//		System.out.println(sstn.simList);
//
//	}
}
