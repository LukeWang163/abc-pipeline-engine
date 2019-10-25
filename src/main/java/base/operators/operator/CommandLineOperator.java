package base.operators.operator;

import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.nio.file.BufferedFileObject;
import base.operators.operator.nio.file.FileObject;
import base.operators.operator.ports.DummyPortPairExtender;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.Port;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeDirectory;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.conditions.PortConnectedCondition;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.nio.file.BufferedFileObject;
import base.operators.operator.ports.DummyPortPairExtender;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeDirectory;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.conditions.PortConnectedCondition;
import base.operators.tools.Tools;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;

public class CommandLineOperator extends Operator {
    public static final String PARAMETER_COMMAND = "command";
    public static final String PARAMETER_LOG_STDOUT = "log_stdout";
    public static final String PARAMETER_LOG_STDERR = "log_stderr";
    public static final String PARAMETER_WORKING_DIRECTORY = "working_directory";
    public static final String PARAMETER_ENV_VARIABLES = "env_variables";
    public static final String PARAMETER_ENV_KEY = "env_key";
    public static final String PARAMETER_ENV_VALUE = "env_value";
    private InputPort stdin = (InputPort)getInputPorts().createPort("in");
    private OutputPort stdout = (OutputPort)getOutputPorts().createPort("out");
    private OutputPort stderr = (OutputPort)getOutputPorts().createPort("err");

    private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

    public CommandLineOperator(OperatorDescription description) {
        super(description);
        this.dummyPorts.start();
        this.stdin.addPrecondition(new SimplePrecondition(this.stdin, new MetaData(FileObject.class), false));
        getTransformer().addRule(this.dummyPorts.makePassThroughRule());
        getTransformer().addGenerationRule(this.stdout, FileObject.class);
        getTransformer().addGenerationRule(this.stderr, FileObject.class);
    }

