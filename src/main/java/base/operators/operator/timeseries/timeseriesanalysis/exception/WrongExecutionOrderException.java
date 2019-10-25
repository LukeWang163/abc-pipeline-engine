package base.operators.operator.timeseries.timeseriesanalysis.exception;

import org.apache.commons.lang3.StringUtils;

public class WrongExecutionOrderException extends RuntimeException {
   private static final long serialVersionUID = 7982763706430588143L;
   private final String problem;
   private final String[] methodsToRunBefore;

   public WrongExecutionOrderException(String problem, String... methodsToRunBefore) {
      super(createMessage(problem, methodsToRunBefore));
      this.problem = problem;
      this.methodsToRunBefore = methodsToRunBefore;
   }

   public WrongExecutionOrderException(Throwable cause, String problem, String... methodsToRunBefore) {
      super(createMessage(problem, methodsToRunBefore), cause);
      this.problem = problem;
      this.methodsToRunBefore = methodsToRunBefore;
   }

   public String getProblem() {
      return this.problem;
   }

   public String[] getMethodsToRunBefore() {
      return this.methodsToRunBefore;
   }

   private static String createMessage(String problem, String... methodsToRunBefore) {
      return problem + " Run " + StringUtils.join((Object[])methodsToRunBefore, ", ") + " before.";
   }
}
