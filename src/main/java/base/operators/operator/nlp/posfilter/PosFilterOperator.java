package base.operators.operator.nlp.posfilter;

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
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;

public class PosFilterOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String LANGUAGE = "language";
    public static final String PART_OF_SPEECH_TO_FILTER = "part_of_speech_to_filter";
    public static final String WHETHER_OUTPUT_IS_PART_OF_SPEECH_OR_NOT = "whether_output_is_part_of_speech_or_not";

    public static final String FILTERED_PART_OF_SPEECH = "filtered_part_of_speech";
    public static final String PART_OF_SPEECH = "part_of_speech";
    public static String[] PART_OF_SPEECH_LIST = {"标点符号_wp","助词_u","叹词_e"};

    public static String[] LANGUAGES = {"Chinese","English"};

    public PosFilterOperator(OperatorDescription description){
        super(description);

        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ATTRIBUTE_NAME)));
    }

    @Override
    public void doWork() throws OperatorException {
        String doc_column = getParameterAsString(DOC_ATTRIBUTE_NAME);
        int lang = getParameterAsInt(LANGUAGE);
        String[] filter = ParameterTypeEnumeration
                .transformString2Enumeration(getParameterAsString(PART_OF_SPEECH_TO_FILTER));
        List<String> posList = new ArrayList<String>();
        if(filter != null) {
            for (String per : filter){
                posList.add(per.split("_")[1]);
            }
        }
        boolean whetherSave = getParameterAsBoolean(WHETHER_OUTPUT_IS_PART_OF_SPEECH_OR_NOT);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        ExampleTable exampleTable = exampleSet.getExampleTable();
        Attribute filter_attribute = AttributeFactory.createAttribute(doc_column+"_posfilter", Ontology.STRING);
        exampleTable.addAttribute(filter_attribute);
        exampleSet.getAttributes().addRegular(filter_attribute);
        for(int i = 0; i < exampleSet.size(); i++){
            Example example = exampleSet.getExample(i);
            // 原子分词并将结果添加至新字段中
            String text = example.getValueAsString(attributes.get(doc_column));
            if(text != null && !"".equals(text)) {
                String[] strArray = text.split(" ");
                StringBuffer sb = new StringBuffer();

                for(int j=0; j < strArray.length; j++) {
                    String item = strArray[j];
                    if(0==lang) {
                        String[] array = item.split("/");
                        if(array.length == 2) {
                            if(!posList.contains(array[1])) {
                                if(!whetherSave) {
                                    sb.append(array[0] + " ");
                                } else {
                                    sb.append(array[0] + "/"  + array[1] + " ");
                                }
                            }
                        } else if(array.length == 1) {
                            sb.append(array[0] + " ");
                        }
                    } else if(1==lang) {
                        String[] array = item.split("_");
                        if(array.length == 2) {
                            if(!posList.contains(array[1])) {
                                if(!whetherSave) {
                                    sb.append(array[0] + " ");
                                } else {
                                    sb.append(array[0] + "_"  + array[1] + " ");
                                }
                            }
                        } else if(array.length == 1) {
                            sb.append(array[0] + " ");
                        }
                    }
                }
                example.setValue(filter_attribute, filter_attribute.getMapping().mapString(sb.toString()));
            } else {
                example.setValue(filter_attribute, filter_attribute.getMapping().mapString(""));
            }
        }
        exampleSetOutput.deliver(exampleSet);
    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeEnumeration(PART_OF_SPEECH_TO_FILTER, "", new ParameterTypeCategory(PART_OF_SPEECH,"The part of speech to filter.", PART_OF_SPEECH_LIST, 0), false));

        types.add(new ParameterTypeCategory(LANGUAGE, "The language of text.",
                LANGUAGES, 0, false));
        types.add(new ParameterTypeBoolean(WHETHER_OUTPUT_IS_PART_OF_SPEECH_OR_NOT, "whether output is part of speech or not", false, false));
        return types;
    }

}
