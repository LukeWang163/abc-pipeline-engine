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
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.learner.AbstractLearner;

import java.io.Serializable;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:A Classifier Trainer to be used with MostFrequentClassifier.
 * 
 * @see MostFrequentClassifier
 * 
 */
public class MostFrequentClassAssignmentTrainer extends AbstractLearner implements Serializable {
	
	MostFrequentClassifier classifier = null;

	ExampleSet trainingSet;        // Needed to construct a new classifier
	Attribute[] dataAlphabet;    // Extracted from InstanceList. Must be the same for all calls to incrementalTrain()
    NominalMapping targetAlphabet; // Extracted from InstanceList. Must be the same for all calls to incrementalTrain

	public MostFrequentClassAssignmentTrainer(OperatorDescription operatorDescription){
		super(operatorDescription);
	}

	public MostFrequentClassifier getClassifier() {
		return this.classifier;
	}

  /**
   * Create a MostFrequent classifier from a set of training data.
   * 
   * @param trainingSet The InstanceList to be used to train the classifier.
   * @return The MostFrequent classifier as trained on the trainingList
   */

	public MostFrequentClassifier learn(ExampleSet trainingSet) {
		// Initialize or check the instancePipe
	  	if (trainingSet != null) {
	  		if (this.trainingSet == null)
	  			this.trainingSet = trainingSet;
	  		this.dataAlphabet = this.trainingSet.getAttributes().createRegularAttributeArray();
	  		this.targetAlphabet = this.trainingSet.getAttributes().getLabel().getMapping();
	  	}
	  	
	  	this.classifier = new MostFrequentClassifier(this.trainingSet);
		this.classifier.labels = this.targetAlphabet;
	  	
	  	// Init alphabets and extract label from instance
		for(Example example : trainingSet) {
			if (dataAlphabet == null) {
		  		this.dataAlphabet = example.getAttributes().createRegularAttributeArray();
		  		this.targetAlphabet = example.getAttributes().getLabel().getMapping();
		  	}
			double label = example.getLabel();
			
			this.classifier.addTargetLabel(label);
		}
		
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
