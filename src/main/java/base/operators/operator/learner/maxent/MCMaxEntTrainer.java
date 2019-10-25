/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.  For further
information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.maxent;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.learner.AbstractLearner;
import base.operators.operator.learner.maxent.optimize.LimitedMemoryBFGS;
import base.operators.operator.learner.maxent.optimize.MatrixOps;
import base.operators.operator.learner.maxent.optimize.Optimizable;
import base.operators.operator.learner.maxent.optimize.Optimizer;
import base.operators.operator.learner.maxent.utils.Maths;
import base.operators.parameter.*;
import base.operators.tools.RandomGenerator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

// Does not currently handle instances that are labeled with distributions
// instead of a single label.
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:The trainer for a Maximum Entropy classifier.
 */

public class MCMaxEntTrainer extends AbstractLearner implements Serializable //implements CommandOption.ListProviding
{
	private static Logger logger = Logger.getLogger(MCMaxEntTrainer.class.getName());
	private static Logger progressLogger = Logger.getLogger(MCMaxEntTrainer.class.getName()+"-pl");

	public static final String NUM_ITERATIONS = "num_iterations";
    public static final String USING_MULTI_CONDITIONAL_TRAINING = "using_multi_conditional_training";
    public static final String USING_HYPERBOLIC_PRIOR = "using_hyperbolic_prior";
    public static final String GAUSSIAN_PRIOR_VARIANCE = "gaussian_prior_variance";
	public static final String HYPERBOLIC_PRIOR_SLOPE = "hyperbolic_prior_slope";
    public static final String HYPERBOLIC_PRIOR_SHARPNESS = "hyperbolic_prior_sharpness";


	int numGetValueCalls = 0;
	int numGetValueGradientCalls = 0;

	public static final String EXP_GAIN = "exp";
	public static final String GRADIENT_GAIN = "grad";
	public static final String INFORMATION_GAIN = "info";

	// xxx Why does TestMaximizable fail when this variance is very small?
	static final Class DEFAULT_MAXIMIZER_CLASS = LimitedMemoryBFGS.class;

    int numIterations;
	// CPAL
	boolean usingMultiConditionalTraining;
	boolean usingHyperbolicPrior;
	double gaussianPriorVariance;
	double hyperbolicPriorSlope;
	double hyperbolicPriorSharpness;

	Class maximizerClass = DEFAULT_MAXIMIZER_CLASS;
	double generativeWeighting = 1.0;
	MaximizableTrainer mt;
	MCMaxEnt initialClassifier;

	/*
	public MCMaxEntTrainer(Maximizer.ByGradient maximizer)
	{
	this.maximizerByGradient = maximizer;
	this.usingHyperbolicPrior = false;
	}
	*/

	public MCMaxEntTrainer (OperatorDescription operatorDescription)
	{
		super(operatorDescription);
	}

	public Optimizable.ByGradientValue getMaximizableTrainer (ExampleSet examples)
	{
		if (examples == null)
			return new MaximizableTrainer ();
		return new MaximizableTrainer (examples, null);
	}

	/**
	 * Specifies the maximum number of iterations to run during a single call
	 * to <code>train</code> or <code>trainWithFeatureInduction</code>.  Not
	 * currently functional.
	 * @return This trainer
	 */
	// XXX Since we maximize before using numIterations, this doesn't work.
	// Is that a bug?  If so, should the default numIterations be higher?
	public MCMaxEntTrainer setNumIterations (int i)
	{
		numIterations = i;
		return this;
	}

	public MCMaxEntTrainer setUseHyperbolicPrior (boolean useHyperbolicPrior)
	{
		this.usingHyperbolicPrior = useHyperbolicPrior;
		return this;
	}

