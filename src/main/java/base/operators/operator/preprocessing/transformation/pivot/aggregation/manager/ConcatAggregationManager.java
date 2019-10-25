package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.buffer.Buffers;
import base.operators.belt.buffer.CategoricalBuffer;
import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.ColumnTypes;
import base.operators.belt.column.Dictionary;
import base.operators.belt.column.Column.TypeId;
import base.operators.belt.execution.Context;
import base.operators.belt.reader.MixedRow;
import base.operators.belt.reader.NumericRow;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationCollector;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationManager;
import java.util.StringJoiner;

class ConcatAggregationManager implements AggregationManager {
    private static final String NAME = "concat";
    private static final String DELIMITER = "|";
    private int rowIndex;
    private Dictionary<String> dictionary;

    ConcatAggregationManager() {
    }

    public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
        return inputType.id() != TypeId.NOMINAL ? null : inputType;
    }

    public void initialize(Column column, int indexInRowReader) {
        if (column.type().id() != TypeId.NOMINAL) {
            throw new IllegalArgumentException("Only nominal columns for concatenation");
        } else {
            this.dictionary = column.getDictionary(String.class);
            this.rowIndex = indexInRowReader;
        }
    }

    public ConcatAggregationManager.ConcatenationAggregationFunction newFunction() {
        return new ConcatAggregationManager.ConcatenationAggregationFunction();
    }

    public ConcatAggregationManager.StringCollector getCollector(int numberOfRows) {
        return new ConcatAggregationManager.StringCollector(numberOfRows);
    }

    public int getIndex() {
        return this.rowIndex;
    }

    public String getAggregationName() {
        return "concat";
    }

    class StringCollector implements AggregationCollector {
        private final CategoricalBuffer<String> buffer;

        StringCollector(int size) {
            this.buffer = Buffers.categoricalBuffer(size);
        }

        public void set(int index, AggregationFunction function) {
            this.buffer.set(index, ((ConcatAggregationManager.ConcatenationAggregationFunction)function).getResult());
        }

        public Column getResult(Context context) {
            return this.buffer.toColumn(ColumnTypes.NOMINAL);
        }
    }

    class ConcatenationAggregationFunction implements AggregationFunction {
        private StringJoiner joiner = new StringJoiner("|");
        private boolean added = false;

        ConcatenationAggregationFunction() {
        }

        public void accept(NumericRow row) {
            int index = (int)row.get(ConcatAggregationManager.this.rowIndex);
            String value = (String)ConcatAggregationManager.this.dictionary.get(index);
            if (value != null) {
                this.joiner.add(value);
                this.added = true;
            }

        }

        public void accept(MixedRow row) {
            String value = (String)row.getObject(ConcatAggregationManager.this.rowIndex, String.class);
            if (value != null) {
                this.joiner.add(value);
                this.added = true;
            }

        }

        public void merge(AggregationFunction function) {
            ConcatAggregationManager.ConcatenationAggregationFunction other = (ConcatAggregationManager.ConcatenationAggregationFunction)function;
            this.joiner.merge(other.joiner);
        }

        public String getResult() {
            return this.added ? this.joiner.toString() : null;
        }
    }
}
