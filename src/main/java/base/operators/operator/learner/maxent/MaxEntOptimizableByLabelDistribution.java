package base.operators.operator.learner.maxent;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.NominalMapping;
import base.operators.operator.learner.maxent.optimize.LimitedMemoryBFGS;
import base.operators.operator.learner.maxent.optimize.MatrixOps;
import base.operators.operator.learner.maxent.optimize.Optimizable;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 */
public class MaxEntOptimizableByLabelDistribution implements Optimizable.ByGradientValue  //, Serializable TODO needs to be done?
{
	private static Logger logger = Logger.getLogger(MaxEntOptimizableByLabelDistribution.class.getName());
	private static Logger progressLogger = Logger.getLogger(MaxEntOptimizableByLabelDistribution.class.getName()+"-pl");

	// xxx Why does TestMaximizable fail when this variance is very small?
	//static final double DEFAULT_GAUSSIAN_PRIOR_VARIANCE = 1;

	static final double DEFAULT_GAUSSIAN_PRIOR_VARIANCE = 1.0;
	static final Class DEFAULT_MAXIMIZER_CLASS = LimitedMemoryBFGS.class;

	double gaussianPriorVariance = DEFAULT_GAUSSIAN_PRIOR_VARIANCE;
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
	int numGetValueCalls = 0;
	int numGetValueGradientCalls = 0;
	double[][] trainScores;


	public MaxEntOptimizableByLabelDistribution() {
	}

