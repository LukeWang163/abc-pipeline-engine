package base.operators.operator.nlp.sensitivity;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import idsw.nlp.read.ReadFileAsStream;

import java.io.*;
import java.util.*;

public class Sensitivity {
	private static volatile Sensitivity defaultInstance;
	
	public static List<String> sensorDict = new ArrayList<String>();
	//private String sensorPath = "/nlp/sensitivity/dict/sensorWords.txt";
	private static DoubleArrayTrie dat = new DoubleArrayTrie();
	
	//定义一个共有的静态方法，返回该类型实例
	public static Sensitivity getInstance(int type, ExampleSet exampleSet, Attribute attribute, boolean hasPos) {
		// 对象实例化时与否判断（不使用同步代码块，instance不等于null时，直接返回对象，提高运行效率）
		if (defaultInstance == null) {
			//同步代码块（对象未初始化时，使用同步代码块，保证多线程访问时对象在第一次创建后，不再重复被创建）
			synchronized (Sensitivity.class) {
				//未初始化，则初始instance变量
				if (defaultInstance == null) {
					defaultInstance = new Sensitivity(type, exampleSet, attribute, hasPos);
				}
			}
		}
		return defaultInstance;
	}
	
	private Sensitivity(int type, ExampleSet exampleSet, Attribute attribute, boolean hasPos) {
		initSenDict(type, exampleSet, attribute, hasPos);
	}
	
