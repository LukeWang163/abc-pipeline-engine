package base.operators.operator.nlp.segment.kshortsegment.predict.ner;

import base.operators.operator.nlp.segment.kshortsegment.predict.segment.IDExtract;
import base.operators.operator.nlp.segment.kshortsegment.predict.segment.Word;
import base.operators.operator.nlp.segment.kshortsegment.predict.segment.WordNet;
import base.operators.operator.nlp.segment.kshortsegment.predict.tagger.POS;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NumberRecognition {
	public static boolean recognition( WordNet wordNetOptimum) {
		String sentence = wordNetOptimum.sentence;
		
		//中文数字
		Map<Integer,String> cnnumMap = new HashMap<Integer,String>();
		cnnumMap = IDExtract.getCNNum(sentence);
		if(cnnumMap.size()>=0){
			addNum(cnnumMap, POS.m,wordNetOptimum);
		}
		
		//分数 百分数 千分数
		Map<Integer,String> pernumMap = IDExtract.getPerNum(sentence);
		if(pernumMap.size()>=0){
			addNum(pernumMap,POS.m,wordNetOptimum);
		}

		return true;
	}
	
	protected static void addNum(Map<Integer, String> idMap, POS pos, WordNet wordNetOptimum) {
		Iterator it = idMap.keySet().iterator();
		while (it.hasNext()) {
			Integer loc = (Integer) it.next();
			String realWord = idMap.get(loc);
			int offset = -1;
			if (loc == 0) {
				offset = loc + 1;
			} else {
				if (!wordNetOptimum.hasExtendedCharset()) {
					offset = loc + 1;
				} else {
					String leftString = wordNetOptimum.sentence.substring(0, loc);
					offset = leftString.codePointCount(0, leftString.length()) + 1;
				}
			}
			double frq = 20000;
			Word word = new Word(realWord, pos, offset, frq, 0);
			wordNetOptimum.add(offset, word);
		}

	}
}
