/* This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.mostfrequent;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.NominalMapping;
import base.operators.operator.learner.PredictionModel;
import base.operators.tools.Tools;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description: A Classifier that will return the most frequent class label based on a training set.
 * The Classifier needs to be trained using the MostFrequentClassAssignmentTrainer.
 * 
 * @see MostFrequentClassAssignmentTrainer
 * 
 */
public class MostFrequentClassifier extends PredictionModel implements Serializable {

	TreeMap<String, Integer> sortedLabelMap = new TreeMap<String, Integer>();
	NominalMapping labels;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2685212760735255652L;

	public MostFrequentClassifier(ExampleSet examples) {
		super(examples);
	}

	/**
	 * Classifies an instance using Winnow's weights
	 * @param examples an ExampleSet to be classified
	 * @param predictedLabel an Attribute to set predict label
	 * @return an object containing the classifier's guess
	 */
	public ExampleSet performPrediction (ExampleSet examples, Attribute predictedLabel){
		double sum = 0;
		double max = 0;
		String mostLabel = "";
		Set<String> keys= this.sortedLabelMap.keySet();
		for(String key: keys){
			if(this.sortedLabelMap.get(key) > max){
				max = this.sortedLabelMap.get(key);
				mostLabel = key;
			}
			sum+=this.sortedLabelMap.get(key);
		}
		for (int jj = 0; jj < examples.size(); jj++) {
			Example example = examples.getExample(jj);
			double label =  this.labels.getIndex(mostLabel);
			example.setPredictedLabel(label);

			for (String key :  this.sortedLabelMap.keySet()) {
				example.setConfidence(key,  this.sortedLabelMap.get(key)/sum);
			}
		}
		// Create and return a Classification object
		return examples;
	}

	public void addTargetLabel(double label) {
		String labelEntry = this.labels.mapIndex((int)label);
		if(this.sortedLabelMap.containsKey(labelEntry)){
			Integer oldCount = this.sortedLabelMap.get(labelEntry);
			Integer newCount = oldCount + 1;
			this.sortedLabelMap.put(labelEntry, newCount);
		}else{
			this.sortedLabelMap.put(labelEntry, 1);
		}
	}
	@Override
	protected boolean supportsConfidences(Attribute label) {
		return label != null && (label.isNominal()||label.isNumerical());
	}

	@Override
	public String getName() {
		return "Most Frequent Classifier";
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (Map.Entry<String, Integer> entry : sortedLabelMap.entrySet()) {
			result.append("label : "+entry.getKey()+" frequency: " +entry.getValue()+ Tools.getLineSeparator());
		}
		return result.toString();
	}

}
