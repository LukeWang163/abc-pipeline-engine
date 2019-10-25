package base.operators.operator.nlp.segment.kshortsegment.predict.segment;


import base.operators.operator.nlp.segment.kshortsegment.predict.tagger.POS;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WordNet {
	/**
	 * 节点，每一行都是前缀词
	 */
	private LinkedList<Word> words[];

	private int currentVertexNum = 0;

	/**
	 * 节点数目
	 */
	int size;

	private int codePointCount;
	/**
	 * 句子中是否包含Unicode扩展字符集
	 */
	private boolean hasExtendedCharset = false;
	/**
	 * 句子中是否有字符标志
	 */
	private boolean hasLetter=false;
	/**
	 * 句子中是否有数字标志
	 */
	private boolean hasNumber=false;
	//+++++++++++++++++++++++++++++++++++++++
//	/**
//	 * 句子中是否有时间标志
//	 */
//	private boolean hasTime=false;
//	/**
//	 * 句子中是否有邮箱标志
//	 */
//	private boolean hasEmail=false;
//	/**
//	 * 句子中是否有车牌号标志
//	 */
//	private boolean hasCarNum=false;
//	/**
//	 * 句子中是否有身份证标志
//	 */
//	private boolean hasIDCard=false;
//	/**
//	 * 句子中是否有电话号码标志
//	 */
//	private boolean hasPhoneNum=false;
//	/**
//	 * 句子中是否有银行卡标志
//	 */
//	private boolean hasBankCard=false;
	//+++++++++++++++++++++++++++++++++++++++

	/**
	 * 原始句子
	 *
	 */
	public String sentence;

	public WordNet(String sentence) {
		this.sentence = sentence;
		int length = sentence.codePointCount(0, sentence.length());
		words = new LinkedList[length + 2];
		for (int i = 0; i < words.length; ++i) {
			words[i] = new LinkedList<Word>();
		}
		Word begin = Word.newB();
		begin.setId(generateWordId());
		begin.confirmPOS(POS.begin);
		words[0].add(begin);
		Word end = Word.newE(words.length - 1);
		end.setId(generateWordId());
		end.confirmPOS(POS.end);
		words[words.length - 1].add(end);
		size = 2;
		testExtendedCharset();
	}

	public WordNet(String sentence, LinkedList<Word> words[], int size) {
		this.sentence = sentence;
		this.words = words;
		this.size = size;
		testExtendedCharset();
	}

	/**
	 * 检测句子中是否含有Unicode扩展字符集
	 */
	private void testExtendedCharset() {
		if (sentence == null)
			return;
		hasExtendedCharset = false;
		codePointCount = sentence.length();
		if (sentence.length() != sentence.codePointCount(0, sentence.length())) {
			hasExtendedCharset = true;
			codePointCount = sentence.codePointCount(0, sentence.length());
		}
		
	}

	private int generateWordId() {
		currentVertexNum=currentVertexNum+30;//为一词多词性预留空间
		return currentVertexNum;
	}

	public int getCodePointCount() {
		return codePointCount;
	}

	/**
	 * 对象克隆
	 * 
	 * @return
	 */
	public WordNet cloneWordNet() {
		return new WordNet(sentence, words, size);
	}

	/**
	 * 添加词
	 *
	 * @param line
	 *            行号
	 * @param word
	 *            词
	 */
	public void add(int line, Word word) {
		for (Word oldWord : words[line]) {
			// 保证唯一性
			if (oldWord.getRealWord().length() == word.getRealWord().length()){
				oldWord.setFrequency(word.getFrequency());
				oldWord.setVector(word.getVector());
				oldWord.addPos(word.getPosMap());
				return;
			}				
		}
		word.setId(generateWordId());
		words[line].add(word);
		++size;
	}

	/**
	 * 添加词
	 * 
	 * @param wordList
	 */
	public void addAll(List<Word> wordList) {
		for (Word word : wordList) {		
			int offset = word.getOffset();
			word.setId(generateWordId());
			add(offset, word);
		}

	}

	/**
	 * 强行添加，替换已有的词
	 *
	 * @param line
	 *            行号
	 * @param word
	 *            词
	 */
	public void push(int line, Word word) {
		Iterator<Word> iterator = words[line].iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getRealWord().length() == word.getRealWord().length()) {
				iterator.remove();
				--size;
				break;
			}
		}
		word.setId(generateWordId());
		words[line].add(word);
		++size;
	}

	/**
	 * 获取某一行的所有节点
	 *
	 * @param line
	 *            行号
	 * @return 一个数组
	 */
	public List<Word> get(int line) {
		return words[line];
	}

	/**
	 * 获取某一行的第一个节点
	 *
	 * @param line
	 * @return
	 */
	public Word getFirst(int line) {
		Iterator<Word> iterator = words[line].iterator();
		if (iterator.hasNext())
			return iterator.next();

		return null;
	}

	/**
	 * 获取某一行长度为length的节点
	 *
	 * @param line
	 * @param length
	 * @return
	 */
	public Word get(int line, int length) {
		for (Word word : words[line]) {
			if (word.getRealWord().length() == length) {
				return word;
			}
		}

		return null;
	}

	public boolean isHasLetter() {
		return hasLetter;
	}

	public void setHasLetter(boolean hasLetter) {
		this.hasLetter = hasLetter;
	}

	public boolean isHasNumber() {
		return hasNumber;
	}

	public void setHasNumber(boolean hasNumber) {
		this.hasNumber = hasNumber;
	}
	
	//+++++++++++++++++++++++++++++++++++++++++++++++
/*	//时间
	public boolean isHasTime() {
		return hasTime;
	}
	public void setHasTime(boolean hasTime) {
		this.hasTime = hasTime;
	}
	
	//邮箱
	public boolean isHasEmail() {
		return hasEmail;
	}
	public void setHasEmail(boolean hasEmail) {
		this.hasEmail = hasEmail;
	}
	
	//车牌号
	public boolean isHasCarNum() {
		return hasCarNum;
	}
	public void setHasCarNum(boolean hasCarNum) {
		this.hasCarNum = hasCarNum;
	}
	
	//身份证号
	public boolean isHasIDCard() {
		return hasIDCard;
	}
	public void setHasIDCard(boolean hasIDCard) {
		this.hasIDCard = hasIDCard;
	}
	
	//电话号码
	public boolean isHasPhoneNum() {
		return hasPhoneNum;
	}
	public void setHasPhoneNum(boolean hasPhoneNum) {
		this.hasPhoneNum = hasPhoneNum;
	}
	
	//银行卡
	public boolean isHasBankCard() {
		return hasBankCard;
	}
	public void setHasBankCard(boolean hasBankCard) {
		this.hasBankCard = hasBankCard;
	}*/
	//+++++++++++++++++++++++++++++++++++++++++++++++

	public int size() {
		return size;
	}

	public boolean hasExtendedCharset() {
		return hasExtendedCharset;
	}

	/**
	 * 清空词图
	 */
	public void clear() {
		for (List<Word> wordList : words) {
			wordList.clear();
		}
		size = 0;
	}

	/**
	 * 获取内部顶点列表
	 *
	 * @return
	 */
	public LinkedList<Word>[] getWords() {
		return words;
	}

	public Word getB() {
		Word begin = words[0].get(0);
		return begin;
	}

	public Word getE() {
		Word end = words[words.length - 1].get(0);
		return end;
	}
	
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        int line = 0;
        for (List<Word> wordList : words)
        {
            sb.append(String.valueOf(line++) + ':' + wordList.toString()).append("\n");
        }
        return sb.toString();
    }

}
