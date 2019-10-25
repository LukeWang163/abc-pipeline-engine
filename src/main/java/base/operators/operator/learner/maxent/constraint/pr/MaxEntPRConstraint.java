/* Copyright (C) 2011 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.maxent.constraint.pr;

import base.operators.example.Example;
import base.operators.example.ExampleSet;

import java.util.BitSet;
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 * Interface for expectation constraints for use with Posterior Regularization (PR).
 * 
 */

public interface MaxEntPRConstraint {

  int numDimensions();
  
  double getScore(Example input, int label, double[] parameters);
  
  void incrementExpectations(Example fv, double[] dist, double weight);

  double getAuxiliaryValueContribution(double[] parameters);
  
  double getCompleteValueContribution();
  
  void getGradient(double[] parameters, double[] gradient);
  
  /**
   * Zero expectation values. Called before re-computing gradient.
   */
  void zeroExpectations();
  
  /**
   * @param data Unlabeled data
   * @return Returns a bitset of the size of the data, with the bit set if a constraint feature fires in that instance.
   */
  BitSet preProcess(ExampleSet data);
  
  /**
   * Gives the constraint the option to do some caching
   * using only the FeatureVector.  For example, the
   * constrained input features could be cached.
   * 
   * @param input FeatureVector input
   */
  void preProcess(Example input);
  
  /**
   * This is used in multi-threading.  
   * 
   * @return A copy of the GEConstraint.  
   */
}
