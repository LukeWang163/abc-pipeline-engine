package base.operators.h2o.model;

import base.operators.example.ExampleSet;
import base.operators.example.set.ExampleSetUtilities;
import base.operators.h2o.model.custom.gbm.GBMModel;
import base.operators.operator.Model;
import base.operators.operator.OperatorException;
import base.operators.operator.learner.meta.MetaModel;
import java.util.List;

public class GradientBoostedModel extends H2OModel implements MetaModel {
    private static final long serialVersionUID = -8588357897500900532L;
    private final GBMModel customGBMModel;
    private List<Model> models = null;
    private List<String> modelNames = null;


    public GradientBoostedModel(ExampleSet exampleSet, H2ONativeModelObject h2oNativeModelObject, String modelString, String[] warnings, GBMModel customGBMModel) throws OperatorException {
        super(exampleSet, h2oNativeModelObject, modelString, warnings, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET, ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);

        this.customGBMModel = customGBMModel;
    }


    @Override
    public List<? extends Model> getModels() {
        if (this.models == null &&
                this.customGBMModel != null) {
            this.models = GradientBoostedModelConverter.convertModels(getTrainingHeader(), this.customGBMModel);
        }


        return this.models;
    }


    @Override
    public List<String> getModelNames() {
        if (this.modelNames == null &&
                this.customGBMModel != null) {
            this.modelNames = GradientBoostedModelConverter.convertModelNames(getTrainingHeader(), this.customGBMModel);
        }


        return this.modelNames;
    }


    @Override
    public void score0(double[] data, double[] preds) throws OperatorException { this.customGBMModel.myScore0(1.0D, 0.0D, data, preds); }
}
