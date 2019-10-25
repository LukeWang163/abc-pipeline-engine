package base.operators.operator.preprocessing.transformation.pivot.aggregation;

import base.operators.belt.reader.MixedRow;
import base.operators.belt.reader.NumericRow;

public interface AggregationFunction {
    void accept(NumericRow var1);

    void accept(MixedRow var1);

    void merge(AggregationFunction var1);
}