	/**
	 * Sets a parameter to prevent overtraining.  A smaller variance for the prior
	 * means that feature weights are expected to hover closer to 0, so extra
	 * evidence is required to set a higher weight.
	 * @return This trainer
	 */
	public MCMaxEntTrainer setGaussianPriorVariance (double gaussianPriorVariance)
	{
		this.usingHyperbolicPrior = false;
		this.gaussianPriorVariance = gaussianPriorVariance;
		return this;
	}

	public MCMaxEntTrainer setHyperbolicPriorSlope(double hyperbolicPriorSlope)
	{
		this.usingHyperbolicPrior = true;
		this.hyperbolicPriorSlope = hyperbolicPriorSlope;

		return this;
	}

	public MCMaxEntTrainer setHyperbolicPriorSharpness (double hyperbolicPriorSharpness)
	{
		this.usingHyperbolicPrior = true;
		this.hyperbolicPriorSharpness = hyperbolicPriorSharpness;

		return this;
	}

	public MCMaxEnt getClassifier () {
		return mt.getClassifier();
	}


	public MCMaxEnt learn (ExampleSet trainingSet) {
        try {
            numIterations = getParameterAsInt(NUM_ITERATIONS);
            usingMultiConditionalTraining = getParameterAsBoolean(USING_MULTI_CONDITIONAL_TRAINING);
            usingHyperbolicPrior = getParameterAsBoolean(USING_HYPERBOLIC_PRIOR);
            gaussianPriorVariance = getParameterAsDouble(GAUSSIAN_PRIOR_VARIANCE);
            hyperbolicPriorSlope = getParameterAsDouble(HYPERBOLIC_PRIOR_SLOPE);
            hyperbolicPriorSharpness = getParameterAsDouble(HYPERBOLIC_PRIOR_SHARPNESS);
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }

		logger.fine ("trainingSet.size() = "+trainingSet.size());
		mt = new MaximizableTrainer (trainingSet, (MCMaxEnt)initialClassifier);
		Optimizer maximizer = new LimitedMemoryBFGS(mt);
		// CPAL - change the tolerance for large vocab experiments
		((LimitedMemoryBFGS)maximizer).setTolerance(.00001);    // std is .0001;
		maximizer.optimize (); // XXX given the loop below, this seems wrong.

		logger.info("MCMaxEnt ngetValueCalls:"+getValueCalls()+"\nMCMaxEnt ngetValueGradientCalls:"+getValueGradientCalls());
//		boolean converged;
//
//	 	for (int i = 0; i < numIterations; i++) {
//			converged = maximizer.maximize (mt, 1);
//			if (converged)
//			 	break;
//			else if (evaluator != null)
//			 	if (!evaluator.evaluate (mt.getClassifier(), converged, i, mt.getValue(),
//				 												 trainingSet, validationSet, testSet))
//				 	break;
//		}
//		TestMaximizable.testValueAndGradient (mt);
		progressLogger.info("\n"); //  progess messages are on one line; move on.
		return mt.getClassifier ();
	}


