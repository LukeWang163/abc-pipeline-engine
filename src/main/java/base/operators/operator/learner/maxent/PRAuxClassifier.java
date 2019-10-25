/* Copyright (C) 2011 Univ. of Massachusetts Amherst, Computer Science Dept.
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
import base.operators.operator.learner.maxent.constraint.pr.MaxEntPRConstraint;
import base.operators.operator.learner.maxent.optimize.MatrixOps;
import base.operators.tools.Tools;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:Auxiliary model (q) for E-step/I-projection in PR training.
 *
 */

public class PRAuxClassifier extends PredictionModel implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private int numLabels;
  private double[][] parameters;
  private ArrayList<MaxEntPRConstraint> constraints;

  public PRAuxClassifier(ExampleSet data, ArrayList<MaxEntPRConstraint> constraints) {
    super(data);
    this.constraints = constraints;
    this.parameters = new double[constraints.size()][];
    for (int i = 0; i < constraints.size(); i++) {
      this.parameters[i] = new double[constraints.get(i).numDimensions()];
    }
    this.numLabels = data.getAttributes().getLabel().getMapping().size();
  }
  
  public void getClassificationScores(Example example, double[] scores) {
    for (MaxEntPRConstraint feature : constraints) {
      feature.preProcess(example);
    }
    for (int li = 0; li < numLabels; li++) {
      int ci = 0;
      for (MaxEntPRConstraint feature : constraints) {
        scores[li] += feature.getScore(example, li, parameters[ci]);
        ci++;
      }
    }
  }
  
  public void getClassificationProbs(Example example, double[] scores) {
    getClassificationScores(example, scores);
    MatrixOps.expNormalize(scores);
  }

  public double predict(Example example) {
    double[] scores = new double[numLabels];
    getClassificationScores(example, scores);

    double max = 0;
    int maxIndex = 0;
    for (int i = 0; i < scores.length; i++) {
      if (scores[i] > max) {
        max = scores[i];
        maxIndex = i;
      }
    }
    // Create and return a Classification object
    return maxIndex;
  }

  /**
   * Classifies an instance using Winnow's weights
   * @param examples an ExampleSet to be classified
   * @param predictedLabel an Attribute to set predict label
   * @return an object containing the classifier's guess
   */
  public ExampleSet performPrediction (ExampleSet examples, Attribute predictedLabel){
    for (int jj = 0; jj < examples.size(); jj++) {
      double label = predict(examples.getExample(jj));
      examples.getExample(jj).setPredictedLabel(label);
    }
    // Create and return a Classification object
    return examples;
  }


  public double[][] getParameters() { 
    return parameters;
  }
  
  public ArrayList<MaxEntPRConstraint> getConstraintFeatures() { 
    return constraints;
  }

  public void zeroExpectations() {
    for (MaxEntPRConstraint constraint : constraints) {
      constraint.zeroExpectations();
    }
  }

  @Override
  public String getName() {
    return "PRAux Classifier";
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < parameters.length; i++) {
      String per = "paramter "+i+": ";
      for (int j = 0; j < parameters[0].length; j++) {
        per = per+" "+parameters[i][j];
      }
      result.append(per+Tools.getLineSeparator());
    }
    result.append("Total number of paramters: " + parameters.length + Tools.getLineSeparator());
    result.append("Total number of labels: "+numLabels+ Tools.getLineSeparator());
    return result.toString();
  }

}
