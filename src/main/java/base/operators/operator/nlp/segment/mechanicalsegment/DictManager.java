package base.operators.operator.nlp.segment.mechanicalsegment;

import base.operators.example.Example;
import base.operators.example.ExampleSet;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class DictManager {
	
	public static TreeMap<String, TreeMap<String, Double>> dictMap = new TreeMap<String, TreeMap<String, Double>>();
	public static TreeMap<String, Double> wordFrequencyMap = new TreeMap<String, Double>();
	public static int MAX_FRQUENCY = 0;
	
	public static DoubleArrayTrie dat = new DoubleArrayTrie(); 
	public static List<String> words = new ArrayList<String>();
	public static Set<String> word_set = new HashSet<String>();

	public static void initDict(ExampleSet dictExampleSet, String wordCol, String posCol) {
		for(Example example : dictExampleSet) {
			String word = example.getValueAsString(dictExampleSet.getAttributes().get(wordCol));
			if(word.equals("1")) {
				System.out.println(1);
			}
			String pos = example.getValueAsString(dictExampleSet.getAttributes().get(posCol));
			
			String[] poslist = pos.split(" ");

			// ++++++++++++++正常词典格式：word nature1 freq1 nature2
			// freq2++++++++++++++++++++++++++
			int natureCount = poslist.length / 2;
			double totalFrequency = 0;
			TreeMap<String, Double> posMap = new TreeMap<String, Double>();
			for (int i = 0; i < natureCount; ++i) {
				String posTag = poslist[2 * i];
				Double posFrq = 0.0;
				if(StringUtils.isNumeric(poslist[1 + 2 * i])) {
					posFrq = Double.parseDouble(poslist[1 + 2 * i]);
				}
				totalFrequency += posFrq;
				if (posTag != null && (!"".equals(posTag))) {
					Double frequency = posMap.get(posTag);
					if (frequency == null)
						frequency = 5d;
					frequency = frequency + posFrq;
					posMap.put(posTag, frequency);
				}
			}
			if (word != null && (!"".equals(word))) {
				word_set.add(word);
				// 惰性词及其词频比较添加
				TreeMap<String, Double> oldPosMap = dictMap.get(word);
				if (oldPosMap == null) {
					dictMap.put(word, posMap);
				} else {
					Iterator<String> it = posMap.keySet().iterator();
					while (it.hasNext()) {
						String posTemp = (String) it.next();
						Double oldFrq = oldPosMap.get(posTemp);
						if (oldFrq == null) {
							oldPosMap.put(pos, posMap.get(posTemp));
						} else {
							Double newFrq = posMap.get(posTemp);
							if (newFrq > oldFrq)
								oldPosMap.put(pos, newFrq);
						}
					}
					dictMap.put(word, oldPosMap);
				}
				MAX_FRQUENCY += totalFrequency;
				wordFrequencyMap.put(word, totalFrequency);
			}
		}
		words = new ArrayList<String>(word_set);
		// 必须先排序
		Collections.sort(words);
		int load_error = dat.build(words);
		System.out.println("词典加载是否错误: " + load_error);
		System.out.println("词总数: " + words.size());
	}
}