	/**
	 * <p>Like the other version of <code>trainWithFeatureInduction</code>, but
	 * allows some default options to be changed.</p>
	 *
	 * @param maxent An initial partially-trained classifier (default <code>null</code>).
	 * This classifier may be modified during training.
	 * @param gainName The estimate of gain (log-likelihood increase) we want our chosen
	 * features to maximize.
	 * Should be one of <code>MaxEntTrainer.EXP_GAIN</code>,
	 * <code>MaxEntTrainer.GRADIENT_GAIN</code>, or
	 * <code>MaxEntTrainer.INFORMATION_GAIN</code> (default <code>EXP_GAIN</code>).
	 *
	 * @return The trained <code>MaxEnt</code> classifier
	 */
	/*
	public Classifier trainWithFeatureInduction (InstanceList trainingData,
	                                             InstanceList validationData,
	                                             InstanceList testingData,
	                                             ClassifierEvaluating evaluator,
	                                             MCMaxEnt maxent,

	                                             int totalIterations,
	                                             int numIterationsBetweenFeatureInductions,
	                                             int numFeatureInductions,
	                                             int numFeaturesPerFeatureInduction,
	                                             String gainName) {

		// XXX This ought to be a parameter, except that setting it to true can
		// crash training ("Jump too small").
		boolean saveParametersDuringFI = false;

		Alphabet inputAlphabet = trainingData.getDataAlphabet();
		Alphabet outputAlphabet = trainingData.getTargetAlphabet();

		if (maxent == null)
			maxent = new MCMaxEnt(trainingData.getPipe(),
			                      new double[(1+inputAlphabet.size()) * outputAlphabet.size()]);

		int trainingIteration = 0;
		int numLabels = outputAlphabet.size();

		// Initialize feature selection
		FeatureSelection globalFS = trainingData.getFeatureSelection();
		if (globalFS == null) {
			// Mask out all features; some will be added later by FeatureInducer.induceFeaturesFor(.)
			globalFS = new FeatureSelection (trainingData.getDataAlphabet());
			trainingData.setFeatureSelection (globalFS);
		}
		if (validationData != null) validationData.setFeatureSelection (globalFS);
		if (testingData != null) testingData.setFeatureSelection (globalFS);
		maxent = new MCMaxEnt(maxent.getInstancePipe(), maxent.getParameters(), globalFS);

		// Run feature induction
		for (int featureInductionIteration = 0;
		     featureInductionIteration < numFeatureInductions;
		     featureInductionIteration++) {

			// Print out some feature information
			logger.info ("Feature induction iteration "+featureInductionIteration);

			// Train the model a little bit.  We don't care whether it converges; we
			// execute all feature induction iterations no matter what.
			if (featureInductionIteration != 0) {
				// Don't train until we have added some features
				setNumIterations(numIterationsBetweenFeatureInductions);
				maxent = (MCMaxEnt)this.train (trainingData, validationData, testingData, evaluator,
				                               maxent);
			}
			trainingIteration += numIterationsBetweenFeatureInductions;

			logger.info ("Starting feature induction with "+(1+inputAlphabet.size())+
			             " features over "+numLabels+" labels.");

			// Create the list of error tokens
			InstanceList errorInstances = new InstanceList (trainingData.getDataAlphabet(),
			                                                trainingData.getTargetAlphabet());

			// This errorInstances.featureSelection will get examined by FeatureInducer,
			// so it can know how to add "new" singleton features
			errorInstances.setFeatureSelection (globalFS);
			List errorLabelVectors = new ArrayList();    // these are length-1 vectors
			for (int i = 0; i < trainingData.size(); i++) {
				Instance instance = trainingData.get(i);
				FeatureVector inputVector = (FeatureVector) instance.getData();
				Label trueLabel = (Label) instance.getTarget();

				// Having trained using just the current features, see how we classify
				// the training data now.
				Classification classification = maxent.classify(instance);
				if (!classification.bestLabelIsCorrect()) {
					errorInstances.add(inputVector, trueLabel, null, null);
					errorLabelVectors.add(classification.getLabelVector());
				}
			}
			logger.info ("Error instance list size = "+errorInstances.size());
			int s = errorLabelVectors.size();

			LabelVector[] lvs = new LabelVector[s];
			for (int i = 0; i < s; i++) {
				lvs[i] = (LabelVector)errorLabelVectors.get(i);
			}

			RankedFeatureVector.Factory gainFactory = null;
			if (gainName.equals (EXP_GAIN))
				gainFactory = new ExpGain.Factory (lvs, gaussianPriorVariance);
			else if (gainName.equals(GRADIENT_GAIN))
				gainFactory =	new GradientGain.Factory (lvs);
			else if (gainName.equals(INFORMATION_GAIN))
				gainFactory =	new InfoGain.Factory ();
			else
				throw new IllegalArgumentException("Unsupported gain name: "+gainName);

			FeatureInducer klfi =
			    new FeatureInducer (gainFactory,
			                        errorInstances,
			                        numFeaturesPerFeatureInduction,
			                        2*numFeaturesPerFeatureInduction,
			                        2*numFeaturesPerFeatureInduction);

			// Note that this adds features globally, but not on a per-transition basis
			klfi.induceFeaturesFor (trainingData, false, false);
			if (testingData != null) klfi.induceFeaturesFor (testingData, false, false);
			logger.info ("MCMaxEnt FeatureSelection now includes "+globalFS.cardinality()+" features");
			klfi = null;

			double[] newParameters = new double[(1+inputAlphabet.size()) * outputAlphabet.size()];

			// XXX (Executing this block often causes an error during training; I don't know why.)
			if (saveParametersDuringFI) {
				// Keep current parameter values
				// XXX This relies on the implementation detail that the most recent features
				// added to an Alphabet get the highest indices.

				// Count parameters per output label
				int oldParamCount = maxent.parameters.length / outputAlphabet.size();
				int newParamCount = 1+inputAlphabet.size();
				// Copy params into the proper locations
				for (int i=0; i<outputAlphabet.size(); i++) {
					System.arraycopy(maxent.parameters, i*oldParamCount,
					                 newParameters, i*newParamCount,
					                 oldParamCount);
				}
				for (int i=0; i<oldParamCount; i++)
					if (maxent.parameters[i] != newParameters[i]) {
						System.out.println(maxent.parameters[i]+" "+newParameters[i]);
						System.exit(0);
					}
			}

			maxent.parameters = newParameters;
			maxent.defaultFeatureIndex = inputAlphabet.size();
		}

		// Finished feature induction
		logger.info("Ended with "+globalFS.cardinality()+" features.");
		setNumIterations(totalIterations - trainingIteration);
		return this.train (trainingData, validationData, testingData,
		                   evaluator, maxent);
	}
	*/

