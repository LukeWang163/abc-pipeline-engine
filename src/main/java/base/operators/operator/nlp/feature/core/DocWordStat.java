package base.operators.operator.nlp.feature.core;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;

import java.util.*;

/**
 * 词频统计
 * 对于批量文档，统计文档中单词的词频。
 *
 */
public class DocWordStat {

	public ExampleSet examples;
	public Attribute idAttribute;
	public Attribute docAttribute;
	public Map<String, List<String>> orderWordResult = new HashMap<>();
	public Map<String, Map<String, Integer>> wordFrequencyResult = new HashMap<>();
	public Map<Integer, String> fullTableWordVocabulary = new HashMap<>();

	public DocWordStat(ExampleSet examples, Attribute idAttribute, Attribute docAttribute){
		this.examples = examples;
		this.idAttribute = idAttribute;
		this.docAttribute = docAttribute;
		getOrderWordResult();
		getWordFrequencyResult();
		getLexiconSet();
	}

	/**
	 * 给定批量文档，给出文档的顺序词表
	 * @return Map<Integer, List<String>>:顺序词表内容。
	 *
	 */
	public void getOrderWordResult(){
		for (int i = 0; i < examples.size(); i++) {
			Example example = examples.getExample(i);
			//每个文档按照空格切分
			String id = example.getValueAsString(idAttribute);
			String[] per_text_list = example.getValueAsString(docAttribute).split(" ");
			orderWordResult.put(id, Arrays.asList(per_text_list));
		}
	}
	/**
	 * 给定批量文档，计算出每个文档中每个单词在该文档的词频
	 * @return Map<String, Map<String, Iteger>> :词频表内容。
	 *
	 */
	public void getWordFrequencyResult(){
		//调用计算顺序词表方法，得到文档的顺序词表
		for (Map.Entry<String, List<String>> idEntry : orderWordResult.entrySet()){
			//存放每个文档的词频统计结果
			Map<String, Integer> countMap = new HashMap<String, Integer>();
			//统计每个文档的词频
			for (String word : idEntry.getValue()) {
				countMap.put(word, countMap.containsKey(word) ? countMap.get(word) + 1 : 1);
			}
			wordFrequencyResult.put(idEntry.getKey(), countMap);

		}
	}
	/**
	 * 给定批量文档，得到该文档集合的全表单词词典
	 * @return Map<Integer, List<String>>:全表词典内容。
	 *
	 */
	public void getLexiconSet(){
		//所有单词的集合
		Set<String> fullTableWordSet = new HashSet<String>();
		for (Map.Entry<String, List<String>> idEntry : orderWordResult.entrySet()){
			fullTableWordSet.addAll(new HashSet<String>(idEntry.getValue()));
		}
		//给每个单词赋值唯一的键word_id
		int word_id = 0;
		for (String word : fullTableWordSet){
			fullTableWordVocabulary.put(word_id, word);
			word_id += 1;
		}
	}
}

