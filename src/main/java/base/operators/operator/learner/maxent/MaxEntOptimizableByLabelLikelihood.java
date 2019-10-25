package base.operators.operator.learner.maxent;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.NominalMapping;
import base.operators.operator.learner.maxent.optimize.LimitedMemoryBFGS;
import base.operators.operator.learner.maxent.optimize.MatrixOps;
import base.operators.operator.learner.maxent.optimize.Optimizable;
import base.operators.operator.learner.maxent.utils.Maths;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 */

public class MaxEntOptimizableByLabelLikelihood implements Optimizable.ByGradientValue {

	private static Logger logger =
			Logger.getLogger(MaxEntOptimizableByLabelLikelihood.class.getName());
	private static Logger progressLogger =
			Logger.getLogger(MaxEntOptimizableByLabelLikelihood.class.getName()+"-pl");

	// xxx Why does TestMaximizable fail when this variance is very small?
	static final Class DEFAULT_MAXIMIZER_CLASS = LimitedMemoryBFGS.class;

	boolean usingHyperbolicPrior;
	boolean usingGaussianPrior;
	double gaussianPriorVariance;
	double hyperbolicPriorSlope;
	double hyperbolicPriorSharpness;
	Class maximizerClass = DEFAULT_MAXIMIZER_CLASS;

	double[] parameters, constraints, cachedGradient;
	MaxEnt theClassifier;
	ExampleSet trainingList;
	// The expectations are (temporarily) stored in the cachedGradient
	double cachedValue;
	boolean cachedValueStale;
	boolean cachedGradientStale;
	int numLabels;
	int numFeatures;
	int defaultFeatureIndex;						// just for clarity
//	FeatureSelection featureSelection;
//	FeatureSelection[] perLabelFeatureSelection;
	int numGetValueCalls = 0;
	int numGetValueGradientCalls = 0;

	public MaxEntOptimizableByLabelLikelihood() {
	}

	public MaxEntOptimizableByLabelLikelihood (ExampleSet trainingSet, MaxEnt initialClassifier)
	{
		this.trainingList = trainingSet;
		Attributes attributes = trainingSet.getAttributes();
		List<Attribute> regularAttributes = Arrays.asList(trainingSet.getAttributes().createRegularAttributeArray());
		NominalMapping ld = attributes.getLabel().getMapping();
		// Don't fd.stopGrowth, because someone might want to do feature induction
		boolean growthStopped = true;
		// Add one feature for the "default feature".
		this.numLabels = ld.size();
		this.numFeatures = regularAttributes.size() + 1;
		this.defaultFeatureIndex = numFeatures-1;
		this.parameters = new double [numLabels * numFeatures];
		this.constraints = new double [numLabels * numFeatures];
		this.cachedGradient = new double [numLabels * numFeatures];
		Arrays.fill (parameters, 0.0);
		Arrays.fill (constraints, 0.0);
		Arrays.fill (cachedGradient, 0.0);
//		this.featureSelection = trainingSet.getFeatureSelection();
//		this.perLabelFeatureSelection = trainingSet.getPerLabelFeatureSelection();
		// Add the default feature index to the selection
//		if (featureSelection != null)
//			featureSelection.add (defaultFeatureIndex);
//		if (perLabelFeatureSelection != null)
//			for (int i = 0; i < perLabelFeatureSelection.length; i++)
//				perLabelFeatureSelection[i].add (defaultFeatureIndex);
//		// xxx Later change this to allow both to be set, but select which one to use by a boolean flag?
//		assert (featureSelection == null || perLabelFeatureSelection == null);
		if (initialClassifier != null) {
			this.theClassifier = initialClassifier;
			this.parameters = theClassifier.parameters;
//			this.featureSelection = theClassifier.featureSelection;
//			this.perLabelFeatureSelection = theClassifier.perClassFeatureSelection;
			this.defaultFeatureIndex = theClassifier.defaultFeatureIndex;
		}
		else if (this.theClassifier == null) {
			this.theClassifier = new MaxEnt (trainingSet, parameters);
		}
		cachedValueStale = true;
		cachedGradientStale = true;

		// Initialize the constraints
		logger.fine("Number of instances in training list = " + trainingList.size());
		for (Example example : trainingList) {
			double instanceWeight = 1.0;
			if (example.getAttributes().getRole("weight") != null) {
				instanceWeight = example.getWeight();
			}
			int label = (int)example.getLabel();
			if(Double.isNaN(label))
				continue;
			//logger.fine ("Instance "+ii+" labeling="+labeling);
			MatrixOps.rowPlusEquals (constraints, numFeatures, label, example, instanceWeight);
			// For the default feature, whose weight is 1.0
			assert(!Double.isNaN(instanceWeight)) : "instanceWeight is NaN";
			assert(!Double.isNaN(label)) : "label is NaN";
			boolean hasNaN = false;

			for (int i = 0; i < regularAttributes.size(); i++) {
				if(Double.isNaN(example.getValue(regularAttributes.get(i)))) {
					hasNaN = true;
				}
			}
			if (hasNaN)
				logger.info("NaN in instance: " + example.getId());

			constraints[label*numFeatures + defaultFeatureIndex] += 1.0 * instanceWeight;
		}
		//TestMaximizable.testValueAndGradientCurrentParameters (this);
	}

