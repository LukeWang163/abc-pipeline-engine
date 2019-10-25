package base.operators.operator.timeseries.timeseriesanalysis.forecast.modelevaluation;

import base.operators.operator.timeseries.timeseriesanalysis.forecast.arima.utils.YuleWalker;

public class PartialAutoCorrelationFunction implements EvaluationFunction {
   private int maxLag;

   public PartialAutoCorrelationFunction(int maxLag) {
      this.maxLag = maxLag;
   }

   public double[] compute(double[] values) {
      double[] pACF = new double[this.maxLag];
      pACF[0] = 1.0D;

      for(int i = 1; i < this.maxLag; ++i) {
         YuleWalker yuleWalker = YuleWalker.create(i, values, false);
         double[] res = yuleWalker.computeCoefficients();
         pACF[i] = res[res.length - 1];
      }

      return pACF;
   }
}
