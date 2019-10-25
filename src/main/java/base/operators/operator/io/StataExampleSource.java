package base.operators.operator.io;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.DataRowReader;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.OperatorDescription;
import base.operators.operator.io.BytewiseExampleSource;
import base.operators.parameter.ParameterType;
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
import java.util.Map;

public class StataExampleSource extends BytewiseExampleSource {
    public static final String PARAMETER_ATTRIBUTE_NAMING_MODE = "attribute_naming_mode";
    public static final String PARAMETER_HANDLE_VALUE_LABELS = "handle_value_labels";
    public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";
    public static final String PARAMETER_SAMPLE_SIZE = "sample_size";
    private static final String STATA_FILE_SUFFIX = "dta";
    public static final int USE_VAR_NAME = 0;
    public static final int USE_VAR_LABEL = 1;
    public static final int USE_VAR_NAME_LABELED = 2;
    public static final int USE_VAR_LABEL_NAMED = 3;
    public static final String[] ATTRIBUTE_NAMING_MODES = new String[]{"name", "label", "name (label)", "label (name)"};
    public static final int FORCE_NUMERIC = 0;
    public static final int IGNORE = 1;
    public static final int USE_ADDITIONALLY = 2;
    public static final int USE_EXCLUSIVELY = 3;
    public static final String[] HANDLE_VALUE_LABELS_MODES = new String[]{"force numeric", "ignore", "use additionally", "use exclusively"};
    private static final int CODE_STRING_TERMINATOR = 0;
    private static final int CODE_DS_FORMAT_VERSION_113 = 113;
    private static final int CODE_DS_FORMAT_VERSION_114 = 114;
    private static final int CODE_BYTEORDER_HILO = 1;
    private static final int CODE_BYTEORDER_LOHI = 2;
    private static final int CODE_FILETYPE = 1;
    private static final int LENGTH_HEADER = 109;
    private static final int INDEX_HEADER_DS_FORMAT = 0;
    private static final int INDEX_HEADER_BYTEORDER = 1;
    private static final int INDEX_HEADER_FILETYPE = 2;
    private static final int INDEX_HEADER_NUMBER_OF_ATTRIBUTES = 4;
    private static final int INDEX_HEADER_NUMBER_OF_EXAMPLES = 6;
    private static final int CODE_TYPE_BYTE = 251;
    private static final int CODE_TYPE_INT = 252;
    private static final int CODE_TYPE_LONG = 253;
    private static final int CODE_TYPE_FLOAT = 254;
    private static final int CODE_TYPE_DOUBLE = 255;
    private static final int LENGTH_TYPE_BYTE = 1;
    private static final int LENGTH_TYPE_INT = 2;
    private static final int LENGTH_TYPE_LONG = 4;
    private static final int LENGTH_TYPE_FLOAT = 4;
    private static final int LENGTH_TYPE_DOUBLE = 8;
    private static final int LENGTH_ATTRIBUTE_NAME = 33;
    private static final int LENGTH_ATTRIBUTE_FORMAT_VERSION_113 = 12;
    private static final int LENGTH_ATTRIBUTE_FORMAT_VERSION_114 = 49;
    private static final int LENGTH_ATTRIBUTE_VALUE_LABEL_IDENTIFIER = 33;
    private static final int LENGTH_ATTRIBUTE_LABEL = 81;
    private static final int LENGTH_EXPANSION_FIELD_HEADER = 5;
    private static final int INDEX_EXPANSION_FIELD_HEADER_TYPE = 0;
    private static final int INDEX_EXPANSION_FIELD_HEADER_LENGTH = 1;
    private static final int LENGTH_VALUE_LABEL_HEADER = 40;
    private static final int INDEX_VALUE_LABEL_HEADER_LENGTH = 0;
    private static final int INDEX_VALUE_LABEL_HEADER_NAME = 4;
    private static final int LENGTH_VALUE_LABEL_HEADER_NAME = 33;
    private static final int INDEX_VALUE_LABEL_TABLE_NUMBER_OF_ENTRIES = 0;
    private static final int INDEX_VALUE_LABEL_TABLE_TEXT_LENGTH = 4;
    private static final int INDEX_VALUE_LABEL_TABLE_OFFSETS = 8;
    private static final byte CODE_MAXIMUM_NONMISSING_BYTE = 100;
    private static final int CODE_MAXIMUM_NONMISSING_INT = 32740;
    private static final int CODE_MAXIMUM_NONMISSING_LONG = 2147483620;
    private static final double CODE_MAXIMUM_NONMISSING_FLOAT = 1.701E38D;
    private static final double CODE_MAXIMUM_NONMISSING_DOUBLE = 8.988E307D;

