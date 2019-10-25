package base.operators.operator.timeseries.timeseriesanalysis.forecast.modelevaluation;

public class AutoCorrelationFunction implements EvaluationFunction {
   private int maxLag;

   public AutoCorrelationFunction(int maxLag) {
      this.maxLag = maxLag;
   }

   public double[] compute(double[] values) {
      double[] res = new double[this.maxLag];
      AutoCovariance autoCovariance = new AutoCovariance();
      double[] aCOV = autoCovariance.compute(values);

      for(int i = 0; i < this.maxLag; ++i) {
         res[i] = aCOV[i] / aCOV[0];
      }

      return res;
   }
}
