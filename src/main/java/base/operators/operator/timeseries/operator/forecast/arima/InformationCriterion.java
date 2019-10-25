package base.operators.operator.timeseries.operator.forecast.arima;

import base.operators.operator.performance.PerformanceCriterion;
import base.operators.tools.math.Averagable;

public abstract class InformationCriterion extends PerformanceCriterion {
   private static final long serialVersionUID = -6122969415752807860L;
   protected String name = "InformationCriterion";
   protected double value;

   public InformationCriterion(double value) {
      this.value = value;
   }

   public InformationCriterion(PerformanceCriterion o) {
      super(o);
   }

   public String getDescription() {
      return this.name;
   }

   public double getExampleCount() {
      return 0.0D;
   }

   public double getFitness() {
      return -this.value;
   }

   public String getName() {
      return this.name.toLowerCase();
   }

   public double getMikroAverage() {
      return this.value;
   }

   public double getMikroVariance() {
      return Double.NaN;
   }

   protected void buildSingleAverage(Averagable averagable) {
      InformationCriterion otherCriterion = (InformationCriterion)averagable;
      this.value = Math.max(this.value, otherCriterion.value);
   }

   public static class CorrectedAkaikesInformationCriterion extends InformationCriterion {
      private static final long serialVersionUID = -6219337488785750551L;

      public CorrectedAkaikesInformationCriterion(double value) {
         super(value);
         this.name = "AICc";
      }
   }

   public static class BayesianInformationCriterion extends InformationCriterion {
      private static final long serialVersionUID = 3552431499278447001L;

      public BayesianInformationCriterion(double value) {
         super(value);
         this.name = "BIC";
      }
   }

   public static class AkaikesInformationCriterion extends InformationCriterion {
      private static final long serialVersionUID = -4942151788561092106L;

      public AkaikesInformationCriterion(double value) {
         super(value);
         this.name = "AIC";
      }
   }

   public static enum CRITERION {
      aic,
      bic,
      aicc;
   }
}
