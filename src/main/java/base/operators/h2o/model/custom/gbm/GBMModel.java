package base.operators.h2o.model.custom.gbm;

import base.operators.h2o.model.custom.Distribution;
import base.operators.h2o.model.custom.GenModel;
import base.operators.h2o.model.custom.Distribution.Family;
import base.operators.h2o.model.custom.gbm.SharedTreeModel.SharedTreeOutput;
import base.operators.h2o.model.custom.gbm.SharedTreeModel.SharedTreeParameters;
import water.util.SBPrintStream;

public class GBMModel extends SharedTreeModel<GBMModel, GBMModel.GBMParameters, GBMModel.GBMOutput> {
    private static final long serialVersionUID = -8014920077114871418L;

    public GBMModel() {
    }

    @Override
    protected double[] score0(double[] data, double[] preds, double weight, double offset, int ntrees) {
        super.score0(data, preds, weight, offset, ntrees);
        double f;
        if (((GBMModel.GBMParameters)this._parms)._distribution == Distribution.Family.bernoulli) {
            f = preds[1] + ((GBMModel.GBMOutput)this._output)._init_f + offset;
            preds[2] = (new Distribution(this._parms)).linkInv(f);
            preds[1] = 1.0D - preds[2];
        } else if (((GBMModel.GBMParameters)this._parms)._distribution == Distribution.Family.multinomial) {
            if (((GBMModel.GBMOutput)this._output).nclasses() == 2) {
                preds[1] += ((GBMModel.GBMOutput)this._output)._init_f + offset;
                preds[2] = -preds[1];
            }

            GenModel.GBM_rescale(preds);
        } else {
            f = preds[0] + ((GBMModel.GBMOutput)this._output)._init_f + offset;
            preds[0] = (new Distribution(this._parms)).linkInv(f);
        }

        return preds;
    }

    public static class GBMOutput extends SharedTreeModel.SharedTreeOutput {
        public GBMOutput() {
        }
    }

    public static class GBMParameters extends SharedTreeModel.SharedTreeParameters {
        public double _learn_rate = 0.1D;
        public double _learn_rate_annealing = 1.0D;
        public double _col_sample_rate = 1.0D;
        public double _max_abs_leafnode_pred;

        public GBMParameters() {
            this._sample_rate = 1.0D;
            this._ntrees = 50;
            this._max_depth = 5;
            this._max_abs_leafnode_pred = 1.7976931348623157E308D;
        }

        @Override
        public String algoName() {
            return "GBM";
        }

        @Override
        public String fullName() {
            return "Gradient Boosting Method";
        }

        @Override
        public String javaName() {
            return GBMModel.class.getName();
        }
    }
}
