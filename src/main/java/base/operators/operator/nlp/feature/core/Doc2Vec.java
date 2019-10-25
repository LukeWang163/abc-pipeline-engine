package base.operators.operator.nlp.feature.core;

import base.operators.operator.nlp.feature.core.assist.Util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 该组件接在Word2Vec组件之后，通过Word2Vec的单词特征结果计算相应的文档特征。
 */
public class Doc2Vec {
	/**
	 * 计算文档特征
	 * @param  docId:区分文档的文档ID；
	 * @param  feature:文档单词的特征数组,其中docId.length = feature.length
	 * @param  mode:生成文档特征的方式（如果是true平均average,否则是加和sum）
	 * @return Map<Integer,double[]>:文档特征，Integer为文档id，double[]为相应的文档特征。
	 */
	public static Map<String, double[]> getDoc2Vec(String[] docId, double[][] feature, boolean mode) throws Exception{
		//得到所有的非重复的文档id的集合
		Set<String> doc_id_set = new HashSet<>();
		for (String id : docId){
			doc_id_set.add(id);
		}
		//根据文档id，计算每个文档的特征
		Map<String, double[]> doc_feature = new HashMap<>();
		for (String id : doc_id_set){
			double[] per_doc_feature = new double[feature[0].length];
			int count = 0;
			for (int i = 0; i < docId.length; i++){
				if (id.equals(docId[i])){
					count +=1;
					per_doc_feature = Util.arrayAdd(per_doc_feature, feature[i]);
				}
			}
			//按照特征的生成模式，分情况计算
			if (mode){
				for (int j = 0; j < per_doc_feature.length; j++){
					per_doc_feature[j] = per_doc_feature[j] / count;
				}
				doc_feature.put(id, per_doc_feature);	
			}else{
				doc_feature.put(id, per_doc_feature);	
			}
			
		}
		return doc_feature;
	}
//	public static void main(String[] args) throws Exception{
//		double[][] arr1 = new double[][]{{1,2}, {2, 3}, {4, 5},{11, 22}};
//		String[] id = new String[]{"1","1","1","3"};
//		Doc2Vec doc2vec = new Doc2Vec();
//		Map<String, double[]> add = doc2vec.getDoc2Vec(id, arr1, true);
//		for (String i : add.keySet()){
//			System.out.println(i);
//			String str = "";
//			for (int l = 0; l < add.get(i).length; l++){
//				str = str + " " + add.get(i)[l];
//			}
//			System.out.println(str);
//		}
//	}
	
}
