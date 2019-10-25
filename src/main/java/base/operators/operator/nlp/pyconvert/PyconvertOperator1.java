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
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangxian on 2019/3/28.
 */
public class PyconvertOperator1 extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String CONVERT_TYPE = "convert_type";
    public static final String DOC_ATTRIBUTE_NAMES = "doc_attribute_names";

    public static String[] TYPES = {"汉字转拼音有音标","汉字转拼音数字音标","汉字转拼音无音标","拼音转汉字(拼音无空格)","拼音转汉字(拼音有空格)"};

    public PyconvertOperator1(OperatorDescription description){
        super(description);
    }
    public void doWork() throws OperatorException {
        String[] selected_column = getParameterAsString(DOC_ATTRIBUTE_NAMES).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        int convert_type = getParameterAsInt(CONVERT_TYPE);
        //0-汉字转拼音有音标  ChineseConvertPinyin.WITH_TONE_MARK
        //1-汉字转拼音数字音标  ChineseConvertPinyin.WITH_TONE_NUMBER
        //2-汉字转拼音无音标  ChineseConvertPinyin.WITHOUT_TONE
        //3-拼音转汉字（拼音无空格） PinyinConvertChinese.pth
        //4-拼音转汉字（拼音有空格） PinyinConvertChinese.pth
        //获取拼音转汉字的工具类（一个工具类，拼音是否带空格，作为方法的入参）
        PinyinConvertChinese py2cnConvert = PinyinConvertChinese.getInstance(PinyinConvertChinese.pth);
        //获取汉字转拼音类型的不同获取不同的工具类
        ChineseConvertPinyin cn2pyConvert = null;
        if(0==convert_type){
            cn2pyConvert = ChineseConvertPinyin.getInstance(ChineseConvertPinyin.WITH_TONE_MARK);
        }else if(1==convert_type){
            cn2pyConvert = ChineseConvertPinyin.getInstance(ChineseConvertPinyin.WITH_TONE_NUMBER);
        }else if(2==convert_type){
            cn2pyConvert = ChineseConvertPinyin.getInstance(ChineseConvertPinyin.WITHOUT_TONE);
        }
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        ExampleTable table = exampleSet.getExampleTable();
        List<Attribute> rawAttributes = new ArrayList<>();
        List<Attribute> addAttributes = new ArrayList<>();
        for (int j = 0; j < selected_column.length; j++) {
            if(!"".equals(selected_column[j])){
                rawAttributes.add(exampleSet.getAttributes().get(selected_column[j]));
                Attribute newAttribute = AttributeFactory.createAttribute(selected_column[j]+"_pyconvert", Ontology.STRING);
                table.addAttribute(newAttribute);
                exampleSet.getAttributes().addRegular(newAttribute);
                addAttributes.add(newAttribute);
            }
        }

        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            for (int ii = 0; ii < addAttributes.size(); ii++) {
                String raw = example.getValueAsString(rawAttributes.get(ii));
                String[] fragments = raw.split("\r\n");//分段，因为转换工具不能处理含回车换行的文本
                int fragmentNum = fragments.length;
                StringBuffer cc_result = new StringBuffer();
                int index = 0;
                for(String cp : fragments){
                    String cp_result = "";
                    if(3==convert_type){
                        cp = cp.replace(" ","");
                        cp_result = py2cnConvert.convert(cp, false);
                    }else if(4==convert_type){
                        cp_result = py2cnConvert.convert(cp, true);
                    }else if(0==convert_type || 1==convert_type || 2==convert_type){
                        cp_result = cn2pyConvert.convert(cp);
                    }
                    cc_result.append(cp_result);
                    if(index < fragmentNum-1){
                        cc_result.append("\r\n");
                    }
                }
                example.setValue(addAttributes.get(ii), cc_result.toString());
            }
        }

        exampleSetOutput.deliver(exampleSet);
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttributes(DOC_ATTRIBUTE_NAMES, "The name of the document attribute.", exampleSetInput));
        types.add(new ParameterTypeCategory(CONVERT_TYPE, "The type of convert.",
                TYPES, 0, false));
        return types;
    }

}
