package base.operators.operator.preprocessing.transformation.pivot.aggregation.manager;

import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationManager;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.CategoricalAppearanceAggregationManager.Mode;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.AverageAggregator;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.LogProductAggregator;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.MedianAggregator;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.ProductAggregator;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.StandardDeviationAggregator;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.SumAggregator;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.aggregator.VarianceAggregator;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

public enum AggregationManagers {
    INSTANCE;

    public static final String FUNCTION_NAME_AVERAGE = "average";
    public static final String FUNCTION_NAME_SUM = "sum";
    public static final String FUNCTION_NAME_MEDIAN = "median";
    public static final String FUNCTION_NAME_VARIANCE = "variance";
    public static final String FUNCTION_NAME_STANDARD_DEVIATION = "standard deviation";
    public static final String FUNCTION_NAME_FRACTIONAL_SUM = "sum (fractional)";
    public static final String FUNCTION_NAME_PRODUCT = "product";
    public static final String FUNCTION_NAME_LOG_PRODUCT = "log product";
    public static final String FUNCTION_NAME_MODE = "mode";
    public static final String FUNCTION_NAME_LEAST = "least";
    public static final String FUNCTION_NAME_CONCATENATION = "concatenation";
    public static final String FUNCTION_NAME_MINIMUM = "minimum";
    public static final String FUNCTION_NAME_MAXIMUM = "maximum";
    public static final String FUNCTION_NAME_FIRST = "first";
    public static final String FUNCTION_NAME_COUNT = "count";
    public static final String FUNCTION_NAME_COUNT_INCLUDING_MISSINGS = "count (including missings)";
    public static final String FUNCTION_NAME_COUNT_FRACTIONAL = "count (fractional)";
    public static final String FUNCTION_NAME_COUNT_PERCENTAGE = "count (percentage)";
    private final Map<String, Supplier<AggregationManager>> aggregationManagerMap;

    private AggregationManagers() {
        Map<String, Supplier<AggregationManager>> tempAggregationManagerMap = new TreeMap();
        tempAggregationManagerMap.put("average", () -> {
            return new NumericAggregationManager("average", AverageAggregator::new, false);
        });
        tempAggregationManagerMap.put("sum", () -> {
            return new NumericAggregationManager("sum", SumAggregator::new, true);
        });
        tempAggregationManagerMap.put("median", () -> {
            return new NumericAggregationManager("median", MedianAggregator::new, false);
        });
        tempAggregationManagerMap.put("variance", () -> {
            return new NumericAggregationManager("variance", VarianceAggregator::new, false);
        });
        tempAggregationManagerMap.put("standard deviation", () -> {
            return new NumericAggregationManager("standard_deviation", StandardDeviationAggregator::new, false);
        });
        tempAggregationManagerMap.put("sum (fractional)", FractionalSumAggregationManager::new);
        tempAggregationManagerMap.put("product", () -> {
            return new NumericAggregationManager("product", ProductAggregator::new, false);
        });
        tempAggregationManagerMap.put("log product", () -> {
            return new NumericAggregationManager("log_product", LogProductAggregator::new, false);
        });
        tempAggregationManagerMap.put("mode", () -> {
            return new CategoricalAppearanceAggregationManager(Mode.MOST);
        });
        tempAggregationManagerMap.put("least", () -> {
            return new CategoricalAppearanceAggregationManager(Mode.LEAST);
        });
        tempAggregationManagerMap.put("concatenation", ConcatAggregationManager::new);
        tempAggregationManagerMap.put("minimum", () -> {
            return new MinMaxAggregationManager(base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.MinMaxAggregationManager.Mode.MIN);
        });
        tempAggregationManagerMap.put("maximum", () -> {
            return new MinMaxAggregationManager(base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.MinMaxAggregationManager.Mode.MAX);
        });
        tempAggregationManagerMap.put("first", FirstAggregationManager::new);
        tempAggregationManagerMap.put("count", () -> {
            return new CountAggregationManager(true, base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.CountAggregationManager.Mode.NORMAL);
        });
        tempAggregationManagerMap.put("count (including missings)", () -> {
            return new CountAggregationManager(false, base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.CountAggregationManager.Mode.NORMAL);
        });
        tempAggregationManagerMap.put("count (fractional)", () -> {
            return new CountAggregationManager(true, base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.CountAggregationManager.Mode.FRACTIONAL);
        });
        tempAggregationManagerMap.put("count (percentage)", () -> {
            return new CountAggregationManager(true, base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.CountAggregationManager.Mode.PERCENTAGE);
        });
        this.aggregationManagerMap = Collections.unmodifiableMap(tempAggregationManagerMap);
    }

    public Map<String, Supplier<AggregationManager>> getAggregationManagers() {
        return this.aggregationManagerMap;
    }

    public Set<String> getAggregationFunctionNames() {
        return this.aggregationManagerMap.keySet();
    }
}
