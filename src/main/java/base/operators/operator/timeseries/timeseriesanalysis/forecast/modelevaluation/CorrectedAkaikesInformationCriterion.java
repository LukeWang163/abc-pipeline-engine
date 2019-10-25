package base.operators.operator.timeseries.timeseriesanalysis.forecast.modelevaluation;

public class CorrectedAkaikesInformationCriterion implements InformationCriterion {
   public double compute(double logLikelihood, int numberOfParameters, int sampleSize) {
      int numParams = numberOfParameters + 1;
      if (sampleSize - numParams - 1 <= 0) {
         throw new IllegalArgumentException("(sampleSize - numParams - 1) is smaller than or equal to 0. The calculated Corrected Akaikes Information Criterion would be negative or an divide by 0 error would occur.");
      } else {
         AkaikesInformationCriterion aic = new AkaikesInformationCriterion();
         return aic.compute(logLikelihood, numParams, sampleSize) + 2.0D * (double)numParams * ((double)numParams + 1.0D) / ((double)(sampleSize - numParams) - 1.0D);
      }
   }
}
