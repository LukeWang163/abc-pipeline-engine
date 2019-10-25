package base.operators.operator.timeseries.operator.transformation;

import base.operators.operator.OperatorDescription;
import base.operators.operator.UserError;
import base.operators.operator.timeseries.operator.helper.ExampleSetTimeSeriesHelper;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.parameter.*;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.ReplaceMissingValues;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.ReplaceMissingValuesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.methods.transformation.SeriesTransformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReplaceMissingValuesOperator extends AbstractTransformationOperator {
   public static final String PARAMETER_REPLACE_TYPE_NUMERICAL = "replace_type_numerical";
   public static final String PARAMETER_REPLACE_TYPE_NOMINAL = "replace_type_nominal";
   public static final String PARAMETER_REPLACE_TYPE_DATE_TIME = "replace_type_date_time";
   public static final String PARAMETER_REPLACE_VALUE_NUMERICAL = "replace_value_numerical";
   public static final String PARAMETER_REPLACE_VALUE_NOMINAL = "replace_value_nominal";
   public static final String PARAMETER_REPLACE_VALUE_DATE_TIME = "replace_value_date_time";
   public static final String PARAMETER_SKIP_OTHER_MISSINGS = "skip_other_missings";
   public static final String PARAMETER_REPLACE_INFINITY = "replace_infinity";
   public static final String PARAMETER_REPLACE_EMPTY_STRINGS = "replace_empty_strings";
   public static final String PARAMETER_ENSURE_FINITE_VALUES = "ensure_finite_values";

   public ReplaceMissingValuesOperator(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
   }

   protected SeriesTransformation createTransformation() throws UserError {
      ReplaceMissingValues.ReplaceType numericalReplaceType = ReplaceMissingValues.ReplaceType.valueOf(this.getParameterAsString("replace_type_numerical").replace(" ", "_").toUpperCase());
      ReplaceMissingValues.ReplaceType nominalReplaceType = ReplaceMissingValues.ReplaceType.valueOf(this.getParameterAsString("replace_type_nominal").replace(" ", "_").toUpperCase());
      ReplaceMissingValues.ReplaceType dateTimeReplaceType = ReplaceMissingValues.ReplaceType.valueOf(this.getParameterAsString("replace_type_date_time").replace(" ", "_").toUpperCase());
      ReplaceMissingValuesBuilder builder = (new ReplaceMissingValuesBuilder()).replaceType(numericalReplaceType).nominalReplaceType(nominalReplaceType).timeReplaceType(dateTimeReplaceType);
      if (this.getParameterAsBoolean("skip_other_missings")) {
         builder.skipOtherMissings();
      }

      if (this.getParameterAsBoolean("replace_infinity")) {
         builder.replaceInfinity();
      }

      if (this.getParameterAsBoolean("replace_empty_strings")) {
         builder.replaceEmptyStrings();
      }

      if (this.getParameterAsBoolean("ensure_finite_values")) {
         builder.ensureFiniteValues();
      }

      if (numericalReplaceType == ReplaceMissingValues.ReplaceType.VALUE) {
         builder.replaceValue(this.getParameterAsDouble("replace_value_numerical"));
      }

      if (nominalReplaceType == ReplaceMissingValues.ReplaceType.VALUE) {
         builder.replaceValue(this.getParameterAsString("replace_value_nominal"));
      }

      if (dateTimeReplaceType == ReplaceMissingValues.ReplaceType.VALUE) {
         builder.replaceValue(ParameterTypeDate.getParameterAsDate("replace_value_date_time", this).toInstant());
      }

      return builder.build();
   }

   protected List getAdditionalParameterTypes() {
      List types = new ArrayList();
      types.add(new ParameterTypeCategory("replace_type_numerical", "The kind of replacement which is used to replace missing values of numerical series.", (String[])Arrays.stream(ReplaceMissingValues.ReplaceType.class.getEnumConstants()).map(Enum::toString).toArray((x$0) -> {
         return new String[x$0];
      }), 0, false));
      types.add(new ParameterTypeCategory("replace_type_nominal", "The kind of replacement which is used to replace missing values of nominal series.", (String[])Arrays.stream(ReplaceMissingValues.ReplaceType.allowedNominalTypes()).map(Enum::toString).toArray((x$0) -> {
         return new String[x$0];
      }), 0, false));
      types.add(new ParameterTypeCategory("replace_type_date_time", "The kind of replacement which is used to replace missing values of date time series.", (String[])Arrays.stream(ReplaceMissingValues.ReplaceType.class.getEnumConstants()).map(Enum::toString).toArray((x$0) -> {
         return new String[x$0];
      }), 0, false));
      ParameterType type = new ParameterTypeDouble("replace_value_numerical", "The replacement value for all missing values for numerical series.", -1.7976931348623157E308D, Double.MAX_VALUE, 0.0D, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "replace_type_numerical", false, new String[]{ReplaceMissingValues.ReplaceType.VALUE.toString()}));
      types.add(type);
       type = new ParameterTypeString("replace_value_nominal", "The replacement value for all missing values for nominal series.", "unknown", false);
      type.registerDependencyCondition(new EqualStringCondition(this, "replace_type_nominal", false, new String[]{ReplaceMissingValues.ReplaceType.VALUE.toString()}));
      types.add(type);
       type = new ParameterTypeDate("replace_value_date_time", "The replacement value for all missing values for date time series.", true, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "replace_type_date_time", true, new String[]{ReplaceMissingValues.ReplaceType.VALUE.toString()}));
      types.add(type);
       type = new ParameterTypeBoolean("skip_other_missings", "If this parameter is set to true, other neighboring values which are also missing are not considered for the determination of the replacement value.", true, false);
      type.registerDependencyCondition(new BooleanParameterCondition(this, "ensure_finite_values", false, false));
      types.add(type);
      type = new ParameterTypeBoolean("replace_infinity", "If this parameter is set to true, also positive and negative infinity values are replaced in numerical series.", true, false);
      type.registerDependencyCondition(new BooleanParameterCondition(this, "ensure_finite_values", false, false));
      types.add(type);
      type = new ParameterTypeBoolean("replace_empty_strings", "If this parameter is set to true, also empty string values are replaced in nominal series.", true, false);
      type.registerDependencyCondition(new BooleanParameterCondition(this, "ensure_finite_values", false, false));
      types.add(type);
      types.add(new ParameterTypeBoolean("ensure_finite_values", "If this parameter is set to true, the operator ensures that no infinite values (missing, positive/negative infinity, empty strings) remain in the series after the replacement.", false, false));
      return types;
   }

   protected String getDefaultPostfix() {
      return "_cleaned";
   }

   protected ExampleSetTimeSeriesHelper.IndiceHandling getIndicesHandling() {
      return ExampleSetTimeSeriesHelper.IndiceHandling.OPTIONAL_INDICES;
   }
}
