package base.operators.operator.generator.generators;

import base.operators.example.ExampleSet;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.parameter.ParameterType;
import java.util.List;

public interface DataGenerator {
    ExampleSetMetaData generateExampleSetMetaData() throws UserError;

    ExampleSet generateExampleSet() throws OperatorException;

    List<ParameterType> getParameterTypes();
}
