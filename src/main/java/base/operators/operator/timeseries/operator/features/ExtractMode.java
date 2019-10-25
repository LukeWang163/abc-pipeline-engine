package base.operators.operator.timeseries.operator.features;

import base.operators.example.table.NominalMapping;
import base.operators.example.table.PolynominalMapping;
import base.operators.operator.timeseries.operator.helper.WrongConfiguredHelperException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.UserError;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.NominalValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.feature.Mode;
import base.operators.tools.RandomGenerator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

public class ExtractMode extends AbstractFeaturesOperator {
   public static final String PARAMETER_MAX_MODE_ORDER = "max_mode_order";
   public static final String PARAMETER_MULTI_MODAL_MODE = "multi_modal_mode";
   public static final String PARAMETER_MAX_K = "max_k";
   private Mode realMode = null;
   private Mode nominalMode = null;
   private Mode timeMode = null;

   public ExtractMode(OperatorDescription description) throws WrongConfiguredHelperException {
      super(description);
   }

   protected void initFeatureCalculation(ISeries inputSeries) throws UserError {
      int maxOrder = this.getParameterAsInt("max_mode_order");
      int maxK = this.getParameterAsInt("max_k");
      boolean skipInvalidValues = this.getParameterAsBoolean("ignore_invalid_values");
      Mode.MultiModalMode modalMode = Mode.MultiModalMode.valueOf(this.getParameterAsString("multi_modal_mode").toUpperCase().replace(" ", "_"));
      Mode.ModeBuilder realModeBuilder = (new Mode.ModeBuilder()).maxK(maxK).maxModeOrder(maxOrder).multiModalMode(modalMode);
      Mode.ModeBuilder nominalModeBuilder = (new Mode.ModeBuilder()).maxK(maxK).maxModeOrder(maxOrder).multiModalMode(modalMode);
      Mode.ModeBuilder timeModeBuilder = (new Mode.ModeBuilder()).maxK(maxK).maxModeOrder(maxOrder).multiModalMode(modalMode);
      if (modalMode == Mode.MultiModalMode.RANDOM_K) {
         Random random = RandomGenerator.getRandomGenerator(this.getParameterAsBoolean("use_local_random_seed"), this.getParameterAsInt("local_random_seed"));
         realModeBuilder = realModeBuilder.random(random);
         nominalModeBuilder = nominalModeBuilder.random(random);
         timeModeBuilder = timeModeBuilder.random(random);
      }

      if (skipInvalidValues) {
         realModeBuilder = realModeBuilder.skipInvalidValues();
         nominalModeBuilder = nominalModeBuilder.skipInvalidValues();
         timeModeBuilder = timeModeBuilder.skipInvalidValues();
      }

      this.realMode = (Mode)realModeBuilder.build();
      this.nominalMode = (Mode)nominalModeBuilder.build();
      this.timeMode = (Mode)timeModeBuilder.build();
   }

   private List getFeaturesReal(IndexDimension indexDimension, SeriesValues seriesValues) {
      List result = new ArrayList();
      List featureList = this.realMode.getComputedFeatures(indexDimension, seriesValues);
      Iterator var5 = featureList.iterator();

      while(var5.hasNext()) {
         Pair valuePair = (Pair)var5.next();
         FeatureContainer feature = (new FeatureContainer()).setName((String)valuePair.getFirst()).setValueType(4).setSeriesValuesType(SeriesBuilder.ValuesType.REAL).setValue(valuePair.getSecond()).setDoubleValue(valuePair.getSecond() == null ? Double.NaN : (Double)valuePair.getSecond());
         result.add(new Pair(valuePair.getFirst(), feature));
      }

      return result;
   }

   private List getFeaturesNominal(IndexDimension indexDimension, SeriesValues seriesValues) {
      NominalValues nominalValues = (NominalValues)seriesValues;
      List result = new ArrayList();
      List featureList = this.nominalMode.getComputedFeatures(indexDimension, nominalValues);
      NominalMapping mapping = new PolynominalMapping(nominalValues.getIndexToNominalMapAsHashMap());
      Iterator var7 = featureList.iterator();

      while(var7.hasNext()) {
         Pair valuePair = (Pair)var7.next();
         FeatureContainer feature = (new FeatureContainer()).setName((String)valuePair.getFirst()).setValueType(1).setSeriesValuesType(SeriesBuilder.ValuesType.NOMINAL).setValue(valuePair.getSecond()).setNominalMapping(mapping).setDoubleValue(valuePair.getSecond() != null ? (double)mapping.getIndex((String)valuePair.getSecond()) : Double.NaN);
         result.add(new Pair(valuePair.getFirst(), feature));
      }

      return result;
   }

