package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.column.Column;
import base.operators.belt.column.Columns;
import base.operators.belt.column.Columns.CleanupOption;
import base.operators.belt.execution.Context;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationCollector;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;
import java.util.Arrays;

class MappingAggregationCollector implements AggregationCollector {
    private final int[] mapping;
    private final Column column;

    MappingAggregationCollector(int numberOfRows, Column column) {
        this.column = column;
        this.mapping = new int[numberOfRows];
        Arrays.fill(this.mapping, -1);
    }

    public void set(int index, AggregationFunction function) {
        this.mapping[index] = ((MappingAggregationCollector.MappingIndexAggregationFunction)function).getMappingIndex();
    }

    public Column getResult(Context context) {
        return Columns.removeUnusedDictionaryValues(this.column.rows(this.mapping, false), CleanupOption.COMPACT, context);
    }

    interface MappingIndexAggregationFunction extends AggregationFunction {
        int getMappingIndex();
    }
}