	public MaxEntOptimizableByLabelDistribution(ExampleSet trainingSet, double[][] trainScores, MaxEnt initialClassifier)
	{
		this.trainingList = trainingSet;
		this.trainScores = trainScores;
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
		if (initialClassifier != null) {
			this.theClassifier = initialClassifier;
			this.parameters = theClassifier.parameters;
			this.defaultFeatureIndex = theClassifier.defaultFeatureIndex;
		}
		else if (this.theClassifier == null) {
			this.theClassifier = new MaxEnt (trainingSet, parameters);
		}
		cachedValueStale = true;
		cachedGradientStale = true;

		// Initialize the constraints
		logger.fine("Number of instances in training list = " + trainingList.size());
		for (int ii = 0; ii < trainingList.size(); ii++) {
			Example example = trainingSet.getExample(ii);
			double instanceWeight = 1.0;
			if (example.getAttributes().getRole("weight") != null) {
				instanceWeight = example.getWeight();
			}
			int label = (int)example.getLabel();
			if(Double.isNaN(label))
				continue;
			//logger.fine ("Instance "+ii+" labeling="+labeling);

			// Here is the difference between this code and the single label
			//  version: rather than only picking out the "best" index,
			//  loop over all label indices.
			for (int pos = 0; pos < this.numLabels; pos++){
				MatrixOps.rowPlusEquals (constraints, numFeatures,
						pos,
						example,
						instanceWeight * this.trainScores[ii][pos]);
			}

			assert(!Double.isNaN(instanceWeight)) : "instanceWeight is NaN";

			boolean hasNaN = false;
			for (int i = 0; i < regularAttributes.size(); i++) {
				if (Double.isNaN(example.getValue(regularAttributes.get(i)))) {
					logger.info("NaN for feature " + regularAttributes.get(i).getName());
					hasNaN = true;
				}
			}
			if (hasNaN)
				logger.info("NaN in instance: " + example.getId());

			// For the default feature, whose weight is 1.0
			for (int pos = 0; pos < this.numLabels; pos++) {
				constraints[pos*numFeatures + defaultFeatureIndex] +=
						1.0 * instanceWeight * this.trainScores[ii][pos];
			}
		}
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


	/** Return the log probability of the training label distributions */
	public double getValue () {

		if (cachedValueStale) {

			numGetValueCalls++;

			cachedValue = 0;
			// We'll store the expectation values in "cachedGradient" for now
			cachedGradientStale = true;
			MatrixOps.setAll (cachedGradient, 0.0);
			NominalMapping labelMapping = trainingList.getAttributes().getLabel().getMapping();
			// Incorporate likelihood of data
			double[] scores = new double[labelMapping.size()];
			double value = 0.0;
			for (int ii=0; ii < trainingList.size();ii++) {
				Example example = trainingList.getExample(ii);
				double instanceWeight = 1.0;
				if (example.getAttributes().getRole("weight") != null) {
					instanceWeight = example.getWeight();
				}
				int label = (int)example.getLabel();
				if (Double.isNaN(label))
					continue;
				//System.out.println("L Now "+inputAlphabet.size()+" regular features.");

				this.theClassifier.getClassificationScores (example, scores);

				value = 0.0;
				for(int pos = 0; pos < labelMapping.size(); pos++) { //loop, added by Limin Yao
				  if (scores[pos] == 0  && trainScores[ii][pos] > 0) {
				    logger.warning ("Instance "+example.getId() + " has infinite value; skipping value and gradient");
				    cachedValue = Double.NEGATIVE_INFINITY;
				    cachedValueStale = false;
				    return cachedValue;
				  }else if (trainScores[ii][pos] != 0) {
				    value -= (instanceWeight * trainScores[ii][pos] * Math.log (scores[pos]));
				  }
				}

				if (Double.isNaN(value)) {
					logger.fine ("MaxEntOptimizableByLabelDistribution: Instance " + example.getId() +
								 "has NaN value.");
				}
				if (Double.isInfinite(value)) {
					logger.warning ("Instance "+example.getId() + " has infinite value; skipping value and gradient");
					cachedValue -= value;
					cachedValueStale = false;
					return -value;
//					continue;
				}
				cachedValue += value;

				//The model expectation? added by Limin Yao
				for (int si = 0; si < scores.length; si++) {
					if (scores[si] == 0) continue;
					assert (!Double.isInfinite(scores[si]));
					MatrixOps.rowPlusEquals (cachedGradient, numFeatures,
											 si, example, -instanceWeight * scores[si]);
					cachedGradient[numFeatures*si + defaultFeatureIndex] += (-instanceWeight * scores[si]);
				}
			}

			//logger.info ("-Expectations:"); cachedGradient.print();
			// Incorporate prior on parameters
			double prior = 0;
			for (int li = 0; li < numLabels; li++) {
				for (int fi = 0; fi < numFeatures; fi++) {
					double param = parameters[li*numFeatures + fi];
					prior += param * param / (2 * gaussianPriorVariance);
				}
			}

			double oValue = cachedValue;
			cachedValue += prior;
			cachedValue *= -1.0; // MAXIMIZE, NOT MINIMIZE
			cachedValueStale = false;
			progressLogger.info ("Value (labelProb="+(-oValue)+" prior="+(-prior)+") loglikelihood = "+cachedValue);
		}
		return cachedValue;
	}

	public void getValueGradient (double [] buffer)
	{
		// Gradient is (constraint - expectation - parameters/gaussianPriorVariance)
		if (cachedGradientStale) {
			numGetValueGradientCalls++;
			if (cachedValueStale)
				// This will fill in the cachedGradient with the "-expectation"
				getValue ();
			MatrixOps.plusEquals (cachedGradient, constraints);
			// Incorporate prior on parameters
			MatrixOps.plusEquals (cachedGradient, parameters,
								  -1.0 / gaussianPriorVariance);

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


	public MaxEntOptimizableByLabelDistribution useGaussianPrior () {
		return this;
	}

	/**
	 * Sets a parameter to prevent overtraining.  A smaller variance for the prior
	 * means that feature weights are expected to hover closer to 0, so extra
	 * evidence is required to set a higher weight.
	 * @return This trainer
	 */
	public MaxEntOptimizableByLabelDistribution setGaussianPriorVariance (double gaussianPriorVariance)
	{
		this.gaussianPriorVariance = gaussianPriorVariance;
		return this;
	}

}
