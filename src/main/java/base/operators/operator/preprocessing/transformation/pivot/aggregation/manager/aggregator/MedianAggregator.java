package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator;

import base.operators.operator.preprocessing.transformation.aggregation.MedianAggregator.VariableDoubleArray;

public class MedianAggregator implements NumericAggregator {
    private VariableDoubleArray values = null;
    private int count = 0;

    public MedianAggregator() {
    }

    public void accept(double value) {
        if (this.count == 0) {
            this.values = new VariableDoubleArray();
        }

        this.values.add(value);
        ++this.count;
    }

    public void merge(NumericAggregator other) {
        MedianAggregator medianOther = (MedianAggregator)other;
        if (this.values == null) {
            this.values = medianOther.values;
        } else if (medianOther.values != null) {
            this.values.addAll(medianOther.values);
        }

        this.count += medianOther.count;
    }

    public double getValue() {
        return this.count == 0 ? 0.0D / 0.0 : base.operators.operator.preprocessing.transformation.aggregation.MedianAggregator.quickNth(this.values, (double)this.count / 2.0D);
    }
}
