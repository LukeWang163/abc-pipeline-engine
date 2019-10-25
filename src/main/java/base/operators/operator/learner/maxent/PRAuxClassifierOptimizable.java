/* Copyright (C) 2011 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.maxent;

import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.learner.maxent.constraint.pr.MaxEntPRConstraint;
import base.operators.operator.learner.maxent.optimize.MatrixOps;
import base.operators.operator.learner.maxent.optimize.Optimizable;
import base.operators.operator.learner.maxent.utils.Maths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:Optimizable for training auxiliary model (q) for E-step/I-projection in PR training.
 * 
 */

public class PRAuxClassifierOptimizable implements Optimizable.ByGradientValue {

  private static Logger logger = Logger.getLogger(PRAuxClassifierOptimizable.class.getName());
  
  private boolean cacheStale;
  private int numParameters;
  private double cachedValue;
  private double[] cachedGradient;
  private double[][] parameters;
  private double[][] baseDist;
  private PRAuxClassifier classifier;
  private ArrayList<MaxEntPRConstraint> constraints;
  private ExampleSet trainingData;
  
  public PRAuxClassifierOptimizable(ExampleSet trainingData, double[][] baseDistribution, PRAuxClassifier classifier) {
    this.trainingData = trainingData;
    this.baseDist = baseDistribution;
    this.classifier = classifier;
    this.parameters = classifier.getParameters();
    this.constraints = classifier.getConstraintFeatures();
    
    this.numParameters = 0;
    for (int i = 0; i < parameters.length; i++) {
      numParameters += parameters[i].length;
    }
    
    this.cachedValue = Double.NEGATIVE_INFINITY;
    this.cachedGradient = new double[numParameters];
    this.cacheStale = true;
  }
  
  public int getNumParameters() {
    return numParameters;
  }

  public void getParameters(double[] buffer) {
    int start = 0;
    for (int i = 0; i < parameters.length; i++) {
      System.arraycopy(parameters[i], 0, buffer, start, parameters[i].length);
      start += parameters[i].length;
    }
  }

  public double getParameter(int index) {
    int start = 0;
    for (int i = 0; i < parameters.length; i++) {
      if (start < parameters[i].length) {
        return parameters[i][start];
      }
      else {
        start -= parameters[i].length;
      }
    }
    throw new RuntimeException(index + " out of bounds.");
  }

  public void setParameters(double[] params) {
    int start = 0;
    for (int i = 0; i < parameters.length; i++) {
      System.arraycopy(params, start, parameters[i], 0, parameters[i].length);
      start += parameters[i].length;
    }
    cacheStale = true;
  }

  public void setParameter(int index, double value) {
    int start = 0;
    for (int i = 0; i < parameters.length; i++) {
      if (start < parameters[i].length) {
        parameters[i][start] = value;
      }
      else {
        start -= parameters[i].length;
      }
    }
    cacheStale = true;
  }
  
  
  public double getValueAndGradient(double[] gradient) {
    Arrays.fill(gradient, 0);
    
    classifier.zeroExpectations();
    
    int numLabels = trainingData.getAttributes().getLabel().getMapping().size();
    

    double value = 0.;
    //double sumLogP = 0;
    
    for (int ii = 0; ii < trainingData.size(); ii++) {
      double[] scores = new double[numLabels];
      Example example = trainingData.getExample(ii);
      double instanceWeight = 1.0;
      if (example.getAttributes().getRole("weight") != null) {
        instanceWeight = example.getWeight();
      }
      classifier.getClassificationScores(example, scores);
      double logZ = Double.NEGATIVE_INFINITY;
      
      for (int li = 0; li < numLabels; li++) {
        if (baseDist != null && baseDist[ii][li] == 0) {
          scores[li] = Double.NEGATIVE_INFINITY;
        }
        else if (baseDist != null) {
          double logP = Math.log(baseDist[ii][li]);
          scores[li] += logP;  
        }
        logZ = Maths.sumLogProb(logZ, scores[li]);
      }
      assert(!Double.isNaN(logZ));
      value -= instanceWeight * logZ;
      
      if (Double.isNaN(value)) {
        logger.warning("Instance " + example.getId() + " has NaN value.");
        continue;
      }
      if (Double.isInfinite(value)) {
        logger.warning("Instance " + example.getId() + " has infinite value; skipping value and gradient");
        continue;
      }
      
      // exp normalize
      MatrixOps.expNormalize(scores);
      
      // increment expectations
      for (MaxEntPRConstraint constraint : constraints) {
        constraint.incrementExpectations(example, scores, 1);
      }
    }
    
    int ci = 0;
    int start = 0;
    for (MaxEntPRConstraint constraint : constraints) {
      double[] temp = new double[parameters[ci].length];
      value += constraint.getAuxiliaryValueContribution(parameters[ci]);
      constraint.getGradient(parameters[ci], temp);
      System.arraycopy(temp, 0, gradient, start, temp.length);
      start += temp.length;
      ci++;
    }
    
    logger.info("PR auxiliary value = " + value);
    return value;
  }

  public double getValue() {
    if (cacheStale) {
      cachedValue = getValueAndGradient(cachedGradient);
      cacheStale = false;
    }
    return cachedValue;
  }
  
  public void getValueGradient(double[] gradient) {
    if (cacheStale) {
      cachedValue = getValueAndGradient(cachedGradient);
      cacheStale = false;
    }
    System.arraycopy(cachedGradient, 0, gradient, 0, gradient.length);
  }
}
