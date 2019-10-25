package base.operators.operator.legacy.io;

import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractWriter;
import base.operators.operator.meta.ParameterSet;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import base.operators.tools.io.Encoding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;


public class ParameterSetWriter
        extends AbstractWriter<ParameterSet>
{
    public static final String PARAMETER_PARAMETER_FILE = "parameter_file";
    public ParameterSetWriter(OperatorDescription description) { super(description, ParameterSet.class); }

    @Override
    public ParameterSet write(ParameterSet parameterSet) throws OperatorException {
        File parameterFile = getParameterAsFile("parameter_file", true);
        try(FileOutputStream fos = new FileOutputStream(parameterFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, Encoding.getEncoding(this));
            PrintWriter out = new PrintWriter(osw)) {
            parameterSet.writeParameterSet(out, Encoding.getEncoding(this));
        } catch (IOException e) {
            throw new UserError(this, 303, new Object[]{e, parameterFile, e.getMessage()});
        }
        return parameterSet;
    }

    @Override
    protected boolean supportsEncoding() { return true; }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();
        types.add(new ParameterTypeFile("parameter_file", "A file containing a parameter set.", "par", false));
        types.addAll(super.getParameterTypes());
        return types;
    }
}
