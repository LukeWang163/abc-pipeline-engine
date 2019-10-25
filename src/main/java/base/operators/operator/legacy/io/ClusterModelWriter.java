package base.operators.operator.legacy.io;

import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.clustering.ClusterModelInterface;
import base.operators.operator.io.AbstractWriter;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class ClusterModelWriter
        extends AbstractWriter<ClusterModelInterface>
{
    public static final String PARAMETER_CLUSTER_MODEL_FILE = "cluster_model_file";

    public ClusterModelWriter(OperatorDescription description) { super(description, ClusterModelInterface.class); }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterTypeFile parameterTypeFile = new ParameterTypeFile("cluster_model_file", "the file to which the cluster model is stored", "clm", false);
        parameterTypeFile.setExpert(false);
        types.add(parameterTypeFile);
        return types;
    }


    @Override
    public ClusterModelInterface write(ClusterModelInterface model) throws OperatorException {
        File file = getParameterAsFile("cluster_model_file", true);
        GZIPOutputStream out = null;
        try {
            out = new GZIPOutputStream(new FileOutputStream(file));
            model.write(out);
        } catch (IOException e) {
            throw new UserError(this, e, 303, new Object[] { file, e.getMessage() });
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logError("Cannot close stream to file " + file);
                }
            }
        }
        return model;
    }
}
