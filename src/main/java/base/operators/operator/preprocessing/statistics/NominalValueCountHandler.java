package base.operators.operator.preprocessing.statistics;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class NominalValueCountHandler implements Serializable {
    private static final long serialVersionUID = 6284625350350851167L;
    private Map<String, Integer> totalSize = new HashMap();
    private Map<String, Integer> nonMissingSize = new HashMap();
    private Map<String, Map<String, Integer>> valueCounts = new HashMap();
    private Map<String, String> modeValues = new HashMap();
    private Map<String, Integer> modeCounts = new HashMap();
    private Map<String, String> leastValues = new HashMap();
    private Map<String, Integer> leastCounts = new HashMap();

    NominalValueCountHandler() {
    }

    NominalValueCountHandler(NominalValueCountHandler nominalValueCounts) {
        Iterator var2 = nominalValueCounts.totalSize.entrySet().iterator();

        Entry entry;
        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.totalSize.put((String)entry.getKey(), (Integer)entry.getValue());
        }

        var2 = nominalValueCounts.nonMissingSize.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.nonMissingSize.put((String)entry.getKey(), (Integer) entry.getValue());
        }

        var2 = nominalValueCounts.valueCounts.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.valueCounts.put((String)entry.getKey(), (Map<String, Integer>)entry.getValue());
        }

        var2 = nominalValueCounts.modeValues.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.modeValues.put((String)entry.getKey(), (String)entry.getValue());
        }

        var2 = nominalValueCounts.modeCounts.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.modeCounts.put((String)entry.getKey(), (Integer)entry.getValue());
        }

        var2 = nominalValueCounts.leastValues.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.leastValues.put((String)entry.getKey(), (String)entry.getValue());
        }

        var2 = nominalValueCounts.leastCounts.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.leastCounts.put((String)entry.getKey(), (Integer)entry.getValue());
        }

    }

    public Map<String, Integer> getValueCounts(Attribute attribute) {
        return this.getValueCounts(attribute.getName());
    }

    public Map<String, Integer> getValueCounts(String attributeName) {
        return (Map)this.valueCounts.get(attributeName);
    }

    public String getMode(Attribute attribute) {
        return this.getMode(attribute.getName());
    }

    public String getMode(String attributeName) {
        return (String)this.modeValues.get(attributeName);
    }

    public int getModeCount(Attribute attribute) {
        return this.getModeCount(attribute.getName());
    }

    public int getModeCount(String attributeName) {
        return (Integer)this.modeCounts.get(attributeName);
    }

    public String getLeast(Attribute attribute) {
        return this.getLeast(attribute.getName());
    }

    public String getLeast(String attributeName) {
        return (String)this.leastValues.get(attributeName);
    }

    public int getLeastCount(Attribute attribute) {
        return this.getLeastCount(attribute.getName());
    }

    public int getLeastCount(String attributeName) {
        return (Integer)this.leastCounts.get(attributeName);
    }

    public int getTotalSize(Attribute attribute) {
        return this.getTotalSize(attribute.getName());
    }

    public int getTotalSize(String attributeName) {
        return (Integer)this.totalSize.get(attributeName);
    }

    public int getNonMissingSize(Attribute attribute) {
        return this.getNonMissingSize(attribute.getName());
    }

    public int getNonMissingSize(String attributeName) {
        return (Integer)this.nonMissingSize.get(attributeName);
    }

    void addValueCounts(ExampleSet exampleSet, Attribute attribute) {
        if (exampleSet != null && attribute != null) {
            if (attribute.isNominal()) {
                String mode = null;
                int modeCount = 0;
                String least = null;
                int leastCount = 2147483647;
                Map<String, Integer> counts = new HashMap();
                Iterator var8 = attribute.getMapping().getValues().iterator();

                while(var8.hasNext()) {
                    String value = (String)var8.next();
                    int count = (int)exampleSet.getStatistics(attribute, "count", value);
                    if (count > 0) {
                        counts.put(value, count);
                        if (count < leastCount) {
                            leastCount = count;
                            least = value;
                        }

                        if (count > modeCount) {
                            modeCount = count;
                            mode = value;
                        }
                    }
                }

                this.valueCounts.put(attribute.getName(), counts);
                this.modeValues.put(attribute.getName(), mode);
                this.modeCounts.put(attribute.getName(), modeCount);
                this.leastValues.put(attribute.getName(), least);
                this.leastCounts.put(attribute.getName(), leastCount);
                int totalSizeCount = exampleSet.size();
                int nonMissingSizeCount = totalSizeCount - (int)exampleSet.getStatistics(attribute, "unknown");
                this.totalSize.put(attribute.getName(), totalSizeCount);
                this.nonMissingSize.put(attribute.getName(), nonMissingSizeCount);
            }

        }
    }

    void clear() {
        this.valueCounts.clear();
        this.modeValues.clear();
        this.modeCounts.clear();
        this.leastValues.clear();
        this.leastCounts.clear();
        this.totalSize.clear();
        this.nonMissingSize.clear();
    }
}
