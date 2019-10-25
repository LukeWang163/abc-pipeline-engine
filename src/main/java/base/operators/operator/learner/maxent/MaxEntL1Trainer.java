package base.operators.operator.learner.maxent;

import base.operators.example.ExampleSet;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.learner.AbstractLearner;
import base.operators.operator.learner.maxent.optimize.*;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.RandomGenerator;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 */

public class MaxEntL1Trainer extends AbstractLearner implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(MaxEntL1Trainer.class.getName());
	private static Logger progressLogger = Logger.getLogger(MaxEntL1Trainer.class.getName()+"-pl");


	public static final String GAUSSIAN_PRIOR_VARIANCE = "gaussian_prior_variance";
	public static final String NUM_ITERATIONS = "num_iterations";
	public static final String L1_WEIGHT_PARAM = "l1_weight_param";

	static final double DEFAULT_GAUSSIAN_PRIOR_VARIANCE = 1;
	static final double DEFAULT_L1_WEIGHT_PARAM = 1.0;

	double gaussianPriorVariance = DEFAULT_GAUSSIAN_PRIOR_VARIANCE;
	double l1_weight_param = DEFAULT_L1_WEIGHT_PARAM;
	int numIterations = Integer.MAX_VALUE;

	ExampleSet trainingSet = null;
	MaxEnt initialClassifier;

	MaxEntOptimizableByLabelLikelihood optimizable = null;
	Optimizer optimizer = null;
	boolean finishedTraining = false;
	public MaxEntL1Trainer(OperatorDescription operatorDescription)
	{
		super(operatorDescription);
	}

	public MaxEnt learn (ExampleSet trainingSet) {
		try {
			gaussianPriorVariance = getParameterAsDouble(GAUSSIAN_PRIOR_VARIANCE);
			l1_weight_param = getParameterAsDouble(L1_WEIGHT_PARAM);
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
	public MaxEnt getClassifier () {
		if (optimizable != null)
			return optimizable.getClassifier();
		return initialClassifier;
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

				if (l1_weight_param == 0.0) {
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


	public Optimizer getOptimizer() {
		if (optimizer == null && optimizable != null)
			optimizer = new OrthantWiseLimitedMemoryBFGS(optimizable, l1_weight_param);
		return optimizer;
	}

	// commented by Limin Yao, use L1 regularization instead
	public Optimizer getOptimizer(ExampleSet trainingSet) {
		if (trainingSet != this.trainingSet || optimizable == null) {
			getOptimizable(trainingSet);
			optimizer = null;
		}
		if (optimizer == null)
			optimizer = new OrthantWiseLimitedMemoryBFGS(optimizable, l1_weight_param);
		return optimizer;
	}

	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeDouble(GAUSSIAN_PRIOR_VARIANCE, "The paramter of gaussian_prior_variance.", 0.0001, Double.MAX_VALUE,
				1.0, false));
		types.add(new ParameterTypeDouble(L1_WEIGHT_PARAM,
				"The paramter of l1_weight_param.", 0.0, Double.MAX_VALUE,
				1.0, false));
		types.add(new ParameterTypeInt(NUM_ITERATIONS,
				"The paramter of num_iterations.", 1, Integer.MAX_VALUE,
				Integer.MAX_VALUE, false));

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
