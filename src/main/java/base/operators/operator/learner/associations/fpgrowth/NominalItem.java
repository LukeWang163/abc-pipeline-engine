package base.operators.operator.learner.associations.fpgrowth;

import base.operators.operator.learner.associations.Item;
import java.util.Objects;

class NominalItem
        implements Item
{
    private static final long serialVersionUID = 1L;
    private String name;
    private int frequency;

    NominalItem(String name) { this.name = name; }

    private NominalItem(String name, int frequency) {
        this.name = name;
        this.frequency = frequency;
    }

    @Override
    public int getFrequency() { return this.frequency; }

    public String getName() { return this.name; }

    @Override
    public void increaseFrequency() { this.frequency++; }

    @Override
    public void increaseFrequency(int frequency) { this.frequency += frequency; }

    @Override
    public int hashCode() {
        int result = (this.name != null) ? this.name.hashCode() : 0;
        return 31 * result + this.frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NominalItem that = (NominalItem)o;
        return (this.frequency == that.frequency &&
                Objects.equals(toString(), that.toString()));
    }

    @Override
    public int compareTo(Item other) {
        if (this.frequency < other.getFrequency()) {
            return 1;
        }
        if (this.frequency == other.getFrequency()) {
            return toString().compareTo(other.toString());
        }
        return -1;
    }

    @Override
    public String toString() { return getName(); }

    public Object clone() { return new NominalItem(this.name, this.frequency); }

    boolean isPresentInItemSet(ItemSet itemSet) { return itemSet.contains(this.name); }
}
