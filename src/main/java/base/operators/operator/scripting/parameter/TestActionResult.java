package base.operators.operator.scripting.parameter;

import base.operators.tools.config.actions.SimpleActionResult;

public class TestActionResult extends SimpleActionResult {
    private String installedModules;

    public TestActionResult(String message, Result result) {
        this(message, "", result);
    }

    public TestActionResult(String message, String installedModules, Result result) {
        super(message, result);
        this.installedModules = installedModules;
    }

    public String getInstalledModules() {
        return this.installedModules;
    }
}
