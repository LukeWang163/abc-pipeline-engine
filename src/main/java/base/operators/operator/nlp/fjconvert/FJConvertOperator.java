package base.operators.operator.nlp.fjconvert;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.fjconvert.core.ConvertInterface;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by zhangxian on 2019/3/14.
 */
public class FJConvertOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static String[] TYPES_CH = { "台湾繁体转简体", "大陆繁体转简体", "简体转大陆繁体", "简体转台湾繁体", "大陆繁体转台湾繁体"};
    public static String[] TYPES_EN = {"translating Taiwanese traditional into simplified",
            "translating traditional Chinese into simplified Chinese in mainland China",
            "translating simplified into traditional Chinese in mainland China",
            "translating simplified into Taiwanese Traditional",
            "translating traditional Chinese in mainland China into Taiwanese traditional"};

    public static HashMap<Integer, String > TYPES_MAP = new HashMap<Integer, String>(){{
        put(0,TYPES_CH[0]);
        put(1,TYPES_CH[1]);
        put(2,TYPES_CH[2]);
        put(3,TYPES_CH[3]);
        put(4,TYPES_CH[4]);
    }};
    public static final String SELECT_ATTRIBUTE_AND_TYPE = "select_attribute_and_type";
    public static final String CONVERT_TYPE = "convert_type";
    public static final String ATTRIBUTE_NAME = "attribute_name";
    public FJConvertOperator(OperatorDescription description){
        super(description);
        try {
            List<String[]> selected_column = getParameterList(SELECT_ATTRIBUTE_AND_TYPE);
            for (int i = 0; i < selected_column.size(); i++) {
                exampleSetInput.addPrecondition(
                        new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                                this, selected_column.get(i)[0])));
            }
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }

    }

    @Override
    public void doWork() throws OperatorException {
        List<String[]> selected_column = getParameterList(SELECT_ATTRIBUTE_AND_TYPE);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        ExampleTable table = exampleSet.getExampleTable();
        //繁简转换
        ConvertInterface convertInterface = new ConvertInterface();
        if(isParameterSet(SELECT_ATTRIBUTE_AND_TYPE)){
            List<Attribute> rawAttributes = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Attribute> addAttributes = new ArrayList<>();
            for (int j = 0; j < selected_column.size(); j++) {
                types.add(TYPES_MAP.get(Integer.valueOf(selected_column.get(j)[1])));
                rawAttributes.add(exampleSet.getAttributes().get(selected_column.get(j)[0]));
                Attribute newAttribute = AttributeFactory.createAttribute(selected_column.get(j)[0]+"_fjconvert", Ontology.STRING);
                table.addAttribute(newAttribute);
                exampleSet.getAttributes().addRegular(newAttribute);
                addAttributes.add(newAttribute);
            }

            for (int i = 0; i < exampleSet.size(); i++) {
                Example example = exampleSet.getExample(i);
                for (int ii = 0; ii < addAttributes.size(); ii++) {
                    String text =example.getValueAsString(rawAttributes.get(ii));
                    //繁简转换
                    String sConvert = convertInterface.convert(text, types.get(ii));
                    example.setValue(addAttributes.get(ii), addAttributes.get(ii).getMapping().mapString(sConvert));
                }
            }
        }
       exampleSetOutput.deliver(exampleSet);

    }
    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();

        types.add(new ParameterTypeList(SELECT_ATTRIBUTE_AND_TYPE, "This parameter defines attributes and convert type.",
                new ParameterTypeAttribute(ATTRIBUTE_NAME, "The name of the attribute.", exampleSetInput, false, false),
                new ParameterTypeCategory(CONVERT_TYPE, "The type of convert.", TYPES_EN, 0, false), false));

        return types;
    }
}
