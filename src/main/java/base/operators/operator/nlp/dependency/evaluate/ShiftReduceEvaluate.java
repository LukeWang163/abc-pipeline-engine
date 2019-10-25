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
import base.operators.operator.nlp.dependency.model.ShiftReduceModel;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.tools.Ontology;
import nlp.core.parser.parser.lexparser.EvaluateTreebank;
import nlp.core.parser.parser.shiftreduce.ShiftReduceParser;
import nlp.core.parser.trees.Treebank;
import nlp.core.parser.util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShiftReduceEvaluate extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort modelInput = getInputPorts().createPort("model input");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String LANGUAGE = "language";

    public static String[] LANGUAGES = {"Chinese","English"};

    public ShiftReduceEvaluate(OperatorDescription description) {
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, ID_ATTRIBUTE_NAME, DOC_ATTRIBUTE_NAME)));

    }
    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        String id_attribute_name = getParameterAsString(ID_ATTRIBUTE_NAME);
        String doc_attribute_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
        String language = getParameterAsString(LANGUAGE);

        String path = System.getProperty("java.class.path");
        int firstIndex = path.lastIndexOf(System.getProperty("path.separator")) + 1;
        int lastIndex = path.lastIndexOf(File.separator) + 1;
        String testName = "testcorpus_" + UUID.randomUUID().toString().substring(0, 6);
        path = path.substring(firstIndex, lastIndex) + File.separator + testName;

        //将数据表中的数据转化为临时文件进行放置
        File testFolder = new File(path);
        if(!testFolder.exists()) {
            testFolder.mkdirs();
        }
        for(Example example : exampleSet) {
            String id = example.getValueAsString(exampleSet.getAttributes().get(id_attribute_name));
            String tempPath = "";
            tempPath = path + File.separator + id;
            File file = new File(tempPath);
            String content = example.getValueAsString(exampleSet.getAttributes().get(doc_attribute_name));
            try {
                if(!file.exists()) {
                    file.createNewFile();
                }
                BufferedReader bufferedReader = new BufferedReader(new StringReader(content));
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                char buf[] = new char[1024];         //字符缓冲区
                int len;
                while ((len = bufferedReader.read(buf)) != -1) {
                    bufferedWriter.write(buf, 0, len);
                }
                bufferedWriter.flush();
                bufferedReader.close();
                bufferedWriter.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        ShiftReduceParser parser = null;
        String[] args = {"-preTag",
                "-taggerSerializedFile","nlp\\dependency\\models\\MyChinese-NoDistsim.tagger"};

        if("English".equals(language)) {
            args[2] = "nlp\\dependency\\models\\MyEnglish-Distsim.tagger";
        }
        ShiftReduceModel shiftReduceModel = ((ShiftReduceModel)modelInput.getData(Model.class));
        parser = shiftReduceModel.parser;
        parser.getOp().testOptions.taggerSerializedFile = "nlp\\dependency\\models\\MyChinese-Distsim.tagger";
        parser.getOp().testOptions.preTag = true;
        parser.getOp().testOptions.forceTags = true;
        if("English".equals(language)) {
            parser.getOp().testOptions.taggerSerializedFile = "nlp\\dependency\\models\\MyEnglish-Distsim.tagger";
        }

        //测试数据集
        Pair<String, FileFilter> testTreebankPath = Pair.makePair(path, null);
        //模型评估

        Treebank testTreebank = parser.getOp().tlpParams.memoryTreebank();
        testTreebank.loadPath(testTreebankPath.first(), testTreebankPath.second());
        //log.info("Loaded " + testTreebank.size() + " trees");
        EvaluateTreebank evaluator = new EvaluateTreebank(parser.getOp(), null, parser);
        Double f1 = evaluator.testOnTreebank(testTreebank);

        // 构造输出表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute f1_attribute = AttributeFactory.createAttribute("F1", Ontology.NUMERICAL);
        attributeList.add(f1_attribute);
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
        DataRowFactory factory = new DataRowFactory(0, '.');
        DataRow dataRow = factory.create(attributeList.size());
        dataRow.set(f1_attribute, f1);
        exampleTable.addDataRow(dataRow);

        ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSet1);

        File[] testFiles = testFolder.listFiles();
        if(testFiles.length > 0) {
            for(File file: testFiles) {
                file.delete();
            }
        }
        testFolder.delete();

    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the attribute of document.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeCategory(LANGUAGE, "The language of document.", LANGUAGES, 0));
        return types;
    }



}
