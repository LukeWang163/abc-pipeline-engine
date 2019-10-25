package base.operators.operator.timeseries.timeseriesanalysis.forecast.modelevaluation;

public class BayesianInformationCriterion implements InformationCriterion {
   public double compute(double logLikelihood, int numberOfParameters, int sampleSize) {
      return -2.0D * logLikelihood + Math.log((double)sampleSize) * (double)(numberOfParameters + 1);
   }
}
