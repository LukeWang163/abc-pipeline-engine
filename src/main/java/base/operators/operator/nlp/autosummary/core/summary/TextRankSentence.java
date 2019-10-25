package base.operators.operator.nlp.autosummary.core.summary;

import base.operators.operator.nlp.autosummary.core.stopwords.StopWordDictionary;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TextRank 自动摘要
 *
 * @author
 */
/* extends KeywordExtractor */
public class TextRankSentence {
	/**
	 * 文档句子的个数
	 */
	int D;
	/**
	 * 拆分为[句子[单词]]形式的文档
	 */
	List<List<String>> sens;
	/**
	 * 排序后的最终结果 score <-> index
	 */
	List<Map<String, Object>> top;
	/**
	 * 句子和其他句子的相关程度
	 */
	double[][] weight;
	/**
	 * 该句子和其他句子相关程度之和
	 */
	double[] weight_sum;
	/**
	 * 迭代之后收敛的权重
	 */
	double[] vertex;

	/**
	 * BM25相似度
	 */
	BM25 bm25;

	static String document;

	private static volatile TextRankSentence single = null;
	
	private static StopWordDictionary dictionary = null;

	private static ConfConfig confConfig;

	private TextRankSentence() {

	}

	private TextRankSentence(double d,int max_iter,double min_diff,String size,int lang,int type) {
		confConfig = ConfConfig.getInstance(d,max_iter,min_diff,size,lang,type);
	}

	public static TextRankSentence getInstance(List<String> dicts) {
		if (single == null) {
			synchronized (TextRankSentence.class) {
				if (single == null) {
					single = new TextRankSentence();
					dictionary = new StopWordDictionary(dicts);
				}
			}
		}
		return single;
	}

	public static TextRankSentence getInstance(List<String> dicts,double d,int max_iter,double min_diff,String size,int lang,int type) {
		if (single == null) {
			synchronized (TextRankSentence.class) {
				if (single == null) {
					single = new TextRankSentence(d,max_iter,min_diff,size,lang,type);
					dictionary = new StopWordDictionary(dicts);
				}
			}
		}
		return single;
	}

	private TextRankSentence(List<List<String>> sens) {
		this.sens = sens;
		bm25 = new BM25(sens);
		D = sens.size();
		weight = new double[D][D];
		weight_sum = new double[D];
		vertex = new double[D];
		top = new ArrayList<Map<String, Object>>();
		solve();
	}

	private void solve() {
		int max_iter = ConfConfig.getInstance().getMax_iter();//迭代次数
		double min_diff = ConfConfig.getInstance().getMin_diff();//收敛系数
		double d = ConfConfig.getInstance().getD();//阻尼系数
		int cnt = 0;
		for (List<String> sentence : sens) {
			double[] scores = bm25.simAll(sentence);//计算句子之间的相似度
			weight[cnt] = scores;
			weight_sum[cnt] = sum(scores) - scores[cnt]; // 减掉自己，自己跟自己肯定最相似
			vertex[cnt] = 1.0;
			++cnt;
		}
		for (int _ = 0; _ < max_iter; ++_) {
			double[] m = new double[D];
			double max_diff = 0;
			for (int i = 0; i < D; ++i) {
				m[i] = 1 - d;
				for (int j = 0; j < D; ++j) {
					if (j == i || weight_sum[j] == 0)
						continue;
					m[i] += (d * weight[j][i] / weight_sum[j] * vertex[j]);
				}
				double diff = Math.abs(m[i] - vertex[i]);
				if (diff > max_diff) {
					max_diff = diff;
				}
			}
			vertex = m;
			if (max_diff <= min_diff)
				break;
		}

		// 我们来排个序吧
		for (int i = 0; i < D; ++i) {
			Map<String, Object> v = new HashMap<String, Object>();
			v.put("weight", vertex[i]);
			v.put("index", i);
			top.add(v);
		}
		Collections.sort(top, new Comparator<Map<String, Object>>() {
			public int compare(Map<String, Object> map_0, Map<String, Object> map_1) {
				Double key0 = (Double) map_0.get("weight");
				Double key1 = (Double) map_1.get("weight");
				return -1 * key0.compareTo(key1);
			}
		});
	}

