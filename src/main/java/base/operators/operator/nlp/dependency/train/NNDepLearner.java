package base.operators.operator.nlp.dependency.train;

import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.dependency.model.NNDepModel;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import nlp.core.parser.parser.nndep.DependencyParser;
import nlp.core.parser.parser.nndep.NNDepImpl;
import nlp.core.parser.tagger.maxent.MaxentTagger;
import nlp.core.parser.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NNDepLearner extends Operator {


    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort modelOutput = getOutputPorts().createPort("model");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String LANGUAGE = "language";
    public static String[] LANGUAGES = {"Chinese","English"};
    public static final String MAXIMUM_NUMBER_OF_ITERATIONS = "maximum_number_of_iterations";
    public static final String REGULARIZATION_COEFFICIENT = "regularization_coefficient";
    public static final String BATCH_SIZE = "batch_size";
    public static final String DROPOUT_PROBABILITY = "dropout_probability";
    public static final String EPSILON = "epsilon";
    public static final String LEARNING_RATE = "learning_rate";

    public NNDepLearner(OperatorDescription description) {
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ATTRIBUTE_NAME)));

    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        String doc_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
        String language = getParameterAsString(LANGUAGE);
        int maxIter = getParameterAsInt(MAXIMUM_NUMBER_OF_ITERATIONS);
        double regularCoef = getParameterAsDouble(REGULARIZATION_COEFFICIENT);
        int batchSize = getParameterAsInt(BATCH_SIZE);
        double dropout = getParameterAsDouble(DROPOUT_PROBABILITY);
        double epsilon = getParameterAsDouble(EPSILON);
        double learning_rate = getParameterAsDouble(LEARNING_RATE);
        List<String> corpusList = new ArrayList<String>();
        String P_REGEX = ".+?(\\n|\\r)";
        for(Example  example: exampleSet) {
            String content = example.getValueAsString(exampleSet.getAttributes().get(doc_name));
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

        DependencyParser parser = new DependencyParser(props);
        StringBuilder sb = new StringBuilder();
        sb = parser.train(language, corpusList, maxIter, batchSize, regularCoef, dropout, learning_rate, epsilon);
        parser.loadModel(sb, false);
        MaxentTagger maxentTagger = null;
        NNDepImpl nnDepImpl = NNDepImpl.getIstance();
        if("Chinese".equals(language)){
            maxentTagger = nnDepImpl.chtagger;
        }else {
            maxentTagger = nnDepImpl.entagger;
        }

        NNDepModel nnDepModel = new NNDepModel(parser, maxentTagger, exampleSet,language);

        modelOutput.deliver(nnDepModel);
        exampleSetOutput.deliver(exampleSet);
    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();

        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeCategory(LANGUAGE, "The language of document.", LANGUAGES, 0));
        types.add(new ParameterTypeInt(MAXIMUM_NUMBER_OF_ITERATIONS, "The maximum number of iterations.", 0, Integer.MAX_VALUE,20000,false));
        BigDecimal b = new BigDecimal("1e-8");
        types.add(new ParameterTypeDouble(REGULARIZATION_COEFFICIENT, "The regularization coeffifient.", 0, 1,b.doubleValue(),false));
        types.add(new ParameterTypeInt(BATCH_SIZE, "The size of batch.", 0, Integer.MAX_VALUE,10000,false));
        types.add(new ParameterTypeDouble(DROPOUT_PROBABILITY, "The probability of dropout.", 0, 1,0.5,false));
        b = new BigDecimal("1e-6");
        types.add(new ParameterTypeDouble(EPSILON, "The value of epsilon.", 0, 1, b.doubleValue(),false));
        types.add(new ParameterTypeDouble(LEARNING_RATE, "The learning rate.", 0, 1,0.01,false));

        return types;
    }
    public Class<? extends NNDepModel> getModelClass() {
        return NNDepModel.class;
    }

}
