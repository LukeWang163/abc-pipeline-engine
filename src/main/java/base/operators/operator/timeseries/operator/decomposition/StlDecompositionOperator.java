package base.operators.operator.timeseries.operator.decomposition;

import base.operators.operator.OperatorDescription;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.parameter.*;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.operator.timeseries.timeseriesanalysis.methods.decomposition.MultivariateSeriesDecomposition;
import base.operators.operator.timeseries.timeseriesanalysis.methods.decomposition.StlDecomposition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StlDecompositionOperator extends AbstractDecompositionOperator {
   public static final String PARAMETER_ROBUST = "default_robust_calculations";
   public static final String PARAMETER_INNER_ITERATIONS = "inner_iterations";
   public static final String PARAMETER_ROBUST_ITERATIONS = "robust_iterations";
   public static final String PARAMETER_SEASONAL_SMOOTHING_SETTINGS = "seasonal_smoothing_settings";
   public static final String PARAMETER_SEASONAL_WIDTH = "seasonal_width";
   public static final String PARAMETER_SEASONAL_DEGREE = "seasonal_degree";
   public static final String PARAMETER_SEASONAL_JUMP = "seasonal_jump";
   public static final String PARAMETER_TREND_SMOOTHING_SETTINGS = "trend_smoothing_settings";
   public static final String PARAMETER_TREND_WIDTH = "trend_width";
   public static final String PARAMETER_TREND_DEGREE = "trend_degree";
   public static final String PARAMETER_TREND_JUMP = "trend_jump";
   public static final String PARAMETER_LOWPASS_SMOOTHING_SETTINGS = "lowpass_smoothing_settings";
   public static final String PARAMETER_LOWPASS_WIDTH = "lowpass_width";
   public static final String PARAMETER_LOWPASS_DEGREE = "lowpass_degree";
   public static final String PARAMETER_LOWPASS_JUMP = "lowpass_jump";

   public StlDecompositionOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
   }

   protected MultivariateSeriesDecomposition createDecomposition(int seasonality) throws UndefinedParameterError {
      StlDecomposition.SeasonalSettingsMethod seasonalSettingsMethod = StlDecomposition.SeasonalSettingsMethod.DEFAULT;
      seasonalSettingsMethod = seasonalSettingsMethod.get(this.getParameterAsString("seasonal_smoothing_settings"));
      StlDecomposition.TrendSettingsMethod trendSettingsMethod = StlDecomposition.TrendSettingsMethod.DEFAULT;
      trendSettingsMethod = trendSettingsMethod.get(this.getParameterAsString("trend_smoothing_settings"));
      StlDecomposition.LowpassSettingsMethod lowpassSettingsMethod = StlDecomposition.LowpassSettingsMethod.DEFAULT;
      lowpassSettingsMethod = lowpassSettingsMethod.get(this.getParameterAsString("lowpass_smoothing_settings"));
      boolean robust = this.getParameterAsBoolean("default_robust_calculations");
      return this.handleSettings(robust, seasonality, seasonalSettingsMethod, trendSettingsMethod, lowpassSettingsMethod);
   }

   protected List parameterTypesBeforeSeasonality() {
      return new ArrayList();
   }

   protected List parameterTypesAfterSeasonality() {
      List types = new ArrayList();
      types.add(new ParameterTypeBoolean("default_robust_calculations", "This parameter defines if the default settings for robust calculations are used.", true, false));
      ParameterType type = new ParameterTypeInt("inner_iterations", "Number of inner iterations.", 1, Integer.MAX_VALUE, 2, false);
      type.registerDependencyCondition(new BooleanParameterCondition(this, "default_robust_calculations", false, false));
      types.add(type);
      type = new ParameterTypeInt("robust_iterations", "Number of robust iterations.", 0, Integer.MAX_VALUE, 0, false);
      type.registerDependencyCondition(new BooleanParameterCondition(this, "default_robust_calculations", false, false));
      types.add(type);
      types.add(new ParameterTypeCategory("seasonal_smoothing_settings", "With this parameter the used seasonal smoothing settings can be selected.", (String[])Arrays.stream(StlDecomposition.SeasonalSettingsMethod.class.getEnumConstants()).map(Enum::toString).toArray((x$0) -> {
         return new String[x$0];
      }), 0, false));
      type = new ParameterTypeInt("seasonal_width", "This parameter defines the seasonal width for the decomposition.", 3, Integer.MAX_VALUE, 37, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "seasonal_smoothing_settings", false, new String[]{StlDecomposition.SeasonalSettingsMethod.DEFAULT.toString(), StlDecomposition.SeasonalSettingsMethod.WIDTH_AND_DEGREE.toString(), StlDecomposition.SeasonalSettingsMethod.WIDTH_AND_JUMP.toString(), StlDecomposition.SeasonalSettingsMethod.ALL.toString()}));
      types.add(type);
      type = new ParameterTypeInt("seasonal_degree", "This parameter defines the seasonal width for the decomposition.", 0, 2, 1, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "seasonal_smoothing_settings", false, new String[]{StlDecomposition.SeasonalSettingsMethod.WIDTH_AND_DEGREE.toString(), StlDecomposition.SeasonalSettingsMethod.ALL.toString()}));
      types.add(type);
      type = new ParameterTypeInt("seasonal_jump", "This parameter defines the seasonal width for the decomposition.", 1, Integer.MAX_VALUE, 4, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "seasonal_smoothing_settings", false, new String[]{StlDecomposition.SeasonalSettingsMethod.WIDTH_AND_JUMP.toString(), StlDecomposition.SeasonalSettingsMethod.ALL.toString()}));
      types.add(type);
      types.add(new ParameterTypeCategory("trend_smoothing_settings", "With this parameter the used seasonal smoothing settings can be selected.", (String[])Arrays.stream(StlDecomposition.TrendSettingsMethod.class.getEnumConstants()).map(Enum::toString).toArray((x$0) -> {
         return new String[x$0];
      }), 0, false));
      type = new ParameterTypeInt("trend_width", "This parameter defines the seasonal width for the decomposition.", 3, Integer.MAX_VALUE, 35, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "trend_smoothing_settings", false, new String[]{StlDecomposition.TrendSettingsMethod.WIDTH.toString(), StlDecomposition.TrendSettingsMethod.WIDTH_AND_DEGREE.toString(), StlDecomposition.TrendSettingsMethod.WIDTH_AND_JUMP.toString(), StlDecomposition.TrendSettingsMethod.ALL.toString()}));
      types.add(type);
      type = new ParameterTypeInt("trend_degree", "This parameter defines the seasonal width for the decomposition.", 0, 2, 1, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "trend_smoothing_settings", false, new String[]{StlDecomposition.TrendSettingsMethod.DEGREE.toString(), StlDecomposition.TrendSettingsMethod.WIDTH_AND_DEGREE.toString(), StlDecomposition.TrendSettingsMethod.DEGREE_AND_JUMP.toString(), StlDecomposition.TrendSettingsMethod.ALL.toString()}));
      types.add(type);
      type = new ParameterTypeInt("trend_jump", "This parameter defines the seasonal width for the decomposition.", 1, Integer.MAX_VALUE, 4, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "trend_smoothing_settings", false, new String[]{StlDecomposition.TrendSettingsMethod.JUMP.toString(), StlDecomposition.TrendSettingsMethod.WIDTH_AND_JUMP.toString(), StlDecomposition.TrendSettingsMethod.DEGREE_AND_JUMP.toString(), StlDecomposition.TrendSettingsMethod.ALL.toString()}));
      types.add(type);
      types.add(new ParameterTypeCategory("lowpass_smoothing_settings", "With this parameter the used seasonal smoothing settings can be selected.", (String[])Arrays.stream(StlDecomposition.LowpassSettingsMethod.class.getEnumConstants()).map(Enum::toString).toArray((x$0) -> {
         return new String[x$0];
      }), 0, false));
      type = new ParameterTypeInt("lowpass_width", "This parameter defines the seasonal width for the decomposition.", 3, Integer.MAX_VALUE, 13, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "lowpass_smoothing_settings", false, new String[]{StlDecomposition.LowpassSettingsMethod.WIDTH.toString(), StlDecomposition.LowpassSettingsMethod.WIDTH_AND_DEGREE.toString(), StlDecomposition.LowpassSettingsMethod.WIDTH_AND_JUMP.toString(), StlDecomposition.LowpassSettingsMethod.ALL.toString()}));
      types.add(type);
      type = new ParameterTypeInt("lowpass_degree", "This parameter defines the seasonal width for the decomposition.", 0, 2, 1, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "lowpass_smoothing_settings", false, new String[]{StlDecomposition.LowpassSettingsMethod.DEGREE.toString(), StlDecomposition.LowpassSettingsMethod.WIDTH_AND_DEGREE.toString(), StlDecomposition.LowpassSettingsMethod.DEGREE_AND_JUMP.toString(), StlDecomposition.LowpassSettingsMethod.ALL.toString()}));
      types.add(type);
      type = new ParameterTypeInt("lowpass_jump", "This parameter defines the seasonal width for the decomposition.", 1, Integer.MAX_VALUE, 2, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "lowpass_smoothing_settings", false, new String[]{StlDecomposition.LowpassSettingsMethod.JUMP.toString(), StlDecomposition.LowpassSettingsMethod.WIDTH_AND_JUMP.toString(), StlDecomposition.LowpassSettingsMethod.DEGREE_AND_JUMP.toString(), StlDecomposition.LowpassSettingsMethod.ALL.toString()}));
      types.add(type);
      return types;
   }

   private StlDecomposition handleSettings(boolean robust, int seasonality, StlDecomposition.SeasonalSettingsMethod seasonalSettingsMethod, StlDecomposition.TrendSettingsMethod trendSettingsMethod, StlDecomposition.LowpassSettingsMethod lowpassSettingsMethod) throws UndefinedParameterError {
      StlDecomposition decomposition = null;
      if (seasonalSettingsMethod == StlDecomposition.SeasonalSettingsMethod.PERIODIC) {
         if (robust) {
            decomposition = StlDecomposition.createRobustPeriodic(seasonality);
         } else {
            decomposition = StlDecomposition.createNonRobustPeriodic(seasonality, this.getParameterAsInt("inner_iterations"), this.getParameterAsInt("robust_iterations"));
         }
      } else if (robust) {
         decomposition = StlDecomposition.createRobustNotPeriodic(seasonality, this.getParameterAsInt("seasonal_width"));
      } else {
         decomposition = StlDecomposition.createNonRobustNotPeriodic(seasonality, this.getParameterAsInt("seasonal_width"), this.getParameterAsInt("inner_iterations"), this.getParameterAsInt("robust_iterations"));
      }

      Integer width = null;
      Integer degree = null;
      Integer jump = null;
      if (seasonalSettingsMethod != StlDecomposition.SeasonalSettingsMethod.DEFAULT && seasonalSettingsMethod != StlDecomposition.SeasonalSettingsMethod.PERIODIC) {
         width = this.getParameterAsInt("seasonal_width");
         degree = this.getParameterAsInt("seasonal_degree");
         jump = this.getParameterAsInt("seasonal_jump");
         if (seasonalSettingsMethod == StlDecomposition.SeasonalSettingsMethod.WIDTH_AND_DEGREE) {
            jump = null;
         } else if (seasonalSettingsMethod == StlDecomposition.SeasonalSettingsMethod.WIDTH_AND_JUMP) {
            degree = null;
         }

         decomposition.setAllSeasonalSettings(width, degree, jump);
      }

      if (trendSettingsMethod == StlDecomposition.TrendSettingsMethod.FLAT) {
         decomposition.setFlatTrend();
      } else if (trendSettingsMethod == StlDecomposition.TrendSettingsMethod.LINEAR) {
         decomposition.setLinearTrend();
      } else if (trendSettingsMethod != StlDecomposition.TrendSettingsMethod.DEFAULT) {
         width = this.getParameterAsInt("trend_width");
         degree = this.getParameterAsInt("trend_degree");
         jump = this.getParameterAsInt("trend_jump");
         switch(trendSettingsMethod) {
         case WIDTH:
            degree = null;
            jump = null;
            break;
         case DEGREE:
            width = null;
            jump = null;
            break;
         case JUMP:
            width = null;
            degree = null;
            break;
         case WIDTH_AND_DEGREE:
            jump = null;
            break;
         case WIDTH_AND_JUMP:
            degree = null;
            break;
         case DEGREE_AND_JUMP:
            width = null;
         }

         decomposition.setAllTrendSettings(width, degree, jump);
      }

      if (lowpassSettingsMethod != StlDecomposition.LowpassSettingsMethod.DEFAULT) {
         width = this.getParameterAsInt("lowpass_width");
         degree = this.getParameterAsInt("lowpass_degree");
         jump = this.getParameterAsInt("lowpass_jump");
         switch(lowpassSettingsMethod) {
         case WIDTH:
            degree = null;
            jump = null;
            break;
         case DEGREE:
            width = null;
            jump = null;
            break;
         case JUMP:
            width = null;
            degree = null;
            break;
         case WIDTH_AND_DEGREE:
            jump = null;
            break;
         case WIDTH_AND_JUMP:
            degree = null;
            break;
         case DEGREE_AND_JUMP:
            width = null;
         }

         decomposition.setAllLowpassSettings(width, degree, jump);
      }

      return decomposition;
   }
}
