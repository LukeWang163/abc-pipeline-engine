package base.operators.operator.io;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDate;
import base.operators.tools.io.Encoding;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ArffExampleSetWriter extends AbstractStreamWriter {
    public static final String PARAMETER_EXAMPLE_SET_FILE = "example_set_file";

    public ArffExampleSetWriter(OperatorDescription description) { super(description); }

    @Override
    protected void writeStream(ExampleSet exampleSet, OutputStream outputStream) throws OperatorException {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, Encoding.getEncoding(this)))) {
            writeArff(exampleSet, out);
            out.flush();
        }
    }

    public static void writeArff(ExampleSet exampleSet, PrintWriter out, String linefeed) {
        if (linefeed == null) {
            linefeed = System.getProperty("line.separator");
        }

        out.print("@RELATION RapidMinerData" + linefeed);
        out.print(linefeed);

        Iterator<Attribute> a = exampleSet.getAttributes().allAttributes();
        while (a.hasNext()) {
            printAttributeData((Attribute)a.next(), out);
        }

        out.print(linefeed);
        out.print("@DATA" + linefeed);

        for (Example example : exampleSet) {
            boolean first = true;
            a = exampleSet.getAttributes().allAttributes();
            while (a.hasNext()) {
                Attribute current = (Attribute)a.next();
                if (!first) {
                    out.print(",");
                }

                if (current.isNominal()) {
                    double value = example.getValue(current);
                    if (Double.isNaN(value)) {
                        out.print("?");
                    } else {
                        out.print("'" + example.getValueAsString(current) + "'");
                    }
                } else if (current.isDateTime()) {
                    Date dateValue = example.getDateValue(current);
                    out.print("\"" + ((SimpleDateFormat)ParameterTypeDate.DATE_FORMAT.get()).format(dateValue) + "\"");
                } else {
                    out.print(example.getValueAsString(current));
                }
                first = false;
            }
            out.print(linefeed);
        }
    }

    public static void writeArff(ExampleSet exampleSet, PrintWriter out) { writeArff(exampleSet, out, null); }

    private static void printAttributeData(Attribute attribute, PrintWriter out) {
        out.print("@ATTRIBUTE '" + attribute.getName() + "' ");
        if (attribute.isNominal()) {
            StringBuilder nominalValues = new StringBuilder("{");
            boolean first = true;
            for (String s : attribute.getMapping().getValues()) {
                if (!first) {
                    nominalValues.append(",");
                }
                nominalValues.append("'").append(s).append("'");
                first = false;
            }
            nominalValues.append("}");
            out.print(nominalValues.toString());
        } else if (attribute.isDateTime()) {
            out.print("DATE \"" + ((SimpleDateFormat)ParameterTypeDate.DATE_FORMAT.get()).toPattern() + "\"");
        } else {
            out.print("real");
        }
        out.println();
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
    protected String[] getFileExtensions() { return new String[] { "arff" }; }

    @Override
    protected String getFileParameterName() { return "example_set_file"; }
}
