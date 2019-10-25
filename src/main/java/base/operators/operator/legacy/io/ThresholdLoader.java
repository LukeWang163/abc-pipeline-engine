package base.operators.operator.legacy.io;

import base.operators.io.process.XMLTools;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractReader;
import base.operators.operator.postprocessing.Threshold;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class ThresholdLoader
        extends AbstractReader<Threshold>
{
    public static final String PARAMETER_THRESHOLD_FILE = "threshold_file";
    public ThresholdLoader(OperatorDescription description) { super(description, Threshold.class); }

    @Override
    public Threshold read() throws OperatorException {
        File thresholdFile = getParameterAsFile("threshold_file");
        Threshold threshold = null;
        try (InputStream in = new FileInputStream(thresholdFile)) {
            Document document = XMLTools.createDocumentBuilder().parse(in);
            Element thresholdElement = document.getDocumentElement();
            if (!"threshold".equals(thresholdElement.getTagName())) {
                throw new IOException("Outer tag of threshold file must be <threshold>");
            }

            String thresholdValueString = thresholdElement.getAttribute("value");
            String thresholdFirst = thresholdElement.getAttribute("first");
            String thresholdSecond = thresholdElement.getAttribute("second");
            double thresholdValue = Double.parseDouble(thresholdValueString);
            threshold = new Threshold(thresholdValue, thresholdFirst, thresholdSecond);
        } catch (IOException|org.xml.sax.SAXException e) {
            throw new UserError(this, e, 303, new Object[] { thresholdFile, e.getMessage() });
        }
        return threshold;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("threshold_file", "Filename for the threshold file.", "thr", false));
        return types;
    }
}
