package base.operators.operator.legacy.io;

import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractReader;
import base.operators.operator.meta.ParameterSet;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ParameterSetLoader
        extends AbstractReader<ParameterSet>
{
    public static final String PARAMETER_PARAMETER_FILE = "parameter_file";

    public ParameterSetLoader(OperatorDescription description) { super(description, ParameterSet.class); }

    @Override
    public ParameterSet read() throws OperatorException {
        ParameterSet parameterSet = null;
        File parameterFile = getParameterAsFile("parameter_file");
        try (InputStream in = new FileInputStream(parameterFile)) {
            parameterSet = ParameterSet.readParameterSet(in);
        } catch (IOException e) {
            throw new UserError(this, 302, new Object[] { e, parameterFile, e.getMessage() });
        }

        return parameterSet;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("parameter_file", "A file containing a parameter set.", "par", false));
        return types;
    }
}
