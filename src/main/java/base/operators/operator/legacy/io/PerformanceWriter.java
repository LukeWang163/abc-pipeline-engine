package base.operators.operator.legacy.io;

import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractWriter;
import base.operators.operator.performance.PerformanceVector;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PerformanceWriter extends AbstractWriter<PerformanceVector> {
    public static final String PARAMETER_PERFORMANCE_FILE = "performance_file";
    public PerformanceWriter(OperatorDescription description) { super(description, PerformanceVector.class); }

    @Override
    public PerformanceVector write(PerformanceVector performance) throws OperatorException {
        File performanceFile = getParameterAsFile("performance_file", true);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(performanceFile);
            performance.write(out);
        } catch (IOException e) {
            throw new UserError(this, e, 303, new Object[] { performanceFile, e.getMessage() });
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logError("Cannot close stream to file " + performanceFile);
                }
            }
        }
        return performance;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("performance_file", "Filename for the performance file.", "per", false));
        return types;
    }
}
