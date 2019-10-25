package base.operators.operator.nlp.fasttext;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.learner.AbstractLearner;
import base.operators.parameter.*;
import base.operators.parameter.conditions.EqualTypeCondition;
import base.operators.operator.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zls
 * create time:  2019.04.02.
 * description:
 */
public class FasttextOperator extends AbstractLearner {

    public static final String PARAMETER_SELECT_COLUMN = "select_column_name";
    public static final String PARAMETER_DIM = "dim";
    public static final String PARAMETER_MIN_COUNT_WORD = "minCountWord";
    public static final String PARAMETER_MIN_COUNT_LABEL = "minCountLabel";

    public static final String PARAMETER_LOSS= "loss";
    public static String[] LOSS_TYPES = { "hierarchical softmax", "negative sample", "softmax"};
    public static final String PARAMETER_NEG = "negative_sample";
    public static final String PARAMETER_THRESHOLD = "sampling_threshold";
    public static final String PARAMETER_LR = "learning_rate";
    public static final String PARAMETER_EPOCH = "epoch";

    public FasttextOperator(OperatorDescription description){
        super(description);
    }

    @Override
    public boolean supportsCapability(OperatorCapability lc) {
        switch (lc) {
            case POLYNOMINAL_ATTRIBUTES:
            case BINOMINAL_ATTRIBUTES:
            case POLYNOMINAL_LABEL:
            case BINOMINAL_LABEL:
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        //注：继承AbstractLearn后不能选择列，因为exampleSetInput为private
        types.add(new ParameterTypeString(PARAMETER_SELECT_COLUMN, "The name of the attribute to extract.",
                false));

        types.add(new ParameterTypeInt(PARAMETER_DIM, "size of word vectors", 20, Integer.MAX_VALUE, 50, false));
        types.add(new ParameterTypeInt(PARAMETER_MIN_COUNT_WORD, "minimal number of word occurences", 0, Integer.MAX_VALUE, 1, false));
        types.add(new ParameterTypeInt(PARAMETER_MIN_COUNT_LABEL, "minimal number of label occurences", 0 , Integer.MAX_VALUE, 0,false));
        types.add(new ParameterTypeCategory(PARAMETER_LOSS, "loss function",
                LOSS_TYPES, 2, false));
        ParameterType type = new ParameterTypeInt(PARAMETER_NEG, "number of negatives sampled", 1, 1000, 5,false);
        type.registerDependencyCondition(
                new EqualTypeCondition(this, PARAMETER_LOSS, LOSS_TYPES, true, 1));
        types.add(type);
        type = new ParameterTypeDouble(PARAMETER_THRESHOLD, "sampling threshold", 0, 1000, 1e-4,false);
        type.registerDependencyCondition(
                new EqualTypeCondition(this, PARAMETER_LOSS, LOSS_TYPES, true, 2));
        types.add(type);
        types.add(new ParameterTypeDouble(PARAMETER_LR, "learning rate", 0.0001, 1, 0.1, false));
        types.add(new ParameterTypeInt(PARAMETER_EPOCH, "training epoch", 1, Integer.MAX_VALUE, 10, false));
        return types;
    }

    public Model learn(ExampleSet exampleSet) throws OperatorException {

        int dim = getParameterAsInt(PARAMETER_DIM);
        int minCountWord = getParameterAsInt(PARAMETER_MIN_COUNT_WORD);
        int minCountLabel = getParameterAsInt(PARAMETER_MIN_COUNT_LABEL);
        String loss = getParameterAsString(PARAMETER_LOSS);
        int neg = getParameterAsInt(PARAMETER_NEG);
        double threshold = getParameterAsDouble(PARAMETER_THRESHOLD);
        double lr = getParameterAsDouble(PARAMETER_LR);
        int epoch = getParameterAsInt(PARAMETER_EPOCH);



        Args argss = new Args();
        argss.dim = dim;
        argss.minCount = minCountWord;
        argss.minCountLabel = minCountLabel;
        for(int i=0; i<LOSS_TYPES.length; ++i){
            if(LOSS_TYPES[i].equals(loss)){
                argss.loss = Args.loss_name.fromValue(i);
            }
        }

        argss.neg = neg;
        argss.t = threshold;
        argss.lr = lr;
        argss.epoch = epoch;

        Attributes attributes = exampleSet.getAttributes();

        String selectColumn = getParameterAsString(PARAMETER_SELECT_COLUMN);
        Attribute selected = attributes.get(selectColumn);

        List<String> text = new ArrayList<>();

        for (Example row : exampleSet) {
            StringBuilder builder = new StringBuilder();
            builder.append(row.getValueAsString(selected));
            builder.append("\t__label__");
            builder.append(row.getValueAsString(attributes.getLabel()));
            text.add(builder.toString());
        }

        Model model = new FastText().train(exampleSet, text, argss);
        return model;

    }


}