    public StataExampleSource(OperatorDescription description) {
        super(description);
    }

    @Override
    protected String getFileSuffix() {
        return "dta";
    }

    @Override
    protected ExampleSet readStream(InputStream inputStream, DataRowFactory dataRowFactory) throws IOException, UndefinedParameterError {
        int attributeNamingMode = this.getParameterAsInt("attribute_naming_mode");
        int handleValueLabelsMode = this.getParameterAsInt("handle_value_labels");
        double sampleRatio = this.getParameterAsDouble("sample_ratio");
        int sampleSize = this.getParameterAsInt("sample_size");
        RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this);
        byte[] buffer = new byte[500];
        boolean reverseEndian = false;
        this.read(inputStream, buffer, 109);
        int dataSetFormat = 255 & buffer[0];
        if (dataSetFormat != 113 && dataSetFormat != 114) {
            throw new IOException("Unsupported data set format");
        } else if (buffer[2] != 1) {
            throw new IOException("Wrong file format");
        } else {
            byte byteOrder = buffer[1];
            if (byteOrder != 2 && byteOrder != 1) {
                throw new IOException("Wrong file format");
            } else {
                reverseEndian = byteOrder == 2;
                int numberOfAttributes = this.extract2ByteInt(buffer, 4, reverseEndian);
                int numberOfExamples = this.extractInt(buffer, 6, reverseEndian);
                byte[] attributeTypes = new byte[numberOfAttributes];
                this.read(inputStream, buffer, numberOfAttributes);

                for(int i = 0; i < numberOfAttributes; ++i) {
                    attributeTypes[i] = buffer[i];
                }

                String[] attributeNames = new String[numberOfAttributes];

                for(int i = 0; i < numberOfAttributes; ++i) {
                    this.read(inputStream, buffer, 33);
                    String attributeNameString = new String(buffer, 0, 33);
                    attributeNames[i] = attributeNameString.substring(0, attributeNameString.indexOf(0)).trim();
                }

                this.read(inputStream, buffer, 2 * (numberOfAttributes + 1));

                for(int i = 0; i < numberOfAttributes; ++i) {
                    if (dataSetFormat == 113) {
                        this.read(inputStream, buffer, 12);
                    } else if (dataSetFormat == 114) {
                        this.read(inputStream, buffer, 49);
                    }
                }

                String[] valueLabelsIdentifiers = new String[numberOfAttributes];
                boolean[] labeled = new boolean[numberOfAttributes];

                for(int i = 0; i < numberOfAttributes; ++i) {
                    this.read(inputStream, buffer, 33);
                    labeled[i] = buffer[0] != 0;
                    String valueLabelsIdentifierString = new String(buffer, 0, 33);
                    valueLabelsIdentifiers[i] = valueLabelsIdentifierString.substring(0, valueLabelsIdentifierString.indexOf(0)).trim();
                    if (valueLabelsIdentifiers[i].equals("")) {
                        valueLabelsIdentifiers[i] = null;
                    }
                }

                String[] attributeLabels = new String[numberOfAttributes];

                int expansionFieldContentsLength;
                for(expansionFieldContentsLength = 0; expansionFieldContentsLength < numberOfAttributes; ++expansionFieldContentsLength) {
                    this.read(inputStream, buffer, 81);
                    String attributeLabelString = new String(buffer, 0, 81);
                    attributeLabels[expansionFieldContentsLength] = attributeLabelString.substring(0, attributeLabelString.indexOf(0)).trim();
                    if (attributeLabels[expansionFieldContentsLength].equals("")) {
                        attributeLabels[expansionFieldContentsLength] = null;
                    }
                }

                while(true) {
                    this.read(inputStream, buffer, 5);
                    expansionFieldContentsLength = this.extractInt(buffer, 1, reverseEndian);
                    if (buffer[0] == 0 && expansionFieldContentsLength == 0) {
                        LinkedHashMap<String, List<Attribute>> attributeValueLabelIdentifiersMap = new LinkedHashMap();
                        AttributeSet attributeSet = new AttributeSet(numberOfAttributes);

                        for(int i = 0; i < numberOfAttributes; ++i) {
                            int valueType;
                            switch(255 & attributeTypes[i]) {
                                case 251:
                                    valueType = 3;
                                    break;
                                case 252:
                                    valueType = 3;
                                    break;
                                case 253:
                                    valueType = 3;
                                    break;
                                case 254:
                                    valueType = 2;
                                    break;
                                case 255:
                                    valueType = 2;
                                    break;
                                default:
                                    valueType = 1;
                            }

                            if (labeled[i] && handleValueLabelsMode != 0) {
                                valueType = 1;
                            }

                            String attributeName = null;
                            switch(attributeNamingMode) {
                                case 0:
                                    attributeName = attributeNames[i];
                                    break;
                                case 1:
                                    attributeName = attributeLabels[i] == null ? attributeNames[i] : attributeLabels[i];
                                    break;
                                case 2:
                                    attributeName = attributeLabels[i] == null ? attributeNames[i] : attributeNames[i] + " (" + attributeLabels[i] + ")";
                                    break;
                                case 3:
                                    attributeName = attributeLabels[i] == null ? attributeNames[i] : attributeLabels[i] + " (" + attributeNames[i] + ")";
                                    break;
                                default:
                                    attributeName = attributeNames[i];
                            }

                            Attribute attribute = AttributeFactory.createAttribute(attributeName, valueType);
                            attributeSet.addAttribute(attribute);
                            if (attributeValueLabelIdentifiersMap.get(valueLabelsIdentifiers[i]) == null) {
                                attributeValueLabelIdentifiersMap.put(valueLabelsIdentifiers[i], new LinkedList());
                            }

                            if (valueLabelsIdentifiers[i] != null) {
                                ((List)attributeValueLabelIdentifiersMap.get(valueLabelsIdentifiers[i])).add(attribute);
                            }
                        }

                        OrderedSamplingWithoutReplacement sampling = null;
                        if (sampleSize != -1) {
                            sampling = new OrderedSamplingWithoutReplacement(randomGenerator, numberOfExamples, sampleSize);
                        } else {
                            sampling = new OrderedSamplingWithoutReplacement(randomGenerator, numberOfExamples, sampleRatio);
                        }

                        ExampleSetBuilder builder = ExampleSets.from(attributeSet.getAllAttributes());
                        builder.withOptimizationHint(DataManagementParameterHelper.getSelectedDataManagement(this));

                        int length;
                        int i;
                        double value;
                        int stringTerminatorIndex;
                        int readLength;
                        for(readLength = 0; readLength < numberOfExamples; ++readLength) {
                            DataRow dataRow = dataRowFactory.create(numberOfAttributes);

                            for(length = 0; length < numberOfAttributes; ++length) {
                                Attribute attribute = attributeSet.getAttribute(length);
                                value = 0.0D / 0.0;
                                switch(255 & attributeTypes[length]) {
                                    case 251:
                                        this.read(inputStream, buffer, 1);
                                        byte byteValue = buffer[0];
                                        value = byteValue > 100 ? 0.0D / 0.0 : (double)byteValue;
                                        break;
                                    case 252:
                                        this.read(inputStream, buffer, 2);
                                        i = this.extract2ByteInt(buffer, 0, reverseEndian);
                                        value = i > 32740 ? 0.0D / 0.0 : (double)i;
                                        break;
                                    case 253:
                                        this.read(inputStream, buffer, 4);
                                        int longValue = this.extractInt(buffer, 0, reverseEndian);
                                        value = longValue > 2147483620 ? 0.0D / 0.0 : (double)longValue;
                                        break;
                                    case 254:
                                        this.read(inputStream, buffer, 4);
                                        float floatValue = this.extractFloat(buffer, 0, reverseEndian);
                                        value = (double)floatValue > 1.701E38D ? 0.0D / 0.0 : (double)floatValue;
                                        break;
                                    case 255:
                                        this.read(inputStream, buffer, 8);
                                        value = this.extractDouble(buffer, 0, reverseEndian);
                                        value = value > 8.988E307D ? 0.0D / 0.0 : value;
                                        break;
                                    default:
                                        stringTerminatorIndex = 255 & attributeTypes[length];
                                        this.read(inputStream, buffer, stringTerminatorIndex);
                                        String stringValue = new String(buffer, 0, stringTerminatorIndex);
                                        stringTerminatorIndex = stringValue.indexOf(0);
                                        if (stringTerminatorIndex >= 0 && stringTerminatorIndex < stringTerminatorIndex) {
                                            value = (double)attribute.getMapping().mapString(stringValue.substring(0, stringTerminatorIndex).trim());
                                        } else {
                                            value = (double)attribute.getMapping().mapString(stringValue.trim());
                                        }
                                }

                                dataRow.set(attribute, value);
                            }

                            if (sampling.acceptElement()) {
                                builder.addDataRow(dataRow);
                            }
                        }

                        readLength = 1;
                        LinkedHashMap valueMappingsMap = new LinkedHashMap();

                        do {
                            readLength = this.readWithoutLengthCheck(inputStream, buffer, 40);
                            if (readLength > 0) {
                                length = this.extractInt(buffer, 0, reverseEndian);
                                String valueLabelIdentifierString = new String(buffer, 4, 33);
                                String valueLabelIdentifier = valueLabelIdentifierString.substring(0, valueLabelIdentifierString.indexOf(0)).trim();
                                LinkedHashMap<Double, String> valueMap = new LinkedHashMap();
                                if (length > 500) {
                                    buffer = new byte[length];
                                }

                                this.read(inputStream, buffer, length);
                                int numberOfEntries = this.extractInt(buffer, 0, reverseEndian);
                                i = this.extractInt(buffer, 4, reverseEndian);
                                int[] offset = new int[numberOfEntries];

                                for(int j = 0; j < numberOfEntries; ++j) {
                                    offset[j] = this.extractInt(buffer, 8 + j * 4, reverseEndian);
                                }

                                double[] values = new double[numberOfEntries];

                                for(int j = 0; j < numberOfEntries; ++j) {
                                    values[j] = (double)this.extractInt(buffer, 8 + numberOfEntries * 4 + j * 4, reverseEndian);
                                }

                                String[] nominalValues = new String[numberOfEntries];

                                for(int j = 0; j < numberOfEntries; ++j) {
                                    nominalValues[j] = this.extractString(buffer, 8 + 2 * numberOfEntries * 4 + offset[i], i - offset[i]);
                                    stringTerminatorIndex = nominalValues[j].indexOf(0);
                                    if (stringTerminatorIndex < 0) {
                                        valueMap.put(values[j], nominalValues[j].trim());
                                    } else {
                                        valueMap.put(values[j], nominalValues[j].substring(0, nominalValues[j].indexOf(0)).trim());
                                    }
                                }

                                Iterator var68 = ((List)attributeValueLabelIdentifiersMap.get(valueLabelIdentifier)).iterator();

                                while(var68.hasNext()) {
                                    Attribute attribute = (Attribute)var68.next();
                                    valueMappingsMap.put(attribute, valueMap);
                                }
                            }
                        } while(readLength >= 0);

                        inputStream.close();
                        ExampleSet exampleSet = builder.build();
                        if (handleValueLabelsMode != 0) {
                            Attribute[] attributes = exampleSet.getExampleTable().getAttributes();
                            Map<Double, String>[] attributeValueMaps = (Map[])(new Object[numberOfAttributes]);

                            for(int j = 0; j < attributes.length; ++j) {
                                attributeValueMaps[j] = (Map)valueMappingsMap.get(attributes[j]);
                            }

                            DataRowReader iterator = exampleSet.getExampleTable().getDataRowReader();

                            while(iterator.hasNext()) {
                                DataRow dataRow = (DataRow)iterator.next();

                                for(i = 0; i < attributes.length; ++i) {
                                    if (labeled[i] && attributeValueMaps[i] != null) {
                                        double originalValue = dataRow.get(attributes[i]);
                                        value = 0.0D / 0.0;
                                        String nominalValue;
                                        switch(handleValueLabelsMode) {
                                            case 1:
                                                value = (double)attributes[i].getMapping().mapString(Tools.formatIntegerIfPossible(originalValue));
                                                break;
                                            case 2:
                                                nominalValue = (String)attributeValueMaps[i].get(originalValue);
                                                if (nominalValue != null) {
                                                    value = (double)attributes[i].getMapping().mapString(nominalValue);
                                                } else {
                                                    value = (double)attributes[i].getMapping().mapString(Tools.formatIntegerIfPossible(originalValue));
                                                }
                                                break;
                                            case 3:
                                                nominalValue = (String)attributeValueMaps[i].get(originalValue);
                                                if (nominalValue != null) {
                                                    value = (double)attributes[i].getMapping().mapString(nominalValue);
                                                } else {
                                                    value = 0.0D / 0.0;
                                                }
                                        }

                                        dataRow.set(attributes[i], value);
                                    }
                                }
                            }
                        }

                        return exampleSet;
                    }

                    this.read(inputStream, buffer, expansionFieldContentsLength);
                }
            }
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type = new ParameterTypeCategory("attribute_naming_mode", "Determines which variable properties should be used for attribute naming.", ATTRIBUTE_NAMING_MODES, 0);
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeCategory("handle_value_labels", "Specifies how to handle attributes with value labels, i.e. whether to ignore the labels or how to use them.", HANDLE_VALUE_LABELS_MODES, 2);
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeDouble("sample_ratio", "The fraction of the data set which should be read (1 = all; only used if sample_size = -1)", 0.0D, 1.0D, 1.0D);
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeInt("sample_size", "The exact number of samples which should be read (-1 = all; if not -1, sample_ratio will not have any effect)", -1, 2147483647, -1);
        type.setExpert(true);
        types.add(type);
        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
        return types;
    }
}
