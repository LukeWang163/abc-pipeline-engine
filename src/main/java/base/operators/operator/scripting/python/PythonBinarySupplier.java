package base.operators.operator.scripting.python;

import base.operators.parameter.UndefinedParameterError;
import java.nio.file.Path;

public interface PythonBinarySupplier {
    Path getPythonBinary() throws UndefinedParameterError;

    String getPythonEnvironmentName() throws UndefinedParameterError;
}
