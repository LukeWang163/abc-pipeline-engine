package base.operators.parameter;

import base.operators.parameter.ParameterTypeString;
import base.operators.tools.XMLException;
import org.w3c.dom.Element;

public class ParameterTypeProcessExecutionQueue extends ParameterTypeString {
    private static final long serialVersionUID = 1L;

    public ParameterTypeProcessExecutionQueue(Element element) throws XMLException {
        super(element);
    }

    public ParameterTypeProcessExecutionQueue(String key, String description, boolean optional, boolean expert) {
        super(key, description, optional, expert);
    }
}
