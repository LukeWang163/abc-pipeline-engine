package base.operators.h2o.model.custom.deeplearning;

import base.operators.h2o.model.custom.H2O;
import java.io.Serializable;

public class DataInfo implements Serializable {
    private static final long serialVersionUID = 7947981812962840513L;
    public int[] _activeCols;
    public int _responses;
    public int _outpus;
    public DataInfo.TransformType _predictor_transform;
    public DataInfo.TransformType _response_transform;
    public boolean _useAllFactorLevels;
    public int _nums;
    public int _cats;
    public int[] _catOffsets;
    public boolean[] _catMissing;
    private int[] _catModes;
    public int[] _permutation;
    public double[] _normMul;
    public double[] _normSub;
    public double[] _normRespMul;
    public double[] _normRespSub;
    public double[] _numMeans;
    public boolean _intercept = true;
    public boolean _offset = false;
    public boolean _weights = false;
    public boolean _fold = false;
    public String[] _interactionColumns;
    public int[] _interactionVecs;
    public int[] _numOffsets;
    public final boolean _skipMissing = true;
    public final boolean _imputeMissing = false;
    public boolean _valid = false;
    public final int[][] _catLvls = (int[][])null;
    public final int[][] _intLvls = (int[][])null;
    public String[] _coefNames;
    private int[] _fullCatOffsets;

    public int[] activeCols() {
        if (this._activeCols != null) {
            return this._activeCols;
        } else {
            int[] res = new int[this.fullN() + 1];

            for(int i = 0; i < res.length; res[i] = i++) {
            }

            return res;
        }
    }

    public int[] catModes() {
        return this._catModes;
    }

    public int catMode(int cid) {
        return this._catModes[cid];
    }

    public int responseChunkId(int n) {
        return n + this._cats + this._nums + (this._weights ? 1 : 0) + (this._offset ? 1 : 0) + (this._fold ? 1 : 0);
    }

    public int foldChunkId() {
        return this._cats + this._nums + (this._weights ? 1 : 0) + (this._offset ? 1 : 0);
    }

    public int offsetChunkId() {
        return this._cats + this._nums + (this._weights ? 1 : 0);
    }

    public int weightChunkId() {
        return this._cats + this._nums;
    }

    public int outputChunkId() {
        return this.outputChunkId(0);
    }

    public int outputChunkId(int n) {
        return n + this._cats + this._nums + (this._weights ? 1 : 0) + (this._offset ? 1 : 0) + (this._fold ? 1 : 0) + this._responses;
    }

    private DataInfo() {
    }

    protected int[] fullCatOffsets() {
        return this._fullCatOffsets == null ? this._catOffsets : this._fullCatOffsets;
    }

    public void updateWeightedSigmaAndMean(double[] sigmas, double[] mean) {
        int sub = this.numNums() - this._nums;
        int i;
        if (this._predictor_transform.isSigmaScaled()) {
            if (sigmas.length + sub != this._normMul.length) {
                throw new IllegalArgumentException("Length of sigmas does not match number of scaled columns.");
            }

            for(i = 0; i < this._normMul.length; ++i) {
                this._normMul[i] = i < sub ? this._normMul[i] : (sigmas[i - sub] != 0.0D ? 1.0D / sigmas[i - sub] : 1.0D);
            }
        }

        if (this._predictor_transform.isMeanAdjusted()) {
            if (mean.length + sub != this._normSub.length) {
                throw new IllegalArgumentException("Length of means does not match number of scaled columns.");
            }

            for(i = 0; i < this._normSub.length; ++i) {
                this._normSub[i] = i < sub ? this._normSub[i] : mean[i - sub];
            }
        }

    }

    public void updateWeightedSigmaAndMeanForResponse(double[] sigmas, double[] mean) {
        if (this._response_transform.isSigmaScaled()) {
            if (sigmas.length != this._normRespMul.length) {
                throw new IllegalArgumentException("Length of sigmas does not match number of scaled columns.");
            }

            for(int i = 0; i < sigmas.length; ++i) {
                this._normRespMul[i] = sigmas[i] != 0.0D ? 1.0D / sigmas[i] : 1.0D;
            }
        }

        if (this._response_transform.isMeanAdjusted()) {
            if (mean.length != this._normRespSub.length) {
                throw new IllegalArgumentException("Length of means does not match number of scaled columns.");
            }

            System.arraycopy(mean, 0, this._normRespSub, 0, mean.length);
        }

    }

    public final int fullN() {
        return this.numNums() + this.numCats();
    }

    public final int largestCat() {
        return this._cats > 0 ? this._catOffsets[1] : 0;
    }

    public final int numStart() {
        return this._catOffsets[this._cats];
    }

    public final int numCats() {
        return this._catOffsets[this._cats];
    }

    public final int numNums() {
        int nnums = 0;
        if (this._numOffsets == null && this._intLvls.length > 0) {
            int[][] var2 = this._intLvls;
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                int[] _intLvl = var2[var4];
                nnums += _intLvl == null ? 0 : _intLvl.length - 1;
            }

            return nnums + this._nums;
        } else {
            return this._interactionVecs != null && this._numOffsets != null ? this._numOffsets[this._numOffsets.length - 1] - this.numStart() : this._nums;
        }
    }

    public final int nextNumericIdx(int currentColIdx) {
        if (this._numOffsets == null) {
            return currentColIdx < this._interactionVecs.length ? this._intLvls[currentColIdx].length : 1;
        } else {
            return currentColIdx + 1 >= this._numOffsets.length ? this.fullN() - this._numOffsets[currentColIdx] : this._numOffsets[currentColIdx + 1] - this._numOffsets[currentColIdx];
        }
    }

    public final void unScaleNumericals(double[] in, double[] out) {
        if (this._nums != 0) {
            assert in.length == out.length;

            assert in.length == this.fullN();

            for(int k = this.numStart(); k < this.fullN(); ++k) {
                double m = this._normMul == null ? 1.0D : this._normMul[k - this.numStart()];
                double s = this._normSub == null ? 0.0D : this._normSub[k - this.numStart()];
                out[k] = in[k] / m + s;
            }

        }
    }

    public final int getCategoricalId(int cid, double val) {
        if (Double.isNaN(val)) {
            return this.getCategoricalId(cid, (double)this._catModes[cid]);
        } else {
            int ival = (int)val;
            if ((double)ival != val) {
                throw new IllegalArgumentException("Categorical id must be an integer or NA (missing).");
            } else {
                return this.getCategoricalId(cid, (double)ival);
            }
        }
    }

    public static enum TransformType {
        NONE,
        STANDARDIZE,
        NORMALIZE,
        DEMEAN,
        DESCALE;

        private TransformType() {
        }

        public boolean isMeanAdjusted() {
            switch(this) {
                case NONE:
                case DESCALE:
                case NORMALIZE:
                    return false;
                case STANDARDIZE:
                case DEMEAN:
                    return true;
                default:
                    throw H2O.unimpl();
            }
        }

        public boolean isSigmaScaled() {
            switch(this) {
                case NONE:
                case NORMALIZE:
                case DEMEAN:
                    return false;
                case DESCALE:
                case STANDARDIZE:
                    return true;
                default:
                    throw H2O.unimpl();
            }
        }
    }
}
