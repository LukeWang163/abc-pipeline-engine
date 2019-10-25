package base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception;

public class IllegalSeriesLengthException extends IllegalArgumentException {
   private static final long serialVersionUID = -7582725398713884908L;
   private final String argumentName;
   private final String seriesName;
   private final Integer argumentLength;
   private final Integer seriesLength;

   public IllegalSeriesLengthException(String argumentName, String seriesName) {
      super(createMessage(argumentName, seriesName));
      this.argumentName = argumentName;
      this.seriesName = seriesName;
      this.argumentLength = null;
      this.seriesLength = null;
   }

   public IllegalSeriesLengthException(String argumentName, String seriesName, int argumentLength, int seriesLength) {
      super(createMessage(argumentName, seriesName, argumentLength, seriesLength));
      this.argumentName = argumentName;
      this.seriesName = seriesName;
      this.argumentLength = argumentLength;
      this.seriesLength = seriesLength;
   }

   public IllegalSeriesLengthException(String argumentName, String seriesName, Throwable cause) {
      super(createMessage(argumentName, seriesName), cause);
      this.argumentName = argumentName;
      this.seriesName = seriesName;
      this.argumentLength = null;
      this.seriesLength = null;
   }

   public IllegalSeriesLengthException(String argumentName, String seriesName, int argumentLength, int seriesLength, Throwable cause) {
      super(createMessage(argumentName, seriesName, argumentLength, seriesLength), cause);
      this.argumentName = argumentName;
      this.seriesName = seriesName;
      this.argumentLength = argumentLength;
      this.seriesLength = seriesLength;
   }

   public String getArgumentName() {
      return this.argumentName;
   }

   public String getSeriesName() {
      return this.seriesName;
   }

   public Integer getArgumentLength() {
      return this.argumentLength;
   }

   public Integer getSeriesLength() {
      return this.seriesLength;
   }

   private static String createMessage(String argumentName, String seriesName) {
      return "Length of provided " + argumentName + " does not match length of " + seriesName + ".";
   }

   private static String createMessage(String argumentName, String seriesName, int argumentLength, int seriesLength) {
      return createMessage(argumentName, seriesName) + " Length of " + argumentName + ": " + argumentLength + "; Length of " + seriesName + ": " + seriesLength;
   }
}
