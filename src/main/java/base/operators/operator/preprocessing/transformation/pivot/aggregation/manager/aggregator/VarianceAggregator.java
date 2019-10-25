package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator;

public class VarianceAggregator implements NumericAggregator {
    private double valueSum = 0.0D;
    private double squaredValueSum = 0.0D;
    private double count = 0.0D;

    public VarianceAggregator() {
    }

    public void accept(double value) {
        this.valueSum += value;
        this.squaredValueSum += value * value;
        ++this.count;
    }

    public void merge(NumericAggregator other) {
        VarianceAggregator varOther = (VarianceAggregator)other;
        this.valueSum += varOther.valueSum;
        this.squaredValueSum += varOther.squaredValueSum;
        this.count += varOther.count;
    }

    public double getValue() {
        return this.count > 0.0D ? variance(this.squaredValueSum, this.valueSum, this.count) : 0.0D / 0.0;
    }

    static double variance(double squaredValueSum, double valueSum, double count) {
        return (squaredValueSum - valueSum * valueSum / count) / ((count - 1.0D) / count * count);
    }
}
