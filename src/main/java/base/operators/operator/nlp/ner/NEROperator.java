package base.operators.operator.nlp.ner;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.tools.Ontology;
import nlp.core.ner.NerLocSingleton;
import nlp.core.ner.NerOrgSingleton;
import nlp.core.ner.NerPsnSingleton;

import java.util.List;

/**
 * @author zls
 * create time:  2019.07.19.
 * description:
 */
public class NEROperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String PARAMETER_SELECT_COLUMN = "select_column_name";

    //识别列表
    public static final String PARAMETER_IS_PERSON = "detect_person";
    public static final String PARAMETER_IS_LOCATION = "detect_location";
    public static final String PARAMETER_IS_ORGANIZATION = "detect_organization";

    public NEROperator(OperatorDescription description){
        super(description);
            exampleSetInput.addPrecondition(
                    new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                            this, PARAMETER_SELECT_COLUMN)));
    }

    @Override
    public void doWork() throws OperatorException {

        ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
        Attributes attributes = exampleSet.getAttributes();

        String selectColumn = getParameterAsString(PARAMETER_SELECT_COLUMN);
        Attribute selected = attributes.get(selectColumn);


        Boolean isPer = getParameterAsBoolean(PARAMETER_IS_PERSON);
        Boolean isLoc = getParameterAsBoolean(PARAMETER_IS_LOCATION);
        Boolean isOrg = getParameterAsBoolean(PARAMETER_IS_ORGANIZATION);


        NerPsnSingleton psnSingleton = null;
        NerLocSingleton locSingleton = null;
        NerOrgSingleton orgSingleton = null;

        Attribute predictedPer = null;
        Attribute predictedLoc = null;
        Attribute predictedOrg = null;


        ExampleTable table = exampleSet.getExampleTable();

        if(isPer){
            psnSingleton = NerPsnSingleton.getSingleton();
            String name = "person";
            predictedPer = AttributeFactory.createAttribute(name,  Ontology.STRING);
            table.addAttribute(predictedPer);
            exampleSet.getAttributes().addRegular(predictedPer);

        }
        if(isLoc){
            locSingleton = NerLocSingleton.getSingleton();
            String name = "location";
            predictedLoc = AttributeFactory.createAttribute(name,  Ontology.STRING);
            table.addAttribute(predictedLoc);
            exampleSet.getAttributes().addRegular(predictedLoc);
        }
        if(isOrg){
            orgSingleton = NerOrgSingleton.getSingleton();
            String name = "organization";
            predictedOrg = AttributeFactory.createAttribute(name,  Ontology.STRING);
            table.addAttribute(predictedOrg);
            exampleSet.getAttributes().addRegular(predictedOrg);
        }

        for(Example example:exampleSet){
            String doc = example.getValueAsString(selected);
            if(isPer){
                example.setValue(predictedPer, predictedPer.getMapping().mapString(psnSingleton.nerJson(doc).toString()));
            }
            if(isLoc){
                example.setValue(predictedLoc, predictedLoc.getMapping().mapString(locSingleton.nerJson(doc).toString()));
            }
            if(isOrg){
                example.setValue(predictedOrg, predictedOrg.getMapping().mapString(orgSingleton.nerJson(doc).toString()));
            }

        }
        exampleSetOutput.deliver(exampleSet);

    }


    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(PARAMETER_SELECT_COLUMN, "The name of the attribute to detect.", exampleSetInput,
                false));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_PERSON, "whether to detect person ", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_LOCATION, "whether to detect location", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_ORGANIZATION, "whether to detect organization", true));

        return types;
    }
}
