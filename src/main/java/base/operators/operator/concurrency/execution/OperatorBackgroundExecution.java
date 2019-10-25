package base.operators.operator.concurrency.execution;

import base.operators.Process;
import base.operators.operator.Operator;
import base.operators.studio.concurrency.internal.util.BackgroundExecution;
import base.operators.studio.concurrency.internal.util.ProcessBackgroundExecutionState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class OperatorBackgroundExecution implements BackgroundExecution {
    private int totalNumberOfTasks;
    private Process parentProcess;
    private String parentOperatorName;
    private List<OperatorBackgroundTask> tasks;

    public static class OperatorBackgroundTask {
        public int applyCount;
        public BackgroundExecutionProcess process;
        public Operator operator;
        private ProcessBackgroundExecutionState state;

        public OperatorBackgroundTask(int applyCount, BackgroundExecutionProcess process, Operator operator) {
            this.applyCount = applyCount;
            this.process = process;
            this.operator = operator;
            this.state = new ProcessBackgroundExecutionState(process);
        }

        public ProcessBackgroundExecutionState getState() { return this.state; }
    }

    public OperatorBackgroundExecution(Process parentProcess, String parentOperatorName) {
        this.tasks = new LinkedList();
        this.parentProcess = parentProcess;
        this.parentOperatorName = parentOperatorName;
    }

    public void addTask(int applyCount, BackgroundExecutionProcess process, Operator operator) {
        this.totalNumberOfTasks++;
        OperatorBackgroundTask application = new OperatorBackgroundTask(applyCount, process, operator);
        synchronized (this.tasks) {
            this.tasks.add(application);
        }
    }

    public void removeTask(int applyCount) {
        Iterator<OperatorBackgroundTask> iterator = this.tasks.iterator();
        while (iterator.hasNext()) {
            if (((OperatorBackgroundTask)iterator.next()).applyCount == applyCount) {
                iterator.remove();
                break;
            }
        }
    }

    public int getNumberOfTasks() { return this.tasks.size(); }

    public List<OperatorBackgroundTask> getTasks() {
        synchronized (this.tasks) {
            return new ArrayList(this.tasks);
        }
    }

    public Process getParentProcess() { return this.parentProcess; }

    public String getParentOperatorName() { return this.parentOperatorName; }

    @Override
    public String getName() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.parentOperatorName);
        buffer.append(" [");
        buffer.append(this.totalNumberOfTasks - this.tasks.size());
        buffer.append(" of ");
        buffer.append(this.totalNumberOfTasks);
        buffer.append(" Tasks]");
        return buffer.toString();
    }
}
