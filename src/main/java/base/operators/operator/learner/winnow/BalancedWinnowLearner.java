package base.operators.operator.learner.winnow;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.learner.AbstractLearner;
import base.operators.operator.learner.PredictionModel;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.RandomGenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 */
public class BalancedWinnowLearner extends AbstractLearner implements Serializable {


    public BalancedWinnowLearner(OperatorDescription description) {
        super(description);
    }

    private static final long serialVersionUID = 1L;

    public static final String M_EPSILON = "m_epsilon";
    public static final String M_DELTA = "m_delta";
    public static final String M_MAX_ITERATIONS = "m_max_iterations";
    public static final String M_COOLING_RATE = "m_cooling_rate";


//    double m_epsilon = .5;
//    double m_delta = .1;
//    int m_maxIterations = 30;
//    double m_coolingRate = .5;
    /**
     * Array of weights, one for each class and feature, initialized to 1.
     * For each class, there is an additional default "feature" weight
     * that is set to 1 in every example (it remains constant; this is
     * used to prevent the instance from having 0 dot product with a class).
     */
    double[][] m_weights;

    BalancedWinnow classifier;

    public BalancedWinnow getClassifier () { return classifier; }


    /**
     * Trains winnow on the instance list, updating
     * {weights} according to errors
     * @param trainExampleSet ExampleSet to be trained on
     * @return Classifier object containing learned weights
     */
    public BalancedWinnow learn (ExampleSet trainExampleSet)
    {
        double m_epsilon = .5;
        double m_delta = .1;
        int m_maxIterations = 30;
        double m_coolingRate = .5;
        try {
            m_epsilon = getParameterAsDouble(M_EPSILON);
            m_delta = getParameterAsDouble(M_DELTA);
            m_maxIterations = getParameterAsInt(M_MAX_ITERATIONS);
            m_coolingRate = getParameterAsDouble(M_COOLING_RATE);
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }
        double epsilon = m_epsilon;
        Attributes attributes = trainExampleSet.getAttributes();
        List<String> allLabelNames = attributes.getLabel().getMapping().getValues();
        int numLabels = allLabelNames.size();
        int numFeats = trainExampleSet.getAttributes().size();
        m_weights = new double [numLabels][numFeats+1];
        // init weights to 1
        for(int i=0; i<numLabels; i++)
            arraysFill(m_weights[i], 1.0);
        //System.out.println("Init weights to 1.  Theta= "+theta);
        // Loop through training instances multiple times
        double[] results = new double[numLabels];
        for (int iter = 0; iter < m_maxIterations; iter++) {
            // loop through all instances
            for (int ii = 0; ii < trainExampleSet.size(); ii++){
                Example inst = trainExampleSet.getExample(ii);
                String label = inst.getNominalValue(attributes.getLabel());
                int correctIndex = allLabelNames.indexOf(label);
                arraysFill(results, 0);
                ArrayList<Integer> fi = new ArrayList<>();
                ArrayList<Double> fv = new ArrayList<>();
                int jj = 0;
                for (Attribute attribute : attributes) {
                    if (inst.getValue(attribute) != 0){
                        fi.add(jj);
                        fv.add(inst.getValue(attribute));
                    }
                    jj++;
                }
                // compute dot(x, wi) for each class i
                for(int lpos = 0; lpos < numLabels; lpos++) {
                    for(int fvi = 0; fvi < fv.size(); fvi++) {
                        int index = fi.get(fvi);
                        double vi = fv.get(fvi);
                        results[lpos] += vi * m_weights[lpos][index];
                    }
                    // This extra value comes from the extra
                    // "feature" present in all examples
                    results[lpos] += m_weights[lpos][numFeats];
                }

                // Get indices of the classes with the 2 highest dot products
                int predictedIndex = 0;
                int secondHighestIndex = 0;
                double max = Double.MIN_VALUE;
                double secondMax = Double.MIN_VALUE;
                for (int i = 0; i < numLabels; i++) {
                    if (results[i] > max) {
                        secondMax = max;
                        max = results[i];
                        secondHighestIndex = predictedIndex;
                        predictedIndex = i;
                    }
                    else if (results[i] > secondMax) {
                        secondMax = results[i];
                        secondHighestIndex = i;
                    }
                }

                // Adjust weights if this example is mispredicted
                // or just barely correct
                if (predictedIndex != correctIndex) {
                    for (int kk = 0; kk < fi.size(); kk++) {
                        int index = fi.get(kk);
                        m_weights[predictedIndex][index] *= (1 - epsilon);
                        m_weights[correctIndex][index] *= (1 + epsilon);
                    }
                    m_weights[predictedIndex][numFeats] *= (1 - epsilon);
                    m_weights[correctIndex][numFeats] *= (1 + epsilon);
                }
                else if (max/secondMax - 1 < m_delta) {
                    for (int ll = 0; ll < fi.size(); ll++) {
                        int index = fi.get(ll);
                        m_weights[secondHighestIndex][index] *= (1 - epsilon);
                        m_weights[correctIndex][index] *= (1 + epsilon);
                    }
                    m_weights[secondHighestIndex][numFeats] *= (1 - epsilon);
                    m_weights[correctIndex][numFeats] *= (1 + epsilon);
                }
            }
            // Cut epsilon by the cooling rate
            epsilon *= (1-m_coolingRate);
        }
        classifier = new BalancedWinnow (trainExampleSet, m_weights);
        return classifier;
    }

    @Override
    public Class<? extends PredictionModel> getModelClass() {
        return BalancedWinnow.class;
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
        types.add(new ParameterTypeDouble(M_EPSILON, "The paramter of m_epsilon.", 0.01, 1,
                0.5, false));
        types.add(new ParameterTypeDouble(M_DELTA,
                "The paramter of m_delta.", 0.01, 1,
                0.1, false));
        types.add(new ParameterTypeInt(M_MAX_ITERATIONS,
                "The paramter of m_max_iterations.", 1, Integer.MAX_VALUE,
                30, false));
        types.add(new ParameterTypeDouble(M_COOLING_RATE,
                "The paramter of m_cooling_rate.", 0.01, 1,
                0.5, false));

        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
        return types;
    }

    public static void arraysFill(double[] a, double val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }
}
