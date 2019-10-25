package base.operators.operator.nlp.stopfilter;

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
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.parameter.conditions.EqualStringCondition;
import base.operators.tools.Ontology;
import idsw.nlp.read.ReadFileAsStream;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zls
 * create time:  2019.07.26.
 * description:
 */
public class StopFilterOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort dictInput = getInputPorts().createPort("stop dict");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String PARAMETER_SELECT_COLUMN = "select_column_name";

    public static final String FILTER_TYPE = "filter_type";
    public static final String[] TYPES_EN = new String[]{"dictionary mode", "regex mode"};
    public static final String[] TYPES_CH = new String[]{"字典方式", "正则方式"};
    public static final String PARAMETER_DEFAULT_DICT = "use_default_dict";
    public static final String PARAMETER_STOP_COLUMN = "stop_word_name";

    public static final String EDIT_REGULAR_EXPRESSION = "edit_regular_expression";
    public static final String EXPRESSION_INDEX = "expression_index";
    public static final String REGEXP_EXPRESSION = "regexp_expression";

    private Set<String> stopdict = new HashSet<>();
    //private String dictPath = "/nlp/stopfilter/dict/stopdict.txt";

    public StopFilterOperator(OperatorDescription description){
        super(description);
    }


    @Override
    public void doWork() throws OperatorException {
        String[] contentColumn = getParameterAsString(PARAMETER_SELECT_COLUMN).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        int type = getParameterAsInt(FILTER_TYPE);
        List<String[]> regexp_list = new ArrayList<>();
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();

        ExampleTable table = exampleSet.getExampleTable();
        for (int j = 0; j < contentColumn.length; j++) {
            if(!"".equals(contentColumn[j])){
                Attribute result = AttributeFactory.createAttribute(contentColumn[j]+"_stopFilter", Ontology.STRING);
                table.addAttribute(result);
                exampleSet.getAttributes().addRegular(result);
            }

        }
        if(0==type){
            boolean useInternal = getParameterAsBoolean(PARAMETER_DEFAULT_DICT);
            if(useInternal){
                initDict();
            }
            if(dictInput.isConnected()){
                String stop_column = getParameterAsString(PARAMETER_STOP_COLUMN);
                ExampleSet dict = dictInput.getData(ExampleSet.class);
                Attribute attribute = dict.getAttributes().get(stop_column);
                for(Example row : dict){
                    stopdict.add(row.getValueAsString(attribute));
                }
            }
        }else if(1==type){
            regexp_list = getParameterList(EDIT_REGULAR_EXPRESSION);
        }

        for(Example row : exampleSet){
            for (int k = 0; k < contentColumn.length; k++) {
                if(!"".equals(contentColumn[k])){
                    String text = row.getValueAsString(exampleSet.getAttributes().get(contentColumn[k]));
                    if(text != null && !"".equals(text)) {
                        String[] token_array = text.split("\\s+");
                        StringBuffer sb = new StringBuffer();
                        if(0==type){
                            for(int i=0; i<token_array.length; i++) {
                                if(!stopdict.contains(token_array[i])) {
                                    sb.append(token_array[i] + " ");
                                }
                            }
                        }else if(1==type){
                            for(int i=0; i< token_array.length; i++){
                                boolean passed = true;
                                for (int j=0;j < regexp_list.size();j++) {
                                    Pattern pattern = Pattern.compile(regexp_list.get(j)[1]);
                                    Matcher matcher = pattern.matcher(text);
                                    if (matcher.matches()) {
                                        passed = false;
                                        break;
                                    }
                                }
                                if (passed) {
                                    sb.append(token_array[i] + " ");
                                }
                            }
                        }
                        row.setValue(exampleSet.getAttributes().get(contentColumn[k]+"_stopFilter"), exampleSet.getAttributes().get(contentColumn[k]+"_stopFilter").getMapping().mapString(sb.toString()));
                    } else {
                        row.setValue(exampleSet.getAttributes().get(contentColumn[k]+"_stopFilter"), exampleSet.getAttributes().get(contentColumn[k]+"_stopFilter").getMapping().mapString(""));
                    }
                }
            }
        }
        exampleSetOutput.deliver(exampleSet);

    }

    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttributes(PARAMETER_SELECT_COLUMN, "The name of the attribute to convert.",exampleSetInput));
        types.add(new ParameterTypeCategory(FILTER_TYPE,"The way of stop word filtering.",TYPES_EN, 0, false));

        ParameterType type = new ParameterTypeBoolean(PARAMETER_DEFAULT_DICT, "whether to use internal dict", true, false);
        type.registerDependencyCondition(new EqualStringCondition(this, FILTER_TYPE, false,TYPES_EN[0]));
        types.add(type);

        type = new ParameterTypeAttribute(PARAMETER_STOP_COLUMN, "The name of the attribute of stop word.", dictInput, true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_DEFAULT_DICT, false, false));
        types.add(type);

        type = new ParameterTypeList(EDIT_REGULAR_EXPRESSION, "Regular expression list",
                new ParameterTypeString(EXPRESSION_INDEX, "Regular expression index.", false, false),
                new ParameterTypeRegexp(REGEXP_EXPRESSION, "Regular expression.", false), false);
        type.registerDependencyCondition(new EqualStringCondition(this, FILTER_TYPE, false,TYPES_EN[1]));
        types.add(type);

        return types;
    }

    // 初始化停用词词典
    private void initDict() {
        InputStream inStream = ReadFileAsStream.readStopDict();
        try {
            BufferedReader dictReader = new BufferedReader(new InputStreamReader(inStream,"UTF-8"));
            String row;
            while ((row = dictReader.readLine()) != null) {
                stopdict.add(row);
            }
            dictReader.close();
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