	// XXX Should these really be public?  Why?
	/** Counts how many times this trainer has computed the gradient of the
	 * log probability of training labels. */
	public int getValueGradientCalls() {return numGetValueGradientCalls;}
	/** Counts how many times this trainer has computed the
	 * log probability of training labels. */
	public int getValueCalls() {return numGetValueCalls;}
//	public int getIterations() {return maximizerByGradient.getIterations();}

	public String toString()
	{
		return "MCMaxEntTrainer"
		//	+ "("+maximizerClass.getName()+") "
		       + ",numIterations=" + numIterations
		       + (usingHyperbolicPrior
		          ? (",hyperbolicPriorSlope="+hyperbolicPriorSlope+
		             ",hyperbolicPriorSharpness="+hyperbolicPriorSharpness)
		          : (",gaussianPriorVariance="+gaussianPriorVariance));
	}



	// A private inner class that wraps up a MCMaxEnt classifier and its training data.
	// The result is a maximize.Maximizable function.
	private class MaximizableTrainer implements Optimizable.ByGradientValue
	{
		double[] parameters, constraints, cachedGradient;
		MCMaxEnt theClassifier;
		ExampleSet trainingList;
		// The expectations are (temporarily) stored in the cachedGradient
		double cachedValue;
		boolean cachedValueStale;
		boolean cachedGradientStale;
		int numLabels;
		int numFeatures;
		int defaultFeatureIndex;						// just for clarity

		public MaximizableTrainer (){}