	/**
	 * 获取前几个关键句子
	 *
	 * @param size
	 *            要几个
	 * @return 关键句子的下标
	 */
	private List<Map<String, Object>> getTopSentence(int size) {
		List<Integer> values = new ArrayList<Integer>();
		for (int i = 0; i < top.size(); i++) {
			values.add((Integer) top.get(i).get("index"));
		}
		// Collection<Integer> values = top.values();
		size = Math.min(size, values.size());
		List<Map<String, Object>> indexArray = new ArrayList<Map<String, Object>>();
		Iterator<Integer> it = values.iterator();
		for (int i = 0; i < size; ++i) {
			Map<String, Object> map = new HashMap<String, Object>();
			int index = it.next();
			map.put("index", index);
			map.put("weight", top.get(i).get("weight"));
			indexArray.add(map);
		}
		return indexArray;
	}

	/**
	 * 简单的求和
	 *
	 * @param array
	 * @return
	 */
	private static double sum(double[] array) {
		double total = 0;
		for (double v : array) {
			total += v;
		}
		return total;
	}

    //private static final String P_REGEX = ".+?(\\n|\\r)";//默认段落分割正则表达式
    private static final String S_REGEX = ".+?(。|\\.|！|!|？|\\?|\\n|\\r)";//默认句子分割正则表达式

    /**
     *
     * @param content 待切分的字符串
     * @return 切分后的字符片段
     */
    private static List<String> spiltSentence(String content){
        List<String> matchResult = new ArrayList<String>();
        Pattern pattern = Pattern.compile(S_REGEX);
        Matcher matcher = pattern.matcher(content);
        int lastEndIndex = 0;
        while (matcher.find()) {
            String sentence = matcher.group();
            if(!"".equals(sentence.trim())){
                matchResult.add(sentence);
            }
            lastEndIndex=matcher.end();
        }
        if(lastEndIndex!=content.length()-1){
            String restSentence = content.substring(lastEndIndex);
            matchResult.add(restSentence);
        }
        return matchResult;
    }

	/**
	 * 将句子转换为文档（类似于文档的形式，将句子拆分为词，并且过滤掉停用词及特殊词性的词）
	 *
	 * @param sentenceList
	 * @return
	 */
	private static List<List<String>> convertSentenceListToDocument(List<String> sentenceList, int lang) {
		List<List<String>> docs = new ArrayList<List<String>>(sentenceList.size());
		for (String sentence : sentenceList) {
			List<String> wordList = new LinkedList<String>();
			String[] posWordsArray = sentence.split(" ");
			for (int i = 0; i < posWordsArray.length; i++) {
				if(0 == lang) {
					String[] posArray = posWordsArray[i].split("/");
					if(posArray.length > 1) {
						if (shouldInclude(posWordsArray[i], lang)) {
							wordList.add(posArray[0]);
						}
					}
				} else {
					String[] posArray = posWordsArray[i].split("_");
					if(posArray.length > 1) {
						if (shouldInclude(posWordsArray[i], lang)) {
							wordList.add(posArray[0]);
						}
					}
				}
			}
			docs.add(wordList);
		}
		return docs;
	}


    /**
     * 根据词性以及停用词表判断是否应该保留该词
     * @param term
     * @param lang
     * @return
     */
	private static boolean shouldInclude(String term, int lang) {
		// 除掉停用词
		String nature = "";
		String word = "";
		if(0==lang) {
			String[] posArray = term.split("/");
			if (posArray.length <= 1)
				return false;
			word = posArray[0];
			nature = posArray[posArray.length - 1];
		} else {
			String[] posArray = term.split("_");
			if (posArray.length <= 1)
				return false;
			word = posArray[0];
			nature = posArray[posArray.length - 1];
		}
		
		if (nature != "") {
			char firstChar = nature.charAt(0);
			switch (firstChar) {
			 case 'c':
             case 'e':
             case 'f':
             case 'g':
             case 'h':
             case 'i':
             case 'j':
             case 'k':
             case 'm':
             case 'o':
             case 'p':
             case 'q':
             case 'r':
             case 'u':
             case 'w':
             case 'x':
             case 'C':
             case 'I':
             case 'M':
             case 'O':
             case 'P':
             case 'B':
             case 'L':
             case 'S':
             case 'D':
             case 'A':
             case 'E':
             case 'F':
             {
                 return false;
             }
			default: {
				if (word.trim().length() > 1 && !dictionary.contains(word, lang)) {
					return true;
				}
			}
				break;
			}

		}
		return false;
	}

