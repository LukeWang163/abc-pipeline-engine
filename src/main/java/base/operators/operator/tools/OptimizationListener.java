package base.operators.operator.tools;

public interface OptimizationListener {
    void nextGeneration(int var1);

    void optimizationStarted(int var1);

    void optimizationFinished();
}
