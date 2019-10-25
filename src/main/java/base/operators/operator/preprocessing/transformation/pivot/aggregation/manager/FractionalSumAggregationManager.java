package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.ColumnTypes;
import base.operators.belt.column.Column.Category;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationCollector;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationManager;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.NumericAggregator;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.SumAggregator;


import java.util.function.Supplier;

class FractionalSumAggregationManager implements AggregationManager {
    private static final String NAME = "fractional_sum";
    private final Supplier<NumericAggregator> supplier = SumAggregator::new;
    private int rowIndex;

    FractionalSumAggregationManager() {
    }

    public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
        return inputType.category() != Category.NUMERIC ? null : ColumnTypes.REAL;
    }

    public void initialize(Column column, int indexInRowReader) {
        if (column.type().category() != Category.NUMERIC) {
            throw new IllegalArgumentException("Only numeric columns for numeric aggregation");
        } else {
            this.rowIndex = indexInRowReader;
        }
    }

    public AggregationFunction newFunction() {
        return new StandardNumericAggregationFunction((NumericAggregator)this.supplier.get(), this.rowIndex);
    }

    public AggregationCollector getCollector(int numberOfRows) {
        return new FractionalNumericAggregationCollector(numberOfRows);
    }

    public int getIndex() {
        return this.rowIndex;
    }

    public String getAggregationName() {
        return "fractional_sum";
    }
}