	/** 
     *   初始化敏感词库 
     */  
    private void initSenDict(int type, ExampleSet exampleSet, Attribute attribute, boolean hasPos) {
    	if(type == 0 || type == 2) {
    		//加载词库  
            InputStream inStream = ReadFileAsStream.readSensitivityDict();
            BufferedReader bufferedReader = null;  
            try {  
                bufferedReader = new BufferedReader(new InputStreamReader(inStream,"UTF-8"));  
                String row;
    			while ((row = bufferedReader.readLine()) != null) {
    				 if(!sensorDict.contains(row) && !("").equals(row))  
    	                sensorDict.add(row); 
    			}
            } catch (UnsupportedEncodingException e) {  
                e.printStackTrace();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }finally{  
                try {  
                    if(null != bufferedReader)  
                    	bufferedReader.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            } 
    	} else if((type == 1 || type == 2) && (attribute!=null && exampleSet != null)) {
    		for(Example example: exampleSet) {
    			String word = example.getValueAsString(attribute);
    			if(!sensorDict.contains(word) && !("").equals(word))  
	                sensorDict.add(word); 
    		}
    	}
        
        // 初始化DAT
    	if(hasPos == false) {
    		Collections.sort(sensorDict);
    		dat.build(sensorDict);
    	}
    }
    //敏感词检测
    public String detect(String word) {
    	if(sensorDict.contains(word)) {
    		return "true";
    	} else {
    		return "false";
    	}
    }
    // 文本过滤
    public Map<String, Object> scan(String text) {
    	Map<String, Object> result = new HashMap<String, Object>();
    	List<String> sensorWords = new ArrayList<String>();
    	List<String> wordList = new ArrayList<String>();
    	String[] words = text.split(" ");
    	int index = 0;
    	String transText = "";
    	for(int i=0; i<words.length; i++) {
    		String word = words[i];
    		if(word != null && !word.trim().equals("")) {
    			if(sensorDict.contains(word)) {
        			String temp = word + " " + index + " " + (index + word.length());
        			wordList.add(word);
        			sensorWords.add(temp);
        			for(int j=0; j<word.length(); j++) {
        				transText = transText + "*";
        			}
        		} else {
        			transText = transText + word;
        		}
        		index = index + word.length();
    		}
    	}
    	result.put("count", wordList.size() + "");
    	result.put("text", transText);
    	result.put("words", String.join("##", sensorWords));
    	return result;
    }
    /**
	 * 检测句子中是否含有Unicode扩展字符集
	 */
	private Map<String, Object> testExtendedCharset(String sentence) {
		Map<String, Object> result = new HashMap<String, Object>();
		int codePointCount = 0;
		String hasExtendedCharset = "false";
		if (sentence == null)
			return null;
		hasExtendedCharset = "false";
		codePointCount = sentence.length();
		if (sentence.length() != sentence.codePointCount(0, sentence.length())) {
			hasExtendedCharset = "true";
			codePointCount = sentence.codePointCount(0, sentence.length());
		}
		result.put("length", codePointCount);
		result.put("hasExtendedCharset", hasExtendedCharset);
		return result;
	}

	private String getSubString(String sentence, int codePointCount, int offset) {
		StringBuilder sb = new StringBuilder();
		for (int i = offset; i < codePointCount; i++) {
			int index = sentence.offsetByCodePoints(0, i);
			int cpp = sentence.codePointAt(index);
			sb.appendCodePoint(cpp);
		}
		return sb.toString();
	}
	/**
	 * 最大机械分词
	 * @param text
	 */
	private List<String> generateMaxWordNet(String text) {
		Map<String, Object> map = testExtendedCharset(text);
		int length = (Integer) map.get("length");

		String hasExtendedCharset = (String) map.get("hasExtendedCharset");
		List<Integer> commonPrefixList = new ArrayList<Integer>();
		List<String> wordList = new LinkedList<String>();
		String subString = null;
		int i = 0;
		int j = 0;
		String numTemp = "";
		String strTemp = "";
		String regex_str = "[A-Za-z]+";
		String regex_num = "(\\+|\\-)?\\d+(\\.\\d+)?";// 正负整数、正负浮点数    (+|-)13456(.12345)
		while(i < length) {
			if (hasExtendedCharset.equals("true")) {
				subString = getSubString(text, length, i);
			} else {
				subString = text.substring(i);
			}
			commonPrefixList = dat.commonPrefixSearch(
					subString);// 判断是否有前缀（判断该字符串是否有根节点）
			int sizePrefix = commonPrefixList.size();// 前缀个数
			if (sizePrefix > 0) {
				int wordIndex = commonPrefixList.get(sizePrefix-1);
				String realWord = sensorDict.get(wordIndex);
				String regex = "[\u4E00-\u9FA5]+";
				if(realWord.matches(regex)){
					if(!"".equals(numTemp)) {
						wordList.add(numTemp);
					}
					if(!"".equals(strTemp)) {
						wordList.add(strTemp);
					}
					wordList.add(realWord);
					strTemp = numTemp = "";
				} else {
					if(realWord.matches(regex_str)) {
						if(!"".equals(numTemp)) {
							wordList.add(numTemp);
						}
						numTemp = "";
						strTemp = strTemp + realWord;
					} else if(realWord.matches(regex_num)) {
						if(!"".equals(strTemp)) {
							wordList.add(strTemp);
						}
						strTemp = "";
						numTemp = numTemp + realWord;
					} else {
						if(!"".equals(numTemp)) {
							wordList.add(numTemp);
						}
						if(!"".equals(strTemp)) {
							wordList.add(strTemp);
						}
						if(!"".equals(realWord)) {
							wordList.add(realWord);
						}
						strTemp = numTemp = "";
					}
					
				}
				i = i + realWord.length();
				j = i;
				
			} else {
				//分数字、英文、其他
				String str = text.substring(i, i+1);
				if(str.matches(regex_str)) {
					if(!"".equals(numTemp)) {
						wordList.add(numTemp);
					}
					numTemp = "";
					strTemp = strTemp + str;
				} else if(str.matches(regex_num)) {
					if(!"".equals(strTemp)) {
						wordList.add(strTemp);
					}
					strTemp = "";
					numTemp = numTemp + str;
				} else {
					if(!"".equals(numTemp)) {
						wordList.add(numTemp);
					}
					if(!"".equals(strTemp)) {
						wordList.add(strTemp);
					}
					wordList.add(str);
					strTemp = numTemp = "";
				}
				i++;
			}
		}
		if(!"".equals(strTemp)) {
			wordList.add(strTemp);
		}
		if(!"".equals(numTemp)) {
			wordList.add(numTemp);
		}
		return wordList;
		// System.out.println("======generateGraph start:");
		// System.out.println("======generateGraph addAall end");
		// System.out.println("======generateGraph annotateNER end wordNet："+wordNet);
	}
    public Map<String, Object> scanByDat(String text) {
    	List<String> segWords = generateMaxWordNet(text);
		return scan(String.join(" ", segWords));
    }
}
