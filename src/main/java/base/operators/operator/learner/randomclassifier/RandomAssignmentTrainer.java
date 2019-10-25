/* This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.randomclassifier;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.NominalMapping;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.learner.AbstractLearner;

import java.io.Serializable;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:A Classifier Trainer to be used with RandomClassifier.
 * 
 * @see RandomAssignmentTrainer
 * 
*/
public class RandomAssignmentTrainer extends AbstractLearner implements Serializable {

	RandomClassifier classifier = null;

	ExampleSet trainingSet;        // Needed to construct a new classifier
	Attribute[] dataAlphabet;    // Extracted from InstanceList. Must be the same for all calls to incrementalTrain()
	NominalMapping targetAlphabet; // Extracted from InstanceList. Must be the same for all calls to incrementalTrain

	public RandomAssignmentTrainer(OperatorDescription operatorDescription){
		super(operatorDescription);
	}

	public RandomClassifier getClassifier() {
		return this.classifier;
	}

	  /**
	   * Create a Random classifier from a set of training data.
	   * 
	   * @param trainingList The InstanceList to be used to train the classifier.
	   * @return The Random classifier as trained on the trainingList
	   */
	public RandomClassifier learn(ExampleSet trainingList) {
		
		// Initialize or check the instancePipe
		if (trainingList != null) {
			if (this.trainingSet == null)
				this.trainingSet = trainingList;
			this.dataAlphabet = this.trainingSet.getAttributes().createRegularAttributeArray();
			this.targetAlphabet = this.trainingSet.getAttributes().getLabel().getMapping();
		}

		this.classifier = new RandomClassifier(this.trainingSet);
		this.classifier.labels = this.targetAlphabet;
	  	
		this.classifier.addTargetLabel(trainingList.getAttributes().getLabel().getMapping());

		return this.classifier;
	}

	public boolean supportsCapability(OperatorCapability capability){
		switch (capability) {
//            case POLYNOMINAL_ATTRIBUTES:
//            case BINOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
			case BINOMINAL_LABEL:
			case NUMERICAL_LABEL:
			case POLYNOMINAL_LABEL:
			case WEIGHTED_EXAMPLES:
			case UPDATABLE:
			case MISSING_VALUES:
				return true;
			default:
				return false;
		}
	}
}
