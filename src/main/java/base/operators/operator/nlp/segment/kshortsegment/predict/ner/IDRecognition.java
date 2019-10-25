package base.operators.operator.nlp.segment.kshortsegment.predict.ner;


import base.operators.operator.nlp.segment.kshortsegment.predict.segment.IDExtract;
import base.operators.operator.nlp.segment.kshortsegment.predict.segment.Word;
import base.operators.operator.nlp.segment.kshortsegment.predict.segment.WordNet;
import base.operators.operator.nlp.segment.kshortsegment.predict.tagger.POS;

import java.util.Iterator;
import java.util.Map;

public class IDRecognition {
	public static boolean recognition( WordNet wordNet) {
		String sentence = wordNet.sentence;
		
		//银行卡
		Map<Integer, String> bankCard = IDExtract.getBankCard(sentence);
		if (bankCard.size() != 0) {
			addID(bankCard, POS.nzb, wordNet);
		}

		//车牌号
		Map<Integer, String> carNumber = IDExtract.getCarNum(sentence);
		if (carNumber.size() != 0) {
			addID(carNumber, POS.nzc, wordNet);
		}

		//URL
		Map<Integer, String> urls = IDExtract.getURL(sentence);
		if (urls.size() != 0) {
			addID(urls, POS.nzw, wordNet);
		}

		//QQ
		Map<Integer, String> qq = IDExtract.getQQ(sentence);
		if (qq.size() != 0) {
			addID(qq, POS.nzq, wordNet);
		}
		
		//IP
		Map<Integer, String> ip = IDExtract.getIPAddr(sentence);
		if (ip.size() != 0) {
			addID(ip, POS.nzn, wordNet);
		}

		//身份证号
		Map<Integer, String> idCard = IDExtract.getIDCard(sentence);
		if (idCard.size() != 0) {
			addID(idCard, POS.nzi, wordNet);
		}

		//移动电话
		Map<Integer, String> mobile = IDExtract.getMobile(sentence);
		if (mobile.size() != 0) {
			addID(mobile, POS.nzt, wordNet);
		}
		
		Map<Integer, String> tel = IDExtract.getTelNumber(sentence);
		if (tel.size() != 0) {
			addID(tel, POS.nzt, wordNet);
		}

		//电子邮箱
		Map<Integer, String> email = IDExtract.getEmail(sentence);
		if (email.size() != 0) {
			addID(email, POS.nze, wordNet);
		}

/*		//中文数字(移至NumberRecognition.java)
		Map<Integer, String> cnNum = IDExtract.getCNNum(sentence);
		if (cnNum.size() != 0) {
			addID(cnNum, POS.m, wordNetOptimum);
		}*/
		
		return true;
	}

	static void addID(Map<Integer, String> idMap, POS pos, WordNet wordNet) {
		Iterator it = idMap.keySet().iterator();
		while (it.hasNext()) {
			Integer loc = (Integer) it.next();
			String realWord = idMap.get(loc);
			int offset = -1;
			if (loc == 0) {
				offset = loc + 1;
			} else {
				if (!wordNet.hasExtendedCharset()) {
					offset = loc + 1;
				} else {
					String leftString = wordNet.sentence.substring(0, loc);
					offset = leftString.codePointCount(0, leftString.length()) + 1;
				}
			}
			double frq =20000;
			Word word = new Word(realWord, pos, offset, frq, 0);
			wordNet.add(offset, word);
		}

	}
}
