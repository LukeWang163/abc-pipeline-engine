package base.operators.operator.nlp.feature;

import base.operators.example.*;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.*;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.feature.core.NGramFeatureExtraction;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NGramFeatureExtractionOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort vocabExampleSetInput = getInputPorts().createPort("vocabulary example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
    private OutputPort vocabExampleSetOutput = getOutputPorts().createPort("vocabulary example set");

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String DOC_ID_ATTRIBUTE_NAME = "doc_id_attribute_name";
    public static final String WORD_ATTRIBUTE_NAME = "word_attribute_name";
    public static final String DF_ATTRIBUTE_NAME = "df_attribute_name";
    public static final String IDF_ATTRIBUTE_NAME = "idf_attribute_name";
    public static final String N_GRAM_SIZE = "n_gram_size";
    public static final String MIN_FREQUENCY = "min_frequency";
    public static final String VOCABULARY_MODE = "vocabulary_mode";
    public static final String WEIGHT_FUNCTION = "weight_function";
    public static final String MAX_NGRAM_DOC_RATIO = "max_ngram_doc_ratio";
    public static final String NORMALIZE = "normalize";

    public static String[] WEIGHT_FUNCTIONS = {"Binary","TF","IDF","TFIDF"};
    public static String[] VOCABULARY_MODES = {"creat","readonly","merge"};

    public NGramFeatureExtractionOperator(OperatorDescription description){
        super(description);

        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ATTRIBUTE_NAME)));

    }

    public void doWork() throws OperatorException {
        String doc_attribute_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
        String doc_id_attribute_name = getParameterAsString(DOC_ID_ATTRIBUTE_NAME);
        String word_attribute_name = getParameterAsString(WORD_ATTRIBUTE_NAME);
        String df_attribute_name = getParameterAsString(DF_ATTRIBUTE_NAME);
        String idf_attribute_name = getParameterAsString(IDF_ATTRIBUTE_NAME);
        int ngram_size = getParameterAsInt(N_GRAM_SIZE);
        int min_frequency = getParameterAsInt(MIN_FREQUENCY);
        int vocabulary_mode = getParameterAsInt(VOCABULARY_MODE);
        int weight_function = getParameterAsInt(WEIGHT_FUNCTION);
        double max_ngram_doc_ratio = getParameterAsDouble(MAX_NGRAM_DOC_RATIO);
        boolean normalize = getParameterAsBoolean(NORMALIZE);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attribute docAttribute = exampleSet.getAttributes().get(doc_attribute_name);
        Attribute docIdAttribute = exampleSet.getAttributes().get(doc_id_attribute_name);
        //获取用户词典
        Map<String, Integer> userVocabularyDF = new HashMap<>();
        Map<String, Double> userVocabularyIDF = new HashMap<>();
        int userTotalNumDocs = 0;
        if(vocabExampleSetInput.isConnected()){
            ExampleSet vocabExampleSet = (ExampleSet) vocabExampleSetInput.getData(ExampleSet.class).clone();
            for (Example example : vocabExampleSet) {
                String word = example.getValueAsString(example.getAttributes().get(word_attribute_name));
                if("total.num.docs".equals(word)){
                    userTotalNumDocs = (int)example.getValue(example.getAttributes().get(df_attribute_name));
                }else{
                    userVocabularyDF.put(example.getValueAsString(example.getAttributes().get(word_attribute_name)), (int)example.getValue(example.getAttributes().get(df_attribute_name)));
                    userVocabularyIDF.put(example.getValueAsString(example.getAttributes().get(word_attribute_name)), example.getValue(example.getAttributes().get(idf_attribute_name)));
                }

            }

            if(vocabulary_mode==1){
                vocabExampleSetOutput.deliver(vocabExampleSet);
            }
        }

        NGramFeatureExtraction nfe = new NGramFeatureExtraction(exampleSet, docIdAttribute, docAttribute, userVocabularyDF, userVocabularyIDF, userTotalNumDocs, vocabulary_mode, ngram_size,  weight_function, min_frequency, max_ngram_doc_ratio, normalize);
        nfe.computeFeature();
        ExampleTable table = exampleSet.getExampleTable();
        for (int j = 0; j < nfe.feature[0].length; j++) {
            Attribute result = AttributeFactory.createAttribute("feature"+j, Ontology.NUMERICAL);
            table.addAttribute(result);
            exampleSet.getAttributes().addRegular(result);
        }
        // 构造特征输出表
        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            for (int j = 0; j < nfe.feature[i].length; j++) {
                example.setValue(example.getAttributes().get("feature"+j), nfe.feature[i][j]);
            }
        }
        exampleSetOutput.deliver(exampleSet);

        if(vocabulary_mode==0||vocabulary_mode==2){
            List<Attribute> attributeList = new ArrayList<>();
            Attribute id_attribute = AttributeFactory.createAttribute("id", Ontology.NUMERICAL);
            attributeList.add(id_attribute);
            Attribute ngram_attribute = AttributeFactory.createAttribute("ngram", Ontology.STRING);
            attributeList.add(ngram_attribute);
            Attribute df_attribute = AttributeFactory.createAttribute("df", Ontology.NUMERICAL);
            attributeList.add(df_attribute);
            Attribute idf_attribute = AttributeFactory.createAttribute("idf", Ontology.NUMERICAL);
            attributeList.add(idf_attribute);

            MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
            int index = 0;
            for(Map.Entry<String, Integer> entry : nfe.vocabulary.df.entrySet()){
                DataRowFactory factory = new DataRowFactory(0, '.');
                DataRow dataRow = factory.create(attributeList.size());
                dataRow.set(id_attribute, index);
                dataRow.set(ngram_attribute, ngram_attribute.getMapping().mapString(entry.getKey()));
                dataRow.set(df_attribute, entry.getValue());
                dataRow.set(idf_attribute, nfe.vocabulary.idf.get(entry.getKey()));

                exampleTable.addDataRow(dataRow);
                index+=1;
            }
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(id_attribute, -1);
            dataRow.set(ngram_attribute, ngram_attribute.getMapping().mapString("total.num.docs"));
            dataRow.set(df_attribute, nfe.vocabulary.totalNumDocs);
            dataRow.set(idf_attribute, 0.0);
            exampleTable.addDataRow(dataRow);
            ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable);
            vocabExampleSetOutput.deliver(exampleSet1);

        }
    }
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the text attribute.", exampleSetInput, false));
        types.add(new ParameterTypeAttribute(DOC_ID_ATTRIBUTE_NAME, "The name of the text id attribute.", exampleSetInput, false));
        types.add(new ParameterTypeAttribute(WORD_ATTRIBUTE_NAME, "The name of the word attribute of vocabulary.", vocabExampleSetInput, false));
        types.add(new ParameterTypeAttribute(DF_ATTRIBUTE_NAME, "The name of the df attribute of vocabulary.", vocabExampleSetInput, true));
        types.add(new ParameterTypeAttribute(IDF_ATTRIBUTE_NAME, "The name of the idf attribute of vocabulary.", vocabExampleSetInput, true));
        types.add(new ParameterTypeInt(MIN_FREQUENCY, "The min frequency of ngram words to save", 1, Integer.MAX_VALUE,1, false));
        types.add(new ParameterTypeDouble(MAX_NGRAM_DOC_RATIO, "The max ratio of ngram doc", 0.0001, 1,1, false));
        types.add(new ParameterTypeCategory(VOCABULARY_MODE, "The method of updating vocabulary.",
                VOCABULARY_MODES, 0, false));
        types.add(new ParameterTypeCategory(WEIGHT_FUNCTION, "The method of weight.",
                WEIGHT_FUNCTIONS, 0, false));
        types.add(new ParameterTypeBoolean(NORMALIZE, "Standardization or not.", true));
        types.add(new ParameterTypeInt(N_GRAM_SIZE, "The size of gram.", 1, Integer.MAX_VALUE, 2));

        return types;
    }

}
