package base.operators.operator.features.optimization;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.features.optimization.AutomaticFeatureEngineering.LearningType;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class OptimizationResultsCollector {
    private double errorMin = 1.0D / 0.0;
    private double errorMax = -1.0D / 0.0;
    private double complexityMin = 1.0D / 0.0;
    private double complexityMax = -1.0D / 0.0;
    private XYSeriesCollection errorDataCollection = new XYSeriesCollection();
    private XYSeriesCollection sizeDataCollection = new XYSeriesCollection();
    private int numberOfGenerations = 0;
    private LearningType learningType;

    public OptimizationResultsCollector(LearningType learningType) {
        this.learningType = learningType;
        XYSeries bestErrorSeriesLines = new XYSeries("Best Error Line");
        this.errorDataCollection.addSeries(bestErrorSeriesLines);
        XYSeries bestErrorSeriesPoints = new XYSeries("Best Error Points");
        this.errorDataCollection.addSeries(bestErrorSeriesPoints);
        XYSeries worstErrorSeriesLines = new XYSeries("Worst Error Line");
        this.errorDataCollection.addSeries(worstErrorSeriesLines);
        XYSeries worstErrorSeriesPoints = new XYSeries("Worst Error Points");
        this.errorDataCollection.addSeries(worstErrorSeriesPoints);
        XYSeries minSizeSeriesLines = new XYSeries("Min Size Line");
        this.sizeDataCollection.addSeries(minSizeSeriesLines);
        XYSeries minSizeSeriesPoints = new XYSeries("Min Size Points");
        this.sizeDataCollection.addSeries(minSizeSeriesPoints);
        XYSeries maxSizeSeriesLines = new XYSeries("Max Size Line");
        this.sizeDataCollection.addSeries(maxSizeSeriesLines);
        XYSeries maxSizeSeriesPoints = new XYSeries("Max Size Points");
        this.sizeDataCollection.addSeries(maxSizeSeriesPoints);
    }

    public void nextGeneration(Population population, int generation) {
        double currentBestError = 1.0D / 0.0;
        double currentWorstError = -1.0D / 0.0;
        double currentMinComplexity = 1.0D / 0.0;
        double currentMaxComplexity = -1.0D / 0.0;
        boolean removeVerticalPart = LearningType.SUPERVISED.equals(this.learningType);
        List<base.operators.operator.features.optimization.Individual> currentParetoFront = population.getParetoFront(removeVerticalPart);
        Iterator var13 = currentParetoFront.iterator();

        while(var13.hasNext()) {
            base.operators.operator.features.optimization.Individual ind = (base.operators.operator.features.optimization.Individual)var13.next();
            if (ind.getComplexity() >= 1) {
                double error = ind.getError();
                if (!Double.isInfinite(error)) {
                    this.errorMin = Math.min(this.errorMin, error);
                    this.errorMax = Math.max(this.errorMax, error);
                    currentBestError = Math.min(currentBestError, error);
                    currentWorstError = Math.max(currentWorstError, error);
                    int complexity = ind.getComplexity();
                    this.complexityMin = Math.min(this.complexityMin, (double)complexity);
                    this.complexityMax = Math.max(this.complexityMax, (double)complexity);
                    currentMinComplexity = Math.min(currentMinComplexity, (double)complexity);
                    currentMaxComplexity = Math.max(currentMaxComplexity, (double)complexity);
                }
            }
        }

        if (!Double.isInfinite(currentBestError)) {
            this.errorDataCollection.getSeries(0).add((double)generation, currentBestError);
            this.errorDataCollection.getSeries(1).add((double)generation, currentBestError);
        }

        if (!Double.isInfinite(currentWorstError)) {
            this.errorDataCollection.getSeries(2).add((double)generation, currentWorstError);
            this.errorDataCollection.getSeries(3).add((double)generation, currentWorstError);
        }

        this.sizeDataCollection.getSeries(0).add((double)generation, currentMinComplexity);
        this.sizeDataCollection.getSeries(1).add((double)generation, currentMinComplexity);
        this.sizeDataCollection.getSeries(2).add((double)generation, currentMaxComplexity);
        this.sizeDataCollection.getSeries(3).add((double)generation, currentMaxComplexity);
        this.numberOfGenerations = Math.max(this.numberOfGenerations, generation);
    }

    public ExampleSet createExampleSet() {
        List<Attribute> allAttributes = new LinkedList();
        Attribute generationAttribute = AttributeFactory.createAttribute("Generation", 3);
        allAttributes.add(generationAttribute);
        Attribute minErrorAttribute = AttributeFactory.createAttribute("Min Error", 4);
        allAttributes.add(minErrorAttribute);
        Attribute maxErrorAttribute = AttributeFactory.createAttribute("Max Error", 4);
        allAttributes.add(maxErrorAttribute);
        Attribute minComplexityAttribute = AttributeFactory.createAttribute("Min Complexity", 3);
        allAttributes.add(minComplexityAttribute);
        Attribute maxComplexityAttribute = AttributeFactory.createAttribute("Max Complexity", 3);
        allAttributes.add(maxComplexityAttribute);
        ExampleSetBuilder builder = ExampleSets.from(allAttributes);

        for(int g = 0; g < this.errorDataCollection.getSeries(0).getItemCount(); ++g) {
            double[] dataRow = new double[allAttributes.size()];
            dataRow[0] = (double)(g + 1);
            XYDataItem minErrorItem = this.errorDataCollection.getSeries(0).getDataItem(g);
            if (minErrorItem != null) {
                dataRow[1] = minErrorItem.getYValue();
            } else {
                dataRow[1] = 0.0D / 0.0;
            }

            XYDataItem maxErrorItem = this.errorDataCollection.getSeries(2).getDataItem(g);
            if (maxErrorItem != null) {
                dataRow[2] = maxErrorItem.getYValue();
            } else {
                dataRow[2] = 0.0D / 0.0;
            }

            XYDataItem minComplexityItem = this.sizeDataCollection.getSeries(0).getDataItem(g);
            if (minComplexityItem != null) {
                dataRow[3] = minComplexityItem.getYValue();
            } else {
                dataRow[3] = 0.0D / 0.0;
            }

            XYDataItem maxComplexityItem = this.sizeDataCollection.getSeries(2).getDataItem(g);
            if (maxComplexityItem != null) {
                dataRow[4] = maxComplexityItem.getYValue();
            } else {
                dataRow[4] = 0.0D / 0.0;
            }

            builder.addRow(dataRow);
        }

        return builder.build();
    }
}
