package base.operators.operator.concurrency.execution;

import base.operators.Process;
import base.operators.core.license.DatabaseConstraintViolationException;
import base.operators.core.license.LicenseViolationException;
import base.operators.operator.IOContainer;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessStoppedException;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.LogService;
import base.operators.tools.ParameterService;
import base.operators.tools.SystemInfoUtilities;
import base.operators.tools.Tools;
import base.operators.tools.usagestats.ActionStatisticsCollector;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;

public class BackgroundProcessExecutor extends Object implements Callable<IOContainer> {
    private final Process process;
    private final IOContainer container;
    private final Map<String, String> macroSettings;

    public BackgroundProcessExecutor(Process process, IOContainer container, Map<String, String> macroSettings) {
        this.process = process;
        this.container = container;
        this.macroSettings = macroSettings;
    }

    @Override
    public IOContainer call() throws OperatorException {
        IOContainer results = null;
        try {
            results = this.process.run(this.container, 7, this.macroSettings);
            this.process.getRootOperator().sendEmail(results, null);
        } catch (DatabaseConstraintViolationException ex) {
            if (ex.getOperatorName() != null) {
                this.process.getLogger().log(Level.SEVERE, "base.operators.gui.ProcessThread.database_constraint_violation_exception_in_operator", new Object[] { ex

                        .getDatabaseURL(), ex.getOperatorName() });
            } else {
                this.process.getLogger().log(Level.SEVERE, "base.operators.gui.ProcessThread.database_constraint_violation_exception", new Object[] { ex

                        .getDatabaseURL() });
            }
            throw ex;
        } catch (LicenseViolationException ex) {
            this.process.getLogger().log(Level.SEVERE, "base.operators.gui.ProcessThread.operator_constraint_violation_exception", new Object[] { ex
                    .getOperatorName() });
            throw ex;
        } catch (ProcessStoppedException ex) {
            this.process.getLogger().info(ex.getMessage());
            throw ex;
        } catch (Throwable e) {
            String location = (this.process.getProcessLocation() == null) ? "Unsaved process" : this.process.getProcessLocation().toString();
            LogService.getRoot().log(Level.SEVERE, "Background process failed: " + location, e);
            if (!(e instanceof OperatorException)) {
                ActionStatisticsCollector.getInstance().log(this.process.getCurrentOperator(), "FAILURE");

                ActionStatisticsCollector.getInstance().log(this.process.getCurrentOperator(), "RUNTIME_EXCEPTION");
            }

            String debugProperty = ParameterService.getParameterValue("rapidminer.general.debugmode");
            boolean debugMode = Tools.booleanValue(debugProperty, false);
            String message = e.getMessage();
            if (!debugMode &&
                    e instanceof RuntimeException) {
                if (e.getMessage() != null) {
                    message = "operator cannot be executed (" + e.getMessage() + "). Check the log messages...";
                } else {
                    message = "operator cannot be executed. Check the log messages...";
                }
            }

            this.process.getLogger().log(Level.SEVERE, "Process failed: " + message, e);
            logProcessTreeList(10, "==>", this.process.getCurrentOperator());

            try {
                this.process.getRootOperator().sendEmail(null, e);
            } catch (UndefinedParameterError ex) {

                this.process.getLogger().log(Level.WARNING, "Problems during sending result mail: " + ex.getMessage(), ex);
            }
            if (e instanceof OutOfMemoryError)
            {
                ActionStatisticsCollector.getInstance().log("error", "out_of_memory",
                        String.valueOf(SystemInfoUtilities.getMaxHeapMemorySize()));
            }
            throw e;
        }
        return results;
    }

    private void logProcessTreeList(int indent, String mark, Operator markOperator) {
        this.process.getLogger().log(Level.SEVERE, "Here: ");
        List<String> processTreeList = this.process.getRootOperator().createProcessTreeList(indent, "", "", markOperator, mark);
        for (String logEntry : processTreeList) {
            this.process.getLogger().log(Level.SEVERE, logEntry);
        }
    }
}

