package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator;

public interface NumericAggregator {
    void accept(double var1);

    void merge(NumericAggregator var1);

    double getValue();
}