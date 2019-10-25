package base.operators.operator.timeseries.operator.helper;

import base.operators.operator.OperatorException;

public class WrongConfiguredHelperException extends OperatorException {
   private static final long serialVersionUID = -8692968918353322762L;
   private final String methodCalled;
   private final String neededConfiguration;
   private final String methodToBeCalledBefore;

   public WrongConfiguredHelperException(String methodCalled, String neededConfiguration, String methodToBeCalledBefore) {
      super(createMessage(methodCalled, neededConfiguration, methodToBeCalledBefore));
      this.methodCalled = methodCalled;
      this.neededConfiguration = neededConfiguration;
      this.methodToBeCalledBefore = methodToBeCalledBefore;
   }

   public WrongConfiguredHelperException(String methodCalled, String neededConfiguration) {
      super(createMessage(methodCalled, neededConfiguration));
      this.methodCalled = methodCalled;
      this.neededConfiguration = neededConfiguration;
      this.methodToBeCalledBefore = null;
   }

   private WrongConfiguredHelperException(String methodCalled, String neededConfiguration, String methodToBeCalledBefore, Throwable cause) {
      super(createMessage(methodCalled, neededConfiguration, methodToBeCalledBefore), cause);
      this.methodCalled = methodCalled;
      this.neededConfiguration = neededConfiguration;
      this.methodToBeCalledBefore = methodToBeCalledBefore;
   }

   private WrongConfiguredHelperException(String methodCalled, String neededConfiguration, Throwable cause) {
      super(createMessage(methodCalled, neededConfiguration), cause);
      this.methodCalled = methodCalled;
      this.neededConfiguration = neededConfiguration;
      this.methodToBeCalledBefore = null;
   }

   public String getMethodCalled() {
      return this.methodCalled;
   }

   public String getNeededConfiguration() {
      return this.neededConfiguration;
   }

   public String getMethodToBeCalledBefore() {
      return this.methodToBeCalledBefore;
   }

   private static String createMessage(String methodCalled, String neededConfiguration) {
      return methodCalled + " is called, but operator was " + neededConfiguration + ". This is a bug in the operator. Please report this in the Product Feedback forum of the RapidMiner Community.";
   }

   private static String createMessage(String methodCalled, String neededConfiguration, String methodToBeCalledBefore) {
      return methodCalled + " is called, but operator was " + neededConfiguration + ". Call " + methodToBeCalledBefore + " from the TimeSeriesHelperBuilder before. This is a bug in the operator. Please report this in the Product Feedback forum of the RapidMiner Community.";
   }
}
