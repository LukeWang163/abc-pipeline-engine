package base.operators.operator.scripting;

import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.UserError;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public abstract class AbstractScriptRunner implements ScriptRunner {
    private static final String INPUT_FILE_PREFIX = "rapidminer_input";
    private static final String OUTPUT_FILE_PATTERN = "rapidminer_output[0-9]{3}\\..*";
    private static final String ERROR_FILE_NAME = "rapidminer_error.log";
    private Logger logger;
    private final String script;
    private Process process;
    private final Operator operator;

    public AbstractScriptRunner(String script, Operator operator) {
        this.script = script;
        this.operator = operator;
    }

    protected abstract void serialize(IOObject var1, File var2) throws IOException, UserError, ProcessStoppedException;

    protected abstract IOObject deserialize(File var1) throws IOException, UserError;

    protected abstract Process start(Path var1, int var2) throws IOException, UserError;

    public void registerLogger(Logger logger) {
        this.logger = logger;
    }

    protected Logger getLogger() {
        return this.logger;
    }

    protected Operator getOperator() {
        return this.operator;
    }

    @Override
    public List<IOObject> run(List<IOObject> inputs, int numberOfOutputPorts) throws IOException, OperatorException {
        Path tempFolder = null;

        List var11;
        try {
            tempFolder = Files.createTempDirectory("scripting");
            this.serializeInputs(inputs, tempFolder);
            this.generateScriptFile(tempFolder);
            this.process = this.start(tempFolder, numberOfOutputPorts);

            try {
                int exitCode = this.process.waitFor();
                if (exitCode != 0) {
                    String errorString = this.getError(tempFolder);
                    this.handleLanguageSpecificExitCode(exitCode, errorString);
                    if (errorString.isEmpty()) {
                        throw new OperatorException(OperatorException.getErrorMessage("python_scripting.script_failed", new Object[0]));
                    }

                    throw new OperatorException(OperatorException.getErrorMessage("python_scripting.script_failed_message", new Object[]{errorString}));
                }
            } catch (InterruptedException var9) {
                this.cancel();
                throw new CancellationException();
            }

            var11 = this.deserializeResults(tempFolder);
        } finally {
            this.deleteTempFolder(tempFolder);
        }

        return var11;
    }

    protected String getError(Path tempFolder) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(tempFolder.toString(), "rapidminer_error.log"));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException var3) {
            return "";
        }
    }

    protected abstract void handleLanguageSpecificExitCode(int var1, String var2) throws UserError;

    public void cancel() {
        if (this.process != null) {
            this.process.destroy();

            try {
                this.process.waitFor();
            } catch (InterruptedException var2) {
            }
        }

    }

    private void serializeInputs(List<IOObject> inputs, Path tempFolder) throws IOException, UserError, ProcessStoppedException {
        int index = 0;

        for(Iterator var4 = inputs.iterator(); var4.hasNext(); ++index) {
            IOObject input = (IOObject)var4.next();
            Path tempPath = Paths.get(tempFolder.toString(), "rapidminer_input" + String.format("%03d", index) + "." + this.getFileExtension(input));
            File tempFile = tempPath.toFile();
            this.serialize(input, tempFile);
        }

    }

    private List<IOObject> deserializeResults(Path tempFolder) throws IOException, UserError {
        List<Path> outputFiles = new LinkedList();
        Pattern pattern = Pattern.compile("rapidminer_output[0-9]{3}\\..*");
        DirectoryStream<Path> stream = Files.newDirectoryStream(tempFolder);
        Throwable var5 = null;

        Iterator var6;
        Path outputFile;
        try {
            var6 = stream.iterator();

            while(var6.hasNext()) {
                outputFile = (Path)var6.next();
                if (pattern.matcher(outputFile.getFileName().toString()).matches()) {
                    outputFiles.add(outputFile);
                }
            }
        } catch (Throwable var15) {
            var5 = var15;
            throw var15;
        } finally {
            if (stream != null) {
                if (var5 != null) {
                    try {
                        stream.close();
                    } catch (Throwable var14) {
                        var5.addSuppressed(var14);
                    }
                } else {
                    stream.close();
                }
            }

        }

        Comparator<Path> comparator = (o1, o2) -> {
            String name1 = o1.getFileName().toString();
            String name2 = o2.getFileName().toString();
            return name1.compareTo(name2);
        };
        outputFiles.sort(comparator);
        List<IOObject> outputs = new LinkedList();
        var6 = outputFiles.iterator();

        while(var6.hasNext()) {
            outputFile = (Path)var6.next();
            IOObject object = this.deserialize(outputFile.toFile());
            if (object != null) {
                outputs.add(object);
            }
        }

        return outputs;
    }

    protected abstract String getFileExtension(IOObject var1);

    private void deleteEntry(Path entry) {
        if (entry.toFile().isDirectory()) {
            this.deleteTempFolder(entry);
        } else {
            try {
                Files.delete(entry);
            } catch (IOException | SecurityException var3) {
                this.logger.warning("Failed to delete temp file " + entry.toAbsolutePath());
            }
        }

    }

    private void deleteTempFolder(Path tempFolder) {
        if (tempFolder != null) {
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(tempFolder);
                Throwable var3 = null;

                try {
                    Iterator var4 = stream.iterator();

                    while(var4.hasNext()) {
                        Path entry = (Path)var4.next();
                        this.deleteEntry(entry);
                    }
                } catch (Throwable var16) {
                    var3 = var16;
                    throw var16;
                } finally {
                    if (stream != null) {
                        if (var3 != null) {
                            try {
                                stream.close();
                            } catch (Throwable var15) {
                                var3.addSuppressed(var15);
                            }
                        } else {
                            stream.close();
                        }
                    }

                }
            } catch (IOException var18) {
                this.logger.warning("Failed to delete temp files");
            }

            try {
                Files.delete(tempFolder);
            } catch (IOException | SecurityException var14) {
                this.logger.warning("Failed to delete temp folder " + tempFolder.toAbsolutePath());
            }
        }

    }

    private void generateScriptFile(Path tempFolder) throws IOException {
        Path tempPath = Paths.get(tempFolder.toString(), this.getUserscriptFilename());
        Files.write(tempPath, this.script.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
    }

    protected abstract String getUserscriptFilename();

    /** @deprecated */
    @Deprecated
    protected Process getProcessWithLogging(ProcessBuilder processBuilder) throws IOException {
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectError(Redirect.PIPE);
        Process process = processBuilder.start();
        InputStreamLogger.log(process.getInputStream(), this.logger);
        return process;
    }
}
