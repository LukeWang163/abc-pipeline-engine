package base.operators.operator.features.optimization;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.features.Feature;
import base.operators.operator.features.FeatureSet;
import base.operators.operator.features.optimization.NonDominatedSortingSelection.ErrorComparator;
import base.operators.tools.RandomGenerator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Population implements Iterable<base.operators.operator.features.optimization.Individual> {
    private static final double MIN_IMPROVEMENT_FOR_FINAL_RESULT = 0.05D;
    private static final double PROB_GENERATION = 0.1D;
    private int genSymCounter = 1;
    private List<base.operators.operator.features.optimization.Function> functions;
    private int maxFunctionComplexity;
    private RandomGenerator rng;
    private List<base.operators.operator.features.optimization.Individual> population;
    private List<Feature> originalFeatures;
    private List<Feature> originalFeaturesForGeneration;
    private base.operators.operator.features.optimization.NonDominatedSortingSelection selection;
    private boolean onlySelection;
    private int generation;
    private base.operators.operator.features.optimization.Individual originalFeatureSetIndividual;

    Population(ExampleSet exampleSet, int initialSize, boolean onlySelection, List<base.operators.operator.features.optimization.Function> functions, int maxFunctionComplexity, String whiteListRegExp, boolean minimizeFeatures, RandomGenerator rng) {
        this.generation = 0;
        this.rng = rng;
        this.onlySelection = onlySelection;
        this.functions = functions;
        this.maxFunctionComplexity = maxFunctionComplexity;
        this.population = new ArrayList(initialSize * 3);
        this.originalFeatures = new ArrayList(exampleSet.getAttributes().size());
        this.originalFeaturesForGeneration = new ArrayList(exampleSet.getAttributes().size());
        Iterator var9 = exampleSet.getAttributes().iterator();

        while(true) {
            Attribute attribute;
            do {
                do {
                    if (!var9.hasNext()) {
                        this.selection = new base.operators.operator.features.optimization.NonDominatedSortingSelection(initialSize, minimizeFeatures);
                        this.fillPopulation(exampleSet, initialSize);
                        return;
                    }

                    attribute = (Attribute)var9.next();
                    this.originalFeatures.add(new Feature(attribute));
                } while(!attribute.isNumerical());
            } while(whiteListRegExp != null && whiteListRegExp.trim().length() != 0 && !attribute.getName().matches(whiteListRegExp));

            this.originalFeaturesForGeneration.add(new Feature(attribute));
        }
    }

    Population(Population other) {
        this.generation = other.generation;
        this.genSymCounter = other.genSymCounter;
        this.functions = other.functions;
        this.maxFunctionComplexity = other.maxFunctionComplexity;
        this.rng = other.rng;
        this.originalFeatures = other.originalFeatures;
        this.originalFeaturesForGeneration = other.originalFeaturesForGeneration;
        this.selection = other.selection;
        this.onlySelection = other.onlySelection;
        this.population = new ArrayList(other.population.size());
        Iterator var2 = other.population.iterator();

        while(var2.hasNext()) {
            base.operators.operator.features.optimization.Individual o = (base.operators.operator.features.optimization.Individual)var2.next();
            this.population.add(new base.operators.operator.features.optimization.Individual(o));
        }

        this.originalFeatureSetIndividual = new base.operators.operator.features.optimization.Individual(other.originalFeatureSetIndividual);
    }

    void fillPopulation(ExampleSet exampleSet, int initialSize) {
        this.originalFeatureSetIndividual = new base.operators.operator.features.optimization.Individual(new FeatureSet(exampleSet));
        this.originalFeatureSetIndividual.setOriginal(true);
        this.population.add(this.originalFeatureSetIndividual);

        while(this.population.size() < initialSize) {
            FeatureSet featureSet = new FeatureSet(exampleSet);

            for(int a = 0; a < featureSet.getNumberOfFeatures(); ++a) {
                if (this.rng.nextDouble() < 0.5D) {
                    featureSet.remove(a);
                }
            }

            if (featureSet.getNumberOfFeatures() > 0) {
                this.population.add(new base.operators.operator.features.optimization.Individual(featureSet));
            }
        }

    }

    public base.operators.operator.features.optimization.Individual get(int index) {
        return (base.operators.operator.features.optimization.Individual)this.population.get(index);
    }

    public void add(base.operators.operator.features.optimization.Individual individual) {
        if (!this.population.contains(individual)) {
            this.population.add(individual);
        }

    }

    public void remove(base.operators.operator.features.optimization.Individual individual) {
        this.population.remove(individual);
    }

    public void clear() {
        this.population.clear();
    }

    int getNumberOfIndividuals() {
        return this.population.size();
    }

    void addAll(List<base.operators.operator.features.optimization.Individual> newIndividuals) {
        Iterator var2 = newIndividuals.iterator();

        while(var2.hasNext()) {
            base.operators.operator.features.optimization.Individual ind = (base.operators.operator.features.optimization.Individual)var2.next();
            this.add(ind);
        }

    }

    base.operators.operator.features.optimization.Individual getOriginalFeatureSetIndividual() {
        return this.originalFeatureSetIndividual;
    }

    void performSelection() {
        this.selection.performSelection(this);
    }

    base.operators.operator.features.optimization.Individual getBestIndividual() {
        double bestError = 1.0D / 0.0;
        base.operators.operator.features.optimization.Individual best = null;
        Iterator var4 = this.population.iterator();

        while(var4.hasNext()) {
            base.operators.operator.features.optimization.Individual ind = (base.operators.operator.features.optimization.Individual)var4.next();
            if (ind.getError() < bestError) {
                bestError = ind.getError();
                best = ind;
            }
        }

        return best;
    }

    FeatureSet mutate(int index) {
        FeatureSet clonedSet = new FeatureSet(((base.operators.operator.features.optimization.Individual)this.population.get(index)).getFeatureSet());
        double removalProb = 0.5D / (double)clonedSet.getNumberOfFeatures();

        for(int f = clonedSet.getNumberOfFeatures() - 1; f > 0; --f) {
            if (this.rng.nextDouble() < removalProb) {
                clonedSet.remove(f);
            }
        }

        List<Feature> possibleArgumentsForGeneration = null;
        if (!this.onlySelection) {
            possibleArgumentsForGeneration = new LinkedList();
            Iterator var6 = clonedSet.iterator();

            label81:
            while(true) {
                Feature feature;
                do {
                    do {
                        if (!var6.hasNext()) {
                            break label81;
                        }

                        feature = (Feature)var6.next();
                    } while(!feature.isNumerical());
                } while(feature.getComplexity() <= 1 && !this.originalFeaturesForGeneration.contains(feature));

                possibleArgumentsForGeneration.add(feature);
            }
        }

        double addingProb = 0.5D / (double)this.originalFeatures.size();
        Iterator var8 = this.originalFeatures.iterator();

        while(true) {
            Feature original;
            do {
                if (!var8.hasNext()) {
                    if (!this.onlySelection && possibleArgumentsForGeneration != null && possibleArgumentsForGeneration.size() > 0 && this.rng.nextDouble() < 0.1D) {
                        base.operators.operator.features.optimization.Function function = (base.operators.operator.features.optimization.Function)this.functions.get(this.rng.nextInt(this.functions.size()));
                        int numberOfArguments = function.getNumberOfArguments();
                        if (!function.needsDifferentArguments() || possibleArgumentsForGeneration.size() >= numberOfArguments) {
                            String[] argumentFeatures = new String[numberOfArguments];
                            int totalComplexity = 0;

                            for(int a = 0; a < argumentFeatures.length; ++a) {
                                Feature currentArgument;
                                if (function.needsDifferentArguments()) {
                                    currentArgument = (Feature)possibleArgumentsForGeneration.remove(this.rng.nextInt(possibleArgumentsForGeneration.size()));
                                } else {
                                    currentArgument = (Feature)possibleArgumentsForGeneration.get(this.rng.nextInt(possibleArgumentsForGeneration.size()));
                                }

                                argumentFeatures[a] = currentArgument.getExpression();
                                totalComplexity += currentArgument.getComplexity();
                            }

                            String newName = "GenSym" + this.genSymCounter;
                            ++this.genSymCounter;
                            String newExpression = function.createExpression(argumentFeatures);
                            totalComplexity += function.getComplexity();
                            if (totalComplexity <= this.maxFunctionComplexity) {
                                Feature newFeature = new Feature(newName, newExpression, true, totalComplexity);
                                clonedSet.add(newFeature);
                            }
                        }
                    }

                    return clonedSet;
                }

                original = (Feature)var8.next();
            } while(clonedSet.getNumberOfFeatures() != 0 && this.rng.nextDouble() >= addingProb);

            Feature newCandidate = new Feature(original);
            clonedSet.add(newCandidate);
        }
    }

    FeatureSet crossover(int parentIndex1, int parentIndex2) {
        FeatureSet child = new FeatureSet();
        FeatureSet parent1 = ((base.operators.operator.features.optimization.Individual)this.population.get(parentIndex1)).getFeatureSet();
        FeatureSet parent2 = ((base.operators.operator.features.optimization.Individual)this.population.get(parentIndex2)).getFeatureSet();
        double crossoverProb = 0.5D;
        Iterator var8 = parent1.iterator();

        Feature feature;
        while(var8.hasNext()) {
            feature = (Feature)var8.next();
            if (this.rng.nextDouble() < crossoverProb) {
                child.add(new Feature(feature));
            }
        }

        var8 = parent2.iterator();

        while(var8.hasNext()) {
            feature = (Feature)var8.next();
            if (this.rng.nextDouble() < crossoverProb) {
                child.add(new Feature(feature));
            }
        }

        return child;
    }

    public List<base.operators.operator.features.optimization.Individual> getParetoFront(boolean removeVerticalPart) {
        List<base.operators.operator.features.optimization.Individual> paretoFront = this.selection.getNextRank(this);
        if (!paretoFront.contains(this.originalFeatureSetIndividual)) {
            paretoFront.add(this.originalFeatureSetIndividual);
        }

        List<base.operators.operator.features.optimization.Individual> removeDuplicateList = new LinkedList();

        base.operators.operator.features.optimization.Individual candidate1;
        for(int i = 0; i < paretoFront.size() - 1; ++i) {
            candidate1 = (base.operators.operator.features.optimization.Individual)paretoFront.get(i);
            if (!candidate1.isOriginal()) {
                for(int j = i + 1; j < paretoFront.size(); ++j) {
                    base.operators.operator.features.optimization.Individual candidate2 = (base.operators.operator.features.optimization.Individual)paretoFront.get(j);
                    if (!candidate2.isOriginal() && candidate1.getComplexity() == candidate2.getComplexity()) {
                        double error1 = candidate1.getError();
                        double error2 = candidate2.getError();
                        if (error1 < error2) {
                            removeDuplicateList.add(candidate2);
                        } else {
                            removeDuplicateList.add(candidate1);
                        }
                    }
                }
            }
        }

        Iterator var16 = removeDuplicateList.iterator();

        while(var16.hasNext()) {
            candidate1 = (base.operators.operator.features.optimization.Individual)var16.next();
            paretoFront.remove(candidate1);
        }

        if (paretoFront.size() == 1) {
            return paretoFront;
        } else {
            paretoFront.sort(new ErrorComparator());
            List<base.operators.operator.features.optimization.Individual> result = paretoFront;
            if (removeVerticalPart && paretoFront.size() > 2) {
                double minError = ((base.operators.operator.features.optimization.Individual)paretoFront.get(0)).getError();
                double maxError = ((base.operators.operator.features.optimization.Individual)paretoFront.get(paretoFront.size() - 1)).getError();
                double errorDistance = maxError - minError;
                int cutOffPoint = -1;

                for(Iterator var12 = paretoFront.iterator(); var12.hasNext(); ++cutOffPoint) {
                    base.operators.operator.features.optimization.Individual individual = (base.operators.operator.features.optimization.Individual)var12.next();
                    double error = individual.getError();
                    if ((error - minError) / errorDistance > 0.05D) {
                        break;
                    }
                }

                result = paretoFront.subList(cutOffPoint, paretoFront.size());
            }

            if (!result.contains(this.originalFeatureSetIndividual)) {
                result.add(this.originalFeatureSetIndividual);
            }

            result.sort(new ErrorComparator());
            return result;
        }
    }

    public base.operators.operator.features.optimization.Individual getIndividualForBalance(boolean removeVerticalPart, double balanceFactor) {
        List<base.operators.operator.features.optimization.Individual> paretoFront = this.getParetoFront(removeVerticalPart);
        if (paretoFront.size() == 1) {
            return (base.operators.operator.features.optimization.Individual)paretoFront.get(0);
        } else {
            int index = (int)Math.max(0L, Math.min(Math.round((double)(paretoFront.size() - 1) * (1.0D - balanceFactor)), (long)(paretoFront.size() - 1)));
            return (base.operators.operator.features.optimization.Individual)paretoFront.get(index);
        }
    }

    public int getGeneration() {
        return this.generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public Iterator<base.operators.operator.features.optimization.Individual> iterator() {
        return this.population.iterator();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("=== Population Size: ").append(this.population.size()).append("\n");
        Iterator var2 = this.population.iterator();

        while(var2.hasNext()) {
            base.operators.operator.features.optimization.Individual ind = (base.operators.operator.features.optimization.Individual)var2.next();
            result.append(ind.toString()).append("\n");
        }

        return result.toString();
    }
}
