package base.operators.operator.legacy.io;

import base.operators.RapidMiner;
import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractWriter;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import base.operators.tools.io.Encoding;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


public class AttributeConstructionsWriter
        extends AbstractWriter<ExampleSet>
{
    public static final String PARAMETER_ATTRIBUTE_CONSTRUCTIONS_FILE = "attribute_constructions_file";

    public AttributeConstructionsWriter(OperatorDescription description) { super(description, ExampleSet.class); }

    @Override
    public ExampleSet write(ExampleSet eSet) throws OperatorException {
        File generatorFile = getParameterAsFile("attribute_constructions_file", true);
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(generatorFile));
            out.println("<?xml version=\"1.0\" encoding=\"" + Encoding.getEncoding(this) + "\"?>");
            out.println("<constructions version=\"" + RapidMiner.getShortVersion() + "\">");
            for (Attribute attribute : eSet.getAttributes()) {
                out.println("    <attribute name=\"" + attribute.getName() + "\" construction=\"" + attribute
                        .getConstruction() + "\"/>");
            }
            out.println("</constructions>");
        } catch (IOException e) {
            throw new UserError(this, e, '?', new Object[] { generatorFile, e.getMessage() });
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return eSet;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("attribute_constructions_file", "Filename for the attribute construction description file.", "att", false, false));

        types.addAll(Encoding.getParameterTypes(this));
        return types;
    }
}
