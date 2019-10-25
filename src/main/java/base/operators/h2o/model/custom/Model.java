package base.operators.h2o.model.custom;

import base.operators.h2o.model.custom.Distribution.Family;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

public abstract class Model<M extends Model<M, P, O>, P extends Model.Parameters, O extends Model.Output> implements Serializable {
    private static final long serialVersionUID = -1780842134474031789L;
    public double _myDefaultThreshold = 0.5D;
    public P _parms;
    public String[] _warnings = new String[0];
    public O _output;

    public final double myDefaultThreshold() {
        return this._myDefaultThreshold;
    }

    public final boolean isSupervised() {
        return this._output.isSupervised();
    }

    public void addWarning(String s) {
        this._warnings = (String[])Arrays.copyOf(this._warnings, this._warnings.length + 1);
        this._warnings[this._warnings.length - 1] = s;
    }

    protected String[][] scoringDomains() {
        return this._output._domains;
    }

    public Model() {
    }

    public double deviance(double w, double y, double f) {
        return (new Distribution(Family.gaussian)).deviance(w, y, f);
    }

    protected String[] makeScoringNames() {
        int nc = this._output.nclasses();
        int ncols = nc == 1 ? 1 : nc + 1;
        String[] names = new String[ncols];
        names[0] = "predict";

        for(int i = 1; i < names.length; ++i) {
            names[i] = this._output.classNames()[i - 1];

            try {
                Integer.valueOf(names[i]);
                names[i] = "p" + names[i];
            } catch (Throwable var6) {
            }
        }

        return names;
    }

    public double[] myScore0(double weight, double offset, double[] data, double[] preds) {
        assert this._output.nfeatures() == data.length;

        double[] scored = this.score0(data, preds, weight, offset);
        if (this.isSupervised() && this._output.isClassifier()) {
            if (this._parms._balance_classes) {
                GenModel.correctProbabilities(scored, this._output._priorClassDist, this._output._modelClassDist);
            }

            scored[0] = (double)GenModel.getPrediction(scored, this._output._priorClassDist, data, this.myDefaultThreshold());
        }

        return scored;
    }

    protected abstract double[] score0(double[] var1, double[] var2);

    protected double[] score0(double[] data, double[] preds, double weight, double offset) {
        assert weight == 1.0D && offset == 0.0D : "Override this method for non-trivial weight/offset!";

        return this.score0(data, preds);
    }

    public double score(double[] data) {
        return (double)ArrayUtils.maxIndex(this.score0(data, new double[this._output.nclasses()]));
    }

    @Override
    public String toString() {
        return this._output.toString();
    }

    public abstract static class Output implements Serializable {
        private static final long serialVersionUID = 9219131508886550125L;
        public String[] _names;
        public long _start_time;
        public long _end_time;
        public long _run_time;
        public String[][] _domains;
        protected boolean _isSupervised;
        protected final boolean _hasOffset;
        protected final boolean _hasWeights;
        protected final boolean _hasFold;
        public double[] _distribution;
        public double[] _modelClassDist;
        public double[] _priorClassDist;
        public String _myStringRepresentation;

        protected void startClock() {
            this._start_time = System.currentTimeMillis();
        }

        protected void stopClock() {
            this._end_time = System.currentTimeMillis();
            this._run_time = this._end_time - this._start_time;
        }

        public Output() {
            this(false, false, false);
        }

        public Output(boolean hasWeights, boolean hasOffset, boolean hasFold) {
            this._hasWeights = hasWeights;
            this._hasOffset = hasOffset;
            this._hasFold = hasFold;
        }

        public int nfeatures() {
            return this._names.length - (this._hasOffset ? 1 : 0) - (this._hasWeights ? 1 : 0) - (this._hasFold ? 1 : 0) - (this.isSupervised() ? 1 : 0);
        }

        public boolean isSupervised() {
            return this._isSupervised;
        }

        public boolean hasOffset() {
            return this._hasOffset;
        }

        public boolean hasWeights() {
            return this._hasWeights;
        }

        public boolean hasFold() {
            return this._hasFold;
        }

