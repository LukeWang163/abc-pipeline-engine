package base.operators.h2o.model.custom.deeplearning;

import base.operators.h2o.model.custom.Distribution;
import base.operators.h2o.model.custom.H2O;
import base.operators.h2o.model.custom.Model;
import base.operators.h2o.model.custom.RandomUtils;
import base.operators.h2o.model.custom.deeplearning.DeepLearningModelInfo;
import base.operators.h2o.model.custom.deeplearning.DeepLearningTask;


public class DeepLearningModel extends Model<DeepLearningModel, DeepLearningModel.DeepLearningParameters, DeepLearningModel.DeepLearningModelOutput> {
    private static final long serialVersionUID = 8727523665891051794L;
    private volatile DeepLearningModelInfo model_info;
    public long total_checkpointed_run_time_ms;
    public long total_training_time_ms;
    public long total_scoring_time_ms;
    public long total_setup_time_ms;
    public long actual_train_samples_per_iteration;
    public long tspiGuess;
    public double time_for_communication_us;
    public double epoch_counter;
    public int iterations;
    public boolean stopped_early;
    public long training_rows;
    public long validation_rows;
    public long _timeLastIterationEnter;
    public long _timeLastScoreStart;
    private final String unstable_msg = H2O.technote(4, "\n\nTrying to predict with an unstable model.\nJob was aborted due to observed numerical instability (exponential growth).\nEither the weights or the bias values are unreasonably large or lead to large activation values.\nTry a different initial distribution, a bounded activation function (Tanh), adding regularization\n(via max_w2, l1, l2, dropout) or learning rate (either enable adaptive_rate or use a smaller learning rate or faster annealing).");

    @Override
    public double deviance(double w, double y, double f) {
        assert this.get_params()._distribution != Distribution.Family.AUTO;

        return (new Distribution(this.get_params())).deviance(w, y, f);
    }

    public final DeepLearningModelInfo model_info() {
        return this.model_info;
    }

    public final DeepLearningModel.DeepLearningParameters get_params() {
        return this.model_info.get_params();
    }

    public DeepLearningModel() {
    }

    @Override
    protected double[] score0(double[] data, double[] preds) {
        return this.score0(data, preds, 1.0D, 0.0D);
    }

    @Override
    public double[] score0(double[] data, double[] preds, double weight, double offset) {
        int mb = 0;
        int n = 1;
        if (this.model_info().isUnstable()) {
            throw new UnsupportedOperationException(this.unstable_msg);
        } else {
            Neurons[] neurons = base.operators.h2o.model.custom.deeplearning.DeepLearningTask.makeNeuronsForTesting(this.model_info);
            ((Neurons.Input)neurons[0]).setInput(-1, data, mb);
            DeepLearningTask.fpropMiniBatch(-1, neurons, this.model_info, (DeepLearningModelInfo)null, false, (double[])null, new double[]{offset}, n);
            double[] out = neurons[neurons.length - 1]._a[mb].raw();
            if (((DeepLearningModel.DeepLearningModelOutput)this._output).isClassifier()) {
                assert preds.length == out.length + 1;

                for(int i = 0; i < preds.length - 1; ++i) {
                    preds[i + 1] = out[i];
                    if (Double.isNaN(preds[i + 1])) {
                        throw new RuntimeException("Predicted class probability NaN!");
                    }
                }

                preds[0] = -1.0D;
            } else {
                if (this.model_info().data_info()._normRespMul != null) {
                    preds[0] = out[0] / this.model_info().data_info()._normRespMul[0] + this.model_info().data_info()._normRespSub[0];
                } else {
                    preds[0] = out[0];
                }

                preds[0] = (new Distribution(this.model_info.get_params())).linkInv(preds[0]);
                if (Double.isNaN(preds[0])) {
                    throw new RuntimeException("Predicted regression target NaN!");
                }
            }

            return preds;
        }
    }

