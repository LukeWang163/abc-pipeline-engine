package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.SeriesContainsInvalidValuesException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.DefaultInteger;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.RealValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.TimeValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilders;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.interfaces.ISeries;
import base.operators.operator.timeseries.timeseriesanalysis.exception.ArgumentIsNullException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.Pair;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReplaceMissingValues implements SeriesTransformation {
   private ReplaceType replaceType;
   private ReplaceType nominalReplaceType;
   private ReplaceType timeReplaceType;
   private boolean skipOtherMissings;
   private boolean replaceInfinity;
   private boolean replaceEmptyStrings;
   private boolean ensureFiniteValues;
   private Double replaceValue;
   private String nominalReplaceValue;
   private Instant timeReplaceValue;

   protected ReplaceMissingValues() {
      this.replaceType = ReplaceType.NEXT_VALUE;
      this.nominalReplaceType = ReplaceType.NEXT_VALUE;
      this.timeReplaceType = ReplaceType.NEXT_VALUE;
      this.skipOtherMissings = false;
      this.replaceInfinity = false;
      this.replaceEmptyStrings = false;
      this.ensureFiniteValues = false;
      this.replaceValue = 0.0D;
      this.nominalReplaceValue = "";
      this.timeReplaceValue = Instant.EPOCH;
   }

   public ISeries compute(ISeries series) {
      if (series == null) {
         throw new ArgumentIsNullException("series");
      } else {
         IndexDimension indexDimension = series.getIndexDimension();
         SeriesBuilder builder = SeriesBuilders.fromSeries(series);
         builder.indexDimension(series.getIndexDimension());

         SeriesValues seriesValues;
         for(Iterator var4 = series.getSeriesValuesList().iterator(); var4.hasNext(); builder = this.computeReplacements(builder, indexDimension, seriesValues)) {
            seriesValues = (SeriesValues)var4.next();
         }

         return builder.build();
      }
   }

   private SeriesBuilder computeReplacements(SeriesBuilder builder, IndexDimension indexDimension, SeriesValues seriesValues) {
      List values = seriesValues.getValues();
      List replacedValues = new ArrayList(values);
      ReplaceType currentType = this.replaceType;
      if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.NOMINAL) {
         currentType = this.nominalReplaceType;
      } else if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.TIME) {
         currentType = this.timeReplaceType;
      }

      for(int i = 0; i < values.size(); ++i) {
         if (this.replace(values.get(i))) {
            Object replacedValue = null;
            switch(currentType) {
            case NEXT_VALUE:
               replacedValue = this.nextValue(i, values, this.ensureFiniteValues).getSecond();
               break;
            case PREVIOUS_VALUE:
               replacedValue = this.previousValue(i, values, this.ensureFiniteValues).getSecond();
               break;
            case AVERAGE:
               replacedValue = this.getAverageReplacementValue(i, values, seriesValues.getValuesType());
               break;
            case VALUE:
               replacedValue = this.replaceValue(seriesValues.getValuesType());
               break;
            case LINEAR_INTERPOLATION:
               replacedValue = this.getLinearInterpolationValue(i, indexDimension, values, seriesValues.getValuesType());
            }

            if (this.ensureFiniteValues && this.replace(replacedValue)) {
               throw new SeriesContainsInvalidValuesException(seriesValues.getName(), SeriesContainsInvalidValuesException.InvalidValuesType.ONLY_NON_FINITE);
            }

            replacedValues.set(i, replacedValue);
         }
      }

      if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.REAL) {
         return builder.addRealValues(replacedValues, seriesValues.getName());
      } else if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.TIME) {
         return builder.addTimeValues(replacedValues, seriesValues.getName());
      } else if (seriesValues.getValuesType() == SeriesBuilder.ValuesType.NOMINAL) {
         return builder.addNominalValues((List)replacedValues, (String)seriesValues.getName());
      } else {
         return builder;
      }
   }

   private boolean replace(Object value) {
      if (value == null) {
         return true;
      } else if (value instanceof Double) {
         if (((Double)value).isNaN()) {
            return true;
         } else {
            return this.replaceInfinity && ((Double)value).isInfinite();
         }
      } else if (!(value instanceof String)) {
         return false;
      } else {
         return this.replaceEmptyStrings && ((String)value).isEmpty();
      }
   }

   private Pair nextValue(int currentIndex, List values, boolean changeDirection) {
      int nextIndex = currentIndex + 1;
      if (nextIndex < values.size()) {
         if (!this.replace(values.get(nextIndex))) {
            return new Pair(nextIndex, values.get(nextIndex));
         }

         if (this.skipOtherMissings) {
            return this.nextValue(nextIndex, values, changeDirection);
         }
      } else if (changeDirection) {
         return this.previousValue(currentIndex, values, false);
      }

      return new Pair(currentIndex, values.get(currentIndex));
   }

   private Pair previousValue(int currentIndex, List values, boolean changeDirection) {
      int previousIndex = currentIndex - 1;
      if (previousIndex >= 0) {
         if (!this.replace(values.get(previousIndex))) {
            return new Pair(previousIndex, values.get(previousIndex));
         }

         if (this.skipOtherMissings) {
            return this.previousValue(previousIndex, values, changeDirection);
         }
      } else if (changeDirection) {
         return this.nextValue(currentIndex, values, false);
      }

      return new Pair(currentIndex, values.get(currentIndex));
   }

   private Object getAverageReplacementValue(int currentIndex, List values, SeriesBuilder.ValuesType type) {
      if (type == SeriesBuilder.ValuesType.NOMINAL) {
         throw new IllegalArgumentException("Average replacement for nominal values is not supported");
      } else {
         Object previousValue = this.previousValue(currentIndex, values, false).getSecond();
         Object nextValue = this.nextValue(currentIndex, values, false).getSecond();
         if (this.replace(previousValue) && this.replace(nextValue)) {
            return values.get(currentIndex);
         } else {
            if (this.ensureFiniteValues) {
               if (this.replace(nextValue)) {
                  return previousValue;
               }

               if (this.replace(previousValue)) {
                  return nextValue;
               }
            } else if (this.replace(previousValue) || this.replace(nextValue)) {
               return values.get(currentIndex);
            }

            Object result = null;
            if (type == SeriesBuilder.ValuesType.REAL) {
               result = ((Double)nextValue + (Double)previousValue) / 2.0D;
            } else if (type == SeriesBuilder.ValuesType.TIME) {
               result = ((Instant)previousValue).plus(Duration.between((Instant)previousValue, (Instant)nextValue).dividedBy(2L));
            }

            return result;
         }
      }
   }

   private Object replaceValue(SeriesBuilder.ValuesType valuesType) {
      if (valuesType == SeriesBuilder.ValuesType.REAL) {
         return this.replaceValue;
      } else if (valuesType == SeriesBuilder.ValuesType.NOMINAL) {
         return this.nominalReplaceValue;
      } else {
         return valuesType == SeriesBuilder.ValuesType.TIME ? this.timeReplaceValue : null;
      }
   }

   private Object getLinearInterpolationValue(int currentIndex, IndexDimension indexDimension, List values, SeriesBuilder.ValuesType type) {
      if (indexDimension.getIndexType() == SeriesBuilder.IndexType.NOMINAL) {
         throw new IllegalArgumentException("Linear interpolation for a nominal index dimension is not supported");
      } else if (type == SeriesBuilder.ValuesType.NOMINAL) {
         throw new IllegalArgumentException("Linear interpolation replacement for nominal values is not supported");
      } else {
         Pair previousValuePair = this.previousValue(currentIndex, values, false);
         int previousArrayIndex = (Integer)previousValuePair.getFirst();
         Object previousValue = previousValuePair.getSecond();
         Pair nextValuePair = this.nextValue(currentIndex, values, false);
         int nextArrayIndex = (Integer)nextValuePair.getFirst();
         Object nextValue = nextValuePair.getSecond();
         if (this.replace(previousValue) && this.replace(nextValue)) {
            return values.get(currentIndex);
         } else {
            if (this.ensureFiniteValues) {
               if (this.replace(nextValue)) {
                  return previousValue;
               }

               if (this.replace(previousValue)) {
                  return nextValue;
               }
            } else if (this.replace(previousValue) || this.replace(nextValue)) {
               return values.get(currentIndex);
            }

            double slopeDivisor = 0.0D;
            double relativeIndex = 0.0D;
            Object result = null;
            double previousIndexValue;
            double currentIndexValue;
            double nextIndexValue;
            if (indexDimension.getIndexType() == SeriesBuilder.IndexType.REAL) {
               RealValues realIndexDimension = (RealValues)indexDimension;
               previousIndexValue = (Double)realIndexDimension.getIndexValue(previousArrayIndex);
               currentIndexValue = (Double)realIndexDimension.getIndexValue(currentIndex);
               nextIndexValue = (Double)realIndexDimension.getIndexValue(nextArrayIndex);
               slopeDivisor = nextIndexValue - previousIndexValue;
               relativeIndex = currentIndexValue - previousIndexValue;
            } else if (indexDimension.getIndexType() == SeriesBuilder.IndexType.DEFAULT) {
               DefaultInteger defaultIndexDimension = (DefaultInteger)indexDimension;
               previousIndexValue = (double)(Integer)defaultIndexDimension.getIndexValue(previousArrayIndex);
               currentIndexValue = (double)(Integer)defaultIndexDimension.getIndexValue(currentIndex);
               nextIndexValue = (double)(Integer)defaultIndexDimension.getIndexValue(nextArrayIndex);
               slopeDivisor = nextIndexValue - previousIndexValue;
               relativeIndex = currentIndexValue - previousIndexValue;
            } else if (indexDimension.getIndexType() == SeriesBuilder.IndexType.TIME) {
               TimeValues timeIndexDimension = (TimeValues)indexDimension;
               previousIndexValue = (double)((Instant)timeIndexDimension.getIndexValue(previousArrayIndex)).toEpochMilli();
               currentIndexValue = (double)((Instant)timeIndexDimension.getIndexValue(currentIndex)).toEpochMilli();
               nextIndexValue = (double)((Instant)timeIndexDimension.getIndexValue(nextArrayIndex)).toEpochMilli();
               slopeDivisor = nextIndexValue - previousIndexValue;
               relativeIndex = currentIndexValue - previousIndexValue;
            }

            if (type == SeriesBuilder.ValuesType.REAL) {
               Double prevV = (Double)previousValue;
               Double nextV = (Double)nextValue;
               Double resultDouble;
               if (slopeDivisor == 0.0D) {
                  resultDouble = nextV - prevV < 0.0D ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
               } else {
                  resultDouble = prevV + relativeIndex * (nextV - prevV) / slopeDivisor;
               }

               result = resultDouble;
            }

            if (type == SeriesBuilder.ValuesType.TIME) {
               Instant prevV = (Instant)previousValue;
               Instant nextV = (Instant)nextValue;
               result = prevV.plus(Duration.between(prevV, nextV).multipliedBy((long)relativeIndex).dividedBy((long)slopeDivisor));
            }

            return result;
         }
      }
   }

   public ReplaceType getReplaceType() {
      return this.replaceType;
   }

   public void setReplaceType(ReplaceType replaceType) {
      this.replaceType = replaceType;
   }

   public ReplaceType getNominalReplaceType() {
      return this.nominalReplaceType;
   }

   public void setNominalReplaceType(ReplaceType nominalReplaceType) {
      if (!ArrayUtils.contains(ReplaceType.allowedNominalTypes(), nominalReplaceType)) {
         throw new IllegalArgumentException("Replace type " + nominalReplaceType.toString() + " is not allowed for nominal values.");
      } else {
         this.nominalReplaceType = nominalReplaceType;
      }
   }

   public ReplaceType getTimeReplaceType() {
      return this.timeReplaceType;
   }

   public void setTimeReplaceType(ReplaceType timeReplaceType) {
      this.timeReplaceType = timeReplaceType;
   }

   public Double getReplaceValue() {
      return this.replaceValue;
   }

   public void setReplaceValue(Double replaceValue) {
      this.replaceValue = replaceValue;
   }

   public String getNominalReplaceValue() {
      return this.nominalReplaceValue;
   }

   public void setNominalReplaceValue(String nominalReplaceValue) {
      this.nominalReplaceValue = nominalReplaceValue;
   }

   public Instant getTimeReplaceValue() {
      return this.timeReplaceValue;
   }

   public void setTimeReplaceValue(Instant timeReplaceValue) {
      this.timeReplaceValue = timeReplaceValue;
   }

   public boolean isSkipOtherMissings() {
      return this.skipOtherMissings;
   }

   public void setSkipOtherMissings(boolean skipOtherMissings) {
      if (this.ensureFiniteValues && !skipOtherMissings) {
         throw new IllegalArgumentException("Cannot set skipOtherMissings to false, when ensureFiniteValues is true.");
      } else {
         this.skipOtherMissings = skipOtherMissings;
      }
   }

   public boolean isReplaceInfinity() {
      return this.replaceInfinity;
   }

   public void setReplaceInfinity(boolean replaceInfinity) {
      if (this.ensureFiniteValues && !replaceInfinity) {
         throw new IllegalArgumentException("Cannot set replaceInfinity to false, when ensureFiniteValues is true.");
      } else {
         this.replaceInfinity = replaceInfinity;
      }
   }

   public boolean isReplaceEmptyStrings() {
      return this.replaceEmptyStrings;
   }

   public void setReplaceEmptyStrings(boolean replaceEmptyStrings) {
      if (this.ensureFiniteValues && !replaceEmptyStrings) {
         throw new IllegalArgumentException("Cannot set replaceEmptyStrings to false, when ensureFiniteValues is true.");
      } else {
         this.replaceEmptyStrings = replaceEmptyStrings;
      }
   }

   public boolean isEnsureFiniteValues() {
      return this.ensureFiniteValues;
   }

   public void setEnsureFiniteValues(boolean ensureFiniteValues) {
      if (ensureFiniteValues) {
         this.skipOtherMissings = true;
         this.replaceInfinity = true;
         this.replaceEmptyStrings = true;
      }

      this.ensureFiniteValues = ensureFiniteValues;
   }

   public static enum ReplaceType {
      PREVIOUS_VALUE,
      NEXT_VALUE,
      AVERAGE,
      LINEAR_INTERPOLATION,
      VALUE;

      public static ReplaceType[] allowedNominalTypes() {
         return new ReplaceType[]{PREVIOUS_VALUE, NEXT_VALUE, VALUE};
      }

      public String toString() {
         return super.name().toLowerCase().replace("_", " ");
      }
   }
}
