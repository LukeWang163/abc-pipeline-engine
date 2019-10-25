package base.operators.operator.preprocessing.cleansing;

import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.SimpleAttributes;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.NominalMapping;
import base.operators.example.table.ViewAttribute;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorProgress;
import base.operators.operator.preprocessing.PreprocessingModel;
import base.operators.tools.Ontology;
import base.operators.tools.Tools;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class OneHotEncodingModel extends PreprocessingModel {
    private static final long serialVersionUID = -4203775081616082145L;
    private boolean removeWithTooMany;
    private List<String> attributesWithTooMany;
    private List<String> removeInAllCases;
    private boolean performEncoding;
    private Map<String, Double> attributeTo1ValueMap;
    private Map<String, List<String>> attributeToAllNominalValues;
    private Map<String, Double> sourceAttributeToComparisonGroupMap;
    private Map<String, String> sourceAttributeToComparisonGroupStringsMap;

    public OneHotEncodingModel(ExampleSet exampleSet) {
        super(exampleSet);
        this.removeWithTooMany = false;
        this.attributesWithTooMany = new LinkedList();
        this.removeInAllCases = new LinkedList();
        this.performEncoding = true;
        this.attributeTo1ValueMap = null;
        this.attributeToAllNominalValues = null;
        this.sourceAttributeToComparisonGroupMap = null;
        this.sourceAttributeToComparisonGroupStringsMap = null;
    }

    public OneHotEncodingModel(ExampleSet exampleSet, Map<String, Double> sourceAttributeToComparisonGroupMap, Map<String, Double> attributeTo1ValueMap, boolean removeWithTooMany, List<String> attributesWithTooMany, boolean performEncoding, List<String> removeInAllCases) {
        this(exampleSet);
        this.sourceAttributeToComparisonGroupMap = sourceAttributeToComparisonGroupMap;
        this.attributeTo1ValueMap = attributeTo1ValueMap;
        this.removeWithTooMany = removeWithTooMany;
        this.attributesWithTooMany = attributesWithTooMany;
        this.performEncoding = performEncoding;
        if (this.attributesWithTooMany == null) {
            this.attributesWithTooMany = new LinkedList();
        }

        this.removeInAllCases = removeInAllCases;
        if (this.removeInAllCases == null) {
            this.removeInAllCases = new LinkedList();
        }

        Iterator var8;
        if (removeWithTooMany) {
            var8 = attributesWithTooMany.iterator();

            while(var8.hasNext()) {
                String name = (String)var8.next();
                this.sourceAttributeToComparisonGroupMap.remove(name);
                this.attributeTo1ValueMap.remove(name);
            }
        }

        this.sourceAttributeToComparisonGroupStringsMap = new LinkedHashMap();
        var8 = sourceAttributeToComparisonGroupMap.entrySet().iterator();

        while(true) {
            String attributeName;
            double comparisonGroup;
            do {
                if (!var8.hasNext()) {
                    this.attributeToAllNominalValues = new HashMap();
                    var8 = exampleSet.getAttributes().iterator();

                    while(var8.hasNext()) {
                        Attribute attribute = (Attribute)var8.next();
                        if (attribute.isNominal() && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 5)) {
                            attributeName = attribute.getName();
                            List<String> values = new LinkedList();
                            values.addAll(attribute.getMapping().getValues());
                            this.attributeToAllNominalValues.put(attributeName, values);
                        }
                    }

                    return;
                }

                Entry<String, Double> entry = (Entry)var8.next();
                attributeName = (String)entry.getKey();
                comparisonGroup = (Double)entry.getValue();
            } while(removeInAllCases != null && removeInAllCases.contains(attributeName));

            Attribute attribute = exampleSet.getAttributes().get(attributeName);
            String comparisonGroupString = attribute.getMapping().mapIndex((int)comparisonGroup);
            this.sourceAttributeToComparisonGroupStringsMap.put(attributeName, comparisonGroupString);
        }
    }

    @Override
    public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {
        Iterator var2 = this.removeInAllCases.iterator();

        String name;
        Attribute attribute;
        while(var2.hasNext()) {
            name = (String)var2.next();
            attribute = exampleSet.getAttributes().get(name);
            if (attribute != null) {
                exampleSet.getAttributes().remove(attribute);
            }
        }

        if (this.removeWithTooMany) {
            var2 = this.attributesWithTooMany.iterator();

            while(var2.hasNext()) {
                name = (String)var2.next();
                attribute = exampleSet.getAttributes().get(name);
                if (attribute != null) {
                    exampleSet.getAttributes().remove(attribute);
                }
            }
        }

        if (this.performEncoding) {
            List<Attribute> nominalAttributes = new ArrayList();
            List<Attribute> transformedAttributes = new ArrayList();
            Map<Attribute, List<Attribute>> targetAttributesFromSources = new HashMap();
            Iterator var5 = exampleSet.getAttributes().iterator();

            label101:
            while(true) {
                Attribute nominalAttribute;
                do {
                    do {
                        if (!var5.hasNext()) {
                            exampleSet.getExampleTable().addAttributes(transformedAttributes);
                            var5 = transformedAttributes.iterator();

                            while(var5.hasNext()) {
                                attribute = (Attribute)var5.next();
                                exampleSet.getAttributes().addRegular(attribute);
                            }

                            long progressCompletedCounter = 0L;
                            long progressTotal = (long)nominalAttributes.size() * (long)exampleSet.size();
                            OperatorProgress progress = null;
                            if (this.getShowProgress() && this.getOperator() != null && this.getOperator().getProgress() != null) {
                                progress = this.getOperator().getProgress();
                                progress.setTotal(1000);
                            }

                            Iterator var24 = nominalAttributes.iterator();

                            while(var24.hasNext()) {
                                nominalAttribute = (Attribute)var24.next();
                                Iterator var12 = exampleSet.iterator();

                                while(var12.hasNext()) {
                                    Example example = (Example)var12.next();
                                    double sourceValue = example.getValue(nominalAttribute);
                                    Iterator var16 = ((List)targetAttributesFromSources.get(nominalAttribute)).iterator();

                                    while(var16.hasNext()) {
                                        Attribute targetAttribute = (Attribute)var16.next();
                                        example.setValue(targetAttribute, this.getValue(targetAttribute, sourceValue));
                                    }

                                    if (progress != null && ++progressCompletedCounter % 10000L == 0L) {
                                        progress.setCompleted((int)(1000.0D * (double)progressCompletedCounter / (double)progressTotal));
                                    }
                                }
                            }

                            var24 = nominalAttributes.iterator();

                            while(var24.hasNext()) {
                                nominalAttribute = (Attribute)var24.next();
                                exampleSet.getAttributes().remove(nominalAttribute);
                            }
                            break label101;
                        }

                        attribute = (Attribute)var5.next();
                    } while(!attribute.isNominal());
                } while(Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 5));

                nominalAttributes.add(attribute);
                List<String> targetNames = this.getTargetAttributesFromSourceAttribute(attribute);
                List<Attribute> targets = new ArrayList();
                Iterator var9 = targetNames.iterator();

                while(var9.hasNext()) {
                    String targetName = (String)var9.next();
                    nominalAttribute = AttributeFactory.createAttribute(targetName, 4);
                    transformedAttributes.add(nominalAttribute);
                    targets.add(nominalAttribute);
                }

                targetAttributesFromSources.put(attribute, targets);
            }
        }

        return exampleSet;
    }

    private List<String> getTargetAttributesFromSourceAttribute(Attribute sourceAttribute) {
        List<String> targetNames = new ArrayList();
        double comparisonGroup = (Double)this.sourceAttributeToComparisonGroupMap.get(sourceAttribute.getName());
        List<String> originalAttributeValues = (List)this.attributeToAllNominalValues.get(sourceAttribute.getName());
        String comparisonGroupValue = null;
        if (!Tools.isEqual(comparisonGroup, -1.0D)) {
            comparisonGroupValue = (String)originalAttributeValues.get((int)comparisonGroup);
        }

        Iterator var7 = originalAttributeValues.iterator();

        while(var7.hasNext()) {
            String currentValue = (String)var7.next();
            if (!currentValue.equals(comparisonGroupValue)) {
                targetNames.add(OneHotEncoding.getTargetAttributeName(sourceAttribute.getName(), currentValue));
            }
        }

        return targetNames;
    }

    @Override
    public Attributes getTargetAttributes(ExampleSet parentSet) {
        SimpleAttributes attributes = new SimpleAttributes();
        Iterator specialRoles = parentSet.getAttributes().specialAttributes();

        while(specialRoles.hasNext()) {
            attributes.add((AttributeRole)specialRoles.next());
        }

        Iterator var4 = parentSet.getAttributes().iterator();

        while(true) {
            Attribute attribute;
            double comparisonGroup;
            List valueList;
            do {
                while(true) {
                    do {
                        label37:
                        do {
                            while(var4.hasNext()) {
                                attribute = (Attribute)var4.next();
                                if (attribute.isNominal() && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 5)) {
                                    continue label37;
                                }

                                attributes.addRegular(attribute);
                            }

                            return attributes;
                        } while(this.attributesWithTooMany.contains(attribute.getName()));
                    } while(this.removeInAllCases.contains(attribute.getName()));

                    if (this.performEncoding) {
                        comparisonGroup = (Double)this.sourceAttributeToComparisonGroupMap.get(attribute.getName());
                        valueList = (List)this.attributeToAllNominalValues.get(attribute.getName());
                        break;
                    }

                    attributes.addRegular(attribute);
                }
            } while(valueList == null);

            int currentValue = 0;

            for(Iterator var10 = valueList.iterator(); var10.hasNext(); ++currentValue) {
                String attributeValue = (String)var10.next();
                if (!Tools.isEqual((double)currentValue, comparisonGroup)) {
                    ViewAttribute viewAttribute = new ViewAttribute(this, attribute, OneHotEncoding.getTargetAttributeName(attribute.getName(), attributeValue), 4, (NominalMapping)null);
                    attributes.addRegular(viewAttribute);
                }
            }
        }
    }

    @Override
    public double getValue(Attribute targetAttribute, double value) {
        String targetName = targetAttribute.getName();
        Double oneValue = (Double)this.attributeTo1ValueMap.get(targetName);
        return oneValue != null && Tools.isEqual(oneValue, value) ? 1.0D : 0.0D;
    }

    @Override
    public String getName() {
        return "One-Hot Encoding Model";
    }

    @Override
    public String toResultString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getName()).append(Tools.getLineSeparators(2));
        Iterator var2;
        String name;
        if (this.removeWithTooMany) {
            builder.append(this.attributesWithTooMany.size()).append(" attributes will be removed because of too many values.").append(Tools.getLineSeparators(2));
            var2 = this.attributesWithTooMany.iterator();

            while(var2.hasNext()) {
                name = (String)var2.next();
                builder.append(" - ").append(name).append(Tools.getLineSeparator());
            }
        }

        builder.append(Tools.getLineSeparator());
        builder.append("Perform Encoding: ").append(this.performEncoding ? "yes" : "no").append(Tools.getLineSeparators(2));
        if (this.performEncoding) {
            builder.append("Model covering ").append(this.attributeTo1ValueMap.size()).append(" attributes (with comparison group):").append(Tools.getLineSeparator());
            var2 = this.attributeTo1ValueMap.keySet().iterator();

            while(var2.hasNext()) {
                name = (String)var2.next();
                if (!this.attributesWithTooMany.contains(name) && !this.removeInAllCases.contains(name)) {
                    builder.append(" - ").append(name).append(" ('").append((String)this.sourceAttributeToComparisonGroupStringsMap.get(name)).append("')").append(Tools.getLineSeparator());
                }
            }
        }

        return builder.toString();
    }
}
