package base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception;

public class IndicesNotEqualException extends IllegalArgumentException {
   private static final long serialVersionUID = 8463065390569028391L;
   private final String seriesName1;
   private final String seriesName2;

   public IndicesNotEqualException(String seriesName1) {
      super(createMessage(seriesName1, "each other"));
      this.seriesName1 = seriesName1;
      this.seriesName2 = null;
   }

   public IndicesNotEqualException(String seriesName1, Throwable cause) {
      super(createMessage(seriesName1, "each other"), cause);
      this.seriesName1 = seriesName1;
      this.seriesName2 = null;
   }

   public IndicesNotEqualException(String seriesName1, String seriesName2) {
      super(createMessage(seriesName1, seriesName2));
      this.seriesName1 = seriesName1;
      this.seriesName2 = seriesName2;
   }

   public IndicesNotEqualException(String seriesName1, String seriesName2, Throwable cause) {
      super(createMessage(seriesName1, seriesName2), cause);
      this.seriesName1 = seriesName1;
      this.seriesName2 = seriesName2;
   }

   public String getSeriesName1() {
      return this.seriesName1;
   }

   public String getSeriesName2() {
      return this.seriesName2;
   }

   private static String createMessage(String seriesName1, String seriesName2) {
      return "Index values of provided " + seriesName1 + " differ to " + seriesName2 + '.';
   }
}
