package base.operators.h2o.model;

import base.operators.h2o.model.H2OModel;
import base.operators.h2o.model.H2ONativeModelObject;
import base.operators.example.ExampleSet;
import base.operators.example.set.ExampleSetUtilities.SetsCompareOption;
import base.operators.example.set.ExampleSetUtilities.TypesCompareOption;
import base.operators.operator.OperatorException;

public class DeepLearningModel extends H2OModel {
    private static final long serialVersionUID = -1011273814946905436L;
    private base.operators.h2o.model.custom.deeplearning.DeepLearningModel customDeepLearningModel;

    public DeepLearningModel(ExampleSet trainingExampleSet, H2ONativeModelObject h2oNativeModel, String modelString, String[] warnings, base.operators.h2o.model.custom.deeplearning.DeepLearningModel customDLModel) throws OperatorException {
        super(trainingExampleSet, h2oNativeModel, modelString, warnings, SetsCompareOption.ALLOW_SUPERSET, TypesCompareOption.ALLOW_SAME_PARENTS);
        this.customDeepLearningModel = customDLModel;
    }

    @Override
    public void score0(double[] data, double[] preds) throws OperatorException {
        this.customDeepLearningModel.myScore0(1.0D, 0.0D, data, preds);
    }
}
