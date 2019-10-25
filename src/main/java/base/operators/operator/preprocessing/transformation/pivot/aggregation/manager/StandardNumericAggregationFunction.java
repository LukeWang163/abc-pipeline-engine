package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.reader.MixedRow;
import base.operators.belt.reader.NumericRow;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.NumericAggregator;
import base.operators.belt.reader.MixedRow;
import base.operators.belt.reader.NumericRow;

class StandardNumericAggregationFunction implements NumericAggregationFunction {
    private final int rowIndex;
    private final NumericAggregator aggregator;
    private boolean foundNonNan = false;

    StandardNumericAggregationFunction(NumericAggregator aggregator, int rowIndex) {
        this.aggregator = aggregator;
        this.rowIndex = rowIndex;
    }

    public void accept(NumericRow row) {
        double value = row.get(this.rowIndex);
        if (!Double.isNaN(value)) {
            this.aggregator.accept(value);
            this.foundNonNan = true;
        }

    }

    public void accept(MixedRow row) {
        double value = row.getNumeric(this.rowIndex);
        if (!Double.isNaN(value)) {
            this.aggregator.accept(value);
            this.foundNonNan = true;
        }

    }

    public void merge(AggregationFunction function) {
        StandardNumericAggregationFunction other = (StandardNumericAggregationFunction)function;
        this.foundNonNan &= other.foundNonNan;
        this.aggregator.merge(other.aggregator);
    }

    public double getValue() {
        return this.foundNonNan ? this.aggregator.getValue() : 0.0D / 0.0;
    }
}
