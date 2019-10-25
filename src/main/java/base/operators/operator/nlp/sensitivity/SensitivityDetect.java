package base.operators.operator.nlp.sensitivity;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.parameter.*;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;


public class SensitivityDetect extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort dictExampleSetInput = getInputPorts().createPort("dict example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String SELECTED_COLUMN = "attribute_name";
    public static final String DICT_TYPE = "dict_type";
    public static final String DICT_SELECTED_COLUMN = "dict_attribute_name";
    public static String[] DICT_TYPES_EN = {"system dictionary","user dictionary","merge dictionary"};
    public static String[] DICT_TYPES_CH = {"系统词典","自定义词典","合并词典"};
    public SensitivityDetect(OperatorDescription description){
        super(description);
    }

    public void doWork() throws OperatorException {
        String[] selected_column = getParameterAsString(SELECTED_COLUMN).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        int dict_type = getParameterAsInt(DICT_TYPE);
        String dict_attribute_name = getParameterAsString(DICT_SELECTED_COLUMN);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        ExampleTable table = exampleSet.getExampleTable();

        List<Attribute> rawAttributes = new ArrayList<>();
        List<Attribute> addAttributes = new ArrayList<>();
        for (int j = 0; j < selected_column.length; j++) {
            if(!"".equals(selected_column[j])){
                rawAttributes.add(exampleSet.getAttributes().get(selected_column[j]));
                Attribute newAttribute = AttributeFactory.createAttribute(selected_column[j]+"_detect", Ontology.STRING);
                table.addAttribute(newAttribute);
                exampleSet.getAttributes().addRegular(newAttribute);
                addAttributes.add(newAttribute);
            }
        }
        ExampleSet dictExampleSet = null;
        Attribute dict_attribute = null;
        if(dictExampleSetInput.isConnected()){
            try{
                dict_attribute = dictExampleSet.getAttributes().get(dict_attribute_name);
                dictExampleSet = dictExampleSetInput.getData(ExampleSet.class);
            }catch(NullPointerException nullPointerException){
                nullPointerException.printStackTrace();
            }
        }


        Sensitivity sensitivity = Sensitivity.getInstance(dict_type, dictExampleSet, dict_attribute, true);

        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            for (int ii = 0; ii < addAttributes.size(); ii++) {
                String raw = example.getValueAsString(rawAttributes.get(ii));
                String isSensitivity = sensitivity.detect(raw);
                example.setValue(addAttributes.get(ii), addAttributes.get(ii).getMapping().mapString(isSensitivity));
            }
        }
        exampleSetOutput.deliver(exampleSet);

    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttributes(SELECTED_COLUMN, "The name of the document attribute.",exampleSetInput));
        types.add(new ParameterTypeCategory(DICT_TYPE, "The type of dict.",
                DICT_TYPES_EN, 0, false));

        ParameterType type = new ParameterTypeString(DICT_SELECTED_COLUMN, "The name of the dict attribute.");
        type.registerDependencyCondition(new EqualStringCondition(this, DICT_TYPE, false, DICT_TYPES_EN[1],DICT_TYPES_EN[2]));
        types.add(type);

        return types;
    }

}