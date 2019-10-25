package base.operators.h2o.model;

import base.operators.example.ExampleSet;
import base.operators.operator.OperatorException;
import hex.AUC2;
import hex.DataInfo;
import hex.ModelMetrics;
import hex.ModelMetricsBinomial;
import hex.glm.GLMModel;
import java.lang.reflect.Method;

public class GeneralizedLinearModelConverter
{
    public static GeneralizedLinearModel convert(ExampleSet trainingExampleSet, GLMModel glmModel, boolean useDefaultThreshold) throws OperatorException {
        BasicTable basicTable = convertToBasicTable(glmModel);
        GeneralizedLinearModel.GLMScore glmScore = convertToGLMScore(glmModel, useDefaultThreshold);

        return new GeneralizedLinearModel(trainingExampleSet, new H2ONativeModelObject(glmModel), basicTable.multinomialModel, basicTable.coefficientNames, basicTable.coefficients, basicTable.stdCoefficients, basicTable.zValues, basicTable.pValues, basicTable.stdError, basicTable.multinominalCoefficients, basicTable.multinominalStdCoefficients, basicTable.domain, basicTable.modelString, glmModel._warnings, glmScore);
    }


    public static class BasicTable
    {
        boolean multinomialModel;

        String[] coefficientNames;

        double[] coefficients;

        double[] stdCoefficients;

        double[] zValues;
        double[] pValues;
        double[] stdError;
        double[][] multinominalCoefficients;
        double[][] multinominalStdCoefficients;
        String[] domain;
        String modelString;
    }

    public static BasicTable convertToBasicTable(GLMModel glmModel) {
        BasicTable basicTable = new BasicTable();

        basicTable.coefficientNames = ((GLMModel.GLMOutput)glmModel._output).coefficientNames();
        basicTable.multinomialModel = ((GLMModel.GLMOutput)glmModel._output)._multinomial;
        if (!basicTable.multinomialModel) {
            basicTable.coefficients = glmModel.beta();
            basicTable.stdCoefficients = ((GLMModel.GLMOutput)glmModel._output).getNormBeta();
            if (((GLMModel.GLMOutput)glmModel._output).hasPValues()) {
                basicTable.zValues = ((GLMModel.GLMOutput)glmModel._output).zValues();
                basicTable.pValues = ((GLMModel.GLMOutput)glmModel._output).pValues();
                basicTable.stdError = ((GLMModel.GLMOutput)glmModel._output).stdErr();
            }
        } else {
            basicTable.multinominalCoefficients = ((GLMModel.GLMOutput)glmModel._output).get_global_beta_multinomial();
            basicTable.multinominalStdCoefficients = ((GLMModel.GLMOutput)glmModel._output).getNormBetaMultinomial();
            basicTable.domain = ((GLMModel.GLMOutput)glmModel._output)._domains[((GLMModel.GLMOutput)glmModel._output)._domains.length - 1];
        }
        basicTable.modelString = glmModel.toString();

        return basicTable;
    }
    public static GeneralizedLinearModel.GLMScore convertToGLMScore(GLMModel glmModel, boolean useDefaultThreshold) {
        GeneralizedLinearModel.ModelMetrics _modmet;
        DataInfo _d = glmModel.dinfo();

        int[] _fullCatOffsets = null;
        try {
            Method fullCatOffsetsMethod = DataInfo.class.getDeclaredMethod("fullCatOffsets", new Class[0]);
            fullCatOffsetsMethod.setAccessible(true);
            _fullCatOffsets = (int[])fullCatOffsetsMethod.invoke(_d, new Object[0]);
        } catch (SecurityException|IllegalArgumentException|IllegalAccessException|NoSuchMethodException|java.lang.reflect.InvocationTargetException securityException) {}





        GeneralizedLinearModel.DataInfo _dinfo = new GeneralizedLinearModel.DataInfo(_d._catOffsets, _d._cats, _d._intercept, _d._responses, _d._weights, _d._nums, _d._skipMissing, _d.catModes(), _fullCatOffsets, _d._offset, _d._numOffsets, _d._intLvls, _d._normMul, _d._normSub, _d._normRespMul, _d._normRespSub, _d._numMeans, _d._fold, _d._catLvls, _d._useAllFactorLevels);



        GLMModel.GLMParameters _p = (GLMModel.GLMParameters)glmModel._parms;


        GeneralizedLinearModel.GLMModel.GLMParameters _parms = new GeneralizedLinearModel.GLMModel.GLMParameters(_p._family.name(), _p._link.name(), _p._tweedie_link_power);

        GLMModel.GLMOutput _o = (GLMModel.GLMOutput)glmModel._output;

        ModelMetrics _mm = _o._training_metrics;


        if (_mm instanceof ModelMetricsBinomial) {
            AUC2 _a = ((ModelMetricsBinomial)_mm)._auc;

            GeneralizedLinearModel.AUC2 _auc = new GeneralizedLinearModel.AUC2(useDefaultThreshold ? -1 : _a._max_idx, _a._ths);


            _modmet = new GeneralizedLinearModel.ModelMetricsBinomial(_auc);
        } else {
            _modmet = new GeneralizedLinearModel.ModelMetrics();
        }


        GeneralizedLinearModel.GLMModel.GLMOutput _output = new GeneralizedLinearModel.GLMModel.GLMOutput(_o.nclasses(), _o.get_global_beta_multinomial(), _o.beta(), _modmet);

        GeneralizedLinearModel.GLMModel _m = new GeneralizedLinearModel.GLMModel(_parms, _output);

        return new GeneralizedLinearModel.GLMScore(_dinfo, _m, true);
    }
}
