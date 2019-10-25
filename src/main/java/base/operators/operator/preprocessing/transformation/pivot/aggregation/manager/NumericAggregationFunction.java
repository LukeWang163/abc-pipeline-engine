package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;

interface NumericAggregationFunction extends AggregationFunction {
    double getValue();
}

