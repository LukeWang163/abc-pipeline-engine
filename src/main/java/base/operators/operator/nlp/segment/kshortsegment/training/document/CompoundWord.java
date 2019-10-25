package base.operators.operator.nlp.segment.kshortsegment.training.document;

import base.operators.operator.nlp.segment.kshortsegment.training.Predefine;

import java.util.LinkedList;
import java.util.List;

public class CompoundWord implements IWord {
	
	public List<Word> innerList;

	public String label;

	private String shadowWord = null;
	
	private String word=null;

	@Override
	public String getValue() {
		return toWord().getValue();
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void setValue(String obj) {
		word=obj;
	}

	public void setLabel(String label) {
		this.label = label;
		shadowWord = compile(label);
	}

	private String compile(String tag) {
		if (tag.startsWith("m"))
			return Predefine.TAG_NUMBER;
		else if (tag.startsWith("nh"))
			return Predefine.TAG_PEOPLE;
		else if (tag.startsWith("ns"))
			return Predefine.TAG_PLACE;
		else if (tag.startsWith("ni"))
			return Predefine.TAG_ORG;
		else if (tag.startsWith("nt"))
			return Predefine.TAG_TIME;
		else if (tag.equals("w"))
			return Predefine.TAG_STRING;
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		int i = 1;
		for (Word word : innerList) {
			sb.append(word.toString());
			if (i != innerList.size()) {
				sb.append(' ');
			}
			++i;
		}
		sb.append("]/");
		sb.append(label);
		return sb.toString();
	}

	/**
	 * 转换为一个简单词
	 * 
	 * @return
	 */
	public Word toWord() {
		StringBuilder sb = new StringBuilder();
		for (Word word : innerList) {
			sb.append(word.value);
		}		
		return new Word(sb.toString(), getLabel());
	}

	public CompoundWord(List<Word> innerList, String label) {
		this.innerList = innerList;
		setLabel(label);
	}

	public static CompoundWord create(String param) {
		if (param == null)
			return null;
		int cutIndex = param.lastIndexOf('/');
		if (cutIndex <= 2 || cutIndex == param.length() - 1)
			return null;
		String wordParam = param.substring(1, cutIndex - 1);
		List<Word> wordList = new LinkedList<Word>();
		for (String single : wordParam.split(" ")) {
			if (single.length() == 0)
				continue;
			Word word = Word.create(single);
			if (word == null) {
				return null;
			}
			wordList.add(word);
		}
		String labelParam = param.substring(cutIndex + 1);
		return new CompoundWord(wordList, labelParam);
	}

	@Override
	public String getShadowWord() {
		return shadowWord;
	}

}
