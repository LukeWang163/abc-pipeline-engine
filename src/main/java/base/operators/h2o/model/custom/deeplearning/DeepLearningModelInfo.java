package base.operators.h2o.model.custom.deeplearning;

import base.operators.h2o.model.custom.MathUtils;
import base.operators.h2o.model.custom.RandomUtils;
import base.operators.h2o.model.custom.deeplearning.DeepLearningModel.DeepLearningParameters;
import base.operators.h2o.model.custom.deeplearning.DeepLearningModel.DeepLearningParameters.InitialWeightDistribution;
import base.operators.h2o.model.custom.deeplearning.Storage.DenseRowMatrix;
import base.operators.h2o.model.custom.deeplearning.Storage.DenseVector;
import base.operators.h2o.model.custom.deeplearning.Storage.Vector;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

public final class DeepLearningModelInfo implements Serializable {
    private static final long serialVersionUID = 3991828298127037835L;
    public DataInfo data_info;
    private DenseRowMatrix[] dense_row_weights;
    private DenseVector[] biases;
    private DenseVector[] avg_activations;
    private DenseRowMatrix[] dense_row_weights_momenta;
    private DenseVector[] biases_momenta;
    private DenseRowMatrix[] dense_row_ada_dx_g;
    private DenseVector[] biases_ada_dx_g;
    private boolean[] _saw_missing_cats;
    public DeepLearningParameters parameters;
    private double[] mean_rate;
    private double[] rms_rate;
    private double[] mean_bias;
    private double[] rms_bias;
    private double[] mean_weight;
    public double[] rms_weight;
    public double[] mean_a;
    private volatile boolean unstable = false;
    private long processed_global;
    private long processed_local;
    int[] units;
    final boolean _classification = false;
    public static DeepLearningModelInfo.GradientCheck gradientCheck = null;
    public static DeepLearningModelInfo.GradientCheck gradientCheckBias = null;

    public DataInfo data_info() {
        return this.data_info;
    }

    public long size() {
        long siz = 0L;
        DenseRowMatrix[] var3 = this.dense_row_weights;
        int var4 = var3.length;

        int var5;
        for(var5 = 0; var5 < var4; ++var5) {
            DenseRowMatrix w = var3[var5];
            if (w != null) {
                siz += w.size();
            }
        }

        DenseVector[] var7 = this.biases;
        var4 = var7.length;

        for(var5 = 0; var5 < var4; ++var5) {
            Vector b = var7[var5];
            siz += (long)b.size();
        }

        return siz;
    }

    void checkMissingCats(int[] cats) {
        if (cats != null) {
            if (this._saw_missing_cats != null) {
                for(int i = 0; i < cats.length; ++i) {
                    assert this.data_info._catMissing[i];

                    if (!this._saw_missing_cats[i]) {
                        this._saw_missing_cats[i] = cats[i] == this.data_info._catOffsets[i + 1] - 1;
                    }
                }

            }
        }
    }

    boolean has_momenta() {
        return this.get_params()._momentum_start != 0.0D || this.get_params()._momentum_stable != 0.0D;
    }

    boolean adaDelta() {
        return this.get_params()._adaptive_rate;
    }

    public final DenseRowMatrix get_weights(int i) {
        return this.dense_row_weights[i];
    }

    public final DenseVector get_biases(int i) {
        return this.biases[i];
    }

    public final DenseRowMatrix get_weights_momenta(int i) {
        return this.dense_row_weights_momenta[i];
    }

    public final DenseVector get_biases_momenta(int i) {
        return this.biases_momenta[i];
    }

    public final DenseRowMatrix get_ada_dx_g(int i) {
        return this.dense_row_ada_dx_g[i];
    }

    public final DenseVector get_biases_ada_dx_g(int i) {
        return this.biases_ada_dx_g[i];
    }

    public final DenseVector get_avg_activations(int i) {
        return this.avg_activations[i];
    }

    public final DeepLearningParameters get_params() {
        return this.parameters;
    }

    public boolean isUnstable() {
        return this.unstable;
    }

    public void setUnstable() {
        if (!this.unstable) {
            this.computeStats();
        }

        this.unstable = true;
    }

    public synchronized long get_processed_global() {
        return this.processed_global;
    }

    public synchronized void set_processed_global(long p) {
        this.processed_global = p;
    }

    public synchronized void add_processed_global(long p) {
        this.processed_global += p;
    }

    public synchronized long get_processed_local() {
        return this.processed_local;
    }

    public synchronized void set_processed_local(long p) {
        this.processed_local = p;
    }

    public synchronized void add_processed_local(long p) {
        this.processed_local += p;
    }

    public synchronized long get_processed_total() {
        return this.processed_global + this.processed_local;
    }

    public DeepLearningModelInfo() {
    }

