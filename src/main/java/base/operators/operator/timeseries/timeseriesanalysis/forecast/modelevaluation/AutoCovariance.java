package base.operators.operator.timeseries.timeseriesanalysis.forecast.modelevaluation;

import com.google.common.math.Stats;

public class AutoCovariance implements EvaluationFunction {
   public double[] compute(double[] values) {
      double mean = Stats.meanOf(values);
      double[] result = new double[values.length];

      for(int i = 0; i < values.length; ++i) {
         result[i] = this.singleACOV(values, mean, i);
      }

      return result;
   }

   public double singleACOV(double[] values, double mean, int lag) {
      double aCOV = 0.0D;

      for(int i = 0; i < values.length - lag; ++i) {
         aCOV += (values[i + lag] - mean) * (values[i] - mean);
      }

      return aCOV / (double)values.length;
   }
}
