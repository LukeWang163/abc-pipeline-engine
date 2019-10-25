package base.operators.operator.scripting.metadata;

import base.operators.Process;
import base.operators.ProcessStateListener;
import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SimpleMetaDataError;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class MetaDataCachingRule implements MDTransformationRule {
    private final Operator operator;
    private List<MetaData> cachedMetaData;
    private Set<Integer> noDataCache;
    private boolean cacherAdded;
    private boolean cachedInProcess;
    private boolean operatorWorked;

    public MetaDataCachingRule(Operator operator) {
        this.operator = operator;
    }

    public void setOperatorWorked() {
        this.operatorWorked = true;
    }

    @Override
    public void transformMD() {
        if (!this.cacherAdded && this.operator.getProcess() != null) {
            this.addCacher();
            this.cacherAdded = true;
        }

        if (this.cachedMetaData == null) {
            this.operator.addError(new SimpleProcessSetupError(Severity.INFORMATION, this.operator.getPortOwner(), "python_scripting.metadata.cache_empty", new Object[0]));
        } else {
            List<OutputPort> outputPorts = this.operator.getOutputPorts().getAllPorts();
            int index = 0;

            for(Iterator var3 = outputPorts.iterator(); var3.hasNext(); ++index) {
                OutputPort port = (OutputPort)var3.next();
                this.tryToDeliverMetaData(index, port);
            }
        }

    }

    private void tryToDeliverMetaData(int index, OutputPort port) {
        if (port.isConnected()) {
            if (index < this.cachedMetaData.size()) {
                MetaData md = (MetaData)this.cachedMetaData.get(index);
                if (md != null) {
                    port.deliverMD(md);
                } else if (this.noDataCache != null && this.noDataCache.contains(index)) {
                    this.operator.addError(new SimpleMetaDataError(Severity.WARNING, port, "python_scripting.no_data", new Object[0]));
                } else {
                    this.operator.addError(new SimpleMetaDataError(Severity.INFORMATION, port, "python_scripting.cache_empty", new Object[0]));
                }
            } else {
                this.operator.addError(new SimpleMetaDataError(Severity.INFORMATION, port, "python_scripting.cache_empty", new Object[0]));
            }
        }

    }

    private void addCacher() {
        this.operator.getProcess().addProcessStateListener(new ProcessStateListener() {
            @Override
            public void stopped(Process process) {
                if (!MetaDataCachingRule.this.cachedInProcess) {
                    MetaDataCachingRule.this.cacheMetaData();
                }

            }

            @Override
            public void started(Process process) {
                MetaDataCachingRule.this.cachedInProcess = false;
                MetaDataCachingRule.this.operatorWorked = false;
            }

            @Override
            public void resumed(Process process) {
            }

            @Override
            public void paused(Process process) {
                if (MetaDataCachingRule.this.operatorWorked && !MetaDataCachingRule.this.cachedInProcess) {
                    MetaDataCachingRule.this.cacheMetaData();
                    MetaDataCachingRule.this.cachedInProcess = true;
                }

            }
        });
    }

    private void cacheMetaData() {
        this.cachedMetaData = new ArrayList();
        List<OutputPort> outputPorts = this.operator.getOutputPorts().getAllPorts();
        this.noDataCache = new HashSet(outputPorts.size());
        int index = 0;

        for(Iterator var3 = outputPorts.iterator(); var3.hasNext(); ++index) {
            OutputPort port = (OutputPort)var3.next();
            IOObject object = port.getAnyDataOrNull();
            if (object != null) {
                this.cachedMetaData.add(MetaData.forIOObject(object));
            } else {
                if (port.isConnected()) {
                    this.noDataCache.add(index);
                }

                this.cachedMetaData.add(null);
            }
        }

//        RapidMinerGUI.getMainFrame().validateProcess(true);
    }
}
