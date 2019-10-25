package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.Column.Category;
import base.operators.belt.reader.MixedRow;
import base.operators.belt.reader.NumericRow;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationCollector;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationManager;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.MappingAggregationCollector.MappingIndexAggregationFunction;

class CategoricalAppearanceAggregationManager implements AggregationManager {
    private static final String NAME_MOST = "mode";
    private static final String NAME_LEAST = "least";
    private final CategoricalAppearanceAggregationManager.Mode mode;
    private Column column;
    private int dictionarySize;
    private int rowIndex;

    CategoricalAppearanceAggregationManager(CategoricalAppearanceAggregationManager.Mode mode) {
        this.mode = mode;
    }

    public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
        return inputType.category() != Category.CATEGORICAL ? null : inputType;
    }

    public void initialize(Column column, int indexInRowReader) {
        if (column.type().category() != Category.CATEGORICAL) {
            throw new IllegalArgumentException("Only categorical columns for mode/least");
        } else {
            this.column = column;
            this.dictionarySize = column.getDictionary(Object.class).maximalIndex() + 1;
            this.rowIndex = indexInRowReader;
        }
    }

    public CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction newFunction() {
        return new CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction();
    }

    public AggregationCollector getCollector(int numberOfRows) {
        return new MappingAggregationCollector(numberOfRows, this.column);
    }

    public int getIndex() {
        return this.rowIndex;
    }

    public String getAggregationName() {
        return this.mode == CategoricalAppearanceAggregationManager.Mode.LEAST ? "least" : "mode";
    }

    class CategoricalModeLeastAggregationFunction implements MappingIndexAggregationFunction {
        private final int[] appearingCounter;
        private final int[] appearingIndex;

        CategoricalModeLeastAggregationFunction() {
            this.appearingCounter = new int[CategoricalAppearanceAggregationManager.this.dictionarySize];
            this.appearingIndex = new int[CategoricalAppearanceAggregationManager.this.dictionarySize];
        }

        public void accept(NumericRow row) {
            int index = (int)row.get(CategoricalAppearanceAggregationManager.this.rowIndex);
            int var10002 = this.appearingCounter[index]++;
            this.appearingIndex[index] = row.position();
        }

        public void accept(MixedRow row) {
            int index = row.getIndex(CategoricalAppearanceAggregationManager.this.rowIndex);
            int var10002 = this.appearingCounter[index]++;
            this.appearingIndex[index] = row.position();
        }

        public void merge(AggregationFunction function) {
            CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction other = (CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction)function;

            for(int i = 0; i < this.appearingCounter.length; ++i) {
                if (this.appearingCounter[i] == 0) {
                    this.appearingIndex[i] = other.appearingIndex[i];
                }

                int[] var10000 = this.appearingCounter;
                var10000[i] += other.appearingCounter[i];
            }

        }

        public int getMappingIndex() {
            return CategoricalAppearanceAggregationManager.this.mode == CategoricalAppearanceAggregationManager.Mode.LEAST ? this.least() : this.most();
        }

        private int least() {
            int arrayIndex = -1;
            int minCount = 2147483647;

            for(int i = 1; i < this.appearingCounter.length; ++i) {
                if (this.appearingCounter[i] < minCount && this.appearingCounter[i] > 0) {
                    minCount = this.appearingCounter[i];
                    arrayIndex = this.appearingIndex[i];
                }
            }

            return arrayIndex;
        }

        private int most() {
            int arrayIndex = -1;
            int maxCount = 0;

            for(int i = 1; i < this.appearingCounter.length; ++i) {
                if (this.appearingCounter[i] > maxCount) {
                    maxCount = this.appearingCounter[i];
                    arrayIndex = this.appearingIndex[i];
                }
            }

            return arrayIndex;
        }
    }

    static enum Mode {
        MOST,
        LEAST;

        private Mode() {
        }
    }
}
