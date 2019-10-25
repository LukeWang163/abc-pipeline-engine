package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.buffer.Buffers;
import base.operators.belt.buffer.NumericBuffer;
import base.operators.belt.column.Column;
import base.operators.belt.execution.Context;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationCollector;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;

class FractionalNumericAggregationCollector implements AggregationCollector {
    private final NumericBuffer buffer;
    private double totalSum;

    FractionalNumericAggregationCollector(int numberOfRows) {
        this.buffer = Buffers.realBuffer(numberOfRows);
    }

    public void set(int index, AggregationFunction function) {
        double value = ((NumericAggregationFunction)function).getValue();
        if (!Double.isNaN(value)) {
            if (value < 0.0D) {
                this.totalSum = 0.0D / 0.0;
            } else {
                this.totalSum += value;
            }
        }

        this.buffer.set(index, value);
    }

    public Column getResult(Context context) {
        for(int i = 0; i < this.buffer.size(); ++i) {
            this.buffer.set(i, this.buffer.get(i) / this.totalSum);
        }

        return this.buffer.toColumn();
    }
}
