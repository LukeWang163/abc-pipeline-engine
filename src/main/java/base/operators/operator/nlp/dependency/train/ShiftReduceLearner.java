package base.operators.operator.nlp.dependency.train;

import base.operators.example.Example;
import base.operators.example.ExampleSet;
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
import nlp.core.parser.parser.shiftreduce.ShiftReduceImpl;
import nlp.core.parser.parser.shiftreduce.ShiftReduceOptions;
import nlp.core.parser.parser.shiftreduce.ShiftReduceParser;
import nlp.core.parser.tagger.maxent.MaxentTagger;
import nlp.core.parser.util.Generics;
import nlp.core.parser.util.Pair;

import java.io.*;
import java.util.List;
import java.util.UUID;

public class ShiftReduceLearner extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort modelOutput = getOutputPorts().createPort("model");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String LANGUAGE = "language";
    public static String[] LANGUAGES = {"Chinese","English"};

    public ShiftReduceLearner(OperatorDescription description) {
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
        String trainName = "traincorpus_" + UUID.randomUUID().toString().substring(0, 6);
        String devName = "devcorpus_" + UUID.randomUUID().toString().substring(0, 6);
        String modelTempName = "tempmodel_" + UUID.randomUUID().toString().substring(0, 6);
        String devPath = path.substring(firstIndex, lastIndex) + File.separator + devName;
        String trainPath = path.substring(firstIndex, lastIndex) + File.separator + trainName;
        String modelTempPath = path.substring(firstIndex, lastIndex) + File.separator + modelTempName;
        //将数据表中的数据转化为临时文件进行放置
        File folder = new File(trainPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File devFolder = new File(devPath);
        if (!devFolder.exists()) {
            devFolder.mkdirs();
        }

        int count = (int) (exampleSet.size() * 0.8);
        int index = 1;

        for (Example example : exampleSet) {
            String id = example.getValueAsString(exampleSet.getAttributes().get(id_attribute_name));
            String tempPath = "";
            tempPath = trainPath + File.separator + id;
            if (index > count) {
                tempPath = devPath + File.separator + id;
            }
            File file = new File(tempPath);
            String content = example.getValueAsString(exampleSet.getAttributes().get(doc_attribute_name));
            try {
                if (!file.exists()) {
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
            index++;
        }
        ShiftReduceParser parser = null;
        ShiftReduceOptions op = null;
        String[] args = {"-preTag",
                "-taggerSerializedFile", "nlp\\dependency\\models\\MyChinese-NoDistsim.tagger"};
        op = parser.buildTrainingOptions("nlp.core.parser.parser.lexparser.ChineseTreebankParserParams", args);
        op.testOptions.taggerSerializedFile = "nlp\\dependency\\models\\MyChinese-NoDistsim.tagger";
        if ("English".equals(language)) {
            args[2] = "nlp\\dependency\\models\\MyEnglish-Distsim.tagger";
            op = parser.buildTrainingOptions("nlp.core.parser.parser.lexparser.EnglishTreebankParserParams", args);
            op.testOptions.taggerSerializedFile = "nlp\\dependency\\models\\MyEnglish-Distsim.tagger";
        }

        parser = new ShiftReduceParser(op);
        List<Pair<String, FileFilter>> trainTreebankPath = Generics.newArrayList();
        Pair<String, FileFilter> devTreebankPath = Pair.makePair(devPath, null);
        //模型训练
        trainTreebankPath.add(Pair.makePair(trainPath, null));
        parser.train(trainTreebankPath, devTreebankPath, modelTempPath);
        MaxentTagger maxentTagger = null;
        ShiftReduceImpl shiftReduceImpl = ShiftReduceImpl.getIstance();
        if("Chinese".equals(language)){
            maxentTagger = shiftReduceImpl.chtagger;
        }else {
            maxentTagger = shiftReduceImpl.entagger;
        }
        ShiftReduceModel shiftReduceModel = new ShiftReduceModel(parser, maxentTagger, exampleSet, language);

        modelOutput.deliver(shiftReduceModel);
        exampleSetOutput.deliver(exampleSet);

        //删除临时文件
        delDir(folder);
        folder.delete();

        delDir(devFolder);
        devFolder.delete();

        File tempFolder = new File(modelTempPath);
        delDir(tempFolder);
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

    public Class<? extends ShiftReduceModel> getModelClass() {
        return ShiftReduceModel.class;
    }

    public static void delDir(File file) {
        if (file.isDirectory()) {
            File zFiles[] = file.listFiles();
            for (File file2 : zFiles) {
                delDir(file2);
            }
            file.delete();
        } else {
            file.delete();
        }
    }
}
