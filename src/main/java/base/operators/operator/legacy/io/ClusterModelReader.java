package base.operators.operator.legacy.io;

import base.operators.operator.IOObject;
import base.operators.operator.OperatorCreationException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.clustering.ClusterModel;
import base.operators.operator.io.AbstractReader;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeFile;
import base.operators.tools.OperatorService;
import java.util.List;

public class ClusterModelReader
        extends AbstractReader<ClusterModel>
{
    public static final String PARAMETER_CLUSTER_MODEL_FILE = "cluster_model_file";
    public static final String PARAMETER_IS_HIERARCHICAL_MODEL_FILE = "is_hierarchical_model_file";

    public ClusterModelReader(OperatorDescription description) { super(description, ClusterModel.class); }

    @Override
    public ClusterModel read() throws OperatorException {
        try {
            IOObjectReader ioReader = (IOObjectReader)OperatorService.createOperator(IOObjectReader.class);
            ioReader.setParameter("object_file", getParameterAsString("cluster_model_file"));
            if (getParameterAsBoolean("is_hierarchical_model_file")) {
                ioReader.setParameter("io_object", "HierarchicalClusterModel");
            } else {
                ioReader.setParameter("io_object", "ClusterModel");
            }
            return (ClusterModel)ioReader.read();
        } catch (OperatorCreationException e) {
            throw new OperatorException("Cannot create IOObjectReader");
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("cluster_model_file", "the file from which the cluster model is read", "clm", false));

        types.add(new ParameterTypeBoolean("is_hierarchical_model_file", "indicates that the stored model file is a hierarchical cluster model", false));

        return types;
    }
}
