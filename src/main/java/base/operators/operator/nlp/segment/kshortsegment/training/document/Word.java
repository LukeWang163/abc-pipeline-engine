package base.operators.operator.nlp.segment.kshortsegment.training.document;

import base.operators.operator.nlp.segment.kshortsegment.training.Predefine;

/**
 * 一个单词
 */
public class Word implements IWord {
	/**
	 * 单词的真实值，比如“程序”
	 */
	public String value;
	/**
	 * 单词的标签，比如“n”
	 */
	public String label;
	
	private String shadowWord=null;

	@Override
	public String toString() {
		return value + '/' + label;
	}

	public Word(String value, String label) {
		this.value = value;
		setLabel(label);
	}

	/**
	 * 通过参数构造一个单词
	 * 
	 * @param param
	 *            比如 人民网/nz
	 * @return 一个单词
	 */
	public static Word create(String param) {
		if (param == null)
			return null;
		int cutIndex = param.lastIndexOf('/');
		if (cutIndex <= 0 || cutIndex == param.length() - 1) {
			return null;
		}

		return new Word(param.substring(0, cutIndex), param.substring(cutIndex + 1));
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void setLabel(String label) {
		this.label = label;
		shadowWord=compile(label);
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
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String getShadowWord() {
		return shadowWord;
	}

}
