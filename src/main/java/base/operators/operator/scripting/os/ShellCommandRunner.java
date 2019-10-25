package base.operators.operator.scripting.os;

import java.io.IOException;
import java.util.List;

public abstract class ShellCommandRunner extends OSCommandRunner {
    public ShellCommandRunner() {
    }

    protected abstract List<String> getShellPrefix();

    protected abstract List<String> getShellCommand(String var1);

    protected String createProcessAndGetOutput(String shellCommand) throws IOException {
        List<String> command = this.getShellCommand(shellCommand);
        return this.createProcessAndGetOutput(command);
    }
}
