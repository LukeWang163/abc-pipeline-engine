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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:Maximum Entropy classifier.
 */

public class MCMaxEnt extends PredictionModel implements Serializable
{
    double [] parameters;										// indexed by <labelIndex,featureIndex>
    int defaultFeatureIndex;

    // The default feature is always the feature with highest index
    public MCMaxEnt (ExampleSet examples,
                     double[] parameters)
    {
        super (examples);
        this.parameters = parameters;
        this.defaultFeatureIndex = examples.getAttributes().createRegularAttributeArray().length;
//		assert (parameters.getNumCols() == defaultFeatureIndex+1);
    }

    public double[] getParameters ()
    {
        return parameters;
    }

    public void setParameter (int classIndex, int featureIndex, double value)
    {
        parameters[classIndex*(defaultFeatureIndex+1) + featureIndex] = value;
    }

    public void getUnnormalizedClassificationScores (Example example, double[] scores)
    {
			  //  arrayOutOfBounds if pipe has grown since training
			  //        int numFeatures = getAlphabet().size() + 1;
        int numFeatures = this.defaultFeatureIndex + 1;

        int numLabels = example.size();
        assert (scores.length == numLabels);

        // Include the feature weights according to each label
        for (int li = 0; li < numLabels; li++) {
            scores[li] = parameters[li*numFeatures + defaultFeatureIndex]
                    + MatrixOps.rowDotProduct (parameters, numFeatures,
                            li, example,
                            defaultFeatureIndex);
        }
    }

    public void getClassificationScores (Example example, double[] scores)
    {
        int numLabels = example.getAttributes().getLabel().getMapping().size();
        assert (scores.length == numLabels);
        int numFeatures = this.defaultFeatureIndex + 1;

        // Include the feature weights according to each label
        for (int li = 0; li < numLabels; li++) {
            scores[li] = parameters[li*numFeatures + defaultFeatureIndex]
                    + MatrixOps.rowDotProduct (parameters, numFeatures,
                            li, example,
                            defaultFeatureIndex);
            // xxxNaN assert (!Double.isNaN(scores[li])) : "li="+li;
        }

        // Move scores to a range where exp() is accurate, and normalize
        double max = MatrixOps.max (scores);
        double sum = 0;
        for (int li = 0; li < numLabels; li++)
            sum += (scores[li] = Math.exp (scores[li] - max));
        for (int li = 0; li < numLabels; li++) {
            scores[li] /= sum;
            // xxxNaN assert (!Double.isNaN(scores[li]));
        }
    }

    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 1;
    static final int NULL_INTEGER = -1;

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.writeInt(CURRENT_SERIAL_VERSION);
        int np = parameters.length;
        out.writeInt(np);
        for (int p = 0; p < np; p++)
            out.writeDouble(parameters[p]);
        out.writeInt(defaultFeatureIndex);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        if (version != CURRENT_SERIAL_VERSION)
            throw new ClassNotFoundException("Mismatched MCMaxEnt versions: wanted " +
                    CURRENT_SERIAL_VERSION + ", got " +
                    version);
        int np = in.readInt();
        parameters = new double[np];
        for (int p = 0; p < np; p++)
            parameters[p] = in.readDouble();
        defaultFeatureIndex = in.readInt();

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
            getClassificationScores (example, scores);
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
    @Override
    protected boolean supportsConfidences(Attribute label) {
        return label != null && (label.isNominal()||label.isNumerical());
    }

    @Override
    public String getName() {
        return "MCMaxEnt Model";
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < parameters.length; i++) {
            result.append("paramter "+i+": "+parameters[i] + Tools.getLineSeparator());
        }
        result.append("Total number of paramters: " + parameters.length + Tools.getLineSeparator());
        return result.toString();
    }
}

