package base.operators.operator.scripting.parameter;

import base.operators.gui.tools.ProgressThread;
import base.operators.operator.Operator;
import base.operators.operator.scripting.python.PythonSetupTester;
import base.operators.tools.ProgressListener;
import java.util.List;

public class PythonBinariesSuggestionProvider extends AbstractEnvironmentSuggestionProvider {
    public PythonBinariesSuggestionProvider() {
    }

    @Override
    public List<String> getSuggestions(Operator op, ProgressListener pl) {
        return PythonSetupTester.INSTANCE.listPythonBinaries();
    }

    @Override
    protected String getRefreshActionI18nKey() {
        return "python_scripting.refresh_python_binaries";
    }

    @Override
    protected String getProgressThreadI18nKey() {
        return "refresh_python_binaries_thread";
    }

    @Override
    protected void runRefreshOperation(ProgressThread progressThread) {
        PythonSetupTester.INSTANCE.refreshPythonBinariesCache(progressThread);
    }

    @Override
    public boolean areFilePathsProvided() {
        return true;
    }

    @Override
    public String getFullPathForEnvironment(String environment) {
        return PythonSetupTester.INSTANCE.getFullPathForPythonBinary(environment).toString();
    }
}
