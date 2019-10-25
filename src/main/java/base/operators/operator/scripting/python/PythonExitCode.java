package base.operators.operator.scripting.python;

public enum PythonExitCode {
    PANDAS_NOT_FOUND(50, "pandas"),
    PANDAS_WRONG_VERSION(51, "pandas_version"),
    PARSING_FAILED(55, "parsing"),
    EXECUTION_FAILED(60, "execution"),
    DESERIALIZATION_FAILED(65, "deserialization"),
    SERIALIZATION_FAILED(66, "serialization"),
    FILE_NOT_FOUND(70, "filenotfound");

    private static final String PYTHON_USER_ERROR_PREFIX = "python_scripting.failure.";
    private int exitCode;
    private String key;

    private PythonExitCode(int exitCode, String key) {
        this.exitCode = exitCode;
        this.key = key;
    }

    public int getExitCode() {
        return this.exitCode;
    }

    public String getUserErrorKey() {
        return "python_scripting.failure." + this.key;
    }

    public static PythonExitCode getForCode(int exitCode) {
        PythonExitCode[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            PythonExitCode code = var1[var3];
            if (code.getExitCode() == exitCode) {
                return code;
            }
        }

        return null;
    }
}
