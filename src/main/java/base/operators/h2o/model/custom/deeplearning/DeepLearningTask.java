package base.operators.h2o.model.custom.deeplearning;

import base.operators.h2o.model.custom.deeplearning.DeepLearningModel.DeepLearningParameters;
import base.operators.h2o.model.custom.deeplearning.Neurons.ExpRectifier;
import base.operators.h2o.model.custom.deeplearning.Neurons.ExpRectifierDropout;
import base.operators.h2o.model.custom.deeplearning.Neurons.Input;
import base.operators.h2o.model.custom.deeplearning.Neurons.Linear;
import base.operators.h2o.model.custom.deeplearning.Neurons.Maxout;
import base.operators.h2o.model.custom.deeplearning.Neurons.MaxoutDropout;
import base.operators.h2o.model.custom.deeplearning.Neurons.Rectifier;
import base.operators.h2o.model.custom.deeplearning.Neurons.RectifierDropout;
import base.operators.h2o.model.custom.deeplearning.Neurons.Softmax;
import base.operators.h2o.model.custom.deeplearning.Neurons.Tanh;
import base.operators.h2o.model.custom.deeplearning.Neurons.TanhDropout;

public class DeepLearningTask {
    public DeepLearningTask() {
    }

    public static void fpropMiniBatch(long seed, Neurons[] neurons, DeepLearningModelInfo minfo, DeepLearningModelInfo consensus_minfo, boolean training, double[] responses, double[] offset, int n) {
        int mb;
        for(mb = 1; mb < neurons.length; ++mb) {
            neurons[mb].fprop(seed, training, n);
        }

        for(mb = 0; mb < n; ++mb) {
            if (offset != null && offset[mb] > 0.0D) {
                assert !minfo._classification;

                double[] m = minfo.data_info()._normRespMul;
                double[] s = minfo.data_info()._normRespSub;
                double mul = m == null ? 1.0D : m[0];
                double sub = s == null ? 0.0D : s[0];
                neurons[neurons.length - 1]._a[mb].add(0, (offset[mb] - sub) * mul);
            }

            if (training) {
                neurons[neurons.length - 1].setOutputLayerGradient(responses[mb], mb, n);
                if (consensus_minfo != null) {
                    for(int i = 1; i < neurons.length; ++i) {
                        neurons[i]._wEA = consensus_minfo.get_weights(i - 1);
                        neurons[i]._bEA = consensus_minfo.get_biases(i - 1);
                    }
                }
            }
        }

    }

    public static Neurons[] makeNeuronsForTraining(DeepLearningModelInfo minfo) {
        return makeNeurons(minfo, true);
    }

    public static Neurons[] makeNeuronsForTesting(DeepLearningModelInfo minfo) {
        return makeNeurons(minfo, false);
    }

    private static Neurons[] makeNeurons(DeepLearningModelInfo minfo, boolean training) {
        DataInfo dinfo = minfo.data_info();
        DeepLearningParameters params = minfo.get_params();
        int[] h = params._hidden;
        Neurons[] neurons = new Neurons[h.length + 2];
        neurons[0] = new Input(params, minfo.units[0], dinfo);

        int i;
        for(i = 0; i < h.length + (params._autoencoder ? 1 : 0); ++i) {
            int n = params._autoencoder && i == h.length ? minfo.units[0] : h[i];
            switch(params._activation) {
                case Tanh:
                    neurons[i + 1] = new Tanh(n);
                    break;
                case TanhWithDropout:
                    neurons[i + 1] = (Neurons)(params._autoencoder && i == h.length ? new Tanh(n) : new TanhDropout(n));
                    break;
                case Rectifier:
                    neurons[i + 1] = new Rectifier(n);
                    break;
                case RectifierWithDropout:
                    neurons[i + 1] = (Neurons)(params._autoencoder && i == h.length ? new Rectifier(n) : new RectifierDropout(n));
                    break;
                case Maxout:
                    neurons[i + 1] = new Maxout(params, (short)2, n);
                    break;
                case MaxoutWithDropout:
                    neurons[i + 1] = (Neurons)(params._autoencoder && i == h.length ? new Maxout(params, (short)2, n) : new MaxoutDropout(params, (short)2, n));
                    break;
                case ExpRectifier:
                    neurons[i + 1] = new ExpRectifier(n);
                    break;
                case ExpRectifierWithDropout:
                    neurons[i + 1] = (Neurons)(params._autoencoder && i == h.length ? new ExpRectifier(n) : new ExpRectifierDropout(n));
            }
        }

        if (!params._autoencoder) {
            if (minfo._classification) {
                neurons[neurons.length - 1] = new Softmax(minfo.units[minfo.units.length - 1]);
            } else {
                neurons[neurons.length - 1] = new Linear();
            }
        }

        for(i = 0; i < neurons.length; ++i) {
            neurons[i].init(neurons, i, params, minfo, training);
            neurons[i]._input = neurons[0];
        }

        return neurons;
    }
}
