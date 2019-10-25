package base.operators.operator.nlp.segment.kshortsegment.predict.ner;


import base.operators.operator.nlp.segment.kshortsegment.predict.segment.Word;
import base.operators.operator.nlp.segment.kshortsegment.predict.segment.WordNet;
import base.operators.operator.nlp.segment.kshortsegment.predict.tagger.POS;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import nlp.annotation.time.XmlParser;

public class TimeAnnotation {
//	public static void annotate(WordNet wordNet){
//		TimeAnnotate normalizer = new TimeAnnotate();
//		normalizer.setPreferFuture(true);
//		if(wordNet.sentence.startsWith("红桥区 停电时间:06-12 06:00-19:00")){
//			System.out.println();
//		}
//		try {
//			normalizer.parse(wordNet.sentence);
//		}catch (Exception e){
//			System.out.println(wordNet.sentence);
//		}
//        ArrayList<TimeUnit> unit = normalizer.getTimeUnit();
//		for(int i=0;i<unit.size();i++){
//			TimeUnit thisUnit=unit.get(i);
//			TreeMap<POS, Double> posMap=new TreeMap();
//			double frq=20000;
//			posMap.put(POS.nt, frq);
//			Word newWord = new Word(thisUnit.getTimeExpression(), posMap, thisUnit.getStartIndex() + 1, 0);
//			newWord.setRefObject(thisUnit);
//			wordNet.add(thisUnit.getStartIndex() + 1, newWord);
//		}
//	}

	private static Pattern patterns = null;
	public TimeAnnotation() {
		if (patterns == null) {
			try {
				patterns = initPattern();
			} catch (Exception e) {
				e.printStackTrace();
				System.err.print("Read model error!");
			}
		}
	}
	/**
	 * 初始化Pattern ， 表达式从TimeExp.xml配置文件里读取
	 *
	 * @return add by Binson
	 */
	private Pattern initPattern() {
		// String regex = ConfigUtil.getString(ConfigUtil.TIME_EXP);
		List<String> ruleList = XmlParser.getTimePatterns();

		// 构造完整的正则表达式
		String regex = "";
		for (String rule : ruleList) {
			regex = regex + rule + "|";
		}

		// 删除最后一个“|”
		regex = regex.substring(0, regex.length() - 1);

		return Pattern.compile(regex);
	}


	//新的方法，不使用nlp-time
	public void annotate2(WordNet wordNet){

		String sentence = wordNet.sentence;
		Map<Integer, String> map = new TreeMap<Integer, String>();
		Matcher match = patterns.matcher(sentence);
		int oldStart = -1;
		int newStart = -1;
		int end = -1;
		while (match.find()){
			if(oldStart == -1){
				oldStart = match.start();
			}
			if(end == -1){
				end = match.end();
				continue;
			}
			newStart = match.start();
			if(end == newStart){
				end = match.end();
				continue;
			}else {
				add(oldStart, end, wordNet);
				oldStart = match.start();
				end = match.end();
			}
		}
		if(oldStart != -1){
			add(oldStart, end, wordNet);
		}


	}

	//新的方法，不使用nlp-time
	public void testAnnotate2(String sentence){

		Map<Integer, String> map = new TreeMap<Integer, String>();
		Matcher match = patterns.matcher(sentence);
		int oldStart = -1;
		int newStart = -1;
		int end = -1;
		while (match.find()){
			System.out.println(match.group());
			if(oldStart == -1){
				oldStart = match.start();
			}

			if(end == -1){
				end = match.end();
				continue;
			}
			newStart = match.start();
			if(end == newStart){
				end = match.end();
				continue;
			}else {
	//			add(oldStart, end, wordNet);
				System.out.println(":::"+sentence.substring(oldStart, end));
				oldStart = match.start();
				end = match.end();
			}
		}
//			add(oldStart, end, wordNet);
		System.out.println(":::"+sentence.substring(oldStart, end));

	}

	private void add(int start, int end, WordNet wordNet){

		int offset;
		String sentence = wordNet.sentence;
		if(start == 0){
			offset = 1;
		}else {
			if (!wordNet.hasExtendedCharset()) {
				offset = start + 1;
			} else {
				String leftString = sentence.substring(0, start);
				offset = leftString.codePointCount(0, leftString.length()) + 1;
			}
		}

	double frq = 20000;
	Word word = new Word(sentence.substring(start, end), POS.nt, offset, frq, 0);
	wordNet.add(offset, word);
}

	public static void main(String[] args) {
		String s = "7点40分，他说昨天8点50贵了前天哈哈哈";
		new TimeAnnotation().testAnnotate2(s);


	}

}
