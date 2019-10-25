package base.operators.operator.io;

import base.operators.example.ExampleSet;
import base.operators.operator.IOObject;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeString;
import base.operators.utils.ParquetExampleSourceUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class ParquetExampleSetWriter extends AbstractExampleSetWriter {
    public static final String PARAMETER_PARQUET_FILE = "parquet_file";
    public static final String PARAMETER_STORAGE_TYPE = "storage_type";

    public ParquetExampleSetWriter(OperatorDescription description) {
        super(description);
    }

    @Override
    public ExampleSet write(ExampleSet exampleSet) throws OperatorException {
        String parquetFileName = getParameterAsString(PARAMETER_PARQUET_FILE);
        Boolean toLocal = false || !"HDFS".equals(getParameterAsString(PARAMETER_STORAGE_TYPE));

        try {
            ParquetExampleSourceUtil.writeToParquet(exampleSet, parquetFileName, toLocal);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exampleSet;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList();
        ParameterTypeString type = new ParameterTypeString(PARAMETER_PARQUET_FILE, "The path to the Parquet file", null, false);
        types.add(type);
        types.add(new ParameterTypeCategory(PARAMETER_STORAGE_TYPE, "Storage type for the data files", new String[]{"HDFS", "Local"}, 1, false));
        types.addAll(super.getParameterTypes());
        return types;
    }
}
