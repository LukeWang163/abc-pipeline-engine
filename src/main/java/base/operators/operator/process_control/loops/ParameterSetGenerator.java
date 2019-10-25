package base.operators.operator.process_control.loops;

import base.operators.operator.meta.ParameterSet;
import java.util.AbstractList;

class ParameterSetGenerator
        extends AbstractList<ParameterSet>
{
    private String[] operatorNames;
    private String[] parameterNames;
    private String[][] valueMatrix;
    private boolean synced;
    private int size;

    ParameterSetGenerator(String[] operatorNames, String[] parameterNames, String[][] valueMatrix, boolean synced, int numberOfCombinations) {
        this.operatorNames = operatorNames;
        this.parameterNames = parameterNames;
        this.valueMatrix = valueMatrix;
        this.synced = synced;
        this.size = numberOfCombinations;
    }

    @Override
    public ParameterSet get(int index) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
        String[] values = new String[this.valueMatrix.length];
        if (this.synced) {
            for (int i = 0; i < values.length; i++) {
                values[i] = this.valueMatrix[i][index];
            }
        } else {
            int tmp = index;
            for (int i = 0; i < this.valueMatrix.length; i++) {
                values[i] = this.valueMatrix[i][tmp % this.valueMatrix[i].length];
                tmp /= this.valueMatrix[i].length;
            }
        }
        return new ParameterSet(this.operatorNames, this.parameterNames, values, null);
    }

    @Override
    public int size() { return this.size; }

    public boolean equals(Object o) { return (o == this); }

    @Override
    public int hashCode() { return System.identityHashCode(this); }
}
