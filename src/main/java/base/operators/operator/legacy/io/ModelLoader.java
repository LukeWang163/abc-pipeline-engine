package base.operators.operator.legacy.io;

import base.operators.operator.IOObject;
import base.operators.operator.Model;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractModelLoader;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import base.operators.utils.ModelHdfsSource;
import org.apache.hadoop.fs.Path;

import java.util.List;

public class ModelLoader
        extends AbstractModelLoader
{
    public static final String PARAMETER_MODEL_FILE = "model_file";
    public ModelLoader(OperatorDescription description) { super(description); }

    @Override
    public Model read() throws OperatorException {
        IOObject model;
        try {
            model = ModelHdfsSource.readFromHDFS(getParameterAsString("model_file") + Path.SEPARATOR + "data");
        } catch (Exception e) {
            throw new UserError(this, e, 302, new Object[] { getParameter("model_file"), e });
        }
        if (!(model instanceof Model)) {
            throw new UserError(this, 942, new Object[] { getParameter("model_file"), "Model", model.getClass().getSimpleName() });
        }
        return (Model)model;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("model_file", "Filename containing the model to load.", "mod", false));
        return types;
    }
}
