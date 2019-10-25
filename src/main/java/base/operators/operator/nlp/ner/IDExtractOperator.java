package base.operators.operator.nlp.ner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeString;
import base.operators.tools.Ontology;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zls
 * create time:  2019.07.12.
 * description:
 */
public class IDExtractOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String PARAMETER_SELECT_COLUMN = "select_column_name";
    public static final String PARAMETER_RESULT_COLUMN = "result_column_name";

    //识别列表
    public static final String PARAMETER_IS_URL = "extract_URL";
    public static final String PARAMETER_IS_PHONE = "extract_phone_number";
    public static final String PARAMETER_IS_DTAE = "extract_date";
    public static final String PARAMETER_IS_TIME = "extract_time";
    public static final String PARAMETER_IS_CAR = "extract_car_number";
    public static final String PARAMETER_IS_PER = "extract_fractional_number";
    public static final String PARAMETER_IS_NUMBER = "extract_number";
    public static final String PARAMETER_IS_LETTER = "extract_letter";
    public static final String PARAMETER_IS_ID = "extract_ID_number";
    public static final String PARAMETER_IS_EMAIL = "extract_Email";
    public static final String PARAMETER_IS_BANK = "extract_bankcard_number";
    public static final String PARAMETER_IS_QQ = "extract_QQ";
    public static final String PARAMETER_IS_IP = "extract_IP";




    public IDExtractOperator(OperatorDescription description){
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

        String newName = getParameterAsString(PARAMETER_RESULT_COLUMN);
        Attribute targetAttribute = AttributeFactory
                .createAttribute(newName, Ontology.STRING);
        targetAttribute.setTableIndex(attributes.size());
        exampleSet.getExampleTable().addAttribute(targetAttribute);
        attributes.addRegular(targetAttribute);

        for(Example example:exampleSet){
            String doc = example.getValueAsString(selected);
            String result = extractText(doc);
            example.setValue(targetAttribute, targetAttribute.getMapping().mapString(result));

        }
        exampleSetOutput.deliver(exampleSet);

    }





    public String extractText(String text){
        Map<String, Map<Integer, String>> hMap = new HashMap<>();
        if(getParameterAsBoolean(PARAMETER_IS_URL)){
            Map<Integer, String> map = IDExtract.getURL(text);
            hMap.put("URL", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_PHONE)){
            Map<Integer, String> map = IDExtract.getPhone(text);
            hMap.put("phone_num", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_DTAE)){
            Map<Integer, String> map = IDExtract.getDate(text);
            hMap.put("date", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_TIME)){
            Map<Integer, String> map = IDExtract.getTime(text);
            hMap.put("time", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_CAR)){
            Map<Integer, String> map = IDExtract.getCarNum(text);
            hMap.put("car_num", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_PER)){
            Map<Integer, String> map = IDExtract.getPerNum(text);
            hMap.put("per_num", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_NUMBER)){
            Map<Integer, String> map = IDExtract.getNumbers(text);
            hMap.put("numbers", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_ID)){
            Map<Integer, String> map = IDExtract.getLetters(text);
            hMap.put("letters", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_LETTER)){
            Map<Integer, String> map = IDExtract.getIDCard(text);
            hMap.put("ID_card", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_EMAIL)){
            Map<Integer, String> map = IDExtract.getEmail(text);
            hMap.put("email", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_BANK)){
            Map<Integer, String> map = IDExtract.getBankCard(text);
            hMap.put("bank_card", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_QQ)){
            Map<Integer, String> map = IDExtract.getQQ(text);
            hMap.put("QQ", map);
        }
        if(getParameterAsBoolean(PARAMETER_IS_IP)){
            Map<Integer, String> map = IDExtract.getIPAddr(text);
            hMap.put("IP_addr", map);
        }

        JSONObject jsonOrgObject = new JSONObject();
        JSONArray jsonOrgArray = new JSONArray();

        hMap.forEach((k, v) -> {

            v.forEach((num, str) ->{
                JSONObject wordJsonObj = new JSONObject();
                wordJsonObj.put("word", str);
                wordJsonObj.put("begin", num);
                wordJsonObj.put("end", num + str.length());
                wordJsonObj.put("type", k);
                jsonOrgArray.add(wordJsonObj);

            });

        });
        jsonOrgObject.put("words", jsonOrgArray);
        return jsonOrgObject.toString();

    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(PARAMETER_SELECT_COLUMN, "The name of the attribute to extract.", exampleSetInput,
                false));
        types.add(new ParameterTypeString(PARAMETER_RESULT_COLUMN, "The result of the attribute.", false));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_URL, "whether to extract URL", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_PHONE, "whether to extract phone number", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_DTAE, "whether to extract date", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_TIME, "whether to extract time", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_CAR, "whether to extract car number", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_PER, "whether to extract fractional number", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_NUMBER, "whether to extract number", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_ID, "whether to extract ID number", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_LETTER, "whether to extract letters", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_EMAIL, "whether to extract Emails", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_BANK, "whether to extract bankcard number", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_QQ, "whether to extract QQ", true));
        types.add(new ParameterTypeBoolean(PARAMETER_IS_IP, "whether to extract IP", true));


        return types;
    }
}
