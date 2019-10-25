package base.operators.operator.tools;

import base.operators.tools.Tools;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CostMatrix implements Serializable {
    private static final long serialVersionUID = -4799627911437197651L;
    private List<String> classes;
    private double[][] costMatrix;

    public CostMatrix(List<String> classes) {
        this.classes = classes;
        this.costMatrix = new double[this.classes.size()][this.classes.size()];

        for(int p = 0; p < this.classes.size(); ++p) {
            for(int t = 0; t < this.classes.size(); ++t) {
                if (p == t) {
                    this.costMatrix[p][t] = 1.0D;
                } else {
                    this.costMatrix[p][t] = -1.0D;
                }
            }
        }

    }

    private CostMatrix(CostMatrix other) {
        this.classes = new LinkedList();
        this.classes.addAll(other.classes);
        this.costMatrix = new double[this.classes.size()][this.classes.size()];

        for(int p = 0; p < this.classes.size(); ++p) {
            System.arraycopy(other.costMatrix[p], 0, this.costMatrix[p], 0, this.classes.size());
        }

    }

    public List<String> getClasses() {
        return this.classes;
    }

    public void setCost(String predictedClass, String trueClass, double cost) {
        int predictedIndex = this.classes.indexOf(predictedClass);
        int trueIndex = this.classes.indexOf(trueClass);
        this.costMatrix[predictedIndex][trueIndex] = cost;
    }

    public double getCost(String predictedClass, String trueClass) {
        int predictedIndex = this.classes.indexOf(predictedClass);
        int trueIndex = this.classes.indexOf(trueClass);
        return this.costMatrix[predictedIndex][trueIndex];
    }

    public boolean isInitialCosts() {
        for(int p = 0; p < this.classes.size(); ++p) {
            for(int t = 0; t < this.classes.size(); ++t) {
                if (p == t) {
                    if (!Tools.isEqual(this.costMatrix[p][t], 1.0D)) {
                        return false;
                    }
                } else if (!Tools.isEqual(this.costMatrix[p][t], -1.0D)) {
                    return false;
                }
            }
        }

        return true;
    }

    public String getParameterString() {
        StringBuilder result = new StringBuilder();
        result.append("[");

        for(int p = 0; p < this.classes.size(); ++p) {
            boolean first = true;

            for(int t = 0; t < this.classes.size(); ++t) {
                if (!first) {
                    result.append(" ");
                }

                first = false;
                if (Tools.isZero(this.costMatrix[p][t])) {
                    result.append(0);
                } else {
                    result.append(-1.0D * this.costMatrix[p][t]);
                }
            }

            result.append(";");
        }

        result.append("]");
        return result.toString();
    }

    public Object clone() {
        return new CostMatrix(this);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Cost Matrix").append(Tools.getLineSeparator());
        result.append(this.classes.size()).append(" classes:").append(Tools.getLineSeparator());
        Iterator var2 = this.classes.iterator();

        while(var2.hasNext()) {
            String label = (String)var2.next();
            result.append(" - ").append(label).append(Tools.getLineSeparator());
        }

        for(int p = 0; p < this.classes.size(); ++p) {
            for(int t = 0; t < this.classes.size(); ++t) {
                result.append(Tools.formatIntegerIfPossible(this.costMatrix[p][t])).append("\t");
            }

            result.append(Tools.getLineSeparator());
        }

        return result.toString();
    }
}

