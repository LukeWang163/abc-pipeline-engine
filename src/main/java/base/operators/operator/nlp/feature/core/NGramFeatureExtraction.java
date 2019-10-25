package base.operators.operator.nlp.feature.core;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.nlp.feature.core.assist.CorpusVocabulary;
import base.operators.operator.nlp.feature.core.assist.NgramText;

import java.util.*;

/**
 * 对于给定文档列，根据生成的n-gram文本信息、选择的词典、选择的权重等参数，构造生成文本计数特征。
 * 组件的所有参数：List<String> text:待分析的文档，分好词的文档，以空格隔开；
 *           String userVocabulary:用户词典；
 String vocabularyMode:词典模式(创建create，只读readonly，合并merge)；
 int ngram：ngram的长度；
 String weight：特征生成中使用的权重函数，包含：{"Binary","TF","IDF","TFIDF"}；
 int minFrequency：截断最小词频，词频小于该数值的ngram，不会加入词典；
 Double maxNgramDocRatio：最大ngram的文档频率，频率过大的ngram，不会加入词典；
 boolean normalize：是否标准化特征（L2范数）；
 boolean detectOutofVocabularyRows：是否筛选词表之外的列；
 */
public class NGramFeatureExtraction {

    public ExampleSet exampleSet;
	public Attribute docIdAttribute;
	public Attribute docAttribute;
    public Double[][] feature;
    public CorpusVocabulary vocabulary;

	public Map<String, Integer> userVocabularyDF;
	public Map<String, Double> userVocabularyIDF;
	public int userTotalNumDocs;
	public int vocabularyMode;
	public int ngramSize;
	public int weight;
	public int minFrequency;
	public double maxNgramDocRatio;
	public boolean normalize;


	/**
	 * 计算给定多文档内容的特征
	 * @param exampleSet:文档数据集；
	 * @param docAttribute:文档所在列
	 * @param userVocabularyDF
	 * @param userVocabularyIDF
	 * @param userTotalNumDocs
	 * @param vocabularyMode:词典的更新使用模式,
	 * @param ngramSize:ngram大小；
	 * @param weight:权重函数；
	 * @param minFrequency:截断词频；
	 * @param maxNgramDocRatio:最大文档频率；
	 * @param normalize:是否标准化；
	 * @return Double[][]:文档特征数组。
	 */
	public NGramFeatureExtraction(ExampleSet exampleSet, Attribute docIdAttibute, Attribute docAttribute, Map<String, Integer> userVocabularyDF, Map<String, Double> userVocabularyIDF, int userTotalNumDocs, int vocabularyMode, int ngramSize, int weight, int minFrequency, double maxNgramDocRatio, boolean normalize){
		this.exampleSet = exampleSet;
		this.docIdAttribute = docIdAttibute;
		this.docAttribute = docAttribute;
		//用户词典
		this.userVocabularyDF = userVocabularyDF;
		this.userVocabularyIDF = userVocabularyIDF;
		this.userTotalNumDocs = userTotalNumDocs;
		this.vocabularyMode = vocabularyMode;
		this.ngramSize = ngramSize;
		this.weight = weight;
		this.minFrequency = minFrequency;
		this.maxNgramDocRatio = maxNgramDocRatio;
		this.normalize = normalize;
	    //最终计算使用的词典
        this.vocabulary = updateVocabulary(userVocabularyDF, userVocabularyIDF, userTotalNumDocs, vocabularyMode, ngramSize, minFrequency, maxNgramDocRatio);
		//最终文本特征的结果
        this.feature = new Double[exampleSet.size()][vocabulary.df.size()];
	}

