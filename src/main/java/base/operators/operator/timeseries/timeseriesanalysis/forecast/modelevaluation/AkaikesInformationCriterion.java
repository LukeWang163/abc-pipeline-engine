package base.operators.operator.timeseries.timeseriesanalysis.forecast.modelevaluation;

public class AkaikesInformationCriterion implements InformationCriterion {
   public double compute(double logLikelihood, int numberOfParameters, int sampleSize) {
      return -2.0D * logLikelihood + (double)(2 * (numberOfParameters + 1));
   }
}