	public MaxEnt getClassifier () { return theClassifier; }

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
			MatrixOps.setAll (cachedGradient, 0.0);
			// Incorporate likelihood of data
			double[] scores = new double[trainingList.getAttributes().getLabel().getMapping().size()];
			double value = 0.0;
			Iterator<Example> iter = trainingList.iterator();
			int ii=0;
			while (iter.hasNext()) {
				ii++;
				Example instance = iter.next();
				double instanceWeight = 1.0;
				if (instance.getAttributes().getRole("weight") != null) {
					instanceWeight = instance.getWeight();
				}
				int label = (int)instance.getLabel();
				if (Double.isNaN(label))
					continue;
				//System.out.println("L Now "+inputAlphabet.size()+" regular features.");

				this.theClassifier.getClassificationScores (instance, scores);
				value = - (instanceWeight * Math.log (scores[label]));
				if(Double.isNaN(value)) {
					logger.fine ("MaxEntTrainer: Instance " + instance.getId() +
							"has NaN value. log(scores)= " + Math.log(scores[label]) +
							" scores = " + scores[label] +
							" has instance weight = " + instanceWeight);

				}
				if (Double.isInfinite(value)) {
					logger.warning ("Instance "+instance.values().toString() + " has infinite value; skipping value and gradient");
					cachedValue -= value;
					cachedValueStale = false;
					return -value;
//					continue;
				}
				cachedValue += value;
				for (int si = 0; si < scores.length; si++) {
					if (scores[si] == 0) continue;
					assert (!Double.isInfinite(scores[si]));
					MatrixOps.rowPlusEquals (cachedGradient, numFeatures,
							si, instance, -instanceWeight * scores[si]);
					cachedGradient[numFeatures * si + defaultFeatureIndex] += (-instanceWeight * scores[si]);
				}
			}
			//logger.info ("-Expectations:"); cachedGradient.print();

			// Incorporate prior on parameters
			double prior = 0;
			//使用双曲线先验
			if (usingHyperbolicPrior) {
				for (int li = 0; li < numLabels; li++)
					for (int fi = 0; fi < numFeatures; fi++)
						prior += (hyperbolicPriorSlope / hyperbolicPriorSharpness
								* Math.log (Maths.cosh (hyperbolicPriorSharpness * parameters[li *numFeatures + fi])));
			}
			//使用高斯先验
			else if (usingGaussianPrior) {
				for (int li = 0; li < numLabels; li++)
					for (int fi = 0; fi < numFeatures; fi++) {
						double param = parameters[li*numFeatures + fi];
						prior += param * param / (2 * gaussianPriorVariance);
					}
			}

			double oValue = cachedValue;
			cachedValue += prior;
			cachedValue *= -1.0; // MAXIMIZE, NOT MINIMIZE
			cachedValueStale = false;
			progressLogger.info ("Value (labelProb="+oValue+" prior="+prior+") loglikelihood = "+cachedValue);
		}
		return cachedValue;
	}

	public void getValueGradient (double [] buffer) {

		// Gradient is (constraint - expectation - parameters/gaussianPriorVariance)
		if (cachedGradientStale) {
			numGetValueGradientCalls++;
			if (cachedValueStale)
				// This will fill in the cachedGradient with the "-expectation"
				getValue ();
			MatrixOps.plusEquals (cachedGradient, constraints);
			// Incorporate prior on parameters
			if (usingHyperbolicPrior) {
				throw new UnsupportedOperationException ("Hyperbolic prior not yet implemented.");
			}
			else if (usingGaussianPrior) {
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
		//System.out.println ("MaxEntTrainer gradient infinity norm = "+MatrixOps.infinityNorm(cachedGradient));
	}

	// XXX Should these really be public?  Why?
	/** Counts how many times this trainer has computed the gradient of the
	 * log probability of training labels. */
	public int getValueGradientCalls() {return numGetValueGradientCalls;}
	/** Counts how many times this trainer has computed the
	 * log probability of training labels. */
	public int getValueCalls() {return numGetValueCalls;}
//	public int getIterations() {return maximizerByGradient.getIterations();}


	public MaxEntOptimizableByLabelLikelihood useGaussianPrior () {
		this.usingGaussianPrior = true;
		this.usingHyperbolicPrior = false;
		return this;
	}

	public MaxEntOptimizableByLabelLikelihood useHyperbolicPrior () {
		this.usingGaussianPrior = false;
		this.usingHyperbolicPrior = true;
		return this;
	}

	/**
	 *  In some cases a prior term is implemented in the optimizer,
	 *  (eg orthant-wise L-BFGS), so we occasionally want to only
	 *   calculate the log likelihood.
	 */
	public MaxEntOptimizableByLabelLikelihood useNoPrior () {
		this.usingGaussianPrior = false;
		this.usingHyperbolicPrior = false;
		return this;
	}

	/**
	 * Sets a parameter to prevent overtraining.  A smaller variance for the prior
	 * means that feature weights are expected to hover closer to 0, so extra
	 * evidence is required to set a higher weight.
	 * @return This trainer
	 */
	public MaxEntOptimizableByLabelLikelihood setGaussianPriorVariance (double gaussianPriorVariance)
	{
		this.usingGaussianPrior = true;
		this.usingHyperbolicPrior = false;
		this.gaussianPriorVariance = gaussianPriorVariance;
		return this;
	}

	public MaxEntOptimizableByLabelLikelihood setHyperbolicPriorSlope (double hyperbolicPriorSlope)
	{
		this.usingGaussianPrior = false;
		this.usingHyperbolicPrior = true;
		this.hyperbolicPriorSlope = hyperbolicPriorSlope;
		return this;
	}

	public MaxEntOptimizableByLabelLikelihood setHyperbolicPriorSharpness (double hyperbolicPriorSharpness)
	{
		this.usingGaussianPrior = false;
		this.usingHyperbolicPrior = true;
		this.hyperbolicPriorSharpness = hyperbolicPriorSharpness;
		return this;
	}


}
