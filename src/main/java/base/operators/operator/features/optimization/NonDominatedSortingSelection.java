package base.operators.operator.features.optimization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

class NonDominatedSortingSelection {
    private int popSize;
    private boolean minimizeFeatures;

    NonDominatedSortingSelection(int popSize, boolean minimizeFeatures) {
        this.popSize = popSize;
        this.minimizeFeatures = minimizeFeatures;
    }

    void performSelection(Population population) {
        Iterator p = population.iterator();

        base.operators.operator.features.optimization.Individual best;
        while(p.hasNext()) {
            best = (base.operators.operator.features.optimization.Individual)p.next();
            if (Double.isInfinite(best.getError())) {
                p.remove();
            }
        }

        best = population.getBestIndividual();
        if (best != null) {
            population.remove(best);
        }

        base.operators.operator.features.optimization.Individual originalIndividual = population.getOriginalFeatureSetIndividual();
        if (originalIndividual != null) {
            population.remove(originalIndividual);
        }

        ArrayList ranks = new ArrayList();

        while(population.getNumberOfIndividuals() > 0) {
            List<base.operators.operator.features.optimization.Individual> rank = this.getNextRank(population);
            ranks.add(rank);
            Iterator var7 = rank.iterator();

            while(var7.hasNext()) {
                base.operators.operator.features.optimization.Individual ind = (base.operators.operator.features.optimization.Individual)var7.next();
                population.remove(ind);
            }
        }

        population.clear();
        if (best != null) {
            population.add(best);
        }

        if (originalIndividual != null) {
            population.add(originalIndividual);
        }

        int index;
        for(index = 0; index < ranks.size() && population.getNumberOfIndividuals() + ((List)ranks.get(index)).size() <= this.popSize; ++index) {
            population.addAll((List)ranks.get(index));
        }

        if (index < ranks.size() && population.getNumberOfIndividuals() < this.popSize) {
            List<base.operators.operator.features.optimization.Individual> rank = (List)ranks.get(index);
            this.sortByCrowdingDistance(rank);

            while(population.getNumberOfIndividuals() < this.popSize && rank.size() > 0) {
                population.add((base.operators.operator.features.optimization.Individual)rank.remove(0));
            }
        }

    }

    private void sortByCrowdingDistance(List<base.operators.operator.features.optimization.Individual> rank) {
        Iterator var2 = rank.iterator();

        while(var2.hasNext()) {
            base.operators.operator.features.optimization.Individual current = (base.operators.operator.features.optimization.Individual)var2.next();
            current.setCrowdingDistance(0.0D);
        }

        Comparator<base.operators.operator.features.optimization.Individual> errorComparator = new NonDominatedSortingSelection.ErrorComparator();
        rank.sort(errorComparator);
        ((base.operators.operator.features.optimization.Individual)rank.get(0)).setCrowdingDistance(1.0D / 0.0);
        ((base.operators.operator.features.optimization.Individual)rank.get(rank.size() - 1)).setCrowdingDistance(1.0D / 0.0);

        base.operators.operator.features.optimization.Individual afterI;
        for(int i = 1; i < rank.size() - 1; ++i) {
            base.operators.operator.features.optimization.Individual current = (base.operators.operator.features.optimization.Individual)rank.get(i);
            double currentCrowdingDistance = current.getCrowdingDistance();
            afterI = (base.operators.operator.features.optimization.Individual)rank.get(i + 1);
            afterI = (base.operators.operator.features.optimization.Individual)rank.get(i - 1);
            double afterError = afterI.getError();
            double beforeError = afterI.getError();
            current.setCrowdingDistance(currentCrowdingDistance + Math.abs(afterError - beforeError));
        }

        Comparator<base.operators.operator.features.optimization.Individual> complexityComparator = new NonDominatedSortingSelection.ComplexityComparator();
        rank.sort(complexityComparator);
        ((base.operators.operator.features.optimization.Individual)rank.get(0)).setCrowdingDistance(1.0D / 0.0);
        ((base.operators.operator.features.optimization.Individual)rank.get(rank.size() - 1)).setCrowdingDistance(1.0D / 0.0);

        for(int i = 1; i < rank.size() - 1; ++i) {
            base.operators.operator.features.optimization.Individual current = (base.operators.operator.features.optimization.Individual)rank.get(i);
            double currentCrowdingDistance = current.getCrowdingDistance();
            afterI = (base.operators.operator.features.optimization.Individual)rank.get(i + 1);
            base.operators.operator.features.optimization.Individual beforeI = (base.operators.operator.features.optimization.Individual)rank.get(i - 1);
            double afterComplexity = afterI.getError();
            double beforeComplexity = beforeI.getError();
            current.setCrowdingDistance(currentCrowdingDistance + Math.abs(afterComplexity - beforeComplexity));
        }

        rank.sort(new NonDominatedSortingSelection.CrowdingComparator());
    }

    List<base.operators.operator.features.optimization.Individual> getNextRank(Population population) {
        List<base.operators.operator.features.optimization.Individual> rank = new ArrayList();

        for(int i = 0; i < population.getNumberOfIndividuals(); ++i) {
            base.operators.operator.features.optimization.Individual current = population.get(i);
            rank.add(current);
            boolean delete = false;

            for(int j = rank.size() - 2; j >= 0; --j) {
                base.operators.operator.features.optimization.Individual ranked = (base.operators.operator.features.optimization.Individual)rank.get(j);
                if (this.isDominated(ranked, current)) {
                    rank.remove(ranked);
                }

                if (this.isDominated(current, ranked)) {
                    delete = true;
                }
            }

            if (delete) {
                rank.remove(current);
            }
        }

        return rank;
    }

    private boolean isDominated(base.operators.operator.features.optimization.Individual i1, base.operators.operator.features.optimization.Individual i2) {
        double error1 = i1.getError();
        double error2 = i2.getError();
        int complexity1 = i1.getComplexity();
        int complexity2 = i2.getComplexity();
        if (this.minimizeFeatures) {
            return error2 <= error1 && complexity2 <= complexity1 && (error2 < error1 || complexity2 < complexity1);
        } else {
            return error2 <= error1 && complexity2 >= complexity1 && (error2 < error1 || complexity2 > complexity1);
        }
    }

    private static class CrowdingComparator implements Comparator<base.operators.operator.features.optimization.Individual>, Serializable {
        private static final long serialVersionUID = -8973760685730111443L;

        CrowdingComparator() {
        }

        @Override
        public int compare(base.operators.operator.features.optimization.Individual i1, base.operators.operator.features.optimization.Individual i2) {
            return -1 * Double.compare(i1.getCrowdingDistance(), i2.getCrowdingDistance());
        }
    }

    private static class ComplexityComparator implements Comparator<base.operators.operator.features.optimization.Individual>, Serializable {
        private static final long serialVersionUID = -8973760685730111443L;

        ComplexityComparator() {
        }

        @Override
        public int compare(base.operators.operator.features.optimization.Individual i1, base.operators.operator.features.optimization.Individual i2) {
            return Double.compare((double)i1.getComplexity(), (double)i2.getComplexity());
        }
    }

    static class ErrorComparator implements Comparator<base.operators.operator.features.optimization.Individual>, Serializable {
        private static final long serialVersionUID = -8973760685730111443L;

        ErrorComparator() {
        }

        @Override
        public int compare(base.operators.operator.features.optimization.Individual i1, base.operators.operator.features.optimization.Individual i2) {
            return Double.compare(i1.getError(), i2.getError());
        }
    }
}
