package base.operators.operator.legacy.io;

import base.operators.operator.Model;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractWriter;
import base.operators.operator.io.OutputTypes;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class ModelWriter
        extends AbstractWriter<Model>
{
    public static final String PARAMETER_MODEL_FILE = "model_file";
    public static final String PARAMETER_OVERWRITE_EXISTING_FILE = "overwrite_existing_file";
    public static final String PARAMETER_OUTPUT_TYPE = "output_type";

    public ModelWriter(OperatorDescription description) { super(description, Model.class); }


    @Override
    public Model write(Model model) throws OperatorException {
        File modelFile = getParameterAsFile("model_file", true);

        if (!getParameterAsBoolean("overwrite_existing_file") && modelFile.exists()) {
            File newFile = null;
            String fileName = modelFile.getAbsolutePath();
            int counter = 1;

            while (true) {
                String[] extension = fileName.split("\\.");
                extension[extension.length - 2] = extension[extension.length - 2] + "_" + counter + ".";
                String newFileName = stringArrayToString(extension);
                newFile = new File(newFileName);
                if (!newFile.exists()) {
                    break;
                }
                counter++;
            }
            modelFile = newFile;
        }

        int outputType = getParameterAsInt("output_type");
        switch(outputType) {
            case 0:
                FileOutputStream out = null;

                try {
                    out = new FileOutputStream(modelFile);
                    model.write(out);
                    break;
                } catch (IOException var52) {
                    throw new UserError(this, var52, 303, new Object[]{modelFile, var52.getMessage()});
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException var48) {
                            this.logError("Cannot close stream to file " + modelFile);
                        }
                    }
                }
            case 1:
                GZIPOutputStream gzipOutputStream = null;

                try {
                    gzipOutputStream = new GZIPOutputStream(new FileOutputStream(modelFile));
                    model.write(gzipOutputStream);
                    break;
                } catch (IOException var51) {
                    throw new UserError(this, var51, 303, new Object[]{modelFile, var51.getMessage()});
                } finally {
                    if (gzipOutputStream != null) {
                        try {
                            gzipOutputStream.close();
                        } catch (IOException var47) {
                            this.logError("Cannot close stream to file " + modelFile);
                        }
                    }
                }
            case 2:
                ObjectOutputStream objectOut = null;

                try {
                    objectOut = new ObjectOutputStream(new FileOutputStream(modelFile));
                    objectOut.writeObject(model);
                } catch (IOException var50) {
                    throw new UserError(this, var50, 303, new Object[]{modelFile, var50.getMessage()});
                } finally {
                    if (objectOut != null) {
                        try {
                            objectOut.close();
                        } catch (IOException var49) {
                            this.logError("Cannot close stream to file " + modelFile);
                        }
                    }
                }
        }
        return model;
    }

    private String stringArrayToString(String[] filenameParts) {
        StringBuffer newString = new StringBuffer();
        for (int i = 0; i < filenameParts.length; i++) {
            newString.append(filenameParts[i]);
        }
        return newString.toString();
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("model_file", "Filename for the model file.", "mod", false));
        types.add(new ParameterTypeBoolean("overwrite_existing_file", "Overwrite an existing file. If set to false then an index is appended to the filename.", true));
        types.add(new ParameterTypeCategory("output_type", "Indicates the type of the output", OutputTypes.OUTPUT_TYPES, 1));
        return types;
    }
}
