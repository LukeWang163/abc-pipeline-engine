package base.operators.operator.features.meta;

import base.operators.example.ExampleSet;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.metadata.AbstractPrecondition;
import base.operators.operator.ports.metadata.CompatibilityLevel;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.InputMissingMetaDataError;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.MetaDataInfo;

public class TrainingDataPrecondition extends AbstractPrecondition {
    public TrainingDataPrecondition(InputPort inputPort) {
        super(inputPort);
    }

    @Override
    public void check(MetaData metaData) {
        InputPort inputPort = this.getInputPort();
        if (metaData == null) {
            inputPort.addError(new InputMissingMetaDataError(inputPort, ExampleSet.class, (Class)null));
        } else if (metaData instanceof ExampleSetMetaData) {
            ExampleSetMetaData emd = (ExampleSetMetaData)metaData;
            MetaDataInfo has = emd.hasSpecial("label");
            switch(has) {
                case NO:
                    this.createError(Severity.ERROR, "special_missing", new Object[]{"label"});
                    break;
                case UNKNOWN:
                    this.createError(Severity.WARNING, "special_unknown", new Object[]{"label"});
                case YES:
            }
        }

    }

    @Override
    public void assumeSatisfied() {
        this.getInputPort().receiveMD(new ExampleSetMetaData());
    }

    @Override
    public String getDescription() {
        return "<em>expects:</em> ExampleSet";
    }

    @Override
    public boolean isCompatible(MetaData input, CompatibilityLevel level) {
        return null != input && ExampleSet.class.isAssignableFrom(input.getObjectClass());
    }

    @Override
    public MetaData getExpectedMetaData() {
        return new ExampleSetMetaData();
    }
}
