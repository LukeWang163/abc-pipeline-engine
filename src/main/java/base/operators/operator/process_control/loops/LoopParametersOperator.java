package base.operators.operator.process_control.loops;

import base.operators.MacroHandler;
import base.operators.datatable.DataTable;
import base.operators.datatable.SimpleDataTable;
import base.operators.datatable.SimpleDataTableRow;
import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessSetupError;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.UserError;
import base.operators.operator.meta.ParameterConfigurator;
import base.operators.operator.meta.ParameterSet;
import base.operators.operator.meta.ParameterValue;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.parameter.*;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.PortConnectedCondition;
import base.operators.parameter.value.ParameterValueGrid;
import base.operators.parameter.value.ParameterValueList;
import base.operators.parameter.value.ParameterValueRange;
import base.operators.parameter.value.ParameterValues;
import base.operators.tools.container.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoopParametersOperator
        extends AbstractLoopOperator<ParameterSet>
        implements ParameterConfigurator
{
    public static final String PARAMETER_SYNCHRONIZE = "synchronize";
    public static final String PARAMETER_ERROR_HANDLING = "error_handling";
    public static final String PARAMETER_LOG_PERFORMANCE = "log_performance";
    public static final String PARAMETER_LOG_TYPE = "log_all_criteria";
    private static final int ERROR_FAIL = 0;
    private static final String[] ERROR_HANDLING_METHOD = { "fail on error", "ignore error" };

    private static final int PARAMETER_VALUES_ARRAY_LENGTH_RANGE = 2;

    private static final int PARAMETER_VALUES_ARRAY_LENGTH_GRID = 3;

    private static final int PARAMETER_VALUES_ARRAY_LENGTH_SCALED_GRID = 4;

    private static final String PARAMETER_OPERATOR_PARAMETER_PAIR = "operator_parameter_pair";

    private static final int USER_ERROR_NO_PARAMETER_COMBINATION = 958;

    private static final int USER_ERROR_ILLEGAL_PARAMETER = 116;

    private static final int USER_ERROR_PARAMETER_KEY_SYNTAX = 907;

    private static final int USER_ERROR_UNKNOWN_OPERATOR = 109;
    private static final int USER_ERROR_UNKNOWN_PARAMETER = 906;
    private static final int USER_ERROR_WRONG_ITERATION = 926;
    protected InputPort performanceInnerSink;

    public LoopParametersOperator(OperatorDescription description) { this(description, "Subprocess"); }

    protected LoopParametersOperator(OperatorDescription description, String subprocessName) { super(description, new String[] { subprocessName }); }

    @Override
    protected void createPorts() {
        createPerformancePorts();
        createAndStartExtender();
    }

    protected void createPerformancePorts() {
        this.performanceInnerSink = (InputPort)getSubprocess(0).getInnerSinks().createPort("performance");
        this.performanceInnerSink.addPrecondition(new SimplePrecondition(this.performanceInnerSink, new MetaData(PerformanceVector.class),
                isPerformanceRequired()));
    }

    protected boolean isPerformanceRequired() { return false; }

    @Override
    public int getParameterValueMode() { return 0; }

    @Override
    protected AbstractLoopOperator.LoopArguments<ParameterSet> prepareArguments(boolean executeParallely) throws OperatorException {
        AbstractLoopOperator.LoopArguments<ParameterSet> arguments = new AbstractLoopOperator.LoopArguments<ParameterSet>();
        List<ParameterValues> parameterValues = parseParameterValues(getParameterList("parameters"));
        arguments.setDataForIteration(createParameterSetList(parameterValues, executeParallely));
        if (arguments.getDataForIteration() == null || arguments.getDataForIteration().isEmpty()) {
            throw new UserError(this, 958);
        }
        arguments.setNumberOfIterations(arguments.getDataForIteration().size());
        return arguments;
    }

    @Override
    public List<ParameterValues> parseParameterValues(List<String[]> parameterList) throws OperatorException {
        if (getProcess() == null) {
            getLogger().warning("Cannot parse parameters while operator is not attached to a process.");
            return Collections.emptyList();
        }
        List<ParameterValues> parameterValuesList = new ArrayList<ParameterValues>();
        for (String[] pair : parameterList) {
            String splitExp; Pair<Operator, ParameterType> operatorParameter = parseOperatorParameter(pair[0]);
            String parameterValuesString = pair[1];
            int startIndex = parameterValuesString.indexOf('[');
            int endIndex = parameterValuesString.indexOf(']');
            boolean rangeOrGrid = (startIndex >= 0);
            if (rangeOrGrid && endIndex < 0) {
                throw new UserError(this, 116, new Object[] { pair[0], "Unknown parameter value specification format: '" + pair[1] + "' (missing ']')." });
            }

            if (rangeOrGrid) {
                parameterValuesString = parameterValuesString.substring(startIndex + 1, endIndex).trim();
                splitExp = "[;:,]";
            } else {
                splitExp = ",";
            }
            String[] parameterValuesArray = parameterValuesString.split(splitExp);
            if (!rangeOrGrid && parameterValuesArray.length == 0) {
                continue;
            }

            int parameterValuesCount = parameterValuesArray.length;
            if (rangeOrGrid && (parameterValuesCount < 2 || parameterValuesCount > 4))
            {
                throw new UserError(this, 116, new Object[] { pair[0], "Unknown parameter value specification format: '" + pair[1] + "' (too many arguments)." });
            }

            parameterValuesList.add(createParameterValuesInstance((Operator)operatorParameter.getFirst(), (ParameterType)operatorParameter
                    .getSecond(), rangeOrGrid, parameterValuesArray));
        }
        return parameterValuesList;
    }

    private Pair<Operator, ParameterType> parseOperatorParameter(String opParString) throws UserError {
        String[] operatorParameter = ParameterTypeTupel.transformString2Tupel(opParString);
        if (operatorParameter.length != 2) {
            throw new UserError(this, 907, new Object[] { opParString });
        }
        Operator operator = lookupOperator(operatorParameter[0]);
        if (operator == null) {
            throw new UserError(this, 109, new Object[] { operatorParameter[0] });
        }
        ParameterType parameterType = operator.getParameters().getParameterType(operatorParameter[1]);
        if (parameterType == null) {
            throw new UserError(this, 906, new Object[] { operatorParameter[0] + "." + operatorParameter[1] });
        }
        return new Pair(operator, parameterType);
    }


    private ParameterValues createParameterValuesInstance(Operator operator, ParameterType parameterType, boolean rangeOrGrid, String[] parameterValuesArray) {
        if (!rangeOrGrid) {
            return new ParameterValueList(operator, parameterType, parameterValuesArray);
        }
        switch (parameterValuesArray.length) {
            case 2:
                return new ParameterValueRange(operator, parameterType, parameterValuesArray[0], parameterValuesArray[1]);
            case 3:
                return new ParameterValueGrid(operator, parameterType, parameterValuesArray[0], parameterValuesArray[1], parameterValuesArray[2]);

            case 4:
                return new ParameterValueGrid(operator, parameterType, parameterValuesArray[0], parameterValuesArray[1], parameterValuesArray[2], parameterValuesArray[3]);
        }

        return null;
    }

    private List<ParameterSet> createParameterSetList(List<ParameterValues> parameterValuesList, boolean executeParallely) throws OperatorException {
        List<ParameterValues> cleanedValuesList = new ArrayList<ParameterValues>();
        boolean synced = getParameterAsBoolean("synchronize");
        int numberOfCombinations = getCleanedValuesList(parameterValuesList, cleanedValuesList, synced);
        if (numberOfCombinations == 0 || cleanedValuesList.isEmpty()) {
            return Collections.emptyList();
        }
        String[] operatorNames = new String[cleanedValuesList.size()];
        String[] parameterNames = new String[cleanedValuesList.size()];
        String[][] valueMatrix = new String[cleanedValuesList.size()][];
        int i = 0;
        for (ParameterValues parameterValues : cleanedValuesList) {
            operatorNames[i] = parameterValues.getOperator().getName();
            parameterNames[i] = parameterValues.getParameterType().getKey();
            valueMatrix[i] = parameterValues.getValuesArray();
            i++;
        }
        if (!executeParallely)
        {
            return new ParameterSetGenerator(operatorNames, parameterNames, valueMatrix, synced, numberOfCombinations);
        }

        String[][] values = new String[numberOfCombinations][operatorNames.length];
        if (synced) {
            for (i = 0; i < operatorNames.length; i++) {
                for (int j = 0; j < numberOfCombinations; j++) {
                    values[j][i] = valueMatrix[i][j];
                }
            }
        } else {
            createAllParameterCombinations(numberOfCombinations, valueMatrix, values);
        }
        List<ParameterSet> parameterSets = new ArrayList<ParameterSet>(numberOfCombinations);
        for (String[] parameterValues : values) {
            parameterSets.add(new ParameterSet(operatorNames, parameterNames, parameterValues, null));
        }
        return parameterSets;
    }

    private int getCleanedValuesList(List<ParameterValues> parameterValuesList, List<ParameterValues> cleanedValuesList, boolean synced) throws UserError {
        if (parameterValuesList.isEmpty()) {
            return 0;
        }
        int lastSize = -1;
        long numberOfCombinations = 1L;
        for (ParameterValues parameterValues : parameterValuesList) {
            if (parameterValues instanceof ParameterValueRange) {
                getLogger().warning("Found (and deleted) parameter values range (" + parameterValues.getKey() + ") which makes no sense in " +
                        getName());
                continue;
            }
            int numberOfValues = parameterValues.getNumberOfValues();
            if (synced && lastSize > -1 && lastSize != numberOfValues) {
                throw new UserError(this, 926);
            }
            if (numberOfValues == 0) {
                throw new UserError(this, 958);
            }
            lastSize = numberOfValues;
            numberOfCombinations *= numberOfValues;
            if (numberOfCombinations > 2147483647L) {
                throw new UserError(this, "loop_parameters.too_many_combinations");
            }
            cleanedValuesList.add(parameterValues);
        }
        return synced ? lastSize : (int)numberOfCombinations;
    }

    private void createAllParameterCombinations(int numberOfCombinations, String[][] valueMatrix, String[][] values) {
        int[] indices = new int[valueMatrix.length];
        for (int i = 0; i < numberOfCombinations; i++) {
            for (int j = 0; j < indices.length; j++) {
                values[i][j] = valueMatrix[j][indices[j]];
            }
            int j = 0;
            for (indices[j] = indices[j] + 1; j < indices.length && indices[j] + 1 >= valueMatrix[j].length; ) {
                indices[j] = 0;
                j++;
            }
            if (j >= indices.length) {
                break;
            }
        }
    }

    @Override
    protected void prepareSingleRun(ParameterSet dataForIteration, AbstractLoopOperator<ParameterSet> operator) throws OperatorException { dataForIteration.applyAll(operator.getProcess(), null); }

    @Override
    protected void setMacros(AbstractLoopOperator.LoopArguments<ParameterSet> arguments, MacroHandler macroHandler, int iteration) throws OperatorException {}

    @Override
    protected List<IOObject> doIteration() throws OperatorException {
        try {
            return super.doIteration();
        } catch (OperatorException e) {
            if (getParameterAsInt("error_handling") == 0) {
                throw e;
            }
            logWarning("Error occurred during iteration and will be neglected: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    protected void processSingleRun(ParameterSet parameterSet, List<IOObject> results, boolean reuseResults, AbstractLoopOperator<ParameterSet> operator) throws OperatorException {
        LoopParametersOperator lpo = (LoopParametersOperator)operator;
        InputPort innerPerformance = lpo.performanceInnerSink;
        if (innerPerformance.isConnected() && getParameterAsBoolean("log_performance")) {
            logPerformance(parameterSet, (PerformanceVector)innerPerformance.getDataOrNull(PerformanceVector.class), operator
                    .getIterationNumber(), getParameterAsBoolean("log_all_criteria"));
        }
        super.processSingleRun(parameterSet, results, reuseResults, operator);
    }

    private void logPerformance(ParameterSet parameterSet, PerformanceVector performance, int id, boolean logAll) throws OperatorException {
        DataTable dataTable;
        if (performance == null) {
            return;
        }
        synchronized (getProcess()) {
            dataTable = getProcess().getDataTable(getName());
            if (dataTable == null) {
                dataTable = createDataTable(parameterSet, performance, logAll);
            } else if (logAll && dataTable.getNumberOfColumns() < parameterSet.size() + performance.size() + 1) {
                throw new UserError(this, "datatable.not_matching.loop_parameters", new Object[] { getName() });
            }
        }
        double[] rowValues = new double[dataTable.getNumberOfColumns()];
        rowValues[0] = (id + 1);
        int i = 1;
        for (ParameterValue paramValue : parameterSet) {
            double value; String valueString = paramValue.getParameterValue();

            try {
                value = Double.parseDouble(valueString);
            } catch (NumberFormatException nfe) {
                synchronized (dataTable) {
                    value = dataTable.mapString(i, valueString);
                }
            }
            rowValues[i++] = value;
        }
        if (logAll) {
            for (int j = 0; j < performance.size(); j++) {
                rowValues[i++] = performance.getCriterion(j).getAverage();
            }
        } else {
            rowValues[i] = performance.getMainCriterion().getAverage();
        }
        SimpleDataTableRow simpleDataTableRow = new SimpleDataTableRow(rowValues);
        synchronized (dataTable) {
            dataTable.add(simpleDataTableRow);
        }
    }

    private DataTable createDataTable(ParameterSet parameterSet, PerformanceVector performance, boolean logAll) {
        String[] columns = new String[1 + parameterSet.size() + (logAll ? performance.size() : 1)];
        columns[0] = "iteration";
        int i = 1;
        for (ParameterValue paramValue : parameterSet) {
            columns[i++] = paramValue.getOperator() + '.' + paramValue.getParameterKey();
        }
        if (logAll) {
            System.arraycopy(performance.getCriteriaNames(), 0, columns, i, performance.size());
        } else {
            columns[i] = performance.getMainCriterion().getName();
        }
        SimpleDataTable simpleDataTable = new SimpleDataTable(getName(), columns);
        getProcess().addDataTable(simpleDataTable);
        return simpleDataTable;
    }

    @Override
    protected boolean canReuseResults() { return false; }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new ArrayList<ParameterType>();
//        ParameterTypeConfiguration parameterTypeConfiguration = new ParameterTypeConfiguration(base.operators.gui.properties.ConfigureParameterOptimizationDialogCreator.class, this);
//        parameterTypeConfiguration.setExpert(false);
//        parameterTypeConfiguration.setPrimary(true);
//        types.add(parameterTypeConfiguration);
        ParameterTypeList parameterTypeList = new ParameterTypeList("parameters", "The parameters.", new ParameterTypeOperatorParameterTupel("operator_parameter_pair", "The operator and it's parameter"), new ParameterTypeParameterValue("values", "The value specifications for the parameters."));
        parameterTypeList.setHidden(true);
        types.add(parameterTypeList);
        types.add(new ParameterTypeCategory("error_handling", "This selects the method for handling errors occurring during the execution of the inner process.", ERROR_HANDLING_METHOD, 0, false));
        ParameterTypeBoolean parameterTypeBoolean = new ParameterTypeBoolean("log_performance", "Log performance for iteration if performance port is connected.", true, false);
        parameterTypeBoolean.registerDependencyCondition(new PortConnectedCondition(this, () -> this.performanceInnerSink, false, true));
        types.add(parameterTypeBoolean);
        parameterTypeBoolean = new ParameterTypeBoolean("log_all_criteria", "Log all performance criteria or just the main criterion.", false, false);
        parameterTypeBoolean.registerDependencyCondition(new BooleanParameterCondition(this, "log_performance", false, true));
        types.add(parameterTypeBoolean);
        types.add(new ParameterTypeBoolean("synchronize", "Synchronize parameter iteration", false));
        types.addAll(super.getParameterTypes());
        return types;
    }

    @Override
    public int checkProperties() {
        boolean parametersPresent = false;
        try {
            List<ParameterValues> list = parseParameterValues(getParameterList("parameters"));
            if (list != null && !list.isEmpty()) {
                parametersPresent = true;
            }
            for (ParameterValues pValues : list) {
                if (!(pValues instanceof ParameterValueGrid)) {
                    continue;
                }
                ParameterValueGrid grid = (ParameterValueGrid)pValues;
                double min = 0.0D;
                double max = 0.0D;
                try {
                    min = Double.parseDouble(grid.getMin());
                } catch (NumberFormatException numberFormatException) {}

                try {
                    max = Double.parseDouble(grid.getMax());
                } catch (NumberFormatException numberFormatException) {}


                if (!Double.isFinite(min) || !Double.isFinite(max)) {
                    addError(new SimpleProcessSetupError(ProcessSetupError.Severity.INFORMATION, getPortOwner(), "parameter_grid_non_finite", new Object[] { grid.getOperator().getName() + "." + grid.getParameterType().getKey() }));
                }
            }
        } catch (OperatorException operatorException) {}

        if (!parametersPresent) {
            addError(new SimpleProcessSetupError(ProcessSetupError.Severity.ERROR, getPortOwner(),
                    Collections.singletonList(new ParameterSettingQuickFix(this, "configure_operator")), "parameter_combination_undefined", new Object[0]));
        }

        return super.checkProperties();
    }
}

