package base.operators.operator.nlp.feature.core;

import base.operators.operator.nlp.feature.core.assist.Util;

import java.util.*;

/**
 * 计算互信息PMI
 * 对于批量文档，计算文档中单词之间的互信息。
 *
 */
public class PMI {

	public List<String> text;
	public int minFrequency;
	public int windowSize;

	//定义最终输出的三个结果
	//存放单词以及单词的频数，键为单词，值为单词的频数wordFrequency
	public static Map<String, Integer> singleWordFrequency = new HashMap<>();
	//存放所有的共现单词对以及共现频数，键为共现单词对，值为共现单词对的频数fullWordPairFrequency
	public static Map<String, Integer> fullWordPairFrequency = new HashMap<>();
	//计算共现单词对的PMI值
	public static Map<String, Double> fullWordPairPMI = new HashMap<>();
	/*
	* @param text:文档内容（内容为分词完毕，以空格隔开的形式）；
 	* @param minFrequency:截断词频；
 	* @param windowSize:窗口大小
 	*/
	public PMI(List<String> text, int minFrequency, int windowSize){
		this.text = text;
		this.minFrequency = minFrequency;
		this.windowSize = windowSize;
	}
	/**
	 * 根据截断词频，生成文档的全表词典以及词频
	 * @param text_split：文档内容（按空格切分好的）
	 * @return Map<String, Integer>:词典。
	 */
	public Map<String, Integer> getVocabulary(List<List<String>> text_split){
		//存放全表词典以及词频vocabulary
		Map<String, Integer> vocabulary = new HashMap<String, Integer>();
		for (int i = 0; i < text_split.size(); i++){
			//将每个单词存入全表词典，并将词频更新
			for (int j = 0; j < text_split.get(i).size(); j++){
				vocabulary.put(text_split.get(i).get(j), vocabulary.containsKey(text_split.get(i).get(j))? vocabulary.get(text_split.get(i).get(j)) + 1 : 1);
			}
		}
		//如果minFrequency大于1，则保留全表词典中词频大于等于minFrequency单词
		if (minFrequency > 1){
			Iterator<String> iterator = vocabulary.keySet().iterator(); 
			 while (iterator.hasNext()) {
			     String key = iterator.next();
			     if (vocabulary.get(key) < minFrequency) {
			    	 iterator.remove(); 
			      }
			  }
		}
		return vocabulary;
	}
	/**
	 * 由原始文档内容，根据窗口大小，生成文本信息
	 * @param text_split:文档内容（按空格切分好的）；
	 * @param vocabulary：词典；
	 * @return List<List<String>>:新的文本信息。
	 */
	public List<List<String>> getWindowsText(List<List<String>> text_split, Map<String, Integer> vocabulary){
		//按照窗口windowSize的大小，生成每个单词的窗口text_windows_list
		//每个单词的窗口列表，第一个单词为当前词，后边的词为该词的共现词
		List<List<String>> text_windows_list = new ArrayList<List<String>>();
		for (int i = 0; i < text_split.size(); i++){
			//如果minFrequency>1，需要从语料中移除低词频的单词，再进行分析
			if (minFrequency > 1){
				text_split.get(i).retainAll(vocabulary.keySet());
			}
			for (int j = 0; j < text_split.get(i).size(); j++){
				//windowSize=0是默认整行
				if (windowSize == 0){
					text_windows_list.add(text_split.get(i).subList(j, text_split.get(i).size()));	
				}else{
					//生成该单词的窗口列表
					if ((j+1) < text_split.get(i).size()){
						int endIndex = (j + windowSize + 1) <= text_split.get(i).size() ? j + windowSize + 1 : text_split.get(i).size();
						List<String> subList = text_split.get(i).subList(j, endIndex);
						text_windows_list.add(subList);	
					}
				}
				
			}
		}
		return text_windows_list;
	}
	/**
	 * 互信息计算
	 * @return Map<String, Object>:Map中包含三个键值输出，键"singleWordFrequency"对应单个单词的词频表、
	 *                                            键"pairWordFrequency"对应共现单词对的词频表、
	 *                                            键"pairWordPMI"对应共现单词对的PMI表
	 */
	public void computePMI(){
		//所有文档按空格切分后，存入text_split
		List<List<String>> text_split = Util.cutWords(text);
		//存放全表词典以及词频vocabulary
		Map<String, Integer> vocabulary = getVocabulary(text_split);
		//按照窗口windowSize的大小，生成每个单词的窗口text_windows_list
		List<List<String>> text_windows_list = getWindowsText(text_split, vocabulary);
		Map<Set<String>, Integer> fullWordPairFrequencyTrans = new HashMap<>();
		for (int u = 0; u < text_windows_list.size(); u++){
			for (int t = 1; t < text_windows_list.get(u).size(); t++){
				String word1 = text_windows_list.get(u).get(0);
				String word2 = text_windows_list.get(u).get(t);
				//单个单词的频数更新
				singleWordFrequency.put(word1, singleWordFrequency.containsKey(word1) ? singleWordFrequency.get(word1) + 1 : 1);
				singleWordFrequency.put(word2, singleWordFrequency.containsKey(word2) ? singleWordFrequency.get(word2) + 1 : 1);	
				//共现单词对的频数更新
				Set<String> per_pair = new HashSet<String>();
				per_pair.add(word1); 
				per_pair.add(word2);
				fullWordPairFrequencyTrans.put(per_pair, fullWordPairFrequencyTrans.containsKey(per_pair) ? fullWordPairFrequencyTrans.get(per_pair) + 1 : 1);
			}		
		}
		//将共现单词结果表fullWordPairFrequencyTrans的键进行标准化，Set转为String使得[开心]->开心 开心
		for (Map.Entry<Set<String>, Integer> entry : fullWordPairFrequencyTrans.entrySet()){
			List<String> keyList = new ArrayList<String>(entry.getKey());
			if (keyList.size() == 1){
				keyList.add(keyList.get(0));
			}
			fullWordPairFrequency.put(String.join(" ",keyList), entry.getValue());
		}
		Integer pairFrequencySum = 0;
		for(int num : fullWordPairFrequency.values()){
			pairFrequencySum = pairFrequencySum + num;
		}
		for (Map.Entry<String, Integer> entry : fullWordPairFrequency.entrySet()){
			//共现单词对中两个单词的各自词频的乘积
			double denominator = 1;
			for (String word : entry.getKey().split(" ")){
				denominator = denominator * singleWordFrequency.get(word);
			}
			//共现单词对的PMI计算
			fullWordPairPMI.put(String.join(" ", entry.getKey()), (double)Math.log(entry.getValue() * pairFrequencySum / denominator));
		}
	}
	
//	public static void main( String[] args ){
//        List<String> test = new ArrayList<String>();
//        test.add("我 很 开心 啊 开心 是 啊 我");
//        test.add("我 开心");
//        PMI pmi = new PMI(test, 2, 0);
//        System.out.println(pmi.singleWordFrequency);
//		System.out.println(pmi.fullWordPairFrequency);
//		System.out.println(pmi.fullWordPairPMI);
//    }
}

