package base.operators.operator.nlp.similar.assist;

import java.util.*;

public class Util {
	//将原始文本内容按照空格隔开，并返回结果
	public static List<List<String>> cutWords(List<String> text){
		//按空格切分之后的结果存放
		List<List<String>> result = new ArrayList<List<String>>();
		for (String string : text){
			List<String> string_split = new ArrayList<String>(Arrays.asList(string.split(" ")));
			result.add(string_split);
		}
		return result;
	}
	//两个数组的对应元素加和
	public static double[] arrayAdd(double[] array1, double[] array2) throws Exception{
		double[] add = new double[array1.length];
		if (array1.length != array2.length){
			throw new Exception("两个数组的长度不一致");
		}else{
			for (int i = 0; i < array1.length; i++){
				add[i] = array1[i] + array2[i];
			}
		}

		return add;
	}
	//两个数组内积
	public static double arrayMultiply(double[] array1, double[] array2) throws Exception{
		double multiply = 0;
		if (array1.length != array2.length){
			throw new Exception("两个数组的长度不一致");
		}else{
			for (int i = 0; i < array1.length; i++){
				multiply += array1[i] * array2[i];
			}
		}

		return multiply;
	}
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
	 * 求三个数中的最小值
	 * @param a
	 * @param b
	 * @param c
	 * @return 最小值
	 */
	public static double getMin(double a, double b, double c) {

		return Math.min(Math.min(a, b), c);
	}

	public static double getMin(double a, double b) {

		return Math.min(a, b);
	}
	/**
	 * 按照treemap中的值进行排序
	  * @param treeMap
	  * @param ascending
	  * @return 排序后结果
	  */
	public static List<Map.Entry<String, Double>> treeMapSortByValue(TreeMap<String, Double> treeMap, boolean ascending) {
		// 比较器
		Comparator<Map.Entry<String, Double>> valueComparator = new Comparator<Map.Entry<String, Double>>(){
			@Override
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				// TODO Auto-generated method stub
				if (ascending){
					return o1.getValue().compareTo(o2.getValue());
				}else{
					return o2.getValue().compareTo(o1.getValue());
				}

			}
		};
		// Map转换成List进行排序
		List<Map.Entry<String, Double>> sort_list = new ArrayList<>(treeMap.entrySet());
		// 排序
		Collections.sort(sort_list, valueComparator);
		return sort_list;
	}
	/**
	 * 按照map中的值进行排序,取前size个
	 * @param size
	 * @param map
	 * @return top结果
	  */

	public static Map<String[], Double> top(int size, Map<String[], Double> map) {
		Map<String[], Double> result = new LinkedHashMap<String[], Double>();
		for (Map.Entry<String[], Double> entry : new MaxHeap<Map.Entry<String[], Double>>(size, new Comparator<Map.Entry<String[], Double>>() {
			public int compare(Map.Entry<String[], Double> o1, Map.Entry<String[], Double> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		}).addAll(map.entrySet()).toList()) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

//	public static void main(String[] args){
//		TreeMap<String, Double> map = new TreeMap<String, Double>();
//		map.put("acb1", 0.23);
//		map.put("bac1", 0.59);
//		map.put("bca1", 0.95);
//		map.put("cab1", 0.83);
//		map.put("cba1", 0.30);
//		map.put("abc1", 0.37);
//		map.put("abc2", 0.57);
//		System.out.println(map);
////		List<Map.Entry<String, Double>> list =  treeMapSortByValue(map, true);
////		System.out.println(list);
//	}
}
