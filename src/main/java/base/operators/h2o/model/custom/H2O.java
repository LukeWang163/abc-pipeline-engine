package base.operators.h2o.model.custom;


public class H2O {
    public static IllegalArgumentException unimpl() {
        return new IllegalArgumentException("unimplemented");
    }

    public static IllegalArgumentException unimpl(String msg) {
        return new IllegalArgumentException("unimplemented: " + msg);
    }

    public static RuntimeException fail() {
        return new RuntimeException();
    }

    public static String technote(int number, String message) {
        StringBuffer sb = (new StringBuffer()).append(message).append("\n").append("\n").append("For more information visit:\n").append("  http://jira.h2o.ai/browse/TN-").append(Integer.toString(number));

        return sb.toString();
    }
}
