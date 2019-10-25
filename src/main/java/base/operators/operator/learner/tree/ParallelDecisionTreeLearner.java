package base.operators.operator.learner.tree;


import base.operators.example.ExampleSet;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.parameter.ParameterType;
import base.operators.studio.internal.Resources;

import java.util.ArrayList;
import java.util.List;

public class ParallelDecisionTreeLearner extends AbstractParallelTreeLearner {
    public ParallelDecisionTreeLearner(OperatorDescription description) { super(description); }

    @Override
    public List<ColumnTerminator> getTerminationCriteria(ExampleSet exampleSet) throws OperatorException {
        List<ColumnTerminator> result = new ArrayList<ColumnTerminator>();
        if (exampleSet.getAttributes().getLabel().isNominal()) {
            result.add(new ColumnSingleLabelTermination());
        } else {
            result.add(new ColumnSingleValueTermination());
        }
        result.add(new ColumnNoAttributeLeftTermination());
        result.add(new ColumnEmptyTermination());
        int maxDepth = getParameterAsInt("maximal_depth");
        if (maxDepth <= 0) {
            maxDepth = exampleSet.size();
        }
        result.add(new ColumnMaxDepthTermination(maxDepth));
        return result;
    }


    @Override
    public boolean supportsCapability(OperatorCapability capability) {
        switch (capability) {
            case BINOMINAL_ATTRIBUTES:
            case POLYNOMINAL_ATTRIBUTES:
            case NUMERICAL_ATTRIBUTES:
            case POLYNOMINAL_LABEL:
            case BINOMINAL_LABEL:
            case WEIGHTED_EXAMPLES:
            case MISSING_VALUES:
            case NUMERICAL_LABEL:
                return true;
        }

        return false;
    }

    @Override
    protected AbstractParallelTreeBuilder getTreeBuilder(ExampleSet exampleSet) throws OperatorException {
//        if (Resources.getConcurrencyContext(this).getParallelism() > 1) {
        if (Runtime.getRuntime().availableProcessors() -1 >1) {
            return new ConcurrentTreeBuilder(this, createCriterion(), getTerminationCriteria(exampleSet), getPruner(),
                    getSplitPreprocessing(0), getParameterAsBoolean("apply_prepruning"),
                    getParameterAsInt("number_of_prepruning_alternatives"),
                    getParameterAsInt("minimal_size_for_split"), getParameterAsInt("minimal_leaf_size"));
        }
        return new NonParallelTreeBuilder(this, createCriterion(), getTerminationCriteria(exampleSet), getPruner(),
                getSplitPreprocessing(0), getParameterAsBoolean("apply_prepruning"),
                getParameterAsInt("number_of_prepruning_alternatives"),
                getParameterAsInt("minimal_size_for_split"), getParameterAsInt("minimal_leaf_size"));
    }


    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();

        for (ParameterType type : types) {
            if ("maximal_depth".equals(type.getKey())) {
                type.setDefaultValue(Integer.valueOf(10));
            }
        }

        return types;
    }
}