		public MaximizableTrainer (ExampleSet ilist, MCMaxEnt initialClassifier)
		{
			this.trainingList = ilist;
            List<Attribute> regularAttributes = Arrays.asList(ilist.getAttributes().createRegularAttributeArray());
            // Don't fd.stopGrowth, because someone might want to do feature induction
            boolean growthStopped = true;
			// Add one feature for the "default feature".
			this.numLabels = ilist.getAttributes().getLabel().getMapping().size();
			this.numFeatures = regularAttributes.size() + 1;
			this.defaultFeatureIndex = numFeatures-1;
			this.parameters = new double [numLabels * numFeatures];
			this.constraints = new double [numLabels * numFeatures];
			this.cachedGradient = new double [numLabels * numFeatures];
			Arrays.fill (parameters, 0.0);
			Arrays.fill (constraints, 0.0);
			Arrays.fill (cachedGradient, 0.0);

			if (initialClassifier != null) {

				this.theClassifier = initialClassifier;
				this.parameters = theClassifier.parameters;
				this.defaultFeatureIndex = theClassifier.defaultFeatureIndex;
			}
			else if (this.theClassifier == null) {
				this.theClassifier = new MCMaxEnt (ilist, parameters);
			}
			cachedValueStale = true;
			cachedGradientStale = true;

			// Initialize the constraints
			logger.fine("Number of instances in training list = " + trainingList.size());
			for (int i = 0;i < trainingList.size(); i++) {
			    Example example = trainingList.getExample(i);
                double instanceWeight = 1.0;
                if (example.getAttributes().getRole("weight") != null) {
                    instanceWeight = example.getWeight();
                }
				//logger.fine ("Instance "+ii+" labeling="+labeling);
                int label = (int)example.getLabel();
				// The "2*" below is because there is one copy for the p(y|x)and another for the p(x|y).
				MatrixOps.rowPlusEquals (constraints, numFeatures, label, example, 2*instanceWeight);
				// For the default feature, whose weight is 1.0
				assert(!Double.isNaN(instanceWeight)) : "instanceWeight is NaN";
				assert(!Double.isNaN(label)) : "bestIndex is NaN";
				boolean hasNaN = false;
                for (int ii = 0; ii < regularAttributes.size(); ii++) {
                    if(Double.isNaN(example.getValue(regularAttributes.get(ii)))) {
                        logger.info("NaN for feature " + regularAttributes.get(ii));
                        hasNaN = true;
                    }
                }

				if(hasNaN)
					logger.info("NaN in instance: " + example.getId());
        // Only p(y|x) uses the default feature; p(x|y) doesn't use it.  The default feature value is 1.0.
        constraints[label*numFeatures + defaultFeatureIndex] += instanceWeight;
			}
			//TestMaximizable.testValueAndGradientCurrentParameters (this);
		}

		public MCMaxEnt getClassifier () { return theClassifier; }

		public double getParameter (int index) {
			return parameters[index];
		}

		public void setParameter (int index, double v) {
			cachedValueStale = true;
			cachedGradientStale = true;
			parameters[index] = v;
		}

		public int getNumParameters() {
			return parameters.length;
		}

		public void getParameters (double[] buff) {
			if (buff == null || buff.length != parameters.length)
				buff = new double [parameters.length];
			System.arraycopy (parameters, 0, buff, 0, parameters.length);
		}

		public void setParameters (double [] buff) {
			assert (buff != null);
			cachedValueStale = true;
			cachedGradientStale = true;
			if (buff.length != parameters.length)
				parameters = new double[buff.length];
			System.arraycopy (buff, 0, parameters, 0, buff.length);
		}


