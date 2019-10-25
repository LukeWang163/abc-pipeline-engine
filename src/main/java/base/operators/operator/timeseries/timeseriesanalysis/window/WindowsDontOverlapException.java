package base.operators.operator.timeseries.timeseriesanalysis.window;

public class WindowsDontOverlapException extends RuntimeException {
   private static final long serialVersionUID = 8620154692915943530L;
   private final String leftEdgeWindow1;
   private final String rightEdgeWindow1;
   private final String leftEdgeWindow2;
   private final String rightEdgeWindow2;

   public WindowsDontOverlapException(String leftEdgeWindow1, String rightEdgeWindow1, String leftEdgeWindow2, String rightEdgeWindow2) {
      super(createMessage(leftEdgeWindow1, rightEdgeWindow1, leftEdgeWindow2, rightEdgeWindow2));
      this.leftEdgeWindow1 = leftEdgeWindow1;
      this.rightEdgeWindow1 = rightEdgeWindow1;
      this.leftEdgeWindow2 = leftEdgeWindow2;
      this.rightEdgeWindow2 = rightEdgeWindow2;
   }

   public WindowsDontOverlapException(String leftEdgeWindow1, String rightEdgeWindow1, String leftEdgeWindow2, String rightEdgeWindow2, Throwable cause) {
      super(createMessage(leftEdgeWindow1, rightEdgeWindow1, leftEdgeWindow2, rightEdgeWindow2), cause);
      this.leftEdgeWindow1 = leftEdgeWindow1;
      this.rightEdgeWindow1 = rightEdgeWindow1;
      this.leftEdgeWindow2 = leftEdgeWindow2;
      this.rightEdgeWindow2 = rightEdgeWindow2;
   }

   public String getLeftEdgeWindow1() {
      return this.leftEdgeWindow1;
   }

   public String getRightEdgeWindow1() {
      return this.rightEdgeWindow1;
   }

   public String getLeftEdgeWindow2() {
      return this.leftEdgeWindow2;
   }

   public String getRightEdgeWindow2() {
      return this.rightEdgeWindow2;
   }

   private static String createMessage(String leftEdgeWindow1, String rightEdgeWindow1, String leftEdgeWindow2, String rightEdgeWindow2) {
      return "Windows do not overlap and check overlap is true. The windows are: [" + leftEdgeWindow1 + " , " + rightEdgeWindow1 + "] , [" + leftEdgeWindow2 + " , " + rightEdgeWindow2 + "].";
   }
}
