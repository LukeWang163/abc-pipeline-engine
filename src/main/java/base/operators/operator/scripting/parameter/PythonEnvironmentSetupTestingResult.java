package base.operators.operator.scripting.parameter;

import base.operators.tools.config.actions.ActionResult.Result;

public class PythonEnvironmentSetupTestingResult extends RuntimeException {
    private static final long serialVersionUID = 1097605084891221445L;
    private final transient TestActionResult result;

    public PythonEnvironmentSetupTestingResult(TestActionResult result) {
        super(result.getMessage());
        this.result = result;
    }

    public boolean succeeded() {
        return this.result.getResult() == Result.SUCCESS;
    }

    public String getInstalledModules() {
        return this.result.getInstalledModules();
    }
}
