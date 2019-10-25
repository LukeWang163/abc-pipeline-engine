package base.operators.operator.learner.winnow;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.NominalMapping;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.learner.AbstractLearner;
import base.operators.operator.learner.PredictionModel;
import base.operators.parameter.*;
import base.operators.tools.RandomGenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static base.operators.operator.learner.winnow.BalancedWinnowLearner.arraysFill;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 */
public class WinnowLearner extends AbstractLearner implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ALPHA = "alpha";
    public static final String BETA = "beta";
    public static final String N_FACTOR = "n_factor";

    public WinnowLearner(OperatorDescription description) {
        super(description);
    }

//    /**
//     *constant to multiply to "correct" weights in promotion step
//     */
//    double alpha = 2.0;
//    /**
//     *constant to divide "incorrect" weights by in demotion step
//     */
//    double beta = 2.0;
//    /**
//     *threshold for sum of wi*xi in formulating guess
//     */
//    double theta;
//    /**
//     *factor of n to set theta to. e.g. if n=1/2, theta = n/2.
//     */
//    double nfactor = .5;
    /**
     *array of weights, one for each feature, initialized to 1
     */
    double [][] weights;

    double theta;

    Winnow classifier;

    public Winnow getClassifier () { return classifier; }

    /**
     * Trains winnow on the instance list, updating
     * {@link #weights weights} according to errors
     * @param trainExampleSet ExampleSet to be trained on
     * @return Classifier object containing learned weights
     */
    public Winnow learn (ExampleSet trainExampleSet)
    {
        double alpha = 2;
        double beta = 2;
        double nfactor = .5;
        try {
            alpha = getParameterAsDouble(ALPHA);
            beta = getParameterAsDouble(BETA);
            nfactor = getParameterAsDouble(N_FACTOR);
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }
        Attributes attributes = trainExampleSet.getAttributes();
        NominalMapping labelMapping = attributes.getLabel().getMapping();
        int numLabels = labelMapping.size();
        int numFeats = trainExampleSet.getAttributes().size();
        theta =  numFeats * nfactor;
        weights = new double [numLabels][numFeats];
        // init weights to 1
        for(int i=0; i<numLabels; i++)
            arraysFill(weights[i], 1.0);
        //System.out.println("Init weights to 1.  Theta= "+theta);
        // loop through all instances
        for (int ii = 0; ii < trainExampleSet.size(); ii++){
            Example inst = trainExampleSet.getExample(ii);
            String label = inst.getNominalValue(attributes.getLabel());
            int correctIndex = labelMapping.getIndex(label);
            ArrayList<Integer> fi = new ArrayList<>();
            int jj = 0;
            for (Attribute attribute : attributes) {
                if (inst.getValue(attribute) != 0){
                    fi.add(jj);
                }
                jj++;
            }
            double[] results = new double [numLabels];
            arraysFill(results,0.0);
            // sum up xi*wi for each class
            for(int fvi=0; fvi < fi.size(); fvi++){
                int index = fi.get(fvi);
                //System.out.println("feature index "+fi);
                for(int lpos=0; lpos < numLabels; lpos++)
                    results[lpos] += weights[lpos][index];
            }
            //System.out.println("In instance " + ii);
            // make guess for each label using threshold
            // update weights according to alpha and beta
            // upon incorrect guess
            for(int ri = 0; ri < numLabels; ri++){
                if(results[ri] > theta){ // guess 1
                    if(correctIndex != ri) // correct is 0
                        demote(ri, fi, beta);
                }
                else{ // guess 0
                    if(correctIndex == ri) // correct is 1
                        promote(ri, fi, alpha);
                }
            }
        }
        classifier = new Winnow (trainExampleSet, weights);
        return classifier;
    }
    /**
     * Promotes (by alpha) the weights
     * responsible for the incorrect guess
     * @param lpos index of incorrectly guessed label
     * @param fv feature vector
     */
    private void promote(int lpos, List<Integer> fv, double alpha){
        int fvisize = fv.size();
        // learner predicted 0, correct is 1 -> promotion
        for(int fvi=0; fvi < fvisize; fvi++){
            int fi = fv.get(fvi);
            weights[lpos][fi] *= alpha;
        }
    }

    /**
     *Demotes (by beta) the weights
     * responsible for the incorrect guess
     * @param lpos index of incorrectly guessed label
     * @param fv feature vector
     */
    private void demote(int lpos, List<Integer> fv ,double beta){
        int fvisize = fv.size();
        // learner predicted 1, correct is 0 -> demotion
        for(int fvi=0; fvi < fvisize; fvi++){
            int fi = fv.get(fvi);
            weights[lpos][fi] /= beta;
        }
    }

    @Override
    public Class<? extends PredictionModel> getModelClass() {
        return Winnow.class;
    }

    public boolean supportsCapability(OperatorCapability capability){
        switch (capability) {
//            case BINOMINAL_ATTRIBUTES:
            case NUMERICAL_ATTRIBUTES:
            case BINOMINAL_LABEL:
            case WEIGHTED_EXAMPLES:
            case UPDATABLE:
            case MISSING_VALUES:
                return true;
            default:
                return false;
        }
    }


    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeDouble(ALPHA, "The paramter of alpha.", 1, Integer.MAX_VALUE,
                2, false));
        types.add(new ParameterTypeDouble(BETA,
                "The paramter of beta.", 1, Integer.MAX_VALUE,
                2, false));
        types.add(new ParameterTypeDouble(N_FACTOR,
                "The paramter of nfactor.", 0.01, 1,
                0.5, false));

        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
        return types;
    }

}

