package base.operators.operator.nlp.segment.kshortsegment.predict.segment;


import base.operators.operator.nlp.segment.kshortsegment.predict.tagger.POS;

import java.util.Iterator;
import java.util.TreeMap;

/**
 * 词
 * 
 * @author Jason Wang
 *
 */
public class Word {

	/**
	 * 词id(唯一标识)
	 */
	private int id;
	/**
	 * 等效词（对应人名、地名、机构名等存储归一后的等效词）
	 */
	private String word;
	/**
	 * 原词
	 */
	private String realWord;

	/**
	 * 最终词性
	 */
	private POS posTag;
	
	/**
	 * 初始化词性
	 */
	private TreeMap<POS, Double> posMap = new TreeMap<POS, Double>();


	/**
	 * 在句子中的起始位置
	 */
	private int offset;
	
	/**
	 * 在文本中的起始位置
	 */
	private int totaloffset;
	
	private int length;
	/**
	 * 词频
	 */
	private double frequency;
	/**
	 * 词向量
	 */
	private double vector;
	/**
	 * 专门标注器产生的附加对象（地址详情、时间详情等）
	 */
	private Object refObject=null;
	
	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}

	public void setVector(double vector) {
		this.vector = vector;
	}
	
	
	
	public void addPos(TreeMap<POS, Double> map){
		if(map!=null){
			Iterator it=map.keySet().iterator();
			while(it.hasNext()){
				POS key=(POS)it.next();
				posMap.put(key, map.get(key));
			}
		}
	}


	/**
	 * 构造函数
	 * 
	 * @param realWord
	 *            词
	 * @param initPosTag
	 *            词性
	 * @param offset
	 *            在文本中的起始位置
	 * @param frequency
	 *            词频
	 * @param vector
	 *            词向量
	 */
	public Word(String realWord, POS initPosTag, int offset, double frequency, double vector) {
		TreeMap<POS, Double> myPosMap = new TreeMap();
		myPosMap.put(initPosTag, frequency);
		this.posMap = myPosMap;
		this.word = convertRealWord(realWord, posMap);
		this.realWord = realWord;
		this.offset = offset;
		this.totaloffset = TokenizeAnnotator.SEN_OFF_SET+offset;
		this.frequency = frequency;
		this.vector = vector;
		this.length=realWord.codePointCount(0, realWord.length());
		
		//++++++++++++++++++++++++
		this.posTag = initPosTag;
	}

	public Word(String realWord, TreeMap<POS, Double> posMap, int offset, double vector) {
		this.word = convertRealWord(realWord, posMap);
		this.realWord = realWord;
		this.posMap = posMap;
		this.offset = offset;
		this.totaloffset = TokenizeAnnotator.SEN_OFF_SET+offset;
		this.vector = vector;
		double frequency = 0d;
		Iterator it = posMap.keySet().iterator();
		//对于多词性的词，如果多个词性的词频相同，则词的最终词频为单个词性的词频
		//否则为多个词频的累加
		while (it.hasNext()) {

			POS pos = (POS) it.next();
			Double frq = posMap.get(pos);
			if(frequency!=frq){
				frequency = frequency + frq;
			}
		}
		this.frequency = frequency;
		this.length=realWord.codePointCount(0, realWord.length());
		
		//++++++++++++++++++++++++
		this.posTag = guessPos();
	}
	
	
	public Word(String realWord) {
		this.word = convertRealWord(realWord, posMap);
		this.realWord = realWord;
		this.length=realWord.codePointCount(0, realWord.length());
	}

	/**
	 * 将命名实体转义成归一词
	 * 如果词性唯一，则将词性转换为等效词；如果不唯一，则不转换。
	 * @param word
	 * @param posMap
	 * @return
	 */
	public String convertRealWord(String word, TreeMap<POS, Double> posMap) {

		if (posMap.size() != 1)
			return word;
		Iterator it = posMap.keySet().iterator();
		POS pos = (POS) it.next();
		String posTag = pos.toString();
		if (POS.isPerson(posTag))
			return Define.TAG_PERSON;
		if (POS.isPlace(posTag))
			return Define.TAG_PLACE;
		if (POS.isOrg(posTag))
			return Define.TAG_ORG;
		if (POS.isTime(posTag))
			return Define.TAG_TIME;
		if (POS.isNumber(posTag))
			return Define.TAG_NUMBER;
		if (POS.isLetter(posTag))
			return Define.TAG_LETTER;
		if (POS.isSpecial(posTag))
		    return Define.TAG_SPECIAL;
		if (POS.isOther(posTag))
			return Define.TAG_OTHER;
		if (POS.isBegin(posTag))
			return Define.TAG_BIGIN;
		if (POS.isEnd(posTag))
			return Define.TAG_END;
		return word;
	}
	
	//+++++++zhangxian:增加一个根据最终词性更新等效词的方法(20170511)+++++++
	public void updateWordByPosTag(String posTag) {
		if (POS.isPerson(posTag))
			this.word = Define.TAG_PERSON;
		if (POS.isPlace(posTag))
			this.word = Define.TAG_PLACE;
		if (POS.isOrg(posTag))
			this.word = Define.TAG_ORG;
		if (POS.isTime(posTag))
			this.word = Define.TAG_TIME;
		if (POS.isNumber(posTag))
			this.word = Define.TAG_NUMBER;
		if (POS.isLetter(posTag))
			this.word = Define.TAG_LETTER;
		if (POS.isSpecial(posTag))
			this.word = Define.TAG_SPECIAL;
		if (POS.isOther(posTag))
			this.word = Define.TAG_OTHER;
		if (POS.isBegin(posTag))
			this.word = Define.TAG_BIGIN;
		if (POS.isEnd(posTag))
			this.word = Define.TAG_END;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * 词
	 * 
	 * @return
	 */
	public String getWord() {
		return word;
	}
    
	//最终词性
	public POS getPosTag() {
		return posTag;
	}

	//词性、词频集合
	public TreeMap<POS, Double> getPosMap(){
		return posMap;
	}
	
	/**
	 * 在句子中的起始位置
	 * 
	 * @return
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * 在文本中的起始位置
	 * 
	 * @return
	 */
	public int getTotalOffset() {
		return totaloffset;
	}
	
	/**
	 * 词频
	 * 
	 * @return
	 */
	public double getFrequency() {
		return frequency;
	}

	/**
	 * 词向量
	 * 
	 * @return
	 */
	public double getVector() {
		return vector;
	}

	public String getRealWord() {
		return realWord;
	}

	/**
	 * 创建一个数词实例
	 *
	 * @param realWord
	 *            数字对应的真实字串
	 * @return 数词顶点
	 */
	public static Word newNumberInstance(String realWord, int offset) {
		return new Word(realWord, POS.m, offset, 1, 0);
	}

	/**
	 * 创建一个地名实例
	 *
	 * @param realWord
	 *            真实字串
	 * @return 地名顶点
	 */
	public static Word newPlaceInstance(String realWord, int offset) {
		return new Word(realWord, POS.ns, offset, 1, 0);
	}

	/**
	 * 创建一个人名实例
	 *
	 * @param realWord
	 *            真实字串
	 * @return
	 */
	public static Word newPersonInstance(String realWord, int offset) {
		return new Word(realWord, POS.nh, offset, 1, 0);
	}

	/**
	 * 创建一个时间实例
	 *
	 * @param realWord
	 *            真实字串
	 * @return
	 */
	public static Word newTimeInstance(String realWord, int offset) {
		return new Word(realWord, POS.nt, offset, 1, 0);
	}

	public static Word newB() {
		return new Word(" ", POS.begin, 0, Define.MAX_FREQUENCY / 10, 0);
	}


	public static Word newE(int offset) {
		return new Word(" ", POS.end, offset, Define.MAX_FREQUENCY / 10, 0);
	}

	/**
	 * 词长度
	 * 
	 * @return
	 */
	public int getLength() {
		return length;
	}

	public POS[] getPOSArray() {
		int size = posMap.size();
		POS pos[] = new POS[size];
		Iterator it = posMap.keySet().iterator();
		int i = 0;
		while (it.hasNext()) {
			POS myPOS = (POS) it.next();
			pos[i] = myPOS;
			i++;
		}
		return pos;
	}

	public double getPOSFrequency(POS pos) {
		Double frq = posMap.get(pos);
		if (frq == null)
			return 0d;
		return frq;
	}

	//更新最终词性，并根据最终词性更新等效词
	public void confirmPOS(POS pos) {
		posTag = pos;
		//++++++同时更新等效词+++++++++
		updateWordByPosTag(posTag.toString());
		//+++++++++++++++++++++++++
	}
/*
	public String toString() {
		 return "[" + realWord + "," + posTag + "," + frequency + "]";
	}
*/	
	public String toString() {
		 return "["  + realWord + ",posMap:" +
		 posMap + ",postag:" + posTag+ ",frequency:" + frequency  +",refObject:"+refObject+ "]";
	}
/*	
	public String toString() {
		 return "[word:" + word + ",realWord:" + realWord + ",postag:" + posTag + ",offset:"
		 + offset + "]";
	}
	
	public String toString() {
		 return "["+realWord+"/"+offset+" "+totaloffset+"]";
	}*/
	
    /**
     * 猜测最可能的词性，也就是这个节点的词性中出现频率最大的那一个词性
     *
     * @return
     */
    public POS guessPos()
    {
    	POS guessPos = POS.x;
    	
    	if(posMap==null || posMap.size()==0){
    		return guessPos;
    	}
    	Iterator<POS> ite= posMap.keySet().iterator(); 
    	double maxFrequency = 0.0d;
    	POS pos = null;
    	
    	while(ite.hasNext()){
    		pos = ite.next();
    		if(posMap.get(pos)>=maxFrequency){
    			maxFrequency = posMap.get(pos);
    			guessPos = pos;
    		}
    	}
    	return guessPos;
    }

	public Object getRefObject() {
		return refObject;
	}

	public void setRefObject(Object refObject) {
		this.refObject = refObject;
	}
    
    

}
