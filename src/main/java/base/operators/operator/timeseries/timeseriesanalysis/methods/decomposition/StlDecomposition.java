package base.operators.operator.timeseries.timeseriesanalysis.methods.decomposition;

import com.github.servicenow.ds.stats.stl.SeasonalTrendLoess;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.Series;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.TimeSeries;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.ValueSeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import org.apache.commons.lang3.tuple.MutableTriple;

public class StlDecomposition extends AbstractDecomposition {
   private SeasonalSettingsMethod seasonalSettingsMethod;
   private TrendSettingsMethod trendSettingsMethod;
   private LowpassSettingsMethod lowpassSettingsMethod;
   SeasonalTrendLoess.Decomposition decomposition;
   boolean robust;
   int seasonality;
   Integer innerIterations;
   Integer robustIterations;
   MutableTriple seasonalTriple;
   MutableTriple trendTriple;
   MutableTriple lowpassTriple;

   private StlDecomposition(int seasonality, boolean periodic, Integer seasonalWidth, boolean robust, Integer innerIterations, Integer robustIterations) {
      this.seasonalSettingsMethod = SeasonalSettingsMethod.DEFAULT;
      this.trendSettingsMethod = TrendSettingsMethod.DEFAULT;
      this.lowpassSettingsMethod = LowpassSettingsMethod.DEFAULT;
      this.decomposition = null;
      this.seasonalTriple = null;
      this.trendTriple = null;
      this.lowpassTriple = null;
      if (seasonality < 2) {
         throw new IllegalArgumentException("Provided seasonality is smaller than 2.");
      } else {
         this.seasonality = seasonality;
         if (periodic) {
            this.seasonalSettingsMethod = SeasonalSettingsMethod.PERIODIC;
         } else {
            if (seasonalWidth == null) {
               throw new ArgumentIsNullException("seasonal width");
            }

            if (seasonalWidth < 3) {
               throw new IllegalArgumentException("Provided " + SeasonalSettingsMethod.DEFAULT.getName() + " width is smaller than 3.");
            }
         }

         this.seasonalTriple = new MutableTriple(seasonalWidth, (Object)null, (Object)null);
         this.robust = robust;
         if (!robust) {
            if (innerIterations == null) {
               throw new ArgumentIsNullException("inner iterations");
            }

            if (robustIterations == null) {
               throw new ArgumentIsNullException("robust iterations");
            }

            if (innerIterations < 1) {
               throw new IllegalIndexArgumentException("inner iterations", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
            }

            if (robustIterations < 0) {
               throw new IllegalIndexArgumentException("robust iterations", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
            }
         }

         this.innerIterations = innerIterations;
         this.robustIterations = robustIterations;
      }
   }

   public static StlDecomposition createRobustNotPeriodic(int seasonality, int seasonalWidth) {
      return new StlDecomposition(seasonality, false, seasonalWidth, true, (Integer)null, (Integer)null);
   }

   public static StlDecomposition createNonRobustNotPeriodic(int seasonality, int seasonalWidth, int innerIterations, int robustIterations) {
      return new StlDecomposition(seasonality, false, seasonalWidth, false, innerIterations, robustIterations);
   }

   public static StlDecomposition createRobustPeriodic(int seasonality) {
      return new StlDecomposition(seasonality, true, (Integer)null, true, (Integer)null, (Integer)null);
   }

   public static StlDecomposition createNonRobustPeriodic(int seasonality, int innerIterations, int robustIterations) {
      return new StlDecomposition(seasonality, true, (Integer)null, false, innerIterations, robustIterations);
   }

   public void setAllSeasonalSettings(Integer width, Integer degree, Integer jump) {
      if (width == null) {
         throw new ArgumentIsNullException("seasonal width");
      } else {
         this.seasonalSettingsMethod = (SeasonalSettingsMethod)this.checkSettingsMethod(width, degree, jump, this.seasonalSettingsMethod);
         this.seasonalTriple.setLeft(width);
         if (degree != null) {
            this.seasonalTriple.setMiddle(degree);
         }

         if (jump != null) {
            this.seasonalTriple.setRight(jump);
         }

      }
   }

   public void setFlatTrend() {
      if (this.trendSettingsMethod != TrendSettingsMethod.DEFAULT && this.trendSettingsMethod != TrendSettingsMethod.FLAT) {
         throw new IllegalArgumentException("The trend settings method is set to " + this.trendSettingsMethod.toString() + ". Setting it to " + TrendSettingsMethod.FLAT.toString() + " is not allowed.");
      } else {
         this.trendSettingsMethod = TrendSettingsMethod.FLAT;
      }
   }

   public void setLinearTrend() {
      if (this.trendSettingsMethod != TrendSettingsMethod.DEFAULT && this.trendSettingsMethod != TrendSettingsMethod.LINEAR) {
         throw new IllegalArgumentException("The trend settings method is set to " + this.trendSettingsMethod.toString() + ". Setting it to " + TrendSettingsMethod.LINEAR.toString() + " is not allowed.");
      } else {
         this.trendSettingsMethod = TrendSettingsMethod.LINEAR;
      }
   }

   public void setAllTrendSettings(Integer width, Integer degree, Integer jump) {
      this.trendSettingsMethod = (TrendSettingsMethod)this.checkSettingsMethod(width, degree, jump, this.trendSettingsMethod);
      this.trendTriple = new MutableTriple(width, degree, jump);
   }

   public void setAllLowpassSettings(Integer width, Integer degree, Integer jump) {
      this.lowpassSettingsMethod = (LowpassSettingsMethod)this.checkSettingsMethod(width, degree, jump, this.lowpassSettingsMethod);
      this.lowpassTriple = new MutableTriple(width, degree, jump);
   }

   private SettingsMethod checkSettingsMethod(Integer width, Integer degree, Integer jump, SettingsMethod currentSettingsMethod) {
      if (width != null && width < 3) {
         throw new IllegalArgumentException("Provided " + currentSettingsMethod.getName() + " width is smaller than 3.");
      } else if (degree != null && (degree < 0 || degree > 2)) {
         throw new IllegalArgumentException("Provided " + currentSettingsMethod.getName() + " degree is not 0,1,2.");
      } else if (jump != null && jump < 1) {
         throw new IllegalIndexArgumentException(currentSettingsMethod.getName() + " jump", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE_ZERO);
      } else {
         int index = 0;
         if (width != null) {
            ++index;
         }

         if (degree != null) {
            index += 2;
         }

         if (jump != null) {
            index += 4;
         }

         String[] methods = new String[]{"default", "width", "degree", "width and degree", "jump", "width and jump", "degree and jump", "all"};
         SettingsMethod newSettingsMethod = currentSettingsMethod.get(methods[index]);
         if (!currentSettingsMethod.isDefault() && newSettingsMethod != currentSettingsMethod) {
            throw new IllegalArgumentException("The " + currentSettingsMethod.getName() + " method is set to " + currentSettingsMethod.toString() + ". Setting it to " + newSettingsMethod.toString() + " is not allowed.");
         } else {
            return newSettingsMethod;
         }
      }
   }

   protected ValueSeries computeValueSeriesTrend(ValueSeries originalSeries) {
      String trendSeriesName = originalSeries.getName() + "_Trend";
      return originalSeries.hasDefaultIndices() ? ValueSeries.create(this.decomposition.getTrend(), trendSeriesName) : ValueSeries.create(originalSeries.getIndices(), this.decomposition.getTrend(), trendSeriesName);
   }

   protected TimeSeries computeTimeSeriesTrend(TimeSeries originalSeries) {
      return TimeSeries.create(originalSeries.getIndices(), this.decomposition.getTrend(), originalSeries.getName() + "_Trend");
   }

   protected ValueSeries computeValueSeriesSeasonal(ValueSeries originalSeries, ValueSeries trendSeries) {
      String seasonalSeriesName = originalSeries.getName() + "_Seasonal";
      return originalSeries.hasDefaultIndices() ? ValueSeries.create(this.decomposition.getSeasonal(), seasonalSeriesName) : ValueSeries.create(originalSeries.getIndices(), this.decomposition.getSeasonal(), seasonalSeriesName);
   }

   protected TimeSeries computeTimeSeriesSeasonal(TimeSeries originalSeries, TimeSeries trendSeries) {
      return TimeSeries.create(originalSeries.getIndices(), this.decomposition.getSeasonal(), originalSeries.getName() + "_Seasonal");
   }

   protected ValueSeries computeValueSeriesRemainder(ValueSeries originalSeries, ValueSeries trendSeries, ValueSeries seasonalSeries) {
      String seasonalSeriesName = originalSeries.getName() + "_Remainder";
      return originalSeries.hasDefaultIndices() ? ValueSeries.create(this.decomposition.getResidual(), seasonalSeriesName) : ValueSeries.create(originalSeries.getIndices(), this.decomposition.getResidual(), seasonalSeriesName);
   }

   protected TimeSeries computeTimeSeriesRemainder(TimeSeries originalSeries, TimeSeries trendSeries, TimeSeries seasonalSeries) {
      return TimeSeries.create(originalSeries.getIndices(), this.decomposition.getResidual(), originalSeries.getName() + "_Remainder");
   }

   protected void initializeOneSeries(Series series) {
      this.decomposition = this.intializeBuilder().buildSmoother(series.getValues()).decompose();
   }

   private SeasonalTrendLoess.Builder intializeBuilder() {
      SeasonalTrendLoess.Builder smootherBuilder = (new SeasonalTrendLoess.Builder()).setPeriodLength(this.seasonality);
      if (this.robust) {
         smootherBuilder.setRobust();
      } else {
         smootherBuilder.setInnerIterations(this.innerIterations).setRobustnessIterations(this.robustIterations);
      }

      if (this.seasonalSettingsMethod == SeasonalSettingsMethod.PERIODIC) {
         smootherBuilder.setPeriodic();
      } else {
         smootherBuilder.setSeasonalWidth((Integer)this.seasonalTriple.getLeft());
      }

      if (this.seasonalTriple.getMiddle() != null) {
         smootherBuilder.setSeasonalDegree((Integer)this.seasonalTriple.getMiddle());
      }

      if (this.seasonalTriple.getRight() != null) {
         smootherBuilder.setSeasonalJump((Integer)this.seasonalTriple.getRight());
      }

      if (this.trendSettingsMethod == TrendSettingsMethod.FLAT) {
         smootherBuilder.setFlatTrend();
      } else if (this.trendSettingsMethod == TrendSettingsMethod.LINEAR) {
         smootherBuilder.setLinearTrend();
      } else if (this.trendTriple != null) {
         if (this.trendTriple.getLeft() != null) {
            smootherBuilder.setTrendWidth((Integer)this.trendTriple.getLeft());
         }

         if (this.trendTriple.getMiddle() != null) {
            smootherBuilder.setTrendDegree((Integer)this.trendTriple.getMiddle());
         }

         if (this.trendTriple.getRight() != null) {
            smootherBuilder.setTrendJump((Integer)this.trendTriple.getRight());
         }
      }

      if (this.lowpassTriple != null) {
         if (this.lowpassTriple.getLeft() != null) {
            smootherBuilder.setLowpassWidth((Integer)this.lowpassTriple.getLeft());
         }

         if (this.lowpassTriple.getMiddle() != null) {
            smootherBuilder.setLowpassDegree((Integer)this.lowpassTriple.getMiddle());
         }

         if (this.lowpassTriple.getRight() != null) {
            smootherBuilder.setLowpassJump((Integer)this.lowpassTriple.getRight());
         }
      }

      return smootherBuilder;
   }

   protected void finishOneSeries() {
      super.finishOneSeries();
      this.decomposition = null;
   }

   public SeasonalSettingsMethod getSeasonalSettingsMethod() {
      return this.seasonalSettingsMethod;
   }

   public TrendSettingsMethod getTrendSettingsMethod() {
      return this.trendSettingsMethod;
   }

   public LowpassSettingsMethod getLowpassSettingsMethod() {
      return this.lowpassSettingsMethod;
   }

   public static enum LowpassSettingsMethod implements SettingsMethod {
      DEFAULT,
      WIDTH,
      DEGREE,
      JUMP,
      WIDTH_AND_DEGREE,
      WIDTH_AND_JUMP,
      DEGREE_AND_JUMP,
      ALL;

      public String toString() {
         return this.name().toLowerCase().replace("_", " ");
      }

      public LowpassSettingsMethod get(String text) {
         LowpassSettingsMethod[] var2 = values();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            LowpassSettingsMethod method = var2[var4];
            if (text.equals(method.toString())) {
               return method;
            }
         }

         return null;
      }

      public String getName() {
         return "lowpass settings";
      }

      public boolean isDefault() {
         return this == DEFAULT;
      }
   }

   public static enum TrendSettingsMethod implements SettingsMethod {
      DEFAULT,
      FLAT,
      LINEAR,
      WIDTH,
      DEGREE,
      JUMP,
      WIDTH_AND_DEGREE,
      WIDTH_AND_JUMP,
      DEGREE_AND_JUMP,
      ALL;

      public String toString() {
         return this.name().toLowerCase().replace("_", " ");
      }

      public TrendSettingsMethod get(String text) {
         TrendSettingsMethod[] var2 = values();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            TrendSettingsMethod method = var2[var4];
            if (text.equals(method.toString())) {
               return method;
            }
         }

         return null;
      }

      public String getName() {
         return "trend settings";
      }

      public boolean isDefault() {
         return this == DEFAULT;
      }
   }

   public static enum SeasonalSettingsMethod implements SettingsMethod {
      DEFAULT,
      PERIODIC,
      WIDTH_AND_DEGREE,
      WIDTH_AND_JUMP,
      ALL;

      public String toString() {
         return this.name().toLowerCase().replace("_", " ");
      }

      public SeasonalSettingsMethod get(String text) {
         SeasonalSettingsMethod[] var2 = values();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            SeasonalSettingsMethod method = var2[var4];
            if (text.equals(method.toString())) {
               return method;
            }
         }

         if (text.equals("width")) {
            return DEFAULT;
         } else {
            return null;
         }
      }

      public String getName() {
         return "seasonal settings";
      }

      public boolean isDefault() {
         return this == DEFAULT;
      }
   }

   public interface SettingsMethod {
      SettingsMethod get(String var1);

      boolean isDefault();

      String getName();
   }
}
