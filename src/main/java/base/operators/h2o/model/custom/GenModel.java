package base.operators.h2o.model.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class GenModel
{
    public static int getPrediction(double[] preds, double[] priorClassDist, double[] data, double threshold) {
        if (preds.length == 3) {
            return (preds[2] >= threshold) ? 1 : 0;
        }
        List<Integer> ties = new ArrayList<Integer>();
        ties.add(Integer.valueOf(0));
        int best = 1, tieCnt = 0;
        for (int c = 2; c < preds.length; c++) {
            if (preds[best] < preds[c]) {
                best = c;
                tieCnt = 0;
            } else if (preds[best] == preds[c]) {
                tieCnt++;
                ties.add(Integer.valueOf(c - 1));
            }
        }
        if (tieCnt == 0) {
            return best - 1;
        }

        long hash = 0L;
        if (data != null) {
            for (double d : data) {
                hash ^= Double.doubleToRawLongBits(d) >> 6;
            }
        }



        if (priorClassDist != null) {





            double sum = 0.0D;
            for (Integer i : ties) {
                sum += priorClassDist[i.intValue()];
            }

            Random rng = new Random(hash);
            double tie = rng.nextDouble();


            double partialSum = 0.0D;
            for (Integer i : ties) {
                partialSum += priorClassDist[i.intValue()] / sum;

                if (tie <= partialSum) {
                    return i.intValue();
                }
            }
        }


        double res = preds[best];
        int idx = (int)hash % (tieCnt + 1);
        for (best = 1; best < preds.length; best++) {
            if (res == preds[best] && --idx < 0) {
                return best - 1;
            }
        }
        throw new RuntimeException("Should Not Reach Here");
    }







    public static double log_rescale(double[] preds) {
        double maxval = Double.NEGATIVE_INFINITY;
        for (int k = 1; k < preds.length; k++) {
            maxval = Math.max(maxval, preds[k]);
        }
        assert !Double.isInfinite(maxval) : "Something is wrong with GBM trees since returned prediction is " +
                Arrays.toString(preds);

        double dsum = 0.0D;
        for (int k = 1; k < preds.length; k++) {
            preds[k] = Math.exp(preds[k] - maxval); dsum += Math.exp(preds[k] - maxval);
        }
        return dsum;
    }


    public static void GBM_rescale(double[] preds) {
        double sum = log_rescale(preds);
        for (int k = 1; k < preds.length; k++) {
            preds[k] = preds[k] / sum;
        }
    }

















    public static double[] correctProbabilities(double[] scored, double[] priorClassDist, double[] modelClassDist) {
        double probsum = 0.0D;
        for (int c = 1; c < scored.length; c++) {
            double original_fraction = priorClassDist[c - 1];
            double oversampled_fraction = modelClassDist[c - 1];
            assert !Double.isNaN(scored[c]) : "Predicted NaN class probability";
            if (original_fraction != 0.0D && oversampled_fraction != 0.0D) {
                scored[c] = scored[c] * original_fraction / oversampled_fraction;
            }
            probsum += scored[c];
        }
        if (probsum > 0.0D) {
            for (int i = 1; i < scored.length; i++) {
                scored[i] = scored[i] / probsum;
            }
        }
        return scored;
    }
}

