/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.maxent;

import base.operators.example.ExampleSet;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.learner.AbstractLearner;
import base.operators.operator.learner.maxent.optimize.*;
import base.operators.parameter.*;
import base.operators.tools.RandomGenerator;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

//Does not currently handle instances that are labeled with distributions
//instead of a single label.
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:The trainer for a Maximum Entropy classifier.
 */

public class MaxEntTrainer extends AbstractLearner implements Serializable {

	private static Logger logger = Logger.getLogger(MaxEntTrainer.class.getName());
	private static Logger progressLogger = Logger.getLogger(MaxEntTrainer.class.getName()+"-pl");

	public static final String GAUSSIAN_PRIOR_VARIANCE = "gaussian_prior_variance";
	public static final String L1_WEIGHT = "l1_weight";
	public static final String NUM_ITERATIONS = "num_iterations";
	public static final String USING_HYPERBOLIC_PRIOR = "using_hyperbolic_prior";
	public static final String USING_GAUSSIAN_PRIOR = "using_gaussian_prior";
	public static final String HYPERBOLIC_PRIOR_SLOPE = "hyperbolic_prior_slope";
	public static final String HYPERBOLIC_PRIOR_SHARPNESS = "hyperbolic_prior_sharpness";



	double gaussianPriorVariance;
	double l1Weight;
	int numIterations;

	// xxx Why does TestMaximizable fail when this variance is very small?
	static final Class DEFAULT_MAXIMIZER_CLASS = LimitedMemoryBFGS.class;
	Class maximizerClass = DEFAULT_MAXIMIZER_CLASS;

	ExampleSet trainingSet = null;
	MaxEnt initialClassifier;

	MaxEntOptimizableByLabelLikelihood optimizable = null;
	Optimizer optimizer = null;
	boolean finishedTraining = false;

	public MaxEntTrainer(OperatorDescription description){
		super(description);
	}

	public MaxEnt getClassifier () {
		if (optimizable != null)
			return optimizable.getClassifier();
		return initialClassifier;
	}

	/**
	 *  Initialize parameters using the provided classifier.
	 */
	public void setClassifier (MaxEnt theClassifierToTrain) {
		// Is this necessary?  What is the caller is about to set the training set to something different? -akm
		//这条判断先去掉，未改造
		//assert (trainingSet == null || Alphabet.alphabetsMatch(theClassifierToTrain, trainingSet));
		if (this.initialClassifier != theClassifierToTrain) {
			this.initialClassifier = theClassifierToTrain;
			optimizable = null;
			optimizer = null;
		}
	}

	//
	//  OPTIMIZABLE OBJECT: implements value and gradient functions
	//

	public Optimizable getOptimizable () {
		return optimizable;
	}

	public MaxEntOptimizableByLabelLikelihood getOptimizable (ExampleSet trainingSet) {
		return getOptimizable(trainingSet, getClassifier());
	}

	public MaxEntOptimizableByLabelLikelihood getOptimizable (ExampleSet trainingSet, MaxEnt initialClassifier) {

		if (trainingSet != this.trainingSet || this.initialClassifier != initialClassifier) {

			this.trainingSet = trainingSet;
			this.initialClassifier = initialClassifier;

			if (optimizable == null || optimizable.trainingList != trainingSet) {
				optimizable = new MaxEntOptimizableByLabelLikelihood (trainingSet, initialClassifier);
				boolean usingHyperbolicPrior = false;
				boolean usingGaussianPrior = true;
				double hyperbolicPriorSlope = 0.2;
				double hyperbolicPriorSharpness = 10.0;
				try {
					usingHyperbolicPrior = getParameterAsBoolean(USING_HYPERBOLIC_PRIOR);
					usingGaussianPrior = getParameterAsBoolean(USING_GAUSSIAN_PRIOR);
					hyperbolicPriorSlope = getParameterAsDouble(HYPERBOLIC_PRIOR_SLOPE);
					hyperbolicPriorSharpness = getParameterAsDouble(HYPERBOLIC_PRIOR_SHARPNESS);
				} catch (UndefinedParameterError undefinedParameterError) {
					undefinedParameterError.printStackTrace();
				}
				if(usingHyperbolicPrior){
					optimizable.setHyperbolicPriorSharpness(hyperbolicPriorSharpness);
					optimizable.setHyperbolicPriorSlope(hyperbolicPriorSlope);

				}else if(usingGaussianPrior){
					optimizable.setGaussianPriorVariance(gaussianPriorVariance);
				}
				if (l1Weight == 0.0) {
					optimizable.setGaussianPriorVariance(gaussianPriorVariance);
				}
				else {
					// the prior term for L1-regularized classifiers
					//  is implemented as part of the optimizer,
					//  so don't include a prior calculation in the value and
					//  gradient functions.
					optimizable.useNoPrior();
				}

				optimizer = null;
			}
		}

		return optimizable;
	}

	//
	//  OPTIMIZER OBJECT: maximizes value function
	//

	public Optimizer getOptimizer () {
		if (optimizer == null && optimizable != null) {
			optimizer = new ConjugateGradient(optimizable);
		}

		return optimizer;
	}

