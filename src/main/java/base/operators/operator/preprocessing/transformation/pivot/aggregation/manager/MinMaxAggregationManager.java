package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.Column.Capability;
import base.operators.belt.column.Column.Category;
import base.operators.belt.column.Column.TypeId;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationCollector;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationManager;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.MaxAggregator;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.MinAggregator;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.NumericAggregator;

class MinMaxAggregationManager implements AggregationManager {
    private final MinMaxAggregationManager.Mode mode;
    private int rowIndex;
    private Column column;
    private boolean asSortableObjects;

    MinMaxAggregationManager(MinMaxAggregationManager.Mode mode) {
        this.mode = mode;
    }

    public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
        return inputType.id() != TypeId.INTEGER && inputType.id() != TypeId.REAL && (inputType.category() != Category.CATEGORICAL && inputType.category() != Category.OBJECT || inputType.comparator() == null) ? null : inputType;
    }

    public void initialize(Column column, int indexInRowReader) {
        this.rowIndex = indexInRowReader;
        this.column = column;
        if (column.type().hasCapability(Capability.OBJECT_READABLE) && column.type().comparator() != null) {
            this.asSortableObjects = true;
        }

        if (!this.asSortableObjects && column.type().id() != TypeId.INTEGER && column.type().id() != TypeId.REAL) {
            throw new IllegalArgumentException("Min and max not defined for this type of column");
        }
    }

    public AggregationFunction newFunction() {
        if (this.asSortableObjects) {
            return (AggregationFunction)(this.mode == MinMaxAggregationManager.Mode.MAX ? new ObjectMaxAggregationFunction(this.column.type(), this.column, this.rowIndex) : new ObjectMinAggregationFunction(this.column.type(), this.column, this.rowIndex));
        } else {
            return new StandardNumericAggregationFunction((NumericAggregator)(this.mode == MinMaxAggregationManager.Mode.MAX ? new MaxAggregator() : new MinAggregator()), this.rowIndex);
        }
    }

    public AggregationCollector getCollector(int numberOfRows) {
        return (AggregationCollector)(this.asSortableObjects ? new MappingAggregationCollector(numberOfRows, this.column) : new NumericBufferAggregationCollector(numberOfRows, this.column.type().id() == TypeId.INTEGER));
    }

    public int getIndex() {
        return this.rowIndex;
    }

    public String getAggregationName() {
        return this.mode == MinMaxAggregationManager.Mode.MAX ? "maximum" : "minimum";
    }

    static enum Mode {
        MIN,
        MAX;

        private Mode() {
        }
    }
}
