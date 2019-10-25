package base.operators.operator.legacy.io;

import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.OutputTypes;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.PassThroughRule;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class IOObjectWriter
        extends Operator
{
    private InputPort objectInput = getInputPorts().createPort("object", IOObject.class);
    private OutputPort objectOutput = (OutputPort)getOutputPorts().createPort("object");

    public static final String PARAMETER_OBJECT_FILE = "object_file";

    public static final String PARAMETER_OUTPUT_TYPE = "output_type";

    public static final String PARAMETER_CONTINUE_ON_ERROR = "continue_on_error";

    public IOObjectWriter(OperatorDescription description) {
        super(description);
        getTransformer().addRule(new PassThroughRule(this.objectInput, this.objectOutput, false));
    }

    @Override
    public void doWork() throws OperatorException {
        IOObject object = this.objectInput.getData(IOObject.class);
        File objectFile = this.getParameterAsFile("object_file", true);
        int outputType = this.getParameterAsInt("output_type");
        switch(outputType) {
            case 0:
                FileOutputStream out = null;
                try {
                    try {
                        out = new FileOutputStream(objectFile);
                        object.write(out);
                    } catch (IOException var58) {
                        if (!this.getParameterAsBoolean("continue_on_error")) {
                            throw new UserError(this, var58, 303, new Object[]{objectFile, var58.getMessage()});
                        }
                        this.logError("Could not write IO Object to file " + objectFile + ": " + var58.getMessage());
                    }
                    break;
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException var53) {
                            this.logError("Cannot close stream to file " + objectFile);
                        }
                    }
                }
            case 1:
                GZIPOutputStream gzipOutputStream = null;

                try {
                    try {
                        gzipOutputStream = new GZIPOutputStream(new FileOutputStream(objectFile));
                        object.write(gzipOutputStream);
                    } catch (IOException var54) {
                        if (!this.getParameterAsBoolean("continue_on_error")) {
                            throw new UserError(this, var54, 303, new Object[]{objectFile, var54.getMessage()});
                        }

                        this.logError("Could not write IO Object to file " + objectFile + ": " + var54.getMessage());
                    }
                    break;
                } finally {
                    if (gzipOutputStream != null) {
                        try {
                            gzipOutputStream.close();
                        } catch (IOException var52) {
                            this.logError("Cannot close stream to file " + objectFile);
                        }
                    }

                }
            case 2:
                ObjectOutputStream objectOut = null;
                try {
                    objectOut = new ObjectOutputStream(new FileOutputStream(objectFile));
                    objectOut.writeObject(object);
                } catch (IOException var56) {
                    if (!this.getParameterAsBoolean("continue_on_error")) {
                        throw new UserError(this, var56, 303, new Object[]{objectFile, var56.getMessage()});
                    }
                    this.logError("Could not write IO Object to file " + objectFile + ": " + var56.getMessage());
                } finally {
                    if (objectOut != null) {
                        try {
                            objectOut.close();
                        } catch (IOException var51) {
                            this.logError("Cannot close stream to file " + objectFile);
                        }
                    }

                }
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("object_file", "Filename of the object file.", "ioo", false));
        types.add(new ParameterTypeCategory("output_type", "Indicates the type of the output", OutputTypes.OUTPUT_TYPES, 1));

        types.add(new ParameterTypeBoolean("continue_on_error", "Defines behavior on errors", false));
        return types;
    }
}
