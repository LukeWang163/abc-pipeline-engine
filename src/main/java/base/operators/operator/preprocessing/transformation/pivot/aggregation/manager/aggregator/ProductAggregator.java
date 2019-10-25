package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator;

public class ProductAggregator implements NumericAggregator {
    private double product = 1.0D;

    public ProductAggregator() {
    }

    public void accept(double value) {
        this.product *= value;
    }

    public void merge(NumericAggregator other) {
        ProductAggregator productOther = (ProductAggregator)other;
        this.product *= productOther.product;
    }

    public double getValue() {
        return this.product;
    }
}