	/**
	 * 一句话调用接口
	 *
	 * @param doc
	 *            目标文档
	 * @return 关键句列表
	 */
	public List<Map<String, Object>> getTopSentenceList(String doc) {
		
		document = doc;
		//分句
		List<String> sentenceList = spiltSentence(doc);
		int size = Integer.parseInt(ConfConfig.getInstance().getSize());
		int lang = ConfConfig.getInstance().getLang();
		//
		List<List<String>> sens = convertSentenceListToDocument(sentenceList, lang);
		TextRankSentence textRank = new TextRankSentence(sens);
		List<Map<String, Object>> topSentence = textRank.getTopSentence(size);
		List<Map<String, Object>> resultList = new LinkedList<Map<String, Object>>();
		
		for (Map<String, Object> temp : topSentence) {
			int i = (Integer) temp.get("index");
			if(!(getOriginSentence(sentenceList.get(i)) == null || getOriginSentence(sentenceList.get(i)).equals(""))) {
				temp.put("sentence", getOriginSentence(sentenceList.get(i)));
				resultList.add(temp);
			}
		}
		return resultList;
	}
	
	private String getOriginSentence(String sentence) {
		int lang = ConfConfig.getInstance().getLang();
		List<String> wordList = new LinkedList<String>();
		String[] posWordsArray = sentence.split(" ");
		for (int i = 0; i < posWordsArray.length; i++) {
			if(0==lang) {
				String[] posArray = posWordsArray[i].split("/");
				wordList.add(posArray[0]);
			} else {
				String[] posArray = posWordsArray[i].split("_");
				wordList.add(posArray[0]);
			}
		}
		return String.join("", wordList);
	}

	/**
	 * 一句话调用接口
	 *
	 * @param document
	 *            目标文档
	 * @param max_length
	 *            需要摘要的长度
	 * @return 摘要文本
	 */
	private String getSummary(String document, int max_length, int lang) {
		List<String> sentenceList = spiltSentence(document);

		int sentence_count = sentenceList.size();
		int document_length = document.length();
		int sentence_length_avg = document_length / sentence_count;
		int size = max_length / sentence_length_avg + 1;
		List<List<String>> docs = convertSentenceListToDocument(sentenceList, lang);

		TextRankSentence textRank = new TextRankSentence(docs);
		List<Map<String, Object>> topSentence = textRank.getTopSentence(size);
		List<String> resultList = new LinkedList<String>();
		for (Map<String, Object> temp : topSentence) {
			int i = (Integer) temp.get("index");
			resultList.add(sentenceList.get(i));
		}

		resultList = permutation(resultList, sentenceList);
		resultList = pick_sentences(resultList, max_length);

		String delimiter = "。";
		StringBuilder sb = new StringBuilder(resultList.size() * (16 + delimiter.length()));
		for (String str : resultList) {
			sb.append(str).append(delimiter);
		}
		return sb.toString();

	}

	private List<String> permutation(List<String> resultList, List<String> sentenceList) {
		int index_buffer_x;
		int index_buffer_y;
		String sen_x;
		String sen_y;
		int length = resultList.size();
		// bubble sort derivative
		for (int i = 0; i < length; i++)
			for (int offset = 0; offset < length - i; offset++) {
				sen_x = resultList.get(i);
				sen_y = resultList.get(i + offset);
				index_buffer_x = sentenceList.indexOf(sen_x);
				index_buffer_y = sentenceList.indexOf(sen_y);
				// if the sentence order in sentenceList does not conform that
				// is in resultList, reverse it
				if (index_buffer_x > index_buffer_y) {
					resultList.set(i, sen_y);
					resultList.set(i + offset, sen_x);
				}
			}

		return resultList;
	}

	private List<String> pick_sentences(List<String> resultList, int max_length) {
		int length_counter = 0;
		int length_buffer;
		int length_jump;
		List<String> resultBuffer = new LinkedList<String>();
		for (int i = 0; i < resultList.size(); i++) {
			length_buffer = length_counter + resultList.get(i).length();
			if (length_buffer <= max_length) {
				resultBuffer.add(resultList.get(i));
				length_counter += resultList.get(i).length();
			} else if (i < (resultList.size() - 1)) {
				length_jump = length_counter + resultList.get(i + 1).length();
				if (length_jump <= max_length) {
					resultBuffer.add(resultList.get(i + 1));
					length_counter += resultList.get(i + 1).length();
					i++;
				}
			}
		}
		return resultBuffer;
	}

}
