package base.operators.operator.features;

import base.operators.example.ExampleSet;
import base.operators.operator.features.meta.ApplyFeatureSetOutputRule;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.conditions.BooleanParameterCondition;
import java.util.List;

public class ApplyFeatureSetOperator extends Operator {
    public static final String DATA_INPUT_PORT_NAME = "example set";
    public static final String FEATURE_SET_INPUT_PORT_NAME = "feature set";
    public static final String DATA_OUTPUT_PORT_NAME = "example set";
    public static final String FEATURE_SET_OUTPUT_PORT_NAME = "feature set";
    public static final String PARAMETER_HANDLE_MISSINGS = "handle missings";
    public static final String PARAMETER_KEEP_ORIGINALS = "keep originals";
    public static final String PARAMETER_ORIGINALS_SPECIAL_ROLE = "originals special role";
    public static final String PARAMETER_RECREATE_MISSING_ATTRIBUTES = "recreate missing attributes";
    private InputPort dataInputPort = this.getInputPorts().createPort("example set", ExampleSet.class);
    private InputPort featureSetInputPort = this.getInputPorts().createPort("feature set", FeatureSetIOObject.class);
    private OutputPort dataOutputPort = (OutputPort)this.getOutputPorts().createPort("example set");
    private OutputPort featureSetOutputPort = (OutputPort)this.getOutputPorts().createPort("feature set");

    public ApplyFeatureSetOperator(OperatorDescription description) {
        super(description);
        ApplyFeatureSetOutputRule rule = new ApplyFeatureSetOutputRule(this.dataInputPort, this.featureSetInputPort, this.dataOutputPort, this.featureSetOutputPort);
        this.getTransformer().addRule(rule);
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet data = (ExampleSet)this.dataInputPort.getData(ExampleSet.class);
        FeatureSetIOObject featureSetObject = (FeatureSetIOObject)this.featureSetInputPort.getData(FeatureSetIOObject.class);
        FeatureSet featureSet = featureSetObject.getFeatureSet();
        ExampleSet transformed = featureSet.apply(data, this.getParameterAsBoolean("handle missings"), this.getParameterAsBoolean("keep originals"), this.getParameterAsBoolean("originals special role"), this.getParameterAsBoolean("recreate missing attributes"));
        this.dataOutputPort.deliver(transformed);
        this.featureSetOutputPort.deliver(featureSetObject);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeBoolean("handle missings", "Indicates if missing and infinite values should be automatically replaced.", true, true));
        ParameterType type = new ParameterTypeBoolean("keep originals", "Indicates if attributes in the data which are not part of the feature set should be still kept.", false, true);
        types.add(type);
        type = new ParameterTypeBoolean("originals special role", "Indicates if original attributes which are kept should get a special role instead of regular so that they are not used by machine learning operators.", true, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "keep originals", false, true));
        types.add(type);
        types.add(new ParameterTypeBoolean("recreate missing attributes", "Indicates if original columns et but are missing in the data should be recreated (using mean / mode values).", true, true));
        return types;
    }
}
