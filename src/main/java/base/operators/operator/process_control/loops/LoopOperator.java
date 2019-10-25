package base.operators.operator.process_control.loops;

import base.operators.MacroHandler;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.ParameterTypeString;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LoopOperator
        extends AbstractLoopOperator<Void>
{
    private static final String PARAMETER_NUMBER_OF_ITERATIONS = "number_of_iterations";
    private static final String PARAMETER_ITERATION_MACRO = "iteration_macro";

    public LoopOperator(OperatorDescription description) { super(description, new String[] { "Loop" }); }

    @Override
    protected AbstractLoopOperator.LoopArguments<Void> prepareArguments(boolean executeParallely) throws OperatorException {
        AbstractLoopOperator.LoopArguments<Void> arguments = new AbstractLoopOperator.LoopArguments<Void>();
        arguments.setNumberOfIterations(getParameterAsInt("number_of_iterations"));
        if (isParameterSet("iteration_macro")) {
            arguments.setMacros(Collections.singletonMap("iteration_macro", getParameter("iteration_macro")));
        }
        return arguments;
    }

    @Override
    protected void setMacros(AbstractLoopOperator.LoopArguments<Void> arguments, MacroHandler macroHandler, int iteration) {
        if (arguments.getMacros() != null) {
            macroHandler.addMacro((String)arguments.getMacros().get("iteration_macro"), Integer.toString(iteration + 1));
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();

        ParameterTypeInt parameterTypeInt = new ParameterTypeInt("number_of_iterations", "The number of times the inner process will be executed.", 1, 2147483647, false);

        parameterTypeInt.setDefaultValue(Integer.valueOf(5));
        types.add(parameterTypeInt);
        types.add(new ParameterTypeString("iteration_macro", "Name of a macro that will contain the current iteration value. Can be left blank if no macro is needed.", "iteration", false));
        types.add(parameterTypeInt);
        types.addAll(super.getParameterTypes());
        return types;
    }
}
