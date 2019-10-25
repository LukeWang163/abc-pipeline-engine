package base.operators.operator.io;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.SparseFormatDataRowReader;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractExampleSource;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeChar;
import base.operators.parameter.ParameterTypeFile;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.ParameterTypeStringCategory;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.tools.ParameterService;
import base.operators.tools.Tools;
import base.operators.tools.att.AttributeSet;
import base.operators.tools.io.Encoding;
import base.operators.tools.parameter.internal.DataManagementParameterHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SparseFormatExampleSource extends AbstractExampleSource {
    public static final String PARAMETER_FORMAT = "format";
    public static final String PARAMETER_ATTRIBUTE_DESCRIPTION_FILE = "attribute_description_file";
    public static final String PARAMETER_DATA_FILE = "data_file";
    public static final String PARAMETER_LABEL_FILE = "label_file";
    public static final String PARAMETER_DIMENSION = "dimension";
    public static final String PARAMETER_SAMPLE_SIZE = "sample_size";
    public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";
    public static final String PARAMETER_DECIMAL_POINT_CHARACTER = "decimal_point_character";
    public static final String PARAMETER_PREFIX_MAP = "prefix_map";
    public static final String PARAMETER_USE_QUOTES = "use_quotes";
    public static final String PARAMETER_QUOTES_CHARACTER = "quotes_character";

    public SparseFormatExampleSource(OperatorDescription description) {
        super(description);
    }

    @Override
    public ExampleSet createExampleSet() throws OperatorException {
        int format = this.getParameterAsInt("format");
        Map<String, String> prefixMap = new HashMap();
        Iterator p = this.getParameterList("prefix_map").iterator();

        while(p.hasNext()) {
            String[] prefixMapping = (String[])p.next();
            prefixMap.put(prefixMapping[0], prefixMapping[1]);
        }

        File dataFile = this.getParameterAsFile("data_file");
        File attributeDescriptionFile = this.getParameterAsFile("attribute_description_file");
        AttributeSet attributeSet = null;
        Attribute attribute;
        if (attributeDescriptionFile != null) {
            try {
                attributeSet = new AttributeSet(attributeDescriptionFile, false, this);
            } catch (Throwable var29) {
                throw new UserError(this, var29, 302, new Object[]{attributeDescriptionFile, var29.getMessage()});
            }

            if (dataFile != null && attributeSet.getDefaultSource() != null && !dataFile.equals(attributeSet.getDefaultSource())) {
                this.logWarning("Attribute file names specified by parameter 'data_file' and default_source specified in '" + attributeDescriptionFile + "' do not match! Assuming the latter to be correct.");
            }

            if (format != 4 && attributeSet.getSpecialAttribute("label") == null) {
                throw new UserError(this, 917, new Object[0]);
            }

            this.log("Found " + attributeSet.getNumberOfRegularAttributes() + " regular attributes.");
            dataFile = attributeSet.getDefaultSource();
        } else {
            int dimension = this.getParameterAsInt("dimension");
            if (dimension < 0) {
                throw new UserError(this, 921);
            }

            attributeSet = new AttributeSet(dimension);

            for(int i = 0; i < dimension; ++i) {
                attribute = AttributeFactory.createAttribute(4);
                attributeSet.addAttribute(attribute);
            }

            Iterator m = prefixMap.values().iterator();

            while(m.hasNext()) {
                String specialName = (String)m.next();
                attributeSet.setSpecialAttribute(specialName, AttributeFactory.createAttribute(4));
            }

            if (format != 4) {
                attributeSet.setSpecialAttribute("label", AttributeFactory.createAttribute(1));
            }
        }

        if (dataFile == null) {
            throw new UserError(this, 902, new Object[0]);
        } else {
            Reader inData = null;
            BufferedReader inLabels = null;

            try {
                inData = Tools.getReader(dataFile, Encoding.getEncoding(this));
            } catch (IOException var28) {
                throw new UserError(this, var28, 302, new Object[]{dataFile, var28.getMessage()});
            }

            attribute = null;

            ExampleSet var13;
            try {
                if (format == 3) {
                    File labelFile = this.getParameterAsFile("label_file");
                    if (labelFile == null) {
                        throw new UserError(this, 201, new Object[]{"format", SparseFormatDataRowReader.FORMAT_NAMES[3], "label_file"});
                    }

                    try {
                        inLabels = Tools.getReader(labelFile, Encoding.getEncoding(this));
                    } catch (IOException var27) {
                        throw new UserError(this, var27, 302, new Object[]{labelFile, var27.getMessage()});
                    }
                }

                ExampleSetBuilder builder = ExampleSets.from(attributeSet.getAllAttributes());
                int datamanagement = this.getParameterAsInt("datamanagement");
                if (!Boolean.parseBoolean(ParameterService.getParameterValue("rapidminer.system.legacy_data_mgmt"))) {
                    datamanagement = 0;
                    builder.withOptimizationHint(DataManagementParameterHelper.getSelectedDataManagement(this));
                }

                SparseFormatDataRowReader reader = new SparseFormatDataRowReader(new DataRowFactory(datamanagement, this.getParameterAsString("decimal_point_character").charAt(0)), format, prefixMap, attributeSet, inData, inLabels, this.getParameterAsInt("sample_size"), this.getParameterAsBoolean("use_quotes"), this.getParameterAsChar("quotes_character"));
                builder.withDataRowReader(reader);
                attributeSet.getSpecialAttributes().entrySet().stream().forEach((entry) -> {
                    builder.withRole((Attribute)entry.getValue(), (String)entry.getKey());
                });
                var13 = builder.build();
            } finally {
                try {
                    inData.close();
                } catch (IOException var26) {
                    this.logError("Could not close reader for data: " + var26.getMessage());
                }

                if (inLabels != null) {
                    try {
                        inLabels.close();
                    } catch (IOException var25) {
                        this.logError("Could not close result set: " + var25.getMessage());
                    }
                }

            }

            return var13;
        }
    }

    @Override
    protected boolean supportsEncoding() {
        return true;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList();
        ParameterType type;
        type = new ParameterTypeCategory("format", "Format of the sparse data file.", SparseFormatDataRowReader.FORMAT_NAMES, 0);
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeFile("attribute_description_file", "Name of the attribute description file.", "aml", true);
        type.setExpert(false);
        types.add(type);
        types.add(new ParameterTypeFile("data_file", "Name of the data file. Only necessary if not specified in the attribute description file.", (String)null, true));
        types.add(new ParameterTypeFile("label_file", "Name of the data file containing the labels. Only necessary if format is 'format_separate_file'.", (String)null, true));
        types.add(new ParameterTypeInt("dimension", "Dimension of the example space. Only necessary if parameter 'attribute_description_file' is not set.", -1, 2147483647, -1));
        types.add(new ParameterTypeInt("sample_size", "The maximum number of examples to read from the data files (-1 = all)", -1, 2147483647, -1));
        types.add(new ParameterTypeBoolean("use_quotes", "Indicates if quotes should be regarded.", true));
        type = new ParameterTypeChar("quotes_character", "The quotes character.", '"', true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use_quotes", false, true));
        types.add(type);
        DataManagementParameterHelper.addParameterTypes(types, this);
        types.add(new ParameterTypeString("decimal_point_character", "Character that is used as decimal point.", "."));
        types.add(new ParameterTypeList("prefix_map", "Maps prefixes to names of special attributes.", new ParameterTypeString("prefix", "The prefix which represents a special attribute"), new ParameterTypeStringCategory("special_attribute", "Maps prefixes to names of special attributes.", Attributes.KNOWN_ATTRIBUTE_TYPES)));
        types.addAll(super.getParameterTypes());
        return types;
    }
}
