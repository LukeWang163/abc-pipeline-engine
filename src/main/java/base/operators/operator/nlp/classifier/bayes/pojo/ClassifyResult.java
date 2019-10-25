package base.operators.operator.nlp.classifier.bayes.pojo;

public class ClassifyResult {
	private String ret;
	private double prob;

	public ClassifyResult(String ret, double prob) {
		this.ret = ret;
		this.prob = prob;
	}

	public String getRet() {
		return ret;
	}

	public void setRet(String ret) {
		this.ret = ret;
	}

	public double getProb() {
		return prob;
	}

	public void setProb(double prob) {
		this.prob = prob;
	}

}
