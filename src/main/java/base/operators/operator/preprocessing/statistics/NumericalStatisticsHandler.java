package base.operators.operator.preprocessing.statistics;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class NumericalStatisticsHandler implements Serializable {
    private static final long serialVersionUID = 6893758384992735499L;
    private Map<String, Map<NumericalStatisticsHandler.Type, Double>> allStatistics = new HashMap();

    NumericalStatisticsHandler() {
    }

    NumericalStatisticsHandler(NumericalStatisticsHandler other) {
        Iterator var2 = other.allStatistics.entrySet().iterator();

        while(var2.hasNext()) {
            Entry<String, Map<NumericalStatisticsHandler.Type, Double>> entry = (Entry)var2.next();
            this.allStatistics.put(entry.getKey(), entry.getValue());
        }

    }

    public Map<NumericalStatisticsHandler.Type, Double> getStatistics(Attribute attribute) {
        return this.getStatistics(attribute.getName());
    }

    public Map<NumericalStatisticsHandler.Type, Double> getStatistics(String attributeName) {
        return (Map)this.allStatistics.get(attributeName);
    }

    void addStatistics(ExampleSet exampleSet, Attribute attribute) {
        if (exampleSet != null && attribute != null) {
            if (attribute.isNumerical()) {
                Map<NumericalStatisticsHandler.Type, Double> statistics = new HashMap();
                statistics.put(NumericalStatisticsHandler.Type.MIN, exampleSet.getStatistics(attribute, "minimum"));
                statistics.put(NumericalStatisticsHandler.Type.MAX, exampleSet.getStatistics(attribute, "maximum"));
                statistics.put(NumericalStatisticsHandler.Type.AVG, exampleSet.getStatistics(attribute, "average"));
                double variance = exampleSet.getStatistics(attribute, "variance");
                statistics.put(NumericalStatisticsHandler.Type.VAR, variance);
                statistics.put(NumericalStatisticsHandler.Type.STD_DEV, Math.sqrt(variance));
                statistics.put(NumericalStatisticsHandler.Type.LOWER_QUARTILE, exampleSet.getStatistics(attribute, "lower_quartile"));
                statistics.put(NumericalStatisticsHandler.Type.HIGHER_QUARTILE, exampleSet.getStatistics(attribute, "higher_quartile"));
                this.allStatistics.put(attribute.getName(), statistics);
            }

        }
    }

    void clear() {
        this.allStatistics.clear();
    }

    public static enum Type {
        MIN,
        MAX,
        AVG,
        VAR,
        STD_DEV,
        LOWER_QUARTILE,
        HIGHER_QUARTILE;

        private Type() {
        }
    }
}
