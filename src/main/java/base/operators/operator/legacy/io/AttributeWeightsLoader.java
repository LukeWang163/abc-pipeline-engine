package base.operators.operator.legacy.io;

import base.operators.example.AttributeWeights;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractReader;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class AttributeWeightsLoader
        extends AbstractReader<AttributeWeights>
{
    public static final String PARAMETER_ATTRIBUTE_WEIGHTS_FILE = "attribute_weights_file";

    public AttributeWeightsLoader(OperatorDescription description) { super(description, AttributeWeights.class); }

    @Override
    public AttributeWeights read() throws OperatorException {
        File weightFile = getParameterAsFile("attribute_weights_file");
        AttributeWeights result = null;
        try {
            result = AttributeWeights.load(weightFile);
        } catch (IOException e) {
            throw new UserError(this, e, 302, new Object[] { weightFile, e.getMessage() });
        }
        return result;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeFile("attribute_weights_file", "Filename of the attribute weights file.", "wgt", false, false));

        return types;
    }
}
