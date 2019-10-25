package base.operators.h2o.model.custom.deeplearning;

import base.operators.h2o.model.custom.ArrayUtils;
import base.operators.h2o.model.custom.Distribution;
import base.operators.h2o.model.custom.H2O;
import base.operators.h2o.model.custom.MathUtils;
import base.operators.h2o.model.custom.MemoryManager;
import base.operators.h2o.model.custom.MurmurHash;
import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class Neurons {
    short _k;
    int[][] _maxIncoming;
    Distribution _dist;
    protected int units;
    protected DeepLearningModel.DeepLearningParameters params;
    protected int _index;
    public Storage.DenseVector[] _origa;
    public Storage.DenseVector[] _a;
    public Storage.DenseVector[] _e;
    public Neurons _previous;
    public Neurons _input;
    DeepLearningModelInfo _minfo;
    public Storage.DenseRowMatrix _w;
    public Storage.DenseRowMatrix _wEA;
    public Storage.DenseVector _b;
    public Storage.DenseVector _bEA;
    Storage.DenseRowMatrix _wm;
    Storage.DenseVector _bm;
    Storage.DenseRowMatrix _ada_dx_g;
    Storage.DenseVector _bias_ada_dx_g;
    protected Dropout _dropout;
    private boolean _shortcut;
    public Storage.DenseVector _avg_a;

    Neurons(int units) {
        _shortcut = false;
        this.units = units;
    }

    void sanityCheck(boolean training) {
        if (this instanceof Input) {
            assert _previous == null;
        } else {
            assert _previous != null;
            if (_minfo.has_momenta()) {
                assert _wm != null;
                assert _bm != null;
                assert _ada_dx_g == null;
            }
            if (_minfo.adaDelta()) {
                if (params._rho == 0.0D) {
                    throw new IllegalArgumentException("rho must be > 0 if epsilon is >0.");
                }
                if (params._epsilon == 0.0D) {
                    throw new IllegalArgumentException("epsilon must be > 0 if rho is >0.");
                }
                assert _minfo.adaDelta();
                assert _bias_ada_dx_g != null;
                assert _wm == null;
                assert _bm == null;
            }
            if (this instanceof MaxoutDropout || this instanceof TanhDropout || this instanceof RectifierDropout) {
                assert (!training || _dropout != null);
            }
        }
    }

    @Override
    public String toString() {
        String s = this.getClass().getSimpleName();
        s = s + "\nNumber of Neurons: " + units;
        s = s + "\nParameters:\n" + params.toString();
        if (_dropout != null) {
            s = s + "\nDropout:\n" + _dropout.toString();
        }
        return s;
    }

    public final void init(Neurons[] neurons, int index, DeepLearningModel.DeepLearningParameters p, DeepLearningModelInfo minfo, boolean training) {
        _index = index - 1;
        params = (DeepLearningModel.DeepLearningParameters)p.clone();
        params._hidden_dropout_ratios = (minfo.get_params())._hidden_dropout_ratios;
        params._rate *= Math.pow(params._rate_decay, (index - 1));
        params._distribution = (minfo.get_params())._distribution;
        _dist = new Distribution(params);
        _a = new Storage.DenseVector[params._mini_batch_size];
        for (int mb = 0; mb < _a.length; ++mb) {
            _a[mb] = new Storage.DenseVector(units);
        }
        if (!(this instanceof Input)) {
            _e = new Storage.DenseVector[params._mini_batch_size];
            for (int mb = 0; mb < _e.length; ++mb) {
                _e[mb] = new Storage.DenseVector(units);
            }
        } else if (params._autoencoder && params._input_dropout_ratio > 0.0D) {
            _origa = new Storage.DenseVector[params._mini_batch_size];
            for (int mb = 0; mb < _origa.length; ++mb) {
                _origa[mb] = new Storage.DenseVector(units);
            }
        }
        if (training && (this instanceof MaxoutDropout || this instanceof TanhDropout || this instanceof RectifierDropout || this instanceof ExpRectifierDropout || this instanceof Input)) {
            _dropout = (this instanceof Input) ? ((params._input_dropout_ratio == 0.0D) ? null : new Dropout(units, params._input_dropout_ratio)) : new Dropout(units, params._hidden_dropout_ratios[_index]);
        }

        if (!(this instanceof Input)) {
            _previous = neurons[_index];
            _minfo = minfo;
            _w = minfo.get_weights(_index);
            _b = minfo.get_biases(_index);
            if (params._autoencoder && params._sparsity_beta > 0.0D && _index < params._hidden.length) {
                _avg_a = minfo.get_avg_activations(_index);
            }
            if (minfo.has_momenta()) {
                _wm = minfo.get_weights_momenta(_index);
                _bm = minfo.get_biases_momenta(_index);
            }

            if (minfo.adaDelta()) {
                _ada_dx_g = minfo.get_ada_dx_g(_index);
                _bias_ada_dx_g = minfo.get_biases_ada_dx_g(_index);
            }
            _shortcut = (params._fast_mode || (!params._adaptive_rate && !_minfo.has_momenta() && params._l1 == 0.0D && params._l2 == 0.0D));
        }
        sanityCheck(training);
    }

    protected abstract void fprop(long paramLong, boolean paramBoolean, int paramInt);

    protected abstract void bprop(int paramInt);

    protected final void bpropOutputLayer(int n) {
        assert _index == params._hidden.length;
        assert _a.length == params._mini_batch_size;

        int rows = _a[0].size();
        float m = _minfo.adaDelta() ? 0.0F : momentum();
        float r = _minfo.adaDelta() ? 0.0F : (rate(_minfo.get_processed_total()) * (1.0F - m));
        for (int row = 0; row < rows; row++) {
            double[] g = new double[n];
            for (int mb = 0; mb < n; ++mb) {
                g[mb] = _e[mb].raw()[row];
            }
            bprop(row, g, r, m, n);
        }
    }

    protected void setOutputLayerGradient(double ignored, int mb, int n) {
        assert (_minfo.get_params())._autoencoder && _index == (_minfo.get_params())._hidden.length;
        int rows = _a[mb].size();
        for (int row = 0; row < rows; row++) {
            _e[mb].set(row, autoEncoderGradient(row, mb) / n);
        }
    }

    final void bprop(int row, double[] partial_grad, float rate, float momentum, int n) {
        float rho = (float)params._rho;
        float eps = (float)params._epsilon;
        float l1 = (float)params._l1;
        float l2 = (float)params._l2;
        float max_w2 = params._max_w2;
        boolean have_momenta = _minfo.has_momenta();
        boolean have_ada = _minfo.adaDelta();
        boolean nesterov = params._nesterov_accelerated_gradient;
        boolean fast_mode = params._fast_mode;
        int cols = _previous._a[0].size();
        assert partial_grad.length == n;

        double avg_grad2 = 0.0D;

        int idx = row * cols;

        for (int mb = 0; mb < n; mb++) {
            if (_shortcut && partial_grad[mb] == 0.0D) {
                return;
            }
            boolean update_prev = (_previous._e != null && _previous._e[mb] != null);
            for (int col = 0; col < cols; col++) {
                int w = idx + col;

                if (_k != 0) {
                    w = _k * w + _maxIncoming[mb][row];
                }

                double weight = _w.raw()[w];
                if (update_prev) {
                    _previous._e[mb].add(col, partial_grad[mb] * weight);
                }

                double previous_a = _previous._a[mb].get(col);
                if (!fast_mode || previous_a != 0.0D) {

                    double grad = partial_grad[mb] * previous_a - Math.signum(weight) * l1 - weight * l2;
                    if (_wEA != null) {
                        grad -= params._elastic_averaging_regularization * (_w.raw()[w] - _wEA.raw()[w]);
                    }

                    if (DeepLearningModelInfo.gradientCheck != null) {
                        DeepLearningModelInfo.gradientCheck.apply(_index, row, col, -grad);
                    }

                    if (have_ada) {
                        double grad2 = grad * grad;
                        avg_grad2 += grad2;
                        float brate = computeAdaDeltaRateForWeight(grad, w, _ada_dx_g, rho, eps);
                        _w.raw()[w] = (float)(_w.raw()[w] + brate * grad);
                    }
                    else if (!nesterov) {
                        double delta = rate * grad;
                        _w.raw()[w] = (float)(_w.raw()[w] + delta);
                        if (have_momenta) {
                            _w.raw()[w] = _w.raw()[w] + momentum * _wm.raw()[w];
                            _wm.raw()[w] = (float)delta;
                        }
                    } else {
                        double tmp = grad;
                        if (have_momenta) {
                            _wm.raw()[w] = _wm.raw()[w] * momentum;
                            _wm.raw()[w] = (float)(_wm.raw()[w] + tmp);
                            tmp = _wm.raw()[w];
                        }
                        _w.raw()[w] = (float)(_w.raw()[w] + rate * tmp);
                    }
                }
            }
        }
        if (max_w2 != Float.POSITIVE_INFINITY) {
            for (int mb = 0; mb < n; mb++) {
                rescale_weights(_w, row, max_w2, mb);
            }
        }
        if (have_ada) {
            avg_grad2 /= (cols * n);
        }
        for (int mb = 0; mb < n; mb++) {
            update_bias(_b, _bEA, _bm, row, partial_grad, avg_grad2, rate, momentum, mb);
        }
    }

    private void rescale_weights(Storage.DenseRowMatrix w, int row, float max_w2, int mb) {
        int end, start, cols = _previous._a[0].size();

        if (_k != 0) {
            start = _k * row * cols + _maxIncoming[mb][row];
            end = _k * (row * cols + cols - 1) + _maxIncoming[mb][row];
        } else {
            if (mb > 0) {
                return;
            }
            start = row * cols;
            end = row * cols + cols;
        }
        float r2 = MathUtils.sumSquares(w.raw(), start, end);

        if (r2 > max_w2) {
            float scale = MathUtils.approxSqrt(max_w2 / r2);
            for (int c = start; c < end; c++) {
                w.raw()[c] = w.raw()[c] * scale;
            }
        }
    }

    protected double autoEncoderGradient(int row, int mb) {
        assert (_minfo.get_params())._autoencoder && _index == (_minfo.get_params())._hidden.length;
        double t = (_input._origa != null) ? _input._origa[mb].get(row) : _input._a[mb].get(row);
        double y = _a[mb].get(row);
        return _dist.gradient(t, y);
    }

    private static float computeAdaDeltaRateForWeight(double grad, int w, Storage.DenseRowMatrix ada_dx_g, float rho, float eps) {
        double grad2 = grad * grad;
        ada_dx_g.raw()[2 * w + 1] = (float)((rho * ada_dx_g.raw()[2 * w + 1]) + (1.0F - rho) * grad2);
        float rate = MathUtils.approxSqrt((ada_dx_g.raw()[2 * w] + eps) / (ada_dx_g.raw()[2 * w + 1] + eps));
        ada_dx_g.raw()[2 * w] = (float)((rho * ada_dx_g.raw()[2 * w]) + ((1.0F - rho) * rate * rate) * grad2);
        return rate;
    }

    private static double computeAdaDeltaRateForBias(double grad2, int row, Storage.DenseVector bias_ada_dx_g, float rho, float eps) {
        bias_ada_dx_g.raw()[2 * row + 1] = rho * bias_ada_dx_g.raw()[2 * row + 1] + (1.0F - rho) * grad2;
        double rate = MathUtils.approxSqrt((bias_ada_dx_g.raw()[2 * row] + eps) / (bias_ada_dx_g
                .raw()[2 * row + 1] + eps));
        bias_ada_dx_g.raw()[2 * row] = rho * bias_ada_dx_g.raw()[2 * row] + (1.0F - rho) * rate * rate * grad2;
        return rate;
    }

    void compute_sparsity() {
        if (_avg_a != null) {
            if (params._mini_batch_size > 1) {
                throw H2O.unimpl("Sparsity constraint is not yet implemented for mini-batch size > 1.");
            }
            for (int mb = 0; mb < (_minfo.get_params())._mini_batch_size; ++mb) {
                for (int row = 0; row < _avg_a.size(); row++) {
                    _avg_a.set(row, 0.999D * _avg_a.get(row) + 0.001D * _a[mb].get(row));
                }
            }
        }
    }

    private void update_bias(Storage.DenseVector _b, Storage.DenseVector _bEA, Storage.DenseVector _bm, int row, double[] partial_grad, double avg_grad2, double rate, double momentum, int mb) {
        boolean have_momenta = _minfo.has_momenta();
        boolean have_ada = _minfo.adaDelta();
        float l1 = (float)params._l1;
        float l2 = (float)params._l2;
        int b = (_k != 0) ? (_k * row + _maxIncoming[mb][row]) : row;
        double bias = _b.get(b);

        partial_grad[mb] = partial_grad[mb] - Math.signum(bias) * l1 + bias * l2;
        if (_bEA != null) {
            partial_grad[mb] = partial_grad[mb] - (bias - _bEA.get(b)) * params._elastic_averaging_regularization;
        }

        if (have_ada) {
            float rho = (float)params._rho;
            float eps = (float)params._epsilon;
            rate = computeAdaDeltaRateForBias(avg_grad2, b, _bias_ada_dx_g, rho, eps);
        }
        if (!params._nesterov_accelerated_gradient) {
            double delta = rate * partial_grad[mb];
            _b.add(b, delta);
            if (have_momenta) {
                _b.add(b, momentum * _bm.get(b));
                _bm.set(b, delta);
            }
        } else {
            double d = partial_grad[mb];
            if (have_momenta) {
                _bm.set(b, _bm.get(b) * momentum);
                _bm.add(b, d);
                d = _bm.get(b);
            }
            _b.add(b, rate * d);
        }

        if (params._autoencoder && params._sparsity_beta > 0.0D && !(this instanceof Output) && !(this instanceof Input) && _index != params._hidden.length)
        {
            _b.add(b, -(rate * params._sparsity_beta * (_avg_a.raw()[b] - params._average_activation)));
        }
        if (Double.isInfinite(_b.get(b))) {
            _minfo.setUnstable();
        }
    }

    public float rate(double n) { return (float)(params._rate / (1.0D + params._rate_annealing * n)); }

    protected float momentum() { return momentum(-1.0D); }

    public final float momentum(double n) {
        double m = params._momentum_start;
        if (params._momentum_ramp > 0.0D) {
            double num = (n != -1.0D) ? _minfo.get_processed_total() : n;
            if (num >= params._momentum_ramp) {
                m = params._momentum_stable;
            } else {
                m += (params._momentum_stable - params._momentum_start) * num / params._momentum_ramp;
            }
        }
        return (float)m;
    }

    public static class Input extends Neurons {
        private DataInfo _dinfo;

        Input(DeepLearningModel.DeepLearningParameters params, int units, DataInfo d) {
            super(units);
            _dinfo = d;
            _a = new Storage.DenseVector[params._mini_batch_size];
            for (int i = 0; i < _a.length; ++i) {
                _a[i] = new Storage.DenseVector(units);
            }
        }

        @Override
        protected void bprop(int n) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void fprop(long seed, boolean training, int n) {
            throw new UnsupportedOperationException();
        }

        public void setInput(long seed, double[] data, int mb) {
            assert _dinfo != null;
            double[] nums = MemoryManager.malloc8d(_dinfo._nums);

            int[] cats = MemoryManager.malloc4(_dinfo._cats);

            int i = 0, ncats = 0;
            for (; i < _dinfo._cats; ++i) {
                assert _dinfo._catMissing[i];

                if (Double.isNaN(data[i])) {
                    cats[ncats] = _dinfo._catOffsets[i + 1] - 1;
                } else {

                    int c = (int)data[i];

                    if (_dinfo._useAllFactorLevels) {
                        cats[ncats] = c + _dinfo._catOffsets[i];
                    } else if (c != 0) {
                        cats[ncats] = c + _dinfo._catOffsets[i] - 1;
                    }


                    if (cats[ncats] >= _dinfo._catOffsets[i + 1]) {
                        cats[ncats] = _dinfo._catOffsets[i + 1] - 1;
                    }
                }
                ncats++;
            }
            for (; i < data.length; ++i) {
                double d = data[i];
                if (_dinfo._normMul != null) {
                    d = (d - _dinfo._normSub[i - _dinfo._cats]) * _dinfo._normMul[i - _dinfo._cats];
                }
                nums[i - _dinfo._cats] = d;
            }
            setInput(seed, null, nums, ncats, cats, mb);
        }

        public void setInput(long seed, int[] numIds, double[] nums, int numcat, int[] cats, int mb) {
            Arrays.fill(_a[mb].raw(), 0.0D);

            if (params._max_categorical_features < _dinfo.fullN() - _dinfo._nums) {
                assert nums.length == _dinfo._nums;
                int M = nums.length + params._max_categorical_features;

                assert _a[mb].size() == M;

                int cM = params._max_categorical_features;

                assert _a[mb].size() == M;
                MurmurHash murmur = MurmurHash.getInstance();
                for (int i = 0; i < numcat; ++i) {
                    ByteBuffer buf = ByteBuffer.allocate(4);
                    int hashval = murmur.hash(buf.putInt(cats[i]).array(), 4, (int)params._seed);
                    _a[mb].add(Math.abs(hashval % cM), 1.0D);
                }
                for (int i = 0; i < nums.length; ++i) {
                    _a[mb].set(cM + i, Double.isNaN(nums[i]) ? 0.0D : nums[i]);
                }
            }
            else {
                assert _a[mb].size() == _dinfo.fullN();
                for (int i = 0; i < numcat; ++i) {
                    _a[mb].set(cats[i], 1.0D);
                }
                if (numIds != null) {

                    for (int i = 0; i < numIds.length; ++i) {
                        _a[mb].set(numIds[i], Double.isNaN(nums[i]) ? 0.0D : nums[i]);
                    }
                }
                else {
                    for (int i = 0; i < nums.length; ++i) {
                        _a[mb].set(_dinfo.numStart() + i, Double.isNaN(nums[i]) ? 0.0D : nums[i]);
                    }
                }
            }

            if (_dropout == null) {
                return;
            }
            if (params._autoencoder && params._input_dropout_ratio > 0.0D)
            {
                System.arraycopy(_a[mb].raw(), 0, _origa[mb].raw(), 0, _a[mb].raw().length);
            }
            seed += params._seed + 322417854L;
            _dropout.randomlySparsifyActivation(_a[mb], seed);
        }
    }

    public static class Tanh extends Neurons {
        public Tanh(int units) { super(units); }

        @Override
        protected void fprop(long seed, boolean training, int n) {
            for (int mb = 0; mb < n; ++mb) {
                gemv(_a[mb], _w, _previous._a[mb], _b, (_dropout != null) ? _dropout.bits() : null);
            }
            int rows = _a[0].size();
            for (int mb = 0; mb < n; ++mb) {
                for (int row = 0; row < rows; row++) {
                    _a[mb].set(row, 1.0D - 2.0D / (1.0D + Math.exp(2.0D * _a[mb].get(row))));
                }
            }

            compute_sparsity();
        }

        @Override
        protected void bprop(int n) {
            assert _index < (_minfo.get_params())._hidden.length;
            float m = _minfo.adaDelta() ? 0.0F : momentum();
            float r = _minfo.adaDelta() ? 0.0F : (rate(_minfo.get_processed_total()) * (1.0F - m));
            int rows = _a[0].size();
            double[] g = new double[n];
            for (int row = 0; row < rows; row++) {
                for (int mb = 0; mb < n; ++mb) {
                    g[mb] = _e[mb].get(row) * (1.0D - _a[mb].get(row) * _a[mb].get(row));
                }
                bprop(row, g, r, m, n);
            }
        }
    }

    public static class TanhDropout extends Tanh {
        public TanhDropout(int units) { super(units); }

        @Override
        protected void fprop(long seed, boolean training, int n) {
            if (training) {
                seed += params._seed + -629514240L;
                _dropout.fillBytes(seed);
                super.fprop(seed, true, n);
            } else {
                super.fprop(seed, false, n);
                for (int mb = 0; mb < n; ++mb) {
                    ArrayUtils.mult(_a[mb].raw(), 1.0D - params._hidden_dropout_ratios[_index]);
                }
            }
        }
    }

    public static class Maxout extends Neurons {
        public Maxout(DeepLearningModel.DeepLearningParameters params, short k, int units) {
            super(units);
            _k = k;
            _maxIncoming = new int[params._mini_batch_size][];
            for (int i = 0; i < _maxIncoming.length; ++i) {
                _maxIncoming[i] = new int[units];
            }
            if (_k != 2) {
                throw H2O.unimpl("Maxout is currently hardcoded for 2 channels. Trivial to enable k > 2 though.");
            }
        }

        @Override
        protected void fprop(long seed, boolean training, int n) {
            assert _b.size() == _a[0].size() * _k;
            assert _w.size() == (_a[0].size() * _previous._a[0].size() * _k);
            int rows = _a[0].size();
            double[] channel = new double[_k];
            for (int row = 0; row < rows; row++) {
                for (int mb = 0; mb < n; ++mb) {
                    _a[mb].set(row, 0.0D);
                    if (!training || _dropout == null || _dropout.unit_active(row)) {
                        int cols = _previous._a[mb].size();
                        short maxK = 0; short k;
                        for (k = 0; k < _k; k++) {
                            channel[k] = 0.0D;
                            for (int col = 0; col < cols; col++) {
                                channel[k] = channel[k] + _w.raw()[_k * (row * cols + col) + k] * _previous._a[mb].get(col);
                            }
                            channel[k] = channel[k] + _b.raw()[_k * row + k];
                            if (channel[k] > channel[maxK]) {
                                maxK = k;
                            }
                        }
                        _maxIncoming[mb][row] = maxK;
                        _a[mb].set(row, channel[maxK]);
                    }
                }
                compute_sparsity();
            }
        }

        @Override
        protected void bprop(int n) {
            assert _index != params._hidden.length;
            float m = _minfo.adaDelta() ? 0.0F : momentum();
            float r = _minfo.adaDelta() ? 0.0F : (rate(_minfo.get_processed_total()) * (1.0F - m));
            double[] g = new double[n];
            int rows = _a[0].size();
            for (int row = 0; row < rows; row++) {
                for (int mb = 0; mb < n; ++mb) {
                    g[mb] = _e[mb].get(row);
                }
                bprop(row, g, r, m, n);
            }
        }
    }

    public static class MaxoutDropout extends Maxout {
        public MaxoutDropout(DeepLearningModel.DeepLearningParameters params, short k, int units) {
            super(params, k, units);
        }

        @Override
        protected void fprop(long seed, boolean training, int n) {
            if (training) {
                seed += params._seed + 1372114957L;
                _dropout.fillBytes(seed);
                super.fprop(seed, true, n);
            } else {
                super.fprop(seed, false, n);
                for (int mb = 0; mb < n; ++mb) {
                    ArrayUtils.mult(_a[mb].raw(), 1.0D - params._hidden_dropout_ratios[_index]);
                }
            }
        }
    }

    public static class Rectifier extends Neurons {
        public Rectifier(int units) { super(units); }

        @Override
        protected void fprop(long seed, boolean training, int n) {
            for (int mb = 0; mb < n; ++mb) {
                gemv(_a[mb], _w, _previous._a[mb], _b, (_dropout != null) ? _dropout.bits() : null);
            }
            int rows = _a[0].size();
            for (int mb = 0; mb < n; ++mb) {
                for (int row = 0; row < rows; row++) {
                    _a[mb].set(row, 0.5D * (_a[mb].get(row) + Math.abs(_a[mb].get(row))));
                }
            }
            compute_sparsity();
        }

        @Override
        protected void bprop(int n) {
            assert _index < (_minfo.get_params())._hidden.length;
            float m = _minfo.adaDelta() ? 0.0F : momentum();
            float r = _minfo.adaDelta() ? 0.0F : (rate(_minfo.get_processed_total()) * (1.0F - m));
            int rows = _a[0].size();
            double[] g = new double[n];
            for (int row = 0; row < rows; row++) {
                for (int mb = 0; mb < n; ++mb)
                {
                    g[mb] = (_a[mb].get(row) > 0.0D) ? _e[mb].get(row) : 0.0D;
                }
                bprop(row, g, r, m, n);
            }
        }
    }

    public static class RectifierDropout extends Rectifier {
        public RectifierDropout(int units) { super(units); }

        @Override
        protected void fprop(long seed, boolean training, int n) {
            if (training) {
                seed += params._seed + 1014100461L;
                _dropout.fillBytes(seed);
                super.fprop(seed, true, n);
            } else {
                super.fprop(seed, false, n);
                for (int mb = 0; mb < n; ++mb) {
                    ArrayUtils.mult(_a[mb].raw(), 1.0D - params._hidden_dropout_ratios[_index]);
                }
            }
        }
    }

    public static class ExpRectifier extends Neurons {
        public ExpRectifier(int units) { super(units); }

        @Override
        protected void fprop(long seed, boolean training, int n) {
            for (int mb = 0; mb < n; ++mb) {
                gemv(_a[mb], _w, _previous._a[mb], _b, (_dropout != null) ? _dropout.bits() : null);
            }
            int rows = _a[0].size();
            for (int row = 0; row < rows; row++) {
                for (int mb = 0; mb < n; ++mb) {
                    double x = _a[mb].get(row);
                    double val = (x >= 0.0D) ? x : (Math.exp(x) - 1.0D);
                    _a[mb].set(row, val);
                }
            }
            compute_sparsity();
        }

        @Override
        protected void bprop(int n) {
            assert _index < (_minfo.get_params())._hidden.length;
            float m = _minfo.adaDelta() ? 0.0F : momentum();
            float r = _minfo.adaDelta() ? 0.0F : (rate(_minfo.get_processed_total()) * (1.0F - m));
            int rows = _a[0].size();
            for (int row = 0; row < rows; row++) {
                double[] g = new double[n];
                for (int mb = 0; mb < n; ++mb) {
                    double x = _a[mb].get(row);
                    double val = (x >= 0.0D) ? 1.0D : Math.exp(x);
                    g[mb] = _e[mb].get(row) * val;
                }
                bprop(row, g, r, m, n);
            }
        }
    }

    public static class ExpRectifierDropout extends ExpRectifier {
        public ExpRectifierDropout(int units) { super(units); }

        @Override
        protected void fprop(long seed, boolean training, int n) {
            if (training) {
                seed += params._seed + -629514240L;
                _dropout.fillBytes(seed);
                super.fprop(seed, true, n);
            } else {
                super.fprop(seed, false, n);
                for (int mb = 0; mb < n; ++mb) {
                    ArrayUtils.mult(_a[mb].raw(), 1.0D - params._hidden_dropout_ratios[_index]);
                }
            }
        }
    }

    public static abstract class Output extends Neurons {
        Output(int units) {
            super(units);
        }

        @Override
        protected void bprop(int n) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Softmax extends Output {
        public Softmax(int units) { super(units); }

        @Override
        protected void fprop(long seed, boolean training, int n) {
            for (int mb = 0; mb < n; ++mb) {
                gemv(_a[mb], _w, _previous._a[mb], _b, null);
            }
            for (int mb = 0; mb < n; ++mb) {
                double max = ArrayUtils.maxValue(_a[mb].raw());
                double scaling = 0.0D;
                int rows = _a[mb].size();
                for (int row = 0; row < rows; row++) {
                    _a[mb].set(row, Math.exp(_a[mb].get(row) - max));
                    scaling += _a[mb].get(row);
                }
                for (int row = 0; row < rows; row++) {
                    _a[mb].raw()[row] = _a[mb].raw()[row] / scaling;
                }
            }
        }

        @Override
        protected void setOutputLayerGradient(double target, int mb, int n) {
            assert target == (int)target;
            double g;
            int rows = _a[mb].size();
            for (int row = 0; row < rows; row++) {
                double t = (row == (int)target ? 1 : 0);
                double y = _a[mb].get(row);

                switch (params._loss) {
                    case Automatic:
                    case CrossEntropy:
                        g = t - y;
                        break;
                    case Absolute:
                        g = (2.0D * t - 1.0D) * (1.0D - y) * y;
                        break;
                    case Quadratic:
                        g = (t - y) * (1.0D - y) * y;
                        break;
                    case Huber:
                        if (t == 0.0D) {
                            if (y < 0.5D) {
                                g = -4.0D * y;
                            } else {
                                g = -2.0D;
                            }
                        } else if (y > 0.5D) {
                            g = 4.0D * (1.0D - y);
                        } else {
                            g = 2.0D;
                        }

                        g *= (1.0D - y) * y;
                        break;
                    default:
                        throw H2O.unimpl();
                }
                _e[mb].set(row, g / n);
            }
        }
    }

    public static class Linear extends Output {
        public Linear() {
            super(1);
        }

        @Override
        protected void fprop(long seed, boolean training, int n) {
            for (int mb = 0; mb < n; ++mb) {
                gemv(_a[mb], _w, _previous._a[mb], _b, (_dropout != null) ? _dropout.bits() : null);
            }
        }

        @Override
        protected void setOutputLayerGradient(double target, int mb, int n) {
            int row = 0;
            double y = _a[mb].get(0);
            double g = _dist.gradient(target, y);
            _e[mb].set(0, g / n);
        }
    }

    static void gemv_naive(double[] res, float[] a, double[] x, double[] y, byte[] row_bits) {
        int cols = x.length;
        int rows = y.length;
        assert res.length == rows;
        for (int row = 0; row < rows; row++) {
            res[row] = 0.0D;
            if (row_bits == null || (row_bits[row / 8] & 1 << row % 8) != 0) {
                for (int col = 0; col < cols; col++) {
                    res[row] = res[row] + a[row * cols + col] * x[col];
                }
                res[row] = res[row] + y[row];
            }
        }
    }

    static void gemv_row_optimized(double[] res, float[] a, double[] x, double[] y, byte[] row_bits) {
        int cols = x.length;
        int rows = y.length;
        assert res.length == rows;
        int extra = cols - cols % 8;
        int multiple = cols / 8 * 8 - 1;
        int idx = 0;
        for (int row = 0; row < rows; row++) {
            res[row] = 0.0D;
            if (row_bits == null || (row_bits[row / 8] & 1 << row % 8) != 0) {
                double psum0 = 0.0D, psum1 = 0.0D, psum2 = 0.0D, psum3 = 0.0D, psum4 = 0.0D, psum5 = 0.0D, psum6 = 0.0D, psum7 = 0.0D;
                for (int col = 0; col < multiple; col += 8) {
                    int off = idx + col;
                    psum0 += a[off] * x[col];
                    psum1 += a[off + 1] * x[col + 1];
                    psum2 += a[off + 2] * x[col + 2];
                    psum3 += a[off + 3] * x[col + 3];
                    psum4 += a[off + 4] * x[col + 4];
                    psum5 += a[off + 5] * x[col + 5];
                    psum6 += a[off + 6] * x[col + 6];
                    psum7 += a[off + 7] * x[col + 7];
                }
                res[row] = res[row] + psum0 + psum1 + psum2 + psum3;
                res[row] = res[row] + psum4 + psum5 + psum6 + psum7;
                for (int col = extra; col < cols; col++) {
                    res[row] = res[row] + a[idx + col] * x[col];
                }
                res[row] = res[row] + y[row];
            }
            idx += cols;
        }
    }

    static void gemv(Storage.DenseVector res, Storage.DenseRowMatrix a, Storage.DenseVector x, Storage.DenseVector y, byte[] row_bits) {
        gemv_row_optimized(res.raw(), a.raw(), x.raw(), y.raw(), row_bits);
    }

    static void gemv_naive(Storage.DenseVector res, Storage.DenseRowMatrix a, Storage.DenseVector x, Storage.DenseVector y, byte[] row_bits) {
        gemv_naive(res.raw(), a.raw(), x.raw(), y.raw(), row_bits);
    }
}