        public String responseName() {
            return this.isSupervised() ? this._names[this.responseIdx()] : null;
        }

        public String weightsName() {
            return this._hasWeights ? this._names[this.weightsIdx()] : null;
        }

        public String offsetName() {
            return this._hasOffset ? this._names[this.offsetIdx()] : null;
        }

        public String foldName() {
            return this._hasFold ? this._names[this.foldIdx()] : null;
        }

        public String[] interactions() {
            return null;
        }

        public int weightsIdx() {
            return !this._hasWeights ? -1 : this._names.length - (this.isSupervised() ? 1 : 0) - (this.hasOffset() ? 1 : 0) - 1 - (this.hasFold() ? 1 : 0);
        }

        public int offsetIdx() {
            return !this._hasOffset ? -1 : this._names.length - (this.isSupervised() ? 1 : 0) - (this.hasFold() ? 1 : 0) - 1;
        }

        public int foldIdx() {
            return !this._hasFold ? -1 : this._names.length - (this.isSupervised() ? 1 : 0) - 1;
        }

        public int responseIdx() {
            return !this.isSupervised() ? -1 : this._names.length - 1;
        }

        public String[] classNames() {
            assert this.isSupervised();

            return this._domains != null && this._domains.length != 0 ? this._domains[this._domains.length - 1] : null;
        }

        public boolean isClassifier() {
            return this.isSupervised() && this.nclasses() > 1;
        }

        public boolean isBinomialClassifier() {
            return this.isSupervised() && this.nclasses() == 2;
        }

        public int nclasses() {
            assert this.isSupervised();

            String[] cns = this.classNames();
            return cns == null ? 1 : cns.length;
        }

        public boolean isAutoencoder() {
            return false;
        }

        @Override
        public String toString() {
            return this._myStringRepresentation;
        }
    }

    public abstract static class Parameters extends Iced<Model.Parameters> implements Serializable {
        private static final long serialVersionUID = -7303102910427511591L;
        public static final int MAX_SUPPORTED_LEVELS = 1000;
        public int _nfolds = 0;
        public boolean _keep_cross_validation_predictions = false;
        public boolean _keep_cross_validation_fold_assignment = false;
        public boolean _parallelize_cross_validation = true;
        public boolean _auto_rebalance = true;
        public Model.Parameters.FoldAssignmentScheme _fold_assignment;
        public Family _distribution;
        public double _tweedie_power;
        public double _quantile_alpha;
        public String[] _ignored_columns;
        public boolean _ignore_const_cols;
        public String _weights_column;
        public String _offset_column;
        public String _fold_column;
        public boolean _score_each_iteration;
        public double _max_runtime_secs;
        public int _stopping_rounds;
        public double _stopping_tolerance;
        public String _response_column;
        public boolean _balance_classes;
        public float _max_after_balance_size;
        public float[] _class_sampling_factors;
        public int _max_confusion_matrix_size;

        public abstract String algoName();

        public abstract String fullName();

        public abstract String javaName();

        protected long nFoldSeed() {
            return (new Random()).nextLong();
        }

        protected double defaultStoppingTolerance() {
            return 0.001D;
        }

        public abstract long progressUnits();

        public Parameters() {
            this._fold_assignment = Model.Parameters.FoldAssignmentScheme.AUTO;
            this._distribution = Family.AUTO;
            this._tweedie_power = 1.5D;
            this._quantile_alpha = 0.5D;
            this._max_runtime_secs = 0.0D;
            this._stopping_rounds = 0;
            this._stopping_tolerance = this.defaultStoppingTolerance();
            this._balance_classes = false;
            this._max_after_balance_size = 5.0F;
            this._max_confusion_matrix_size = 20;
            this._ignore_const_cols = this.defaultDropConsCols();
        }

        protected boolean defaultDropConsCols() {
            return true;
        }

        public double missingColumnsType() {
            return 0.0D / 0.0;
        }

        public static enum FoldAssignmentScheme {
            AUTO,
            Random,
            Modulo,
            Stratified;

            private FoldAssignmentScheme() {
            }
        }
    }
}