    private void score_autoencoder(double[] data, double[] preds, Neurons[] neurons, boolean reconstruction, boolean reconstruction_error_per_feature) {
        int mb = 0;
        int n = 1;

        assert this.model_info().get_params()._autoencoder;

        if (this.model_info().isUnstable()) {
            throw new UnsupportedOperationException(this.unstable_msg);
        } else {
            ((base.operators.h2o.model.custom.deeplearning.Neurons.Input)neurons[0]).setInput(-1, data, 0);
            DeepLearningTask.fpropMiniBatch(-1, neurons, this.model_info, (DeepLearningModelInfo)null, false, (double[])null, (double[])null, 1);
            double[] in = neurons[0]._a[0].raw();
            double[] out = neurons[neurons.length - 1]._a[0].raw();

            assert in.length == out.length;

            if (reconstruction) {
                this.model_info().data_info().unScaleNumericals(out, out);
                System.arraycopy(out, 0, preds, 0, out.length);
            } else if (reconstruction_error_per_feature) {
                for(int i = 0; i < in.length; ++i) {
                    preds[i] = Math.pow(out[i] - in[i], 2.0D);
                }
            } else {
                assert preds.length == 1;

                double l2 = 0.0D;

                for(int i = 0; i < in.length; ++i) {
                    l2 += Math.pow(out[i] - in[i], 2.0D);
                }

                l2 /= (double)in.length;
                preds[0] = l2;
            }

        }
    }

    public static class DeepLearningParameters extends Model.Parameters {
        private static final long serialVersionUID = -5935774882883944148L;
        public boolean _overwrite_with_best_model = true;
        public boolean _autoencoder = false;
        public boolean _use_all_factor_levels = true;
        public boolean _standardize = true;
        public DeepLearningModel.DeepLearningParameters.Activation _activation;
        public int[] _hidden;
        public double _epochs;
        public long _train_samples_per_iteration;
        public double _target_ratio_comm_to_comp;
        public long _seed;
        public boolean _adaptive_rate;
        public double _rho;
        public double _epsilon;
        public double _rate;
        public double _rate_annealing;
        public double _rate_decay;
        public double _momentum_start;
        public double _momentum_ramp;
        public double _momentum_stable;
        public boolean _nesterov_accelerated_gradient;
        public double _input_dropout_ratio;
        public double[] _hidden_dropout_ratios;
        public double _l1;
        public double _l2;
        public float _max_w2;
        public DeepLearningModel.DeepLearningParameters.InitialWeightDistribution _initial_weight_distribution;
        public double _initial_weight_scale;
        public DeepLearningModel.DeepLearningParameters.Loss _loss;
        public double _score_interval;
        public long _score_training_samples;
        public long _score_validation_samples;
        public double _score_duty_cycle;
        public double _classification_stop;
        public double _regression_stop;
        public boolean _quiet_mode;
        public DeepLearningModel.DeepLearningParameters.ClassSamplingMethod _score_validation_sampling;
        public boolean _diagnostics;
        public boolean _variable_importances;
        public boolean _fast_mode;
        public boolean _force_load_balance;
        public boolean _replicate_training_data;
        public boolean _single_node_mode;
        public boolean _shuffle_training_data;
        public DeepLearningModel.DeepLearningParameters.MissingValuesHandling _missing_values_handling;
        public boolean _sparse;
        public boolean _col_major;
        public double _average_activation;
        public double _sparsity_beta;
        public int _max_categorical_features;
        public boolean _reproducible;
        public boolean _export_weights_and_biases;
        public boolean _elastic_averaging;
        public double _elastic_averaging_moving_rate;
        public double _elastic_averaging_regularization;
        public int _mini_batch_size;

        @Override
        public String algoName() {
            return "DeepLearning";
        }

        @Override
        public String fullName() {
            return "Deep Learning";
        }

        @Override
        public String javaName() {
            return DeepLearningModel.class.getName();
        }

        @Override
        protected double defaultStoppingTolerance() {
            return 0.0D;
        }

