package base.operators.operator.features.meta;

import base.operators.example.ExampleSet;
import base.operators.operator.features.FeatureSetIOObject;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.MetaData;

public class ApplyFeatureSetOutputRule implements MDTransformationRule {
    private InputPort dataInput;
    private InputPort featureSetInput;
    private OutputPort dataOutput;
    private OutputPort featureSetOutput;

    public ApplyFeatureSetOutputRule(InputPort dataInput, InputPort featureSetInput, OutputPort dataOutput, OutputPort featureSetOutput) {
        this.dataInput = dataInput;
        this.featureSetInput = featureSetInput;
        this.dataOutput = dataOutput;
        this.featureSetOutput = featureSetOutput;
    }

    @Override
    public void transformMD() {
        if (this.dataInput.isConnected() && this.dataInput.getMetaData() != null && ExampleSet.class.isAssignableFrom(this.dataInput.getMetaData().getObjectClass()) && this.featureSetInput.isConnected() && this.featureSetInput.getMetaData() != null && FeatureSetIOObject.class.isAssignableFrom(this.featureSetInput.getMetaData().getObjectClass())) {
            MetaData dataMD = new ExampleSetMetaData();
            if (this.dataInput.getMetaData() instanceof ExampleSetMetaData && this.featureSetInput.getMetaData() instanceof FeatureSetMetaData) {
                dataMD = this.modifyMetaData((ExampleSetMetaData)this.dataInput.getMetaData(), (FeatureSetMetaData)this.featureSetInput.getMetaData());
            }

            ((MetaData)dataMD).addToHistory(this.dataOutput);
            this.dataOutput.deliverMD((MetaData)dataMD);
            MetaData featureSetMD = this.featureSetInput.getMetaData().clone();
            featureSetMD.addToHistory(this.featureSetOutput);
            this.featureSetOutput.deliverMD(featureSetMD);
        } else {
            this.dataOutput.deliverMD((MetaData)null);
            this.featureSetOutput.deliverMD((MetaData)null);
        }
    }

    private MetaData modifyMetaData(ExampleSetMetaData dataMD, FeatureSetMetaData featureSetMD) {
        return featureSetMD.transformExampleSetMetaData(dataMD);
    }
}
