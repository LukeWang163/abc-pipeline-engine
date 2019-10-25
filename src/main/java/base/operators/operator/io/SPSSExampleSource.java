package base.operators.operator.io;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRowFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.OperatorDescription;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.RandomGenerator;
import base.operators.tools.Tools;
import base.operators.tools.att.AttributeSet;
import base.operators.tools.math.sampling.OrderedSamplingWithoutReplacement;
import base.operators.tools.parameter.internal.DataManagementParameterHelper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class SPSSExampleSource extends BytewiseExampleSource{
    public static final String PARAMETER_ATTRIBUTE_NAMING_MODE = "attribute_naming_mode";
    public static final String PARAMETER_USE_VALUE_LABELS = "use_value_labels";
    public static final String PARAMETER_RECODE_USER_MISSINGS = "recode_user_missings";
    public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";
    public static final String PARAMETER_SAMPLE_SIZE = "sample_size";
    private static final String SPSS_FILE_SUFFIX = "sav";
    public static final int USE_VAR_NAME = 0;
    public static final int USE_VAR_LABEL = 1;

    static  {
        AbstractReader.registerReaderDescription(new AbstractReader.ReaderDescription("sav", SPSSExampleSource.class, "filename"));
        ATTRIBUTE_NAMING_MODES = new String[] { "name", "label", "name (label)", "label (name)" };
    }
    public static final int USE_VAR_NAME_LABELED = 2;
    public static final int USE_VAR_LABEL_NAMED = 3;
    public static final String[] ATTRIBUTE_NAMING_MODES;
    private static final int CODE_HEADER = 608586802;
    private static final int LENGTH_HEADER = 176;
    private static final int INDEX_CODE_HEADER = 0;
    private static final int INDEX_HEADER_PRODUCT_NAME = 4;
    private static final int LENGTH_HEADER_PRODUCT_NAME = 60;
    private static final int INDEX_HEADER_LAYOUT_CODE = 64;
    private static final int CODE_HEADER_LAYOUT_CODE = 2;
    private static final int INDEX_HEADER_CASE_SIZE = 68;
    private static final int INDEX_HEADER_COMPRESSED = 72;
    private static final int INDEX_HEADER_WEIGHT_INDEX = 76;
    private static final int INDEX_HEADER_NUMBER_OF_CASES = 80;
    private static final int INDEX_HEADER_BIAS = 84;
    private static final int INDEX_HEADER_DATE = 92;
    private static final int LENGTH_HEADER_DATE = 9;
    private static final int INDEX_HEADER_TIME = 101;
    private static final int LENGTH_HEADER_TIME = 8;
    private static final int INDEX_HEADER_DATASET_LABEL = 109;
    private static final int LENGTH_HEADER_DATASET_LABEL = 64;
    private static final int CODE_VARIABLE = 2;
    private static final int LENGTH_VARIABLE = 32;
    private static final int INDEX_VARIABLE_TYPE = 4;
    private static final int INDEX_VARIABLE_LABELED = 8;
    private static final int INDEX_VARIABLE_NUMBER_OF_MISSING_VALUES = 12;
    private static final int INDEX_VARIABLE_PRINT_FORMAT = 16;
    private static final int INDEX_VARIABLE_NAME = 24;
    private static final int LENGTH_VARIABLE_NAME = 8;
    private static final int FORMAT_DATE = 20;
    private static final int FORMAT_EDATE = 38;
    private static final int FORMAT_SDATE = 39;
    private static final int FORMAT_TIME = 21;
    private static final int FORMAT_DATETIME = 22;
    private static final int CODE_VALUE_LABEL = 3;
    private static final int CODE_VALUE_LABEL_VARIABLE = 4;
    private static final int CODE_DOCUMENT = 6;
    private static final int LENGTH_DOCUMENT_LINE = 80;
    private static final int CODE_INFORMATION_HEADER = 7;
    private static final int LENGTH_INFORMATION_HEADER = 12;
    private static final int INDEX_INFORMATION_HEADER_SUBTYPE = 0;
    private static final int INDEX_INFORMATION_HEADER_SIZE = 4;
    private static final int INDEX_INFORMATION_HEADER_COUNT = 8;
    private static final int CODE_INFORMATION_HEADER_SUBTYPE_MACHINE_32 = 3;
    private static final int LENGTH_INFORMATION_HEADER_SUBTYPE_MACHINE_32 = 32;
    private static final int CODE_INFORMATION_HEADER_SUBTYPE_MACHINE_64 = 4;
    private static final int LENGTH_INFORMATION_HEADER_SUBTYPE_MACHINE_64 = 24;
    private static final int CODE_INFORMATION_HEADER_AUXILIARY_VARIABLE_PARAMETERS = 11;
    private static final int LENGTH_INFORMATION_HEADER_SINGLE_AUXILIARY_VARIABLE_PARAMETERS = 12;
    private static final int CODE_LONG_VARIABLE_NAMES = 13;
    private static final int CODE_LONG_VARIABLE_NAME_RECORDS_DIVIDER = 9;
    private static final int CODE_DICTIONARY_TERMINATION = 999;
    private static final int LENGTH_COMMAND_CODE_BLOCK = 8;
    private static final int CODE_COMMAND_CODE_IGNORED = 0;
    private static final int CODE_COMMAND_CODE_EOF = 252;
    private static final int CODE_COMMAND_CODE_NOT_COMPRESSIBLE = 253;
    private static final int CODE_COMMAND_CODE_ALL_SPACES_STRING = 254;
    private static final int CODE_COMMAND_CODE_SYSTEM_MISSING = 255;
    private static final int LENGTH_VALUE_BLOCK = 8;
    private static final long GREGORIAN_CALENDAR_OFFSET_IN_MILLISECONDS = -12219379200000L;

    private static class Variable
    {
        private static final int TYPE_NUMERICAL = 0;
        private static final int MEASURE_NOMINAL = 1;
        private static final int MEASURE_ORDINAL = 2;
        private static final int MEASURE_CONTINUOUS = 3;
        private int type;
        private boolean labeled;
        private int printFormat;
        private int numberOfMissingValues;
        private String name;
        private String label;
        private double[] missingValues;
        private LinkedHashMap<Double, String> valueLabels;
        private int measure;

        private Variable() {}

        private boolean isDateVariable() { return (this.printFormat == 20 || this.printFormat == 38 || this.printFormat == 39); }

        private boolean isTimeVariable() { return (this.printFormat == 21); }

        private boolean isDateTimeVariable() { return (this.printFormat == 22); }
    }

    public SPSSExampleSource(OperatorDescription description) { super(description); }

    @Override
    protected String getFileSuffix() { return "sav"; }

    @Override
    protected ExampleSet readStream(InputStream inputStream, DataRowFactory dataRowFactory) throws IOException, UndefinedParameterError {
        int attributeNamingMode = getParameterAsInt("attribute_naming_mode");
        boolean useValueLabels = getParameterAsBoolean("use_value_labels");
        boolean recodeUserMissings = getParameterAsBoolean("recode_user_missings");
        int sampleSize = getParameterAsInt("sample_size");
        double sampleRatio = getParameterAsDouble("sample_ratio");
        RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this);

        byte[] buffer = new byte[500];
        boolean reverseEndian = false;


        read(inputStream, buffer, 176);
        if (extractInt(buffer, 0, false) != 608586802) {
            throw new IOException("Wrong file format");
        }
        String productName = extractString(buffer, 4, 60);
        int layoutCode = extractInt(buffer, 64, false);
        if (layoutCode != 2) {
            reverseEndian = true;
            layoutCode = extractInt(buffer, 64, reverseEndian);
            if (layoutCode != 2) {
                throw new IOException("Wrong file format");
            }
        }
        int caseSize = extractInt(buffer, 68, reverseEndian);
        boolean compressed = (extractInt(buffer, 72, reverseEndian) == 1);
        int weightIndex = extractInt(buffer, 76, reverseEndian);
        int numberOfExamples = extractInt(buffer, 80, reverseEndian);
        double bias = extractDouble(buffer, 84, reverseEndian);
        String date = extractString(buffer, 92, 9);
        String time = extractString(buffer, 101, 8);
        String dataSetLabel = extractString(buffer, 109, 64);

        StringBuffer logMessage = new StringBuffer("SPSSExampleSource starts reading..." + Tools.getLineSeparator());
        logMessage.append((compressed ? "" : "un") + "compressed, written by  " + productName + "  at " + time + ", " + date +
                Tools.getLineSeparator());
        if (dataSetLabel.equals("")) {
            logMessage.append("no file label, ");
        } else {
            logMessage.append("file label is " + dataSetLabel + Tools.getLineSeparator());
        }
        logMessage.append("contains " + numberOfExamples + " examples, case size is " + caseSize + "x8=" + (caseSize * 8) + " Bytes" +
                Tools.getLineSeparator());
        logMessage.append("weight index is " + weightIndex + Tools.getLineSeparator());
        log(logMessage.toString());


        List<Variable> variables = new LinkedList<Variable>();
        LinkedHashMap<Integer, Integer> variableNrTranslations = new LinkedHashMap<Integer, Integer>();

        int variableNr = 0;
        for (int i = 0; i < caseSize; i++) {
            read(inputStream, buffer, 32);
            if (extractInt(buffer, 0, reverseEndian) != 2) {
                throw new IOException("file corrupt (missing variable definitions)");
            }
            Variable variable = new Variable();
            variable.type = extractInt(buffer, 4, reverseEndian);
            variable.labeled = (extractInt(buffer, 8, reverseEndian) == 1);
            variable.numberOfMissingValues = extractInt(buffer, 12, reverseEndian);
            variable.printFormat = (0xFF0000 & extractInt(buffer, 16, reverseEndian)) >> 16;
            variable.name = extractString(buffer, 24, 8);
            if (variable.labeled) {
                read(inputStream, buffer, 32, 4);
                int labelLength = extractInt(buffer, 32, reverseEndian);
                int adjLabelLength = labelLength;
                if (labelLength % 4 != 0) {
                    adjLabelLength = labelLength + 4 - labelLength % 4;
                }
                read(inputStream, buffer, adjLabelLength);
                variable.label = extractString(buffer, 0, labelLength);
            }
            if (variable.numberOfMissingValues != 0) {
                read(inputStream, buffer, variable.numberOfMissingValues * 8);
                variable.missingValues = new double[variable.numberOfMissingValues];
                for (int j = 0; j < variable.numberOfMissingValues; j++) {
                    variable.missingValues[j] = extractDouble(buffer, j * 8, reverseEndian);
                }
            }
            if (variable.type != -1) {
                variables.add(variable);
                variableNrTranslations.put(Integer.valueOf(i), Integer.valueOf(variableNr));
                variableNr++;
            }
        }



        boolean valueLabelsRead = false;
        LinkedHashMap<Double, String> valueLabels = null;
        boolean terminated = false;
        do {
            String longVariableNamePairs[], longVariableNamesString;
            int i, size, subType, count = 0;
            read(inputStream, buffer, 4);
            int recordType = extractInt(buffer, 0, reverseEndian);
            switch (recordType) {
                case 3:
                    read(inputStream, buffer, 4);
                    count = extractInt(buffer, 0, reverseEndian);
                    valueLabels = new LinkedHashMap<Double, String>();
                    for (i = 0; i < count; i++) {
                        read(inputStream, buffer, 8);
                        double labelValue = extractDouble(buffer, 0, reverseEndian);
                        read(inputStream, buffer, 1);
                        int labelLength = buffer[0];
                        int adjLabelLength = labelLength + 8 - labelLength % 8 - 1;
                        read(inputStream, buffer, adjLabelLength);
                        String labelLabel = extractString(buffer, 0, adjLabelLength);
                        valueLabels.put(Double.valueOf(labelValue), labelLabel);
                    }
                    valueLabelsRead = true;
                    break;
                case 4:
                    if (!valueLabelsRead) {
                        throw new IOException("Wrong file format: value labels have not been read");
                    }
                    valueLabelsRead = false;
                    read(inputStream, buffer, 4);
                    count = extractInt(buffer, 0, reverseEndian);
                    for (i = 0; i < count; i++) {
                        read(inputStream, buffer, 4);
                        variableNr = ((Integer)variableNrTranslations.get(Integer.valueOf(extractInt(buffer, 0, reverseEndian) - 1))).intValue();
                        if (variableNr < variables.size()) {
                            Variable variable = (Variable)variables.get(variableNr);
                            variable.valueLabels = valueLabels;
                        }
                    }
                    break;
                case 6:
                    read(inputStream, buffer, 4);
                    count = extractInt(buffer, 0, reverseEndian);
                    for (i = 0; i < count; i++) {
                        read(inputStream, buffer, 80);
                    }
                    break;
                case 7:
                    read(inputStream, buffer, 0, 12);
                    subType = extractInt(buffer, 0, reverseEndian);
                    size = extractInt(buffer, 4, reverseEndian);
                    count = extractInt(buffer, 8, reverseEndian);
                    switch (subType) {
                        case 3:
                            read(inputStream, buffer, 32);
                            break;
                        case 4:
                            read(inputStream, buffer, 24);
                            break;
                        case 11:
                            for (i = 0; i < variables.size(); i++) {
                                read(inputStream, buffer, 12);
                                Variable variable = (Variable)variables.get(i);
                                variable.measure = extractInt(buffer, 0, reverseEndian);
                            }
                            break;
                        case 13:
                            buffer = new byte[count * size];
                            read(inputStream, buffer, count * size);
                            longVariableNamesString = new String(buffer);

                            longVariableNamePairs = longVariableNamesString.split(new String(new char[] { '\t' }));
                            for (i = 0; i < longVariableNamePairs.length; i++) {
                                String[] keyLongVariablePair = longVariableNamePairs[i].split("=");
                                if (keyLongVariablePair.length == 2)
                                {

                                    for (Variable variable : variables) {
                                        if (variable.name.equals(keyLongVariablePair[0]))
                                            variable.name = keyLongVariablePair[1];
                                    }
                                }
                            }
                            buffer = new byte[500];
                            break;
                    }
                    buffer = new byte[count * size];
                    read(inputStream, buffer, count * size);
                    buffer = new byte[500];
                    break;


                case 999:
                    read(inputStream, buffer, 4);
                    terminated = true;
                    break;
            }

        } while (!terminated);

        AttributeSet attributeSet = new AttributeSet();
        Attribute attribute = null;
        for (int i = 0; i < variables.size(); i++) {
            Variable variable = (Variable)variables.get(i);
            String attributeName = null;
            if (variable.label == null) {
                variable.label = variable.name;
            }
            switch (attributeNamingMode) {
                case 0:
                    attributeName = variable.name;
                    break;
                case 1:
                    attributeName = variable.label;
                    break;
                case 2:
                    attributeName = variable.name + " (" + variable.label + ")";
                    break;
                case 3:
                    attributeName = variable.label + " (" + variable.name + ")";
                    break;
                default:
                    attributeName = variable.name; break;
            }
            if (variable.type == 0) {

                if (variable.isDateVariable()) {
                    attribute = AttributeFactory.createAttribute(attributeName, 10);
                } else if (variable.isTimeVariable()) {
                    attribute = AttributeFactory.createAttribute(attributeName, 11);
                } else if (variable.isDateTimeVariable()) {
                    attribute = AttributeFactory.createAttribute(attributeName, 9);
                } else {
                    switch (variable.measure) {
                        case 1:
                            attribute = AttributeFactory.createAttribute(attributeName, 1);
                            break;
                        case 2:
                            attribute = AttributeFactory.createAttribute(attributeName, 1);
                            break;
                        case 3:
                            attribute = AttributeFactory.createAttribute(attributeName, 2);
                            break;
                        default:
                            if (useValueLabels && variable.valueLabels != null) {
                                attribute = AttributeFactory.createAttribute(attributeName, 1); break;
                            }
                            attribute = AttributeFactory.createAttribute(attributeName, 2);
                            break;
                    }
                }
            } else {
                attribute = AttributeFactory.createAttribute(attributeName, 5);
            }

            if (attribute.isNominal() &&
                    variable.valueLabels != null) {
                Iterator<Double> iterator = variable.valueLabels.keySet().iterator();
                while (iterator.hasNext()) {
                    Double numericValue = (Double)iterator.next();
                    boolean missing = false;
                    if (recodeUserMissings) {
                        for (int j = 0; j < variable.numberOfMissingValues; j++) {
                            if (numericValue.doubleValue() == variable.missingValues[j]) {
                                missing = true;
                                break;
                            }
                        }
                    }
                    if (!missing) {
                        if (useValueLabels) {
                            attribute.getMapping().mapString((String)variable.valueLabels.get(numericValue)); continue;
                        }
                        attribute.getMapping().mapString(Double.toString(numericValue.doubleValue()));
                    }
                }
            }

            attributeSet.addAttribute(attribute);
        }

        OrderedSamplingWithoutReplacement sampling = null;
        if (sampleSize != -1) {
            sampling = new OrderedSamplingWithoutReplacement(randomGenerator, numberOfExamples, sampleSize);
        } else {
            sampling = new OrderedSamplingWithoutReplacement(randomGenerator, numberOfExamples, sampleRatio);
        }


        Attribute weight = (weightIndex == 0) ? null : attributeSet.getAttribute(((Integer)variableNrTranslations.get(Integer.valueOf(weightIndex - 1))).intValue());
        List<Attribute> allAttributes = attributeSet.getAllAttributes();
        ExampleSetBuilder builder = ExampleSets.from(allAttributes);
        builder.withOptimizationHint(DataManagementParameterHelper.getSelectedDataManagement(this));

        Attribute[] attributes = (Attribute[])allAttributes.toArray(new Attribute[allAttributes.size()]);
        int commandCodeCounter = 0;
        int bytesRead = 0;
        for (int i = 0; i < numberOfExamples; i++) {
            String[] values = new String[variables.size()];
            if (!compressed) {
                for (int j = 0; j < variables.size(); j++) {
                    read(inputStream, buffer, 8);
                    values[j] = Double.toString(extractDouble(buffer, 0, reverseEndian));
                }
            } else {
                for (int j = 0; j < variables.size(); j++) {
                    boolean readValue = false;
                    String value = null;
                    Variable variable = (Variable)variables.get(j); while (true) {
                        double numericValue;
                        if (commandCodeCounter % 8 == 0) {
                            commandCodeCounter = 0;
                            bytesRead = read(inputStream, buffer, 0, 8);
                            if (bytesRead == -1) {
                                break;
                            }
                        }
                        int commandCode = 0xFF & buffer[commandCodeCounter];
                        switch (commandCode) {
                            case 0:
                                break;

                            case 252:
                                for (int k = commandCodeCounter + 1; k < 8; k++) {
                                    buffer[k] = 0;
                                }
                                break;
                            case 253:
                                bytesRead = read(inputStream, buffer, 8, 8);
                                if (bytesRead == -1) {
                                    throw new IOException("file corrupt (data inconsistency)");
                                }
                                if (variable.type == 0) {
                                    numericValue = extractDouble(buffer, 8, reverseEndian);
                                    if (variable.isDateVariable() || variable.isTimeVariable() || variable
                                            .isDateTimeVariable()) {
                                        numericValue = ((long)numericValue * 1000L + -12219379200000L);
                                    }

                                    value = Double.toString(numericValue);
                                    if (variable.measure != 3 &&
                                            useValueLabels &&
                                            variable.valueLabels != null) {
                                        String label = (String)variable.valueLabels.get(Double.valueOf(numericValue));
                                        value = label;
                                    }

                                    if (recodeUserMissings) {
                                        for (int k = 0; k < variable.numberOfMissingValues; k++) {
                                            if (Tools.isEqual(numericValue, variable.missingValues[k])) {
                                                value = null;
                                            }
                                        }
                                    }
                                    readValue = true; break;
                                }
                                if (value == null) {
                                    value = new String(buffer, 8, 8);
                                } else {
                                    value = value + new String(buffer, 8, 8);
                                }
                                if (value.length() >= ((Variable)variables.get(j)).type) {
                                    value = value.trim();
                                    readValue = true;
                                }
                                break;

                            case 254:
                                value = (value == null) ? String.valueOf("        ") : value.concat(String.valueOf("        "));
                                if (value.length() >= ((Variable)variables.get(j)).type) {
                                    value = value.trim();
                                    readValue = true;
                                }
                                break;
                            case 255:
                                value = null;
                                readValue = true;
                                break;
                            default:
                                numericValue = commandCode - bias;
                                value = Double.toString(numericValue);
                                if (variable.measure != 3 &&
                                        useValueLabels &&
                                        variable.valueLabels != null) {
                                    String label = (String)variable.valueLabels.get(Double.valueOf(numericValue));
                                    value = label;
                                }

                                if (recodeUserMissings) {
                                    for (int k = 0; k < variable.numberOfMissingValues; k++) {
                                        if (Tools.isEqual(numericValue, variable.missingValues[k])) {
                                            value = null;
                                        }
                                    }
                                }
                                readValue = true;
                                break;
                        }
                        commandCodeCounter++;
                        if (readValue) {
                            values[j] = value;

                            break;
                        }
                    }
                }
            }
            if (sampling.acceptElement()) {
                builder.addDataRow(dataRowFactory.create(values, attributes));
            }
        }
        inputStream.close();

        ExampleSet exampleSet = builder.build();
        exampleSet.getAttributes().setWeight(weight);
        return exampleSet;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterTypeCategory parameterTypeCategory = new ParameterTypeCategory("attribute_naming_mode", "Determines which SPSS variable properties should be used for attribute naming.", ATTRIBUTE_NAMING_MODES, 0);


        parameterTypeCategory.setExpert(false);
        types.add(parameterTypeCategory);
        ParameterTypeBoolean parameterTypeBoolean = new ParameterTypeBoolean("use_value_labels", "Use SPSS value labels as values.", true);
        parameterTypeBoolean.setExpert(false);
        types.add(parameterTypeBoolean);
        parameterTypeBoolean = new ParameterTypeBoolean("recode_user_missings", "Recode SPSS user missings to missing values.", true);

        parameterTypeBoolean.setExpert(false);
        types.add(parameterTypeBoolean);
        ParameterTypeDouble parameterTypeDouble = new ParameterTypeDouble("sample_ratio", "The fraction of the data set which should be read (1 = all; only used if sample_size = -1)", 0.0D, 1.0D, 1.0D);


        parameterTypeDouble.setExpert(false);
        types.add(parameterTypeDouble);
        ParameterTypeInt parameterTypeInt = new ParameterTypeInt("sample_size", "The exact number of samples which should be read (-1 = all; if not -1, sample_ratio will not have any effect)", -1, 2147483647, -1);


        parameterTypeInt.setExpert(true);
        types.add(parameterTypeInt);

        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

        return types;
    }
}

