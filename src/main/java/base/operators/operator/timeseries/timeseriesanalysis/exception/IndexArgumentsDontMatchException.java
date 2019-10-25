package base.operators.operator.timeseries.timeseriesanalysis.exception;

public class IndexArgumentsDontMatchException extends IllegalArgumentException {
   private static final long serialVersionUID = -9108401809515402524L;
   private final String indexName1;
   private final String indexName2;
   private final Number number1;
   private final Number number2;
   private final MisMatchType type;

   public IndexArgumentsDontMatchException(String indexName1, String indexName2, MisMatchType type) {
      super(createMessage(indexName1, indexName2, type));
      this.indexName1 = indexName1;
      this.indexName2 = indexName2;
      this.number1 = null;
      this.number2 = null;
      this.type = type;
   }

   public IndexArgumentsDontMatchException(String indexName1, String indexName2, Number number1, Number number2, MisMatchType type) {
      super(createMessage(indexName1, indexName2, number1, number2, type));
      this.indexName1 = indexName1;
      this.indexName2 = indexName2;
      this.number1 = number1;
      this.number2 = number2;
      this.type = type;
   }

   public IndexArgumentsDontMatchException(String indexName1, String indexName2, MisMatchType type, Throwable cause) {
      super(createMessage(indexName1, indexName2, type), cause);
      this.indexName1 = indexName1;
      this.indexName2 = indexName2;
      this.number1 = null;
      this.number2 = null;
      this.type = type;
   }

   public IndexArgumentsDontMatchException(String indexName1, String indexName2, Number number1, Number number2, MisMatchType type, Throwable cause) {
      super(createMessage(indexName1, indexName2, number1, number2, type), cause);
      this.indexName1 = indexName1;
      this.indexName2 = indexName2;
      this.number1 = number1;
      this.number2 = number2;
      this.type = type;
   }

   public String getIndexName1() {
      return this.indexName1;
   }

   public String getIndexName2() {
      return this.indexName2;
   }

   public Number getNumber1() {
      return this.number1;
   }

   public Number getNumber2() {
      return this.number2;
   }

   public MisMatchType getType() {
      return this.type;
   }

   private static String createMessage(String indexName1, String indexName2, MisMatchType type) {
      return indexName1 + " is " + type + " " + indexName2 + ".";
   }

   private static String createMessage(String indexName1, String indexName2, Number number1, Number number2, MisMatchType type) {
      return createMessage(indexName1, indexName2, type) + " " + indexName1 + ": " + number1 + "; " + indexName2 + ": " + number2;
   }

   public static enum MisMatchType {
      SMALLER("smaller than"),
      SMALLER_EQUAL("smaller than or equal to"),
      LARGER("larger than"),
      LARGER_EQUAL("larger than or equal to");

      private final String text;

      private MisMatchType(String text) {
         this.text = text;
      }

      public String toString() {
         return this.text;
      }
   }
}
