package base.operators.operator.learner.winnow;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.learner.PredictionModel;
import base.operators.tools.Tools;

import java.io.Serializable;
import java.util.ArrayList;
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 */
public class BalancedWinnow extends PredictionModel implements Serializable {

    private static final long serialVersionUID = 1;

    double [][] m_weights;
    /**
     * Passes along ExampleSet and weights from BalancedWinnowLearner
     * @param trainExampleSet needed for dictionary, labels, feature vectors, etc
     * @param weights weights calculated during training phase
     */
    public BalancedWinnow (ExampleSet trainExampleSet, double [][] weights)
    {
        super (trainExampleSet);
        m_weights = new double[weights.length][weights[0].length];
        for (int i = 0; i < weights.length; i++)
            for (int j = 0; j < weights[0].length; j++)
                m_weights[i][j] = weights[i][j];
    }

    /**
     * @return a copy of the weight vectors
     */
    public double[][] getWeights()
    {
        int numCols = m_weights[0].length;
        double[][] ret = new double[m_weights.length][numCols];
        for (int i = 0; i < ret.length; i++)
            System.arraycopy(m_weights[i], 0, ret[i], 0, numCols);
        return ret;
    }

    /**
     * Classifies an instance using Winnow's weights
     * @param examples an ExampleSet to be classified
     * @param predictedLabel an Attribute to set predict label
     * @return an object containing the classifier's guess
     */
    public ExampleSet performPrediction (ExampleSet examples, Attribute predictedLabel){
        for (int jj = 0; jj < examples.size(); jj++) {
            Example example = examples.getExample(jj);
            int nfeats = m_weights[0].length - 1;
            int numClasses = m_weights.length;
            double[] scores = new double[numClasses];
            ArrayList<Integer> fi = new ArrayList<>();
            ArrayList<Double> fv = new ArrayList<>();
            int attribute_index = 0;
            for (Attribute attribute : example.getAttributes()) {
                if (example.getValue(attribute)!=0){
                    fi.add(attribute_index);
                    fv.add(example.getValue(attribute));
                }
                attribute_index++;
            }
            // Take dot products
            double sum = 0;
            for (int ci = 0; ci < numClasses; ci++) {
                for (int fvi = 0; fvi < fi.size(); fvi++) {
                    int index = fi.get(fvi);
                    double vi = fv.get(fvi);

                    if ( m_weights[ci].length > index ) {
                        scores[ci] += vi * m_weights[ci][index];
                        sum += vi * m_weights[ci][index];
                    }
                }
                scores[ci] += m_weights[ci][nfeats];
                sum += m_weights[ci][nfeats];
            }
            timesEquals(scores, 1.0 / sum);
            double max = 0;
            int maxIndex = 0;
            for (int kk = 0; kk < scores.length; kk++) {
                if (scores[kk] > max) {
                    max = scores[kk];
                    maxIndex = kk;
                }
            }

            example.setPredictedLabel(maxIndex);

            for (int ll = 0; ll < scores.length; ll++) {
                example.setConfidence(getLabel().getMapping().mapIndex(ll), scores[ll]);
            }
        }
        // Create and return a Classification object
        return examples;
    }

    /**
     *  Multiplies every element in an array by a scalar.
     *  @param m The array
     *  @param factor The scalar
     */
    public static void timesEquals (double[] m, double factor) {
        for (int i=0; i < m.length; i++)
            m[i] *= factor;
    }

    @Override
    protected boolean supportsConfidences(Attribute label) {
        return label != null && (label.isNominal()||label.isNumerical());
    }

    @Override
    public String getName() {
        return "Balanced Winnow";
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < m_weights.length; i++) {
            result.append("weight "+i+": "+m_weights[i] + Tools.getLineSeparator());
        }
        result.append("Shape of weights: " + m_weights.length +"*"+m_weights[0].length +  Tools.getLineSeparator());
        return result.toString();
    }

}
