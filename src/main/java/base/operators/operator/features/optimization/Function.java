package base.operators.operator.features.optimization;

public class Function {
    private String functionName;
    private int numberOfArguments;
    private boolean inline;
    private boolean needsDifferentArguments;
    private int complexity;

    public Function(String functionName, int numberOfArguments, boolean needsDifferentArguments, boolean inline, int complexity) {
        this.functionName = functionName;
        this.numberOfArguments = numberOfArguments;
        this.needsDifferentArguments = needsDifferentArguments;
        this.inline = inline;
        this.complexity = complexity;
    }

    int getNumberOfArguments() {
        return this.numberOfArguments;
    }

    boolean needsDifferentArguments() {
        return this.needsDifferentArguments;
    }

    int getComplexity() {
        return this.complexity;
    }

    String createExpression(String... arguments) {
        if (arguments.length != this.numberOfArguments) {
            throw new IllegalArgumentException("Wrong number of arguments for function '" + this.functionName + "': " + arguments.length + ", needs: " + this.numberOfArguments);
        } else {
            StringBuilder result;
            boolean first;
            String[] var4;
            int var5;
            int var6;
            String argument;
            if (this.inline) {
                if (this.numberOfArguments == 1) {
                    return this.functionName + arguments[0];
                } else {
                    result = new StringBuilder();
                    first = true;
                    var4 = arguments;
                    var5 = arguments.length;

                    for(var6 = 0; var6 < var5; ++var6) {
                        argument = var4[var6];
                        if (!first) {
                            result.append(this.functionName);
                        }

                        result.append(argument);
                        first = false;
                    }

                    return result.toString();
                }
            } else {
                result = new StringBuilder();
                result.append(this.functionName);
                result.append("(");
                first = true;
                var4 = arguments;
                var5 = arguments.length;

                for(var6 = 0; var6 < var5; ++var6) {
                    argument = var4[var6];
                    if (!first) {
                        result.append(",");
                    }

                    result.append(argument);
                    first = false;
                }

                result.append(")");
                return result.toString();
            }
        }
    }
}
