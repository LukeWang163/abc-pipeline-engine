package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator;

import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.VarianceAggregator;

public class StandardDeviationAggregator implements NumericAggregator {
    private double valueSum = 0.0D;
    private double squaredValueSum = 0.0D;
    private double count = 0.0D;

    public StandardDeviationAggregator() {
    }

    public void accept(double value) {
        this.valueSum += value;
        this.squaredValueSum += value * value;
        ++this.count;
    }

    public void merge(NumericAggregator other) {
        StandardDeviationAggregator varOther = (StandardDeviationAggregator)other;
        this.valueSum += varOther.valueSum;
        this.squaredValueSum += varOther.squaredValueSum;
        this.count += varOther.count;
    }

    public double getValue() {
        if (this.count > 0.0D) {
            double value = VarianceAggregator.variance(this.squaredValueSum, this.valueSum, this.count);
            if (Double.isNaN(value)) {
                return 0.0D / 0.0;
            } else {
                return value > 0.0D ? Math.sqrt(value) : 0.0D;
            }
        } else {
            return 0.0D / 0.0;
        }
    }
}
