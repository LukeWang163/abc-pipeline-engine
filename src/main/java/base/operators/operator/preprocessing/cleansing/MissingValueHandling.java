package base.operators.operator.preprocessing.cleansing;

import base.operators.example.ExampleSet;
import base.operators.operator.OperatorDescription;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.preprocessing.PreprocessingModel;
import base.operators.operator.preprocessing.PreprocessingOperator;

import java.util.Collection;
import java.util.Collections;

public class MissingValueHandling extends PreprocessingOperator {
    public MissingValueHandling(OperatorDescription description) {
        super(description);
    }

    @Override
    protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd) {
        amd.setNumberOfMissingValues(new MDInteger(0));
        return Collections.singleton(amd);
    }

    @Override
    public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) {
        return new MissingValuesPreprocessingModel(exampleSet);
    }

    @Override
    protected int[] getFilterValueTypes() {
        return new int[]{0};
    }

    @Override
    public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
        return MissingValuesPreprocessingModel.class;
    }

}