    @Override
    public void doWork() throws OperatorException {
        String command = getParameterAsString("command");
        final boolean logOut = (!this.stdout.isConnected() && getParameterAsBoolean("log_stdout"));
        final boolean logErr = (!this.stderr.isConnected() && getParameterAsBoolean("log_stderr"));
        final List exceptions = Collections.synchronizedList(new ArrayList(3));
        boolean var19 = false;
        try {
            var19 = true;
            StringTokenizer tokenizer = new StringTokenizer(command);
            List<String> tokenizedStrings = new LinkedList<>();
            while (tokenizer.hasMoreTokens()) {
                tokenizedStrings.add(tokenizer.nextToken());
            }

            ProcessBuilder processBuilder = new ProcessBuilder(tokenizedStrings);
            if (isParameterSet("working_directory")) {
                File execPath = getParameterAsFile("working_directory");
                processBuilder.directory(execPath);
            }

            List<String[]> envVariables = ParameterTypeList.transformString2List(getParameterAsString("env_variables"));
            for (String[] envVariable : envVariables) {
                processBuilder.environment().put(envVariable[0], envVariable[1]);
            }

            final Process process = processBuilder.start();
            final ByteArrayOutputStream stdOutBuf = new ByteArrayOutputStream();
            final ByteArrayOutputStream stdErrBuf = new ByteArrayOutputStream();

            if (this.stdin.isConnected()) {
                FileObject input = (FileObject)this.stdin.getData(FileObject.class);
                (new Thread(() -> {
                    try {
                        InputStream inputStream = input.openStream();
                        Throwable var4 = null;

                        try {
                            OutputStream outputStream = process.getOutputStream();
                            Throwable var6 = null;

                            try {
                                Tools.copyStreamSynchronously(inputStream, outputStream, true);
                            } catch (Throwable var31) {
                                var6 = var31;
                                throw var31;
                            } finally {
                                if (outputStream != null) {
                                    if (var6 != null) {
                                        try {
                                            outputStream.close();
                                        } catch (Throwable var30) {
                                            var6.addSuppressed(var30);
                                        }
                                    } else {
                                        outputStream.close();
                                    }
                                }

                            }
                        } catch (Throwable var33) {
                            var4 = var33;
                            throw var33;
                        } finally {
                            if (inputStream != null) {
                                if (var4 != null) {
                                    try {
                                        inputStream.close();
                                    } catch (Throwable var29) {
                                        var4.addSuppressed(var29);
                                    }
                                } else {
                                    inputStream.close();
                                }
                            }

                        }
                    } catch (Exception var35) {
                        exceptions.add(var35);
                    }

                }, this.getName() + "-stdin")).start();
            }

            Thread stdoutThread = new Thread(getName() + "-stdout")
            {
                @Override
                public void run() {
                    try {
                        if (logOut) {
                            CommandLineOperator.this.logOutput("stdout: ", process.getInputStream());
                        } else {
                            Tools.copyStreamSynchronously(process.getInputStream(), stdOutBuf, true);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
            };
            stdoutThread.setDaemon(true);
            stdoutThread.start();
            Thread stderrThread = new Thread(getName() + "-stderr")
            {
                @Override
                public void run() {
                    try {
                        if (logErr) {
                            CommandLineOperator.this.logOutput("stderr: ", process.getErrorStream());
                        } else {
                            Tools.copyStreamSynchronously(process.getErrorStream(), stdErrBuf, true);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
            };
            stderrThread.setDaemon(true);
            stderrThread.start();

            Tools.waitForDependentProcess(this, process, command, new Thread[] { stdoutThread, stderrThread });
            getLogger().info("Program exited succesfully.");

            if (this.stdout.isConnected()) {
                this.stdout.deliver(new BufferedFileObject(stdOutBuf.toByteArray()));
                var19 = false;
            }
            if (this.stderr.isConnected()) {
                this.stderr.deliver(new BufferedFileObject(stdErrBuf.toByteArray()));
                var19 = false;
            }
        } catch (IOException e) {
            throw new UserError(this, e, 310, new Object[] { command, e.getMessage() });
        } finally {
            if (var19) {
                this.getLogger().log(Level.WARNING, "base.operators.operator.CommandLineOperator.errors_occurred", new Object[]{exceptions.size(), command});
                Iterator var15 = exceptions.iterator();

                while(var15.hasNext()) {
                    Throwable t = (Throwable)var15.next();
                    this.getLogger().log(Level.WARNING, t.toString(), t);
                }

                if (!exceptions.isEmpty()) {
                    Throwable t = (Throwable)exceptions.get(0);
                    if (t instanceof OperatorException) {
                        throw (OperatorException)t;
                    }

                    throw new UserError(this, t, 310, new Object[]{command, t.getMessage()});
                }

            }
        }

        this.getLogger().log(Level.WARNING, "base.operators.operator.CommandLineOperator.errors_occurred", new Object[]{exceptions.size(), command});
        Iterator var22 = exceptions.iterator();

        while(var22.hasNext()) {
            Throwable t = (Throwable)var22.next();
            this.getLogger().log(Level.WARNING, t.toString(), t);
        }

        if (!exceptions.isEmpty()) {
            Throwable t = (Throwable)exceptions.get(0);
            if (t instanceof OperatorException) {
                throw (OperatorException)t;
            } else {
                throw new UserError(this, t, 310, new Object[]{command, t.getMessage()});
            }
        } else {
            this.dummyPorts.passDataThrough();
        }
    }

    private void logOutput(String message, InputStream in) throws IOException {
        BufferedReader bin = new BufferedReader(new InputStreamReader(in));
        String line = null;
        while ((line = bin.readLine()) != null) {
            logNote(message + line);
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeString("command", "Command to execute.", false, false));
        ParameterTypeBoolean t = new ParameterTypeBoolean("log_stdout", "If set to true, the stdout stream of the command is redirected to the logfile.", true);
        t.registerDependencyCondition(new PortConnectedCondition(this, () -> this.stdout, false, false));
        types.add(t);
        ParameterTypeBoolean e = new ParameterTypeBoolean("log_stderr", "If set to true, the stderr stream of the command is redirected to the logfile.", true);
        e.registerDependencyCondition(new PortConnectedCondition(this, () -> this.stderr, false, false));
        types.add(e);
        types.add(new ParameterTypeDirectory("working_directory", "Working directory of the command.", true));
        types.add(new ParameterTypeList("env_variables", "Environment variables used for executing the command.", new ParameterTypeString("env_key", "Environment variable key."), new ParameterTypeString("env_value", "Environment variable value")));
        return types;
    }
}
