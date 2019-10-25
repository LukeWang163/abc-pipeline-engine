package base.operators.operator.nlp.pyconvert;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.pyconvert.cn2py.ChineseConvertPinyin;
import base.operators.operator.nlp.pyconvert.py2cn.PinyinConvertChinese;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wangpanpan on 2019/8/01.
 */
public class PyconvertOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String SELECT_ATTRIBUTE_AND_TYPE = "select_attribute_and_type";
    public static final String CONVERT_TYPE = "convert_type";
    public static final String ATTRIBUTE_NAME = "attribute_name";

    public static String[] TYPES_EN = {"transliteration of Chinese Characters into Pinyin with phonetic alphabet",
            "transliteration of Chinese Characters into Pinyin with number phonetic alphabet",
            "transliteration of Chinese Characters into Pinyin without phonetic alphabet",
            "transliteration of Pinyin into Chinese Characters (Pinyin without spaces)",
            "transliteration of Pinyin into Chinese Characters (Pinyin with spaces)"};
//    public static HashMap<String, Integer > TYPES_MAP = new HashMap<String, Integer>(){{
//        put(TYPES_EN[0],0);
//        put(TYPES_EN[1],1);
//        put(TYPES_EN[2],2);
//        put(TYPES_EN[3],3);
//        put(TYPES_EN[4],4);
//    }};
    public static String[] TYPES_CH = {"汉字转拼音有音标","汉字转拼音数字音标","汉字转拼音无音标","拼音转汉字(拼音无空格)","拼音转汉字(拼音有空格)"};

    public PyconvertOperator(OperatorDescription description){
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
    public void doWork() throws OperatorException {
        List<String[]> selected_column = getParameterList(SELECT_ATTRIBUTE_AND_TYPE);
        //0-汉字转拼音有音标  ChineseConvertPinyin.WITH_TONE_MARK
        //1-汉字转拼音数字音标  ChineseConvertPinyin.WITH_TONE_NUMBER
        //2-汉字转拼音无音标  ChineseConvertPinyin.WITHOUT_TONE
        //3-拼音转汉字（拼音无空格） PinyinConvertChinese.pth
        //4-拼音转汉字（拼音有空格） PinyinConvertChinese.pth
        //获取拼音转汉字的工具类（一个工具类，拼音是否带空格，作为方法的入参）
        PinyinConvertChinese py2cnConvert = PinyinConvertChinese.getInstance(PinyinConvertChinese.pth);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        ExampleTable table = exampleSet.getExampleTable();

        if(isParameterSet(SELECT_ATTRIBUTE_AND_TYPE)){
            List<Attribute> rawAttributes = new ArrayList<>();
            List<Integer> types = new ArrayList<>();
            List<Attribute> addAttributes = new ArrayList<>();
            for (int j = 0; j < selected_column.size(); j++) {
                types.add(Integer.valueOf(selected_column.get(j)[1]));
                rawAttributes.add(exampleSet.getAttributes().get(selected_column.get(j)[0]));
                Attribute newAttribute = AttributeFactory.createAttribute(selected_column.get(j)[0]+"_pyconvert", Ontology.STRING);
                table.addAttribute(newAttribute);
                exampleSet.getAttributes().addRegular(newAttribute);
                addAttributes.add(newAttribute);
            }

            for (int i = 0; i < exampleSet.size(); i++) {
                Example example = exampleSet.getExample(i);
                for (int ii = 0; ii < addAttributes.size(); ii++) {
                    int convert_type = types.get(ii);
                    //获取汉字转拼音类型的不同获取不同的工具类
                    ChineseConvertPinyin cn2pyConvert = null;
                    String raw = example.getValueAsString(rawAttributes.get(ii));
                    String[] fragments = raw.split("\r\n");//分段，因为转换工具不能处理含回车换行的文本
                    int fragmentNum = fragments.length;
                    StringBuffer cc_result = new StringBuffer();
                    int index = 0;
                    for(String cp : fragments){
                        String cp_result = "";
                        if(3 == convert_type){
                            cp = cp.replace(" ","");
                            cp_result = py2cnConvert.convert(cp, false);
                        }else if(4 == convert_type){
                            cp_result = py2cnConvert.convert(cp, true);
                        }else if(0 == convert_type){
                            cn2pyConvert = ChineseConvertPinyin.getInstance(ChineseConvertPinyin.WITH_TONE_MARK);
                            cp_result = cn2pyConvert.convert(cp);
                        }else if(1 == convert_type){
                            cn2pyConvert = ChineseConvertPinyin.getInstance(ChineseConvertPinyin.WITH_TONE_NUMBER);
                            cp_result = cn2pyConvert.convert(cp);
                        }else if(2 == convert_type){
                            cn2pyConvert = ChineseConvertPinyin.getInstance(ChineseConvertPinyin.WITHOUT_TONE);
                            cp_result = cn2pyConvert.convert(cp);
                        }
                        cc_result.append(cp_result);
                        if(index < fragmentNum-1){
                            cc_result.append("\r\n");
                        }
                    }
                    example.setValue(addAttributes.get(ii), addAttributes.get(ii).getMapping().mapString(cc_result.toString()));
                }
            }
        }

        exampleSetOutput.deliver(exampleSet);
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();

        types.add(new ParameterTypeList(SELECT_ATTRIBUTE_AND_TYPE, "This parameter defines attributes and convert type.",
                new ParameterTypeAttribute(ATTRIBUTE_NAME, "The name of the attribute.", exampleSetInput, false, false),
                new ParameterTypeCategory(CONVERT_TYPE, "The type of convert.", TYPES_EN, 0, false), false));

        return types;
    }

}