		// log probability of the training labels
		public double getValue ()
		{
			if (cachedValueStale) {
				numGetValueCalls++;
				cachedValue = 0;
				// We'll store the expectation values in "cachedGradient" for now
				cachedGradientStale = true;
				Arrays.fill (cachedGradient, 0.0);
				// Incorporate likelihood of data
				double[] scores = new double[trainingList.getAttributes().getLabel().getMapping().size()];
				double value = 0.0;
				//System.out.println("I Now "+inputAlphabet.size()+" regular features.");
				// Normalize the parameters to be per-class multinomials
				double probs[][] = new double[scores.length][numFeatures];
				double lprobs[][] = new double[scores.length][numFeatures];

				for (int si = 0; si < scores.length; si++) {
					double sum = 0, max = MatrixOps.max (parameters);
					for (int fi = 0; fi < numFeatures; fi++) {
            // TODO Strongly consider some smoothing here.  What happens when all parameters are zero?
            // Oh, this should be no problem, because exp(0) == 1.
                        probs[si][fi] = Math.exp(parameters[si*numFeatures+fi] - max);
						sum += probs[si][fi];
					}
                    assert (sum > 0);
                    for (int fi = 0; fi < numFeatures; fi++) {
                        probs[si][fi] /= sum;
                        lprobs[si][fi] = Math.log(probs[si][fi]);
                    }
				}

				for (int j = 0; j < trainingList.size(); j++) {
					Example example = trainingList.getExample(j);
                    double instanceWeight = 1.0;
                    if (example.getAttributes().getRole("weight") != null) {
                        instanceWeight = example.getWeight();
                    }
					//System.out.println("L Now "+inputAlphabet.size()+" regular features.");

					this.theClassifier.getClassificationScores (example, scores);
                    int label = (int)example.getLabel();
					value = - (instanceWeight * Math.log (scores[label]));
					if(Double.isNaN(value)) {
						logger.fine ("MCMaxEntTrainer: Instance " + example.getId() +
						             "has NaN value. log(scores)= " + Math.log(scores[label]) +
						             " scores = " + scores[label] +
						             " has instance weight = " + instanceWeight);

					}
					if (Double.isInfinite(value)) {
						logger.warning ("Instance "+example.getId() + " has infinite value; skipping value and gradient");
						cachedValue -= value;
						cachedValueStale = false;
						return -value;
//						continue;
					}
					cachedValue += value;
					// CPAL - this is a loop over classes and their scores
					//      - we compute the gradient by taking the dot product of the feature value
					//        and the probability of the class
					for (int si = 0; si < scores.length; si++) {
						if (scores[si] == 0) continue;
						assert (!Double.isInfinite(scores[si]));
						// CPAL - accumulating the current classifiers expectation of the feature
						// vector counts for this class label
						// Current classifier has expectation over class label, not over feature vector
						MatrixOps.rowPlusEquals (cachedGradient, numFeatures,
						                         si, example, -instanceWeight * scores[si]);
						cachedGradient[numFeatures*si + defaultFeatureIndex] += (-instanceWeight * scores[si]);
					}

					// CPAL - if we wish to do multiconditional training we need another term for this accumulated
					//        expectation
					if (usingMultiConditionalTraining) {
						// need something analogous to this
						// this.theClassifier.getClassificationScores (instance, scores);
						// this.theClassifier.getFeatureDistributions (instance,
						// Note: li is the "label" for this instance

						// Get the sum of the feature vector
						// which is the number of counts for the document if we use that as input
						double Ncounts = MatrixOps.sum(example);

						// CPAL - get the additional term for the value of our - log probability
						//      - this computation amounts to the dot product of the feature vector and the probability vector
						cachedValue -= (instanceWeight * MatrixOps.dotProduct(example, lprobs[label]));

						// CPAL - get the model expectation over features for the given class
						for (int fi = 0; fi < numFeatures; fi++) {

							//if(parameters[numFeatures*li + fi] != 0) {
							// MatrixOps.rowPlusEquals(cachedGradient, numFeatures,li,fv,))
							cachedGradient[numFeatures*label + fi] += (-instanceWeight * Ncounts * probs[label][fi]);
							//    }
						}

					}
				}
				//logger.info ("-Expectations:"); cachedGradient.print();
				// Incorporate prior on parameters
				if (usingHyperbolicPrior) {
					for (int li = 0; li < numLabels; li++)
						for (int fi = 0; fi < numFeatures; fi++)
							cachedValue += (hyperbolicPriorSlope / hyperbolicPriorSharpness
							                * Math.log (Maths.cosh (hyperbolicPriorSharpness * parameters[li *numFeatures + fi])));
				} else {
					for (int li = 0; li < numLabels; li++)
						for (int fi = 0; fi < numFeatures; fi++) {
							double param = parameters[li*numFeatures + fi];
							cachedValue += param * param / (2 * gaussianPriorVariance);
						}
				}
				cachedValue *= -1.0; // MAXIMIZE, NOT MINIMIZE
				cachedValueStale = false;
				progressLogger.info ("Value (loglikelihood) = "+cachedValue);
			}
			return cachedValue;
		}

