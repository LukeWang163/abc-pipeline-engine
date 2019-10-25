package base.operators.operator.nlp.lda;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.MemoryExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.lda.core.LDAOption;
import base.operators.operator.nlp.lda.core.LDATrain;
import base.operators.operator.nlp.lda.core.Model;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//z是主题, w是词, d是文档

public class LdaLearner extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput1 = getOutputPorts().createPort("the number of times each word appears in the topic");
    private OutputPort exampleSetOutput2 = getOutputPorts().createPort("probability of each word under each topic");
    private OutputPort exampleSetOutput3 = getOutputPorts().createPort("probability of each word corresponding to each topic");
    private OutputPort exampleSetOutput4 = getOutputPorts().createPort("topic distribution points corresponding to documents");
    private OutputPort exampleSetOutput5 = getOutputPorts().createPort("overall Thematic Distribution");

    public static final String DOC_ID_ATTRIBUTE_NAME = "doc_id_attribute_name";
    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String TOPIC_NUMBER = "topic_number";
    public static final String ALPHA = "alpha";
    public static final String BETA = "beta";
    public static final String ITERATIONS = "iterations";

    public ExampleSet trainExampleSet = null;

    public LdaLearner(OperatorDescription description){
        super(description);

        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ID_ATTRIBUTE_NAME,DOC_ATTRIBUTE_NAME)));

    }

    public void doWork() throws OperatorException {
        String id_name = getParameterAsString(DOC_ID_ATTRIBUTE_NAME);
        String doc_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
        int topic_number = getParameterAsInt(TOPIC_NUMBER);
        double alpha = getParameterAsDouble(ALPHA);
        double beta = getParameterAsDouble(BETA);
        int iterations = getParameterAsInt(ITERATIONS);
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        trainExampleSet = exampleSet;
        Attributes attributes = exampleSet.getAttributes();
        List<String> text = new ArrayList<>();
        List<String> id = new ArrayList<>();
        for(Example example : exampleSet){
            text.add(example.getValueAsString(attributes.get(doc_name)));
            id.add(example.getValueAsString(attributes.get(id_name)));
        }
        train(id, text, topic_number, alpha, beta, iterations);
    }

    /**
     * @Title: train
     * @Description: LDA训练
     * @param id：id序列，对应text
     * @param text：文本，list中每个为一个doc，doc格式为以空格分词的string
     * @param K：主题个数
     * @param alpha：doc-topic 先验参数
     * @param beta：topic-word 先验参数
     * @param niters ：迭代次数
     * @return void
     */
    public void train(List<String>id, List<String> text, int K, double alpha, double beta, int niters) {
        LDAOption option = new LDAOption();

        //option.dir = "./"+File.separator+"model3";
        option.niters = niters;
        option.K = K;
        option.alpha = alpha;
        option.beta = beta;
        option.train = true;  //训练

        LDATrain train = new LDATrain();
        if (train.init(option, text))
        {
            train.train();
        }

        final Model model = train.trainModel;
        process(id, model);
    }
    private void process(final List<String>id, final Model model){
        process1(model);
        process2(model);
        process3(model);
        process4(model);
        process5(id, model);
        process6(model);

    }

    //输出桩1 topic-word频率贡献表
    private void process1(final Model model){

        int nw[][] = model.nw;
        int topicNum = nw[0].length;
        int wordNum = nw.length;

        // 构造第一个输出表
        List<Attribute> attributeList = new ArrayList<>();
        for (int i = 0; i < topicNum; i++) {
            Attribute topic_attribute = AttributeFactory.createAttribute("topic_" + i, Ontology.NUMERICAL);
            attributeList.add(topic_attribute);
        }
        Attribute word_id_attribute = AttributeFactory.createAttribute("word_id", Ontology.STRING);
        attributeList.add(word_id_attribute);
        Attribute word_attribute = AttributeFactory.createAttribute("word", Ontology.STRING);
        attributeList.add(word_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        Map<Integer, String> map = model.data.localDict.id2word;
        for(int v = 0; v < wordNum; ++v){
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(word_id_attribute, word_id_attribute.getMapping().mapString(v + ""));
            dataRow.set(word_attribute, word_attribute.getMapping().mapString(map.get(v)));

            for(int k=0; k<topicNum; ++k){
                dataRow.set(attributeList.get(k), nw[v][k]);
            }
            exampleTable.addDataRow(dataRow);
        }

        ExampleSet exampleSet = new SimpleExampleSet(exampleTable);
        exampleSetOutput1.deliver(exampleSet);

    }
    //输出桩2 单词|主题输出表
    private void process2(final Model model){

        double phi[][] = model.phi;

        int topicNum = phi.length;
        int wordNum = phi[0].length;

        // 构造第二个输出表
        List<Attribute> attributeList = new ArrayList<>();
        for (int i = 0; i < topicNum; i++) {
            Attribute topic_attribute = AttributeFactory.createAttribute("topic_" + i, Ontology.NUMERICAL);
            attributeList.add(topic_attribute);
        }
        Attribute word_id_attribute = AttributeFactory.createAttribute("word_id", Ontology.STRING);
        attributeList.add(word_id_attribute);
        Attribute word_attribute = AttributeFactory.createAttribute("word", Ontology.STRING);
        attributeList.add(word_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        Map<Integer, String> map = model.data.localDict.id2word;

        for(int v = 0; v < wordNum; ++v){
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(word_id_attribute, word_id_attribute.getMapping().mapString(v + ""));
            dataRow.set(word_attribute, word_attribute.getMapping().mapString(map.get(v)));

            for(int k=0; k < topicNum; ++k){
                dataRow.set(attributeList.get(k), phi[k][v]);
            }
            exampleTable.addDataRow(dataRow);
        }

        ExampleSet exampleSet = new SimpleExampleSet(exampleTable);
        exampleSetOutput2.deliver(exampleSet);
    }
    //输出桩3 主题|单词输出表
    private void process3(final Model model){

        int nw[][] = model.nw;
        int topicNum = nw[0].length;
        int wordNum = nw.length;

        // 构造第三个输出表
        List<Attribute> attributeList = new ArrayList<>();
        for (int i = 0; i < topicNum; i++) {
            Attribute topic_attribute = AttributeFactory.createAttribute("topic_" + i, Ontology.NUMERICAL);
            attributeList.add(topic_attribute);
        }
        Attribute word_id_attribute = AttributeFactory.createAttribute("word_id", Ontology.STRING);
        attributeList.add(word_id_attribute);
        Attribute word_attribute = AttributeFactory.createAttribute("word", Ontology.STRING);
        attributeList.add(word_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        Map<Integer, String> map = model.data.localDict.id2word;
        double beta = model.beta;
        double betaSum = beta * topicNum;
        for(int v = 0; v < wordNum; ++v){
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(word_id_attribute, word_id_attribute.getMapping().mapString(v + ""));
            dataRow.set(word_attribute, word_attribute.getMapping().mapString(map.get(v)));
            double sum = 0;
            for(int k=0; k<topicNum; ++k){
                sum += nw[v][k];
            }

            for(int k=0; k<topicNum; ++k){
                double temp = (nw[v][k] + beta) / (sum + betaSum);
                dataRow.set(attributeList.get(k), temp);
            }

            exampleTable.addDataRow(dataRow);
        }

        ExampleSet exampleSet = new SimpleExampleSet(exampleTable);
        exampleSetOutput3.deliver(exampleSet);

    }
    //输出桩4 文档|主题输出表
    private void process4(final Model model){

        //to be done

    }
    //输出桩5 主题|文档输出表
    private void process5(final List<String>id, final Model model){

        double theta[][] = model.theta;
        int topicNum = theta[0].length;
        int docNum = theta.length;
        Attribute raw_doc_id_attribute = null;
        try {
            raw_doc_id_attribute = trainExampleSet.getAttributes().get(getParameterAsString(DOC_ID_ATTRIBUTE_NAME));
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }
        // 构造第五个输出表
        List<Attribute> attributeList = new ArrayList<>();
        for (int i = 0; i < topicNum; i++) {
            Attribute topic_attribute = AttributeFactory.createAttribute("topic_" + i, Ontology.NUMERICAL);
            attributeList.add(topic_attribute);
        }
        Attribute doc_id_attribute = null;
        try {
            doc_id_attribute = AttributeFactory.createAttribute(getParameterAsString(DOC_ID_ATTRIBUTE_NAME), raw_doc_id_attribute.getValueType());
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }
        attributeList.add(doc_id_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        for(int m=0; m < docNum; ++m){
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(doc_id_attribute, doc_id_attribute.isNominal()?raw_doc_id_attribute.getMapping().mapString(id.get(m)) : Double.parseDouble(id.get(m)));

            for(int k=0; k < topicNum; ++k){
                dataRow.set(attributeList.get(k), theta[m][k]);
            }
            exampleTable.addDataRow(dataRow);
        }
        ExampleSet exampleSet = new SimpleExampleSet(exampleTable);
        exampleSetOutput4.deliver(exampleSet);
    }
    //输出桩6  主题输出表
    private void process6(final Model model){

        int nwsum[] = model.nwsum;
        int topicNum = nwsum.length;

        // 构造第五个输出表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute topic_distribution_attribute = AttributeFactory.createAttribute("topic_distribution", Ontology.NUMERICAL);
        attributeList.add(topic_distribution_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        double sum = 0;
        for(int k=0; k < topicNum; ++k){
            sum += nwsum[k];
        }

        for(int k=0; k<topicNum; ++k){
            double temp =  nwsum[k] / sum;
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(topic_distribution_attribute, temp);
            exampleTable.addDataRow(dataRow);
        }
        ExampleSet exampleSet = new SimpleExampleSet(exampleTable);
        exampleSetOutput5.deliver(exampleSet);
    }


    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ID_ATTRIBUTE_NAME, "The name of the id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the attribute to train.", exampleSetInput,
                false));
        types.add(new ParameterTypeInt(TOPIC_NUMBER, "Specify the number of topics.", 1, Integer.MAX_VALUE, 10, false));
        types.add(new ParameterTypeInt(ITERATIONS, "The number of iterations.", 1, Integer.MAX_VALUE, 100, false));
        types.add(new ParameterTypeDouble(ALPHA, "P(Z|d)Prior Dirichlet distribution parameters.", 0.00001, 1, 0.1, false));
        types.add(new ParameterTypeDouble(BETA, "P(w|Z)Prior Dirichlet distribution parameters.", 0.00001, 1, 0.001, false));

        return types;
    }
}
