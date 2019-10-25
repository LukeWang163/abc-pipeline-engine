package base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception;

public class NameNotInMultivariateSeriesException extends IllegalArgumentException {
   private static final long serialVersionUID = -1076716377010279391L;
   private final String seriesName;

   public NameNotInMultivariateSeriesException(String seriesName) {
      super(createMessage(seriesName));
      this.seriesName = seriesName;
   }

   public NameNotInMultivariateSeriesException(String seriesName, Throwable cause) {
      super(createMessage(seriesName), cause);
      this.seriesName = seriesName;
   }

   public String getSeriesName() {
      return this.seriesName;
   }

   private static String createMessage(String seriesName) {
      return "Name of provided series (" + seriesName + ") does not exist in MultivariateSeries.";
   }
}
