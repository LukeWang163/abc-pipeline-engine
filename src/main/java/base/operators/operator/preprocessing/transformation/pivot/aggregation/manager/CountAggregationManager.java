package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.ColumnTypes;
import base.operators.belt.column.Column.Capability;
import base.operators.belt.reader.MixedRow;
import base.operators.belt.reader.NumericRow;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationCollector;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationManager;

class CountAggregationManager implements AggregationManager {
    private final boolean ignoreMissings;
    private final CountAggregationManager.Mode mode;
    private int rowIndex;
    private boolean notNumericReadable;

    CountAggregationManager(boolean ignoreMissings, CountAggregationManager.Mode mode) {
        this.ignoreMissings = ignoreMissings;
        this.mode = mode;
    }

    public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
        return this.mode == CountAggregationManager.Mode.NORMAL ? ColumnTypes.INTEGER : ColumnTypes.REAL;
    }

    public void initialize(Column column, int indexInRowReader) {
        this.rowIndex = indexInRowReader;
        this.notNumericReadable = !column.type().hasCapability(Capability.NUMERIC_READABLE);
    }

    public AggregationFunction newFunction() {
        return new CountAggregationManager.CountAggregationFunction();
    }

    public AggregationCollector getCollector(int numberOfRows) {
        switch(this.mode) {
            case FRACTIONAL:
                return new FractionalNumericAggregationCollector(numberOfRows);
            case PERCENTAGE:
                return new PercentageNumericAggregationCollector(numberOfRows);
            case NORMAL:
            default:
                return new NumericBufferAggregationCollector(numberOfRows, true);
        }
    }

    public int getIndex() {
        return this.rowIndex;
    }

    public String getAggregationName() {
        String name;
        switch(this.mode) {
            case FRACTIONAL:
                name = "fractional_count";
                break;
            case PERCENTAGE:
                name = "percentage_count";
                break;
            case NORMAL:
            default:
                name = "count";
        }

        if (!this.ignoreMissings) {
            name = name + "_with_missings";
        }

        return name;
    }

    class CountAggregationFunction implements NumericAggregationFunction {
        private int count;

        CountAggregationFunction() {
        }

        public void accept(NumericRow row) {
            if (CountAggregationManager.this.ignoreMissings) {
                if (!Double.isNaN(row.get(CountAggregationManager.this.rowIndex))) {
                    ++this.count;
                }
            } else {
                ++this.count;
            }

        }

        public void accept(MixedRow row) {
            if (CountAggregationManager.this.ignoreMissings) {
                if (CountAggregationManager.this.notNumericReadable) {
                    if (row.getObject(CountAggregationManager.this.rowIndex) != null) {
                        ++this.count;
                    }
                } else if (!Double.isNaN(row.getNumeric(CountAggregationManager.this.rowIndex))) {
                    ++this.count;
                }
            } else {
                ++this.count;
            }

        }

        public void merge(AggregationFunction function) {
            CountAggregationManager.CountAggregationFunction other = (CountAggregationManager.CountAggregationFunction)function;
            this.count += other.count;
        }

        public double getValue() {
            return (double)this.count;
        }
    }

    static enum Mode {
        NORMAL,
        FRACTIONAL,
        PERCENTAGE;

        private Mode() {
        }
    }
}
