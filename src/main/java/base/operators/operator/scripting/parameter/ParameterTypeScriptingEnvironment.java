package base.operators.operator.scripting.parameter;

import base.operators.parameter.ParameterTypeSuggestion;

public class ParameterTypeScriptingEnvironment extends ParameterTypeSuggestion {
    private static final long serialVersionUID = 2153184195773983491L;
    private transient AbstractEnvironmentSuggestionProvider provider;

    public ParameterTypeScriptingEnvironment(String key, String description, AbstractEnvironmentSuggestionProvider provider) {
        super(key, description, provider);
        this.provider = provider;
    }

    public ParameterTypeScriptingEnvironment(String key, String description, AbstractEnvironmentSuggestionProvider provider, String defaultValue) {
        super(key, description, provider, defaultValue);
        this.provider = provider;
    }

    public boolean areFilePathsProvided() {
        return this.provider.areFilePathsProvided();
    }
}
