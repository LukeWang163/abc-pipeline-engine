package base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception;

public class NotStrictlyMonotonicIncreasingException extends IllegalArgumentException {
   private static final long serialVersionUID = 539311077017107699L;
   private static final String MESSAGE = "Provided index values are not strictly monotonic increasing.";

   public NotStrictlyMonotonicIncreasingException() {
      super("Provided index values are not strictly monotonic increasing.");
   }

   public NotStrictlyMonotonicIncreasingException(Throwable cause) {
      super("Provided index values are not strictly monotonic increasing.", cause);
   }
}
