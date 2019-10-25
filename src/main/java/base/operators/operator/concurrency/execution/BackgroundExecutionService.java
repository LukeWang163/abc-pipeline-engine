package base.operators.operator.concurrency.execution;

import base.operators.RapidMiner;
import base.operators.core.concurrency.ConcurrencyContext;
import base.operators.Process;
import base.operators.operator.ExecutionUnit;
import base.operators.operator.IOContainer;
import base.operators.operator.Operator;
import base.operators.operator.OperatorChain;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.concurrency.gui.helper.LogFormatter;
import base.operators.operator.tools.ThreadParameterProvider;
import base.operators.studio.concurrency.internal.BackgroundConcurrencyContext;
import base.operators.studio.concurrency.internal.ConcurrencyExecutionService;
import base.operators.studio.concurrency.internal.ExecutionExceptionHandling;
import base.operators.studio.concurrency.internal.util.BackgroundExecution;
import base.operators.studio.concurrency.internal.util.BackgroundExecutionServiceListener;
import base.operators.studio.concurrency.internal.util.ProcessBackgroundExecution;
import base.operators.studio.concurrency.internal.util.ProcessBackgroundExecutionState;
import base.operators.studio.internal.Resources;
import base.operators.tools.LogService;
import base.operators.tools.ParameterService;
import base.operators.tools.ProcessTools;
import base.operators.tools.RandomGenerator;
import base.operators.tools.XMLException;
import base.operators.tools.container.Pair;
import base.operators.tools.parameter.ParameterChangeListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


public final class BackgroundExecutionService implements ConcurrencyExecutionService {
    private final List<BackgroundExecution> backgroundExecutions;
    private int executionSize;
    private final ExecutorService processQueue;
    private final CopyOnWriteArrayList<BackgroundExecutionServiceListener> listeners;
    private final Map<Pair<Process, String>, OperatorBackgroundExecution> operatorTaskExecutions;

    public BackgroundExecutionService() {
        this.backgroundExecutions = Collections.synchronizedList(new ArrayList());
        this.executionSize = getThreadPoolSize();

        this.processQueue = Executors.newFixedThreadPool(this.executionSize);
        this.listeners = new CopyOnWriteArrayList();
        this.operatorTaskExecutions = new HashMap();

        ParameterService.registerParameterChangeListener(new ParameterChangeListener() {
            @Override
            public void informParameterChanged(String key, String value) {}

            @Override
            public void informParameterSaved() {
                BackgroundExecutionService.this.updatePoolSize();
            }
        });


//        ProductConstraintManager.INSTANCE.registerLicenseManagerListener(new LicenseManagerListener()
//        {
//            @Override
//            public <S, C> void handleLicenseEvent(LicenseEvent<S, C> event)
//            {
//                if (event.getType() == LicenseEvent.LicenseEventType.ACTIVE_LICENSE_CHANGED || event
//                        .getType() == LicenseEvent.LicenseEventType.LICENSE_EXPIRED) {
        BackgroundExecutionService.this.updatePoolSize();
//                }
//            }
//        });
    }

    private static final Formatter LOG_FORMATTER = new LogFormatter();

    private final void updatePoolSize() {
        if (this.executionSize != getThreadPoolSize()) {
            this.executionSize = getThreadPoolSize();
            ((ThreadPoolExecutor)this.processQueue).setCorePoolSize(this.executionSize);
            ((ThreadPoolExecutor)this.processQueue).setMaximumPoolSize(this.executionSize);
            LogService.getRoot().log(Level.CONFIG, "Updated BES to size " + this.executionSize + ".");
        }
    }


    @Override
    public void executeProcess(Process process) throws UserError { executeProcess(process, null, null); }


