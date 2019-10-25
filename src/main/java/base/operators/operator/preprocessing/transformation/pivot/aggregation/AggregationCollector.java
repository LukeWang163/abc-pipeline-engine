package base.operators.operator.preprocessing.transformation.pivot.aggregation;

import base.operators.belt.column.Column;
import base.operators.belt.execution.Context;

public interface AggregationCollector {
    void set(int var1, AggregationFunction var2);

    Column getResult(Context var1);
}
