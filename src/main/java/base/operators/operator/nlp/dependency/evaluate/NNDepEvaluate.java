package base.operators.operator.nlp.dependency.evaluate;


import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.MemoryExampleTable;
import base.operators.operator.Model;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.dependency.model.NNDepModel;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.tools.Ontology;
import nlp.core.parser.parser.nndep.DependencyParser;
import nlp.core.parser.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NNDepEvaluate extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort modelInput = getInputPorts().createPort("model input");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String LANGUAGE = "language";
    public static String[] LANGUAGES = {"Chinese","English"};

    public NNDepEvaluate(OperatorDescription description) {
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ATTRIBUTE_NAME)));

    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        String doc_attribute_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
        String language = getParameterAsString(LANGUAGE);

        List<String> corpusList = new ArrayList<String>();
        String P_REGEX = ".+?(\\n|\\r)";
        for(Example example : exampleSet) {
            String content = example.getValueAsString(exampleSet.getAttributes().get(doc_attribute_name));
            Pattern pattern = Pattern.compile(P_REGEX);
            Matcher matcher = pattern.matcher(content);
            int lastEndIndex = 0;
            while (matcher.find()) {
                String sentence = matcher.group();
                sentence = sentence.replaceAll("(\\n|\\r)", "");
                if (!"".equals(sentence)) {
                    corpusList.add(sentence);
                }
                lastEndIndex=matcher.end();
            }
            if (lastEndIndex != content.length() - 1) {
                String restSentence = content.substring(lastEndIndex);
                restSentence = restSentence.replaceAll("(\\n|\\r)", "");
                if (!"".equals(restSentence.trim())) {
                    corpusList.add(restSentence);
                }
            }
            corpusList.add("");
        }
        /**
         * 明确指出命令行中各选项需要的参数数量
         */
        Map<String, Integer> numArgs = new HashMap<>();
        numArgs.put("textFile", 1);
        numArgs.put("outFile", 1);

        Properties props = null;

        if("Chinese".equals(language)) {
            String[] args = {"-embeddingSize","100", "-tlp", "ChineseTreebankLanguagePack"};
            props = StringUtils.argsToProperties(args, numArgs);
        } else {
            String[] args = {"-embeddingSize","100"};
            props = StringUtils.argsToProperties(args, numArgs);
        }

        //模型评估
        DependencyParser parser = new DependencyParser(props);
        StringBuilder modelSb = ((NNDepModel)modelInput.getData(Model.class)).parser.writeModel();
        parser.loadModel(modelSb, true);
        Map<String, Double> result = parser.testCoNLL(corpusList);

        //输出f1评估结果表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute USA_attribute = AttributeFactory.createAttribute("USA", Ontology.NUMERICAL);
        attributeList.add(USA_attribute);
        Attribute UASnoPunc_attribute = AttributeFactory.createAttribute("UASnoPunc", Ontology.NUMERICAL);
        attributeList.add(UASnoPunc_attribute);
        Attribute LAS_attribute = AttributeFactory.createAttribute("LAS", Ontology.NUMERICAL);
        attributeList.add(LAS_attribute);
        Attribute LASnoPunc_attribute = AttributeFactory.createAttribute("LASnoPunc", Ontology.NUMERICAL);
        attributeList.add(LASnoPunc_attribute);
        Attribute UEM_attribute = AttributeFactory.createAttribute("UEM", Ontology.NUMERICAL);
        attributeList.add(UEM_attribute);
        Attribute UEMnoPunc_attribute = AttributeFactory.createAttribute("UEMnoPunc", Ontology.NUMERICAL);
        attributeList.add(UEMnoPunc_attribute);
        Attribute ROOT_attribute = AttributeFactory.createAttribute("ROOT", Ontology.NUMERICAL);
        attributeList.add(ROOT_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
        DataRowFactory factory = new DataRowFactory(0, '.');
        DataRow dataRow = factory.create(attributeList.size());
        dataRow.set(USA_attribute, result.get("UAS"));//头结点正确的数量(有标点)
        dataRow.set(UASnoPunc_attribute,result.get("UASnoPunc") );//头结点正确的数量(无标点)
        dataRow.set(LAS_attribute, result.get("LAS"));//弧正确的数量(有标点)
        dataRow.set(LASnoPunc_attribute, result.get("LASnoPunc"));//弧正确的数量(无标点)
        dataRow.set(UEM_attribute, result.get("UEM"));//全部树结构正确的数量（有标点）
        dataRow.set(UEMnoPunc_attribute, result.get("UEMnoPunc"));//全部树结构正确的数量（无标点）
        dataRow.set(ROOT_attribute, result.get("ROOT"));// 根节点正确的数量

        exampleTable.addDataRow(dataRow);

        ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSet1);

    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeCategory(LANGUAGE, "The language of document.", LANGUAGES, 0));
        return types;
    }

}
