package base.operators.operator.nlp.feature.core;

import java.util.*;

/**
 * 给定三元组（row，col，value）类型为“XXD” 或 “XXL”，“X”表示任意类型，“D”表示Double，“L”表示bigint，
 * 转成kv格式（row,[col_id:value]），其中 row 和 value 类型和原始输入数据一致，
 * col_id 类型是 bigint，并给出col的索引表映射到col_id。
 */
public class TripleToKV {
	/**
	 * 有索引表情况下生成kv表。
	 * @param row：id列；
	 * @param col：键列；
	 * @param value：值列；
	 * @param indexInput：用户索引表；
	 * @return Map<String, Map<Integer, Double>>：kv表的结果。
	 */
	public static Map<String, Map<String, Double>> tripleToKV(List<String> row, List<String> col, List<Double> value, Map<String, String> indexInput){
		//最终kv表的结果
		Map<String, Map<String, Double>> result = new HashMap<>();
		//去除row中重复数据
		Set<String> row_set = new HashSet<String>(row);
		//生成kv表
		for (String row_value : row_set){
			Map<String, Double> k_v = new HashMap<String, Double>();
			for (int i = 0; i < row.size(); i++){
				if (row.get(i).equals(row_value)){
					k_v.put(indexInput.get(col.get(i)), k_v.containsKey(indexInput.get(col.get(i))) ? k_v.get(indexInput.get(col.get(i))) + value.get(i) : value.get(i));
				}
			}
			result.put(row_value, k_v);
		}
		return result;
	}
	public static String getMapToString(Map<String, Object> map, String keyValueSeparator, String keyValuePairSeparator){
		List<String> keySet = new ArrayList<>(map.keySet());
		//将set集合转换为数组
		String[] keyArray = keySet.toArray(new String[keySet.size()]);
		//给数组排序(升序)
		Arrays.sort(keyArray);
		//因为String拼接效率会很低的，所以转用StringBuilder
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < keyArray.length; i++) {
			// 参数值为空，则不参与签名 这个方法trim()是去空格
			if ((String.valueOf(map.get(keyArray[i]))).trim().length() > 0) {
				sb.append(keyArray[i]).append(keyValueSeparator).append(String.valueOf(map.get(keyArray[i])).trim());
			}
			if(i != keyArray.length-1){
				sb.append(keyValuePairSeparator);
			}
		}
		return sb.toString();
	}
//	public static void main(String[] args){
//		Map<String, Double> map = new HashMap<>();
//		map.put(""+2, 23.0);
//		map.put(""+3,000.0);
//		Map<String, Object> mapNew = new HashMap<>(map);
//		System.out.println(getMapToString(mapNew,":",","));
//	}
}
