package base.operators.operator.legacy.io;

import base.operators.datatable.DataTable;
import base.operators.datatable.GnuPlotDataTableHandler;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.DummyPortPairExtender;
import base.operators.operator.ports.PortPairExtender;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import base.operators.parameter.ParameterTypeString;
import base.operators.tools.Tools;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class GNUPlotOperator
        extends Operator
{
    public static final String PARAMETER_OUTPUT_FILE = "output_file";
    public static final String PARAMETER_NAME = "name";
    public static final String PARAMETER_TITLE = "title";
    public static final String PARAMETER_X_AXIS = "x_axis";
    public static final String PARAMETER_Y_AXIS = "y_axis";
    public static final String PARAMETER_VALUES = "values";
    public static final String PARAMETER_ADDITIONAL_PARAMETERS = "additional_parameters";
    private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

    public GNUPlotOperator(OperatorDescription description) {
        super(description);
        this.dummyPorts.start();
        getTransformer().addRule(this.dummyPorts.makePassThroughRule());
    }

    @Override
    public void doWork() throws OperatorException {
        String dataTableName = getParameterAsString("name");
        if (!getProcess().dataTableExists(dataTableName)) {
            getLogger().warning("Data table with name '" + dataTableName + "' does not exist.");
            return;
        }
        DataTable dataTable = getProcess().getDataTable(dataTableName);
        String[] valueNames = getParameterAsString("values").split(" ");
        int[] values = new int[valueNames.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = dataTable.getColumnIndex(valueNames[i]);
            if (values[i] == -1) {
                getLogger().warning(getName() + ": No data column with name '" + valueNames[i] + "' exists.");

                return;
            }
        }
        String xAxisName = getParameterAsString("x_axis");
        int xAxis = dataTable.getColumnIndex(xAxisName);
        if (xAxis == -1) {
            getLogger().warning("No data column with name '" + xAxisName + "' exists.");
            return;
        }
        String yAxisName = getParameterAsString("y_axis");
        int yAxis = -1;
        if (yAxisName != null) {
            yAxis = dataTable.getColumnIndex(yAxisName);
            if (yAxis == -1) {
                getLogger().warning("No data column with name '" + yAxisName + "' exists.");

                return;
            }
        }
        String additional = "";
        if (isParameterSet("title")) {
            additional = additional + "set title \"" + getParameterAsString("title") + "\"" + Tools.getLineSeparator();
        }
        if (isParameterSet("additional_parameters")) {
            additional = additional + getParameterAsString("additional_parameters");
        }

        File file = getParameterAsFile("output_file", true);
        getLogger().info("Creating gnuplot file '" + file + "'");
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(file));
            GnuPlotDataTableHandler handler = new GnuPlotDataTableHandler(dataTable);
            handler.writeGNUPlot(out, xAxis, yAxis, values, "linespoints", additional, null);
        } catch (IOException e) {
            getLogger().warning("Cannot create output file: " + e.getMessage());
            return;
        } finally {
            if (out != null) {
                out.close();
            }
        }
        this.dummyPorts.passDataThrough();
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("output_file", "The gnuplot file.", "gnu", false));
        types.add(new ParameterTypeString("name", "The name of the process log operator which produced the data table.", false));

        types.add(new ParameterTypeString("title", "The title of the plot.", "Created by RapidMiner"));
        types.add(new ParameterTypeString("x_axis", "The values of the x-axis.", false));
        ParameterTypeString parameterTypeString = new ParameterTypeString("y_axis", "The values of the y-axis (for 3d plots).", true);
        parameterTypeString.setExpert(false);
        types.add(parameterTypeString);
        types.add(new ParameterTypeString("values", "A whitespace separated list of values which should be plotted.", false));

        types.add(new ParameterTypeString("additional_parameters", "Additional parameters for the gnuplot header.", true));

        return types;
    }
}
