package base.operators.operator.features.optimization;

import base.operators.operator.OperatorException;

public interface ErrorCalculator {
    void calculateError(Individual var1) throws OperatorException;
}
