package base.operators.operator.nlp.dependency.train;


import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.dependency.model.LexParserModel;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeCategory;
import nlp.core.parser.parser.lexparser.LexicalizedParser;

import java.io.*;
import java.util.List;
import java.util.UUID;

public class LexParserLearner extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort modelOutput = getOutputPorts().createPort("model");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String LANGUAGE = "language";
    public static String[] LANGUAGES = {"Chinese","English"};


    public LexParserLearner(OperatorDescription description) {
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

        //String path = "C:\\Users\\wangpanpan\\Downloads\\rapidminer_data\\train";
        String path = System.getProperty("java.class.path");
        int firstIndex = path.lastIndexOf(System.getProperty("path.separator")) + 1;
        int lastIndex = path.lastIndexOf(File.separator) + 1;
        String trainName = "traincorpus_" + UUID.randomUUID().toString().substring(0, 6);
        path = path.substring(firstIndex, lastIndex) + File.separator + trainName;

        //将数据表中的数据转化为临时文件进行放置
        File folder = new File(path);
        if(!folder.exists()) {
            folder.mkdirs();
        }

        for(Example example: exampleSet) {
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
        LexicalizedParser lp = LexicalizedParser.train(path, language);
        LexParserModel lpm = new LexParserModel(lp,exampleSet,language);
        modelOutput.deliver(lpm);
        exampleSetOutput.deliver(exampleSet);

        //删除临时文件
        File[] trainFiles = folder.listFiles();
        if(trainFiles.length > 0) {
            for(File file: trainFiles) {
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
        types.add(new ParameterTypeCategory(LANGUAGE, "The language of document.", LANGUAGES, 0));
        return types;
    }

    public Class<? extends LexParserModel> getModelClass() {
        return LexParserModel.class;
    }

}
