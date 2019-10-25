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

import java.util.*;
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 * Expectation constraint for use with GE.
 * Penalizes L_2^2 difference from zero-penalty region [lower,upper]. 
 * 
 * Multiple constraints are grouped together here
 * to make things more efficient.
 * 
 */

public class MaxEntRangeL2FLGEConstraints implements MaxEntGEConstraint {

  // maps between input feature indices and constraints
  
  private boolean useValues;
  private boolean normalize;
  
  private int numFeatures;
  private int numLabels;
  
  protected TIntObjectHashMap<MaxEntL2IndGEConstraint> constraints;
  
  // cache of set of constrained features that fire at last FeatureVector
  // provided in preprocess call
  protected TIntArrayList indexCache;
  protected TDoubleArrayList valueCache;

  public MaxEntRangeL2FLGEConstraints(int numFeatures, int numLabels, boolean useValues, boolean normalize) {
    this.numFeatures = numFeatures;
    this.numLabels = numLabels;
    this.useValues = useValues;
    this.normalize = normalize;
    this.constraints = new TIntObjectHashMap<MaxEntL2IndGEConstraint>();
    this.indexCache = new TIntArrayList();
    this.valueCache = new TDoubleArrayList();
  }
  
  public void addConstraint(int fi, int li, double lower, double upper, double weight) {
    if (!constraints.containsKey(fi)) {
      constraints.put(fi,new MaxEntL2IndGEConstraint());
    }
    constraints.get(fi).add(li, lower, upper, weight);
  }

  public BitSet preProcess(ExampleSet examples) {
    // count
    int ii = 0;
    int fi;
    BitSet bitSet = new BitSet(examples.size());
    List<Attribute> regularAttributes = Arrays.asList(examples.getAttributes().createRegularAttributeArray());

    for (int index = 0;index < examples.size();index++) {
      Example example = examples.getExample(index);
      double weight = 1.0;
      if (example.getAttributes().getRole("weight") != null) {
        weight = example.getWeight();
      }
      for (int loc = 0; loc < regularAttributes.size(); loc++) {
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
    int fi;
    // cache constrained input features
    List<Attribute> regularAttributes = Arrays.asList(example.getAttributes().createRegularAttributeArray());
    for (int loc = 0; loc < regularAttributes.size(); loc++) {
      if (constraints.containsKey(loc)) {
        indexCache.add(loc);
        if (useValues) valueCache.add(example.getValue(regularAttributes.get(loc)));
      }
    }
    
    // default feature, for label regularization
    if (constraints.containsKey(numFeatures)) {
      indexCache.add(numFeatures);
      if (useValues) valueCache.add(1);
    }
  }
  
  public double getCompositeConstraintFeatureValue(Example example, int label) {
    double value = 0;
    for (int i = 0; i < indexCache.size(); i++) {
      if (useValues) {
        value += constraints.get(indexCache.getQuick(i)).getGradientContribution(label) * valueCache.getQuick(i);
      }
      else {
        value += constraints.get(indexCache.getQuick(i)).getGradientContribution(label);
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

  public double getValue() {
    double value = 0.0;
    for (int fi : constraints.keys()) {
      MaxEntL2IndGEConstraint constraint = constraints.get(fi);
      if ( constraint.count > 0.0) {
        // value due to current constraint
        for (int labelIndex = 0; labelIndex < numLabels; ++labelIndex) {
          value -= constraint.getValue(labelIndex);
        }
      }
    }
    assert(!Double.isNaN(value) && !Double.isInfinite(value));
    return value;
  }

  public void zeroExpectations() {
    for (int fi : constraints.keys()) {
      constraints.get(fi).expectation = new double[constraints.get(fi).getNumConstrainedLabels()];
    }
  }
  
  protected class MaxEntL2IndGEConstraint {
    
    protected int index;
    protected double count;
    protected ArrayList<Double> lower;
    protected ArrayList<Double> upper;
    protected ArrayList<Double> weights;
    protected HashMap<Integer,Integer> labelMap;
    protected double[] expectation;

    public MaxEntL2IndGEConstraint() {
      lower = new ArrayList<Double>();
      upper = new ArrayList<Double>();
      weights = new ArrayList<Double>();
      labelMap = new HashMap<Integer,Integer>();
      index = 0;
      count = 0;
    }
    
    public void add(int label, double lower, double upper, double weight) {
      this.lower.add(lower);
      this.upper.add(upper);
      this.weights.add(weight);
      labelMap.put(label, index);
      index++;
    }
    
    public void incrementExpectation(int li, double value) {
      if (labelMap.containsKey(li)) {
        int i = labelMap.get(li);
        expectation[i] += value;
      }
    }
    
    public double getValue(int li) {
      if (labelMap.containsKey(li)) {
        int i = labelMap.get(li);
        assert(this.count != 0);
        
        double ex;
        if (normalize) {
          ex = this.expectation[i] / this.count;
        }
        else {
          ex = this.expectation[i];
        }

        if (ex < lower.get(i)) {
          return weights.get(i) * Math.pow(lower.get(i) - ex,2);
        }
        else if (ex > upper.get(i)) {
          return weights.get(i) * Math.pow(upper.get(i) - ex,2);         
        }
      }
      return 0;
    }
    
    public int getNumConstrainedLabels() {
      return index;
    }
    
    public double getGradientContribution(int li) {
      if (labelMap.containsKey(li)) {
        int i = labelMap.get(li);
        assert(this.count != 0);
        
        if (normalize) {
          double ex = this.expectation[i] / this.count;
          if (ex < lower.get(i)) {
            return 2 * weights.get(i) * (lower.get(i) / count - expectation[i] / (count * count));
          }
          else if (ex > upper.get(i)) {
            return 2 * weights.get(i) * (upper.get(i) / count - expectation[i] / (count * count));          
          }
        }
        else {
          double ex = this.expectation[i];
          if (ex < lower.get(i)) {
            return 2 * weights.get(i) * (lower.get(i) - expectation[i]);
          }
          else if (ex > upper.get(i)) {
            return 2 * weights.get(i) * (upper.get(i) - expectation[i]);          
          }
        }
      }
      return 0;
    }
  }
}