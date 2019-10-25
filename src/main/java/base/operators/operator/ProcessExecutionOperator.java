package base.operators.operator;

import base.operators.Process;
import base.operators.ProcessStateListener;
import base.operators.operator.IOContainer;
import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.ProcessRootOperator;
import base.operators.operator.ProcessSetupError;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.UserData;
import base.operators.operator.UserError;
import base.operators.operator.error.ProcessExecutionOperatorExceptionError;
import base.operators.operator.error.ProcessExecutionUserErrorError;
import base.operators.operator.internal.ProcessEmbeddingOperator;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.InputPortExtender;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.OutputPortExtender;
import base.operators.operator.ports.Port;
import base.operators.operator.ports.Ports;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SimpleMetaDataError;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeProcessLocation;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.AboveOperatorVersionCondition;
import base.operators.tools.Observable;
import base.operators.tools.Observer;
import base.operators.tools.ParameterService;
import base.operators.tools.ProcessTools;
import base.operators.tools.Tools;
import base.operators.tools.XMLException;
import base.operators.tools.container.Pair;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;


public class ProcessExecutionOperator extends Operator implements ProcessEmbeddingOperator {
    private static final OperatorVersion OPTION_TO_FAIL_ON_UNKNOWN_MACROS = new OperatorVersion(6, 0, 2);
    private final InputPortExtender inputExtender = new InputPortExtender("input", getInputPorts())
    {
        private boolean inWork;

        @Override
        protected void updatePorts() {
            if (!this.inWork) {
                this.inWork = true;
                Process innerProcess = ProcessExecutionOperator.this.cachedProcess;
                int defaultNumberOfPorts = 1;
                for (int index = this.managedPorts.size() - 1; index >= 0; index--) {
                    if (((InputPort)this.managedPorts.get(index)).isConnected()) {
                        defaultNumberOfPorts = index + 2;
                        break;
                    }
                }

                int innerSources = (innerProcess == null) ? defaultNumberOfPorts : innerProcess.getRootOperator().getSubprocess(0).getInnerSources().getNumberOfConnectedPorts();

                if (this.minNumber > innerSources) {
                    innerSources = this.minNumber;
                }
                if (this.managedPorts.size() < innerSources) {

                    int numbOfPorts = this.managedPorts.size();
                    do {
                        this.managedPorts.add(createPort());
                        ++numbOfPorts;
                    } while (numbOfPorts < innerSources);
                } else {
                    LinkedList<InputPort> markedForDelete = new LinkedList<InputPort>();
                    for (int index = this.managedPorts.size() - 1; index >= innerSources; ) {
                        InputPort port = (InputPort)this.managedPorts.get(index);
                        if (!port.isConnected() && !port.isLocked()) {
                            markedForDelete.add(port);

                            index--;
                        }
                    }

                    for (InputPort inputPort : markedForDelete) {
                        this.managedPorts.remove(inputPort);
                        deletePort(inputPort);
                    }
                }
                fixNames();
                this.inWork = false;
            }
        }
    };

    private final OutputPortExtender outputExtender = new OutputPortExtender("result", getOutputPorts())
    {
        private boolean inWork;

        @Override
        protected void updatePorts() {
            if (!this.inWork) {
                this.inWork = true;
                Process innerProcess = ProcessExecutionOperator.this.cachedProcess;

                int defaultNumberOfPorts = 1;
                for (int index = this.managedPorts.size() - 1; index >= 0; index--) {
                    if (((OutputPort)this.managedPorts.get(index)).isConnected()) {
                        defaultNumberOfPorts = index + 2;
                        break;
                    }
                }

                int innerSinks = (innerProcess == null) ? defaultNumberOfPorts : innerProcess.getRootOperator().getSubprocess(0).getInnerSinks().getNumberOfConnectedPorts();

                if (this.minNumber > innerSinks) {
                    innerSinks = this.minNumber;
                }
                if (this.managedPorts.size() < innerSinks) {

                    int numbOfPorts = this.managedPorts.size();
                    do {
                        this.managedPorts.add(createPort());
                        ++numbOfPorts;
                    } while (numbOfPorts < innerSinks);
                } else {
                    LinkedList<OutputPort> markedForDelete = new LinkedList<OutputPort>();
                    for (int index = this.managedPorts.size() - 1; index >= innerSinks; ) {
                        OutputPort port = (OutputPort)this.managedPorts.get(index);
                        if (!port.isConnected() && !port.isLocked()) {
                            markedForDelete.add(port);
                            index--;
                        }
                    }
                    for (OutputPort inputPort : markedForDelete) {
                        this.managedPorts.remove(inputPort);
                        deletePort(inputPort);
                    }
                }
                fixNames();
                this.inWork = false;
            }
        }
    };

