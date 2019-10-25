package base.operators.operator.etl.trans;


import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeString;
import base.operators.tools.Ontology;

import java.util.List;

public class AddSequenceOperator extends Operator {

    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public AddSequenceOperator(OperatorDescription description){
        super(description);
    }

    public static final String RESULT_ATTRIBUTE_NAME = "result_attribute_name";
    public static final String STARTING_VALUE = "starting_value";
    public static final String INCREMENT = "increment";
    public static final String MAXIMUM_VALUE = "maximum_value";

    private double start;
    private double increment;
    private double maximum;

    @Override
    public void doWork() throws OperatorException {
        String output  = getParameterAsString(RESULT_ATTRIBUTE_NAME);
        ExampleSet input = (ExampleSet) exampleSetInput.getData(ExampleSet.class).clone();
        Attributes attributes = input.getAttributes();
        ExampleTable exampleTable = input.getExampleTable();
        Attribute result_attribute = null;
        result_attribute = AttributeFactory.createAttribute(output, Ontology.NUMERICAL);
        exampleTable.addAttribute(result_attribute);
        attributes.addRegular(result_attribute);

        start = getParameterAsDouble(STARTING_VALUE);
        increment = getParameterAsDouble(INCREMENT);
        maximum = getParameterAsDouble(MAXIMUM_VALUE);
        Counter counter = new Counter( start, increment, maximum );
        double next = 0.0;
        for (int i = 0; i < input.size(); i++) {
            Example example = input.getExample(i);
            double prev = counter.getCounter();

            double nval = prev + increment;
            if ( increment > 0 && maximum > start && nval > maximum ) {
                nval = start;
            }
            if ( increment < 0 && maximum < start && nval < maximum ) {
                nval = start;
            }
            counter.setCounter( nval );
            next = prev;
            example.setValue(result_attribute, next);
        }
        exampleSetOutput.deliver(input);

    }

    @Override
    public List<ParameterType> getParameterTypes() {

        List<ParameterType> types = super.getParameterTypes();

        types.add(new ParameterTypeString(RESULT_ATTRIBUTE_NAME, "Select the attribute name of the result.","result", false));
        types.add(new ParameterTypeDouble(STARTING_VALUE,"Start value of sequence.",Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,1.0,false));
        types.add(new ParameterTypeDouble(INCREMENT,"Increment value of sequence.",Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,1.0,false));
        types.add(new ParameterTypeDouble(MAXIMUM_VALUE,"Maximum value of sequence.",Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,999999999,false));

        return types;
    }


}
