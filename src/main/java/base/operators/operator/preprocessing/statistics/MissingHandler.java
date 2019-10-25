package base.operators.operator.preprocessing.statistics;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MissingHandler implements Serializable {
    private static final long serialVersionUID = 3147789882401241967L;
    private Map<String, Integer> missingCounts = new HashMap();

    MissingHandler() {
    }

    MissingHandler(MissingHandler other) {
        Iterator var2 = other.missingCounts.entrySet().iterator();

        while(var2.hasNext()) {
            Entry<String, Integer> entry = (Entry)var2.next();
            this.missingCounts.put(entry.getKey(), entry.getValue());
        }

    }

    public int getMissing(Attribute attribute) {
        return this.getMissing(attribute.getName());
    }

    public int getMissing(String attributeName) {
        return (Integer)this.missingCounts.get(attributeName);
    }

    void addMissing(ExampleSet exampleSet, Attribute attribute) {
        if (exampleSet != null && attribute != null) {
            int count = (int)exampleSet.getStatistics(attribute, "unknown");
            this.missingCounts.put(attribute.getName(), count);
        }
    }

    void clear() {
        this.missingCounts.clear();
    }
}
