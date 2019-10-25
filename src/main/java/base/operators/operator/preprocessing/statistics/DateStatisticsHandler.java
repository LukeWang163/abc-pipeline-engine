package base.operators.operator.preprocessing.statistics;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.gui.viewer.metadata.model.DateTimeAttributeStatisticsModel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class DateStatisticsHandler implements Serializable {
    private static final long serialVersionUID = -6210876572788847717L;
    private Map<String, Map<DateStatisticsHandler.Type, String>> allStatistics = new HashMap();

    DateStatisticsHandler() {
    }

    DateStatisticsHandler(DateStatisticsHandler other) {
        Iterator var2 = other.allStatistics.entrySet().iterator();

        while(var2.hasNext()) {
            Entry<String, Map<DateStatisticsHandler.Type, String>> entry = (Entry)var2.next();
            this.allStatistics.put(entry.getKey(), entry.getValue());
        }

    }

    public Map<DateStatisticsHandler.Type, String> getStatistics(Attribute attribute) {
        return this.getStatistics(attribute.getName());
    }

    public Map<DateStatisticsHandler.Type, String> getStatistics(String attributeName) {
        return (Map)this.allStatistics.get(attributeName);
    }

    void addStatistics(ExampleSet exampleSet, Attribute attribute) {
        if (exampleSet != null && attribute != null) {
            if (attribute.isDateTime()) {
                DateTimeAttributeStatisticsModel statisticsModel = new DateTimeAttributeStatisticsModel(exampleSet, attribute);
                statisticsModel.updateStatistics(exampleSet);
                Map<DateStatisticsHandler.Type, String> valueMap = new HashMap();
                valueMap.put(DateStatisticsHandler.Type.FROM, statisticsModel.getFrom());
                valueMap.put(DateStatisticsHandler.Type.UNTIL, statisticsModel.getUntil());
                valueMap.put(DateStatisticsHandler.Type.DURATION, statisticsModel.getDuration());
                valueMap.put(DateStatisticsHandler.Type.MEAN, statisticsModel.getMean());
                this.allStatistics.put(attribute.getName(), valueMap);
            }

        }
    }

    void clear() {
        this.allStatistics.clear();
    }

    public static enum Type {
        FROM,
        UNTIL,
        DURATION,
        MEAN;

        private Type() {
        }
    }
}
