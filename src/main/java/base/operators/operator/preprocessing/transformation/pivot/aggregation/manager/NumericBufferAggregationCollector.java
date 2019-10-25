package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.buffer.Buffers;
import base.operators.belt.buffer.NumericBuffer;
import base.operators.belt.column.Column;
import base.operators.belt.execution.Context;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationCollector;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;

class NumericBufferAggregationCollector implements AggregationCollector {
    private final NumericBuffer buffer;

    NumericBufferAggregationCollector(int numberOfRows, boolean integer) {
        this.buffer = integer ? Buffers.integerBuffer(numberOfRows) : Buffers.realBuffer(numberOfRows);
    }

    public void set(int index, AggregationFunction function) {
        this.buffer.set(index, ((NumericAggregationFunction)function).getValue());
    }

    public Column getResult(Context context) {
        return this.buffer.toColumn();
    }
}
