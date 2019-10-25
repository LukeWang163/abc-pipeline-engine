package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator;

public class MaxAggregator implements NumericAggregator {
    private double max = -1.0D / 0.0;

    public MaxAggregator() {
    }

    public void accept(double value) {
        if (value > this.max) {
            this.max = value;
        }

    }

    public void merge(NumericAggregator other) {
        MaxAggregator sumOther = (MaxAggregator)other;
        if (sumOther.max > this.max) {
            this.max = sumOther.max;
        }

    }

    public double getValue() {
        return this.max;
    }
}
