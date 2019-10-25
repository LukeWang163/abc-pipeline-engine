package base.operators.operator.preprocessing.cleansing;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.NominalMapping;
import base.operators.example.table.ViewAttribute;
import base.operators.operator.preprocessing.PreprocessingModel;
import base.operators.tools.Ontology;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MissingValuesPreprocessingModel extends PreprocessingModel {
    private static final long serialVersionUID = -5373518707854402199L;
    private static final String NOMINAL_MISSING = "MISSING";
    private HashMap<String, Double> numericalAndDateReplacementMap = new HashMap();
    private Set<String> nominalColumns = new HashSet();
    private HashMap<Attribute, Double> attributeReplacementMap = new HashMap();

    public MissingValuesPreprocessingModel(ExampleSet exampleSet) {
        super(exampleSet);
        Iterator var2 = exampleSet.getAttributes().iterator();

        while(var2.hasNext()) {
            Attribute attribute = (Attribute)var2.next();
            String attributeName = attribute.getName();
            if (attribute.isNominal()) {
                this.nominalColumns.add(attributeName);
            } else if (attribute.isNumerical()) {
                this.numericalAndDateReplacementMap.put(attributeName, this.getAverage(exampleSet, attribute));
            } else if (attribute.isDateTime()) {
                this.numericalAndDateReplacementMap.put(attributeName, this.getFirstDate(exampleSet, attribute));
            }
        }

    }

    private double getAverage(ExampleSet data, Attribute attribute) {
        double sum = 0.0D;
        int counter = 0;
        Iterator var6 = data.iterator();

        while(var6.hasNext()) {
            Example example = (Example)var6.next();
            double value = example.getValue(attribute);
            if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                sum += value;
                ++counter;
            }
        }

        if (counter > 0) {
            return sum / (double)counter;
        } else {
            return 0.0D;
        }
    }

    private double getFirstDate(ExampleSet data, Attribute attribute) {
        double min = 1.0D / 0.0;
        boolean foundDate = false;
        Iterator var6 = data.iterator();

        while(var6.hasNext()) {
            Example example = (Example)var6.next();
            double value = example.getValue(attribute);
            if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                min = Math.min(min, value);
                foundDate = true;
            }
        }

        if (foundDate) {
            return min;
        } else {
            return (double)(new Date(0L)).getTime();
        }
    }

    @Override
    public ExampleSet applyOnData(ExampleSet exampleSet) {
        Iterator var2 = exampleSet.getAttributes().iterator();

        while(true) {
            Attribute attribute;
            label55:
            do {
                label51:
                while(var2.hasNext()) {
                    attribute = (Attribute)var2.next();
                    if (attribute.isNominal()) {
                        continue label55;
                    }

                    if (attribute.isNumerical() || attribute.isDateTime()) {
                        Double replacementValue = (Double)this.numericalAndDateReplacementMap.get(attribute.getName());
                        if (replacementValue != null) {
                            Iterator var5 = exampleSet.iterator();

                            while(true) {
                                Example example;
                                double value;
                                do {
                                    if (!var5.hasNext()) {
                                        continue label51;
                                    }

                                    example = (Example)var5.next();
                                    value = example.getValue(attribute);
                                } while(!Double.isNaN(value) && !Double.isInfinite(value));

                                example.setValue(attribute, replacementValue);
                            }
                        }
                    }
                }

                return exampleSet;
            } while(!this.nominalColumns.contains(attribute.getName()));

            if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 6)) {
                Attribute newAttribute = AttributeFactory.changeValueType(attribute, 1);
                exampleSet.getAttributes().replace(attribute, newAttribute);
                attribute = newAttribute;
            }

            double nominalMissing = (double)attribute.getMapping().mapString("MISSING");
            Iterator var12 = exampleSet.iterator();

            while(var12.hasNext()) {
                Example example = (Example)var12.next();
                double value = example.getValue(attribute);
                if (Double.isNaN(value)) {
                    example.setValue(attribute, nominalMissing);
                }
            }
        }
    }

    @Override
    public Attributes getTargetAttributes(ExampleSet viewParent) {
        List<Attribute> targetAttributes = new ArrayList();
        Attributes attributes = viewParent.getAttributes();
        Iterator iterator = attributes.allAttributes();

        while(iterator.hasNext()) {
            Attribute attribute = (Attribute)iterator.next();
            if (attribute.isNominal()) {
                Attribute viewAttribute = new ViewAttribute(this, attribute, attribute.getName(), attribute.getValueType(), attribute.getMapping());
                this.attributeReplacementMap.put(viewAttribute, (double)attribute.getMapping().mapString("MISSING"));
                targetAttributes.add(viewAttribute);
                iterator.remove();
            }

            if (attribute.isNumerical() || attribute.isDateTime()) {
                Double replacement = (Double)this.numericalAndDateReplacementMap.get(attribute.getName());
                if (replacement != null) {
                    Attribute viewAttribute = new ViewAttribute(this, attribute, attribute.getName(), attribute.getValueType(), (NominalMapping)null);
                    this.attributeReplacementMap.put(viewAttribute, replacement);
                    targetAttributes.add(viewAttribute);
                    iterator.remove();
                }
            }
        }

        Iterator var8 = targetAttributes.iterator();

        while(var8.hasNext()) {
            Attribute attribute = (Attribute)var8.next();
            attributes.addRegular(attribute);
        }

        return attributes;
    }

    @Override
    public double getValue(Attribute targetAttribute, double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) ? value : (Double)this.attributeReplacementMap.get(targetAttribute);
    }

    @Override
    protected boolean writesIntoExistingData() {
        return true;
    }

    @Override
    protected boolean needsRemapping() {
        return false;
    }
}
