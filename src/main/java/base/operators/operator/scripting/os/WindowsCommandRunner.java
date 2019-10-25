package base.operators.operator.scripting.os;

import base.operators.tools.I18N;
import base.operators.tools.SystemInfoUtilities;
import base.operators.tools.SystemInfoUtilities.OperatingSystem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WindowsCommandRunner extends ShellCommandRunner {
    public WindowsCommandRunner() {
        if (SystemInfoUtilities.getOperatingSystem() != OperatingSystem.WINDOWS) {
            throw new IllegalStateException(I18N.getErrorMessage("process.error.python_scripting.windows_commands_unix", new Object[0]));
        }
    }

    @Override
    protected List<String> getShellPrefix() {
        List<String> result = new ArrayList();
        result.add("cmd");
        result.add("/C");
        return result;
    }

    @Override
    protected List<String> getShellCommand(String command) {
        List<String> result = this.getShellPrefix();
        result.add(command);
        return result;
    }

    @Override
    public String runCondaVersionCommand() throws IOException {
        return this.createProcessAndGetOutput("conda -V");
    }

    @Override
    public String runCondaEnvironmentListCommand() throws IOException {
        return this.createProcessAndGetOutput("conda info --json");
    }

    @Override
    public String runVenvwVersionCommand() throws IOException {
        return this.runVenvwEnvironmentListCommand();
    }

    @Override
    public String runVenvwEnvironmentListCommand() throws IOException {
        return this.createProcessAndGetOutput("lsvirtualenv");
    }

    @Override
    public String runPrintWorkonHomeCommand() throws IOException {
        List<String> command = new ArrayList();
        command.add("cmd");
        command.add("/U");
        command.add("/C");
        command.add("workon & set WORKON_HOME");
        Process p = this.createProcess(command);
        return this.getOutput(p, "UTF-16LE");
    }
}
