package base.operators.operator.concurrency.execution;

import base.operators.*;
import base.operators.Process;
import base.operators.datatable.DataTable;
import base.operators.datatable.DataTableRow;
import base.operators.datatable.SimpleDataTableRow;
import base.operators.operator.*;
import base.operators.operator.concurrency.internal.ParallelOperatorChain;
import base.operators.operator.tools.ConcurrencyTools;
import base.operators.tools.Observer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class BackgroundExecutionProcess extends Process {
    private Process parent;
    private boolean synchronizeStatelySideEffects;
    private boolean alwaysSynchronizeRememberedData;
    private List<String> storedObjectNames = new LinkedList();
    private static final String WRAPPER_NAME = "Concurrency Wrapper";
    private volatile Exception processException;

    public BackgroundExecutionProcess(Process parent, Operator clonedOperator, boolean synchronizeStatelySideEffects) {
        this.parent = parent;
        this.alwaysSynchronizeRememberedData = clonedOperator.getCompatibilityLevel().isAbove(ParallelOperatorChain.DOES_NOT_ALWAYS_SYNCHRONIZE_REMEMBERED_DATA);
        this.synchronizeStatelySideEffects = synchronizeStatelySideEffects;
        if (!synchronizeStatelySideEffects) {
            this.registerClonedOperators(clonedOperator);
        }

        this.copyMacros(parent, this);
        parent.copyProcessFlowListenersToOtherProcess(this);
        ExecutionUnit executionUnit = clonedOperator.getExecutionUnit();
        if (executionUnit != null) {
            ConcurrencyTools.setEnclosingProcess(clonedOperator, (ExecutionUnit)null);
        }

        this.getRootOperator().getSubprocess(0).addOperator(clonedOperator, synchronizeStatelySideEffects);
        UserData<Object> overridingContext = parent.getRootOperator().getUserData("base.operators.core.concurrency.OverridingConcurrencyContext");
        this.getRootOperator().setUserData("base.operators.core.concurrency.OverridingConcurrencyContext", overridingContext);
        UserData<Object> concurrencyContext = parent.getRootOperator().getUserData("base.operators.core.concurrency.ContextUserData");
        this.getRootOperator().setUserData("base.operators.core.concurrency.ContextUserData", concurrencyContext);
        if (executionUnit != null) {
            OperatorChain wrapper = new OperatorChain(clonedOperator.getOperatorDescription(), new String[]{"Concurrency Wrapper"}) {
                @Override
                public Process getProcess() {
                    return BackgroundExecutionProcess.this;
                }
            };
            ConcurrencyTools.setEnclosingProcess(wrapper, executionUnit);
            ExecutionUnit execution = new ExecutionUnit(wrapper, "Concurrency Wrapper");
            ConcurrencyTools.setEnclosingProcess(clonedOperator, execution);
        }

    }

    private void registerClonedOperators(Operator clonedOperator) {
        if (clonedOperator instanceof OperatorChain) {
            ((OperatorChain)clonedOperator).getAllInnerOperatorsAndMe().forEach((op) -> {
                this.registerName(op.getName(), op);
            });
        } else {
            this.registerName(clonedOperator.getName(), clonedOperator);
        }

    }

    public void setProcessException(Exception exception) {
        this.processException = exception;
    }

    private void copyMacros(Process source, Process target) {
        synchronized(target.getMacroHandler()) {
            MacroHandler sourceHandler = source.getMacroHandler();
            MacroHandler targetHandler = target.getMacroHandler();
            Iterator macroIterator = sourceHandler.getDefinedMacroNames();

            while(macroIterator.hasNext()) {
                String macroName = (String)macroIterator.next();
                targetHandler.addMacro(macroName, sourceHandler.getMacro(macroName));
            }

        }
    }

    public void synchronizeSideEffects() throws OperatorException {
        if (this.synchronizeStatelySideEffects) {
            this.copyMacros(this, this.parent);
            this.copyRememberedData(this, this.parent);
        } else if (this.alwaysSynchronizeRememberedData) {
            this.copyRememberedData(this, this.parent);
        }

        this.copyDataTables(this, this.parent);
    }

    private void copyRememberedData(BackgroundExecutionProcess source, Process target) {
        synchronized(target) {
            Iterator var4 = source.storedObjectNames.iterator();

            while(var4.hasNext()) {
                String storageName = (String)var4.next();
                target.store(storageName, source.retrieve(storageName, false));
            }

        }
    }

    private void copyDataTables(Process source, Process target) throws OperatorException {
        synchronized(target) {
            Iterator var4 = source.getDataTables().iterator();

            while(true) {
                while(var4.hasNext()) {
                    DataTable sourceTable = (DataTable)var4.next();
                    DataTable targetTable = target.getDataTable(sourceTable.getName());
                    if (targetTable == null) {
                        target.addDataTable(sourceTable);
                    } else {
                        if (targetTable.getNumberOfColumns() != sourceTable.getNumberOfColumns()) {
                            throw new UserError((Operator)null, "error.datatable.not_matching.parallel_operators", new Object[]{sourceTable.getName()});
                        }

                        boolean[] isNominalColumn = new boolean[targetTable.getColumnNumber()];

                        int rowIndex;
                        for(rowIndex = 0; rowIndex < targetTable.getColumnNumber(); ++rowIndex) {
                            isNominalColumn[rowIndex] = targetTable.isNominal(rowIndex);
                        }

                        rowIndex = 0;

                        for(Iterator var9 = sourceTable.iterator(); var9.hasNext(); ++rowIndex) {
                            DataTableRow sourceRow = (DataTableRow)var9.next();
                            double[] targetRowValues = new double[sourceRow.getNumberOfValues()];
                            DataTableRow targetRow = new SimpleDataTableRow(targetRowValues, sourceRow.getId());

                            for(int i = 0; i < isNominalColumn.length; ++i) {
                                if (isNominalColumn[i]) {
                                    String value = sourceTable.getCell(rowIndex, i);
                                    int nominalMapIndex = targetTable.mapString(i, value);
                                    targetRowValues[i] = (double)nominalMapIndex;
                                } else {
                                    targetRowValues[i] = sourceRow.getValue(i);
                                }
                            }

                            targetTable.add(targetRow);
                        }
                    }
                }

                return;
            }
        }
    }

    @Override
    public void store(String name, IOObject object) {
        super.store(name, object);
        this.storedObjectNames.add(name);
    }

    @Override
    public IOObject retrieve(String name, boolean remove) {
        if (remove) {
            this.storedObjectNames.remove(name);
        }

        IOObject result = super.retrieve(name, remove);
        if (result == null) {
            synchronized(this.parent) {
                return this.parent.retrieve(name, remove);
            }
        } else {
            return result;
        }
    }

    @Override
    public void clearStorage() {
        super.clearStorage();
        this.storedObjectNames.clear();
    }

    @Override
    public ProcessLocation getProcessLocation() {
        return this.parent.getProcessLocation();
    }

    @Override
    public Logger getLogger() {
        return this.parent.getLogger();
    }

    @Override
    public boolean shouldPause() {
        return this.parent.shouldPause();
    }

    @Override
    public Operator getOperator(String name) {
        Operator result = super.getOperator(name);
        return result != null ? result : this.parent.getOperator(name);
    }

    @Override
    public boolean shouldStop() {
        if (this.processException != null) {
            throw new RuntimeException(this.processException);
        } else {
            return this.parent.shouldStop();
        }
    }

//    @Override
//    public RepositoryLocation getRepositoryLocation() {
//        return this.parent.getRepositoryLocation();
//    }
//
//    @Override
//    public RepositoryAccessor getRepositoryAccessor() {
//        return this.parent.getRepositoryAccessor();
//    }

    @Override
    public void addProcessStateListener(ProcessStateListener processStateListener) {
        this.parent.addProcessStateListener(processStateListener);
    }

    @Override
    public void removeProcessStateListener(ProcessStateListener processStateListener) {
        this.parent.removeProcessStateListener(processStateListener);
    }

    @Override
    public void addBreakpointListener(BreakpointListener listener) {
        this.parent.addBreakpointListener(listener);
    }

    @Override
    public void removeBreakpointListener(BreakpointListener listener) {
        this.parent.removeBreakpointListener(listener);
    }

    @Override
    public void addProcessSetupListener(ProcessSetupListener listener) {
        this.parent.addProcessSetupListener(listener);
    }

    @Override
    public void removeProcessSetupListener(ProcessSetupListener listener) {
        this.parent.removeProcessSetupListener(listener);
    }

    @Override
    public void addObserver(Observer<Process> observer, boolean onEDT) {
        this.parent.addObserver(observer, onEDT);
    }

    @Override
    public void addObserverAsFirst(Observer<Process> observer, boolean onEDT) {
        this.parent.addObserverAsFirst(observer, onEDT);
    }

    @Override
    public void removeObserver(Observer<Process> observer) {
        this.parent.removeObserver(observer);
    }
}
