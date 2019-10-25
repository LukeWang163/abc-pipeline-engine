/* This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.randomclassifier;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.NominalMapping;
import base.operators.operator.learner.PredictionModel;

import java.io.Serializable;
import java.util.Random;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 * A Classifier that will return a randomly selected class label.
 * The Classifier needs to be trained using the RandomAssignmentTrainer.
 * 
 * Note that the frequency distribution gives more weight to class labels with
 * a higher frequency in the training data. To create a Classifier which gives
 * equal weight to each class, simply use a Set<Label> instead of the List<Label>.
 * 
 * @see RandomAssignmentTrainer
 * 
 */
public class RandomClassifier extends PredictionModel implements Serializable {

	NominalMapping labels;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3689741912639283481L;

	public RandomClassifier(ExampleSet examples) {
		super(examples);
	}

    /**
     * Classify an instance using random selection based on the trained data.
     * 
     * @param examples to be classified. Data field must be a FeatureVector.
     * @return Classification containing the labeling of the instance.
     */

	public ExampleSet performPrediction (ExampleSet examples, Attribute predictedLabel){
		int max = this.labels.size() - 1;
		for (int jj = 0; jj < examples.size(); jj++) {
			Example example = examples.getExample(jj);
			Random random = new Random();
			int rndIndex = random.nextInt(max + 1);
			example.setPredictedLabel(rndIndex);
			for (int i = 0; i < labels.size(); i++) {
				if(i==rndIndex){
					example.setConfidence(labels.mapIndex(i),  1);
				}else {
					example.setConfidence(labels.mapIndex(i),  0);
				}
			}
		}
		// Create and return a Classification object
		return examples;
	}

	public void addTargetLabel(NominalMapping label) {
		this.labels = label;
	}

	@Override
	public String getName() {
		return "Random Classifier";
	}

}
