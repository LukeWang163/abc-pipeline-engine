package base.operators.operator.nlp.feature.core.assist;

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
	//将原始文本内容按照空格隔开，并返回结果
	public static Map<String, List<String>> cutWords(Map<String, String> text){
		//按空格切分之后的结果存放
		Map<String, List<String>> result = new HashMap<>();
		for (Map.Entry<String, String> entry : text.entrySet()){
			List<String> string_split = new ArrayList<String>(Arrays.asList(entry.getValue().split(" ")));
			result.put(entry.getKey(), string_split);
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
	 * 按照treemap中的值进行排序
	  * @param treeMap
	  * @param ascending
	  * @return 排序后结果
	  */
	public static List<Map.Entry<Integer, Double>> treeMapSortByValue(TreeMap<Integer, Double> treeMap, boolean ascending) {
		// 比较器
		Comparator<Map.Entry<Integer, Double>> valueComparator = new Comparator<Map.Entry<Integer, Double>>(){
			@Override
			public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
				// TODO Auto-generated method stub
				if (ascending){
					return o1.getValue().compareTo(o2.getValue());
				}else{
					return o2.getValue().compareTo(o1.getValue());
				}

			}
		};
		// Map转换成List进行排序
		List<Map.Entry<Integer, Double>> sort_list = new ArrayList<>(treeMap.entrySet());
		// 排序
		Collections.sort(sort_list, valueComparator);
		return sort_list;
	}
}
