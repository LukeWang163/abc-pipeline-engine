package base.operators.operator.nlp.commenttag;

//三元组，由依存分析结果获取三元组；类中保留信息供使用

public class ThreeTuples {

	private String sentence=null; //依存分析的结果
	private String nsubj=null;  // 依存分析的nsubj
	private String advmod=null;  //依存分析的advmod
	private String root=null;  //依存分析的root
	private String noun=null;  //名词
	private String adj=null; //形容词
	private String adv=null; //副词
	private String sentenceTag=null; //结果
	
	public ThreeTuples(String s) {
		sentence = s;
	}
	
	public String getSentence() {
		return sentence;
	}

	public String getNsubj() {
		return nsubj;
	}

	public String getAdvmod() {
		return advmod;
	}

	public String getRoot() {
		return root;
	}

	public String getNoun() {
		return noun;
	}

	public String getAdj() {
		return adj;
	}

	public String getAdv() {
		return adv;
	}

	public String getsentenceTag() {
		return sentenceTag;
	}

	public boolean analys() {
		
		int nsuint = sentence.indexOf("nsubj");
		if(nsuint == -1) {
			return false;
		}else {
			int beginIndex = nsuint+6;
			int endIndex = sentence.indexOf(")", beginIndex);
			nsubj = sentence.substring(beginIndex, endIndex);
		}
		String []temp1 = nsubj.split(",");
		noun = temp1[1].substring(1, temp1[1].indexOf("-"));
		adj = temp1[0].substring(0, temp1[0].indexOf("-"));
		
		int advint = sentence.indexOf("advmod");
		if(advint == -1) {
			sentenceTag = noun+adj;
			return true;
		}else {
			int beginIndex = advint+7;
			int endIndex = sentence.indexOf(")", beginIndex);
			advmod = sentence.substring(beginIndex, endIndex);
		}
		
		String []temp2 = advmod.split(",");
		if(temp1[0].equals(temp2[0])) {
		adv=temp2[1].substring(1, temp2[1].indexOf("-"));
		//sentenceTag = noun+adv+adj;
		sentenceTag = noun+adj;
		}else {
			sentenceTag=noun+adj;
		}
		return true;
	}
}
