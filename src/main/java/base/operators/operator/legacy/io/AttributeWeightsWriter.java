package base.operators.operator.legacy.io;

import base.operators.example.AttributeWeights;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.io.AbstractWriter;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeFile;
import base.operators.tools.io.Encoding;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class AttributeWeightsWriter extends AbstractWriter<AttributeWeights> {
    public static final String PARAMETER_ATTRIBUTE_WEIGHTS_FILE = "attribute_weights_file";
    public AttributeWeightsWriter(OperatorDescription description) {
        super(description, AttributeWeights.class);
    }

    @Override
    public AttributeWeights write(AttributeWeights weights) throws OperatorException {
        File weightFile = getParameterAsFile("attribute_weights_file", true);

        try {
            weights.writeAttributeWeights(weightFile, Encoding.getEncoding(this));
        } catch (IOException e) {
            throw new UserError(this, e, '?', new Object[] { weightFile, e.getMessage() });
        }

        return weights;
    }

    @Override
    protected boolean supportsEncoding() { return true; }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();
        types.add(new ParameterTypeFile("attribute_weights_file", "Filename for the attribute weight file.", "wgt", false));
        types.addAll(super.getParameterTypes());
        return types;
    }
}
