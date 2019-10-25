package base.operators.operator.learner.associations.fpgrowth;

import base.operators.example.Attribute;
import base.operators.example.Example;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

public final class ItemSet
        extends Object
        implements Iterable<String>
{
    private final Set<String> set;

    ItemSet(String[] array, boolean trim) {
        if (array != null) {
            this.set = new HashSet();
            for (String item : array) {
                this.set.add(trim ? item.trim() : item);
            }
        } else {
            this.set = Collections.emptySet();
        }
    }

    ItemSet(Example example, Attribute[] attributes, boolean trim) {
        this.set = new HashSet();
        for (Attribute attribute : attributes) {
            if (!Double.isNaN(example.getValue(attribute))) {
                String item = example.getValueAsString(attribute);
                this.set.add(trim ? item.trim() : item);
            }
        }
    }

    ItemSet(Example example, Attribute[] attributes, int[] positiveIndices) {
        this.set = new HashSet();
        for (int i = 0; i < attributes.length; i++) {
            Attribute attribute = attributes[i];
            if (example.getValue(attribute) == positiveIndices[i]) {
                this.set.add(attribute.getName());
            }
        }
    }

    int size() { return this.set.size(); }

    boolean contains(String o) { return this.set.contains(o); }

    @Override
    public Iterator<String> iterator() { return this.set.iterator(); }

    @Override
    public void forEach(Consumer<? super String> action) { this.set.forEach(action); }

    @Override
    public Spliterator<String> spliterator() { return this.set.spliterator(); }
}

