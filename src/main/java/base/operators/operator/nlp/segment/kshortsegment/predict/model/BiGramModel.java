package base.operators.operator.nlp.segment.kshortsegment.predict.model;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class BiGramModel {
	static TreeMap<String, TreeMap<String, Double>> biGramMap = null;

	static TreeMap<String, TreeMap<String, Double>> biPosGramMap = null;
	BiGramModel(InputStream inputStream) {
		load(inputStream);
	}

	BiGramModel(List<String> data) {
		load(data);
	}

	private static volatile BiGramModel biGramModel;

	// 定义一个共有的静态方法，返回该类型实例
	public static BiGramModel getInstance(InputStream inputStream) {
		// 对象实例化时与否判断（不使用同步代码块，instance不等于null时，直接返回对象，提高运行效率）
		if (biGramModel == null) {
			// 同步代码块（对象未初始化时，使用同步代码块，保证多线程访问时对象在第一次创建后，不再重复被创建）
			synchronized (BiGramModel.class) {
				// 未初始化，则初始instance变量
				if (biGramModel == null) {
					biGramModel = new BiGramModel(inputStream);
				}
			}
		}
		return biGramModel;
	}

	//+++++++++++++++实例集合(根据文件的HashCode创建的)++++++++++++++++++++++++++++++++
	static HashMap<String,BiGramModel> biGramModelHashMap = new HashMap<String,BiGramModel>();

	public static BiGramModel getInstance(String hashBiGram, List<String> data){
		if(biGramModelHashMap.get(hashBiGram)==null){
			biGramModelHashMap.put(hashBiGram,new BiGramModel(data));
		}
		return biGramModelHashMap.get(hashBiGram);
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++

	// 定义一个共有的静态方法，返回该类型实例
	public static BiGramModel getIstance(List<String> data) {
		// 对象实例化时与否判断（不使用同步代码块，instance不等于null时，直接返回对象，提高运行效率）
		if (biGramModel == null) {
			// 同步代码块（对象未初始化时，使用同步代码块，保证多线程访问时对象在第一次创建后，不再重复被创建）
			synchronized (BiGramModel.class) {
				// 未初始化，则初始instance变量
				if (biGramModel == null) {
					biGramModel = new BiGramModel( data);
				}
			}
		}
		return biGramModel;
	}


	public static void load(InputStream inputStream) {
		LoadBiGramModel loader = new LoadBiGramModel();
		TreeMap<String, TreeMap<String, Double>> reulstMap[] = loader
				.load(inputStream);
		biGramMap = reulstMap[0];
		biPosGramMap = reulstMap[1];

	}

	public static void load(List<String> data) {
		LoadBiGramModel loader = new LoadBiGramModel();
		TreeMap<String, TreeMap<String, Double>> reulstMap[] = loader
				.load(data);
		biGramMap = reulstMap[0];
		biPosGramMap = reulstMap[1];

	}

	public static double getBiFrequency(String a, String b) {
		TreeMap<String, Double> bMap = biGramMap.get(a);
		if (bMap == null)
			return 0;
		Double count = bMap.get(b);
		if (count == null)
			return 0;
		return count;
	}

	public static double getBiFrequency(String scrWord, String srcPos,
			String targetWord, String targetPos) {
		String wordKey = scrWord + "\t" + targetWord;
		TreeMap<String, Double> transMap = biPosGramMap.get(wordKey);
		if (transMap == null)
			return 0d;
		String posKey = srcPos + "##" + targetPos;
		Double count = transMap.get(posKey);
		if (count == null)
			count = 0d;
		return count;
	}

}
