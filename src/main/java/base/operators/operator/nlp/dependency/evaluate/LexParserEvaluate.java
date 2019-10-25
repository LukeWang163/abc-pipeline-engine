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
import base.operators.operator.nlp.dependency.model.LexParserModel;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.tools.Ontology;
import nlp.core.parser.parser.lexparser.EvaluateTreebank;
import nlp.core.parser.parser.lexparser.LexicalizedParser;
import nlp.core.parser.parser.lexparser.Options;
import nlp.core.parser.trees.Treebank;
import nlp.core.parser.util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LexParserEvaluate extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort modelInput = getInputPorts().createPort("model input");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";


    public LexParserEvaluate(OperatorDescription description) {
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

        String path = System.getProperty("java.class.path");
        int firstIndex = path.lastIndexOf(System.getProperty("path.separator")) + 1;
        int lastIndex = path.lastIndexOf(File.separator) + 1;
        String testName = "testcorpus_" + UUID.randomUUID().toString().substring(0, 6);
        path = path.substring(firstIndex, lastIndex) + File.separator + testName;

        //将数据表中的数据转化为临时文件进行放置
        File folder = new File(path);
        if(!folder.exists()) {
            folder.mkdirs();
        }
        for(Example example : exampleSet) {
            String id = example.getValueAsString(exampleSet.getAttributes().get(id_attribute_name));
            String tempPath = "";
            tempPath = path + File.separator + id + ".mz";
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
        LexicalizedParser lp = null;
        LexParserModel lexParserModel = ((LexParserModel)modelInput.getData(Model.class));
        lp = lexParserModel.lexicalizedParser;
        EvaluateTreebank evaluator = new EvaluateTreebank(lp);

        Options op = lp.getOp();
        //测试数据集
        Pair<String, FileFilter> testTreebankPath = Pair.makePair(path, null);
        //模型评估
        op.tlpParams.setInputEncoding("utf-8");
        op.tlpParams.setOutputEncoding("utf-8");
        Treebank testTreebank = op.tlpParams.memoryTreebank();
        testTreebank.loadPath(testTreebankPath.first(), testTreebankPath.second());
        //log.info("Loaded " + testTreebank.size() + " trees");
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

        File[] testFiles = folder.listFiles();
        if(testFiles.length > 0) {
            for(File file: testFiles) {
                file.delete();
            }
        }
        folder.delete();
    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the attribute of document.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput,
                false));
        return types;
    }
}