		// CPAL first get value, then gradient

		public void getValueGradient (double [] buffer)
		{
			// Gradient is (constraint - expectation - parameters/gaussianPriorVariance)
			if (cachedGradientStale) {
				numGetValueGradientCalls++;
				if (cachedValueStale)
				// This will fill in the cachedGradient with the "-expectation"
					getValue ();
				// cachedGradient contains the negative expectations
				// expectations are model expectations and constraints are
				// empirical expectations
				MatrixOps.plusEquals (cachedGradient, constraints);
				// CPAL - we need a second copy of the constraints
				//      - actually, we only want this for the feature values
				//      - I've moved this up into getValue
				//if (usingMultiConditionalTraining){
				//    MatrixOps.plusEquals(cachedGradient, constraints);
				//}
				// Incorporate prior on parameters
				if (usingHyperbolicPrior) {
					throw new UnsupportedOperationException ("Hyperbolic prior not yet implemented.");
				}
				else {
					MatrixOps.plusEquals (cachedGradient, parameters,
					                      -1.0 / gaussianPriorVariance);
				}

				// A parameter may be set to -infinity by an external user.
				// We set gradient to 0 because the parameter's value can
				// never change anyway and it will mess up future calculations
				// on the matrix, such as norm().
				MatrixOps.substitute (cachedGradient, Double.NEGATIVE_INFINITY, 0.0);
				// Set to zero all the gradient dimensions that are not among the selected features
                for (int labelIndex = 0; labelIndex < numLabels; labelIndex++)
                    MatrixOps.rowSetAll (cachedGradient, numFeatures,
                            labelIndex, 0.0, false);
				cachedGradientStale = false;
			}
			assert (buffer != null && buffer.length == parameters.length);
			System.arraycopy (cachedGradient, 0, buffer, 0, cachedGradient.length);
		}

		public double sumNegLogProb (double a, double b)
		{
			if (a == Double.POSITIVE_INFINITY && b == Double.POSITIVE_INFINITY)
				return Double.POSITIVE_INFINITY;
			else if (a > b)
				return b - Math.log (1 + Math.exp(b-a));
			else
				return a - Math.log (1 + Math.exp(a-b));
		}

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
        types.add(new ParameterTypeInt(NUM_ITERATIONS, "The paramter of num_iterations.", 1, Integer.MAX_VALUE,
                10, false));
        types.add(new ParameterTypeBoolean(USING_MULTI_CONDITIONAL_TRAINING, "The paramter of using_multi_conditional_training.", true));
        types.add(new ParameterTypeBoolean(USING_HYPERBOLIC_PRIOR, "The paramter of using_hperbolic_prior.", false));
        types.add(new ParameterTypeDouble(GAUSSIAN_PRIOR_VARIANCE, "The paramter of gaussian_prior_variance.", 0.0001, Double.MAX_VALUE,
                0.1, false));
        types.add(new ParameterTypeDouble(HYPERBOLIC_PRIOR_SLOPE, "The paramter of hyperbolic_prior_slope.", 0.0001, Double.MAX_VALUE,
                0.2, false));
        types.add(new ParameterTypeDouble(HYPERBOLIC_PRIOR_SHARPNESS, "The paramter of hyperbolic_prior_sharpness.", 1.0, Double.MAX_VALUE,
                10.0, false));

        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
        return types;
    }

}
