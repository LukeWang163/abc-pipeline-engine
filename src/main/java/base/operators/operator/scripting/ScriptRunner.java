package base.operators.operator.scripting;

import base.operators.operator.IOObject;
import base.operators.operator.OperatorException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public interface ScriptRunner {
    List<Class<? extends IOObject>> getSupportedTypes();

    List<IOObject> run(List<IOObject> var1, int var2) throws IOException, OperatorException;

    void cancel();

    void registerLogger(Logger var1);
}
