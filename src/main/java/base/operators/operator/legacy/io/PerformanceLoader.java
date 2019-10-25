package base.operators.operator.legacy.io;

import base.operators.operator.AbstractIOObject;
import base.operators.operator.IOObject;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractReader;
import base.operators.operator.performance.PerformanceVector;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import java.io.IOException;
import java.util.List;


public class PerformanceLoader
        extends AbstractReader<PerformanceVector>
{
    public static final String PARAMETER_PERFORMANCE_FILE = "performance_file";
    public PerformanceLoader(OperatorDescription description) { super(description, PerformanceVector.class); }

    @Override
    public PerformanceVector read() throws OperatorException {
        IOObject performance;
        getParameter("performance_file");
        AbstractIOObject.InputStreamProvider inputStreamProvider = () -> {
            try {
                return getParameterAsInputStream("performance_file");
            } catch (UserError e) {
                throw new IOException(e);
            }
        };
        try {
            performance = AbstractIOObject.read(inputStreamProvider);
        } catch (IOException e) {
            throw new UserError(this, e, '?', new Object[] { getParameter("performance_file"), e });
        }
        if (!(performance instanceof PerformanceVector)) {
            throw new UserError(this, '?', new Object[] { getParameter("performance_file"), "PerformanceVector", performance.getClass().getSimpleName() });
        }
        return (PerformanceVector)performance;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("performance_file", "Filename for the performance file.", "per", false));
        return types;
    }
}
