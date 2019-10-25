package base.operators.operator.nio;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.*;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.parameter.*;
import base.operators.tools.Ontology;
import base.operators.tools.StrictDecimalFormat;
import base.operators.utils.HDFSUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public class NewCSVExampleSource extends Operator {

    public static final String PARAMETER_STORAGE_TYPE = "storage_type";
    public static final String PARAMETER_CSV_FILE = "csv_file";
    public static final String PARAMETER_COLUMN_SEPARATORS = "column_separators";
    public static final String PARAMETER_FIRST_ROW_AS_NAMES = "first_row_as_names";
    public static final String PARAMETER_STARTING_ROW = "starting_row";

    /**
     * Values will be trimmed for guessing after this version
     * @since 9.2.0
     */
    public static final OperatorVersion BEFORE_VALUE_TRIMMING_GUESSING = new OperatorVersion(9, 0, 3);

    private InputPort fileInputPort = getInputPorts().createPort("file");
    private final OutputPort outputPort = getOutputPorts().createPort("output");

    public NewCSVExampleSource(final OperatorDescription description) {
        super(description);
    }

    @Override
    public void doWork() throws OperatorException {
        String storageType = getParameterAsString(PARAMETER_STORAGE_TYPE);
        boolean withHeader = getParameterAsBoolean(PARAMETER_FIRST_ROW_AS_NAMES);
        CSVFormat format;
        if (withHeader) {
            format = CSVFormat.DEFAULT.withHeader().withDelimiter(getParameterAsChar(PARAMETER_COLUMN_SEPARATORS));
        } else {
            format = CSVFormat.DEFAULT.withDelimiter(getParameterAsChar(PARAMETER_COLUMN_SEPARATORS));
        }
        Charset charset = StandardCharsets.UTF_8;
        int offset = getParameterAsInt(PARAMETER_STARTING_ROW);
        String pathString = getParameterAsString(PARAMETER_CSV_FILE);
        try {
            ExampleSet exampleSet;
            String[] columnNames = null;
            int[] valueTypes = null;
            Attribute[] listOfAttrs = null;
            DateFormat dateFormat = (DateFormat) DateFormat.getDateTimeInstance().clone();
            NumberFormat numberFormat = new StrictDecimalFormat('.');
            FileSystem fs = null;
            InputStream in;
            try {
                if ("hdfs".equals(storageType.toLowerCase())){
                    fs = HDFSUtil.getFileSystem();
                    in = fs.open(new Path(pathString));
                } else {
                    in = new FileInputStream(new File(pathString));
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
            try (CSVParser parser = CSVParser.parse(in, charset, format)) {
                Map<String, Integer> header = parser.getHeaderMap();
                if (header != null) {
                    columnNames = new String[header.size()];
                    valueTypes = new int[header.size()];
                    listOfAttrs = new Attribute[header.size()];
                    for (Map.Entry<String, Integer> column : header.entrySet()) {
                        columnNames[column.getValue()] = column.getKey();
                        valueTypes[column.getValue()] = Ontology.INTEGER;
                    }
                    int k = 0;
                    int count = 0;

                    for (CSVRecord record : parser) {
                        if (++count < offset) {
                            continue;
                        }
                        guessValueTypes(record, valueTypes, dateFormat, numberFormat);
                        if (++k >= 1000) {
                            break;
                        }
                    }

                } else {
                    Iterator<CSVRecord> iter = parser.iterator();
                    if (!iter.hasNext()) {
                        throw new RuntimeException("Empty File");
                    }
                    int k = 0;
                    int count = 0;

                    for (CSVRecord record : parser) {
                        if (++count < offset) {
                            continue;
                        }
                        if (count == offset) {
                            columnNames = new String[record.size()];
                            valueTypes = new int[record.size()];
                            listOfAttrs = new Attribute[record.size()];
                            for (int i = 0; i < columnNames.length; i++) {
                                columnNames[i] = String.format("Column %d", i + 1);
                                valueTypes[i] = Ontology.INTEGER;
                            }
                        }
                        guessValueTypes(record, valueTypes, dateFormat, numberFormat);
                        if (++k >= 1000) {
                            break;
                        }
                    }
                }

                in.close();
                for (int c = 0; c < valueTypes.length; c++) {
                    Attribute newAttribute = AttributeFactory.createAttribute(columnNames[c], valueTypes[c]);
                    newAttribute.setConstruction(columnNames[c]);
                    listOfAttrs[c] = newAttribute;
                }
            } catch (IOException e) {
                throw new OperatorException("io exception");
            }

            try {
                if ("hdfs".equals(storageType.toLowerCase())){
                    fs = HDFSUtil.getFileSystem();
                    in = fs.open(new Path(pathString));
                } else {
                    in = new FileInputStream(new File(pathString));
                }
            } catch (IOException e) {
                throw new RuntimeException();
            }
            try {
                CSVParser parser = CSVParser.parse(in, charset, format);
                ExampleSetBuilder builder = ExampleSets.from(listOfAttrs);
                int count = 0;
                for (CSVRecord record : parser) {
                    if (++count < offset) {
                        continue;
                    }
                    double[] doubleArray = new double[listOfAttrs.length];
                    for (int j = 0; j < listOfAttrs.length; j++) {
                        Attribute attribute = listOfAttrs[j];
                        String attributeType = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attribute.getValueType());
                        switch (attributeType) {
                            case "date_time":
                            case "integer":
                                doubleArray[j] = getLong(record.get(j), numberFormat);
                            case "numeric":
                            case "real":
                                doubleArray[j] = getDouble(record.get(j), numberFormat);
                                break;
                            case "nominal":
                            case "attribute_value":
                            case "text":
                            case "binominal":
                            case "polynominal":
                            case "file_path":
                            default:
                                doubleArray[j] = attribute.getMapping().mapString(record.get(j));
                                break;
                        }
                    }

                    builder.addRow(doubleArray);
                }
                exampleSet = builder.build();

                outputPort.deliver(exampleSet);
                try {
                    parser.close();
                    fs.close();
                } catch (Exception e){
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
        }
    }

    public void guessValueTypes(CSVRecord record, int[] valueTypes, DateFormat dateFormat, NumberFormat numberFormat) {
        for (int c = 0; c < valueTypes.length; c++) {
            String value = record.get(c).trim();
            if ((value != null) && (!value.equals("?")) && (value.length() > 0)) {
                valueTypes[c] = guessValueType(valueTypes[c], value, dateFormat, numberFormat);
            }
        }
    }

    private int guessValueType(int currentValueType, String value, DateFormat dateFormat,
                               NumberFormat numberFormat) {
        if (currentValueType == Ontology.BINOMINAL || currentValueType == Ontology.POLYNOMINAL) {
            // Don't set to binominal type, it fails too often.
            return Ontology.POLYNOMINAL;
        }

        if (currentValueType == Ontology.DATE) {
            try {
                dateFormat.parse(value);
                return currentValueType;
            } catch (ParseException e) {
                return Ontology.POLYNOMINAL;
            }
        }
        if (currentValueType == Ontology.REAL) {
            if (numberFormat != null) {
                try {
                    numberFormat.parse(value);
                    return Ontology.REAL;
                } catch (ParseException e) {
                    return guessValueType(Ontology.DATE, value, dateFormat, numberFormat);
                }
            } else {
                try {
                    Double.parseDouble(value);
                    return Ontology.REAL;
                } catch (NumberFormatException e) {
                    return guessValueType(Ontology.DATE, value, dateFormat, null);
                }
            }
        }
        try {
            Integer.parseInt(value);
            return Ontology.INTEGER;
        } catch (NumberFormatException e) {
            return guessValueType(Ontology.REAL, value, dateFormat, numberFormat);
        }
    }

    private double getDouble(String value, NumberFormat numberFormat) {
        try {
            Number parsedValue;
            parsedValue = numberFormat.parse(value);
            if (parsedValue == null) {
                return Double.NaN;
            } else {
                return parsedValue.doubleValue();
            }
        } catch (ParseException e) {
            return Double.NaN;
        }
    }

    private double getLong(String value, NumberFormat numberFormat) {
        try {
            Number parsedValue;
            parsedValue = numberFormat.parse(value);
            if (parsedValue == null) {
                return Double.NaN;
            } else {
                return parsedValue.longValue();
            }
        } catch (ParseException e) {
            return Double.NaN;
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        LinkedList<ParameterType> types = new LinkedList<>();

        //位置
        types.add(new ParameterTypeString(PARAMETER_CSV_FILE,
                "csv file location", false));
        // storage type
        types.add(new ParameterTypeCategory(PARAMETER_STORAGE_TYPE, "Storage type for the data files", new String[]{"HDFS", "Local"}, 0, false));
        // Separator
        types.add(new ParameterTypeString(PARAMETER_COLUMN_SEPARATORS,
                "Column separators for data files (regular expression)", ",", false));

        types.add(new ParameterTypeBoolean(PARAMETER_FIRST_ROW_AS_NAMES,
                "Indicates if the first row should be used for the attribute names. If activated no annotations can be used.",
                true, false));
        // Starting row
        types.add(new ParameterTypeInt(PARAMETER_STARTING_ROW, "The first row where reading should start, everything before it will be skipped.",
                1, Integer.MAX_VALUE, 1, true));

        return types;
    }

    @Override
    public OperatorVersion[] getIncompatibleVersionChanges() {
        return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
                new OperatorVersion[]{
                        BEFORE_VALUE_TRIMMING_GUESSING
                });
    }
}
