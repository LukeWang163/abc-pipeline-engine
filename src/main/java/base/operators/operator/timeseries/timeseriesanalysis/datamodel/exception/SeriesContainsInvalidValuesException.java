package base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception;

public class SeriesContainsInvalidValuesException extends RuntimeException {
   private static final long serialVersionUID = 5152186496858686013L;
   private final String seriesName;
   private final InvalidValuesType type;

   public SeriesContainsInvalidValuesException(String seriesName, InvalidValuesType type) {
      super(createMessage(seriesName, type));
      this.seriesName = seriesName;
      this.type = type;
   }

   public SeriesContainsInvalidValuesException(String seriesName, InvalidValuesType type, Throwable cause) {
      super(createMessage(seriesName, type), cause);
      this.seriesName = seriesName;
      this.type = type;
   }

   public String getSeriesName() {
      return this.seriesName;
   }

   public InvalidValuesType getType() {
      return this.type;
   }

   private static String createMessage(String seriesName, InvalidValuesType type) {
      return "Series " + seriesName + " contains " + type + " values.";
   }

   public static enum InvalidValuesType {
      NAN("NaN"),
      INFINITE("infinite"),
      POS_INFINITE("positive infinite"),
      NEG_INFINITE("negative infinite"),
      ONLY_NON_FINITE("only non-finite");

      private final String text;

      private InvalidValuesType(String text) {
         this.text = text;
      }

      public String toString() {
         return this.text;
      }
   }
}
