/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.  For further
information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.maxent;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.learner.PredictionModel;
import base.operators.operator.learner.maxent.optimize.MatrixOps;
import base.operators.tools.Tools;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:Maximum Entropy (AKA Multivariate Logistic Regression) classifier.
 */

public class MaxEnt extends PredictionModel implements Serializable
{
	protected ExampleSet trainExampleSet;
	protected double [] parameters;										// indexed by <labelIndex,featureIndex>
	protected int defaultFeatureIndex;
//	protected FeatureSelection featureSelection;
//	protected FeatureSelection[] perClassFeatureSelection;

	// The default feature is always the feature with highest index
	public MaxEnt (ExampleSet trainExampleSet,
				   double[] parameters)
	{
		super (trainExampleSet);
		this.trainExampleSet = trainExampleSet;
		if (parameters != null)
			this.parameters = parameters;
		else
			this.parameters = new double[getNumParameters(this.trainExampleSet)];
//		this.featureSelection = featureSelection;
//		this.perClassFeatureSelection = perClassFeatureSelection;
		List<Attribute> regularAttributes = Arrays.asList(this.trainExampleSet.getAttributes().createRegularAttributeArray());
		this.defaultFeatureIndex = regularAttributes.size();
//		assert (parameters.getNumCols() == defaultFeatureIndex+1);
	}

	public double[] getParameters () {
		return parameters;
	}

	public int getNumParameters () {
		return MaxEnt.getNumParameters(this.trainExampleSet);
	}

	public static int getNumParameters (ExampleSet trainExampleSet) {
		List<Attribute> regularAttributes = Arrays.asList(trainExampleSet.getAttributes().createRegularAttributeArray());
		return (regularAttributes.size() + 1) * trainExampleSet.getAttributes().getLabel().getMapping().size();
	}

	public void setParameters(double[] parameters){
		this.parameters = parameters;
	}

	public void setParameter (int classIndex, int featureIndex, double value)
	{
		int sizef = trainExampleSet.getAttributes().size()-trainExampleSet.getAttributes().specialSize();
		parameters[classIndex*(sizef+1) + featureIndex] = value;
	}


	public int getDefaultFeatureIndex(){
		return defaultFeatureIndex;
	}

	public void setDefaultFeatureIndex(int defaultFeatureIndex){
		this.defaultFeatureIndex = defaultFeatureIndex;
	}


	public void getUnnormalizedClassificationScores (Example instance, double[] scores)
	{
		//  arrayOutOfBounds if pipe has grown since training
		//        int numFeatures = getAlphabet().size() + 1;
		int numFeatures = this.defaultFeatureIndex + 1;

		int numLabels = instance.getAttributes().getLabel().getMapping().size();
		assert (scores.length == numLabels);
		// Make sure the feature vector's feature dictionary matches
		// what we are expecting from our data pipe (and thus our notion
		// of feature probabilities.

		// Include the feature weights according to each label
		for (int li = 0; li < numLabels; li++) {
			scores[li] = parameters[li*numFeatures + defaultFeatureIndex]
					+ MatrixOps.rowDotProduct (parameters, numFeatures,
					li, instance,
					defaultFeatureIndex);
		}
	}

	public void getClassificationScores (Example instance, double[] scores)
	{
		getUnnormalizedClassificationScores(instance, scores);
		// Move scores to a range where exp() is accurate, and normalize
		int numLabels = instance.getAttributes().getLabel().getMapping().size();
		double max = MatrixOps.max (scores);
		double sum = 0;
		for (int li = 0; li < numLabels; li++)
			sum += (scores[li] = Math.exp (scores[li] - max));
		for (int li = 0; li < numLabels; li++) {
			scores[li] /= sum;
			// xxxNaN assert (!Double.isNaN(scores[li]));
		}
	}

	//modified by Limin Yao, to deal with decreasing the peak of some labels
	public void getClassificationScoresWithTemperature (Example instance, double temperature, double[] scores)
	{
		getUnnormalizedClassificationScores(instance, scores);

		//scores should be divided by temperature, scores are sum of weighted features
		MatrixOps.timesEquals(scores, 1/temperature);

		// Move scores to a range where exp() is accurate, and normalize
		int numLabels = instance.getAttributes().getLabel().getMapping().size();
		double max = MatrixOps.max (scores);
		double sum = 0;
		for (int li = 0; li < numLabels; li++)
			sum += (scores[li] = Math.exp (scores[li] - max));
		for (int li = 0; li < numLabels; li++) {
			scores[li] /= sum;
			// xxxNaN assert (!Double.isNaN(scores[li]));
		}
	}
	public double[] predictLabelAndProbability(Example example){
		int numClasses = example.getAttributes().getLabel().getMapping().size();
		double[] scores = new double[numClasses];
		//getClassificationScores (instance, scores);
		getClassificationScores(example, scores);
		return scores;
	}
	/**
	 * Classifies an instance using Winnow's weights
	 * @param examples an ExampleSet to be classified
	 * @param predictedLabel an Attribute to set predict label
	 * @return an object containing the classifier's guess
	 */
	public ExampleSet performPrediction (ExampleSet examples, Attribute predictedLabel){
		for (int jj = 0; jj < examples.size(); jj++) {
			Example example = examples.getExample(jj);
			int numClasses = example.getAttributes().getLabel().getMapping().size();
			double[] scores = new double[numClasses];
			//getClassificationScores (instance, scores);
			getClassificationScores(example, scores);
			// Create and return a Classification object

			double max = 0;
			int maxIndex = 0;
			for (int i = 0; i < scores.length; i++) {
				if (scores[i] > max) {
					max = scores[i];
					maxIndex = i;
				}
			}
			example.setPredictedLabel(maxIndex);

			for (int ll = 0; ll < scores.length; ll++) {
				example.setConfidence(getLabel().getMapping().mapIndex(ll), scores[ll]);
			}
		}
		// Create and return a Classification object
		return examples;
	}

	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 1;
	static final int NULL_INTEGER = -1;

	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeObject(trainExampleSet);
		int np = parameters.length;
		out.writeInt(np);
		for (int p = 0; p < np; p++)
			out.writeDouble(parameters[p]);
		out.writeInt(defaultFeatureIndex);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt();
		if (version != CURRENT_SERIAL_VERSION)
			throw new ClassNotFoundException("Mismatched MaxEnt versions: wanted " +
					CURRENT_SERIAL_VERSION + ", got " +
					version);
		trainExampleSet = (ExampleSet) in.readObject();
		int np = in.readInt();
		parameters = new double[np];
		for (int p = 0; p < np; p++)
			parameters[p] = in.readDouble();
		defaultFeatureIndex = in.readInt();

	}
	@Override
	protected boolean supportsConfidences(Attribute label) {
		return label != null && (label.isNominal()||label.isNumerical());
	}

	@Override
	public String getName() {
		return "MaxEnt Model";
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < getNumParameters(); i++) {
			result.append("paramter "+i+": "+parameters[i] + Tools.getLineSeparator());
		}
		result.append("Total number of paramters: " + getNumParameters() + Tools.getLineSeparator());
		return result.toString();
	}

}