    public String toStringAll() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.toString());

        int i;
        for(i = 0; i < this.units.length - 1; ++i) {
            sb.append("\nweights[").append(i).append("][]=").append(Arrays.toString(this.get_weights(i).raw()));
        }

        for(i = 0; i < this.units.length - 1; ++i) {
            sb.append("\nbiases[").append(i).append("][]=").append(Arrays.toString(this.get_biases(i).raw()));
        }

        if (this.has_momenta()) {
            for(i = 0; i < this.units.length - 1; ++i) {
                sb.append("\nweights_momenta[").append(i).append("][]=").append(Arrays.toString(this.get_weights_momenta(i).raw()));
            }
        }

        if (this.biases_momenta != null) {
            for(i = 0; i < this.units.length - 1; ++i) {
                sb.append("\nbiases_momenta[").append(i).append("][]=").append(Arrays.toString(this.biases_momenta[i].raw()));
            }
        }

        sb.append("\nunits[]=").append(Arrays.toString(this.units));
        sb.append("\nprocessed global: ").append(this.get_processed_global());
        sb.append("\nprocessed local:  ").append(this.get_processed_local());
        sb.append("\nprocessed total:  ").append(this.get_processed_total());
        sb.append("\n");
        return sb.toString();
    }

    void initializeFromPretrainedModel(DeepLearningModelInfo autoencoder) {
        assert autoencoder.parameters._autoencoder;

        this.randomizeWeights();

        int w;
        int i;
        for(w = 0; w < this.dense_row_weights.length - 1; ++w) {
            if (this.get_weights(w).rows() != autoencoder.get_weights(w).rows()) {
                throw new IllegalArgumentException("Mismatch between weights in pretrained model and this model: rows in layer " + w + ": " + autoencoder.get_weights(w).rows() + " vs " + this.get_weights(w).rows() + ". Enable ignored_const_cols for both models and/or check categorical levels for consistency.");
            }

            if (this.get_weights(w).cols() != autoencoder.get_weights(w).cols()) {
                throw new IllegalArgumentException("Mismatch between weights in pretrained model and this model: cols in layer " + w + ": " + autoencoder.get_weights(w).cols() + " vs " + this.get_weights(w).cols() + ". Enable ignored_const_cols for both models and/or check categorical levels for consistency.");
            }

            for(i = 0; i < this.get_weights(w).rows(); ++i) {
                for(int j = 0; j < this.get_weights(w).cols(); ++j) {
                    this.get_weights(w).set(i, j, autoencoder.get_weights(w).get(i, j));
                }
            }
        }

        for(w = 0; w < this.get_params()._hidden.length; ++w) {
            for(i = 0; i < this.biases[w].raw().length; ++i) {
                this.biases[w].set(i, autoencoder.biases[w].get(i));
            }
        }

        Arrays.fill(this.biases[this.biases.length - 1].raw(), 0.0D);
    }

    double uniformDist(Random rand, double min, double max) {
        return min + (double)rand.nextFloat() * (max - min);
    }

    private void randomizeWeights() {
        for(int w = 0; w < this.dense_row_weights.length; ++w) {
            Random rng = RandomUtils.getRNG(new long[]{this.get_params()._seed + 195911405L + (long)w + 1L});
            double range = Math.sqrt(6.0D / (double)(this.units[w] + this.units[w + 1]));

            for(int i = 0; i < this.get_weights(w).rows(); ++i) {
                for(int j = 0; j < this.get_weights(w).cols(); ++j) {
                    if (this.get_params()._initial_weight_distribution == InitialWeightDistribution.UniformAdaptive) {
                        if (w == this.dense_row_weights.length - 1 && this._classification) {
                            this.get_weights(w).set(i, j, (float)(4.0D * this.uniformDist(rng, -range, range)));
                        } else {
                            this.get_weights(w).set(i, j, (float)this.uniformDist(rng, -range, range));
                        }
                    } else if (this.get_params()._initial_weight_distribution == InitialWeightDistribution.Uniform) {
                        this.get_weights(w).set(i, j, (float)this.uniformDist(rng, -this.get_params()._initial_weight_scale, this.get_params()._initial_weight_scale));
                    } else if (this.get_params()._initial_weight_distribution == InitialWeightDistribution.Normal) {
                        this.get_weights(w).set(i, j, (float)(rng.nextGaussian() * this.get_params()._initial_weight_scale));
                    }
                }
            }
        }

    }

    public void computeStats() {
        float[][] rate = this.get_params()._adaptive_rate ? new float[this.units.length - 1][] : (float[][])null;
        double[] var10000;
        int y;
        int u;
        if (this.get_params()._autoencoder && this.get_params()._sparsity_beta > 0.0D) {
            for(y = 0; y < this.get_params()._hidden.length; ++y) {
                this.mean_a[y] = 0.0D;

                for(u = 0; u < this.avg_activations[y].size(); ++u) {
                    var10000 = this.mean_a;
                    var10000[y] += this.avg_activations[y].get(u);
                }

                var10000 = this.mean_a;
                var10000[y] /= (double)this.avg_activations[y].size();
            }
        }

        for(y = 0; y < this.units.length - 1; ++y) {
            this.mean_rate[y] = this.rms_rate[y] = 0.0D;
            this.mean_bias[y] = this.rms_bias[y] = 0.0D;
            this.mean_weight[y] = this.rms_weight[y] = 0.0D;

            for(u = 0; u < this.biases[y].size(); ++u) {
                var10000 = this.mean_bias;
                var10000[y] += this.biases[y].get(u);
            }

            if (rate != null) {
                rate[y] = new float[this.get_weights(y).raw().length];
            }

            for(u = 0; u < this.get_weights(y).raw().length; ++u) {
                var10000 = this.mean_weight;
                var10000[y] += (double)this.get_weights(y).raw()[u];
                if (rate != null) {
                    float RMS_dx = MathUtils.approxSqrt(this.get_ada_dx_g(y).raw()[2 * u] + (float)this.get_params()._epsilon);
                    float invRMS_g = MathUtils.approxInvSqrt(this.get_ada_dx_g(y).raw()[2 * u + 1] + (float)this.get_params()._epsilon);
                    rate[y][u] = RMS_dx * invRMS_g;
                    var10000 = this.mean_rate;
                    var10000[y] += (double)rate[y][u];
                }
            }

            var10000 = this.mean_bias;
            var10000[y] /= (double)this.biases[y].size();
            var10000 = this.mean_weight;
            var10000[y] /= (double)this.get_weights(y).size();
            if (rate != null) {
                var10000 = this.mean_rate;
                var10000[y] /= (double)rate[y].length;
            }

            double dw;
            for(u = 0; u < this.biases[y].size(); ++u) {
                dw = this.biases[y].get(u) - this.mean_bias[y];
                var10000 = this.rms_bias;
                var10000[y] += dw * dw;
            }

            for(u = 0; (long)u < this.get_weights(y).size(); ++u) {
                dw = (double)this.get_weights(y).raw()[u] - this.mean_weight[y];
                var10000 = this.rms_weight;
                var10000[y] += dw * dw;
                if (rate != null) {
                    double drate = (double)rate[y][u] - this.mean_rate[y];
                    var10000 = this.rms_rate;
                    var10000[y] += drate * drate;
                }
            }

            this.rms_bias[y] = MathUtils.approxSqrt(this.rms_bias[y] / (double)this.biases[y].size());
            this.rms_weight[y] = MathUtils.approxSqrt(this.rms_weight[y] / (double)this.get_weights(y).size());
            if (rate != null) {
                this.rms_rate[y] = MathUtils.approxSqrt(this.rms_rate[y] / (double)rate[y].length);
            }

            double thresh = 1.0E10D;
            double bthresh = 100000.0D;
            this.unstable |= Double.isNaN(this.mean_bias[y]) || Double.isNaN(this.rms_bias[y]) || Double.isNaN(this.mean_weight[y]) || Double.isNaN(this.rms_weight[y]) || Math.abs(this.mean_weight[y]) > 1.0E10D || this.rms_weight[y] > 1.0E10D || Math.abs(this.mean_bias[y]) > 100000.0D || this.rms_bias[y] > 100000.0D;
        }

    }

    protected long checksum_impl() {
        this.computeStats();
        Random rng = new Random(-557122629L);
        double cs = Double.longBitsToDouble(this.get_params()._seed);
        cs += (double)(this.size() * this.get_processed_total());
        double[] var4 = this.mean_bias;
        int var5 = var4.length;

        int var6;
        double d;
        for(var6 = 0; var6 < var5; ++var6) {
            d = var4[var6];
            cs += rng.nextDouble() * (d + 123.23D);
        }

        var4 = this.rms_bias;
        var5 = var4.length;

        for(var6 = 0; var6 < var5; ++var6) {
            d = var4[var6];
            cs += rng.nextDouble() * (d + 123.23D);
        }

        var4 = this.mean_weight;
        var5 = var4.length;

        for(var6 = 0; var6 < var5; ++var6) {
            d = var4[var6];
            cs += rng.nextDouble() * (d + 123.23D);
        }

        var4 = this.rms_weight;
        var5 = var4.length;

        for(var6 = 0; var6 < var5; ++var6) {
            d = var4[var6];
            cs += rng.nextDouble() * (d + 123.23D);
        }

        var4 = this.mean_rate;
        var5 = var4.length;

        for(var6 = 0; var6 < var5; ++var6) {
            d = var4[var6];
            cs += rng.nextDouble() * (d + 123.23D);
        }

        var4 = this.rms_rate;
        var5 = var4.length;

        for(var6 = 0; var6 < var5; ++var6) {
            d = var4[var6];
            cs += rng.nextDouble() * (d + 123.23D);
        }

        return Double.doubleToRawLongBits(cs);
    }

    public static class GradientCheck {
        int layer;
        int row;
        int col;
        double gradient;

        GradientCheck(int l, int r, int c) {
            this.layer = l;
            this.row = r;
            this.col = c;
            this.gradient = 0.0D;
        }

        void apply(int l, int r, int c, double g) {
            if (r == this.row && c == this.col && l == this.layer) {
                this.gradient += g;
            }

        }
    }
}
