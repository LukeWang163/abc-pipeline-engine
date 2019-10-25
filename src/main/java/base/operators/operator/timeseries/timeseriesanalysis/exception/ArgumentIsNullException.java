package base.operators.operator.timeseries.timeseriesanalysis.exception;

public class ArgumentIsNullException extends IllegalArgumentException {
   private static final long serialVersionUID = 3202737834751635127L;
   private final String argumentName;

   public ArgumentIsNullException(String argumentName) {
      super(createMessage(argumentName));
      this.argumentName = argumentName;
   }

   public ArgumentIsNullException(String argumentName, Throwable cause) {
      super(createMessage(argumentName), cause);
      this.argumentName = argumentName;
   }

   public String getArgumentName() {
      return this.argumentName;
   }

   private static String createMessage(String argumentName) {
      return "Provided " + argumentName + " is null.";
   }
}
