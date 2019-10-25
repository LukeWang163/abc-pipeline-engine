package base.operators.operator.process_control.loops;

import base.operators.MacroHandler;
import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ValueString;
import base.operators.operator.error.AttributeNotFoundError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeString;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class LoopValuesOperator
        extends AbstractLoopOperator<String>
{
    private static final String PARAMETER_ITERATION_MACRO = "iteration_macro";
    private static final String DEFAULT_ITERATION_MACRO_NAME = "loop_value";
    private static final String PARAMETER_ATTRIBUTE = "attribute";
    private String currentValue = null;

    public LoopValuesOperator(OperatorDescription description) {
        super(description, new String[] { "LoopValues" });

        addValue(new ValueString("current_value", "The nominal value of the current loop.")
        {
            @Override
            public String getStringValue()
            {
                return currentValue;
            }
        });
    }

    @Override
    protected void init() {
        InputPort examplePort = ((PortPairExtender.PortPair)this.inputPortPairExtender.getManagedPairs().get(0)).getInputPort();
        examplePort.addPrecondition(new SimplePrecondition(examplePort, new ExampleSetMetaData(), true));
    }

    @Override
    protected AbstractLoopOperator.LoopArguments<String> prepareArguments(boolean executeParallely) throws OperatorException {
        ExampleSet exampleSet = (ExampleSet)((PortPairExtender.PortPair)this.inputPortPairExtender.getManagedPairs().get(0)).getInputPort().getData(ExampleSet.class);
        String attributeName = getParameterAsString("attribute");
        Attribute attribute = exampleSet.getAttributes().get(attributeName);
        if (attribute == null) {
            throw new AttributeNotFoundError(this, "attribute", attributeName);
        }
        if (!attribute.isNominal()) {
            throw new UserError(this, 119, new Object[] { attributeName, getName() });
        }
        exampleSet.recalculateAttributeStatistics(attribute);


        Predicate<String> countOverZero = value -> (exampleSet.getStatistics(attribute, "count", value) > 0.0D);

        Stream<String> rawValues = StreamSupport.stream(attribute.getMapping().getValues().spliterator(),
                checkParallelizability());

        List<String> values = (List)rawValues.filter(countOverZero).collect(Collectors.toList());
        AbstractLoopOperator.LoopArguments<String> arguments = new AbstractLoopOperator.LoopArguments<String>();
        arguments.setDataForIteration(values);
        arguments.setNumberOfIterations(values.size());
        if (isParameterSet("iteration_macro")) {
            arguments.setMacros(Collections.singletonMap("iteration_macro", getParameter("iteration_macro")));
        }
        return arguments;
    }

    @Override
    protected void setMacros(AbstractLoopOperator.LoopArguments<String> arguments, MacroHandler macroHandler, int iteration) {
        if (arguments.getMacros() == null) {
            return;
        }
        macroHandler.addMacro((String)arguments.getMacros().get("iteration_macro"), (String)arguments.getDataForIteration().get(iteration));
    }

    @Override
    protected void prepareSingleRun(String dataForIteration, AbstractLoopOperator<String> operator) throws OperatorException {
        LoopValuesOperator castOperator = (LoopValuesOperator)operator;
        castOperator.currentValue = dataForIteration;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();

        ParameterTypeAttribute parameterTypeAttribute = new ParameterTypeAttribute("attribute", "The nominal attribute for which the iteration should be defined", ((PortPairExtender.PortPair)this.inputPortPairExtender.getManagedPairs().get(0)).getInputPort(), false, new int[] { 1 });
        types.add(parameterTypeAttribute);
        types.add(new ParameterTypeString("iteration_macro", "Name of a macro that will contain the current iteration value. Can be left blank if no macro is needed.", "loop_value", false));
        types.addAll(super.getParameterTypes());
        return types;
    }
}
