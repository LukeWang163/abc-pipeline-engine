package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator;

public class LogProductAggregator implements NumericAggregator {
    private double logSum = 0.0D;

    public LogProductAggregator() {
    }

    public void accept(double value) {
        this.logSum += Math.log(value);
    }

    public void merge(NumericAggregator other) {
        LogProductAggregator productOther = (LogProductAggregator)other;
        this.logSum += productOther.logSum;
    }

    public double getValue() {
        return this.logSum;
    }
}