    public static final String PARAMETER_USE_INPUT = "use_input";
    public static final String PARAMETER_STORE_OUTPUT = "store_output";
    public static final String PARAMETER_PROPAGATE_METADATA_RECURSIVELY = "propagate_metadata_recursively";
    public static final String PARAMETER_CACHE_PROCESS = "cache_process";
    public static final String PARAMETER_MACROS = "macros";
    public static final String PARAMETER_MACRO_NAME = "macro_name";
    public static final String PARAMETER_MACRO_VALUE = "macro_value";
    private static final String PARAMETER_FAIL_ON_UNKNOW_MACROS = "fail_for_unknown_macros";
    private Process cachedProcess;
    private ProcessSetupError cachedError = null;
    private int oldNumberOfInputs = 0, oldNumberOfOutputs = 0;

    public ProcessExecutionOperator(OperatorDescription description) {
        super(description);
        this.inputExtender.start();
        this.outputExtender.start();
        getParameters().addObserver(new Observer<String>() {
            @Override
            public void update(Observable<String> observable, String arg)
            {
                ProcessExecutionOperator.this.cachedProcess = null;
                ProcessExecutionOperator.this.cachedError = null;
            }
        },  false);

        getTransformer().addRule(new MDTransformationRule() {
            @Override
            public void transformMD()
            {
                if (ProcessExecutionOperator.this.getParameterAsBoolean("propagate_metadata_recursively")) {
                    if (ProcessExecutionOperator.this.cachedProcess == null) {
                        try {
                            ProcessExecutionOperator.this.cachedProcess = ProcessExecutionOperator.this.loadIncludedProcess();
                        } catch (Exception e) {
                            ProcessExecutionOperator.this.cachedError = new SimpleProcessSetupError(ProcessSetupError.Severity.ERROR, ProcessExecutionOperator.this.getPortOwner(), "cannot_load_included_process", new Object[] { e.getMessage() });
                            ProcessExecutionOperator.this.addError(ProcessExecutionOperator.this.cachedError);
                        }
                    }
                    if (ProcessExecutionOperator.this.cachedProcess != null) {
                        if (Tools.doesProcessContainPossibleCircle(ProcessExecutionOperator.this.cachedProcess)) {
                            Iterator<OutputPort> outputIterator = ProcessExecutionOperator.this.getOutputPorts().getAllPorts().iterator();
                            OutputPort port = (OutputPort)outputIterator.next();
                            ProcessExecutionOperator.this.addError(new SimpleMetaDataError(ProcessSetupError.Severity.INFORMATION, port, "included_process_recursiv_call", new Object[0]));
                            while (outputIterator.hasNext()) {
                                ((OutputPort)outputIterator.next()).deliverMD(null);
                            }
                            return;
                        }
                        ProcessRootOperator root = ProcessExecutionOperator.this.cachedProcess.getRootOperator();
                        if (ProcessExecutionOperator.this.getParameterAsBoolean("use_input")) {
                            int requires = root.getSubprocess(0).getInnerSources().getNumberOfConnectedPorts();
                            if (ProcessExecutionOperator.this.inputExtender.getManagedPorts().size() < requires || requires != ProcessExecutionOperator.this.oldNumberOfInputs) {
                                ProcessExecutionOperator.this.inputExtender.ensureMinimumNumberOfPorts(0);
                                ProcessExecutionOperator.this.oldNumberOfInputs = requires;
                            }
                            List<InputPort> managedInput = ProcessExecutionOperator.this.inputExtender.getManagedPorts();
                            for (int index = managedInput.size() - 1; index >= requires; index--) {
                                if (((InputPort)managedInput.get(index)).isConnected()) {
                                    ProcessExecutionOperator.this.addError(new SimpleMetaDataError(ProcessSetupError.Severity.WARNING, (Port)managedInput.get(index), "included_process_input_unused", new Object[0]));
                                }
                            }
                        }

                        int delivers = root.getSubprocess(0).getInnerSinks().getNumberOfConnectedPorts();
                        if (ProcessExecutionOperator.this.outputExtender.getManagedPorts().size() < delivers || delivers != ProcessExecutionOperator.this.oldNumberOfOutputs) {
                            ProcessExecutionOperator.this.outputExtender.ensureMinimumNumberOfPorts(0);
                            ProcessExecutionOperator.this.oldNumberOfOutputs = delivers;
                        }

                        List<OutputPort> managedOutput = ProcessExecutionOperator.this.outputExtender.getManagedPorts();
                        if (managedOutput.size() >= delivers) {
                            for (int index = managedOutput.size() - 1; index >= delivers; index--) {
                                if (((OutputPort)managedOutput.get(index)).isConnected()) {
                                    ProcessExecutionOperator.this.addError(new SimpleMetaDataError(ProcessSetupError.Severity.WARNING, (Port)managedOutput.get(index), "included_process_input_unused", new Object[0]));
                                }
                            }
                        }

                        if (ProcessExecutionOperator.this.getParameterAsBoolean("use_input")) {
                            List<MetaData> inputMetaData = new LinkedList<MetaData>();
                            for (InputPort port : ProcessExecutionOperator.this.inputExtender.getManagedPorts()) {
                                inputMetaData.add(port.getMetaData());
                            }
                            root.deliverInputMD(inputMetaData);
                        }
                        Process process = ProcessExecutionOperator.this.getProcess();
                        Process rootProcess = root.getProcess();
                        if (process != null && rootProcess != null) {
                            root.transformMetaData();
                            List<MetaData> result = root.getResultMetaData();
                            Iterator<MetaData> resultIterator = result.iterator();
                            Iterator<OutputPort> outputIterator = ProcessExecutionOperator.this.getOutputPorts().getAllPorts().iterator();
                            while (resultIterator.hasNext() && outputIterator.hasNext()) {
                                ((OutputPort)outputIterator.next()).deliverMD((MetaData)resultIterator.next());
                            }
                        }
                    } else {
                        ProcessExecutionOperator.this.inputExtender.ensureMinimumNumberOfPorts(0);
                        ProcessExecutionOperator.this.outputExtender.ensureMinimumNumberOfPorts(0);
                    }
                }
                try {
                    if (ProcessExecutionOperator.this.cachedProcess != null) {
                        ProcessExecutionOperator.this.getMacros(ProcessExecutionOperator.this.cachedProcess);
                    }
                } catch (UserError e) {
                    ProcessExecutionOperator.this.addError(new SimpleMetaDataError(ProcessSetupError.Severity.WARNING, ProcessExecutionOperator.this.getInputPorts().getPortByIndex(0), "included_process_macros_not_known", new Object[0]));
                }
                if (!ProcessExecutionOperator.this.getParameterAsBoolean("use_input")) {
                    ProcessExecutionOperator.this.inputExtender.ensureMinimumNumberOfPorts(0);
                    if (ProcessExecutionOperator.this.getInputPorts().getNumberOfConnectedPorts() > 0) {
                        for (InputPort input : ProcessExecutionOperator.this.inputExtender.getManagedPorts()) {
                            if (input.isConnected()) {
                                ProcessExecutionOperator.this.addError(new SimpleMetaDataError(ProcessSetupError.Severity.WARNING, input,
                                        Collections.singletonList(new ParameterSettingQuickFix(ProcessExecutionOperator.this, "use_input", "true")), "included_process_input_unused", new Object[0]));
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void performAdditionalChecks() {
        super.performAdditionalChecks();
        if (getParameterAsBoolean("propagate_metadata_recursively")) {
            if (this.cachedProcess == null) {
                try {
                    this.cachedProcess = loadIncludedProcess();
                } catch (Exception e) {
                    this.cachedError = new SimpleProcessSetupError(ProcessSetupError.Severity.ERROR, getPortOwner(), "cannot_load_included_process", new Object[] { e.getMessage() });
                    addError(this.cachedError);
                }
            } else if (this.cachedError != null) {
                addError(this.cachedError);
            }
        }
    }

    @Override
    public void doWork() throws OperatorException{
        final Process embeddedProcess;
        try {
            embeddedProcess = loadIncludedProcess();
            if (getProcess() != null) {
                getProcess().copyProcessFlowListenersToOtherProcess(embeddedProcess);
                embeddedProcess.setIOObjectCache(getProcess().getIOObjectCache());
                UserData<Object> overridingContext = getProcess().getRootOperator().getUserData("base.operators.core.concurrency.OverridingConcurrencyContext");
                embeddedProcess.getRootOperator().setUserData("base.operators.core.concurrency.OverridingConcurrencyContext", overridingContext);
                UserData<Object> concurrencyContext = getProcess().getRootOperator().getUserData("base.operators.core.concurrency.ContextUserData");
                embeddedProcess.getRootOperator().setUserData("base.operators.core.concurrency.ContextUserData", concurrencyContext);
                embeddedProcess.setDepth(getProcess().getDepth() + 1);
                int maxDepth = 100;
                try {
                    maxDepth = Integer.parseInt(ParameterService.getParameterValue("rapidminer.general.max_process_execution_nesting_depth"));
                } catch (NumberFormatException numberFormatException) {}

                if (embeddedProcess.getDepth() > maxDepth) {
                    throw new UserError(this, 969, new Object[] { Integer.valueOf(maxDepth) });
                }
            }
        } catch (Exception e) {
            throw new UserError(this, e, 312, new Object[] { getParameterAsString("process_location"), e.getMessage() });
        }

        ProcessStateListener processStopListener = new ProcessStateListener() {
            @Override
            public void stopped(Process process)
            {
                if (embeddedProcess != null) {
                    embeddedProcess.stop();
                }
            }

            @Override
            public void started(Process process) {}

            @Override
            public void paused(Process process) {
                if (embeddedProcess != null) {
                    embeddedProcess.pause();
                }
            }

            @Override
            public void resumed(Process process) {
                if (embeddedProcess != null) {
                    embeddedProcess.resume();
                }
            }
        };

        if (getProcess() != null) {
            getProcess().addProcessStateListener(processStopListener);
        }

        IOContainer result = null;
        try {
            Map<String, String> macroMap = getMacros(embeddedProcess);
            embeddedProcess.setOmitNullResults(false);

            if (getParameterAsBoolean("use_input")) {
                IOContainer input = new IOContainer();
                ListIterator<InputPort> inputs = getInputPorts().getAllPorts().listIterator(getInputPorts().getNumberOfPorts());
                while (inputs.hasPrevious()) {
                    input = input.append(((InputPort)inputs.previous()).getDataOrNull(IOObject.class));
                }
                result = embeddedProcess.run(input, -1, macroMap,
                        getParameterAsBoolean("store_output"));
            } else {
                result = embeddedProcess.run(new IOContainer(), -1, macroMap,
                        getParameterAsBoolean("store_output"));
            }
            Iterator<IOObject> results = result.asList().iterator();
            Iterator<OutputPort> outputs = getOutputPorts().getAllPorts().iterator();

            while (results.hasNext() && outputs.hasNext()) {
                ((OutputPort)outputs.next()).deliver((IOObject)results.next());
            }
        } catch (UserError e) {
            throw new ProcessExecutionUserErrorError(this, e);
        } catch (OperatorException e) {
            throw new ProcessExecutionOperatorExceptionError(this, e);
        } finally {
            if (getProcess() != null) {
                getProcess().removeProcessStateListener(processStopListener);
            }
        }
    }

    private Map<String, String> getMacros(Process embeddedProcess) throws UndefinedParameterError, UserError {
        boolean failOnUnknown = (!getCompatibilityLevel().isAtMost(OPTION_TO_FAIL_ON_UNKNOWN_MACROS) && getParameterAsBoolean("fail_for_unknown_macros"));
        List<Pair<String, String>> contextMacros = null;
        if (failOnUnknown) {
            contextMacros = embeddedProcess.getContext().getMacros();
        }
        Map<String, String> macroMap = new HashMap<String, String>();
        String processStart = getProcess().getMacroHandler().getMacro("process_start");
        macroMap.put("process_start", processStart);
        List<String[]> macros = getParameterList("macros");
        if (macros != null) {
            for (String[] macroPair : macros) {
                String macroName = macroPair[0];
                String macroValue = macroPair[1];
                if (failOnUnknown) {
                    boolean found = false;
                    if (contextMacros != null) {
                        for (Pair<String, String> pair : contextMacros) {
                            if (((String)pair.getFirst()).equals(macroName)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        throw new UserError(this, "processembeddingoperator.not_in_subprocess", new Object[] { macroName });
                    }
                }
                macroMap.put(macroName, macroValue);
            }
        }
        return macroMap;
    }

    private Process loadIncludedProcess() throws UndefinedParameterError, UserError {
        boolean useCache = getParameterAsBoolean("cache_process");
        if (useCache && this.cachedProcess != null) {
            return this.cachedProcess;
        }
//        RepositoryLocation location = getParameterAsRepositoryLocation("process_location");
//        Entry entry = location.locateEntry();
//        if (entry == null) {
//            throw new RepositoryEntryNotFoundException(location);
//        } else if (entry instanceof base.operators.repository.ProcessEntry) {
        // TODO: load Process from...
        Process process;
        File location = getParameterAsFile("process_location");
        if (location.exists()) {
            try {
                process = new Process(location);
//                process.setRepositoryAccessor(getProcess().getRepositoryAccessor());

                for (Operator op : process.getRootOperator().getAllInnerOperators()) {
                    op.setBreakpoint(1, false);
                    op.setBreakpoint(0, false);
                }

//                ProcessTools.setProcessOrigin(process);
            } catch (IOException e) {
                throw new UserError(this, '?', new Object[]{location, e.getMessage()});
            } catch (XMLException e) {
                throw new UserError(this, '?', new Object[]{e.getMessage()});
            }
            if (useCache) {
                this.cachedProcess = process;
            }
            return process;
        }
//        throw new RepositoryEntryWrongTypeException(location, "process", entry.getType());
        throw new UserError(this, "?", new Object[] {location});
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterTypeProcessLocation parameterTypeProcessLocation = new ParameterTypeProcessLocation("process_location", "The process location which should be encapsulated by this operator", false);
        parameterTypeProcessLocation.setPrimary(true);
        types.add(parameterTypeProcessLocation);
        types.add(new ParameterTypeBoolean("use_input", "Indicates if the operator input should be used as input of the process", true));
        types.add(new ParameterTypeBoolean("store_output", "Indicates if the operator output should be stored (if the context of the embedded process defines output locations).", false));
        types.add(new ParameterTypeBoolean("propagate_metadata_recursively", "Determines whether meta data is propagated through the included process.", true));
        types.add(new ParameterTypeBoolean("cache_process", "If checked, the process will not be loaded during execution.", false));
        types.add(new ParameterTypeList("macros", "Defines macros for this sub-process.", new ParameterTypeString("macro_name", "The name of the macro.", false), new ParameterTypeString("macro_value", "The value of the macro.", false), true));
        ParameterTypeBoolean parameterTypeBoolean = new ParameterTypeBoolean("fail_for_unknown_macros", "If checked, only macros defined in the embedded process' context can be defined in the 'macros' list above.", true, true);
        parameterTypeBoolean.registerDependencyCondition(new AboveOperatorVersionCondition(this, OPTION_TO_FAIL_ON_UNKNOWN_MACROS));
        types.add(parameterTypeBoolean);
        return types;
    }

    @Override
    public OperatorVersion[] getIncompatibleVersionChanges() {
        OperatorVersion[] old = super.getIncompatibleVersionChanges();
        OperatorVersion[] extended = (OperatorVersion[])Arrays.copyOf(old, old.length + 1);
        extended[old.length] = OPTION_TO_FAIL_ON_UNKNOWN_MACROS;
        return extended;
    }
}
