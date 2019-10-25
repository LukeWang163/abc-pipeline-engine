package base.operators.operator.nlp.synonym;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SynonymOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort dictionaryInput = getInputPorts().createPort("user dictionary");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String WORD_ATTRIBUTE_NAME = "word_attribute_name";
    public static final String DICTIONARY_MODE = "dictionary_mode";
    public static String[] MODES_EN = {"system dictionary","user dictionary","merge dictionary"};
    public static String[] MODES_CH = {"系统词典","自定义词典","合并词典"};

    public SynonymOperator(OperatorDescription description){
        super(description);

        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, WORD_ATTRIBUTE_NAME)));
        if(dictionaryInput.isConnected()){
            dictionaryInput.addPrecondition(
                    new AttributeSetPrecondition(dictionaryInput, AttributeSetPrecondition.getAttributesByParameter(
                            this, "bigcode", "bigcat", "midcode", "midcat", "smallcode", "smallcat", "pos", "synonym")));
        }
    }

    @Override
    public void doWork() throws OperatorException {
        String word_attribute_name = getParameterAsString(WORD_ATTRIBUTE_NAME);
        int mode  = getParameterAsInt(DICTIONARY_MODE);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();

        List<Map<String, String>> dicts = new ArrayList<Map<String, String>>();
        if(dictionaryInput.isConnected() && mode != 0) {
            //读取输入词典
            ExampleSet dicExampleSet = (ExampleSet) dictionaryInput.getData(ExampleSet.class).clone();
            Attributes dicAttributes = dicExampleSet.getAttributes();
            // 获取自定义词典
            for(Example example : dicExampleSet) {
                Map<String, String> map = new HashMap<String, String>();
                map.put("bigcode", example.getValueAsString(dicAttributes.get("bigcode")));
                map.put("bigcat", example.getValueAsString(dicAttributes.get("bigcat")));
                map.put("midcode", example.getValueAsString(dicAttributes.get("midcode")));
                map.put("midcat", example.getValueAsString(dicAttributes.get("midcat")));
                map.put("smallcode", example.getValueAsString(dicAttributes.get("smallcode")));
                map.put("smallcat", example.getValueAsString(dicAttributes.get("smallcat")));
                map.put("pos", example.getValueAsString(dicAttributes.get("pos")));
                map.put("synonym", example.getValueAsString(dicAttributes.get("synonym")));
                dicts.add(map);
            }
        }

        // 定义输出表结构
        List<Attribute> attributeList = new ArrayList<>();
        Attribute word_attribute = AttributeFactory.createAttribute("word", Ontology.STRING);
        attributeList.add(word_attribute);
        Attribute bigcat_attribute = AttributeFactory.createAttribute("bigcat", Ontology.STRING);
        attributeList.add(bigcat_attribute);
        Attribute midcat_attribute = AttributeFactory.createAttribute("midcat", Ontology.STRING);
        attributeList.add(midcat_attribute);
        Attribute smallcat_attribute = AttributeFactory.createAttribute("smallcat", Ontology.STRING);
        attributeList.add(smallcat_attribute);
        Attribute pos_attribute = AttributeFactory.createAttribute("pos", Ontology.STRING);
        attributeList.add(pos_attribute);
        Attribute synonym_attribute = AttributeFactory.createAttribute("synonym", Ontology.STRING);
        attributeList.add(synonym_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
        Synonym synonym = null;

        if(dicts.isEmpty()||dicts.size()==0){
            synonym = Synonym.getInstance(null, mode);
        }else{
            //添加自定义同义词林
            synonym = Synonym.getInstance(dicts, mode);
        }
        // 查询同义词
        for(Example example : exampleSet) {
            String item = example.getValueAsString(attributes.get(word_attribute_name));
            JSONObject json = synonym.searchSynonym(item);
            JSONArray wordList = json.getJSONArray("wordList");
            for(int i=0; i<wordList.size(); i++) {
                JSONObject obj = wordList.getJSONObject(i);
                String bigCat = obj.getString("bigCat");
                String midCat = obj.getString("midCat");
                String smallCat = obj.getString("smallCat");
                JSONArray synonyms = obj.getJSONArray("synonyms");
                for(int j=0; j<synonyms.size(); j++) {
                    JSONObject synonymobj = synonyms.getJSONObject(j);
                    String nature = synonymobj.getString("pos");
                    List<String> words = (List<String>) synonymobj.get("words");
                    DataRowFactory factory = new DataRowFactory(0, '.');
                    DataRow dataRow = factory.create(attributeList.size());
                    dataRow.set(word_attribute, word_attribute.getMapping().mapString(item));
                    dataRow.set(bigcat_attribute, bigcat_attribute.getMapping().mapString(bigCat));
                    dataRow.set(midcat_attribute, midcat_attribute.getMapping().mapString(midCat));
                    dataRow.set(smallcat_attribute, smallcat_attribute.getMapping().mapString(smallCat));
                    dataRow.set(pos_attribute, pos_attribute.getMapping().mapString(nature));
                    dataRow.set(synonym_attribute, synonym_attribute.getMapping().mapString(String.join(" ", words)));
                    exampleTable.addDataRow(dataRow);
                }
            }
        }

        ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSet1);

    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(WORD_ATTRIBUTE_NAME, "The name of the word attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeCategory(DICTIONARY_MODE, "The mode of dictionary using.",
                MODES_EN, 0, false));
        return types;
    }
}
