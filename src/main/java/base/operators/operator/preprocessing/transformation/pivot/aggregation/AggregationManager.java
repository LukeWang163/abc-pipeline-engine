package base.operators.operator.preprocessing.transformation.pivot.aggregation;

import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;

public interface AggregationManager {
    ColumnType<?> checkColumnType(ColumnType<?> var1);

    void initialize(Column var1, int var2);

    AggregationFunction newFunction();

    AggregationCollector getCollector(int var1);

    int getIndex();

    String getAggregationName();
}
