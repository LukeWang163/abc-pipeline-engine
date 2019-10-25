package base.operators.operator.scripting.parameter;

import base.operators.gui.tools.ProgressThread;
import base.operators.operator.Operator;
import base.operators.operator.scripting.python.PythonSetupTester;
import base.operators.tools.ProgressListener;
import java.nio.file.Path;
import java.util.List;

public class CondaEnvironmentSuggestionProvider extends AbstractEnvironmentSuggestionProvider {
    public CondaEnvironmentSuggestionProvider() {
    }

    @Override
    public List<String> getSuggestions(Operator op, ProgressListener pl) {
        return PythonSetupTester.INSTANCE.listCondaEnvironments();
    }

    @Override
    protected String getRefreshActionI18nKey() {
        return "python_scripting.refresh_conda_environments";
    }

    @Override
    protected String getProgressThreadI18nKey() {
        return "refresh_conda_envs_thread";
    }

    @Override
    protected void runRefreshOperation(ProgressThread progressThread) {
        PythonSetupTester.INSTANCE.refreshCondaCache();
    }

    @Override
    public boolean areFilePathsProvided() {
        return false;
    }

    @Override
    public String getFullPathForEnvironment(String environment) {
        Path p = PythonSetupTester.INSTANCE.getFullPathForCondaEnvironment(environment);
        return p == null ? null : p.toString();
    }
}
