package base.operators.operator.preprocessing.statistics;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.tools.ChartTools;
import base.operators.tools.Tools;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class QualityMeasurementsHandler implements Serializable {
    private static final long serialVersionUID = -2654580040203461548L;
    private static final double AVG_LENGTH_TO_BE_TEXT = 75.0D;
//    public static final Color CORRELATION_COLOR;
//    public static final Color MISSING_COLOR;
//    public static final Color ID_COLOR;
//    public static final Color STABILITY_COLOR;
    private Map<String, Double> correlations = new HashMap();
    private Map<String, Double> idNess = new HashMap();
    private Map<String, Double> missingNess = new HashMap();
    private Map<String, Double> infiniteNess = new HashMap();
    private Map<String, Double> stability = new HashMap();
    private Map<String, Double> textNess = new HashMap();
    private boolean[] samplingSelection = null;
    private boolean calculateExtendedMeasures = false;

    QualityMeasurementsHandler(boolean calculateExtendedMeasures) {
        this.calculateExtendedMeasures = calculateExtendedMeasures;
    }

    QualityMeasurementsHandler(QualityMeasurementsHandler qualityMeasurementsHandler) {
        Iterator var2 = qualityMeasurementsHandler.correlations.entrySet().iterator();

        Entry entry;
        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.correlations.put((String)entry.getKey(), (Double)entry.getValue());
        }

        var2 = qualityMeasurementsHandler.idNess.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.idNess.put((String)entry.getKey(), (Double)entry.getValue());
        }

        var2 = qualityMeasurementsHandler.missingNess.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.missingNess.put((String)entry.getKey(), (Double)entry.getValue());
        }

        var2 = qualityMeasurementsHandler.infiniteNess.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.infiniteNess.put((String)entry.getKey(), (Double)entry.getValue());
        }

        var2 = qualityMeasurementsHandler.stability.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.stability.put((String)entry.getKey(), (Double)entry.getValue());
        }

        var2 = qualityMeasurementsHandler.textNess.entrySet().iterator();

        while(var2.hasNext()) {
            entry = (Entry)var2.next();
            this.textNess.put((String)entry.getKey(), (Double)entry.getValue());
        }

        this.samplingSelection = Arrays.copyOf(qualityMeasurementsHandler.samplingSelection, qualityMeasurementsHandler.samplingSelection.length);
        this.calculateExtendedMeasures = qualityMeasurementsHandler.calculateExtendedMeasures;
    }

    void addQualityMeasures(ExampleSet exampleSet, Attribute attribute) {
        if (exampleSet != null && attribute != null) {
            if (this.samplingSelection == null) {
                this.samplingSelection = PreparationStatistics.createSamplingSelection(exampleSet);
            }

            double correlationValue = 0.0D / 0.0;
            if (this.calculateExtendedMeasures) {
                Attribute label = exampleSet.getAttributes().getLabel();
                correlationValue = this.getCorrelation(attribute, exampleSet, label, this.samplingSelection);
            }

            this.correlations.put(attribute.getName(), correlationValue);
            double idNessValue = this.getIDNess(attribute, exampleSet, this.samplingSelection);
            this.idNess.put(attribute.getName(), idNessValue);
            double stabilityValue = this.getStability(attribute, exampleSet, this.samplingSelection);
            this.stability.put(attribute.getName(), stabilityValue);
            double missingNessValue = this.getMissingNess(attribute, exampleSet);
            this.missingNess.put(attribute.getName(), missingNessValue);
            double infiniteNessValue = this.getInfiniteNess(attribute, exampleSet, this.samplingSelection);
            this.infiniteNess.put(attribute.getName(), infiniteNessValue);
            double textNessValue = 0.0D / 0.0;
            if (this.calculateExtendedMeasures) {
                textNessValue = this.getTextNess(attribute, exampleSet, this.samplingSelection);
            }

            this.textNess.put(attribute.getName(), textNessValue);
        }
    }

    public double getCorrelation(Attribute attribute) {
        return attribute == null ? this.getCorrelation((String)null) : this.getCorrelation(attribute.getName());
    }

    public double getCorrelation(String attributeName) {
        return attributeName == null ? 0.0D / 0.0 : (Double)this.correlations.get(attributeName);
    }

    public double getIDNess(Attribute attribute) {
        return attribute == null ? this.getIDNess((String)null) : this.getIDNess(attribute.getName());
    }

    public double getIDNess(String attributeName) {
        return attributeName == null ? 0.0D / 0.0 : (Double)this.idNess.get(attributeName);
    }

    public double getMissing(Attribute attribute) {
        return attribute == null ? this.getMissing((String)null) : this.getMissing(attribute.getName());
    }

    public double getMissing(String attributeName) {
        return attributeName == null ? 0.0D / 0.0 : (Double)this.missingNess.get(attributeName);
    }

    public double getInfinite(Attribute attribute) {
        return attribute == null ? this.getInfinite((String)null) : this.getInfinite(attribute.getName());
    }

    public double getInfinite(String attributeName) {
        return attributeName == null ? 0.0D / 0.0 : (Double)this.infiniteNess.get(attributeName);
    }

    public double getStability(Attribute attribute) {
        return attribute == null ? this.getStability((String)null) : this.getStability(attribute.getName());
    }

    public double getStability(String attributeName) {
        return attributeName == null ? 0.0D / 0.0 : (Double)this.stability.get(attributeName);
    }

    public double getTextNess(Attribute attribute) {
        return attribute == null ? this.getTextNess((String)null) : this.getTextNess(attribute.getName());
    }

    public double getTextNess(String attributeName) {
        return attributeName == null ? 0.0D / 0.0 : (Double)this.textNess.get(attributeName);
    }

    void clear() {
        this.correlations.clear();
        this.idNess.clear();
        this.missingNess.clear();
        this.infiniteNess.clear();
        this.stability.clear();
        this.textNess.clear();
        this.samplingSelection = null;
    }

    public String generateTooltipHTML(Attribute attribute) {
        return "";
//        if (attribute == null) {
//            return "";
//        } else {
//            StringBuilder result = new StringBuilder();
//            boolean first = true;
//            double valid = 1.0D;
//            StringBuilder backgroundInfo = new StringBuilder();
//            backgroundInfo.append("<br /><br /><hr /><b color=\"").append(ChartTools.toHex(ChartTools.NEUTRAL2_COLOR)).append("\">Info on Quality Measures</b>");
//            double correlationValue = this.getCorrelation(attribute);
//            if (!Double.isNaN(correlationValue)) {
//                result.append("<br />");
//                first = false;
//                result.append("<br /><span color=\"").append(ChartTools.toHex(CORRELATION_COLOR)).append("\">Correlation: ");
//                result.append(Tools.formatPercent(correlationValue));
//                result.append("</span>");
//                backgroundInfo.append("<br /><span color=\"").append(ChartTools.toHex(ChartTools.NEUTRAL2_COLOR)).append("\">");
//                backgroundInfo.append("Correlation: The correlation of this column with the target column in the data.");
//                backgroundInfo.append("</span>");
//            }
//
//            double missingValue = this.getMissing(attribute);
//            if (!Double.isNaN(missingValue) && missingValue > 0.0D) {
//                if (first) {
//                    result.append("<br />");
//                    first = false;
//                }
//
//                valid -= missingValue;
//                result.append("<br /><span color=\"").append(ChartTools.toHex(MISSING_COLOR)).append("\">Missing: ");
//                result.append(Tools.formatPercent(missingValue));
//                result.append("</span>");
//                backgroundInfo.append("<br /><span color=\"").append(ChartTools.toHex(ChartTools.NEUTRAL2_COLOR)).append("\">");
//                backgroundInfo.append("Missing: The number of missing values in this column divided by the number of rows.");
//                backgroundInfo.append("</span>");
//            }
//
//            double infiniteValue = this.getInfinite(attribute);
//            if (!Double.isNaN(infiniteValue) && infiniteValue > 0.0D) {
//                if (first) {
//                    result.append("<br />");
//                    first = false;
//                }
//
//                valid -= infiniteValue;
//                result.append("<br /><span color=\"").append(ChartTools.toHex(MISSING_COLOR)).append("\">Infinite: ");
//                result.append(Tools.formatPercent(infiniteValue));
//                result.append("</span>");
//                backgroundInfo.append("<br /><span color=\"").append(ChartTools.toHex(ChartTools.NEUTRAL2_COLOR)).append("\">");
//                backgroundInfo.append("Infinite: The number of infinite values in this column divided by the number of rows.");
//                backgroundInfo.append("</span>");
//            }
//
//            double idValue = this.getIDNess(attribute);
//            if (!Double.isNaN(idValue) && idValue > 0.0D) {
//                if (first) {
//                    result.append("<br />");
//                    first = false;
//                }
//
//                valid -= idValue;
//                result.append("<br /><span color=\"").append(ChartTools.toHex(ID_COLOR)).append("\">ID-ness: ");
//                result.append(Tools.formatPercent(idValue));
//                result.append("</span>");
//                backgroundInfo.append("<br /><span color=\"").append(ChartTools.toHex(ChartTools.NEUTRAL2_COLOR)).append("\">");
//                backgroundInfo.append("ID-ness: The number of different integer values for this column divided by the number of rows.");
//                backgroundInfo.append("</span>");
//            }
//
//            double stabilityValue = this.getStability(attribute);
//            if (!Double.isNaN(stabilityValue) && stabilityValue > 0.0D) {
//                if (first) {
//                    result.append("<br />");
//                    first = false;
//                }
//
//                valid -= stabilityValue;
//                result.append("<br /><span color=\"").append(ChartTools.toHex(STABILITY_COLOR)).append("\">Stability: ");
//                result.append(Tools.formatPercent(stabilityValue));
//                result.append("</span>");
//                backgroundInfo.append("<br /><span color=\"").append(ChartTools.toHex(ChartTools.NEUTRAL2_COLOR)).append("\">");
//                backgroundInfo.append("Stability: The count for the most frequent non-missing value for this column divided by the number of rows.");
//                backgroundInfo.append("</span>");
//            }
//
//            double textNessValue = this.getTextNess(attribute);
//            if (!Double.isNaN(textNessValue) && textNessValue > 0.0D) {
//                if (first) {
//                    result.append("<br />");
//                    first = false;
//                }
//
//                result.append("<br /><span color=\"").append(ChartTools.toHex(ChartTools.NEUTRAL3_COLOR)).append("\">Text-ness: ");
//                result.append(Tools.formatPercent(textNessValue));
//                result.append("</span>");
//                backgroundInfo.append("<br /><span color=\"").append(ChartTools.toHex(ChartTools.NEUTRAL2_COLOR)).append("\">");
//                backgroundInfo.append("Text-ness: A percentage describing how much this column looks like a text column.");
//                backgroundInfo.append("</span>");
//            }
//
//            if (valid > 0.0D) {
//                if (first) {
//                    result.append("<br />");
//                }
//
//                result.append("<br /><span color=\"").append(ChartTools.toHex(ChartTools.POSITIVE_COLOR)).append("\">Valid: ");
//                result.append(Tools.formatPercent(valid));
//                result.append("</span>");
//                backgroundInfo.append("<br /><span color=\"").append(ChartTools.toHex(ChartTools.NEUTRAL2_COLOR)).append("\">");
//                backgroundInfo.append("Valid: The fraction of values of this column which are not counted as missing, infinite, id, or stable.");
//                backgroundInfo.append("</span>");
//            }
//
//            return result.toString() + backgroundInfo.toString();
//        }
    }

    private double getCorrelation(Attribute attribute, ExampleSet exampleSet, Attribute label, boolean[] sampleSelection) {
        if (label == null) {
            return 0.0D / 0.0;
        } else {
            Map<String, Double> calculatedCorrelations = new HashMap();
            if (!label.isNominal()) {
                return this.getCorrelationForSingleClass(attribute, exampleSet, label, sampleSelection, (String)null);
            } else {
                Iterator var6 = label.getMapping().getValues().iterator();

                while(var6.hasNext()) {
                    String labelValue = (String)var6.next();
                    calculatedCorrelations.putIfAbsent(labelValue, this.getCorrelationForSingleClass(attribute, exampleSet, label, sampleSelection, labelValue));
                }

                double finalCorrelation = 0.0D;

                Entry singleCorrelation;
                for(Iterator var8 = calculatedCorrelations.entrySet().iterator(); var8.hasNext(); finalCorrelation = Math.max(finalCorrelation, (Double)singleCorrelation.getValue())) {
                    singleCorrelation = (Entry)var8.next();
                }

                return finalCorrelation;
            }
        }
    }

    private double getCorrelationForSingleClass(Attribute attribute, ExampleSet exampleSet, Attribute label, boolean[] sampleSelection, String currentClass) {
        double sumProd = 0.0D;
        double sumFirst = 0.0D;
        double sumSecond = 0.0D;
        double sumFirstSquared = 0.0D;
        double sumSecondSquared = 0.0D;
        int counter = 0;
        int exampleCounter = 0;
        Iterator var18 = exampleSet.iterator();

        double first;
        while(var18.hasNext()) {
            Example example = (Example)var18.next();
            if (sampleSelection[exampleCounter++]) {
                first = example.getValue(attribute);
                if (!attribute.isNumerical() && Double.isNaN(first)) {
                    first = -1.0D;
                }

                double second;
                if (label.isNominal()) {
                    if (Double.isNaN(example.getValue(label))) {
                        second = 0.0D / 0.0;
                    } else {
                        String labelValue = example.getValueAsString(label);
                        second = labelValue.equals(currentClass) ? 1.0D : 0.0D;
                    }
                } else {
                    second = example.getValue(label);
                }

                double prod = first * second;
                if (!Double.isNaN(prod)) {
                    sumProd += prod;
                    sumFirst += first;
                    sumFirstSquared += first * first;
                    sumSecond += second;
                    sumSecondSquared += second * second;
                    ++counter;
                }
            }
        }

        double divisor = Math.sqrt(((double)counter * sumFirstSquared - sumFirst * sumFirst) * ((double)counter * sumSecondSquared - sumSecond * sumSecond));
        if (Tools.isEqual(divisor, 0.0D)) {
            first = 0.0D / 0.0;
        } else {
            first = ((double)counter * sumProd - sumFirst * sumSecond) / divisor;
        }

        return first * first;
    }

