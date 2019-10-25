package base.operators.operator.preprocessing.cleansing;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.preprocessing.PreprocessingModel;
import base.operators.tools.Ontology;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class KnownValuesModel extends PreprocessingModel {
    private static final long serialVersionUID = 2637739528279767916L;
    private Map<String, Set<String>> knownValues = new HashMap();

    public KnownValuesModel(ExampleSet exampleSet) {
        super(exampleSet);
        Iterator var2 = exampleSet.getAttributes().iterator();

        while(var2.hasNext()) {
            Attribute attribute = (Attribute)var2.next();
            if (attribute.isNominal() && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 5)) {
                this.knownValues.put(attribute.getName(), new HashSet());
            }
        }

        var2 = exampleSet.iterator();

        while(var2.hasNext()) {
            Example example = (Example)var2.next();
            Iterator var4 = exampleSet.getAttributes().iterator();

            while(var4.hasNext()) {
                Attribute attribute = (Attribute)var4.next();
                if (attribute.isNominal() && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 5)) {
                    double doubleValue = example.getValue(attribute);
                    if (!Double.isNaN(doubleValue)) {
                        ((Set)this.knownValues.get(attribute.getName())).add(example.getValueAsString(attribute));
                    }
                }
            }
        }

    }

    @Override
    public ExampleSet applyOnData(ExampleSet exampleSet) {
        Iterator var2 = exampleSet.iterator();

        while(var2.hasNext()) {
            Example example = (Example)var2.next();
            Iterator var4 = exampleSet.getAttributes().iterator();

            while(var4.hasNext()) {
                Attribute attribute = (Attribute)var4.next();
                if (attribute.isNominal() && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 5) && this.knownValues.get(attribute.getName()) != null) {
                    double doubleValue = example.getValue(attribute);
                    if (!Double.isNaN(doubleValue)) {
                        String value = example.getValueAsString(attribute);
                        if (!((Set)this.knownValues.get(attribute.getName())).contains(value)) {
                            example.setValue(attribute, 0.0D / 0.0);
                        }
                    }
                }
            }
        }

        return exampleSet;
    }

    @Override
    public Attributes getTargetAttributes(ExampleSet viewParent) {
        return viewParent.getAttributes();
    }

    @Override
    public double getValue(Attribute targetAttribute, double value) {
        if (targetAttribute.isNominal() && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(targetAttribute.getValueType(), 5) && this.knownValues.get(targetAttribute.getName()) != null && !Double.isNaN(value)) {
            String valueString = targetAttribute.getMapping().mapIndex((int)value);
            if (!((Set)this.knownValues.get(targetAttribute.getName())).contains(valueString)) {
                return 0.0D / 0.0;
            }
        }

        return value;
    }

    @Override
    protected boolean writesIntoExistingData() {
        return true;
    }

    @Override
    protected boolean needsRemapping() {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Known values for ").append(this.knownValues.keySet().size()).append(" attributes:\n\n");
        Iterator var2 = this.knownValues.entrySet().iterator();

        while(var2.hasNext()) {
            Entry<String, Set<String>> entry = (Entry)var2.next();
            result.append("   - ").append((String)entry.getKey()).append(" (").append(((Set)entry.getValue()).size()).append(")\n");
        }

        return result.toString();
    }

    @Override
    public String toResultString() {
        return this.toString();
    }
}
