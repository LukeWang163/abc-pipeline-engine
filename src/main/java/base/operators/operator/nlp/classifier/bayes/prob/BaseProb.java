package base.operators.operator.nlp.classifier.bayes.prob;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class BaseProb implements Serializable {

	private static final long serialVersionUID = 1L;
	//当前标签下的单词及其频数
	public Map<String, Double> data;
	//当前标签下的总频数
	public Double total;
	public Double none;

	public BaseProb() {
		this.data = new HashMap<String, Double>();
		this.total = 0.0;
		this.none = 0d;
	}

	public boolean exist(String key) {
		return this.data.containsKey(key);
	}

	public double getSum() {
		return this.total;
	}

	public double get(String key) {
		return this.exist(key) ? this.data.get(key) : none;
	}

	public double frequency(String key) {
		return get(key) / total;
	}

	public Set<String> samples() {
		return data.keySet();
	}

	public abstract void add(String key, Integer value);

	public Map<String, Double> getData() {
		return data;
	}

}
