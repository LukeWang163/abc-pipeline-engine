package base.operators.operator.tools;

import base.operators.studio.internal.ParameterServiceProvider;
import base.operators.tools.LogService;
import base.operators.tools.ParameterService;
import java.util.logging.Level;

public class ThreadParameterProvider implements ParameterServiceProvider {
    @Override
    public String getParameterValue(String key) {
        if ("rapidminer.general.number_of_threads".equals(key)) {
            return Integer.toString(getNumberOfThreads());
        }
        else if ("rapidminer.general.number_of_threads_background".equals(key)) {
            return Integer.toString(getNumberOfBackgroundThreads());
        }
        return ParameterService.getParameterValue(key);
    }

    public static int getNumberOfThreads() { return getNumberOfThreads("rapidminer.general.number_of_threads"); }

    public static int getNumberOfBackgroundThreads() { return getNumberOfThreads("rapidminer.general.number_of_threads_background"); }

    private static int getNumberOfThreads(String key) {
        int maxAllowedThreads = getNumberOfAllowedThreads();
        String numberOfThreads = ParameterService.getParameterValue(key);
        int numOfThreads = 0;
        if (numberOfThreads != null) {
            try {
                numOfThreads = Integer.parseInt(numberOfThreads);
            } catch (NumberFormatException numberFormatException) {}
        }

        if (numOfThreads == 0) {
            numOfThreads = Math.min(Runtime.getRuntime().availableProcessors() - 1, maxAllowedThreads);
        } else {
            numOfThreads = Math.min(numOfThreads, maxAllowedThreads);
        }
        return Math.max(1, numOfThreads);
    }

    public static int getNumberOfAllowedThreads() {
//        ProductConstraintManager productConstraintManager = ProductConstraintManager.INSTANCE;
//        License activeLicense = productConstraintManager.getActiveLicense();
//        Integer maxAllowedThreads = null;
//        try {
//            String stringConstraint = activeLicense.getConstraints().getConstraintValue(productConstraintManager.getLogicalProcessorConstraint());
//            if (stringConstraint != null)
//            {
//
//                maxAllowedThreads = Integer.valueOf(Integer.parseInt(stringConstraint));
//            }
//        } catch (ConstraintNotRestrictedException constraintNotRestrictedException) {}
//
//        if (maxAllowedThreads == null) {
//            maxAllowedThreads = Integer.valueOf(2147483647);
//        }
        return Runtime.getRuntime().availableProcessors() - 1;
    }

    public static int getNumberOfAllowedBackgroundThreads() { return getNumberOfAllowedParallelProcesses(); }

    public static int getNumberOfAllowedParallelProcesses() {
//        if (ProductConstraintManager.INSTANCE.getActiveLicense()
//                .getPrecedence() < 70 &&
//                !ProductConstraintManager.INSTANCE.isTrialLicense()) {
//            return 0;
//        }
        return getNumberOfAllowedThreads();
    }

    public static int getNumberOfParallelProcesses() {
        int maxAllowedProcesses = getNumberOfAllowedParallelProcesses();
        if (maxAllowedProcesses == 0) {
            return maxAllowedProcesses;
        }

        String numberOfProcesses = ParameterService.getParameterValue("rapidminer.general.number_of_processes");
        int numOfProcesses = 0;
        if (numberOfProcesses != null) {
            try {
                numOfProcesses = Integer.parseInt(numberOfProcesses);
                LogService.getRoot().log(Level.FINE, "Parsed BES setting of " + numOfProcesses + ".");
            } catch (NumberFormatException e) {

                LogService.getRoot().log(Level.FINE, "Failed to parse BES setting of " + numberOfProcesses + ".");
            }
        }

        if (numOfProcesses == 0) {
            numOfProcesses = Math.min(Runtime.getRuntime().availableProcessors() - 1, maxAllowedProcesses);
        } else {
            numOfProcesses = Math.min(numOfProcesses, maxAllowedProcesses);
        }

        return Math.max(1, numOfProcesses);
    }
}
