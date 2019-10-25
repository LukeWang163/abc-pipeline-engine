package base.operators.operator.features.optimization;

import base.operators.example.ExampleSet;
import base.operators.operator.tools.OptimizationListener;
import base.operators.operator.OperatorException;
import base.operators.tools.RandomGenerator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class AutomaticFeatureEngineering {
    private static final int POP_SIZE_FACTOR = 250;
    private static final int MAX_POP_SIZE = 50;
    private static final int MIN_POP_SIZE = 20;
    private static final int MAX_GEN_SIZE_FACTOR = 700;
    private static final int MIN_GENERATIONS = 30;
    private static final int MAX_GENERATIONS = 250;
    private static final int DEFAULT_MAX_MULTI_STARTS = 10;
    private static final int DEFAULT_GENERATIONS_UNTIL_MULTISTART = 7;
    private static final double ERROR_RATE_FOR_IMPROVAL = 0.98D;
    private boolean shouldStop = false;
    private Population population;
    private List<OptimizationListener> listeners = new LinkedList();

    public AutomaticFeatureEngineering() {
    }

    public Population run(ExampleSet exampleSet, ErrorCalculator errorCalculator, boolean onlySelection, List<Function> functions, int maxFunctionComplexity, String whiteListRegExp, int maxGenerations, int populationSize, int maximumMultiStarts, int generationsUntilMultiStart, int timeLimit, AutomaticFeatureEngineering.LearningType learningType, RandomGenerator rng) throws OperatorException {
        this.shouldStop = false;
        int popSize;
        if (populationSize <= 0) {
            popSize = 250 * (int)Math.ceil(Math.log10((double)exampleSet.getAttributes().size()));
            double sizeFactor = Math.max(1.0D, Math.sqrt((double)exampleSet.size()) / 4.0D);
            popSize = (int)((double)popSize / sizeFactor);
            if (popSize > 50) {
                popSize = 50;
            }

            if (popSize < 20) {
                popSize = 20;
            }
        } else {
            popSize = populationSize;
        }

        int maxGen;
        if (maxGenerations <= 0) {
            maxGen = 700 * ((int)Math.ceil(Math.sqrt((double)exampleSet.getAttributes().size())) + 1);
            double sizeFactor = Math.max(1.0D, Math.sqrt((double)exampleSet.size()));
            maxGen = (int)((double)maxGen / sizeFactor);
            if (maxGen > 250) {
                maxGen = 250;
            }

            if (maxGen < 30) {
                maxGen = 30;
            }

            if (onlySelection) {
                maxGen = (int)Math.ceil((double)maxGen / 4.0D);
            }
        } else {
            maxGen = maxGenerations;
        }

        int maxMultiStarts = 10;
        if (maximumMultiStarts >= 0) {
            maxMultiStarts = maximumMultiStarts;
        }

        int genUntilMultiStart = 7;
        if (generationsUntilMultiStart >= 0) {
            genUntilMultiStart = generationsUntilMultiStart;
        }

        Iterator var18 = this.listeners.iterator();

        while(var18.hasNext()) {
            OptimizationListener listener = (OptimizationListener)var18.next();
            listener.optimizationStarted(maxGen);
        }

        boolean minimizeFeatures = AutomaticFeatureEngineering.LearningType.SUPERVISED.equals(learningType);
        this.population = new Population(exampleSet, popSize, onlySelection, functions, maxFunctionComplexity, whiteListRegExp, minimizeFeatures, rng);
        double bestErrorRate = 1.0D / 0.0;
        int generationOfLastImproval = 0;
        int multiStartCounter = 0;
        int lastMultiStartGeneration = 0;
        int generationsWithoutImproval = (int)Math.max((double)genUntilMultiStart * 2.5D, 0.4D * (double)maxGen);
        long startTime = System.currentTimeMillis();

        for(int g = 0; g < maxGen; ++g) {
            Population populationCopy = new Population(this.population);
            if (multiStartCounter < maxMultiStarts && generationOfLastImproval == 0 && g > lastMultiStartGeneration + genUntilMultiStart) {
                lastMultiStartGeneration = g;
                ++multiStartCounter;
                maxGen += genUntilMultiStart;
                Individual bestSoFar = this.population.getBestIndividual();
                this.population.clear();
                this.population.fillPopulation(exampleSet, popSize);
                if (bestSoFar != null) {
                    this.population.add(new Individual(bestSoFar));
                }
            }

            List<Individual> newChildren = new LinkedList();

            for(int c = 0; c < this.population.getNumberOfIndividuals() / 2; ++c) {
                newChildren.add(new Individual(this.population.crossover(rng.nextInt(this.population.getNumberOfIndividuals()), rng.nextInt(this.population.getNumberOfIndividuals()))));
            }

            this.population.addAll(newChildren);
            List<Individual> mutations = new LinkedList();
            int newPopSize = this.population.getNumberOfIndividuals();

            for(int m = 0; m < newPopSize; ++m) {
                mutations.add(new Individual(this.population.mutate(m)));
            }

            this.population.addAll(mutations);
            Iterator individualIterator = this.population.iterator();

            Individual best;
            while(individualIterator.hasNext()) {
                best = (Individual)individualIterator.next();
                best.getFeatureSet().removeDuplicates();
                if (best.getFeatureSet().getNumberOfFeatures() == 0) {
                    individualIterator.remove();
                }
            }

            if (this.population.getNumberOfIndividuals() == 0) {
                this.population = populationCopy;
            } else {
                this.evaluate(this.population, errorCalculator);
                this.population.performSelection();
            }

            best = this.population.getBestIndividual();
            if (best.getError() < 0.98D * bestErrorRate) {
                bestErrorRate = best.getError();
                generationOfLastImproval = g;
            }

            this.population.setGeneration(g);
            Iterator var34 = this.listeners.iterator();

            while(var34.hasNext()) {
                OptimizationListener listener = (OptimizationListener)var34.next();
                listener.nextGeneration(g);
            }

            if (this.shouldStop) {
                break;
            }

            if (timeLimit > 0) {
                long currentTime = System.currentTimeMillis();
                if ((double)(currentTime - startTime) / 1000.0D > (double)timeLimit) {
                    break;
                }
            }

            if (g - lastMultiStartGeneration - generationOfLastImproval > generationsWithoutImproval) {
                break;
            }
        }

        this.fireOptimizationFinished();
        return this.population;
    }

    public void addListener(OptimizationListener listener) {
        this.listeners.add(listener);
    }

    public Population getCurrentPopulation() {
        return this.population;
    }

    public void shouldStop() {
        this.shouldStop = true;
    }

    private void evaluate(Population population, ErrorCalculator errorCalculator) throws OperatorException {
        Iterator var3 = population.iterator();

        while(var3.hasNext()) {
            Individual individual = (Individual)var3.next();
            errorCalculator.calculateError(individual);
        }

    }

    private void fireOptimizationFinished() {
        Iterator var1 = this.listeners.iterator();

        while(var1.hasNext()) {
            OptimizationListener listener = (OptimizationListener)var1.next();
            listener.optimizationFinished();
        }

    }

    public static enum LearningType {
        SUPERVISED,
        UNSUPERVISED;

        private LearningType() {
        }
    }
}