   private List getFeaturesTime(IndexDimension indexDimension, SeriesValues seriesValues) {
      List featureList = this.timeMode.getComputedFeatures(indexDimension, seriesValues);
      List result = new ArrayList();
      Iterator var5 = featureList.iterator();

      while(var5.hasNext()) {
         Pair valuePair = (Pair)var5.next();
         FeatureContainer feature = (new FeatureContainer()).setName((String)valuePair.getFirst()).setValueType(9).setSeriesValuesType(SeriesBuilder.ValuesType.TIME).setValue(valuePair.getSecond()).setDoubleValue(valuePair.getSecond() != null ? (double)((Instant)valuePair.getSecond()).toEpochMilli() : Double.NaN);
         result.add(new Pair(valuePair.getFirst(), feature));
      }

      return result;
   }

   protected List getFeaturesDefaultReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeaturesReal(indexDimension, seriesValues);
   }

   protected List getFeaturesRealReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeaturesReal(indexDimension, seriesValues);
   }

   protected List getFeaturesTimeReal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeaturesReal(indexDimension, seriesValues);
   }

   protected List getFeaturesDefaultNominal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeaturesNominal(indexDimension, seriesValues);
   }

   protected List getFeaturesRealNominal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeaturesNominal(indexDimension, seriesValues);
   }

   protected List getFeaturesTimeNominal(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeaturesNominal(indexDimension, seriesValues);
   }

   protected List getFeaturesRealTime(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeaturesTime(indexDimension, seriesValues);
   }

   protected List getFeaturesDefaultTime(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeaturesTime(indexDimension, seriesValues);
   }

   protected List getFeaturesTimeTime(IndexDimension indexDimension, SeriesValues seriesValues) throws UserError {
      return this.getFeaturesTime(indexDimension, seriesValues);
   }

   protected List parameterTypesBefore() {
      List types = new ArrayList();
      types.add(new ParameterTypeInt("max_mode_order", "max order mode", 1, Integer.MAX_VALUE, 1, false));
      types.add(new ParameterTypeCategory("multi_modal_mode", "multi modal mode", (String[])Arrays.stream(Mode.MultiModalMode.class.getEnumConstants()).map(Enum::toString).toArray((x$0) -> {
         return new String[x$0];
      }), 0, false));
      ParameterType type = new ParameterTypeInt("max_k", "max k", 1, Integer.MAX_VALUE, 1, false);
      type.registerDependencyCondition(new EqualStringCondition(this, "multi_modal_mode", false, new String[]{Mode.MultiModalMode.FIRST_K_OCCURENCE.toString(), Mode.MultiModalMode.RANDOM_K.toString()}));
      types.add(type);
      return types;
   }

   protected List parameterTypesAfter() {
      List types = new ArrayList();
      List randomParameterTypes = RandomGenerator.getRandomGeneratorParameters(this);
      Iterator var3 = randomParameterTypes.iterator();

      while(var3.hasNext()) {
         ParameterType type = (ParameterType)var3.next();
         type.registerDependencyCondition(new EqualStringCondition(this, "multi_modal_mode", false, new String[]{Mode.MultiModalMode.RANDOM_K.toString()}));
      }

      types.addAll(randomParameterTypes);
      return types;
   }

   protected String[] getFeatureNames() throws UndefinedParameterError {
      boolean skipInvalidValues = this.getParameterAsBoolean("ignore_invalid_values");
      Mode.MultiModalMode modalMode = Mode.MultiModalMode.valueOf(this.getParameterAsString("multi_modal_mode").toUpperCase().replace(" ", "_"));
      Mode.ModeBuilder builder = (new Mode.ModeBuilder()).maxK(this.getParameterAsInt("max_k")).maxModeOrder(this.getParameterAsInt("max_mode_order")).multiModalMode(modalMode);
      if (skipInvalidValues) {
         builder = builder.skipInvalidValues();
      }

      return builder.build().getFeatureNames();
   }

   protected int getDefaultValueType() {
      return 2;
   }

   protected SeriesBuilder.ValuesType getValuesType() {
      return SeriesBuilder.ValuesType.MIXED;
   }

   protected boolean sortAttributes() {
      return true;
   }
}
