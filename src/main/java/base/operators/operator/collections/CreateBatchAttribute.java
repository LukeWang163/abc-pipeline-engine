package base.operators.operator.collections;

import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.PassThroughRule;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.math.container.Range;
import java.util.Iterator;
import java.util.List;

public class CreateBatchAttribute extends Operator {
    private static final String DATA_INPUT_PORT_NAME = "example set";
    private static final String DATA_OUTPUT_PORT_NAME = "example set";
    private static final String PARAMETER_BATCH_ATTRIBUTE_NAME = "batch attribute name";
    private static final String PARAMETER_NUMBER_OF_BATCHES = "number of batches";
    private InputPort dataInputPort = (InputPort)this.getInputPorts().createPort("example set");
    private OutputPort dataOutputPort = (OutputPort)this.getOutputPorts().createPort("example set");

    public CreateBatchAttribute(OperatorDescription description) {
        super(description);
        this.getTransformer().addRule(new PassThroughRule(this.dataInputPort, this.dataOutputPort, true) {
            @Override
            public MetaData modifyMetaData(MetaData md) {
                if (md instanceof ExampleSetMetaData) {
                    ExampleSetMetaData metaData = (ExampleSetMetaData)md;

                    try {
                        AttributeMetaData amd = new AttributeMetaData(CreateBatchAttribute.this.getParameterAsString("batch attribute name"), 3, CreateBatchAttribute.this.getParameterAsString("batch attribute name"));
                        amd.setValueRange(new Range(1.0D, (double)CreateBatchAttribute.this.getParameterAsInt("number of batches")), SetRelation.EQUAL);
                        metaData.addAttribute(amd);
                        return metaData;
                    } catch (UndefinedParameterError var4) {
                        return metaData;
                    }
                } else {
                    return md;
                }
            }
        });
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet data = (ExampleSet)this.dataInputPort.getData(ExampleSet.class);
        String batchName = this.getParameterAsString("batch attribute name");
        Attribute batchAttribute = AttributeFactory.createAttribute(batchName, 3);
        data.getExampleTable().addAttribute(batchAttribute);
        AttributeRole batchRole = new AttributeRole(batchAttribute);
        batchRole.setSpecial(batchName);
        data.getAttributes().add(batchRole);
        int numberOfBatches = this.getParameterAsInt("number of batches");
        int counter = 0;

        for(Iterator var7 = data.iterator(); var7.hasNext(); ++counter) {
            Example example = (Example)var7.next();
            int batch = counter % numberOfBatches + 1;
            example.setValue(batchAttribute, (double)batch);
        }

        this.dataOutputPort.deliver(data);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeString("batch attribute name", "The name of the newly created batch attribute.  This name is also used as the special role.", "batch", false));
        types.add(new ParameterTypeInt("number of batches", "The desired number of batches.", 2, 2147483647, 2));
        return types;
    }
}