	public void computeFeature(){

		//根据权重函数，生成相应的特征
		List<String> wordSetVocab = new ArrayList<>(vocabulary.df.keySet());
		for (int k = 0; k < wordSetVocab.size(); k++){
			for (int r = 0; r < exampleSet.size(); r++){
				Example example = exampleSet.getExample(r);
				NgramText perNgram = new NgramText(Arrays.asList(example.getValueAsString(docAttribute).split("\\s+")), ngramSize);
				if (perNgram.ngramText.contains(wordSetVocab.get(k))){
					if (0 == weight){
						this.feature[r][k] = (double)1;
					}else if(1 == weight){
						this.feature[r][k] = (double)Math.round(vocabulary.tf.get(example.getValue(docIdAttribute)).get(wordSetVocab.get(k))*1000)/1000;
					}else if(2 == weight){
						this.feature[r][k] = (double)Math.round(vocabulary.idf.get(wordSetVocab.get(k))*1000)/1000;
					}else if(3 == weight){
						this.feature[r][k] = (double)Math.round(vocabulary.tfidf.get(example.getValue(docIdAttribute)).get(wordSetVocab.get(k))*1000)/1000;
					}
				}else{
					this.feature[r][k] = (double) 0;
				}
			}
		}
		//对得到的特征进行L2范数标准化
		if (normalize){
			for (int m = 0; m < this.feature.length; m++){
				double rowSum = 0;
				for (double num : this.feature[m]){
					rowSum = rowSum + Math.pow(num, 2);
				}
				rowSum = Math.sqrt(rowSum);
				if (rowSum != 0){
					for (int n = 0; n < this.feature[m].length; n++){
						this.feature[m][n] = (double)Math.round(this.feature[m][n] / rowSum * 1000)/1000;
					}
				}
			}
		}
	}

	/**
	 * 根据词典的模式参数，生成特征计算过程中使用的最终的词典
	 * @param userVocabularyDF
	 * @param userVocabularyIDF
	 * @param userTotalNumDocs
	 * @param vocabularyMode:词典的更新使用模式,
	 * @param ngramSize:ngram大小；
	 * @param minFrequency:截断词频；
	 * @param maxNgramDocRatio:最大文档频率；
	 * @return Map<String, Map<String, Double>>:最终的词典。
	 */

	public CorpusVocabulary updateVocabulary(Map<String, Integer> userVocabularyDF, Map<String, Double> userVocabularyIDF, int userTotalNumDocs, int vocabularyMode, int ngramSize, int minFrequency, Double maxNgramDocRatio){
		if(userVocabularyDF.size()==0||userVocabularyIDF.size()==0){
		    vocabularyMode = 0;
        }
	    //最终计算使用的词典
		CorpusVocabulary finalVocabulary = new CorpusVocabulary();
		//调用生成词典方法，获得语料生成的词典
		CorpusVocabulary corpusVocabulary = new CorpusVocabulary(exampleSet, docIdAttribute, docAttribute, ngramSize, minFrequency, maxNgramDocRatio);
		if (0 == vocabularyMode){
			finalVocabulary = corpusVocabulary;
		}else if (1 == vocabularyMode){
			finalVocabulary.df = userVocabularyDF;
			finalVocabulary.idf = userVocabularyIDF;
            finalVocabulary.totalNumDocs = userTotalNumDocs;
			//更新tf以及tfidf,去掉在用户词典不存在的tf和tfidf
			for(Map.Entry<String, Map<String, Long>> entry : corpusVocabulary.tf.entrySet()){
				Map<String, Long> update_tf = new HashMap<>();
				Map<String, Double> update_tfidf = new HashMap<>();
				for(Map.Entry<String, Long> perEntry : entry.getValue().entrySet()){
					if(finalVocabulary.df.keySet().contains(perEntry.getKey())){
						update_tf.put(perEntry.getKey(), perEntry.getValue());
						update_tfidf.put(perEntry.getKey(), perEntry.getValue() * finalVocabulary.idf.get(perEntry.getKey()));
					}
				}
				finalVocabulary.tf.put(entry.getKey(), update_tf);
				finalVocabulary.tfidf.put(entry.getKey(), update_tfidf);
			}
		}else if (3 == vocabularyMode){
			for(Map.Entry<String, Integer> dfEntry : corpusVocabulary.df.entrySet()){
				int user_value = userVocabularyDF.keySet().contains(dfEntry.getKey()) ? userVocabularyDF.get(dfEntry.getKey()) : 0;
				finalVocabulary.df.put(dfEntry.getKey(), user_value + corpusVocabulary.df.get(dfEntry.getKey()));
			}
            for(Map.Entry<String, Integer> dfEntry : userVocabularyDF.entrySet()){
				int corpus_value = corpusVocabulary.df.keySet().contains(dfEntry.getKey()) ? corpusVocabulary.df.get(dfEntry.getKey()) : 0;
				finalVocabulary.df.put(dfEntry.getKey(), userVocabularyDF.get(dfEntry.getKey()) + corpus_value);
            }
			for(Map.Entry<String, Integer> dfEntry : finalVocabulary.df.entrySet()){
				finalVocabulary.idf.put(dfEntry.getKey(), Math.log10((this.exampleSet.size() + userTotalNumDocs) / (finalVocabulary.df.get(dfEntry.getKey()) + 1)));
			}
			finalVocabulary.tf = corpusVocabulary.tf;

			for(Map.Entry<String, Map<String, Long>> entry : corpusVocabulary.tf.entrySet()){
				Map<String, Double> update_tfidf = new HashMap<>();
				for(Map.Entry<String, Long> perEntry : entry.getValue().entrySet()){
					if(finalVocabulary.df.keySet().contains(perEntry.getKey())){
						update_tfidf.put(perEntry.getKey(), perEntry.getValue() * finalVocabulary.idf.get(perEntry.getKey()));
					}
				}
				finalVocabulary.tfidf.replace(entry.getKey(), update_tfidf);
			}
			finalVocabulary.totalNumDocs = corpusVocabulary.totalNumDocs + userTotalNumDocs;

		}
		return finalVocabulary;
	}

