package base.operators.operator.scripting;

import base.operators.operator.nio.NewCSVExampleSource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nio.CSVExampleSource;
import base.operators.operator.nio.model.ColumnMetaData;
import base.operators.operator.nio.model.DataResultSet;
import base.operators.operator.nio.model.DataResultSetTranslationConfiguration;
import base.operators.operator.nio.model.DataResultSetTranslator;
import base.operators.tools.Ontology;
import base.operators.tools.OperatorService;
import base.operators.tools.ProgressListener;
import base.operators.tools.StrictDecimalFormat;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class ScriptingCSVExampleSource extends CSVExampleSource {
    private DecimalFormat specialDecimalFormat;
    private List<String[]> metadata;

    public ScriptingCSVExampleSource() {
        super(getCSVExampleSourceDescription());
    }

    private static OperatorDescription getCSVExampleSourceDescription() {
        OperatorDescription descriptions = new OperatorDescription("data_access.files.read", "new_read_csv", NewCSVExampleSource.class, Thread.currentThread().getContextClassLoader(), "inbox_into.png", null, null);
        return descriptions;
    }

    @Override
    protected NumberFormat getNumberFormat() throws OperatorException {
        return (NumberFormat)(this.specialDecimalFormat == null ? StrictDecimalFormat.getInstance(this, true) : this.specialDecimalFormat);
    }

    public void setNumberFormat(DecimalFormat decimalFormat) {
        this.specialDecimalFormat = decimalFormat;
    }

    @Override
    protected ExampleSet transformDataResultSet(DataResultSet dataResultSet) throws OperatorException {
        DataResultSetTranslationConfiguration configuration = new DataResultSetTranslationConfiguration(this);
        DataResultSetTranslator translator = new DataResultSetTranslator(this);
        NumberFormat numberFormat = this.getNumberFormat();
        if (numberFormat != null) {
            configuration.setNumberFormat(numberFormat);
        }

        ColumnMetaData[] columnMetaData = this.prepareMetaData(dataResultSet, this.metadata);
        if (columnMetaData != null) {
            configuration.setColumnMetaData(columnMetaData);
        } else if (!configuration.isComplete()) {
            configuration.reconfigure(dataResultSet);
            translator.guessValueTypes(configuration, dataResultSet, (ProgressListener)null);
        }

        return translator.read(dataResultSet, configuration, false, (ProgressListener)null);
    }

    public void setMetadata(List<String[]> metadata) {
        this.metadata = metadata;
    }

    public void readMetadataFromFile(File metadataFile) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            this.metadata = (List)mapper.readValue(metadataFile, new TypeReference<ArrayList<String[]>>() {});
        } catch (IOException var4) {
            this.getLogger().warning("Failed to read metadata");
            this.metadata = null;
        }

    }

    protected ColumnMetaData[] prepareMetaData(DataResultSet dataResultSet, List<String[]> metadata) {
        if (dataResultSet != null && metadata != null) {
            int numberOfColumns = dataResultSet.getNumberOfColumns();
            ColumnMetaData[] columnMetaData = new ColumnMetaData[numberOfColumns];
            String[] originalColumnNames = dataResultSet.getColumnNames();

            try {
                for(int i = 0; i < numberOfColumns; ++i) {
                    String[] typeRolePair = (String[])metadata.get(i);
                    String attributeRole = "attribute";
                    int valueType = 7;
                    if (typeRolePair != null) {
                        String valueTypeName = typeRolePair[0].trim();
                        valueType = Ontology.ATTRIBUTE_VALUE_TYPE.mapName(valueTypeName);
                        if (valueType < 0) {
                            valueType = 7;
                        }

                        attributeRole = typeRolePair[1].trim();
                    }

                    columnMetaData[i] = new ColumnMetaData(originalColumnNames[i], originalColumnNames[i], valueType, attributeRole, true);
                }

                return columnMetaData;
            } catch (IndexOutOfBoundsException var11) {
                return null;
            }
        } else {
            return null;
        }
    }
}
