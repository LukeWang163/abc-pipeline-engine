package base.operators.operator.nlp.segment;

import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.segment.kshortsegment.training.CorpusLoader;
import base.operators.operator.nlp.segment.kshortsegment.training.CorpusUtil;
import base.operators.operator.nlp.segment.kshortsegment.training.NatureDictionaryMaker;
import base.operators.operator.nlp.segment.kshortsegment.training.document.Document;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.*;

import java.util.List;

public class KShortSegmentLearner extends Operator {
    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort modelOutput = getOutputPorts().createPort("model");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String DOC_ATTRIBUTE_NAME = "doc_attribute_name";

    public KShortSegmentLearner(OperatorDescription description) {
        super(description);
            exampleSetInput.addPrecondition(
                    new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                            this, DOC_ATTRIBUTE_NAME)));
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        String doc_attribute_name = getParameterAsString(DOC_ATTRIBUTE_NAME);
        final NatureDictionaryMaker dictionaryMaker = new NatureDictionaryMaker(exampleSet);
        CorpusLoader.walk(exampleSet, doc_attribute_name, new CorpusLoader.Handler() {
            @Override
            public void handle(Document document) {
                dictionaryMaker.compute(CorpusUtil.convert2CompatibleList(document.getSimpleSentenceList(false))); // 再打一遍不拆分的
                dictionaryMaker.compute(CorpusUtil.convert2CompatibleList(document.getSimpleSentenceList(true))); // 先打一遍拆分的
            }
        });
        modelOutput.deliver(dictionaryMaker);
        exampleSetOutput.deliver(exampleSet);
    }

    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(DOC_ATTRIBUTE_NAME, "The name of the attribute to convert.", exampleSetInput,
                false));
        return types;
    }

    public Class<? extends NatureDictionaryMaker> getModelClass() {
        return NatureDictionaryMaker.class;
    }
}
