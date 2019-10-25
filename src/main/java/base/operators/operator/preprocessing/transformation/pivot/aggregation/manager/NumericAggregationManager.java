package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.ColumnTypes;
import base.operators.belt.column.Column.Category;
import base.operators.belt.column.Column.TypeId;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationCollector;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationManager;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.NumericAggregator;
import java.util.function.Supplier;

class NumericAggregationManager implements AggregationManager {
    private final String name;
    private final Supplier<NumericAggregator> supplier;
    private final boolean supportsIntegers;
    private int rowIndex;
    private boolean integer = false;

    NumericAggregationManager(String name, Supplier<NumericAggregator> supplier, boolean supportsIntegers) {
        this.name = name;
        this.supplier = supplier;
        this.supportsIntegers = supportsIntegers;
    }

    public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
        if (inputType.category() != Category.NUMERIC) {
            return null;
        } else {
            return this.supportsIntegers && inputType.id() == TypeId.INTEGER ? ColumnTypes.INTEGER : ColumnTypes.REAL;
        }
    }

    public void initialize(Column column, int indexInRowReader) {
        if (column.type().category() != Category.NUMERIC) {
            throw new IllegalArgumentException("Only numeric columns for numeric aggregation");
        } else {
            if (this.supportsIntegers && column.type().id() == TypeId.INTEGER) {
                this.integer = true;
            }

            this.rowIndex = indexInRowReader;
        }
    }

    public StandardNumericAggregationFunction newFunction() {
        return new StandardNumericAggregationFunction((NumericAggregator)this.supplier.get(), this.rowIndex);
    }

    public AggregationCollector getCollector(int numberOfRows) {
        return new NumericBufferAggregationCollector(numberOfRows, this.integer);
    }

    public int getIndex() {
        return this.rowIndex;
    }

    public String getAggregationName() {
        return this.name;
    }
}
