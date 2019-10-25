package base.operators.operator.nlp.extra;

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
import base.operators.parameter.*;
import base.operators.tools.Ontology;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wagnpanpan
 * create time:  2019.09.05.
 * description:
 */

public class CharSequence2CharNGramsOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    int n;
    boolean distinguishBorders = false;

    static char startBorderChar = '>';
    static char endBorderChar = '<';

    public static final String ID_ATTRIBUTE_NAME = "id_attribute_name";
    public static final String CONTENT_ATTRIBUTE_NAME = "content_attribute_name";
    public static final String SIZE = "size";
    public static final String DISTINGUISH_BORDERS = "distinguish_borders";

    public CharSequence2CharNGramsOperator(OperatorDescription description){
        super(description);
        exampleSetInput.addPrecondition(
                new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition.getAttributesByParameter(
                        this, ID_ATTRIBUTE_NAME, CONTENT_ATTRIBUTE_NAME)));
    }

    @Override
    public void doWork() throws OperatorException {
        String id_column = getParameterAsString(ID_ATTRIBUTE_NAME);
        String content_column = getParameterAsString(CONTENT_ATTRIBUTE_NAME);
        n = getParameterAsInt(SIZE);
        distinguishBorders = getParameterAsBoolean(DISTINGUISH_BORDERS);

        ExampleSet exampleSet = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = exampleSet.getAttributes();
        // 构造输出表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute new_id_attribute = AttributeFactory.createAttribute(id_column, attributes.get(id_column).isNumerical() ? Ontology.NUMERICAL : Ontology.NOMINAL);
        attributeList.add(new_id_attribute);
        Attribute ngram_attribute = AttributeFactory.createAttribute("char_ngram", Ontology.STRING);
        attributeList.add(ngram_attribute);
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        for (int i = 0; i < exampleSet.size(); i++) {
            Example example = exampleSet.getExample(i);
            double id = example.getValue(attributes.get(id_column));
            String content  = example.getValueAsString(attributes.get(content_column));
            String[] ngramArray = ngramify(content);
            for (int j = 0; j < ngramArray.length; j++) {
                DataRowFactory factory = new DataRowFactory(0, '.');
                DataRow dataRow = factory.create(attributeList.size());
                dataRow.set(new_id_attribute, id);
                dataRow.set(ngram_attribute, ngram_attribute.getMapping().mapString(ngramArray[j]));
                exampleTable.addDataRow(dataRow);

            }

        }
        ExampleSet exampleSet1 = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSet1);

    }

    public String[] ngramify (String s)
    {
        if (distinguishBorders)
            s = new StringBuffer().append(startBorderChar).append(s).append(endBorderChar).toString();
        int count = s.length() - n;
        String[] ngrams = new String[count];
        for (int i = 0; i < count; i++)
            ngrams[i] = s.subSequence (i, i+n).toString();
        return ngrams;
    }

    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttribute(ID_ATTRIBUTE_NAME, "The name of the id attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeAttribute(CONTENT_ATTRIBUTE_NAME, "The name of the string attribute.", exampleSetInput,
                false));
        types.add(new ParameterTypeInt(SIZE,"The size of n grams.", 1, Integer.MAX_VALUE, 2));
        types.add(new ParameterTypeBoolean(DISTINGUISH_BORDERS, "Whether to add boundary symbols.", false));
        return types;
    }
}