	/** This method is called by the train method.
	 *   This is the main entry point for the optimizable and optimizer
	 *   compontents.
	 */
	public Optimizer getOptimizer (ExampleSet trainingSet) {

		// If the data is not set, or has changed,
		// initialize the optimizable object and
		// replace the optimizer.
		if (trainingSet != this.trainingSet ||
				optimizable == null) {
			getOptimizable(trainingSet);
			optimizer = null;
		}

		// Build a new optimizer
		if (optimizer == null) {
			// If l1Weight is 0, this devolves to
			//  standard L-BFGS, but the implementation
			//  may be faster.
			optimizer = new LimitedMemoryBFGS(optimizable);
			//OrthantWiseLimitedMemoryBFGS(optimizable, l1Weight);
		}
		return optimizer;
	}

	/**
	 * Specifies the maximum number of iterations to run during a single call
	 * to <code>train</code> or <code>trainWithFeatureInduction</code>.  Not
	 * currently functional.
	 * @return This trainer
	 */
	// XXX Since we maximize before using numIterations, this doesn't work.
	// Is that a bug?  If so, should the default numIterations be higher?
	public MaxEntTrainer setNumIterations (int i) {
		numIterations = i;
		return this;
	}

	public int getIteration () {
		if (optimizable == null)
			return 0;
		else
			return Integer.MAX_VALUE;
//			return optimizer.getIteration ();
	}

	/**
	 * Sets a parameter to prevent overtraining.  A smaller variance for the prior
	 * means that feature weights are expected to hover closer to 0, so extra
	 * evidence is required to set a higher weight.
	 * @return This trainer
	 */
	public MaxEntTrainer setGaussianPriorVariance (double gaussianPriorVariance) {
		this.gaussianPriorVariance = gaussianPriorVariance;
		return this;
	}

	/**
	 *  Use an L1 prior. Larger values mean parameters will be closer to 0.
	 *   Note that this setting overrides any Gaussian prior.
	 */
	public MaxEntTrainer setL1Weight(double l1Weight) {
		this.l1Weight = l1Weight;
		return this;
	}

	public MaxEnt learn (ExampleSet trainingSet) {
		try {
			gaussianPriorVariance = getParameterAsDouble(GAUSSIAN_PRIOR_VARIANCE);
			l1Weight = getParameterAsDouble(L1_WEIGHT);
			numIterations = getParameterAsInt(NUM_ITERATIONS);
		} catch (UndefinedParameterError undefinedParameterError) {
			undefinedParameterError.printStackTrace();
		}
		return learn (trainingSet, numIterations);
	}

	public MaxEnt learn (ExampleSet trainingSet, int numIterations)
	{
		logger.fine ("trainingSet.size() = "+trainingSet.size());
		getOptimizer (trainingSet);  // This will set this.optimizer, this.optimizable

		for (int i = 0; i < numIterations; i++) {
			try {
				finishedTraining = optimizer.optimize (1);
			} catch (InvalidOptimizableException e) {
				e.printStackTrace();
				logger.warning("Catching InvalidOptimizatinException! saying converged.");
				finishedTraining = true;
			} catch (OptimizationException e) {
				e.printStackTrace();
				logger.info ("Catching OptimizationException; saying converged.");
				finishedTraining = true;
			}
			if (finishedTraining)
				break;
		}

		// only if any number of iterations is allowed
		if (numIterations == Integer.MAX_VALUE) {
			// Run it again because in our and Sam Roweis' experience, BFGS can still
			// eke out more likelihood after first convergence by re-running without
			// being restricted by its gradient history.
			optimizer = null;
			getOptimizer(trainingSet);
			try {
				finishedTraining = optimizer.optimize ();
			} catch (InvalidOptimizableException e) {
				e.printStackTrace();
				logger.warning("Catching InvalidOptimizatinException! saying converged.");
				finishedTraining = true;
			} catch (OptimizationException e) {
				e.printStackTrace();
				logger.info ("Catching OptimizationException; saying converged.");
				finishedTraining = true;
			}
		}
		//TestMaximizable.testValueAndGradientCurrentParameters (mt);
		progressLogger.info("\n"); //  progress messages are on one line; move on.
		//logger.info("MaxEnt ngetValueCalls:"+getValueCalls()+"\nMaxEnt ngetValueGradientCalls:"+getValueGradientCalls());
		return optimizable.getClassifier();
	}

	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(NUM_ITERATIONS,
				"The paramter of num_iterations.", 1, Integer.MAX_VALUE,
				Integer.MAX_VALUE, false));

		types.add(new ParameterTypeDouble(L1_WEIGHT,
				"The paramter of l1_weight.", 0.0, Double.MAX_VALUE,
				0.0, false));

		types.add(new ParameterTypeBoolean(USING_GAUSSIAN_PRIOR, "The paramter of using_gaussian_prior.", true));

		types.add(new ParameterTypeDouble(GAUSSIAN_PRIOR_VARIANCE, "The paramter of gaussian_prior_variance.", 0.0001, Double.MAX_VALUE,
				1.0, false));

		types.add(new ParameterTypeBoolean(USING_HYPERBOLIC_PRIOR, "The paramter of using_hperbolic_prior.", false));

		types.add(new ParameterTypeDouble(HYPERBOLIC_PRIOR_SLOPE, "The paramter of hyperbolic_prior_slope.", 0.0001, Double.MAX_VALUE,
				0.2, false));

		types.add(new ParameterTypeDouble(HYPERBOLIC_PRIOR_SHARPNESS, "The paramter of hyperbolic_prior_sharpness.", 1.0, Double.MAX_VALUE,
				10.0, false));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
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


}
