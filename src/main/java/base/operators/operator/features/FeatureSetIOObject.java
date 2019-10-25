package base.operators.operator.features;

import base.operators.operator.ResultObjectAdapter;
import base.operators.tools.Tools;

public class FeatureSetIOObject extends ResultObjectAdapter {
    private static final long serialVersionUID = -4872148452467130940L;
    private FeatureSet featureSet;
    private double lastKnownFitness = 0.0D / 0.0;
    private boolean isPercent = false;
    private boolean originalFeatureSet;

    FeatureSetIOObject(FeatureSet featureSet, boolean originalFeatureSet) {
        this.featureSet = featureSet;
        this.originalFeatureSet = originalFeatureSet;
    }

    public FeatureSet getFeatureSet() {
        return this.featureSet;
    }

    public double getLastKnownFitness() {
        return this.lastKnownFitness;
    }

    public boolean isPercent() {
        return this.isPercent;
    }

    public boolean isOriginal() {
        return this.originalFeatureSet;
    }

    void setLastKnownFitness(double lastKnownFitness, boolean isPercent) {
        this.lastKnownFitness = lastKnownFitness;
        this.isPercent = isPercent;
    }

    @Override
    public String toString() {
        if (this.featureSet != null) {
            if (!Double.isNaN(this.lastKnownFitness)) {
                return this.isPercent ? "Feature Set [" + Tools.formatPercent(this.lastKnownFitness) + "]\n" + this.featureSet.toString() : "Feature Set [" + Tools.formatIntegerIfPossible(this.lastKnownFitness) + "]\n" + this.featureSet.toString();
            } else {
                return "Feature Set\n" + this.featureSet.toString();
            }
        } else {
            return "Unknown feature set";
        }
    }
}
