package base.operators.operator.scripting.python;

import base.operators.operator.scripting.python.*;
import base.operators.RapidMiner;
import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.ExampleSet;
import base.operators.operator.scripting.AbstractScriptRunner;
import base.operators.operator.scripting.InputStreamLogger;
import base.operators.operator.scripting.ScriptingCSVExampleSource;
import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.UserError;
import base.operators.operator.io.CSVExampleSetWriter;
import base.operators.operator.nio.file.BufferedFileObject;
import base.operators.operator.nio.file.FileObject;
import base.operators.operator.nio.file.SimpleFileObject;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.Ontology;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PythonScriptRunner extends AbstractScriptRunner {
    private static final String ENCODING = "# coding=utf-8\n";
    private static final String PYTHON_OBJECT_EXTENSION = "bin";
    private static final String FILE_OBJECT_INFO_EXTENSION = "foi";
    private static final String EXAMPLE_SET_EXTENSION = "csv";
    private static final String EXAMPLE_SET_META_EXTENSION = "pmd";
    private static final String WRAPPER_FILE_NAME = "wrapper.py";
    private static final String PATH_WRAPPER_SCRIPT = "/com/rapidminer/resources/python/wrapper.py";
    private PythonBinarySupplier pythonBinarySupplier;

    public PythonScriptRunner(String script, Operator operator, PythonBinarySupplier pythonBinarySupplier) {
        super(addEncoding(script), operator);
        this.pythonBinarySupplier = pythonBinarySupplier;
    }

    private static String addEncoding(String script) {
        return "# coding=utf-8\n" + script;
    }

    @Override
    public List<Class<? extends IOObject>> getSupportedTypes() {
        int INITIAL_LIST_SIZE = 1;
        List<Class<? extends IOObject>> types = new ArrayList(1);
        types.add(ExampleSet.class);
//        types.add(PythonNativeObject.class);
//        types.add(FileObject.class);
        return types;
    }

    public void serialize(IOObject object, File file) throws IOException, UserError, ProcessStoppedException {
        FileOutputStream outputStream;
        Throwable var5;
        PrintWriter printer;
        Throwable var7;
        if (object instanceof ExampleSet) {
            ExampleSet exampleSet = (ExampleSet)object;
            outputStream = new FileOutputStream(file);
            var5 = null;

            try {
                printer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
                var7 = null;

                try {
                    CSVExampleSetWriter.writeCSV(exampleSet, printer, ",", true, true, true, "inf", this.getOperator().getProgress());
                    printer.flush();
                } catch (Throwable var162) {
                    var7 = var162;
                    throw var162;
                } finally {
                    if (printer != null) {
                        if (var7 != null) {
                            try {
                                printer.close();
                            } catch (Throwable var158) {
                                var7.addSuppressed(var158);
                            }
                        } else {
                            printer.close();
                        }
                    }

                }
            } catch (Throwable var168) {
                var5 = var168;
                throw var168;
            } finally {
                if (outputStream != null) {
                    if (var5 != null) {
                        try {
                            outputStream.close();
                        } catch (Throwable var156) {
                            var5.addSuppressed(var156);
                        }
                    } else {
                        outputStream.close();
                    }
                }

            }

            this.writeMetaData(exampleSet, file);
        } else if (object instanceof PythonNativeObject) {
            PythonNativeObject pythonObject = (PythonNativeObject)object;
            ByteArrayInputStream stream = pythonObject.openStream();
            var5 = null;

            try {
                Files.copy(stream, file.toPath(), new CopyOption[0]);
            } catch (Throwable var166) {
                var5 = var166;
                throw var166;
            } finally {
                if (stream != null) {
                    if (var5 != null) {
                        try {
                            stream.close();
                        } catch (Throwable var159) {
                            var5.addSuppressed(var159);
                        }
                    } else {
                        stream.close();
                    }
                }

            }
        } else {
            if (!(object instanceof FileObject)) {
                throw new IllegalArgumentException("object type not supported");
            }

            String path;
            if (object instanceof BufferedFileObject) {
                BufferedFileObject bufferedFile = (BufferedFileObject)object;
                Path workingDirectoryPython = Paths.get(file.getParent());
                Path tempFile = Files.createTempFile(workingDirectoryPython, "rm_file_", ".dump");
                ByteArrayInputStream stream = bufferedFile.openStream();
                Throwable var8 = null;

                try {
                    Files.copy(stream, tempFile, new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                } catch (Throwable var164) {
                    var8 = var164;
                    throw var164;
                } finally {
                    if (stream != null) {
                        if (var8 != null) {
                            try {
                                stream.close();
                            } catch (Throwable var161) {
                                var8.addSuppressed(var161);
                            }
                        } else {
                            stream.close();
                        }
                    }

                }

                path = tempFile.toString();
            } else {
                FileObject foiFile = (FileObject)object;

                try {
                    path = foiFile.getFile().getAbsolutePath();
                } catch (OperatorException var165) {
                    throw new UserError(this.getOperator(), "python_scripting.serialization", new Object[]{var165.getMessage()});
                }
            }

            outputStream = new FileOutputStream(file);
            var5 = null;

            try {
                printer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
                var7 = null;

                try {
                    printer.print(path);
                    printer.flush();
                } catch (Throwable var163) {
                    var7 = var163;
                    throw var163;
                } finally {
                    if (printer != null) {
                        if (var7 != null) {
                            try {
                                printer.close();
                            } catch (Throwable var160) {
                                var7.addSuppressed(var160);
                            }
                        } else {
                            printer.close();
                        }
                    }

                }
            } catch (Throwable var172) {
                var5 = var172;
                throw var172;
            } finally {
                if (outputStream != null) {
                    if (var5 != null) {
                        try {
                            outputStream.close();
                        } catch (Throwable var157) {
                            var5.addSuppressed(var157);
                        }
                    } else {
                        outputStream.close();
                    }
                }

            }
        }

    }

    private void writeMetaData(ExampleSet exampleSet, File exampleSetFile) {
        String parent = exampleSetFile.getParent();
        String name = exampleSetFile.getName();
        String newName = name.replace(".csv", ".pmd");
        Path metaDataPath = Paths.get(parent, newName);

        try {
            OutputStream outputStream = new FileOutputStream(metaDataPath.toFile());
            Throwable var8 = null;

            try {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
                Throwable var10 = null;

                try {
                    out.print("{");
                    Iterator<Attribute> a = exampleSet.getAttributes().allAttributes();
                    boolean first = true;

                    while(a.hasNext()) {
                        if (!first) {
                            out.print(", ");
                        } else {
                            first = false;
                        }

                        Attribute attribute = (Attribute)a.next();
                        String attributeName = attribute.getName();
                        attributeName = attributeName.replaceAll("\"", "'");
                        out.print("\"" + attributeName + "\"");
                        out.print(": [");
                        out.print("\"" + Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attribute.getValueType()) + "\"");
                        out.print(", ");
                        AttributeRole role = exampleSet.getAttributes().findRoleByName(attribute.getName());
                        String roleName;
                        if (role == null) {
                            roleName = "attribute";
                        } else {
                            roleName = role.getSpecialName() == null ? "attribute" : role.getSpecialName();
                            roleName = roleName.replaceAll("\"", "'");
                        }

                        out.print("\"" + roleName + "\"");
                        out.print("]");
                    }

                    out.print("}");
                } catch (Throwable var40) {
                    var10 = var40;
                    throw var40;
                } finally {
                    if (out != null) {
                        if (var10 != null) {
                            try {
                                out.close();
                            } catch (Throwable var39) {
                                var10.addSuppressed(var39);
                            }
                        } else {
                            out.close();
                        }
                    }

                }
            } catch (Throwable var42) {
                var8 = var42;
                throw var42;
            } finally {
                if (outputStream != null) {
                    if (var8 != null) {
                        try {
                            outputStream.close();
                        } catch (Throwable var38) {
                            var8.addSuppressed(var38);
                        }
                    } else {
                        outputStream.close();
                    }
                }

            }
        } catch (IOException var44) {
            this.getLogger().warning("Failed to send meta data to Python.");
        }

    }

    private IOObject deserializeCSV(File file) throws IOException {
        try {
            ScriptingCSVExampleSource csvSource = new ScriptingCSVExampleSource();
            csvSource.setParameter("storage_type", "local");
            csvSource.setParameter("column_separators", ",");
            csvSource.setParameter("csv_file", file.getAbsolutePath());
            csvSource.setParameter("encoding", StandardCharsets.UTF_8.name());
            csvSource.setParameter("date_format", "yyyy-MM-dd HH:mm:ss");
            csvSource.setNumberFormat(new PythonDecimalFormat());
            String parent = file.getParent();
            String name = file.getName();
            String newName = name.replace(".csv", ".pmd");
            Path metaDataPath = Paths.get(parent, newName);
            csvSource.readMetadataFromFile(metaDataPath.toFile());
            return csvSource.createExampleSet();
        } catch (OperatorException var7) {
            throw new IOException("Deserialization failed", var7);
        }
    }

    private IOObject deserializePythonObject(File file) throws IOException, UserError {
        if (Files.size(file.toPath()) > 2147483639L) {
            throw new UserError(this.getOperator(), "python_scripting.deserialization.file_size");
        } else {
            return new PythonNativeObject(file);
        }
    }

    private IOObject deserializeFileObject(File file) throws UserError, IOException {
        String path = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        Path fileOfInterest = Paths.get(path);
        Path workingDirectory = file.toPath().getParent();
        if (!fileOfInterest.startsWith(workingDirectory) && fileOfInterest.getParent() != null) {
            return new SimpleFileObject(fileOfInterest.toFile());
        } else {
            if (fileOfInterest.getParent() == null) {
                fileOfInterest = Paths.get(workingDirectory.toString(), fileOfInterest.toString());
            }

            if (RapidMiner.getExecutionMode().isHeadless()) {
                if (Files.size(fileOfInterest) > 2147483639L) {
                    throw new UserError(this.getOperator(), "python_scripting.deserialization.file_size");
                } else {
                    return new BufferedFileObject(Files.readAllBytes(fileOfInterest));
                }
            } else {
                String name = fileOfInterest.getFileName().toString();
                Path destinationFile = Files.createTempFile("scripting-studio-", "-" + name);
                Files.move(fileOfInterest, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                destinationFile.toFile().deleteOnExit();
                return new SimpleFileObject(destinationFile.toFile());
            }
        }
    }

    @Override
    public IOObject deserialize(File file) throws IOException, UserError {
        String fileName = file.getName();
        String extension = "";
        int index = fileName.lastIndexOf(46);
        if (index > 0) {
            extension = fileName.substring(index + 1);
        }

        if ("csv".equals(extension)) {
            return this.deserializeCSV(file);
        } else if ("bin".equals(extension)) {
            return this.deserializePythonObject(file);
        } else if ("foi".equals(extension)) {
            return this.deserializeFileObject(file);
        } else if ("pmd".equals(extension)) {
            return null;
        } else {
            throw new IllegalArgumentException("File type not supported");
        }
    }

    @Override
    public Process start(Path directory, int numberOfOutputPorts) throws IOException, UserError {
        Path wrapperPath = Paths.get(directory.toString(), "wrapper.py");
        Path guardPath = Paths.get(directory.toString(), "guard.py");
        Files.copy(PythonScriptRunner.class.getResourceAsStream("/base/operators/resources/python/wrapper.py"), wrapperPath, new CopyOption[0]);
        Files.copy(PythonScriptRunner.class.getResourceAsStream("/base/operators/resources/python/guard.py"), guardPath, new CopyOption[0]);
        Path pythonBinaryPath;
        try {
            pythonBinaryPath = this.pythonBinarySupplier.getPythonBinary();
        } catch (UndefinedParameterError var8) {
            throw new IllegalStateException(var8);
        }

        String name;
        if (pythonBinaryPath == null) {
            name = this.pythonBinarySupplier.getPythonEnvironmentName();
            if (name == null) {
                name = "";
            }

            throw new UserError(this.getOperator(), "python_scripting.setup_test_environment.failure", new Object[]{name});
        } else {
            name = pythonBinaryPath.toString();
            PythonProcessBuilder processBuilder = new PythonProcessBuilder(name, new String[]{"-u", "wrapper.py", "" + numberOfOutputPorts});
            processBuilder.directory(directory.toFile());
            Map<String, String> env = processBuilder.environment();
            env.put("PYTHONIOENCODING", StandardCharsets.UTF_8.name());
            return this.getProcessWithLogging(processBuilder);
        }
    }

    private Process getProcessWithLogging(PythonProcessBuilder processBuilder) throws IOException {
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectError(Redirect.PIPE);
        Process process = processBuilder.start();
        InputStreamLogger.log(process.getInputStream(), this.getLogger());
        return process;
    }

    @Override
    protected String getUserscriptFilename() {
        return "userscript.py";
    }

    @Override
    protected void handleLanguageSpecificExitCode(int exitCode, String errorString) throws UserError {
        PythonExitCode code = PythonExitCode.getForCode(exitCode);
        if (code != null) {
            throw new UserError(this.getOperator(), code.getUserErrorKey(), new Object[]{errorString});
        }
    }

    @Override
    protected String getFileExtension(IOObject object) {
        if (object instanceof ExampleSet) {
            return "csv";
        } else if (object instanceof PythonNativeObject) {
            return "bin";
        } else if (object instanceof FileObject) {
            return "foi";
        } else {
            throw new IllegalArgumentException("object type not supported");
        }
    }
}
