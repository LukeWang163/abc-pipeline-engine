package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator;

public class MinAggregator implements NumericAggregator {
    private double min = 1.0D / 0.0;

    public MinAggregator() {
    }

    public void accept(double value) {
        if (value < this.min) {
            this.min = value;
        }

    }

    public void merge(NumericAggregator other) {
        MinAggregator sumOther = (MinAggregator)other;
        if (sumOther.min < this.min) {
            this.min = sumOther.min;
        }

    }

    public double getValue() {
        return this.min;
    }
}
