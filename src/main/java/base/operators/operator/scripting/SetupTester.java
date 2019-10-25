package base.operators.operator.scripting;

import base.operators.tools.config.actions.ActionResult;

public interface SetupTester {
    ActionResult scriptingSetupTest(String var1);

    void autodetectPath();
}
