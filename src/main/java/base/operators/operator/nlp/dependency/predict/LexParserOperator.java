package base.operators.operator.nlp.dependency.predict;

import com.alibaba.fastjson.JSONObject;
import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Model;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.dependency.model.LexParserModel;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.parameter.*;
import base.operators.tools.Ontology;
import nlp.core.parser.ling.CoreLabel;
import nlp.core.parser.parser.lexparser.LexParserImpl;
import nlp.core.parser.parser.lexparser.LexicalizedParser;
import nlp.core.parser.process.CoreLabelTokenFactory;
import nlp.core.parser.process.PTBTokenizer;
import nlp.core.parser.process.Tokenizer;
import nlp.core.parser.process.TokenizerFactory;
import nlp.core.parser.trees.*;
import nlp.core.parser.trees.international.pennchinese.ChineseGrammaticalStructure;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by wangpanpan on 2019/7/31.
 */
public class LexParserOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort modelInput = getInputPorts().createPort("model input");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
    private OutputPort modelOutput = getOutputPorts().createPort("model");
    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String LANGUAGE = "language";

    public static String[] LANGUAGES = {"Chinese","English"};

    public LexParserOperator(OperatorDescription description){
        super(description);
    }

    public void doWork() throws OperatorException {
        String[] doc_column = getParameterAsString(DOC_ATTRIBUTE_NAME).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        String language = getParameterAsString(LANGUAGE);
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        ExampleTable table = exampleSet.getExampleTable();
        List<Attribute> rawAttributes = new ArrayList<>();
        List<Attribute> addAttributes = new ArrayList<>();
        for (int j = 0; j < doc_column.length; j++) {
            if(!"".equals(doc_column[j])){
                rawAttributes.add(exampleSet.getAttributes().get(doc_column[j]));
                Attribute newAttribute = AttributeFactory.createAttribute(doc_column[j]+"_lex", Ontology.STRING);
                table.addAttribute(newAttribute);
                exampleSet.getAttributes().addRegular(newAttribute);
                addAttributes.add(newAttribute);
            }
        }
        LexicalizedParser lexParser = null;
        if(modelInput.isConnected()){
            lexParser = ((LexParserModel)modelInput.getData(Model.class)).lexicalizedParser;
        }else{
            LexParserImpl lexParserImpl = LexParserImpl.getIstance();
            if(language=="English"){
                lexParser = lexParserImpl.enlp;
            }else{
                lexParser = lexParserImpl.chlp;
            }
        }

        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            for (int ii = 0; ii < addAttributes.size(); ii++) {
                String raw = example.getValueAsString(rawAttributes.get(ii));
                //判空处理
                String sreal = raw.replaceAll("\\s+","");
                if("".equals(sreal) || raw==null){//如果s为空或者
                    example.setValue(addAttributes.get(ii), addAttributes.get(ii).getMapping().mapString(raw));
                    continue;
                }

                //非空处理
                JSONObject result = new JSONObject();
                if ("Chinese".equals(language)) {
                    Tree parser = lexParser.parse(raw);
                    String syntacticParserResult = parser.pennString();
                    if(syntacticParserResult!=null){
                        syntacticParserResult = syntacticParserResult.replaceAll("\r\n","").replaceAll("\\s+", " ");//去掉结果中的空白符
                        result.put("syntactic",syntacticParserResult);
                    }
                    ChineseGrammaticalStructure gs = new ChineseGrammaticalStructure(parser);
                    Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
                    String dependenceParserResult = tdl.toString();
                    result.put("dependence", dependenceParserResult);

                } else if ("English".equals(language)) {
                    TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
                    Tokenizer<CoreLabel> tok = tokenizerFactory.getTokenizer(new StringReader(raw));
                    List<CoreLabel> rawWords = tok.tokenize();
                    Tree parse = lexParser.apply(rawWords);
                    TreebankLanguagePack tlp = lexParser.treebankLanguagePack();
                    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
                    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
                    String syntacticParserResult = parse.pennString();
                    if(syntacticParserResult!=null){
                        syntacticParserResult = syntacticParserResult.replaceAll("\r\n","").replaceAll("\\s+", " ");//去掉结果中的空白符
                        result.put("syntactic",syntacticParserResult);
                    }                    List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
                    result.put("dependence", tdl.toString());
                }
                example.setValue(addAttributes.get(ii), addAttributes.get(ii).getMapping().mapString(result.toString()));
            }
        }
        exampleSetOutput.deliver(exampleSet);
        modelOutput.deliver(new LexParserModel(lexParser, exampleSet, language));
    }

    @Override
    public boolean shouldAutoConnect(OutputPort port) {
        if (port == modelOutput) {
            return getParameterAsBoolean("keep_model");
        } else {
            return super.shouldAutoConnect(port);
        }
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();

        types.add(new ParameterTypeAttributes(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput));
        types.add(new ParameterTypeCategory(LANGUAGE, "The language of text.",
                LANGUAGES, 0, false));
        return types;
    }

}
