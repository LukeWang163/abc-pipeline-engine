package base.operators.operator.legacy.io;

import base.operators.RapidMiner;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractWriter;
import base.operators.operator.postprocessing.Threshold;
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

public class ThresholdWriter
        extends AbstractWriter<Threshold>
{
    public static final String PARAMETER_THRESHOLD_FILE = "threshold_file";
    public ThresholdWriter(OperatorDescription description) { super(description, Threshold.class); }

    @Override
    public Threshold write(Threshold threshold) throws OperatorException {
        File thresholdFile = getParameterAsFile("threshold_file", true);
        try(FileOutputStream fos = new FileOutputStream(thresholdFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, Encoding.getEncoding(this));
            PrintWriter out = new PrintWriter(osw)) {
            out.println("<?xml version=\"1.0\" encoding=\"" + Encoding.getEncoding(this) + "\"?>");
            out.println("<threshold version=\"" + RapidMiner.getShortVersion() + "\" value=\"" + threshold.getThreshold() + "\" first=\"" + threshold
                    .getZeroClass() + "\" second=\"" + threshold.getOneClass() + "\"/>");
            out.close();
        } catch (IOException e) {
            throw new UserError(this, e, 303, new Object[] { thresholdFile, e.getMessage() });
        }
        return threshold;
    }

    @Override
    protected boolean supportsEncoding() { return true; }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();
        types.add(new ParameterTypeFile("threshold_file", "Filename for the threshold file.", "thr", false));
        types.addAll(super.getParameterTypes());
        return types;
    }
}
