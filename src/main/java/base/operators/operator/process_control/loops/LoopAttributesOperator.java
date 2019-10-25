package base.operators.operator.process_control.loops;

import base.operators.MacroHandler;
import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ValueString;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.operator.tools.AttributeSubsetSelector;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeString;
import base.operators.tools.ProcessTools;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class LoopAttributesOperator
        extends AbstractLoopOperator<Attribute>
{
    private static final String PARAMETER_ATTRIBUTE_MACRO = "attribute_name_macro";
    private String currentName = null;

    private AttributeSubsetSelector attributeSelector;

    public LoopAttributesOperator(OperatorDescription description) {
        super(description, new String[] { "Loop Attributes" });
        addValue(new ValueString("attribute_name", "The number of the current feature.")
        {
            @Override
            public String getStringValue()
            {
                return LoopAttributesOperator.this.currentName;
            }
        });
    }

    @Override
    protected void init() {
        InputPort examplePort = ((PortPairExtender.PortPair)this.inputPortPairExtender.getManagedPairs().get(0)).getInputPort();
        examplePort.addPrecondition(new SimplePrecondition(examplePort, new ExampleSetMetaData(), true));
        this.attributeSelector = new AttributeSubsetSelector(this, examplePort);
    }

    @Override
    protected AbstractLoopOperator.LoopArguments<Attribute> prepareArguments(boolean executeParallely) throws OperatorException {
        ExampleSet exampleSet = (ExampleSet)((PortPairExtender.PortPair)this.inputPortPairExtender.getManagedPairs().get(0)).getInputPort().getData(ExampleSet.class);
        Set<Attribute> selectedAttributes = this.attributeSelector.getAttributeSubset(exampleSet, false);
        AbstractLoopOperator.LoopArguments<Attribute> arguments = new AbstractLoopOperator.LoopArguments<Attribute>();
        arguments.setDataForIteration(new ArrayList(selectedAttributes));
        arguments.setNumberOfIterations(selectedAttributes.size());
        if (isParameterSet("attribute_name_macro")) {
            arguments.setMacros(Collections.singletonMap("attribute_name_macro", getParameter("attribute_name_macro")));
        }
        return arguments;
    }

    @Override
    protected void setMacros(AbstractLoopOperator.LoopArguments<Attribute> arguments, MacroHandler macroHandler, int iteration) {
        if (arguments.getMacros() == null) {
            return;
        }
        macroHandler.addMacro((String)arguments.getMacros().get("attribute_name_macro"), ((Attribute)arguments.getDataForIteration().get(iteration)).getName());
    }

    @Override
    protected void prepareSingleRun(Attribute dataForIteration, AbstractLoopOperator<Attribute> operator) throws OperatorException {
        LoopAttributesOperator castOperator = (LoopAttributesOperator)operator;
        castOperator.currentName = dataForIteration.getName();
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();
        types.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(this.attributeSelector.getParameterTypes(), true));
        types.add(new ParameterTypeString("attribute_name_macro", "Name of a macro that will contain the current attribute name. Can be left blank if no macro is needed.", "loop_attribute", false));
        List<ParameterType> superTypes = super.getParameterTypes();
        types.addAll(superTypes);
        return types;
    }
}
