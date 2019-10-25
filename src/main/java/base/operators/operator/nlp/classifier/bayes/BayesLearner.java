package base.operators.operator.nlp.classifier.bayes;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.learner.AbstractLearner;
import base.operators.operator.learner.PredictionModel;
import base.operators.operator.nlp.classifier.bayes.prob.AddOneProb;
import base.operators.parameter.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BayesLearner extends AbstractLearner implements Serializable {

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";

    public BayesLearner(OperatorDescription description) {
        super(description);
    }

    private static final long serialVersionUID = 1L;

    Bayes classifier;
    //存放先验概率，键为标签label，值为该标签下各个元素及其概率
    public Map<Double, AddOneProb> d = new HashMap<>();
    //训练集的总频数
    public Double total = 0.0;

    /**
     * Trains bayes on the instance list, updating
     * {weights} according to errors
     * @param trainExampleSet ExampleSet to be trained on
     * @return Classifier object containing learned weights
     */
    public Bayes learn (ExampleSet trainExampleSet) {
        Attributes attributes = trainExampleSet.getAttributes();
//        Attribute[] regularAttributes = attributes.createRegularAttributeArray();
//        if(regularAttributes.length!=1){
//            try {
//                throw new OperatorException("表中只能含有一个规则属性（文本属性），以及ID和label特殊属性");
//            } catch (OperatorException e) {
//                e.printStackTrace();
//            }
//        }
        Attribute docAttribute = null;
        try {
            docAttribute = attributes.get(getParameterAsString(DOC_ATTRIBUTE_NAME));
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }
        for (int i = 0; i < trainExampleSet.size(); i++) {
            String[] words = trainExampleSet.getExample(i).getValueAsString(docAttribute).split("\\s+");
            double label = trainExampleSet.getExample(i).getLabel();
            if (!this.d.containsKey(label)) {
                this.d.put(label, new AddOneProb());
            }
            for (String word : words) {
                this.d.get(label).add(word, 1);
            }
        }
        for (Map.Entry<Double, AddOneProb> entry : this.d.entrySet()) {
            this.total += entry.getValue().getSum();
        }
        classifier = new Bayes (trainExampleSet, d, total);
        return classifier;
    }

    public Bayes getClassifier () { return classifier; }

    @Override
    public Class<? extends PredictionModel> getModelClass() {
        return Bayes.class;
    }

    public boolean supportsCapability(OperatorCapability capability){
        switch (capability) {
            case POLYNOMINAL_ATTRIBUTES:
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
        types.add(new ParameterTypeString(DOC_ATTRIBUTE_NAME, "The name of the document attribute.",
                false));
        return types;
    }


}
