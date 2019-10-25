package base.operators.operator.nlp.classifier.bayes;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorException;
import base.operators.operator.learner.PredictionModel;
import base.operators.operator.nlp.classifier.bayes.prob.AddOneProb;
import base.operators.tools.Tools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 贝叶斯分类器使用
 * 
 */
public class Bayes extends PredictionModel implements Serializable {

	public Map<Double, AddOneProb> d;
	public Double total;

	public Bayes(ExampleSet exampleSet, Map<Double, AddOneProb> d, Double total) {
		super(exampleSet);
		this.d = d;
		this.total = total;
	}

	public ExampleSet performPrediction(ExampleSet predictSet, Attribute predictedLabel){
		Attributes attributes = predictSet.getAttributes();
		Attribute[] regularAttributes = attributes.createRegularAttributeArray();
		if(regularAttributes.length!=1){
			try {
				throw new OperatorException("表中只能含有一个规则属性（文本属性），以及ID和label特殊属性");
			} catch (OperatorException e) {
				e.printStackTrace();
			}
		}
		for (int jj = 0; jj < predictSet.size(); jj++) {
			Example example = predictSet.getExample(jj);
			String[] text = example.getValueAsString(regularAttributes[0]).split("\\s+");
			//temp存放计算的标签和标签的概率
			Map<Double, Double> tmp = new HashMap<Double, Double>();
			for (Map.Entry<Double, AddOneProb> entry : this.d.entrySet()) {
				tmp.put(entry.getKey(), Math.log(entry.getValue().getSum()) - Math.log(this.total));
				for (String word : text) {
					tmp.put(entry.getKey(), tmp.get(entry.getKey()) + Math.log(entry.getValue().frequency(word)));
				}
//				tmp.put(entry.getKey(), 0.0);
//				for (String word : text) {
//					tmp.put(entry.getKey(), tmp.get(entry.getKey()) + Math.log(entry.getValue().getSum()) - Math.log(this.total) + Math.log(entry.getValue().frequency(word)));
//				}
			}
			Double predictLabel = 0.0;
			double predictProb = 0;
			//下边是进行softmax归一化e(i)/sum(e(j))
			Map<Double, Double> tmp_normalize = new HashMap<Double, Double>();
			for (Map.Entry<Double, AddOneProb> entry : this.d.entrySet()) {
				double now = 0;
				for (Map.Entry<Double, AddOneProb> otherEntry : this.d.entrySet()) {
					now += Math.exp(tmp.get(otherEntry.getKey()) - tmp.get(entry.getKey()));
				}
				now = 1 / now;
				tmp_normalize.put(entry.getKey(), now);
				if (now > predictProb) {
					predictLabel = entry.getKey();
					predictProb = now;
				}
			}
			example.setPredictedLabel(predictLabel);
			for (double key : tmp_normalize.keySet()) {
				example.setConfidence(getLabel().getMapping().mapIndex((int)key), tmp_normalize.get(key));
			}
		}

		return predictSet;
	}
	@Override
	protected boolean supportsConfidences(Attribute label) {
		return label != null && (label.isNominal()||label.isNumerical());
	}

	@Override
	public String getName() {
		return "Bayes";
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("Total frequency of words: " + total + Tools.getLineSeparator());
		result.append("The number of labels: " + d.size() + Tools.getLineSeparator());
		return result.toString();
	}

}
