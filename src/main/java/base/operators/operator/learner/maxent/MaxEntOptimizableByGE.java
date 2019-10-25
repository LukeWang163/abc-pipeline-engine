/* Copyright (C) 2011 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.maxent;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.learner.maxent.constraint.ge.MaxEntGEConstraint;
import base.operators.operator.learner.maxent.optimize.MatrixOps;
import base.operators.operator.learner.maxent.optimize.Optimizable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 * Training of MaxEnt models with labeled features using
 * Generalized Expectation Criteria.
 * 
 * Based on: 
 * "Learning from Labeled Features using Generalized Expectation Criteria"
 * Gregory Druck, Gideon Mann, Andrew McCallum
 * SIGIR 2008
 * 
 */

public class MaxEntOptimizableByGE implements Optimizable.ByGradientValue {
  
  private static Logger progressLogger = Logger.getLogger(MaxEntOptimizableByGE.class.getName()+"-pl");
  
  protected boolean cacheStale = true;
  protected int defaultFeatureIndex;
  protected double temperature;
  protected double objWeight;
  protected double cachedValue;
  protected double gaussianPriorVariance;
  protected double[] cachedGradient;
  protected double[] parameters;
  protected ExampleSet trainingList;
  protected MaxEnt classifier;
  protected ArrayList<MaxEntGEConstraint> constraints;
  
  /**
   * @param trainingList List with unlabeled training instances.
   * @param constraints Feature expectation constraints.
   * @param initClassifier Initial classifier.
   */
  public MaxEntOptimizableByGE(ExampleSet trainingList, ArrayList<MaxEntGEConstraint> constraints, MaxEnt initClassifier) {
    temperature = 1.0;
    objWeight = 1.0;
    gaussianPriorVariance = 1.0;
    this.trainingList = trainingList;
    
    int numFeatures = trainingList.getAttributes().createRegularAttributeArray().length;

    defaultFeatureIndex = numFeatures;
    int numLabels = trainingList.getAttributes().getLabel().getMapping().size();

    cachedGradient = new double[(numFeatures + 1) * numLabels];
    cachedValue = 0;
       
    if (initClassifier != null) {
      this.parameters = initClassifier.parameters;
      this.classifier = initClassifier;
    }
    else {
      this.parameters = new double[(numFeatures + 1) * numLabels];
      this.classifier = new MaxEnt(trainingList,parameters);
    }
    
     this.constraints = constraints;
     
     for (MaxEntGEConstraint constraint : constraints) {
       constraint.preProcess(trainingList);
     }
  } 
  
  /**
   * Sets the variance for Gaussian prior or
   * equivalently the inverse of the weight 
   * of the L2 regularization term.
   * 
   * @param variance Gaussian prior variance.
   */
  public void setGaussianPriorVariance(double variance) {
    this.gaussianPriorVariance = variance;
  }
  
  
  /**
   * Model probabilities are raised to the power 1/temperature and 
   * renormalized. As the temperature decreases, model probabilities 
   * approach 1 for the maximum probability class, and 0 for other classes. 
   * 
   * DEFAULT: 1  
   * 
   * @param temp Temperature.
   */
  public void setTemperature(double temp) {
    this.temperature = temp;
  }
  
  /**
   * The weight of GE term in the objective function.
   * 
   * @param weight GE term weight.
   */
  public void setWeight(double weight) {
    this.objWeight = weight;
  }
  
  public MaxEnt getClassifier() {
    return classifier;
  }

  public double getValue() {
    if (!cacheStale) {
      return cachedValue;
    }
    
    if (objWeight == 0) {
      return 0.0;
    }
    
    for (MaxEntGEConstraint constraint : constraints) {
      constraint.zeroExpectations();
    }
    
    Arrays.fill(cachedGradient,0);

    int numFeatures = trainingList.getAttributes().getLabel().getMapping().size() + 1;
    int numLabels = trainingList.getAttributes().getLabel().getMapping().size();

    double[][] scores = new double[trainingList.size()][numLabels];
    double[] constraintValue = new double[numLabels];
    
    // pass 1: calculate model distribution
    for (int ii = 0; ii < trainingList.size(); ii++) {
      Example example = trainingList.getExample(ii);
      double instanceWeight = 1.0;
      if (example.getAttributes().getRole("weight") != null) {
        instanceWeight = example.getWeight();
      }
      // skip if labeled
      if (!Double.isNaN(example.getLabel())) {
        continue;
      }
      classifier.getClassificationScoresWithTemperature(example, temperature, scores[ii]);
      for (MaxEntGEConstraint constraint : constraints) {
        constraint.computeExpectations(example,scores[ii],instanceWeight);
      }
    }
    
    // compute value
    double value = 0;
    for (MaxEntGEConstraint constraint : constraints) {
      value += constraint.getValue();
    }
    value *= objWeight;

    // pass 2: determine per example gradient
    for (int ii = 0; ii < trainingList.size(); ii++) {
      Example example = trainingList.getExample(ii);
      
      // skip if labeled
      if (!Double.isNaN(example.getLabel())) {
        continue;
      }
      
      Arrays.fill(constraintValue,0);
      double instanceExpectation = 0;
      double instanceWeight = 1.0;
      if (example.getAttributes().getRole("weight") != null) {
        instanceWeight = example.getWeight();
      }

      for (MaxEntGEConstraint constraint : constraints) {
        constraint.preProcess(example);
        for (int label = 0; label < numLabels; label++) {
          double val = constraint.getCompositeConstraintFeatureValue(example, label);
          constraintValue[label] += val; 
          instanceExpectation += val * scores[ii][label];
        }
      }

      for (int label = 0; label < numLabels; label++) {
        if (scores[ii][label] == 0) continue;
        assert (!Double.isInfinite(scores[ii][label]));
        double weight = objWeight * instanceWeight * scores[ii][label] * (constraintValue[label] - instanceExpectation) / temperature;
        assert(!Double.isNaN(weight));
        MatrixOps.rowPlusEquals(cachedGradient, numFeatures, label, example, weight);
        cachedGradient[numFeatures * label + defaultFeatureIndex] += weight;
      }  
    }

    cachedValue = value;
    cacheStale = false;
    
    double reg = getRegularization();
    progressLogger.info ("Value (GE=" + value + " Gaussian prior= " + reg + ") = " + cachedValue);
    
    return cachedValue;
  }

  protected double getRegularization() {
    double regularization = 0;
    for (int pi = 0; pi < parameters.length; pi++) {
      double p = parameters[pi];
      regularization -= p * p / (2 * gaussianPriorVariance);
      cachedGradient[pi] -= p / gaussianPriorVariance;
    }
    cachedValue += regularization;
    return regularization;
  }
  
  public void getValueGradient(double[] buffer) {
    if (cacheStale) {
      getValue();  
    }
    assert(buffer.length == cachedGradient.length);
    System.arraycopy (cachedGradient, 0, buffer, 0, buffer.length);
  }

  public int getNumParameters() {
    return parameters.length;
  }

  public double getParameter(int index) {
    return parameters[index];
  }

  public void getParameters(double[] buffer) {
    assert(buffer.length == parameters.length);
    System.arraycopy (parameters, 0, buffer, 0, buffer.length);
  }

  public void setParameter(int index, double value) {
    cacheStale = true;
    parameters[index] = value;
  }

  public void setParameters(double[] params) {
    assert(params.length == parameters.length);
    cacheStale = true;
    System.arraycopy (params, 0, parameters, 0, parameters.length);
  }
}