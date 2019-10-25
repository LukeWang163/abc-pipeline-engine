package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator;

public class SumAggregator implements NumericAggregator {
    private double sum = 0.0D;

    public SumAggregator() {
    }

    public void accept(double value) {
        this.sum += value;
    }

    public void merge(NumericAggregator other) {
        SumAggregator sumOther = (SumAggregator)other;
        this.sum += sumOther.sum;
    }

    public double getValue() {
        return this.sum;
    }
}
