/* Copyright (C) 2011 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.maxent.constraint.ge;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:Abstract expectation constraint for use with Generalized Expectation (GE).
 */

public abstract class MaxEntFLGEConstraints implements MaxEntGEConstraint {

  protected boolean useValues;
  protected int numLabels;
  protected int numFeatures;
  
  // maps between input feature indices and constraints
  protected TIntObjectHashMap<MaxEntFLGEConstraint> constraints;
  
  // cache of set of constrained features that fire at last FeatureVector
  // provided in preprocess call
  protected TIntArrayList indexCache;
  protected TDoubleArrayList valueCache;
  
  public MaxEntFLGEConstraints(int numFeatures, int numLabels, boolean useValues) {
    this.numFeatures = numFeatures;
    this.numLabels = numLabels;
    this.useValues = useValues;
    this.constraints = new TIntObjectHashMap<MaxEntFLGEConstraint>();
    this.indexCache = new TIntArrayList();
    this.valueCache = new TDoubleArrayList();
  }

  public abstract void addConstraint(int fi, double[] ex, double weight);
  
  public double getCompositeConstraintFeatureValue(Example example, int label) {
    double value = 0;
    for (int i = 0; i < indexCache.size(); i++) {
      if (useValues) {
        value += constraints.get(indexCache.getQuick(i)).getValue(label) * valueCache.getQuick(i);
      }
      else {
        value += constraints.get(indexCache.getQuick(i)).getValue(label);
      }
    }
    return value;
  }

  public void computeExpectations(Example example, double[] dist, double weight) {
    preProcess(example);
    for (int li = 0; li < numLabels; li++) {
      double p = weight * dist[li];
      for (int i = 0; i < indexCache.size(); i++) {
        if (useValues) {
          constraints.get(indexCache.getQuick(i)).expectation[li] += p * valueCache.getQuick(i); 
        }
        else {
          constraints.get(indexCache.getQuick(i)).expectation[li] += p; 
        }
      }
    }
  }

  public void zeroExpectations() {
    for (int fi : constraints.keys()) {
      constraints.get(fi).expectation = new double[numLabels];
    }
  }

  public BitSet preProcess(ExampleSet examples) {
    // count
    int ii = 0;
    BitSet bitSet = new BitSet(examples.size());
    List<Attribute> regularAttributes = Arrays.asList(examples.getAttributes().createRegularAttributeArray());
    for (int index = 0; index < examples.size(); index++) {
      Example example = examples.getExample(index);
      double weight = 1.0;
      if (example.getAttributes().getRole("weight") != null) {
        weight = example.getWeight();
      }
      for (int loc = 0; loc < regularAttributes.size(); loc++) {
        if (useValues) {
          constraints.get(loc).count += weight * example.getValue(regularAttributes.get(loc));
        }
        else {
          constraints.get(loc).count += weight;
        }
        bitSet.set(ii);
      }
      ii++;
      // default feature, for label regularization
      if (constraints.containsKey(numFeatures)) {
        bitSet.set(ii);
        constraints.get(numFeatures).count += weight; 
      }
    }
    return bitSet;
  }

  public void preProcess(Example example) {
    indexCache.resetQuick();
    if (useValues) valueCache.resetQuick();
    // cache constrained input features
    List<Attribute> regularAttributes = Arrays.asList(example.getAttributes().createRegularAttributeArray());
    for (int loc = 0; loc < regularAttributes.size(); loc++) {
        indexCache.add(loc);
        if (useValues) valueCache.add(example.getValue(regularAttributes.get(loc)));
    }
    
    // default feature, for label regularization
    if (constraints.containsKey(numFeatures)) {
      indexCache.add(numFeatures);
      if (useValues) valueCache.add(1);
    }
  }

  protected abstract class MaxEntFLGEConstraint {
    
    protected double[] target;
    protected double[] expectation;
    protected double count;
    protected double weight;
    
    public MaxEntFLGEConstraint(double[] target, double weight) {
      this.target = target;
      this.weight = weight;
      this.expectation = null;
      this.count = 0;
    }
    
    public double[] getTarget() { 
      return target;
    }
    
    public double[] getExpectation() { 
      return expectation;
    }
    
    public double getCount() { 
      return count;
    }
    
    public double getWeight() { 
      return weight;
    }
    
    public abstract double getValue(int li);
  }
}
