package base.operators.operator.scripting.parameter;

import base.operators.gui.tools.ProgressThread;
import base.operators.parameter.SuggestionProvider;

public abstract class AbstractEnvironmentSuggestionProvider implements SuggestionProvider<String> {
    private static final int SHOW_DIALOG_TIMER_DELAY = 2000;

    public AbstractEnvironmentSuggestionProvider() {
    }

    protected abstract String getRefreshActionI18nKey();

    protected abstract String getProgressThreadI18nKey();

    protected abstract void runRefreshOperation(ProgressThread var1);

    public abstract boolean areFilePathsProvided();

    public abstract String getFullPathForEnvironment(String var1);

}
