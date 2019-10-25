package base.operators.operator.timeseries.timeseriesanalysis.exception;

public class IllegalIndexArgumentException extends IllegalArgumentException {
   private static final long serialVersionUID = -3386211115241146030L;
   private final String indexName;
   private final Number number;
   private final IllegalIndexArgumentType type;

   public IllegalIndexArgumentException(String indexName, IllegalIndexArgumentType type) {
      super(createMessage(indexName, type));
      this.indexName = indexName;
      this.type = type;
      this.number = null;
   }

   public IllegalIndexArgumentException(String indexName, Number number, IllegalIndexArgumentType type) {
      super(createMessage(indexName, number, type));
      this.indexName = indexName;
      this.type = type;
      this.number = number;
   }

   public IllegalIndexArgumentException(String indexName, IllegalIndexArgumentType type, Throwable cause) {
      super(createMessage(indexName, type), cause);
      this.indexName = indexName;
      this.type = type;
      this.number = null;
   }

   public IllegalIndexArgumentException(String indexName, Number number, IllegalIndexArgumentType type, Throwable cause) {
      super(createMessage(indexName, number, type), cause);
      this.indexName = indexName;
      this.type = type;
      this.number = number;
   }

   public String getIndexName() {
      return this.indexName;
   }

   public Number getNumber() {
      return this.number;
   }

   public IllegalIndexArgumentType getType() {
      return this.type;
   }

   private static String createMessage(String indexName, IllegalIndexArgumentType type) {
      return "Provided " + indexName + " is " + type + ".";
   }

   private static String createMessage(String indexName, Number number, IllegalIndexArgumentType type) {
      return createMessage(indexName, type) + " " + indexName + ": " + number;
   }

   public static enum IllegalIndexArgumentType {
      NEGATIVE("negative"),
      NEGATIVE_ZERO("negative or zero"),
      POSITIVE("positive"),
      POSITIVE_ZERO("positive or zero");

      private final String text;

      private IllegalIndexArgumentType(String text) {
         this.text = text;
      }

      public String toString() {
         return this.text;
      }
   }
}
