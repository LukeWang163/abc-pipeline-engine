package base.operators.operator.preprocessing.meta;

import base.operators.example.ExampleSet;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDTransformationRule;
import base.operators.operator.ports.metadata.MetaData;

public class QualityOutputRule implements MDTransformationRule {
    private InputPort dataInput;
    private OutputPort dataOutput;

    public QualityOutputRule(InputPort dataInput, OutputPort dataOutput) {
        this.dataInput = dataInput;
        this.dataOutput = dataOutput;
    }

    @Override
    public void transformMD() {
        if (this.dataInput.isConnected() && this.dataInput.getMetaData() != null && ExampleSet.class.isAssignableFrom(this.dataInput.getMetaData().getObjectClass())) {
            ExampleSetMetaData inputMD = (ExampleSetMetaData)this.dataInput.getMetaData();
            ExampleSetMetaData outputMD = new ExampleSetMetaData(ExampleSet.class);
            outputMD.setNumberOfExamples(inputMD.getNumberOfRegularAttributes());
            outputMD.addAttribute(new AttributeMetaData("Attribute", 1));
            if (inputMD.getAttributeByRole("label") != null) {
                outputMD.addAttribute(new AttributeMetaData("Correlation", 4));
            }

            outputMD.addAttribute(new AttributeMetaData("ID-ness", 4));
            outputMD.addAttribute(new AttributeMetaData("Stabillity", 4));
            outputMD.addAttribute(new AttributeMetaData("Missing", 4));
            outputMD.addAttribute(new AttributeMetaData("Text-ness", 4));
            outputMD.addToHistory(this.dataOutput);
            this.dataOutput.deliverMD(outputMD);
        } else {
            this.dataOutput.deliverMD((MetaData)null);
        }
    }

    public OutputPort getOutputPort() {
        return this.dataOutput;
    }
}
