package base.operators.h2o.model.custom.gbm;

import base.operators.h2o.model.custom.Distribution;
import base.operators.h2o.model.custom.Model;
import java.util.Arrays;

public abstract class SharedTreeModel<M extends SharedTreeModel<M, P, O>, P extends SharedTreeModel.SharedTreeParameters, O extends SharedTreeModel.SharedTreeOutput>
        extends Model<M, P, O>
{
    private static final long serialVersionUID = -820894296698104129L;

    public static abstract class SharedTreeParameters
            extends Model.Parameters
    {
        public int _ntrees = 50;


        public int _max_depth = 5;

        public double _min_rows = 10.0D;


        public int _nbins = 20;


        public int _nbins_cats = 1024;


        public double _min_split_improvement = 0.0D;

        public enum HistogramType
        {
            AUTO, UniformAdaptive, Random, QuantilesGlobal, RoundRobin;
        }

        public HistogramType _histogram_type = HistogramType.AUTO;



        public double _r2_stopping = 0.999999D;


        public long _seed = -1L;

        public int _nbins_top_level = 1024;


        public boolean _build_tree_one_node = false;

        public int _score_tree_interval = 0;

        public int _initial_score_interval = 4000;



        public int _score_interval = 4000;


        public double _sample_rate = 0.632D;


        public double[] _sample_rate_per_class;



        @Override
        public long progressUnits() { return (this._ntrees + ((this._histogram_type == HistogramType.QuantilesGlobal || this._histogram_type == HistogramType.RoundRobin) ? 1 : 0)); }









        public double _col_sample_rate_change_per_level = 1.0D;

        public double _col_sample_rate_per_tree = 1.0D;






        private static String[] CHECKPOINT_NON_MODIFIABLE_FIELDS = { "_build_tree_one_node", "_sample_rate", "_max_depth", "_min_rows", "_nbins", "_nbins_cats", "_nbins_top_level" };



        protected String[] getCheckpointNonModifiableFields() { return CHECKPOINT_NON_MODIFIABLE_FIELDS; }
    }




    @Override
    public double deviance(double w, double y, double f) { return (new Distribution(this._parms)).deviance(w, y, f); }






















    public static abstract class SharedTreeOutput
            extends Model.Output
    {
        public long[] _training_time_ms = { System.currentTimeMillis() };












        public int _ntrees = 0;
        public CompressedTree[][] _trees = new CompressedTree[this._ntrees][];


        private static final long serialVersionUID = -3510512526597141772L;


        public double _init_f;


        public CompressedTree ctree(int tnum, int knum) { return this._trees[tnum][knum]; }
    }



















    @Override
    protected double[] score0(double[] data, double[] preds, double weight, double offset) { return score0(data, preds, weight, offset, ((SharedTreeOutput)this._output)._trees.length); }




    @Override
    public double[] score0(double[] data, double[] preds) { return score0(data, preds, 1.0D, 0.0D); }




    protected double[] score0(double[] data, double[] preds, double weight, double offset, int ntrees) {
        Arrays.fill(preds, 0.0D);
        for (int tidx = 0; tidx < ntrees; tidx++) {
            score0(data, preds, tidx);
        }
        return preds;
    }



    private void score0(double[] data, double[] preds, int treeIdx) {
        CompressedTree[] treesForTreeIdx = ((SharedTreeOutput)this._output)._trees[treeIdx];
        for (int c = 0; c < treesForTreeIdx.length; c++) {
            if (treesForTreeIdx[c] != null) {
                double pred = treesForTreeIdx[c].score(data);
                assert !Double.isInfinite(pred);
                preds[(treesForTreeIdx.length == 1) ? 0 : (c + 1)] = preds[(treesForTreeIdx.length == 1) ? 0 : (c + 1)] + pred;
            }
        }
    }
}

