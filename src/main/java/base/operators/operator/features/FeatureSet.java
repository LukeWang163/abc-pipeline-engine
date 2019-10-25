package base.operators.operator.features;

import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.ProcessStoppedException;
import base.operators.tools.LogService;
import base.operators.tools.expression.ExampleResolver;
import base.operators.tools.expression.ExpressionException;
import base.operators.tools.expression.ExpressionParser;
import base.operators.tools.expression.ExpressionParserBuilder;
import base.operators.tools.expression.ExpressionRegistry;
import base.operators.tools.expression.internal.ExpressionParserUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Level;

public class FeatureSet implements Serializable, Iterable<Feature> {
    private static final long serialVersionUID = 6823921718738701058L;
    private List<Feature> featureSet = new ArrayList();
    private Map<String, Double> numericalValuesForRecreation;
    private Map<String, String> nominalValuesForRecreation;
    private Map<String, Double> dateValuesForRecreation;
    private Map<String, FeatureSet.Type> typeMapForRecreation;

    public FeatureSet() {
    }

    public FeatureSet(ExampleSet exampleSet) {
        Iterator var2 = exampleSet.getAttributes().iterator();

        while(var2.hasNext()) {
            Attribute a = (Attribute)var2.next();
            this.add(new Feature(a));
        }

    }

    public FeatureSet(FeatureSet other) {
        Iterator var2 = other.featureSet.iterator();

        while(var2.hasNext()) {
            Feature o = (Feature)var2.next();
            this.featureSet.add(new Feature(o));
        }

        this.numericalValuesForRecreation = other.numericalValuesForRecreation;
        this.nominalValuesForRecreation = other.nominalValuesForRecreation;
        this.dateValuesForRecreation = other.dateValuesForRecreation;
        this.typeMapForRecreation = other.typeMapForRecreation;
    }

    public void add(Feature feature) {
        if (!this.featureSet.contains(feature)) {
            this.featureSet.add(feature);
        }

    }

    public void remove(int index) {
        this.featureSet.remove(index);
    }

    public boolean contains(Feature feature) {
        return this.featureSet.contains(feature);
    }

    public int getNumberOfFeatures() {
        return this.featureSet.size();
    }

    public Feature get(int index) {
        return (Feature)this.featureSet.get(index);
    }

    void storeStatistics(ExampleSet data) {
        this.numericalValuesForRecreation = new HashMap();
        this.nominalValuesForRecreation = new HashMap();
        this.dateValuesForRecreation = new HashMap();
        this.typeMapForRecreation = new HashMap();
        data.recalculateAllAttributeStatistics();
        Iterator var2 = data.getAttributes().iterator();

        while(var2.hasNext()) {
            Attribute attribute = (Attribute)var2.next();
            if (attribute.isNominal()) {
                this.nominalValuesForRecreation.put(attribute.getName(), attribute.getMapping().mapIndex((int)data.getStatistics(attribute, "mode")));
                this.typeMapForRecreation.put(attribute.getName(), FeatureSet.Type.NOMINAL);
            } else if (attribute.isNumerical()) {
                this.numericalValuesForRecreation.put(attribute.getName(), data.getStatistics(attribute, "average"));
                this.typeMapForRecreation.put(attribute.getName(), FeatureSet.Type.NUMERICAL);
            } else if (attribute.isDateTime()) {
                this.dateValuesForRecreation.put(attribute.getName(), data.getStatistics(attribute, "minimum"));
                this.typeMapForRecreation.put(attribute.getName(), FeatureSet.Type.DATE);
            }
        }

    }

    public int getTotalComplexity() {
        int total = 0;

        Feature f;
        for(Iterator var2 = this.featureSet.iterator(); var2.hasNext(); total += f.getComplexity()) {
            f = (Feature)var2.next();
        }

        return total;
    }

    public void removeDuplicates() {
        Iterator f = this.iterator();

        while(f.hasNext()) {
            Feature candidateForRemoval = (Feature)f.next();
            boolean remove = false;
            Iterator var4 = this.iterator();

            while(var4.hasNext()) {
                Feature feature = (Feature)var4.next();
                if (!feature.equals(candidateForRemoval) && feature.getExpression().equals(candidateForRemoval.getExpression())) {
                    remove = true;
                    break;
                }
            }

            if (remove) {
                f.remove();
            }
        }

    }

