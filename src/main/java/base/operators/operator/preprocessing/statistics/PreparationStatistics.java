package base.operators.operator.preprocessing.statistics;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.tools.RandomGenerator;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PreparationStatistics implements Serializable {
    private static final long serialVersionUID = 3294764898403600798L;
    private static final int ATTRIBUTE_THRESHOLD_FOR_SMALLER_SAMPLE = 25;
    private static final int SAMPLE_SIZE_SMALL = 5000;
    private static final int SAMPLE_SIZE_LARGE = 10000;
    private Map<String, Integer> knownAttributes;
    private Map<String, String> knownRoles;
    private NumericalStatisticsHandler numericalStatisticsHandler;
    private NominalValueCountHandler nominalValueCountHandler;
    private DateStatisticsHandler dateStatisticsHandler;
    private MissingHandler missingHandler;
    private DistributionHandler distributionHandler;
    private QualityMeasurementsHandler qualityMeasurementsHandler;

    public PreparationStatistics() {
        this(false);
    }

    public PreparationStatistics(boolean extendedQualityCalculations) {
        this.knownAttributes = new HashMap();
        this.knownRoles = new HashMap();
        this.numericalStatisticsHandler = new NumericalStatisticsHandler();
        this.nominalValueCountHandler = new NominalValueCountHandler();
        this.dateStatisticsHandler = new DateStatisticsHandler();
        this.missingHandler = new MissingHandler();
        this.distributionHandler = new DistributionHandler();
        this.qualityMeasurementsHandler = new QualityMeasurementsHandler(extendedQualityCalculations);
    }

    public PreparationStatistics(PreparationStatistics other) {
        this.knownAttributes = new HashMap();
        this.knownRoles = new HashMap();
        this.numericalStatisticsHandler = new NumericalStatisticsHandler();
        this.nominalValueCountHandler = new NominalValueCountHandler();
        this.dateStatisticsHandler = new DateStatisticsHandler();
        this.missingHandler = new MissingHandler();
        this.distributionHandler = new DistributionHandler();
        this.knownAttributes = new HashMap();
        this.knownAttributes.putAll(other.knownAttributes);
        this.knownRoles = new HashMap();
        this.knownRoles.putAll(other.knownRoles);
        this.numericalStatisticsHandler = new NumericalStatisticsHandler(other.numericalStatisticsHandler);
        this.nominalValueCountHandler = new NominalValueCountHandler(other.nominalValueCountHandler);
        this.dateStatisticsHandler = new DateStatisticsHandler(other.dateStatisticsHandler);
        this.missingHandler = new MissingHandler(other.missingHandler);
        this.distributionHandler = new DistributionHandler(other.distributionHandler);
        this.qualityMeasurementsHandler = new QualityMeasurementsHandler(other.qualityMeasurementsHandler);
    }

    public void updateStatistics(ExampleSet exampleSet) {
        this.clear();
        exampleSet.recalculateAllAttributeStatistics();
        Iterator a = exampleSet.getAttributes().allAttributes();

        while(a.hasNext()) {
            this.updateStatistics(exampleSet, (Attribute)a.next(), false);
        }

    }

    public void updateStatistics(ExampleSet exampleSet, Attribute attribute) {
        this.updateStatistics(exampleSet, attribute, true);
    }

    public void updateStatistics(ExampleSet exampleSet, Attribute attribute, boolean recalculateExampleStatistics) {
        if (recalculateExampleStatistics) {
            exampleSet.recalculateAttributeStatistics(attribute);
        }

        if (exampleSet.getAttributes().getRole(attribute).isSpecial()) {
            this.knownRoles.put(attribute.getName(), exampleSet.getAttributes().getRole(attribute).getSpecialName());
        }

        this.knownAttributes.put(attribute.getName(), attribute.getValueType());
        if (attribute.isNominal()) {
            this.nominalValueCountHandler.addValueCounts(exampleSet, attribute);
        } else if (attribute.isNumerical()) {
            this.numericalStatisticsHandler.addStatistics(exampleSet, attribute);
        } else if (attribute.isDateTime()) {
            this.dateStatisticsHandler.addStatistics(exampleSet, attribute);
        }

        this.missingHandler.addMissing(exampleSet, attribute);
        this.distributionHandler.addDistribution(exampleSet, attribute);
        this.qualityMeasurementsHandler.addQualityMeasures(exampleSet, attribute);
    }

    public Map<String, Integer> getKnownAttributes() {
        return Collections.unmodifiableMap(this.knownAttributes);
    }

    public Map<String, String> getKnownRoles() {
        return Collections.unmodifiableMap(this.knownRoles);
    }

    public NumericalStatisticsHandler getNumericalStatisticsHandler() {
        return this.numericalStatisticsHandler;
    }

    public NominalValueCountHandler getNominalValueCountHandler() {
        return this.nominalValueCountHandler;
    }

    public MissingHandler getMissingHandler() {
        return this.missingHandler;
    }

    public DistributionHandler getDistributionHandler() {
        return this.distributionHandler;
    }

    public QualityMeasurementsHandler getQualityMeasurementsHandler() {
        return this.qualityMeasurementsHandler;
    }

    public DateStatisticsHandler getDateStatisticsHandler() {
        return this.dateStatisticsHandler;
    }

    public void clear() {
        this.knownAttributes.clear();
        this.knownRoles.clear();
        this.numericalStatisticsHandler.clear();
        this.nominalValueCountHandler.clear();
        this.dateStatisticsHandler.clear();
        this.missingHandler.clear();
        this.distributionHandler.clear();
        this.qualityMeasurementsHandler.clear();
    }

    static boolean[] createSamplingSelection(ExampleSet exampleSet) {
        RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(true, 1992);
        boolean[] samplingSelection = new boolean[exampleSet.size()];

        for(int counter = 0; counter < exampleSet.size(); ++counter) {
            if (exampleSet.getAttributes().size() > 25) {
                samplingSelection[counter] = exampleSet.size() <= 5000 || randomGenerator.nextDouble() < 5000.0D / (double)exampleSet.size();
            } else {
                samplingSelection[counter] = exampleSet.size() <= 10000 || randomGenerator.nextDouble() < 10000.0D / (double)exampleSet.size();
            }
        }

        return samplingSelection;
    }
}
