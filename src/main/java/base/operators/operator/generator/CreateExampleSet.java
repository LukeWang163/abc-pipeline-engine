package base.operators.operator.generator;

import base.operators.example.ExampleSet;
import base.operators.operator.generator.generators.DataGenerators;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.io.AbstractExampleSource;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.parameter.ParameterType;
import java.util.List;

public class CreateExampleSet extends AbstractExampleSource {
    public CreateExampleSet(OperatorDescription description) { super(description); }

    @Override
    public MetaData getGeneratedMetaData() throws OperatorException { return DataGenerators.getGenerator(this).generateExampleSetMetaData(); }

    @Override
    public ExampleSet createExampleSet() throws OperatorException { return DataGenerators.getGenerator(this).generateExampleSet(); }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.addAll(DataGenerators.getParameterTypes(this));
        return types;
    }
}
