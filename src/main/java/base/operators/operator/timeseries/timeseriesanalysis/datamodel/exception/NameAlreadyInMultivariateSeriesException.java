package base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception;

public class NameAlreadyInMultivariateSeriesException extends IllegalArgumentException {
   private static final long serialVersionUID = -4189614335857183609L;
   private final String seriesName;

   public NameAlreadyInMultivariateSeriesException(String seriesName) {
      super(createMessage(seriesName));
      this.seriesName = seriesName;
   }

   public NameAlreadyInMultivariateSeriesException(String seriesName, Throwable cause) {
      super(createMessage(seriesName), cause);
      this.seriesName = seriesName;
   }

   public String getSeriesName() {
      return this.seriesName;
   }

   private static String createMessage(String seriesName) {
      return "Name of provided series (" + seriesName + ") already exists in the series.";
   }
}
