package base.operators.operator.scripting.os;

import base.operators.tools.I18N;
import base.operators.tools.SystemInfoUtilities;
import base.operators.tools.SystemInfoUtilities.OperatingSystem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UNIXCommandRunner extends ShellCommandRunner {
    private static final String UNIQUE_STRING = "%%!!//==";
    private static final Pattern CONTENT_BETWEEN_UNIQUE_STRINGS = Pattern.compile(".*%%!!//==(.*)%%!!//==.*", 32);

    public UNIXCommandRunner() {
        if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.WINDOWS) {
            throw new IllegalStateException(I18N.getErrorMessage("process.error.python_scripting.unix_commands_windows", new Object[0]));
        }
    }

    @Override
    protected List<String> getShellPrefix() {
        List<String> result = new ArrayList();
        result.add("bash");
        result.add("-l");
        result.add("-i");
        result.add("-c");
        return result;
    }

    @Override
    protected List<String> getShellCommand(String command) {
        String fullCommand = String.format("echo '%s' ; %s ; echo '%s' ; exit", "%%!!//==", command, "%%!!//==");
        List<String> result = this.getShellPrefix();
        result.add(fullCommand);
        return result;
    }

    @Override
    protected String createProcessAndGetOutput(List<String> command) throws IOException {
        String output = super.createProcessAndGetOutput(command);
        return CONTENT_BETWEEN_UNIQUE_STRINGS.matcher(output).replaceFirst("$1").trim();
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
        return this.createProcessAndGetOutput("lsvirtualenv -h");
    }

    @Override
    public String runVenvwEnvironmentListCommand() throws IOException {
        return this.createProcessAndGetOutput("lsvirtualenv -b");
    }

    @Override
    public String runPrintWorkonHomeCommand() throws IOException {
        return this.createProcessAndGetOutput("echo $WORKON_HOME");
    }
}
