/* Copyright (C) 2011 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.maxent.constraint.pr;

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
 * description:Abstract expectation constraint for use with Posterior Regularization (PR).
 * 
 */

public abstract class MaxEntFLPRConstraints implements MaxEntPRConstraint {
  protected boolean useValues;
  protected int numFeatures;
  protected int numLabels;
  
  // maps between input feature indices and constraints
  protected TIntObjectHashMap<MaxEntFLPRConstraint> constraints;
  
  // cache of set of constrained features that fire at last FeatureVector
  // provided in preprocess call
  protected TIntArrayList indexCache;
  protected TDoubleArrayList valueCache;

  public MaxEntFLPRConstraints(int numFeatures, int numLabels, boolean useValues) {
    this.useValues = useValues;
    this.numFeatures = numFeatures;
    this.numLabels = numLabels;
    this.constraints = new TIntObjectHashMap<MaxEntFLPRConstraint>();
    this.indexCache = new TIntArrayList();
    this.valueCache = new TDoubleArrayList();
  }

  public abstract void addConstraint(int fi, double[] ex, double weight);
  
  public void incrementExpectations(Example example, double[] dist, double weight) {
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

  public BitSet preProcess(ExampleSet data) {
    // count
    int ii = 0;
    List<Attribute> regularAttributes = Arrays.asList(data.getAttributes().createRegularAttributeArray());
    BitSet bitSet = new BitSet(data.size());
    for (int index = 0; index < data.size(); index++) {
      Example example = data.getExample(index);
      double weight = 1.0;
      if (example.getAttributes().getRole("weight") != null) {
        weight = example.getWeight();
      }
      int[] keys = constraints.keys();
      for (int loc = 0; loc < regularAttributes.size(); loc++) {
        if(example.getValue(regularAttributes.get(loc))!=0){
          if (constraints.containsKey(loc)) {
            if (useValues) {
              constraints.get(loc).count += weight * example.getValue(regularAttributes.get(loc));
            }
            else {
              constraints.get(loc).count += weight;
            }
            bitSet.set(ii);
          }
        }
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

  public void preProcess(Example input) {
    indexCache.resetQuick();
    if (useValues) valueCache.resetQuick();
    int fi;
    List<Attribute> regularAttributes = Arrays.asList(input.getAttributes().createRegularAttributeArray());
    // cache constrained input features
    for (int loc = 0; loc < regularAttributes.size(); loc++) {
      if(input.getValue(regularAttributes.get(loc))!=0 && constraints.containsKey(loc)){
        indexCache.add(loc);
        if (useValues) valueCache.add(input.getValue(regularAttributes.get(loc)));
      }
    }
    
    // default feature, for label regularization
    if (constraints.containsKey(numFeatures)) {
      indexCache.add(numFeatures);
      if (useValues) valueCache.add(1);
    }
  }

  protected abstract class MaxEntFLPRConstraint {
    
    protected double count;
    protected double weight;
    protected double[] target;
    protected double[] expectation;
    
    public MaxEntFLPRConstraint(double[] target, double weight) {
      this.count = 0;
      this.weight = weight;
      this.target = target;
      this.expectation = null;
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
  }
}
