package base.operators.operator.preprocessing.statistics;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.tools.ChartTools;
import base.operators.tools.container.ValueAndCount;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.HistogramDataset;

public class DistributionHandler implements Serializable {
    private static final long serialVersionUID = -5395696079553005312L;
    private static final int MAX_NOMINAL_COUNT = 8;
    private Map<String, HistogramDataset> numericalDistributions = new HashMap();
    private Map<String, CategoryDataset> nominalDistributions = new HashMap();
    private boolean[] samplingSelection = null;

    DistributionHandler() {
    }

    DistributionHandler(DistributionHandler distributionHandler) {
        Iterator var2 = distributionHandler.numericalDistributions.entrySet().iterator();

        Entry entry;
        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.numericalDistributions.put((String)entry.getKey(), (HistogramDataset)entry.getValue());
        }

        var2 = distributionHandler.nominalDistributions.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.nominalDistributions.put((String)entry.getKey(), (CategoryDataset)entry.getValue());
        }

        this.samplingSelection = Arrays.copyOf(distributionHandler.samplingSelection, distributionHandler.samplingSelection.length);
    }

    void addDistribution(ExampleSet exampleSet, Attribute attribute) {
        if (exampleSet != null && attribute != null) {
            if (this.samplingSelection == null) {
                this.samplingSelection = PreparationStatistics.createSamplingSelection(exampleSet);
            }

            if (!attribute.isNumerical() && !attribute.isDateTime()) {
                if (attribute.isNominal()) {
                    List<ValueAndCount> nominalValues = new LinkedList();
                    Iterator var4 = attribute.getMapping().getValues().iterator();

                    while(var4.hasNext()) {
                        String value = (String)var4.next();
                        nominalValues.add(new ValueAndCount(value, (int)exampleSet.getStatistics(attribute, "count", value)));
                    }

                    Collections.sort(nominalValues);
                    CategoryDataset distribution = ChartTools.createBarDataset(nominalValues, 8);
                    this.nominalDistributions.put(attribute.getName(), distribution);
                }
            } else {
                HistogramDataset distribution = ChartTools.createHistogramDataset(exampleSet, attribute, this.samplingSelection);
                this.numericalDistributions.put(attribute.getName(), distribution);
            }

        }
    }

    public HistogramDataset getNumericalDistribution(Attribute attribute) {
        return this.getNumericalDistribution(attribute.getName());
    }

    public HistogramDataset getNumericalDistribution(String attributeName) {
        return (HistogramDataset)this.numericalDistributions.get(attributeName);
    }

    public CategoryDataset getNominalDistribution(Attribute attribute) {
        return this.getNominalDistribution(attribute.getName());
    }

    public CategoryDataset getNominalDistribution(String attributeName) {
        return (CategoryDataset)this.nominalDistributions.get(attributeName);
    }

    void clear() {
        this.numericalDistributions.clear();
        this.nominalDistributions.clear();
        this.samplingSelection = null;
    }
}
