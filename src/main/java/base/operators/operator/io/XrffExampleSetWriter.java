package base.operators.operator.io;

import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.parameter.ParameterType;
import base.operators.tools.Tools;
import base.operators.tools.io.Encoding;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class XrffExampleSetWriter extends AbstractStreamWriter {
    public static final String PARAMETER_EXAMPLE_SET_FILE = "example_set_file";
    public static final String PARAMETER_COMPRESS = "compress";

    public XrffExampleSetWriter(OperatorDescription description) { super(description); }

    @Override
    protected void writeStream(ExampleSet exampleSet, OutputStream outputStream) throws OperatorException {
        Charset encoding = Encoding.getEncoding(this);
        writeXrff(exampleSet, outputStream, encoding);
    }

    public static void writeXrff(ExampleSet exampleSet, OutputStream outputStream, Charset encoding) {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, encoding))) {
            out.println("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
            out.println("<dataset name=\"RapidMinerData\" version=\"3.5.4\">");
            out.println("  <header>");
            out.println("    <attributes>");

            Iterator a = exampleSet.getAttributes().allAttributeRoles();
            while (a.hasNext()) {
                AttributeRole role = (AttributeRole)a.next();

                if (role.getSpecialName() != null && role.getSpecialName().equals("weight")) {
                    continue;
                }
                Attribute attribute = role.getAttribute();
                boolean label = (role.getSpecialName() != null && role.getSpecialName().equals("label"));
                printAttribute(attribute, out, label);
            }
            out.println("    </attributes>");
            out.println("  </header>");

            out.println("  <body>");
            out.println("    <instances>");

            Attribute weightAttribute = exampleSet.getAttributes().getWeight();
            for (Example example : exampleSet) {
                String weightString = "";
                if (weightAttribute != null) {
                    weightString = " weight=\"" + example.getValue(weightAttribute) + "\"";
                }
                out.println("      <instance" + weightString + ">");
                a = exampleSet.getAttributes().allAttributeRoles();
                while (a.hasNext()) {
                    AttributeRole role = (AttributeRole)a.next();

                    if (role.getSpecialName() != null && role.getSpecialName().equals("weight")) {
                        continue;
                    }
                    Attribute attribute = role.getAttribute();
                    out.println("        <value>" + Tools.escapeXML(example.getValueAsString(attribute)) + "</value>");
                }
                out.println("      </instance>");
            }

            out.println("    </instances>");
            out.println("  </body>");
            out.println("</dataset>");
        }
    }

    private static void printAttribute(Attribute attribute, PrintWriter out, boolean isClass) {
        String classString = isClass ? "class=\"yes\" " : "";
        if (attribute.isNominal()) {
            out.println("      <attribute name=\"" + Tools.escapeXML(attribute.getName()) + "\" " + classString + "type=\"nominal\">");

            out.println("        <labels>");
            for (String s : attribute.getMapping().getValues()) {
                out.println("          <label>" + Tools.escapeXML(s) + "</label>");
            }
            out.println("        </labels>");
            out.println("      </attribute>");
        } else {
            out.println("      <attribute name=\"" + Tools.escapeXML(attribute.getName()) + "\" " + classString + "type=\"numeric\"/>");
        }
    }

    @Override
    protected boolean supportsEncoding() { return true; }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();
        ParameterType type = makeFileParameterType();
        type.setPrimary(true);
        types.add(type);
        types.addAll(super.getParameterTypes());
        return types;
    }

    @Override
    protected String[] getFileExtensions() { return new String[] { "xrff" }; }

    @Override
    protected String getFileParameterName() { return "example_set_file"; }
}
