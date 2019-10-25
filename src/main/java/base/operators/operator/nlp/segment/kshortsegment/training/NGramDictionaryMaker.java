package base.operators.operator.nlp.segment.kshortsegment.training;

import base.operators.operator.nlp.segment.kshortsegment.training.document.IWord;

/**
 * 2-gram词典制作工具
 *
 */
public class NGramDictionaryMaker {
	public BinTrie<Integer> trie;
	/**
	 * 转移矩阵
	 */
	public TMDictionaryMaker tmDictionaryMaker;

	public NGramDictionaryMaker() {
		trie = new BinTrie<Integer>();
		tmDictionaryMaker = new TMDictionaryMaker();
	}

	public void addPair(IWord first, IWord second) {
		String combine = first.getValue() + "\t" + second.getValue();
		Integer frequency = trie.get(combine);
		if (frequency == null)
			frequency = 0;
		trie.put(combine, frequency + 1);
		//统计两个词间不同词性的转移概率
		trie.incrementTransCount(combine, first.getLabel(), second.getLabel());
		//处理虚拟词计数（人名、地名、机构名、数字、字符串、头、尾等）
		if(first.getShadowWord()!=null || second.getShadowWord()!=null){
			String srcTxt=first.getShadowWord()!=null?first.getShadowWord():first.getValue();
			String destTxt=second.getShadowWord()!=null?second.getShadowWord():second.getValue();
			combine=srcTxt+"\t" + destTxt;
			trie.incrementTransCount(combine, first.getLabel(), second.getLabel());
		}

		if (first.getShadowWord() != null) {
			String comL = first.getShadowWord() + "\t" + second.getValue();
			Integer frqL = trie.get(comL);
			if (frqL == null)
				frqL = 0;
			trie.put(comL, frqL + 1);

			if (second.getShadowWord() != null) {
				String comR = first.getShadowWord() + "\t" + second.getShadowWord();
				Integer frqR = trie.get(comR);
				if (frqR == null)
					frqR = 0;
				trie.put(comR, frqR + 1);
			}
		} else {
			if (second.getShadowWord() != null) {
				String comL = first.getValue() + "\t" + second.getShadowWord();
				Integer frqL = trie.get(comL);
				if (frqL == null)
					frqL = 0;
				trie.put(comL, frqL + 1);
			}

		}
		// 同时还要统计标签的转移情况
		tmDictionaryMaker.addPair(first.getLabel(), second.getLabel());
	}
}