	/**
	 * 根据词典，筛选给定多文档内容
	 * @param userVocabularyDF
	 * @param userVocabularyIDF
	 * @param userTotalNumDocs
	 * @param ngramSize:ngram大小；
	 * @param minFrequency:截断词频；
	 * @param maxNgramDocRatio:最大文档频率；
	 * @return List<List<String>>:筛选后的文档内容。
	 */
	public List<List<String>> getDetectAfterText(Map<String, Integer> userVocabularyDF, Map<String, Double> userVocabularyIDF, int userTotalNumDocs, int vocabularyMode, int ngramSize, int minFrequency, Double maxNgramDocRatio){
		//最终结果：删除词典之外的n-gram文本
		List<List<String>> text_detect = new ArrayList<List<String>>();
		//调用生成词典方法，获得生成的词典
		CorpusVocabulary vocabulary = updateVocabulary(userVocabularyDF, userVocabularyIDF, userTotalNumDocs, vocabularyMode, ngramSize, minFrequency, maxNgramDocRatio);
		for (int i = 0; i < exampleSet.size(); i++){
			NgramText perNgram = new NgramText(Arrays.asList(exampleSet.getExample(i).getValueAsString(docAttribute).split("\\s+")), ngramSize);
			perNgram.ngramText.retainAll(new ArrayList<String>(vocabulary.df.keySet()));
			text_detect.add(perNgram.ngramText);
		}
		return text_detect;
	}
	/**
	 * 计算每个文档筛选后剩余的特征个数
	 * @param userVocabularyDF
	 * @param userVocabularyIDF
	 * @param userTotalNumDocs
	 * @param vocabularyMode:词典的更新使用模式,
	 * @param ngramSize:ngram大小；
	 * @param minFrequency:截断词频；
	 * @param maxNgramDocRatio:最大文档频率；
	 * @return List<Integer>:每个文本筛选后的特征个数。
	 */
	public List<Integer> getOutofVocabularyRows(Map<String, Integer> userVocabularyDF, Map<String, Double> userVocabularyIDF, int userTotalNumDocs, int vocabularyMode, int ngramSize, int minFrequency, Double maxNgramDocRatio){
		//最终结果：删除词典之外的n-gram文本之后，ngram个数
		List<Integer> text_detect_num = new ArrayList<Integer>();
		//调用文本筛选方法，获得删选之后的文本
		List<List<String>> text_detect = getDetectAfterText(userVocabularyDF, userVocabularyIDF, userTotalNumDocs, vocabularyMode, ngramSize, minFrequency, maxNgramDocRatio);
		for (int i = 0; i < text_detect.size(); i++){
			text_detect_num.add(text_detect.get(i).size());
		}
		return text_detect_num;
	}
}
