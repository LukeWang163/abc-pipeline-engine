package base.operators.operator.etl.trans;

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
import base.operators.operator.UserError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttributes;
import base.operators.parameter.ParameterTypeString;
import base.operators.tools.Ontology;

import java.util.*;

public class MergeRowsOperator extends Operator {

    private static final String VALUE_IDENTICAL = "identical";
    private static final String VALUE_CHANGED = "changed";
    private static final String VALUE_NEW = "new";
    private static final String VALUE_DELETED = "deleted";

    private boolean useRefWhenIdentical = false;

    private InputPort oldExampleSetInput = getInputPorts().createPort("old example set");
    private InputPort newExampleSetInput = getInputPorts().createPort("new example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public static final String KEY_ATTRIBUTES_NAME = "key_attributes_name";
    public static final String VALUE_ATTRIBUTES_NAME = "value_attributes_name";
    public static final String OUTPUT_FLAG_ATTRIBUTE_NAME = "output_flag_attribute_name";


    public MergeRowsOperator(OperatorDescription description) {
        super(description);
    }

    private String[] keyNrs;
    private String[] valueNrs;
    private Example one = null, two = null;
    private boolean isOne;
    @Override
    public void doWork() throws OperatorException {

        ExampleSet oldExampleSet = (ExampleSet) oldExampleSetInput.getData(ExampleSet.class).clone();
        ExampleSet newExampleSet = (ExampleSet) newExampleSetInput.getData(ExampleSet.class).clone();
        Attributes oldAttributes = oldExampleSet.getAttributes();
        Attributes newAttributes = newExampleSet.getAttributes();

        if(!compare(oldAttributes,newAttributes)){
            throw new UserError(this, -1, oldExampleSetInput, "The attibutes of the old example set and the new example set must be same！");
        }

        keyNrs = getParameterAsString(KEY_ATTRIBUTES_NAME).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        valueNrs = getParameterAsString(VALUE_ATTRIBUTES_NAME).split(ParameterTypeAttributes.ATTRIBUTE_SEPARATOR_REGEX);
        String flagName = getParameterAsString(OUTPUT_FLAG_ATTRIBUTE_NAME);
        //构造第一个输出
        List<Attribute> attributeList = new ArrayList<>();
        Iterator<Attribute> it = newAttributes.iterator();
        while(it.hasNext()){
            Attribute raw_attribute = it.next();
            Attribute generate_attribute = AttributeFactory.createAttribute(raw_attribute.getName(), raw_attribute.getValueType());
            attributeList.add(generate_attribute);
        }
        Attribute flag_attribute = AttributeFactory.createAttribute(flagName, Ontology.STRING);
        attributeList.add(flag_attribute);
        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

        int indexOne = 0;
        int indexTwo = 0;

        one = indexOne >= oldExampleSet.size() ? null : oldExampleSet.getExample(indexOne);
        two = indexTwo >= newExampleSet.size() ? null : newExampleSet.getExample(indexTwo);

        while ( one != null || two != null) {
            String flagField = "";
            Example outputExample = null;
            if ( one == null && two != null ) { // Record 2 is flagged as new!
                outputExample = two;
                isOne = false;
                flagField = VALUE_NEW;
                indexTwo++;
                two = indexTwo >= newExampleSet.size() ? null : newExampleSet.getExample(indexTwo);
            } else if ( one != null && two == null ) // Record 1 is flagged as deleted!
            {
                outputExample = one;
                isOne = true;
                flagField = VALUE_DELETED;
                indexOne++;
                one = indexOne >= oldExampleSet.size() ? null : oldExampleSet.getExample(indexOne);
            } else { // OK, Here is the real start of the compare code!
                int compare = compare( one, two, keyNrs );
                if ( compare == 0 ) { // The Key matches, we CAN compare the two rows...
                    int compareValues = compare( one, two, valueNrs );
                    if ( compareValues == 0 ) {
                        if ( useRefWhenIdentical ) {  //backwards compatible behavior: use the reference stream (PDI-736)
                            outputExample = one;
                            isOne = true;
                        } else {
                            outputExample = two;       //documented behavior: use the comparison stream (PDI-736)
                            isOne = false;
                        }
                        flagField = VALUE_IDENTICAL;
                    } else {
                        // Return the compare (most recent) row
                        //
                        outputExample = two;
                        isOne = false;
                        flagField = VALUE_CHANGED;
                    }
                    indexOne++;
                    indexTwo++;
                    one = indexOne >= oldExampleSet.size() ? null : oldExampleSet.getExample(indexOne);
                    two = indexTwo >= newExampleSet.size() ? null : newExampleSet.getExample(indexTwo);
                } else {
                    if ( compare < 0 ) { // one < two
                        outputExample = one;
                        isOne = true;
                        flagField = VALUE_DELETED;
                        indexOne++;
                        one = indexOne >= oldExampleSet.size() ? null : oldExampleSet.getExample(indexOne);
                    } else {
                        outputExample = two;
                        isOne = false;
                        flagField = VALUE_NEW;
                        indexTwo++;
                        two = indexTwo >= newExampleSet.size() ? null : newExampleSet.getExample(indexTwo);
                    }
                }
            }
            DataRowFactory factory = new DataRowFactory(0, '.');
            DataRow dataRow = factory.create(attributeList.size());
            for (int w = 0; w < attributeList.size()-1; w++) {
                if(attributeList.get(w).isNumerical()){
                    double value = outputExample.getValue(attributeList.get(w));
                    dataRow.set(attributeList.get(w), value);
                }else{
                    String value = "";
                    if(isOne){
                        value = outputExample.getValueAsString(oldAttributes.get(attributeList.get(w).getName()));
                    }else{
                        value = outputExample.getValueAsString(newAttributes.get(attributeList.get(w).getName()));
                    }
                    dataRow.set(attributeList.get(w), attributeList.get(w).getMapping().mapString(value));
                }
            }
            dataRow.set(flag_attribute, flag_attribute.getMapping().mapString(flagField));
            exampleTable.addDataRow(dataRow);
        }
        ExampleSet exampleSetResult = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSetResult);
    }

