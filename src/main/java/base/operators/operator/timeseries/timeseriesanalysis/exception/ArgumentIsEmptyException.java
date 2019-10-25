package base.operators.operator.timeseries.timeseriesanalysis.exception;

public class ArgumentIsEmptyException extends IllegalArgumentException {
   private static final long serialVersionUID = -2688389481653710299L;
   private final String argumentName;

   public ArgumentIsEmptyException(String argumentName) {
      super(createMessage(argumentName));
      this.argumentName = argumentName;
   }

   public ArgumentIsEmptyException(String argumentName, Throwable cause) {
      super(createMessage(argumentName), cause);
      this.argumentName = argumentName;
   }

   public String getArgumentName() {
      return this.argumentName;
   }

   private static String createMessage(String argumentName) {
      return "Provided " + argumentName + " is empty.";
   }
}
