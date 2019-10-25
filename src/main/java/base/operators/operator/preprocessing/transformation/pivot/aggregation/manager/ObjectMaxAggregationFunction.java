package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.Dictionary;
import base.operators.belt.column.Column.Category;
import base.operators.belt.reader.MixedRow;
import base.operators.belt.reader.NumericRow;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.MappingAggregationCollector.MappingIndexAggregationFunction;
import java.util.Comparator;

class ObjectMaxAggregationFunction<T> implements MappingIndexAggregationFunction {
    private final int rowIndex;
    private final Class<T> type;
    private final Comparator<T> comparator;
    private final Dictionary<T> dictionary;
    private T max = null;
    private int mappingIndex = -1;

    ObjectMaxAggregationFunction(ColumnType<T> columnType, Column column, int rowIndex) {
        this.type = columnType.elementType();
        this.comparator = Comparator.nullsFirst(columnType.comparator());
        if (columnType.category() == Category.CATEGORICAL) {
            this.dictionary = column.getDictionary(this.type);
        } else {
            this.dictionary = null;
        }

        this.rowIndex = rowIndex;
    }

    public void accept(NumericRow row) {
        int index = (int)row.get(this.rowIndex);
        T value = this.dictionary.get(index);
        this.accept(value, row.position());
    }

    public void accept(MixedRow row) {
        T value = row.getObject(this.rowIndex, this.type);
        this.accept(value, row.position());
    }

    private void accept(T value, int rowPosition) {
        if (this.comparator.compare(this.max, value) < 0) {
            this.max = value;
            this.mappingIndex = rowPosition;
        }

    }

    public void merge(AggregationFunction function) {
        ObjectMaxAggregationFunction<T> other = (ObjectMaxAggregationFunction)function;
        this.accept(other.max, other.mappingIndex);
    }

    public int getMappingIndex() {
        return this.mappingIndex;
    }
}
