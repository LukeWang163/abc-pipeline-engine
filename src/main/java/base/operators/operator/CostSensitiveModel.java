package base.operators.operator;

import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.ExampleSetUtilities.SetsCompareOption;
import base.operators.example.set.ExampleSetUtilities.TypesCompareOption;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.learner.PredictionModel;
import base.operators.operator.preprocessing.MaterializeDataInMemory;
import base.operators.operator.preprocessing.statistics.NumericalStatisticsHandler;
import base.operators.operator.preprocessing.statistics.PreparationStatistics;
import base.operators.operator.tools.CostMatrix;
import base.operators.tools.Ontology;
import base.operators.tools.RandomGenerator;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CostSensitiveModel extends PredictionModel {
    private static final long serialVersionUID = -7378871544357578954L;
    private static final int MIN_NUMBER_OF_INT_FOR_GAUSSIAN = 6;
    private static final double RANGE_FACTOR = 20.0D;
    public static final double NOMINAL_CHANGE_PROB = 0.2D;
    private PreparationStatistics statistics = new PreparationStatistics();
    private PredictionModel baseModel;
    private CostMatrix costMatrix;
    private int numberOfVariants;

    public CostSensitiveModel(ExampleSet exampleSet, PredictionModel baseModel, CostMatrix costMatrix, int numberOfVariants) {
        super(exampleSet, (SetsCompareOption)null, (TypesCompareOption)null);
        this.statistics.updateStatistics(exampleSet);
        this.baseModel = baseModel;
        this.costMatrix = costMatrix;
        this.numberOfVariants = numberOfVariants;
    }

    @Override
    public ExampleSet performPrediction(ExampleSet originalExampleSet, Attribute predictedLabel) throws OperatorException {
        ExampleSet exampleSet = MaterializeDataInMemory.materializeExampleSet(originalExampleSet);
        exampleSet = this.baseModel.performPrediction(exampleSet, exampleSet.getAttributes().getPredictedLabel());
        double[][] confidences = new double[exampleSet.size()][this.costMatrix.getClasses().size()];
        int counter = 0;

        for(Iterator var6 = exampleSet.iterator(); var6.hasNext(); ++counter) {
            Example example = (Example)var6.next();
            ExampleSet randomData = this.getRandomData(example);
            randomData = this.baseModel.apply(randomData);
            int currentClassNumber = 0;

            for(Iterator var10 = this.costMatrix.getClasses().iterator(); var10.hasNext(); ++currentClassNumber) {
                String currentClass = (String)var10.next();

                double confidence;
                for(Iterator var12 = randomData.iterator(); var12.hasNext(); confidences[counter][currentClassNumber] += confidence) {
                    Example randomExample = (Example)var12.next();
                    confidence = randomExample.getConfidence(currentClass);
                }
            }
        }

        Attribute classificationCost = AttributeFactory.createAttribute("cost", 4);
        originalExampleSet.getExampleTable().addAttribute(classificationCost);
        originalExampleSet.getAttributes().setCost(classificationCost);
        counter = 0;
        RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(true, 1000);

        for(Iterator var20 = originalExampleSet.iterator(); var20.hasNext(); ++counter) {
            Example example = (Example)var20.next();

            for(int i = 0; i < this.costMatrix.getClasses().size(); ++i) {
                confidences[counter][i] /= (double)this.numberOfVariants + 1.0D;
            }

            double[] expectedCosts = new double[this.costMatrix.getClasses().size()];
            int bestIndex = -1;
            double bestValue = -1.0D / 0.0;

            int i;
            for(i = 0; i < this.costMatrix.getClasses().size(); ++i) {
                for(int t = 0; t < this.costMatrix.getClasses().size(); ++t) {
                    String predictedClass = (String)this.costMatrix.getClasses().get(i);
                    String trueClass = (String)this.costMatrix.getClasses().get(t);
                    expectedCosts[i] += confidences[counter][t] * this.costMatrix.getCost(predictedClass, trueClass);
                }

                if (!Double.isNaN(expectedCosts[i]) && expectedCosts[i] > bestValue) {
                    bestValue = expectedCosts[i];
                    bestIndex = i;
                }
            }

            if (bestIndex == -1) {
                bestIndex = randomGenerator.nextIntInRange(0, this.costMatrix.getClasses().size());
            }

            example.setValue(originalExampleSet.getAttributes().getPredictedLabel(), (double)originalExampleSet.getAttributes().getPredictedLabel().getMapping().mapString((String)this.costMatrix.getClasses().get(bestIndex)));
            example.setValue(classificationCost, expectedCosts[bestIndex]);

            for(i = 0; i < this.costMatrix.getClasses().size(); ++i) {
                example.setConfidence((String)this.costMatrix.getClasses().get(i), confidences[counter][i]);
            }
        }

        return originalExampleSet;
    }

    private ExampleSet getRandomData(Example predictionExample) {
        List<Attribute> newAttributes = new LinkedList();
        Iterator a = predictionExample.getAttributes().regularAttributes();

        while(a.hasNext()) {
            AttributeRole role = (AttributeRole)a.next();
            if (!role.isSpecial()) {
                newAttributes.add((Attribute)role.getAttribute().clone());
            }
        }

        ExampleSetBuilder builder = ExampleSets.from(newAttributes);
        double[] row = new double[newAttributes.size()];
        a = predictionExample.getAttributes().regularAttributes();

        double oldValue;
        for(byte index = 0; a.hasNext(); row[index] = oldValue) {
            Attribute attribute = ((AttributeRole)a.next()).getAttribute();
            oldValue = predictionExample.getValue(attribute);
        }

        builder.addRow(row);
        RandomGenerator rand = new RandomGenerator(1992L);

        for(int i = 0; i < this.numberOfVariants; ++i) {
            row = new double[newAttributes.size()];
            int index = 0;

            for(a = predictionExample.getAttributes().regularAttributes(); a.hasNext(); ++index) {
                Attribute attribute = ((AttributeRole)a.next()).getAttribute();
                oldValue = predictionExample.getValue(attribute);
                if (!attribute.isNominal()) {
                    if (attribute.isNumerical()) {
                        if (Double.isNaN(oldValue)) {
                            oldValue = (Double)this.statistics.getNumericalStatisticsHandler().getStatistics(attribute).get(NumericalStatisticsHandler.Type.AVG);
                        }

                        double gaussian;
                        double min;
                        double max;
                        double range;
                        if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 3)) {
                            min = (Double)this.statistics.getNumericalStatisticsHandler().getStatistics(attribute).get(NumericalStatisticsHandler.Type.MIN);
                            max = (Double)this.statistics.getNumericalStatisticsHandler().getStatistics(attribute).get(NumericalStatisticsHandler.Type.MAX);
                            if (max - min < 6.0D) {
                                row[index] = (double)rand.nextIntInRange((int)min, (int)max + 1);
                            } else {
                                range = Math.abs(max - min) / 20.0D;
                                gaussian = rand.nextGaussian();
                                row[index] = (double)Math.round(oldValue + gaussian * range);
                            }
                        } else {
                            min = (Double)this.statistics.getNumericalStatisticsHandler().getStatistics(attribute).get(NumericalStatisticsHandler.Type.MIN);
                            max = (Double)this.statistics.getNumericalStatisticsHandler().getStatistics(attribute).get(NumericalStatisticsHandler.Type.MAX);
                            range = Math.abs(max - min) / 20.0D;
                            gaussian = rand.nextGaussian();
                            row[index] = oldValue + gaussian * range;
                        }
                    } else {
                        row[index] = oldValue;
                    }
                } else if (rand.nextDouble() >= 0.2D) {
                    row[index] = (double)attribute.getMapping().mapString(attribute.getMapping().mapIndex((int)oldValue));
                } else {
                    Map<String, Integer> counts = this.statistics.getNominalValueCountHandler().getValueCounts(attribute);
                    int countSum = 0;

                    int c;
                    for(Iterator var14 = counts.values().iterator(); var14.hasNext(); countSum += c) {
                        c = (Integer)var14.next();
                    }

                    int randomSelector = rand.nextIntInRange(0, countSum);
                    countSum = 0;
                    Iterator var27 = counts.keySet().iterator();

                    while(var27.hasNext()) {
                        String nominalValue = (String)var27.next();
                        int nominalValueCount = (Integer)counts.get(nominalValue);
                        countSum += nominalValueCount;
                        if (countSum > randomSelector) {
                            row[index] = (double)attribute.getMapping().mapString(nominalValue);
                            break;
                        }
                    }
                }
            }

            builder.addRow(row);
        }

        return builder.build();
    }

    public Model getBaseModel() {
        return this.baseModel;
    }

    @Override
    public String getName() {
        return this.baseModel.getName();
    }

    public CostMatrix getCostMatrix() {
        return this.costMatrix;
    }

    @Override
    public String toString() {
        return this.baseModel.toString();
    }
}