    public ExampleSet apply(ExampleSet exampleSet, boolean handleMissing, boolean keepOriginals, boolean originalsSpecial, boolean recreateMissingSimpleAttributes) {
        this.removeDuplicates();
        ExampleSet clone = (ExampleSet)exampleSet.clone();
        Iterator att;
        if (recreateMissingSimpleAttributes && this.typeMapForRecreation != null) {
            Iterator var7 = this.typeMapForRecreation.entrySet().iterator();

            label138:
            while(true) {
                Entry typeEntry;
                Attribute attribute;
                do {
                    if (!var7.hasNext()) {
                        break label138;
                    }

                    typeEntry = (Entry)var7.next();
                    attribute = clone.getAttributes().get((String)typeEntry.getKey());
                } while(attribute != null);

                Attribute newAttribute = null;
                double newValue = 0.0D / 0.0;
                switch((FeatureSet.Type)typeEntry.getValue()) {
                    case NUMERICAL:
                        newAttribute = AttributeFactory.createAttribute((String)typeEntry.getKey(), 4);
                        newValue = (Double)this.numericalValuesForRecreation.get(typeEntry.getKey());
                        break;
                    case NOMINAL:
                        newAttribute = AttributeFactory.createAttribute((String)typeEntry.getKey(), 1);
                        newValue = (double)newAttribute.getMapping().mapString((String)this.nominalValuesForRecreation.get(typeEntry.getKey()));
                        break;
                    case DATE:
                        newAttribute = AttributeFactory.createAttribute((String)typeEntry.getKey(), 9);
                        newValue = (Double)this.dateValuesForRecreation.get(typeEntry.getKey());
                }

                ExampleTable table = clone.getExampleTable();
                table.addAttribute(newAttribute);
                clone.getAttributes().addRegular(newAttribute);
                att = clone.iterator();

                while(att.hasNext()) {
                    Example example = (Example)att.next();
                    example.setValue(newAttribute, newValue);
                }
            }
        }

        ExampleResolver resolver = new ExampleResolver(clone);
        ExpressionParserBuilder builder = new ExpressionParserBuilder();
        builder.withDynamics(resolver);
        builder.withModules(ExpressionRegistry.INSTANCE.getAll());
        ExpressionParser expParser = builder.build();
        Map<String, String> name2construction = new HashMap();
        Iterator var30 = this.featureSet.iterator();

        while(true) {
            Feature feature;
            do {
                if (!var30.hasNext()) {
                    int originalCounter = 1;
                    Iterator a = clone.getAttributes().allAttributeRoles();

                    Iterator var37;
                    while(a.hasNext()) {
                        AttributeRole attributeRole = (AttributeRole)a.next();
                        boolean found = false;
                        var37 = this.iterator();

                        while(var37.hasNext()) {
                            feature = (Feature)var37.next();
                            if (feature.getName().equals(attributeRole.getAttribute().getName())) {
                                found = true;
                                break;
                            }
                        }

                        if (!attributeRole.isSpecial() && !found) {
                            if (!keepOriginals) {
                                a.remove();
                            } else if (originalsSpecial) {
                                attributeRole.setSpecial("Original " + originalCounter++);
                            }
                        }
                    }

                    Set<String> allConstructions = new HashSet();

                    Attribute attribute;
                    for(att = clone.getAttributes().iterator(); att.hasNext(); allConstructions.add(attribute.getConstruction())) {
                        attribute = (Attribute)att.next();
                        if (allConstructions.contains(attribute.getConstruction())) {
                            att.remove();
                        }
                    }

                    var37 = clone.getAttributes().iterator();

                    while(var37.hasNext()) {
                        attribute = (Attribute)var37.next();
                        String constructionFromMap = (String)name2construction.get(attribute.getName());
                        if (constructionFromMap != null) {
                            attribute.setName(constructionFromMap);
                        }
                    }

                    return clone;
                }

                feature = (Feature)var30.next();
            } while(feature.getComplexity() <= 1);

            try {
                Attribute newAttribute = ExpressionParserUtils.addAttribute(clone, feature.getName(), feature.getExpression(), expParser, resolver, (Operator)null);
                name2construction.put(newAttribute.getName(), newAttribute.getConstruction());
                if (handleMissing) {
                    double sum = 0.0D;
                    int counter = 0;
                    int missingCounter = 0;
                    Iterator var18 = clone.iterator();

                    while(var18.hasNext()) {
                        Example example = (Example)var18.next();
                        double value = example.getValue(newAttribute);
                        if (Double.isInfinite(value)) {
                            example.setValue(newAttribute, 0.0D / 0.0);
                            ++missingCounter;
                        } else if (!Double.isNaN(value)) {
                            sum += value;
                            ++counter;
                        } else {
                            ++missingCounter;
                        }
                    }

                    if (missingCounter > 0) {
                        double replacementValue = 0.0D;
                        if (counter > 0) {
                            replacementValue = sum / (double)counter;
                        }

                        Iterator var43 = clone.iterator();

                        while(var43.hasNext()) {
                            Example example = (Example)var43.next();
                            double value = example.getValue(newAttribute);
                            if (Double.isNaN(value)) {
                                example.setValue(newAttribute, replacementValue);
                            }
                        }
                    }
                }
            } catch (ExpressionException | ProcessStoppedException var24) {
                LogService.getRoot().log(Level.WARNING, "Cannot create feature: ", var24);
            }
        }
    }

    public Iterator<Feature> iterator() {
        return this.featureSet.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FeatureSet) {
            FeatureSet other = (FeatureSet)o;
            if (other.getNumberOfFeatures() != this.getNumberOfFeatures()) {
                return false;
            } else {
                Iterator var3 = this.iterator();

                Feature f;
                do {
                    if (!var3.hasNext()) {
                        var3 = other.iterator();

                        do {
                            if (!var3.hasNext()) {
                                return true;
                            }

                            f = (Feature)var3.next();
                        } while(this.contains(f));

                        return false;
                    }

                    f = (Feature)var3.next();
                } while(other.contains(f));

                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.getNormalizedString().hashCode();
    }

    public String getNormalizedString() {
        Set<String> sortedFeatureList = new TreeSet();
        Iterator var2 = this.featureSet.iterator();

        while(var2.hasNext()) {
            Feature f = (Feature)var2.next();
            sortedFeatureList.add(f.getName() + "_" + f.getExpression());
        }

        StringBuilder builder = new StringBuilder();
        Iterator var6 = sortedFeatureList.iterator();

        while(var6.hasNext()) {
            String f = (String)var6.next();
            builder.append(f);
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        Iterator var2 = this.featureSet.iterator();

        while(var2.hasNext()) {
            Feature f = (Feature)var2.next();
            result.append(f.toString()).append("\n");
        }

        return result.toString();
    }

    private static enum Type {
        NUMERICAL,
        NOMINAL,
        DATE;

        private Type() {
        }
    }
}
