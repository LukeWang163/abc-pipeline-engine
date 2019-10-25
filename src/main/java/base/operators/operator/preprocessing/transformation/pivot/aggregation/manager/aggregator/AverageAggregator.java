package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator;

public class AverageAggregator implements NumericAggregator {
    private double sum = 0.0D;
    private int count = 0;

    public AverageAggregator() {
    }

    public void accept(double value) {
        this.sum += value;
        ++this.count;
    }

    public void merge(NumericAggregator other) {
        AverageAggregator averageOther = (AverageAggregator)other;
        this.sum += averageOther.sum;
        this.count += averageOther.count;
    }

    public double getValue() {
        return this.sum / (double)this.count;
    }
}
