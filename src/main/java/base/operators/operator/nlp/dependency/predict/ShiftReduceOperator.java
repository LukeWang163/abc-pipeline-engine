package base.operators.operator.nlp.dependency.predict;

import com.alibaba.fastjson.JSONObject;
import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.*;
import base.operators.operator.nlp.dependency.model.ShiftReduceModel;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.parameter.*;
import base.operators.tools.Ontology;
import nlp.core.parser.parser.shiftreduce.ShiftReduceImpl;
import nlp.core.parser.parser.shiftreduce.ShiftReduceParser;
import nlp.core.parser.tagger.maxent.MaxentTagger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangpanpan on 2019/7/31.
 */
public class ShiftReduceOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private InputPort modelInput = getInputPorts().createPort("model input");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
    private OutputPort modelOutput = getOutputPorts().createPort("model");

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";
    public static final String LANGUAGE = "language";

    public static String[] LANGUAGES = {"Chinese","English"};
    public ShiftReduceOperator(OperatorDescription description){
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
                Attribute newAttribute = AttributeFactory.createAttribute(doc_column[j]+"_sr", Ontology.STRING);
                table.addAttribute(newAttribute);
                exampleSet.getAttributes().addRegular(newAttribute);
                addAttributes.add(newAttribute);
            }
        }
        ShiftReduceModel shiftReduceModel = null;
        if(modelInput.isConnected()){
            shiftReduceModel = ((ShiftReduceModel)modelInput.getData(Model.class));
            if(shiftReduceModel.language!=language){
                throw new UnsupportedApplicationParameterError(new ShiftReduceOperator(null), "ShiftReduceModel", "language");
            }
        }else{
            ShiftReduceImpl shiftReduceImpl = ShiftReduceImpl.getIstance();
            MaxentTagger tagger = null;
            ShiftReduceParser parser = null;
            if("Chinese".equals(language)){
                tagger = shiftReduceImpl.chtagger;
                parser = shiftReduceImpl.chparser;
            }else {
                tagger = shiftReduceImpl.entagger;
                parser = shiftReduceImpl.enparser;
            }
            shiftReduceModel = new ShiftReduceModel(parser, tagger,exampleSet, language);
        }

        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            for (int ii = 0; ii < addAttributes.size(); ii++) {
                String raw = example.getValueAsString(rawAttributes.get(ii));
                //判空处理
                String sreal = raw.replaceAll("\\s+","");
                if("".equals(sreal) || raw==null){//如果s为空或者
                    example.setValue(addAttributes.get(ii), addAttributes.get(ii).getMapping().mapString(sreal));
                    continue;
                }

                //非空处理
                JSONObject result = shiftReduceModel.predictImpl(raw);
                example.setValue(addAttributes.get(ii), addAttributes.get(ii).getMapping().mapString(result.toString()));
            }
        }

        exampleSetOutput.deliver(exampleSet);
        modelOutput.deliver(shiftReduceModel);

    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();

        types.add(new ParameterTypeAttributes(DOC_ATTRIBUTE_NAME, "The name of the document attribute.", exampleSetInput));
        types.add(new ParameterTypeCategory(LANGUAGE, "The language of text.",
                LANGUAGES, 0, false));
        return types;
    }
}
