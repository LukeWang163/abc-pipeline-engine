package base.operators.operator.scripting.os;

import base.operators.Process;
import base.operators.operator.scripting.PluginInitPythonScripting;
import base.operators.operator.scripting.python.PythonProcessBuilder;
import base.operators.tools.LogService;
import base.operators.tools.Tools;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public abstract class OSCommandRunner {
    public OSCommandRunner() {
    }

    public abstract String runCondaVersionCommand() throws IOException;

    public abstract String runCondaEnvironmentListCommand() throws IOException;

    public abstract String runVenvwVersionCommand() throws IOException;

    public abstract String runVenvwEnvironmentListCommand() throws IOException;

    public abstract String runPrintWorkonHomeCommand() throws IOException;

    public static String readStream(InputStream stream) {
        return readStream(stream, (String)null);
    }

    protected static String readStream(InputStream stream, String encoding) {
        StringBuilder result = new StringBuilder();

        try {
            BufferedReader s;
            if (encoding != null) {
                s = new BufferedReader(new InputStreamReader(stream, encoding));
            } else {
                s = new BufferedReader(new InputStreamReader(stream, Process.getEncoding((String)null)));
            }

            String line;
            try {
                while((line = s.readLine()) != null) {
                    result.append(line);
                    result.append("\n");
                }
            } finally {
                s.close();
            }
        } catch (IOException var9) {
        }

        return result.toString();
    }

    protected String getOutput(java.lang.Process p) {
        return this.getOutput(p, (String)null);
    }

    protected String getOutput(java.lang.Process p, String encoding) {
        String output = readStream(p.getInputStream(), encoding);
        String error = readStream(p.getErrorStream(), encoding);

        try {
            p.waitFor();
        } catch (InterruptedException var6) {
            LogService.getRoot().warning("Error during executing process: " + var6.getMessage());
            Thread.currentThread().interrupt();
            return "";
        }

        if (output.length() > 0) {
            LogService.getRoot().finer(String.format("The output stream during operating system process execution was: '%s'", output.replaceAll("\\n", " ")));
        }

        if (error.length() > 0) {
            LogService.getRoot().finest(String.format("The error stream during executing operating system execution process was: '%s' Note that a non empty error stream may indicate proper behaviour.", error.replaceAll("\\n", " ")));
        }

        return output;
    }

    protected java.lang.Process createProcess(List<String> command) throws IOException {
        PythonProcessBuilder p = new PythonProcessBuilder(command);
        if (PluginInitPythonScripting.getCurrentSearchPath().length() > 0) {
            p.environment().put("PATH", Tools.unescape(PluginInitPythonScripting.getCurrentSearchPath().replaceAll(",", File.pathSeparator)) + File.pathSeparator + System.getenv("PATH"));
        } else {
            p.environment().put("PATH", System.getenv("PATH"));
        }

        LogService.getRoot().finest(String.format("Running external process: '%s'.", command.stream().reduce("", (s1, s2) -> {
            return s1 + " " + s2;
        })));
        return p.start();
    }

    protected String createProcessAndGetOutput(List<String> command) throws IOException {
        java.lang.Process p = this.createProcess(command);
        return this.getOutput(p);
    }
}