    @Override
    public void executeProcess(Process process, IOContainer container, Map<String, String> macroSettings) throws UserError {
        if (ThreadParameterProvider.getNumberOfAllowedParallelProcesses() < 1) {
            throw new UserError(process.getRootOperator(), "background_process_not_allowed");
        }

        try {
            Process originalProcess = (Process)process.clone();


            Process noPauseProcess = new Process(process.getRootOperator().getXML(true), process) {
                @Override
                public void pause() {}

                @Override
                public void pause(Operator operator, IOContainer iocontainer, int breakpointType) {}

                @Override
                protected Logger makeLogger() {
                    return Logger.getLogger(Process.class.getName() + UUID.randomUUID().toString());
                }
            };
//            ProcessTools.setProcessOrigin(noPauseProcess);

            if (!RapidMiner.getExecutionMode().isHeadless())
            {
                noPauseProcess.getRootOperator().setUserData("base.operators.gui.isGUIProcess", null);
            }

            for (Operator chain : noPauseProcess.getAllOperators()) {
                if (chain instanceof OperatorChain) {
                    for (ExecutionUnit unit : ((OperatorChain)chain).getSubprocesses()) {
                        for (Operator operator : unit.getAllInnerOperators()) {
                            if (operator.isEnabled() && operator.hasBreakpoint()) {
                                operator.setBreakpoint(0, false);
                                operator.setBreakpoint(1, false);
                            }
                        }
                    }
                }
            }

            noPauseProcess.getRootOperator().setUserData("base.operators.core.concurrency.ContextUserData", new Resources.OverridingContextUserData(new BackgroundConcurrencyContext(noPauseProcess)));


            Callable<IOContainer> task = new BackgroundProcessExecutor(noPauseProcess, container, macroSettings);
            ProcessBackgroundExecutionState processState = new ProcessBackgroundExecutionState(noPauseProcess);
            Logger logger = noPauseProcess.getLogger();
            logger.setUseParentHandlers(false);

            for (Handler toRemove : logger.getHandlers()) {
                logger.removeHandler(toRemove);
            }

            Handler fileHandler = null;
            try {
//                String id = (noPauseProcess.getRepositoryLocation() != null) ? noPauseProcess.getRepositoryLocation().getName() : "unsaved";
                String id = UUID.randomUUID().toString();
                Path logFile = Files.createTempFile("rm-process-background-" + id, ".log", new java.nio.file.attribute.FileAttribute[0]);

                logFile.toFile().deleteOnExit();

                fileHandler = new FileHandler(logFile.toAbsolutePath().toString(), false)
                {
                    @Override
                    public void publish(LogRecord record)
                    {
                        super.publish(record);
                        flush();
                    }
                };
                fileHandler.setFormatter(LOG_FORMATTER);
                fileHandler.setEncoding(StandardCharsets.UTF_8.name());

                Logger logLevelProvider = LogService.getRoot();
                Level newLevel = logLevelProvider.getLevel();
                while (newLevel == null && logLevelProvider != null) {
                    if (logLevelProvider == logLevelProvider.getParent()) {

                        newLevel = Level.INFO;

                        break;
                    }
                    newLevel = logLevelProvider.getLevel();
                    logLevelProvider = logLevelProvider.getParent();
                }
                if (newLevel == null)
                {
                    newLevel = Level.INFO;
                }
                fileHandler.setLevel(newLevel);

                Level customLevel = Level.parse(ParameterService.getParameterValue("rapidminer.gui.log_level"));

                if (customLevel.intValue() > Level.INFO.intValue()) {
                    customLevel = Level.INFO;
                }
                fileHandler.setLevel(customLevel);
                logger.addHandler(fileHandler);
                processState.setLogFilePath(logFile);
            } catch (SecurityException|IOException e) {
                System.err.println("The logfile handler could not be created!");
                e.printStackTrace();
            }
            Future<IOContainer> futureResults = this.processQueue.submit(task);
            processState.setResults(futureResults);
            ProcessBackgroundExecution backgroundExecution = new ProcessBackgroundExecution(originalProcess, noPauseProcess, processState);
            this.backgroundExecutions.add(backgroundExecution);
            newProcessEvent(backgroundExecution);
        } catch (IOException e) {
//            throw new UserError(null, '?', new Object[] { (process != null && process.getRepositoryLocation() != null) ? process.getRepositoryLocation().getName() : "Unsaved Process", e });
            throw new UserError(null, '?', new Object[] { e });
        } catch (XMLException e) {
            throw new UserError(null, '?', new Object[] { e });
        }
    }

    @Override
    public <V, T> Callable<V> prepareOperatorTask(Process parentProcess, Operator clonedOperator, int applyCount, boolean synchronizeSideEffects, Callable<V> task) {
        BackgroundExecutionProcess process = new BackgroundExecutionProcess(parentProcess, clonedOperator, synchronizeSideEffects);

        RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(parentProcess, -1);
        process.getRootOperator().setParameter("random_seed",
                Long.toString(randomGenerator.nextLongInRange(1L, 2147483648L)));

        Pair<Process, String> identifier = new Pair<Process, String>(parentProcess, clonedOperator.getName());
        OperatorBackgroundExecution execution = (OperatorBackgroundExecution)this.operatorTaskExecutions.get(identifier);
        if (execution == null) {
            execution = new OperatorBackgroundExecution(parentProcess, clonedOperator.getName());
            this.operatorTaskExecutions.put(identifier, execution);
            this.backgroundExecutions.add(execution);
            newProcessEvent(execution);
        }
        execution.addTask(applyCount, process, clonedOperator);
        String clonedOperatorName = clonedOperator.getName();
        return new ExecutionCallable(task, process, parentProcess, clonedOperatorName, applyCount);
    }

