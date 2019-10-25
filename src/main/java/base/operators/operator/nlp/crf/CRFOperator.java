package base.operators.operator.nlp.crf;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.Model;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.learner.AbstractLearner;
import base.operators.parameter.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zls
 * create time:  2019.03.26.
 * description:
 */
public class CRFOperator extends AbstractLearner {

    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String FEATURE_ATTRIBUTE_NAMES = "feature_attribute_names";
    public static final String LABEL_ATTRIBUTE_NAME = "label_attribute_name";

    public static final String PARAMETER_FREQ = "feature_frequency";
    public static final String PARAMETER_ITER = "iters";
    public static final String PARAMETER_TEMPLATE = "template";

    public static final String PARAMETER_REGULARIZATION_TYPE= "regularization_type";
    public static String[] REGULARIZATION_MODES = { "L1", "L2"};
    public static final String PARAMETER_COST = "regularization_cost";
    public static final String PARAMETER_TERMINATION_CRITERION = "termination_criterion";


    public CRFOperator(OperatorDescription description){
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
        types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the id attribute.", super.getExampleSetInputPort(),
                false));
        types.add(new ParameterTypeAttributes(FEATURE_ATTRIBUTE_NAMES, "The name of the feature attribute.", super.getExampleSetInputPort(),
                false));
        types.add(new ParameterTypeAttribute(LABEL_ATTRIBUTE_NAME, "The name of the label attribute.", super.getExampleSetInputPort(),
                false));

        types.add(new ParameterTypeInt(PARAMETER_FREQ, "use features that occur no less than (default 1)", 1, Integer.MAX_VALUE, 1, false));
        types.add(new ParameterTypeInt(PARAMETER_ITER, "set for max iterations in LBFGS routine", 1, Integer.MAX_VALUE, 100, false));
        types.add(new ParameterTypeString(PARAMETER_TEMPLATE, "Template to use in CRF", "[-1,0];[0,0];[1,0];[-2,0]/[-1,0];[-1,0]/[0,0];[0,0]/[1,0];[1,0]/[2,0];B" ,true));
        types.add(new ParameterTypeCategory(PARAMETER_REGULARIZATION_TYPE, "Determines which cost type is specified.",
                REGULARIZATION_MODES, 1, true));
        types.add(new ParameterTypeDouble(PARAMETER_COST, "set FLOAT for cost parameter", 0, 10, 1,true));
        types.add(new ParameterTypeDouble(PARAMETER_TERMINATION_CRITERION, "set for termination criterion", 0.001, 1, 0.001, true));
        return types;
    }
    public Model learn(ExampleSet exampleSet) throws OperatorException {
        String id_col = getParameterAsString(ID_ATTRIBUTE_NAME);
        String[] feature_col = getParameterAsString(FEATURE_ATTRIBUTE_NAMES).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        String label_col = getParameterAsString(LABEL_ATTRIBUTE_NAME);

        String template = getParameterAsString(PARAMETER_TEMPLATE);
        int freq = getParameterAsInt(PARAMETER_FREQ);
        String type = getParameterAsString(PARAMETER_REGULARIZATION_TYPE);
        double cost = getParameterAsDouble(PARAMETER_COST);

        int iters = getParameterAsInt(PARAMETER_ITER);
        double eta = getParameterAsDouble(PARAMETER_TERMINATION_CRITERION);


        //参数传入：
        CRFLearn.Option option = new CRFLearn.Option();
        option.freq = freq;
        option.maxiter = iters;
        option.eta = eta;
        //优先使用L2正则
        if ("L2".equals(type)) {
            option.algorithm = "CRF";

        } else {
            option.algorithm = "CRF-L1";

        }
        option.cost = cost;

        Attributes attributes = exampleSet.getAttributes();
        // 读取输入数据
        List<List<String>> text = new ArrayList<>();
        String id = "";
        List<String> temp = new ArrayList<>();
        for (Example row : exampleSet) {
            String docId = row.getValueAsString(attributes.get(id_col));
            if (!id.equals(docId)) {
                id = docId;
                if (temp.size() != 0) {
                    text.add(temp);
                    temp = new ArrayList<>();
                }
            }
            StringBuilder builder = new StringBuilder();
            for (String s : feature_col) {
                if(!"".equals(s)){
                    String docContent = row.getValueAsString(attributes.get(s));
                    builder.append(docContent);
                    builder.append("\t");
                }
            }
            String label = row.getValueAsString(attributes.get(label_col));
            builder.append(label);
            temp.add(builder.toString());

        }
        text.add(temp);


        FeatureIndex featureIndex = CRFLearn.run(template, text, option);

        return new CRFPredictionModel(exampleSet, featureIndex);
    }
}

