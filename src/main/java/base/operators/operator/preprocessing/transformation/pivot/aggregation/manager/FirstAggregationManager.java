package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.Column.Capability;
import base.operators.belt.reader.MixedRow;
import base.operators.belt.reader.NumericRow;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationCollector;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationManager;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.MappingAggregationCollector.MappingIndexAggregationFunction;

class FirstAggregationManager implements AggregationManager {
    static final String NAME = "first";
    private int rowIndex;
    private Column column;
    private boolean notNumericReadable;

    FirstAggregationManager() {
    }

    public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
        return inputType;
    }

    public void initialize(Column column, int indexInRowReader) {
        this.rowIndex = indexInRowReader;
        this.column = column;
        this.notNumericReadable = !column.type().hasCapability(Capability.NUMERIC_READABLE);
    }

    public FirstAggregationManager.FirstAggregationFunction newFunction() {
        return new FirstAggregationManager.FirstAggregationFunction();
    }

    public AggregationCollector getCollector(int numberOfRows) {
        return new MappingAggregationCollector(numberOfRows, this.column);
    }

    public int getIndex() {
        return this.rowIndex;
    }

    public String getAggregationName() {
        return "first";
    }

    class FirstAggregationFunction implements MappingIndexAggregationFunction {
        private int mappingIndex = -1;

        FirstAggregationFunction() {
        }

        public void accept(NumericRow row) {
            if (this.mappingIndex < 0 && !Double.isNaN(row.get(FirstAggregationManager.this.rowIndex))) {
                this.mappingIndex = row.position();
            }

        }

        public void accept(MixedRow row) {
            if (this.mappingIndex < 0 && this.isNotMissing(row)) {
                this.mappingIndex = row.position();
            }

        }

        private boolean isNotMissing(MixedRow row) {
            if (FirstAggregationManager.this.notNumericReadable) {
                return row.getObject(FirstAggregationManager.this.rowIndex) != null;
            } else {
                return !Double.isNaN(row.getNumeric(FirstAggregationManager.this.rowIndex));
            }
        }

        public void merge(AggregationFunction function) {
            FirstAggregationManager.FirstAggregationFunction other = (FirstAggregationManager.FirstAggregationFunction)function;
            if (this.mappingIndex < 0) {
                this.mappingIndex = other.mappingIndex;
            }

        }

        public int getMappingIndex() {
            return this.mappingIndex;
        }
    }
}