    private class ExecutionCallable<V> extends Object implements Callable<V> {
        private Process parentProcess;

        private BackgroundExecutionProcess process;

        private String name;

        private int applyCount;

        private Callable<V> task;

        private ExecutionCallable(Callable<V> task, BackgroundExecutionProcess process, Process parentProcess, String name, int applyCount) {
            this.process = process;
            this.name = name;
            this.applyCount = applyCount;
            this.task = task;
            this.parentProcess = parentProcess;
        }

        @Override
        public V call() throws Exception {
            try {
                RandomGenerator.init(this.process);
                V v = (V)this.task.call();
                this.process.synchronizeSideEffects();
                return v;
            } catch (Exception e) {


                this.process.setProcessException(e);
                throw e;
            } finally {
//                BackgroundExecutionService.this.removeOperatorTask(this.parentProcess, this.name, this.applyCount);

                this.process = null;
                this.task = null;
            }
        }
    }

    @Override
    public <T> List<T> executeOperatorTasks(Operator operator, List<Callable<T>> tasks) throws OperatorException {
        if (operator == null) {
            throw new IllegalArgumentException("Operator must not be null");
        }
        ConcurrencyContext context = Resources.getConcurrencyContext(operator);
        try {
            return context.call(tasks);
        } catch (ExecutionException e) {
            throw ExecutionExceptionHandling.INSTANCE.processExecutionException(e, operator.getProcess());
        }
    }

    @Override
    public <T> Future<T> submitOperatorTask(Operator operator, Callable<T> task) {
        if (operator == null) {
            throw new IllegalArgumentException("Operator must not be null");
        }
        ConcurrencyContext context = Resources.getConcurrencyContext(operator);
        return (Future)context.submit(Collections.singletonList(task)).get(0);
    }

    @Override
    public <T> List<T> collectResults(Operator operator, List<Future<T>> futures) throws OperatorException {
        if (operator == null) {
            throw new IllegalArgumentException("Operator must not be null");
        }
        ConcurrencyContext context = Resources.getConcurrencyContext(operator);
        try {
            return context.collectResults(futures);
        } catch (ExecutionException e) {
            throw ExecutionExceptionHandling.INSTANCE.processExecutionException(e, operator.getProcess());
        }
    }

    @Override
    public void removeOperatorTask(Process parentProcess, String operatorName, int applyCount) {
        Pair<Process, String> identifier = new Pair<Process, String>(parentProcess, operatorName);
        OperatorBackgroundExecution execution = (OperatorBackgroundExecution)this.operatorTaskExecutions.get(identifier);
        if (execution != null) {
            execution.removeTask(applyCount);
            if (execution.getNumberOfTasks() == 0) {
                this.backgroundExecutions.remove(execution);
                this.operatorTaskExecutions.remove(identifier);
                removedProcessEvent(execution);
            }
        }
    }

    @Override
    public List<BackgroundExecution> getExecutions() { return new ArrayList(this.backgroundExecutions); }

    @Override
    public <T> T executeBlockingTask(Callable<T> callable) throws Exception { return (T)callable.call(); }

    @Override
    public void stopProcessExecution(ProcessBackgroundExecution execution) {
        if (!execution.getBackgroundExecutionState().isRunning()) {
            return;
        }

        execution.getBackgroundExecutionState().setStopped();
        Process process = execution.getProcess();
        if (process != null) {
            process.stop();
        }
    }

    @Override
    public void removeProcessExecution(ProcessBackgroundExecution execution) {
        this.backgroundExecutions.remove(execution);
        removedProcessEvent(execution);
    }

    @Override
    public void addListener(BackgroundExecutionServiceListener listener) {
        if (listener != null) {
            this.listeners.addIfAbsent(listener);
        }
    }

    @Override
    public void newProcessEvent(BackgroundExecution execution) { this.listeners.forEach(listener -> listener.processAdded(execution)); }

    @Override
    public void removedProcessEvent(BackgroundExecution execution) { this.listeners.forEach(listener -> listener.processRemoved(execution)); }

    private int getThreadPoolSize() { return Math.max(ThreadParameterProvider.getNumberOfParallelProcesses(), 1); }
}
