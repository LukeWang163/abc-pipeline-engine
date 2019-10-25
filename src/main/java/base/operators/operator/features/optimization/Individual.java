package base.operators.operator.features.optimization;

import base.operators.operator.features.FeatureSet;
import base.operators.tools.Tools;

public class Individual {
    private FeatureSet featureSet;
    private double errorRate = 0.0D / 0.0;
    private boolean isPercent = false;
    private boolean original = false;
    private double crowdingDistance = 0.0D / 0.0;

    Individual(FeatureSet featureSet) {
        this.featureSet = featureSet;
    }

    Individual(Individual other) {
        this.featureSet = new FeatureSet(other.featureSet);
        this.errorRate = other.errorRate;
        this.isPercent = other.isPercent;
        this.crowdingDistance = other.crowdingDistance;
        this.original = other.original;
    }

    void setOriginal(boolean original) {
        this.original = original;
    }

    public boolean isOriginal() {
        return this.original;
    }

    public void setError(double errorRate, boolean isPercent) {
        this.errorRate = errorRate;
        this.isPercent = isPercent;
    }

    public double getError() {
        return this.errorRate;
    }

    public boolean isPercent() {
        return this.isPercent;
    }

    int getComplexity() {
        return this.featureSet.getTotalComplexity();
    }

    public FeatureSet getFeatureSet() {
        return this.featureSet;
    }

    double getCrowdingDistance() {
        return this.crowdingDistance;
    }

    void setCrowdingDistance(double crowdingDistance) {
        this.crowdingDistance = crowdingDistance;
    }

    public boolean equals(Object o) {
        if (o instanceof Individual) {
            Individual other = (Individual)o;
            return this.featureSet.equals(other.featureSet);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.featureSet.hashCode();
    }

    public String toString() {
        return "Individual --> # = " + this.featureSet.getNumberOfFeatures() + ", error = " + Tools.formatIntegerIfPossible(this.errorRate);
    }
}
