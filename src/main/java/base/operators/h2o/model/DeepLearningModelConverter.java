package base.operators.h2o.model;

import base.operators.h2o.model.H2ONativeModelObject;
import com.google.common.collect.ImmutableMap;
import base.operators.example.ExampleSet;
import base.operators.h2o.model.custom.CustomisationUtils;
import base.operators.operator.OperatorException;
import hex.DataInfo;
import hex.DataInfo.TransformType;
import hex.Distribution.Family;
import hex.Model.Parameters.FoldAssignmentScheme;
import hex.deeplearning.DeepLearningModelInfo;
import hex.deeplearning.DeepLearningModel.DeepLearningModelOutput;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters.Activation;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters.ClassSamplingMethod;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters.InitialWeightDistribution;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters.Loss;
import hex.deeplearning.DeepLearningModel.DeepLearningParameters.MissingValuesHandling;
import hex.deeplearning.Storage.DenseRowMatrix;
import hex.deeplearning.Storage.DenseVector;
import hex.tree.gbm.GBMModel.GBMOutput;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import water.Iced;
import water.Keyed;
import water.Lockable;

public class DeepLearningModelConverter {
    public static final Map<Class<?>, Class<?>> CLASS_CONVERSIONS = ImmutableMap.<Class<?>, Class<?>>builder().put(DeepLearningModelInfo.class, base.operators.h2o.model.custom.deeplearning.DeepLearningModelInfo.class).put(GBMOutput.class, base.operators.h2o.model.custom.gbm.GBMModel.GBMOutput.class).put(DenseRowMatrix.class, base.operators.h2o.model.custom.deeplearning.Storage.DenseRowMatrix.class).put(DenseVector.class, base.operators.h2o.model.custom.deeplearning.Storage.DenseVector.class).put(DeepLearningParameters.class, base.operators.h2o.model.custom.deeplearning.DeepLearningModel.DeepLearningParameters.class).put(DeepLearningModelOutput.class, base.operators.h2o.model.custom.deeplearning.DeepLearningModel.DeepLearningModelOutput.class).put(DataInfo.class, base.operators.h2o.model.custom.deeplearning.DataInfo.class).build();
    public static final Map<Class<? extends Enum<?>>, Class<? extends Enum<?>>> ENUM_CONVERSIONS = ImmutableMap.<Class<? extends Enum<?>>, Class<? extends Enum<?>>>builder().put(Activation.class, base.operators.h2o.model.custom.deeplearning.DeepLearningModel.DeepLearningParameters.Activation.class).put(InitialWeightDistribution.class, base.operators.h2o.model.custom.deeplearning.DeepLearningModel.DeepLearningParameters.InitialWeightDistribution.class).put(Loss.class, base.operators.h2o.model.custom.deeplearning.DeepLearningModel.DeepLearningParameters.Loss.class).put(ClassSamplingMethod.class, base.operators.h2o.model.custom.deeplearning.DeepLearningModel.DeepLearningParameters.ClassSamplingMethod.class).put(MissingValuesHandling.class, base.operators.h2o.model.custom.deeplearning.DeepLearningModel.DeepLearningParameters.MissingValuesHandling.class).put(Family.class, base.operators.h2o.model.custom.Distribution.Family.class).put(TransformType.class, base.operators.h2o.model.custom.deeplearning.DataInfo.TransformType.class).put(FoldAssignmentScheme.class, base.operators.h2o.model.custom.Model.Parameters.FoldAssignmentScheme.class).build();
    public static final Set<Class<?>> IGNORED_H2O_SUPERCLASSES = Collections.unmodifiableSet(new HashSet(Arrays.asList(Keyed.class, Lockable.class, Iced.class)));

    public DeepLearningModelConverter() {
    }

    public static DeepLearningModel convert(ExampleSet trainingExampleSet, hex.deeplearning.DeepLearningModel deepLearningModel) throws OperatorException {
        String modelString = deepLearningModel.toString();
        base.operators.h2o.model.custom.deeplearning.DeepLearningModel customDLModel = convertDeepLearningModel(deepLearningModel);
        return new DeepLearningModel(trainingExampleSet, new H2ONativeModelObject(deepLearningModel), modelString, deepLearningModel._warnings, customDLModel);
    }

    public static base.operators.h2o.model.custom.deeplearning.DeepLearningModel convertDeepLearningModel(hex.deeplearning.DeepLearningModel h2oDLModel) throws OperatorException {
        base.operators.h2o.model.custom.deeplearning.DeepLearningModel model = null;

        try {
            model = (base.operators.h2o.model.custom.deeplearning.DeepLearningModel)CustomisationUtils.convertObject(h2oDLModel, base.operators.h2o.model.custom.deeplearning.DeepLearningModel.class, CLASS_CONVERSIONS, ENUM_CONVERSIONS, IGNORED_H2O_SUPERCLASSES);
            model._myDefaultThreshold = h2oDLModel.defaultThreshold();
            ((base.operators.h2o.model.custom.deeplearning.DeepLearningModel.DeepLearningModelOutput)model._output)._myStringRepresentation = ((DeepLearningModelOutput)h2oDLModel._output).toString();
            return model;
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException var3) {
            throw new OperatorException("Could not convert Deep Learning model!", var3);
        }
    }
}
