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
public class Winnow extends PredictionModel implements Serializable {
    /**
     *array of weights, one for each feature, initialized to 1
     */
    double [][] weights;

    /**
     * Passes along data pipe and weights from WinnowLearner
     * @param newWeights weights calculated during training phase
     */
    public Winnow (ExampleSet trainingExampleSet, double [][]newWeights){
        super(trainingExampleSet);
        this.weights = new double[newWeights.length][newWeights[0].length];
        for(int i=0; i<newWeights.length; i++)
            for(int j=0; j<newWeights[0].length; j++)
                this.weights[i][j] = newWeights[i][j];
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
            int numClasses = weights.length;
            double[] scores = new double[numClasses];
            ArrayList<Integer> fi = new ArrayList<>();
            int attribute_index = 0;
            for (Attribute attribute : example.getAttributes()) {
                if (example.getValue(attribute)!=0){
                    fi.add(attribute_index);
                }
                attribute_index++;
            }
            // Make sure the feature vector's feature dictionary matches
            // what we are expecting from our data pipe (and thus our notion
            // of feature probabilities.
            int fvisize = fi.size();
            // Set the scores by summing wi*xi
            for (int fvi = 0; fvi < fvisize; fvi++) {
                int index = fi.get(fvi);
                for (int ci = 0; ci < numClasses; ci++)
                    scores[ci] += this.weights[ci][index];
            }
            double max = 0;
            int maxIndex = 0;
            double sum = 0.0;
            for (int kk = 0; kk < scores.length; kk++) {
                sum+=scores[kk];
                if (scores[kk] > max) {
                    max = scores[kk];
                    maxIndex = kk;
                }
            }
            example.setValue(predictedLabel, maxIndex);

            for (int ll = 0; ll < scores.length; ll++) {
                example.setConfidence(getLabel().getMapping().mapIndex(ll), scores[ll]/sum);
            }

        }


        // Create and return a Classification object
        return examples;
    }

    @Override
    protected boolean supportsConfidences(Attribute label) {
        return label != null && (label.isNominal()||label.isNumerical());
    }

    @Override
    public String getName() {
        return "Winnow";
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < weights.length; i++) {
            result.append("weight "+i+": "+weights[i] + Tools.getLineSeparator());
        }
        result.append("Shape of weights: " + weights.length +"*"+weights[0].length + Tools.getLineSeparator());
        return result.toString();
    }
}