    public int compare( Example one, Example two, String[] attributes ){
        for ( String attribute : attributes ) {
            int cmp = 0;
            if(one.getAttributes().get(attribute).isNumerical()){
                cmp = Double.compare(one.getValue(one.getAttributes().get(attribute)), two.getValue(two.getAttributes().get(attribute)));

            }else{
                String strone = one.getValueAsString(one.getAttributes().get(attribute));
                String strtwo = two.getValueAsString(two.getAttributes().get(attribute));
                cmp = strone.compareTo( strtwo );
            }
            if ( cmp != 0 ) {
                return cmp;
            }
        }
        return 0;
    }
    public boolean compare(Attributes attributes1, Attributes attributes2){
        if(attributes1.size()!=attributes2.size()){
            return false;
        }
        Map<String, Integer> nameAndType1 = new HashMap<>();
        Map<String, Integer> nameAndType2 = new HashMap<>();

        Iterator<Attribute > it1 = attributes1.iterator();
        Iterator<Attribute > it2 = attributes2.iterator();
        while(it1.hasNext()){
            Attribute attribute = it1.next();
            nameAndType1.put(attribute.getName(), attribute.getValueType());
        }
        while(it2.hasNext()){
            Attribute attribute = it2.next();
            nameAndType2.put(attribute.getName(), attribute.getValueType());
        }
        if(nameAndType1.size()!=nameAndType2.size()){
            return false;
        }
        for (String key : nameAndType1.keySet()) {
            if((!nameAndType2.keySet().contains(key))||(nameAndType2.keySet().contains(key) && nameAndType1.get(key)!=nameAndType2.get(key))){
                return false;
            }
        }
        return true;
    }

    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeAttributes(KEY_ATTRIBUTES_NAME, "Select the attributes of key attributes.", oldExampleSetInput,
                false));
        types.add(new ParameterTypeAttributes(VALUE_ATTRIBUTES_NAME, "Select the attributes of value attributes.", oldExampleSetInput,
                false));
        types.add(new ParameterTypeString(OUTPUT_FLAG_ATTRIBUTE_NAME,"Attribute name of output  flag results","flag", false));

        return types;
    }

}
