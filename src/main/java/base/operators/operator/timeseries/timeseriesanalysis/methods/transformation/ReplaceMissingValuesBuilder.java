package base.operators.operator.timeseries.timeseriesanalysis.methods.transformation;

import java.time.Instant;

public class ReplaceMissingValuesBuilder {
   private ReplaceMissingValues missingValues = null;

   public ReplaceMissingValuesBuilder() {
      this.reset();
   }

   public ReplaceMissingValuesBuilder reset() {
      this.missingValues = new ReplaceMissingValues();
      return this;
   }

   public ReplaceMissingValuesBuilder allReplaceTypes(ReplaceMissingValues.ReplaceType type) {
      return this.replaceType(type).nominalReplaceType(type).timeReplaceType(type);
   }

   public ReplaceMissingValuesBuilder replaceType(ReplaceMissingValues.ReplaceType type) {
      this.missingValues.setReplaceType(type);
      return this;
   }

   public ReplaceMissingValuesBuilder nominalReplaceType(ReplaceMissingValues.ReplaceType type) {
      this.missingValues.setNominalReplaceType(type);
      return this;
   }

   public ReplaceMissingValuesBuilder timeReplaceType(ReplaceMissingValues.ReplaceType type) {
      this.missingValues.setTimeReplaceType(type);
      return this;
   }

   public ReplaceMissingValuesBuilder skipOtherMissings() {
      this.missingValues.setSkipOtherMissings(true);
      return this;
   }

   public ReplaceMissingValuesBuilder replaceInfinity() {
      this.missingValues.setReplaceInfinity(true);
      return this;
   }

   public ReplaceMissingValuesBuilder replaceEmptyStrings() {
      this.missingValues.setReplaceEmptyStrings(true);
      return this;
   }

   public ReplaceMissingValuesBuilder ensureFiniteValues() {
      this.missingValues.setEnsureFiniteValues(true);
      return this;
   }

   public ReplaceMissingValuesBuilder replaceValue(double value) {
      this.missingValues.setReplaceValue(value);
      return this;
   }

   public ReplaceMissingValuesBuilder replaceValue(String value) {
      this.missingValues.setNominalReplaceValue(value);
      return this;
   }

   public ReplaceMissingValuesBuilder replaceValue(Instant value) {
      this.missingValues.setTimeReplaceValue(value);
      return this;
   }

   public ReplaceMissingValues build() {
      return this.missingValues;
   }
}
