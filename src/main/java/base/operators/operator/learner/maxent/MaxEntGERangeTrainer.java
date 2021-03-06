/* Copyright (C) 2011 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.maxent;

import base.operators.example.ExampleSet;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.learner.maxent.constraint.ge.MaxEntGEConstraint;
import base.operators.operator.learner.maxent.constraint.ge.MaxEntRangeL2FLGEConstraints;
import base.operators.operator.learner.maxent.constraintslearner.MaxEntAbstractLearner;
import base.operators.operator.learner.maxent.optimize.LimitedMemoryBFGS;
import base.operators.operator.learner.maxent.optimize.Optimizable;
import base.operators.operator.learner.maxent.optimize.Optimizer;
import base.operators.operator.nio.file.SimpleFileObject;
import base.operators.parameter.*;
import base.operators.tools.RandomGenerator;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Better explanations of parameters is given in MaxEntOptimizableByGE
 */

public class MaxEntGERangeTrainer  extends MaxEntAbstractLearner implements Serializable{

  private static final long serialVersionUID = 1L;
  private static Logger logger = Logger.getLogger(MaxEntGERangeTrainer.class.getName());
  private static Logger progressLogger = Logger.getLogger(MaxEntGERangeTrainer.class.getName()+"-pl");

  // these are for using this code from the command line
  public static final String NORMALIZE = "normalize";
  public static final String USE_VALUES = "use_values";
  public static final String NUM_ITERATIONS = "num_iterations";
  public static final String TEMPERATURE = "temperature";
  public static final String GAUSSIAN_PRIOR_VARIANCE = "gaussian_prior_variance";


  private boolean normalize;
  private boolean useValues;
  private int numIterations;
  private double temperature;
  private double gaussianPriorVariance;

  private int allNumIterations = 0;
  private File constraintsFile;
  protected ArrayList<MaxEntGEConstraint> constraints;
  private ExampleSet trainingList = null;
  private MaxEnt classifier = null;
  private MaxEntOptimizableByGE ge = null;
  private Optimizer opt = null;

  public MaxEntGERangeTrainer(OperatorDescription description) {super(description);}

  public void setConstraintsFile(File file) {
    this.constraintsFile = file;
  }

  public int getIteration () {
    return allNumIterations;
  }

  public void setTemperature(double temp) {
    this.temperature = temp;
  }
  
  public void setGaussianPriorVariance(double variance) {
    this.gaussianPriorVariance = variance;
  }
  
  public MaxEnt getClassifier () {
    return classifier;
  }

  public void setUseValues(boolean flag) {
    this.useValues = flag;
  }
  
  public void setNormalize(boolean normalize) {
    this.normalize = normalize;
  }
  
  public Optimizable.ByGradientValue getOptimizable (ExampleSet trainingList) {
    if (ge == null) {
      ge = new MaxEntOptimizableByGE(trainingList,constraints,classifier);
      ge.setTemperature(temperature);
      ge.setGaussianPriorVariance(gaussianPriorVariance);
    }
    return ge;
  }

  public Optimizer getOptimizer () {
    getOptimizable(trainingList);
    if (opt == null) {
      opt = new LimitedMemoryBFGS(ge);
    }
    return opt;
  }
  
  public void setOptimizer(Optimizer opt) { 
    this.opt = opt;
  }

  public MaxEnt learn (ExampleSet trainingList, SimpleFileObject simpleFileObject) {

    try {
      normalize = getParameterAsBoolean(NORMALIZE);
      useValues = getParameterAsBoolean(USE_VALUES);
      numIterations = getParameterAsInt(NUM_ITERATIONS);
      temperature = getParameterAsDouble(TEMPERATURE);
      gaussianPriorVariance = getParameterAsDouble(GAUSSIAN_PRIOR_VARIANCE);
    } catch (UndefinedParameterError undefinedParameterError) {
      undefinedParameterError.printStackTrace();
    }
    return learn (trainingList, numIterations, simpleFileObject);
  }

  public MaxEnt learn (ExampleSet train, int iterations, SimpleFileObject simpleFileObject) {
    trainingList = train;
    constraintsFile = simpleFileObject.getFile();
    if (constraints == null && constraintsFile != null) {
      HashMap<Integer,double[][]> constraintsMap = 
        FeatureConstraintUtil.readRangeConstraintsFromFile(constraintsFile, trainingList);

      logger.info("number of constraints: " + constraintsMap.size());
      constraints = new ArrayList<MaxEntGEConstraint>();
      int regular_attributes_size = train.getAttributes().createRegularAttributeArray().length;
      MaxEntRangeL2FLGEConstraints geConstraints = new MaxEntRangeL2FLGEConstraints(regular_attributes_size,
        train.getAttributes().getLabel().getMapping().size(),useValues,normalize);
      for (int fi : constraintsMap.keySet()) {
        double[][] dist = constraintsMap.get(fi);
        for (int li = 0; li < dist.length; li++) {
          if (!Double.isInfinite(dist[li][0])) {
            geConstraints.addConstraint(fi, li, dist[li][0], dist[li][1], 1);
          }
        }
      }
      constraints.add(geConstraints);
    }
    
    getOptimizable(trainingList);
    getOptimizer();
    
    if (opt instanceof LimitedMemoryBFGS) {
      ((LimitedMemoryBFGS)opt).reset();
    }    
    
    logger.fine ("trainingList.size() = "+trainingList.size());

    try {
      opt.optimize(iterations);
      allNumIterations += iterations;
    } catch (Exception e) {
      e.printStackTrace();
      logger.info ("Catching exception; saying converged.");
    }

    if (iterations == Integer.MAX_VALUE && opt instanceof LimitedMemoryBFGS) {
      // Run it again because in our and Sam Roweis' experience, BFGS can still
      // eke out more likelihood after first convergence by re-running without
      // being restricted by its gradient history.
      ((LimitedMemoryBFGS)opt).reset();
      try {
        opt.optimize(iterations);
        allNumIterations += iterations;
      } catch (Exception e) {
        e.printStackTrace();
        logger.info ("Catching exception; saying converged.");
      }
    }
    progressLogger.info("\n"); //  progress messages are on one line; move on.
    
    classifier = ge.getClassifier();
    return classifier;
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


  public List<ParameterType> getParameterTypes() {
    List<ParameterType> types = super.getParameterTypes();
    types.add(new ParameterTypeBoolean(NORMALIZE, "The paramter of normalize.", true));
    types.add(new ParameterTypeBoolean(USE_VALUES, "The paramter of use_values.", false));
    types.add(new ParameterTypeInt(NUM_ITERATIONS, "The paramter of num_iterations.", 1, Integer.MAX_VALUE,
            Integer.MAX_VALUE, false));
    types.add(new ParameterTypeDouble(TEMPERATURE, "The paramter of temperature.", 0, Double.MAX_VALUE,
            1, false));
    types.add(new ParameterTypeDouble(GAUSSIAN_PRIOR_VARIANCE, "The paramter of gaussian_prior_variance.", 0.0001, Double.MAX_VALUE,
            1, false));

    types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
    return types;
  }
}