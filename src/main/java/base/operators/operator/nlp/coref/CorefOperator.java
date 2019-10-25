package base.operators.operator.nlp.coref;

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
import base.operators.tools.Ontology;
import idsw.nlp.coref.coref.CorefCoreAnnotations;
import idsw.nlp.coref.coref.data.CorefChain;
import idsw.nlp.coref.international.Language;
import idsw.nlp.coref.ling.CoreAnnotations;
import idsw.nlp.coref.ling.CoreLabel;
import idsw.nlp.coref.pipeline.Annotation;
import idsw.nlp.coref.pipeline.CorefAnnotator;
import idsw.nlp.coref.pipeline.EntityMentionsAnnotator;
import idsw.nlp.coref.semgraph.SemanticGraph;
import idsw.nlp.coref.semgraph.SemanticGraphCoreAnnotations;
import idsw.nlp.coref.trees.Tree;
import idsw.nlp.coref.trees.TreeCoreAnnotations;
import idsw.nlp.coref.util.CoreMap;

import java.util.*;

public class CorefOperator extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort dependencyExampleSetInput = getInputPorts().createPort("dependency example set input");
    private InputPort nerExampleSetInput = getInputPorts().createPort("ner example set input");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
    public static final String DOC_ID_ATTRIBUTE_NAME = "doc_id_attribute_name";
    public static final String SENTENCE_INDEX_ATTRIBUTE_NAME = "sentence_index_attribute_name";
    public static final String SENTENCE_ATTRIBUTE_NAME = "sentence_attribute_name";
    public static final String SENTENCE_DEPENDENCY_ATTRIBUTE_NAME = "sentence_dependency_attribute_name";
    public static final String PERSON_ATTRIBUTE_NAME = "person_attribute_name";
    public static final String LOCATION_ATTRIBUTE_NAME = "location_attribute_name";
    public static final String ORGANIZATION_ATTRIBUTE_NAME = "organization_attribute_name";

    public CorefOperator(OperatorDescription description) {
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ID_ATTRIBUTE_NAME, SENTENCE_INDEX_ATTRIBUTE_NAME, SENTENCE_ATTRIBUTE_NAME)));
        dependencyExampleSetInput.addPrecondition(
                new AttributeSetPrecondition(dependencyExampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ID_ATTRIBUTE_NAME, SENTENCE_INDEX_ATTRIBUTE_NAME, SENTENCE_DEPENDENCY_ATTRIBUTE_NAME)));
        nerExampleSetInput.addPrecondition(
                new AttributeSetPrecondition(nerExampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, DOC_ID_ATTRIBUTE_NAME, SENTENCE_INDEX_ATTRIBUTE_NAME, PERSON_ATTRIBUTE_NAME, LOCATION_ATTRIBUTE_NAME, ORGANIZATION_ATTRIBUTE_NAME)));
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        ExampleSet dependencyExampleSet = (ExampleSet) dependencyExampleSetInput.getData(ExampleSet.class).clone();
        ExampleSet nerExampleSet = (ExampleSet) nerExampleSetInput.getData(ExampleSet.class).clone();

        String doc_id_name = getParameterAsString(DOC_ID_ATTRIBUTE_NAME);
        String sentence_index_name = getParameterAsString(SENTENCE_INDEX_ATTRIBUTE_NAME);
        String sentence_name = getParameterAsString(SENTENCE_ATTRIBUTE_NAME);
        String dependency_name = getParameterAsString(SENTENCE_DEPENDENCY_ATTRIBUTE_NAME);

        String person_name = getParameterAsString(PERSON_ATTRIBUTE_NAME);
        String location_name = getParameterAsString(LOCATION_ATTRIBUTE_NAME);
        String organization_name = getParameterAsString(ORGANIZATION_ATTRIBUTE_NAME);

        JSONObject docObj = new JSONObject();
        Attributes attributes = exampleSet.getAttributes();
        for(Example example : exampleSet) {
            String docId = example.getValueAsString(attributes.get(doc_id_name));
            String content = example.getValueAsString(attributes.get(sentence_name));
            String index = example.getValueAsString(attributes.get(sentence_index_name));
            JSONObject doc = docObj.getJSONObject(docId);
            if(doc != null) {
                doc.put(index, content);
            } else {
                doc = new JSONObject();
                doc.put(index, content);
            }
            docObj.put(docId, doc);
        }

        Map<String, String> dependMap = new HashMap<String, String>();
        Attributes denAttributes = dependencyExampleSet.getAttributes();
        for(Example example : dependencyExampleSet) {
            String docId = example.getValueAsString(denAttributes.get(doc_id_name));
            String index = example.getValueAsString(denAttributes.get(sentence_index_name));
            String dependency = example.getValueAsString(denAttributes.get(dependency_name));
            dependMap.put(docId + "_" + index, dependency);
        }

        Map<String, String> nerMap = new HashMap<String, String>();
        Attributes nerAttributes = nerExampleSet.getAttributes();
        for(Example example : nerExampleSet) {
            String docId = example.getValueAsString(nerAttributes.get(doc_id_name));
            String index = example.getValueAsString(nerAttributes.get(sentence_index_name));
            if(person_name != null) {
                String str = example.getValueAsString(nerAttributes.get(person_name));
                nerMap.put(docId + "_" + index, str);
            }
            if(organization_name != null) {
                String str = example.getValueAsString(nerAttributes.get(organization_name));
                nerMap.put(docId + "_" + index, str);
            }
            if(location_name != null) {
                String str = example.getValueAsString(nerAttributes.get(location_name));
                nerMap.put(docId + "_" + index, str);
            }
        }
        //共指消解并将结果形成新表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute new_doc_id_attribute = AttributeFactory.createAttribute(doc_id_name, attributes.get(doc_id_name).isNumerical() ? Ontology.NUMERICAL : Ontology.NOMINAL);
        attributeList.add(new_doc_id_attribute);
        Attribute doc_content_attribute = AttributeFactory.createAttribute("doc_content", Ontology.STRING);
        attributeList.add(doc_content_attribute);
        Attribute coref_chains_attribute = AttributeFactory.createAttribute("coref_chains", Ontology.STRING);
        attributeList.add(coref_chains_attribute);
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        Set<String> keys = docObj.keySet();
        for(String key : keys) {
            JSONObject json = docObj.getJSONObject(key);
            Set<String> senKeys = json.keySet();
            List<CoreMap> senList = new ArrayList<>();
            List<CoreLabel> labelList = new ArrayList<CoreLabel>();
            String docText = "";
            List<Map<String, Object>> CorefChains = new ArrayList<Map<String, Object>>();
            for(String senIndex: senKeys) {
                String sentence = json.getString(senIndex);
                String[] posArray = sentence.split(" ");
                List<CoreLabel> sentenceTokens = new ArrayList<CoreLabel>();

                String nerStr = nerMap.get(key + "_" + senIndex);
                JSONObject nerJson = JSONObject.parseObject(nerStr);
                JSONArray words = nerJson.getJSONArray("words");
                Map<Integer, Map<String, Object>> nerItem = new HashMap<Integer, Map<String, Object>>();
                for(int i=0; i<words.size(); i++) {
                    JSONObject word = words.getJSONObject(i);
                    int index = word.getIntValue("begin");
                    Integer begin = new Integer(index - 1);
                    int end = word.getIntValue("end") - 1;
                    String text = word.getString("word");
                    String ner = word.getString("NER");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("word", text);
                    map.put("end", end);
                    map.put("ner", ner);
                    map.put("begin", begin);
                    nerItem.put(begin, map);
                }
                String senText = "";
                int offset = 0;
                for(int i=0; i<posArray.length; i++) {
                    CoreLabel label = new CoreLabel();
                    String[] wordPos = posArray[i].split("/");
                    String word = wordPos[0];
                    String pos = wordPos[1];
                    if(!(word.equals(" ") || "\t".equals(word))) {
                        label.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, offset);
                        label.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, offset + word.length());
                        label.set(CoreAnnotations.TokenBeginAnnotation.class, i);
                        label.set(CoreAnnotations.TokenEndAnnotation.class, i+1);
                        label.set(CoreAnnotations.IsNewlineAnnotation.class, false);
                        label.set(CoreAnnotations.PositionAnnotation.class, senIndex);
                        // ner 后期替换下
                        Integer start = new Integer(offset);
                        Map<String, Object> map = nerItem.get(start);
                        String ner = "O";
                        if(map != null) {
                            ner = (String) map.get("ner");
                        }

                        label.set(CoreAnnotations.AnswerAnnotation.class, ner);
                        label.set(CoreAnnotations.CoarseNamedEntityTagAnnotation.class, ner);

                        label.setNER(ner);
                        label.setIndex(i + 1);
                        label.setLemma("");
                        label.setSentIndex((int)Double.parseDouble(senIndex));
                        //label.setSentIndex(Integer.parseInt(senIndex));
                        pos = LtpPos2Stanford(pos, word);
                        label.setTag(pos);
                        label.setOriginalText(word);
                        label.setValue(word);
                        label.setWord(word);
                        labelList.add(label);
                        sentenceTokens.add(label);
                    }
                    offset = offset + word.length();
                    senText = senText + word;
                }
                docText = docText + senText;
                Annotation senAnno = new Annotation(senText);
                int begin = sentenceTokens.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
                int last = sentenceTokens.size() - 1;
                int end = sentenceTokens.get(last).get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
                senAnno.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, begin);
                senAnno.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, end);
                int sentenceTokenBegin = sentenceTokens.get(0).get(CoreAnnotations.TokenBeginAnnotation.class);
                int sentenceTokenEnd = sentenceTokens.get(sentenceTokens.size()-1).get(
                        CoreAnnotations.TokenEndAnnotation.class);
                senAnno.set(CoreAnnotations.TokenBeginAnnotation.class, sentenceTokenBegin);
                senAnno.set(CoreAnnotations.TokenEndAnnotation.class, sentenceTokenEnd);
                senAnno.set(CoreAnnotations.TokensAnnotation.class, sentenceTokens);
                //senAnno.set(CoreAnnotations.SentenceIndexAnnotation.class, Integer.parseInt(senIndex));
                senAnno.set(CoreAnnotations.SentenceIndexAnnotation.class, (int)Double.parseDouble(senIndex));
                String dependStr = dependMap.get(key + "_" + senIndex);
                JSONObject dependJson = JSONObject.parseObject(dependStr);
                String syntactic = dependJson.getString("syntactic");
                Tree tree = Tree.valueOf(syntactic);
                senAnno.set(TreeCoreAnnotations.TreeAnnotation.class, tree);

                SemanticGraph sGragh = SemanticGraph.valueOf(dependJson.getString("dependence"), Language.Chinese);
                senAnno.set(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class,  sGragh);
                senAnno.set(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class, sGragh);
                senList.add(senAnno);
            }
            Annotation anno = new Annotation(docText);
            anno.set(CoreAnnotations.SentencesAnnotation.class, senList);
            anno.set(CoreAnnotations.TokensAnnotation.class, labelList);
            EntityMentionsAnnotator entityMentionsAnnotator = new EntityMentionsAnnotator();
            entityMentionsAnnotator.annotate(anno);
            Properties props = new Properties();
            //props.setProperty("annotators", "dcoref");
            props.setProperty("coref.algorithm", "hybrid");
            props.setProperty("coref.language", "zh");
            props.setProperty("coref.useSemantics", "true");
            props.setProperty("coref.sieves", "ChineseHeadMatch, ExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, PronounMatch");
            props.setProperty("coref.postprocessing", "true");
            props.setProperty("coref.zh.dict", "nlp/coref/models/dcoref/zh-attributes.txt.gz");
            CorefAnnotator corefAnno = new CorefAnnotator(props);
            corefAnno.annotate(anno);
            List<CoreMap> senAnnoList = anno.get(CoreAnnotations.SentencesAnnotation.class);
            Map<Integer, CorefChain> corefChains = anno.get(CorefCoreAnnotations.CorefChainAnnotation.class);

            for (Map.Entry<Integer, CorefChain> entry : corefChains.entrySet()) {
                List<Map<String, Object>> mentionList = new ArrayList<Map<String, Object>>();
                for (CorefChain.CorefMention m : entry.getValue().getMentionsInTextualOrder()) {
                    List<CoreLabel> tokens = senAnnoList.get(m.sentNum - 1).get(CoreAnnotations.TokensAnnotation.class);
                    Map<String, Object> mention = new HashMap<String, Object>();
                    mention.put("word", m.mentionSpan);
                    mention.put("sentNum", m.sentNum);
                    mention.put("begin", tokens.get(m.startIndex - 1).beginPosition());
                    mention.put("end", tokens.get(m.endIndex - 2).endPosition());
                    mentionList.add(mention);
                }
                if(mentionList.size() != 0) {
                    Map<String, Object> temp = new HashMap<String, Object>();
                    temp.put("corefChain", mentionList);
                    CorefChains.add(temp);
                }
            }
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            dataRow.set(new_doc_id_attribute, attributes.get(doc_id_name).isNumerical()? Double.parseDouble(key): new_doc_id_attribute.getMapping().mapString(key));
            dataRow.set(doc_content_attribute, doc_content_attribute.getMapping().mapString(docText));
            dataRow.set(coref_chains_attribute, coref_chains_attribute.getMapping().mapString(CorefChains.toString()));
            exampleTable.addDataRow(dataRow);
        }

        ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSet1);
    }

    private static String LtpPos2Stanford(String pos, String word) {
        if("n".equals(pos) || "ng".equals(pos) || "nn".equals(pos) || "in".equals(pos) || "jv".equals(pos) || "k".equals(pos)
                || "g".equals(pos) || "gv".equals(pos) || "gn".equals(pos) || "ga".equals(pos) || "x".equals(pos)
                || "ws".equals(pos) || "n".equals(pos) || "n".equals(pos) || "n".equals(pos)) {
            pos = "NN";
        } else if("nd".equals(pos) || "nl".equals(pos)) {
            pos = "LC";
        } else if(pos.startsWith("nh") || "ns".equals(pos) || "j".equals(pos) || "ni".equals(pos) || "jn".equals(pos) || "h".equals(pos) || "nz".equals(pos) || "nzx".equals(pos)) {
            pos = "NR";
        } else if("nt".equals(pos)) {
            pos = "NT";
        } else if("nze".equals(pos) || "nzw".equals(pos)) {
            pos = "URL";
        } else if("nzt".equals(pos) || "nzf".equals(pos) || "nzp".equals(pos) || "nzq".equals(pos) || "nzn".equals(pos) || "nzb".equals(pos)
                || "m".equals(pos) || "mp".equals(pos) || "mi".equals(pos) || "mf".equals(pos) || "md".equals(pos) || "mr".equals(pos)) {
            pos = "CD";
        } else if("v".equals(pos) || "vt".equals(pos) || "vi".equals(pos) || "vu".equals(pos) || "vd".equals(pos) || "iv".equals(pos)
                || "ic".equals(pos)) {
            pos = "VV";
        } else if("vl".equals(pos)) {
            pos = "VC";
        } else if("a".equals(pos) || "aq".equals(pos) || "f".equals(pos) || "ja".equals(pos)) {
            pos = "JJ";
        } else if("as".equals(pos) || "ia".equals(pos)) {
            pos = "VA";
        } else if("r".equals(pos)) {
            pos = "PN";
        } else if("d".equals(pos)) {
            pos = "AD";
        } else if("p".equals(pos) && (word.indexOf("把") >= 0 || word.indexOf("将") >= 0)) {
            pos = "BA";
        } else if("p".equals(pos) && (word.indexOf("被") >= 0 || word.indexOf("给") >= 0)) {
            pos = "LB";
        }  else if("p".equals(pos)) {
            pos = "P";
        }  else if("c".equals(pos)) {
            pos = "CC";
        } else if("u".equals(pos) && word.indexOf("的") >= 0) {
            pos = "DEC";
        } else if("u".equals(pos) && word.indexOf("得") >= 0) {
            pos = "DER";
        } else if("u".equals(pos) && word.indexOf("地") >= 0) {
            pos = "DEV";
        } else if("u".equals(pos) && word.indexOf("吗") >= 0) {
            pos = "SP";
        } else if("u".equals(pos) && word.indexOf("等") >= 0) {
            pos = "ETC";
        }   else if("u".equals(pos)) {
            pos = "DEC";
        } else if("o".equals(pos)) {
            pos = "ON";
        } else if("w".equals(pos) || "wp".equals(pos)) {
            pos = "PU";
        } else if("e".equals(pos)) {
            pos = "IJ";
        } else if("wu".equals(pos)) {
            pos = "FW";
        } else if("q".equals(pos) || "mq".equals(pos) || "mqa".equals(pos) || "mqt".equals(pos) || "mqn".equals(pos) || "mql".equals(pos)
                || "mqr".equals(pos) || "mqc".equals(pos) || "mqw".equals(pos) || "mqs".equals(pos) || "mqv".equals(pos)) {
            pos = "M";
        }
        return pos.toUpperCase();
    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ID_ATTRIBUTE_NAME, "The attribute name of the document id.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(SENTENCE_INDEX_ATTRIBUTE_NAME, "The attribute name of the sentence index in documnet.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(SENTENCE_ATTRIBUTE_NAME, "The attribute name of the sentence.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(SENTENCE_DEPENDENCY_ATTRIBUTE_NAME, "The attribute name of the sentence dependency.", dependencyExampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(PERSON_ATTRIBUTE_NAME, "The attribute name of the person ner result of sentence.", nerExampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(LOCATION_ATTRIBUTE_NAME, "The attribute name of the sentence location ner result of sentence.", nerExampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(ORGANIZATION_ATTRIBUTE_NAME, "The attribute name of the sentence organization ner result of sentence.", nerExampleSetInput,
                false));
        return types;
    }
}
