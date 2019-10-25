package base.operators.operator.io;

import base.operators.example.ExampleSet;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.parameter.*;
import base.operators.tools.parameter.internal.DataManagementParameterHelper;
import base.operators.utils.ParquetExampleSourceUtil;

import java.util.LinkedList;
import java.util.List;

public class ParquetExampleSource extends AbstractExampleSource{
    public static final String PARAMETER_PARQUET_FILE = "parquet_file";
    public static final String PARAMETER_STORAGE_TYPE = "storage_type";
    public static final String PARAMETER_INFER_METADATA = "infer_metadata";

    public ParquetExampleSource(OperatorDescription description) {
        super(description);
    }

    @Override
    public ExampleSet createExampleSet() throws OperatorException {
        String parquetFileName = getParameterAsString(PARAMETER_PARQUET_FILE);
        Boolean inferMetaData = getParameterAsBoolean(PARAMETER_INFER_METADATA);
        Boolean fromLocal = false || !"HDFS".equals(getParameterAsString(PARAMETER_STORAGE_TYPE));
        return ParquetExampleSourceUtil.readFromParquet(parquetFileName, inferMetaData, fromLocal);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList();
        ParameterTypeString type = new ParameterTypeString(PARAMETER_PARQUET_FILE, "The path to the Parquet file", null, false);
        types.add(type);
        types.add(new ParameterTypeCategory(PARAMETER_STORAGE_TYPE, "Storage type for the data files", new String[]{"HDFS", "Local"}, 1, false));
        types.add(new ParameterTypeBoolean(PARAMETER_INFER_METADATA, "Infer from existing metadata or not", true));
        types.addAll(super.getParameterTypes());
        return types;
    }
}
