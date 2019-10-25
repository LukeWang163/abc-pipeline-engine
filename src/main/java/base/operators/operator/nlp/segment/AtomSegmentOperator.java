package base.operators.operator.nlp.segment;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.segment.atomsegment.IDExtract;
import base.operators.operator.nlp.segment.atomsegment.TextTools;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.tools.Ontology;

import java.util.*;

public class AtomSegmentOperator extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");


	public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
	public static final String IS_IDENTIFY = "identification_item";//"识别项";
	public static final String IS_URL = "identify_url";
	public static final String IS_TELEPHONE = "identify_telephone";
	public static final String IS_IDENTIFY_DATE = "identify_date";
	public static final String IS_LETTER_NUMBER = "identify_letter_number";
	public static final String IS_ID_CARD = "identify_id_card";
	public static final String IS_EMAIL = "identify_email";
	public static final String IS_BANK_CARD = "identify_bank_card";
	public static final String IS_IP_ADDRESS = "identify_ip_address";
	public static final String IS_CAR_NUMBER = "identifycar_number";
	public static final String IS_FRACTION = "identify_fraction";

	public static final String IS_MERGE = "merge_item";//"合并项";
	public static final String IS_CHINESE_NUMBER = "merge_chinese_number";
	public static final String IS_ARABIC_NUMBER = "merge_arabic_number";
	public static final String IS_MERGE_DATE = "merge_date";

	public static final String WILDCARD_REPLACE = "wildcard_replace";//"通配符替换";
	public static final String REPLACE_STRAT = "replace the beginning of the substitution sentence with '始##始'";//"句子的开始替换为\"始##始\"";
	public static final String REPLACE_END = "replace the end of the substitution sentence with '末##末'";//"结束替换为\"末##末\"";
	public static final String REPLACE_NUMERAL = "replace numerals with '未##数'";//"数词替换为\"未##数\"";
	public static final String REPLACE_TIME = "replace time with '未##时'";//"时间替换为\"未##时\"";
	public static final String REPLACE_STRING = "replace string with '未##串'";//"字符串替换为\"未##串\"";

	public static final String FILTER_NUMBER ="words that filter word segmentation results into Numbers";//"过滤分词结果为数字的词";
	public static final String FILTER_ENGLISH ="words that filter word segmentation results into English";//"过滤分词结果为全英文的词";
	public static final String FILTER_PUNCTUATION ="words that filter word segmentation results into Punctuation";//"过滤分词结果为标点符号的词";

	public AtomSegmentOperator(OperatorDescription description){
		super(description);
		exampleSetInput.addPrecondition(
				new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
						this, DOC_ATTRIBUTE_NAME)));

	}

	public void doWork() throws OperatorException {
		String selected_column = getParameterAsString(DOC_ATTRIBUTE_NAME);
		//获取数据表
		ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
		ExampleTable table = exampleSet.getExampleTable();
		Attribute newAttribute = AttributeFactory.createAttribute(selected_column+"_atom", Ontology.STRING);
		table.addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);

		for(Example example : exampleSet){
			// 原子分词并将结果添加至新字段中
			String text = example.getValueAsString(exampleSet.getAttributes().get(selected_column));
			if(text != null && !"".equals(text)) {
				text = atomSegment(text);
			} else {
				text = "";
			}
			example.setValue(newAttribute, newAttribute.getMapping().mapString(text));
		}

		exampleSetOutput.deliver(exampleSet);
	}

	/**
	 * 检测句子中是否含有Unicode扩展字符集
	 */
	private static boolean testExtendedCharset(String sentence) {
		if (sentence == null)
			return false;
		boolean hasExtendedCharset = false;
		if (sentence.length() != sentence.codePointCount(0, sentence.length())) {
			hasExtendedCharset = true;
		}
		return hasExtendedCharset;
	}
	
	public static LinkedList<Map<String, String>>[] atom(String sentence) {
		int length = sentence.codePointCount(0, sentence.length());
		LinkedList<Map<String, String>> words[] = new LinkedList[length + 2];
		for (int i = 0; i < words.length; ++i) {
			words[i] = new LinkedList<Map<String, String>>();
		}
		Map<String, String> begin = new HashMap<String, String>();
		begin.put("start", "0");
		begin.put("length", "1");
		begin.put("realWord", " ");
		words[0].add(begin);
		Map<String, String> end = new HashMap<String, String>();
		end.put("start", words.length - 1 + "");
		end.put("length", "1");
		end.put("realWord", " ");
		words[words.length - 1].add(end);
		return words;
	}
	
	/**
	 * 添加词
	 *
	 * @param line
	 *            行号
	 * @param word
	 *            词
	 */
	private static void add(int line, Map<String, String> word, List<Map<String, String>>[] words) {
		for (Map<String, String> oldWord : words[line]) {
			// 保证唯一性
			if (oldWord.get("realWord").length() == word.get("realWord").length()){
				oldWord.put("pos", word.get("pos"));
				return;
			}				
		}
		words[line].add(word);
	}
	
	/**
	 * 添加词
	 * 
	 * @param wordList
	 */
	private static void addAll(List<Map<String, String>> wordList, LinkedList<Map<String, String>>[] words) {
		for (Map<String, String> word : wordList) {		
			int offset = Integer.parseInt(word.get("start"));
			add(offset, word, words);
		}

	}
	
	private static void pushWord(Map<Integer, String> wordMap, String pos, LinkedList<Map<String, String>>[] words) {
		Iterator it = wordMap.keySet().iterator();
		List<Map<String, String>> wordList = new LinkedList<Map<String, String>>();
		while (it.hasNext()) {
			Integer offset = (Integer) it.next();
			String realWord = wordMap.get(offset);
			Map<String, String> wordItem = new HashMap<String, String>();
			wordItem.put("realWord", realWord);
			wordItem.put("pos", pos);
			wordItem.put("start", offset+1 + "");
			wordItem.put("length", realWord.codePointCount(0, realWord.length()) + "");
			wordList.add(wordItem);
		}
		addAll(wordList, words);
	}
	
	private String atomSegment(String sentence) {
		LinkedList<Map<String, String>> words[] = atom(sentence);
		Map<Integer, String> wordMap = new HashMap<Integer, String>();
		if(getParameterAsBoolean(IS_IDENTIFY)==true) {
			if(getParameterAsBoolean(IS_URL)==true) {
				wordMap = IDExtract.getURL(sentence);
				pushWord(wordMap, "nzw", words);
			}
			if(getParameterAsBoolean(IS_TELEPHONE)==true) {
				wordMap = IDExtract.getMobile(sentence);
				pushWord(wordMap, "nzt", words);
			}
			if(getParameterAsBoolean(IS_IDENTIFY_DATE)==true) {
				wordMap = IDExtract.getCNTime(sentence);
				pushWord(wordMap, "nt", words);
			}
			
			if(getParameterAsBoolean(IS_ID_CARD)==true) {
				wordMap = IDExtract.getIDCard(sentence);
				pushWord(wordMap, "nzi", words);
			}
			if(getParameterAsBoolean(IS_EMAIL)==true) {
				wordMap = IDExtract.getEmail(sentence);
				pushWord(wordMap, "nze", words);
			}
			if(getParameterAsBoolean(IS_BANK_CARD)==true) {
				wordMap = IDExtract.getBankCard(sentence);
				pushWord(wordMap, "nzb", words);
			}
			if(getParameterAsBoolean(IS_IP_ADDRESS)==true) {
				wordMap = IDExtract.getIPAddr(sentence);
				pushWord(wordMap, "nzn", words);
			}
			if(getParameterAsBoolean(IS_CAR_NUMBER)==true) {
				wordMap = IDExtract.getCarNum(sentence);
				pushWord(wordMap, "nzc", words);
			}
			if(getParameterAsBoolean(IS_FRACTION)==true) {
				wordMap = IDExtract.getPerNum(sentence);
				pushWord(wordMap, "mf", words);
			}
		}
		if(getParameterAsBoolean(IS_MERGE)==true) {
			if(getParameterAsBoolean(IS_CHINESE_NUMBER)==true) {
				wordMap = IDExtract.getCNNum(sentence);
				pushWord(wordMap, "m", words);
			}
			if(getParameterAsBoolean(IS_ARABIC_NUMBER)==true) {
				wordMap = IDExtract.getNumbers(sentence);
				pushWord(wordMap, "m", words);
			}
			if(getParameterAsBoolean(IS_MERGE_DATE)==true) {
				wordMap = IDExtract.getNumbers(sentence);
				pushWord(wordMap, "m", words);
				wordMap = IDExtract.getLetters(sentence);
				pushWord(wordMap, "ws", words);
			}
		}
		
		pushAtomWord(words, sentence);
		
		StringBuilder sb = new StringBuilder();
		if(getParameterAsBoolean(WILDCARD_REPLACE)==true && getParameterAsBoolean(REPLACE_STRAT)==true) {
			sb.append("始##始 ");
		}
		for (int i = 1; i < words.length - 1;) {
			LinkedList<Map<String, String>> wordList = words[i];
			int index = -1;
			int maxLength = 0;
			// 获取以当前字符未开始的
			for(int j=0; j<wordList.size(); j++) {
				Map<String, String> word = wordList.get(j);
				String realWord = word.get("realWord");

				if(realWord.length() > maxLength) {
					maxLength = realWord.length();
					index = j;
				}
			}
			if(index >= 0) {
				Map<String, String> word = wordList.get(index);
				String pos = word.get("pos");
				String realWord = word.get("realWord");

				if(getParameterAsBoolean(WILDCARD_REPLACE)==true && getParameterAsBoolean(REPLACE_NUMERAL)==true && pos.startsWith("m")) {
					realWord = "未##数";
				}
				if(getParameterAsBoolean(WILDCARD_REPLACE)==true && getParameterAsBoolean(REPLACE_TIME)==true && pos.equals("nt")) {
					realWord = "未##时";
				}
				if(getParameterAsBoolean(WILDCARD_REPLACE)==true && getParameterAsBoolean(REPLACE_STRING)==true && pos.equals("ws")) {
					realWord = "未##串";
				}
				if(!(getParameterAsBoolean(WILDCARD_REPLACE)==true && getParameterAsBoolean(FILTER_NUMBER)==true && pos.startsWith("m") && !pos.startsWith("mq") || getParameterAsBoolean(WILDCARD_REPLACE)==true && getParameterAsBoolean(FILTER_ENGLISH)==true && pos.equals("ws") || getParameterAsBoolean(WILDCARD_REPLACE)==true && getParameterAsBoolean(FILTER_PUNCTUATION)==true && pos.equals("wp"))) {
					sb.append(realWord +  " ");
				}
				i = i + realWord.length();
			} else {
				i++;
			}
		}
		if(getParameterAsBoolean(WILDCARD_REPLACE)==true && getParameterAsBoolean(REPLACE_END)==true) {
			sb.append("末##末");
		}
		return sb.toString();
	}
	
	/**
	 * 对去除基于规则所识别出的内容后的其他文本进行原子分词
	 * @param words
	 * @param sentence
	 */
	private static void pushAtomWord(LinkedList<Map<String, String>> words[], String sentence) {
		List<Map<String, String>> wordList = new LinkedList<Map<String, String>>();
		if (!testExtendedCharset(sentence)) {

			for (int i = 1; i < words.length;) {
				if (words[i].isEmpty()) {
					int j = i;
					for (; j < words.length - 1; ++j) {
						if (!words[j].isEmpty())
							break;
						char c = sentence.charAt(j - 1);
						String realWord = c + "";
						String posTag = TextTools.testCharType(c);
						Map<String, String> word = new HashMap<String, String>();
						word.put("realWord", realWord);
						word.put("pos", posTag);
						word.put("start", j + "");
						word.put("length", 1 + "");
						wordList.add(word);
					}

					i = j;
				} else {
					LinkedList<Map<String, String>> list = words[i];
					int index = -1;
					int maxLength = 0;
					for(int k=0; k<list.size(); k++) {
						Map<String, String> word = list.get(k);
						String realWord = word.get("realWord");
						
						if(realWord.length() > maxLength) {
							maxLength = realWord.length();
							index = k;
						}
					}
					i += Integer.parseInt(words[i].get(index).get("length"));
				}
					
			}

		} else {
			for (int i = 1; i < words.length;) {
				if (words[i].isEmpty()) {
					int j = i;
					for (; j < words.length - 1; ++j) {
						if (!words[j].isEmpty())
							break;
						int index = sentence.offsetByCodePoints(0, j - 1);
						int cpp = sentence.codePointAt(index);
						String posTag = TextTools.testCharType(cpp);
						String realWord = new String(Character.toChars(cpp));
						Map<String, String> wordItem = new HashMap<String, String>();
						wordItem.put("realWord", realWord);
						wordItem.put("pos", posTag);
						wordItem.put("start", j + "");
						wordItem.put("length", 1 + "");
						wordList.add(wordItem);
					}

					i = j;
				} else {
					LinkedList<Map<String, String>> list = words[i];
					int index = -1;
					int maxLength = 0;
					for(int k=0; k<list.size(); k++) {
						Map<String, String> word = list.get(k);
						String realWord = word.get("realWord");
						
						if(realWord.length() > maxLength) {
							maxLength = realWord.length();
							index = k;
						}
					}
					i += Integer.parseInt(words[i].get(index).get("length"));
				}
			}
		}
		addAll(wordList, words);
	}


	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput, false));
		types.add(new ParameterTypeBoolean(IS_IDENTIFY, "Is there an identifier?",false, false));

		ParameterType type = new ParameterTypeBoolean(IS_URL, "Whether to identify URLs.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(IS_TELEPHONE, "Whether to identify telephone.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(IS_IDENTIFY_DATE, "Whether to identify date.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(IS_LETTER_NUMBER, "Whether to identify letter number.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(IS_ID_CARD, "Whether to identify id card.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(IS_EMAIL, "Whether to identify email.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(IS_BANK_CARD, "Whether to identify bank card.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(IS_IP_ADDRESS, "Whether to identify ip number.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(IS_CAR_NUMBER, "Whether to identify car number.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(IS_FRACTION, "Whether to identify fraction.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_IDENTIFY, false, true));
		types.add(type);

		types.add(new ParameterTypeBoolean(IS_MERGE, "Is there an merge?",false, false));
		type = new ParameterTypeBoolean(IS_CHINESE_NUMBER, "Whether to merge chinese number.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_MERGE, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(IS_ARABIC_NUMBER, "Whether to merge arabic number.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_MERGE, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(IS_MERGE_DATE, "Whether to merge date.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, IS_MERGE, false, true));
		types.add(type);

		types.add(new ParameterTypeBoolean(WILDCARD_REPLACE, "Is there an wildcard replacement?",false, false));
		type = new ParameterTypeBoolean(REPLACE_STRAT, "Whether to replace start.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(REPLACE_END, "Whether to merge replace ending.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(REPLACE_TIME, "Whether to replace time.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(REPLACE_NUMERAL, "Whether to replace number.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(REPLACE_STRING, "Whether to replace string.", false, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, WILDCARD_REPLACE, false, true));
		types.add(type);

		types.add(new ParameterTypeBoolean(FILTER_ENGLISH, "Whether to filter english or not?",false, false));
		types.add(new ParameterTypeBoolean(FILTER_NUMBER, "Whether to filter number or not?",false, false));
		types.add(new ParameterTypeBoolean(FILTER_PUNCTUATION, "Whether to filter punctuation or not?",false, false));

		return types;
	}
	
}