//    private DiscretizationModel getDiscretizationModel(ModelWizardProcessModel processModel) {
//        if (processModel.isDiscretizeLabel()) {
//            try {
//                return DiscretizationTools.getDiscretizationModel(processModel.getExampleSet(), processModel.getLabelName(), processModel.getDiscretizationType(), processModel.getNumberOfDiscretizedClasses());
//            } catch (OperatorException var3) {
//                LogService.getRoot().log(Level.SEVERE, "Error during discretization.", var3);
//                return null;
//            }
//        } else {
//            return null;
//        }
//    }

    private double getIDNess(Attribute attribute, ExampleSet exampleSet, boolean[] sampleSelection) {
        HashSet values;
        int exampleCounter;
        if (attribute.isNominal()) {
            values = new HashSet();
            exampleCounter = 0;
            Iterator var12 = exampleSet.iterator();

            while(var12.hasNext()) {
                Example example = (Example)var12.next();
                if (sampleSelection[exampleCounter++]) {
                    double doubleValue = example.getValue(attribute);
                    if (!Double.isNaN(doubleValue)) {
                        String value = example.getValueAsString(attribute);
                        values.add(value);
                    }
                }
            }

            return (double)values.size() / (double)sampleSelection.length;
        } else if (attribute.isNumerical()) {
            values = new HashSet();
            exampleCounter = 0;
            int intCounter = 0;
            Iterator var7 = exampleSet.iterator();

            while(var7.hasNext()) {
                Example example = (Example)var7.next();
                if (sampleSelection[exampleCounter++]) {
                    double doubleValue = example.getValue(attribute);
                    if (!Double.isNaN(doubleValue)) {
                        int roundedValue = (int)Math.round(doubleValue);
                        if (Math.abs(doubleValue - (double)roundedValue) < 1.0E-5D) {
                            values.add(roundedValue);
                            ++intCounter;
                        }
                    }
                }
            }

            if (intCounter == 0) {
                return 0.0D / 0.0;
            } else {
                return (double)values.size() / (double)sampleSelection.length;
            }
        } else {
            return 0.0D / 0.0;
        }
    }

    private double getMissingNess(Attribute attribute, ExampleSet exampleSet) {
        return exampleSet.getStatistics(attribute, "unknown") / (double)exampleSet.size();
    }

    private double getInfiniteNess(Attribute attribute, ExampleSet exampleSet, boolean[] sampleSelection) {
        if (!attribute.isNumerical()) {
            return 0.0D / 0.0;
        } else {
            int infiniteCounter = 0;
            int exampleCounter = 0;
            Iterator var6 = exampleSet.iterator();

            while(var6.hasNext()) {
                Example example = (Example)var6.next();
                if (sampleSelection[exampleCounter++]) {
                    double doubleValue = example.getValue(attribute);
                    if (Double.isInfinite(doubleValue)) {
                        ++infiniteCounter;
                    }
                }
            }

            return (double)infiniteCounter / (double)sampleSelection.length;
        }
    }

    private double getStability(Attribute attribute, ExampleSet exampleSet, boolean[] sampleSelection) {
        Map<String, Integer> valueCounts = new HashMap();
        int counter = 0;
        int exampleCounter = 0;
        Iterator var7 = exampleSet.iterator();

        while(var7.hasNext()) {
            Example e = (Example)var7.next();
            if (sampleSelection[exampleCounter++]) {
                double doubleValue = e.getValue(attribute);
                if (!Double.isNaN(doubleValue)) {
                    String value = e.getValueAsString(attribute);
                    Integer currentCount = (Integer)valueCounts.get(value);
                    if (currentCount == null) {
                        valueCounts.put(value, 1);
                    } else {
                        valueCounts.put(value, currentCount + 1);
                    }

                    ++counter;
                }
            }
        }

        int maxCount = 1;
        Iterator var14 = valueCounts.values().iterator();

        while(var14.hasNext()) {
            Integer count = (Integer)var14.next();
            if (count > maxCount) {
                maxCount = count;
            }
        }

        if (counter == 0) {
            return 0.0D / 0.0;
        } else {
            return (double)maxCount / (double)counter;
        }
    }

    private double getTextNess(Attribute attribute, ExampleSet exampleSet, boolean[] sampleSelection) {
        if (!attribute.isNumerical() && !attribute.isDateTime()) {
            Set<String> values = new HashSet();
            int cellsWithTokenLimiterCount = 0;
            int lengthSum = 0;
            int counter = 0;
            int exampleCounter = 0;
            Iterator var9 = exampleSet.iterator();

            double cellsWithLimitersFraction;
            while(var9.hasNext()) {
                Example e = (Example)var9.next();
                if (sampleSelection[exampleCounter++]) {
                    cellsWithLimitersFraction = e.getValue(attribute);
                    if (!Double.isNaN(cellsWithLimitersFraction)) {
                        String stringValue = e.getValueAsString(attribute);
                        values.add(stringValue);
                        lengthSum += stringValue.length();
                        String[] tokens = stringValue.split("\\s+");
                        if (tokens.length > 1) {
                            ++cellsWithTokenLimiterCount;
                        }

                        ++counter;
                    }
                }
            }

            if (counter == 0) {
                return 0.0D / 0.0;
            } else {
                double idNess = (double)values.size() / (double)counter;
                cellsWithLimitersFraction = (double)cellsWithTokenLimiterCount / (double)counter;
                double avgLength = (double)lengthSum / (double)counter;
                double avgLengthScore = 1.0D;
                if (avgLength < 75.0D) {
                    avgLengthScore = avgLength / 75.0D;
                }

                return (idNess + cellsWithLimitersFraction + avgLengthScore) / 3.0D;
            }
        } else {
            return 0.0D;
        }
    }

/*    static {
        CORRELATION_COLOR = ChartTools.HIGHLIGHT_COLOR;
        MISSING_COLOR = ChartTools.NEGATIVE_COLOR;
        ID_COLOR = ChartTools.NORMAL_COLOR;
        STABILITY_COLOR = ChartTools.NEUTRAL2_COLOR;
    }*/
}
