package base.operators.operator.scripting.python;

import base.operators.tools.LogService;
import base.operators.tools.SystemInfoUtilities;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PythonProcessBuilder {
    private final String executableName;
    private final Path executablePath;
    private final ProcessBuilder processBuilder;

    public PythonProcessBuilder(String executableName, String... arguments) {
        this.executableName = executableName;
        this.executablePath = Paths.get(executableName);
        List<String> args = new ArrayList();
        args.add(this.executableName);
        args.addAll(Arrays.asList(arguments));
        this.processBuilder = new ProcessBuilder(args);
    }

    public PythonProcessBuilder(List<String> commands) {
        this((String)commands.get(0), (String[])commands.subList(1, commands.size()).toArray(new String[0]));
    }

    public Map<String, String> environment() {
        return this.processBuilder.environment();
    }

    private String getPathVariable() {
        String p = (String)this.processBuilder.environment().get("PATH");
        if (p != null) {
            return p;
        } else {
            p = System.getenv("PATH");
            return p != null ? p : "";
        }
    }

    public Process start() throws IOException {
        if (PythonSetupTester.INSTANCE.isPythonExecutable(this.executablePath)) {
            String oldPath = this.getPathVariable();
            String newPath = this.getPathPrefix() + oldPath;
            this.processBuilder.environment().put("PATH", newPath);
            LogService.getRoot().finest(String.format("Starting Python process %s using PATH=%s", this.executableName, newPath));
        } else {
            LogService.getRoot().finest(String.format("Starting process using PATH=%s (executable %s detected as no Python excutable)", this.processBuilder.environment().get("PATH"), this.executableName));
        }

        return this.processBuilder.start();
    }

    public void redirectErrorStream(boolean redirectErrorStream) {
        this.processBuilder.redirectErrorStream(redirectErrorStream);
    }

    public void redirectError(ProcessBuilder.Redirect stream) {
        this.processBuilder.redirectError(stream);
    }

    public void directory(File directory) {
        this.processBuilder.directory(directory);
    }

    private String getPathPrefix() {
        if (this.executablePath == null) {
            return "";
        } else if (!PythonSetupTester.INSTANCE.isCondaExecutable(this.executablePath)) {
            LogService.getRoot().finest(String.format("Python executable %s detected and it is not a Conda installation.", this.executablePath));
            return "";
        } else {
            LogService.getRoot().finest("Running Python executable from Conda distribution, modifying the PATH variable.");
            Path baseCondaEnvPath = PythonSetupTester.INSTANCE.getFullPathForCondaEnvironment("base");
            String prefix = this.getPathPrefix(baseCondaEnvPath);
            if (!this.executablePath.equals(baseCondaEnvPath)) {
                prefix = this.getPathPrefix(this.executablePath) + prefix;
            }

            return prefix;
        }
    }

    private Path resolvePythonBaseDir(Path path) {
        Path baseDirectory = path.getParent().toAbsolutePath();
        String deepestDir = baseDirectory.getFileName().toString();
        if ("bin".equals(deepestDir) || "Scripts".equals(deepestDir)) {
            baseDirectory = baseDirectory.getParent().toAbsolutePath();
        }

        return baseDirectory;
    }

    private String getPathPrefix(Path path) {
        if (path == null || !Files.exists(path, new java.nio.file.LinkOption[0])) {
            return "";
        }

        Path condaEnvDir = resolvePythonBaseDir(path);
        if (SystemInfoUtilities.getOperatingSystem() == SystemInfoUtilities.OperatingSystem.WINDOWS) {
            return condaEnvDir + File.pathSeparator + condaEnvDir
                    .resolve("Library").resolve("mingw-w64").resolve("bin") + File.pathSeparator + condaEnvDir
                    .resolve("Library").resolve("usr").resolve("bin") + File.pathSeparator + condaEnvDir
                    .resolve("Library").resolve("bin") + File.pathSeparator + condaEnvDir
                    .resolve("Scripts") + File.pathSeparator + condaEnvDir
                    .resolve("bin") + File.pathSeparator;
        }
        return condaEnvDir.resolve("bin") + File.pathSeparator;
    }
}