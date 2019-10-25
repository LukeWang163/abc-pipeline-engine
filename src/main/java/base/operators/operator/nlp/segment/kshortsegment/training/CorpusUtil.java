package base.operators.operator.nlp.segment.kshortsegment.training;

import base.operators.operator.nlp.segment.kshortsegment.training.document.CompoundWord;
import base.operators.operator.nlp.segment.kshortsegment.training.document.IWord;
import base.operators.operator.nlp.segment.kshortsegment.training.document.Word;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class CorpusUtil {
	/**
	 * 将word列表转为兼容的IWord列表
	 *
	 * @param simpleSentenceList
	 * @return
	 */
	public static List<List<IWord>> convert2CompatibleList(List<List<Word>> simpleSentenceList) {
		List<List<IWord>> compatibleList = new LinkedList<List<IWord>>();
		for (List<Word> wordList : simpleSentenceList) {
			compatibleList.add(new LinkedList<IWord>(wordList));
		}
		return compatibleList;
	}

	public static List<IWord> spilt(List<IWord> wordList) {
		ListIterator<IWord> listIterator = wordList.listIterator();
		while (listIterator.hasNext()) {
			IWord word = listIterator.next();
			if (word instanceof CompoundWord) {
				listIterator.remove();
				for (Word inner : ((CompoundWord) word).innerList) {
					listIterator.add(inner);
				}
			}
		}
		return wordList;
	}
}
