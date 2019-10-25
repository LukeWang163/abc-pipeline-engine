package base.operators.operator.scripting.os;

import base.operators.tools.SystemInfoUtilities;
import base.operators.tools.SystemInfoUtilities.OperatingSystem;

public final class SingletonOSCommandFactory {
    private static OSCommandRunner instance = null;

    private SingletonOSCommandFactory() {
    }

    public static synchronized OSCommandRunner getCommandFactory() {
        if (instance == null) {
            OperatingSystem os = SystemInfoUtilities.getOperatingSystem();
            if (os == OperatingSystem.WINDOWS) {
                instance = new WindowsCommandRunner();
            } else {
                instance = new UNIXCommandRunner();
            }
        }

        return instance;
    }
}