        public DeepLearningParameters() {
            this._activation = DeepLearningModel.DeepLearningParameters.Activation.Rectifier;
            this._hidden = new int[]{200, 200};
            this._epochs = 10.0D;
            this._train_samples_per_iteration = -2L;
            this._target_ratio_comm_to_comp = 0.05D;
            this._seed = RandomUtils.getRNG(new long[]{System.nanoTime()}).nextLong();
            this._adaptive_rate = true;
            this._rho = 0.99D;
            this._epsilon = 1.0E-8D;
            this._rate = 0.005D;
            this._rate_annealing = 1.0E-6D;
            this._rate_decay = 1.0D;
            this._momentum_start = 0.0D;
            this._momentum_ramp = 1000000.0D;
            this._momentum_stable = 0.0D;
            this._nesterov_accelerated_gradient = true;
            this._input_dropout_ratio = 0.0D;
            this._l1 = 0.0D;
            this._l2 = 0.0D;
            this._max_w2 = 1.0F / 0.0F;
            this._initial_weight_distribution = DeepLearningModel.DeepLearningParameters.InitialWeightDistribution.UniformAdaptive;
            this._initial_weight_scale = 1.0D;
            this._loss = DeepLearningModel.DeepLearningParameters.Loss.Automatic;
            this._score_interval = 5.0D;
            this._score_training_samples = 10000L;
            this._score_validation_samples = 0L;
            this._score_duty_cycle = 0.1D;
            this._classification_stop = 0.0D;
            this._regression_stop = 1.0E-6D;
            this._quiet_mode = false;
            this._score_validation_sampling = DeepLearningModel.DeepLearningParameters.ClassSamplingMethod.Uniform;
            this._diagnostics = true;
            this._variable_importances = false;
            this._fast_mode = true;
            this._force_load_balance = true;
            this._replicate_training_data = true;
            this._single_node_mode = false;
            this._shuffle_training_data = false;
            this._missing_values_handling = DeepLearningModel.DeepLearningParameters.MissingValuesHandling.MeanImputation;
            this._sparse = false;
            this._col_major = false;
            this._average_activation = 0.0D;
            this._sparsity_beta = 0.0D;
            this._max_categorical_features = 2147483647;
            this._reproducible = false;
            this._export_weights_and_biases = false;
            this._elastic_averaging = false;
            this._elastic_averaging_moving_rate = 0.9D;
            this._elastic_averaging_regularization = 0.001D;
            this._mini_batch_size = 1;
            this._stopping_rounds = 5;
        }

        @Override
        public long progressUnits() {
            return 1L;
        }

        @Override
        public double missingColumnsType() {
            return this._sparse ? 0.0D : 0.0D / 0.0;
        }

        @Override
        protected long nFoldSeed() {
            return this._seed;
        }

        public static enum Loss {
            Automatic,
            Quadratic,
            CrossEntropy,
            Huber,
            Absolute,
            Quantile;

            private Loss() {
            }
        }

        public static enum Activation {
            Tanh,
            TanhWithDropout,
            Rectifier,
            RectifierWithDropout,
            Maxout,
            MaxoutWithDropout,
            ExpRectifier,
            ExpRectifierWithDropout;

            private Activation() {
            }
        }

        public static enum InitialWeightDistribution {
            UniformAdaptive,
            Uniform,
            Normal;

            private InitialWeightDistribution() {
            }
        }

        public static enum ClassSamplingMethod {
            Uniform,
            Stratified;

            private ClassSamplingMethod() {
            }
        }

        public static enum MissingValuesHandling {
            Skip,
            MeanImputation;

            private MissingValuesHandling() {
            }
        }
    }

    public static class DeepLearningModelOutput extends Model.Output {
        private static final long serialVersionUID = 358087765055173081L;
        final boolean autoencoder = false;
        double[] normmul;
        double[] normsub;
        double[] normrespmul;
        double[] normrespsub;
        int[] catoffsets;

        public DeepLearningModelOutput() {
        }

        @Override
        public boolean isAutoencoder() {
            return this.autoencoder;
        }

        @Override
        public boolean isSupervised() {
            return !this.autoencoder;
        }
    }
}
