package base.operators.operator.nlp.autosummary;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.*;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.autosummary.core.summary.SummaryImpl;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SummaryOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort stopDictInput = getInputPorts().createPort("stop dict");
    private OutputPort exampleSetOutput1 = getOutputPorts().createPort("first example set");
    private OutputPort exampleSetOutput2 = getOutputPorts().createPort("second example set");

    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String STOP_WORDS_ATTRIBUTE_NAME = "stop_words_attribute_name";
    public static final String SIZE = "size";
    public static final String LANGUAGE = "language";
    public static final String DAMPING_FACTOR = "damping_factor";//阻尼系数
    public static final String MAX_ITER = "max_iter";//迭代次数
    public static final String MIN_DIFF = "min_diff";//迭代结束误差
    public static final String STOP_WORDS_MODE = "stop_words_mode";//0:系统停用词；1:自定义停用词;2:合并
    public static String[] STOP_WORDS_MODES_EN = {"system dictionary","user dictionary","merge dictionary"};
    public static String[] STOP_WORDS_MODES_CH = {"系统停用词","自定义停用词","合并词典"};
    public static String[] LANGUAGES = {"Chinese","English"};

    public SummaryOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, ID_ATTRIBUTE_NAME, DOC_ATTRIBUTE_NAME)));

    }
    @Override
    public void doWork() throws OperatorException {

        String id_column = getParameterAsString(ID_ATTRIBUTE_NAME);
        String doc_column = getParameterAsString(DOC_ATTRIBUTE_NAME);
        String size = getParameterAsString(SIZE);
        int language = getParameterAsInt(LANGUAGE);
        Float damping_factor = (float) getParameterAsDouble(DAMPING_FACTOR);
        int max_iter = getParameterAsInt(MAX_ITER);
        Float min_diff = (float)getParameterAsDouble(MIN_DIFF);
        int stop_mode = getParameterAsInt(STOP_WORDS_MODE);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();

        List<String> stopList = null;
        if(stopDictInput.isConnected()){
            stopList = new ArrayList<>();
            ExampleSet dict = stopDictInput.getData(ExampleSet.class);
            String stop_column = getParameterAsString(STOP_WORDS_ATTRIBUTE_NAME);
            Attributes dictAttrs = dict.getAttributes();
            for(Example row : dict){
                stopList.add(row.getValueAsString(dictAttrs.get(stop_column)));
            }
        }
        SummaryImpl summaryImpl = new SummaryImpl(stopList, damping_factor, max_iter, min_diff, size, language, stop_mode);
        Attributes attributes = exampleSet.getAttributes();

        //构造第一个输出
        List<Attribute> attributeList = new ArrayList<>();
        Attribute index_attribute = AttributeFactory.createAttribute("index", Ontology.NUMERICAL);
        attributeList.add(index_attribute);
        Attribute new_id_attribute = AttributeFactory.createAttribute(id_column, attributes.get(id_column).isNumerical() ? Ontology.NUMERICAL : Ontology.NOMINAL);
        attributeList.add(new_id_attribute);
        Attribute summary_attribute = AttributeFactory.createAttribute("summary", Ontology.STRING);
        attributeList.add(summary_attribute);
        Attribute weight_attribute = AttributeFactory.createAttribute("weight", Ontology.NUMERICAL);
        attributeList.add(weight_attribute);
        Attribute sentence_index_attribute = AttributeFactory.createAttribute("sentence_index", Ontology.NUMERICAL);
        attributeList.add(sentence_index_attribute);
        MemoryExampleTable exampleTable1 = new MemoryExampleTable(attributeList);

        //构造第二个输出
        ExampleTable exampleTable2 = exampleSet.getExampleTable();
        Attribute summary_merge_attribute = AttributeFactory.createAttribute("summary_merge", Ontology.STRING);
        exampleTable2.addAttribute(summary_merge_attribute);
        exampleSet.getAttributes().addRegular(summary_merge_attribute);

        //计算每个关键句
        int index = 0;
        for (int i = 0; i < exampleSet.size(); i++){
            Example example = exampleSet.getExample(i);
            double docId = example.getValue(attributes.get(id_column));
            String docContent = example.getValueAsString(attributes.get(doc_column));
            if("".equals(docContent) || docContent==null){
                continue;
            }

            //增加判空处理
            List<Map<String, Object>> docSummary = summaryImpl.summary(docContent);
            String summarySen = "";//句子内容
            StringBuffer summarySensMerge = new StringBuffer();
            int senIndex = 0;//句子索引
            double senWeight = 0.0;//句子权重
            if(docSummary==null || docSummary.size()==0){
                DataRowFactory factory = new DataRowFactory(0, '.');
                DataRow dataRow = factory.create(attributeList.size());
                dataRow.set(index_attribute, index);
                dataRow.set(new_id_attribute, docId);
                dataRow.set(summary_attribute, summary_attribute.getMapping().mapString(summarySen));
                dataRow.set(weight_attribute, senWeight);
                dataRow.set(sentence_index_attribute, senIndex);
                summarySensMerge.append(summarySen);
                exampleTable1.addDataRow(dataRow);
                index++;
            }else{
                for(Map abs : docSummary){
                    DataRowFactory factory = new DataRowFactory(0, '.');
                    DataRow dataRow = factory.create(attributeList.size());
                    summarySen = String.valueOf(abs.get("sentence"));//句子内容
                    senIndex = (int)abs.get("index");//句子索引
                    senWeight = (double)abs.get("weight");//句子权重
                    dataRow.set(index_attribute, index);
                    dataRow.set(new_id_attribute, docId);
                    dataRow.set(summary_attribute, summary_attribute.getMapping().mapString(summarySen));
                    dataRow.set(weight_attribute, senWeight);
                    dataRow.set(sentence_index_attribute, senIndex);
                    summarySensMerge.append(summarySen);
                    exampleTable1.addDataRow(dataRow);
                    index++;
                }
            }
            example.setValue(summary_merge_attribute, summary_merge_attribute.getMapping().mapString(summarySensMerge.toString()));
        }
        ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable1);
        exampleSetOutput1.deliver(exampleSet1);
        exampleSetOutput2.deliver(exampleSet);
    }

    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(STOP_WORDS_ATTRIBUTE_NAME, "The name of the stop words attribute.", stopDictInput,
                true));
        types.add(new ParameterTypeCategory(STOP_WORDS_MODE, "The mode of using stop dictionary",
                STOP_WORDS_MODES_EN, 0, false));
        types.add(new ParameterTypeInt(SIZE,"The size of summary.", 1, Integer.MAX_VALUE, 5));
        types.add(new ParameterTypeCategory(LANGUAGE, "The language of text.",
                LANGUAGES, 0, false));
        types.add(new ParameterTypeDouble(DAMPING_FACTOR, "The damping factor of text rank.", 0.0001,1, 0.85));
        types.add(new ParameterTypeInt(MAX_ITER, "The maximum number of iterations in textrank.", 1, Integer.MAX_VALUE, 200));
        types.add(new ParameterTypeDouble(MIN_DIFF, "The iteration end error in textrank.",0.0000001,1, 0.001));

        return types;
    }

}
