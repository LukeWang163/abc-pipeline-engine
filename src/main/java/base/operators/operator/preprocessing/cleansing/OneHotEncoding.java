package base.operators.operator.preprocessing.cleansing;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.preprocessing.PreprocessingModel;
import base.operators.operator.preprocessing.PreprocessingOperator;
import base.operators.operator.preprocessing.statistics.PreparationStatistics;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.tools.Ontology;
import base.operators.tools.Tools;
import base.operators.tools.math.container.Range;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OneHotEncoding extends PreprocessingOperator {
    public static final String PARAMETER_REMOVE_WITH_TOO_MANY_VALUES = "remove_with_too_many_values";
    public static final String PARAMETER_REMOVE_WITH_MORE_THAN = "maximum_number_of_values";
    public static final String PARAMETER_PERFORM_ENCODING = "perform_encoding";

    public OneHotEncoding(OperatorDescription description) {
        super(description);
    }

    @Override
    protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd) {
        if (!this.getParameterAsBoolean("perform_encoding")) {
            return Collections.singleton(amd);
        } else {
            Collection<AttributeMetaData> newAttribs = new LinkedList();
            Iterator var4 = amd.getValueSet().iterator();

            while(var4.hasNext()) {
                String value = (String)var4.next();
                AttributeMetaData newAttrib = new AttributeMetaData(getTargetAttributeName(amd.getName(), value), 4);
                double lowerBound = 0.0D;
                newAttrib.setValueRange(new Range(lowerBound, 1.0D), SetRelation.EQUAL);
                newAttribs.add(newAttrib);
            }

            return newAttribs;
        }
    }

    static String getTargetAttributeName(String sourceAttributeName, String value) {
        return sourceAttributeName + " = " + value;
    }

    private Map<String, Double> getAttributeTo1ValueMap(ExampleSet exampleSet, PreparationStatistics statistics, List<String> removeInAllCases) {
        Map<String, Double> attributeTo1ValueMap = new LinkedHashMap();
        LinkedList<Attribute> nominalAttributes = new LinkedList();
        Iterator var6 = exampleSet.getAttributes().iterator();

        while(var6.hasNext()) {
            Attribute attribute = (Attribute)var6.next();
            if (!removeInAllCases.contains(attribute.getName()) && attribute.isNominal() && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 5)) {
                nominalAttributes.add(attribute);
            }
        }

        Map<String, Double> sourceAttributeToComparisonGroupMap = this.getSourceAttributeToComparisonGroupMap(exampleSet, statistics, removeInAllCases);
        Iterator var13 = nominalAttributes.iterator();

        while(var13.hasNext()) {
            Attribute nominalAttribute = (Attribute)var13.next();
            double comparisonGroupValue = (Double)sourceAttributeToComparisonGroupMap.get(nominalAttribute.getName());

            for(int currentValue = 0; currentValue < nominalAttribute.getMapping().size(); ++currentValue) {
                if (!Tools.isEqual((double)currentValue, comparisonGroupValue)) {
                    attributeTo1ValueMap.put(getTargetAttributeName(nominalAttribute.getName(), nominalAttribute.getMapping().mapIndex(currentValue)), (double)currentValue);
                }
            }
        }

        return attributeTo1ValueMap;
    }

    private Map<String, Double> getSourceAttributeToComparisonGroupMap(ExampleSet exampleSet, PreparationStatistics statistics, List<String> removeInAllCases) {
        Map<String, Double> sourceAttributeToComparisonGroupMap = new LinkedHashMap();
        Iterator var5 = exampleSet.getAttributes().iterator();

        while(var5.hasNext()) {
            Attribute attribute = (Attribute)var5.next();
            if (!removeInAllCases.contains(attribute.getName()) && attribute.isNominal() && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 5)) {
                String leastValue = statistics.getNominalValueCountHandler().getLeast(attribute);
                double leastIndex = (double)attribute.getMapping().mapString(leastValue);
                sourceAttributeToComparisonGroupMap.put(attribute.getName(), leastIndex);
            }
        }

        return sourceAttributeToComparisonGroupMap;
    }

    @Override
    public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
        PreparationStatistics statistics = new PreparationStatistics(false);
        statistics.updateStatistics(exampleSet);
        List<String> removeInAllCases = new LinkedList();
        Iterator var4 = exampleSet.getAttributes().iterator();

        int maxValueCount;
        while(var4.hasNext()) {
            Attribute attribute = (Attribute)var4.next();
            if (attribute.isNominal() && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 5)) {
                maxValueCount = statistics.getMissingHandler().getMissing(attribute);
                if (maxValueCount == exampleSet.size()) {
                    removeInAllCases.add(attribute.getName());
                }
            }
        }

        boolean removeWithTooMany = this.getParameterAsBoolean("remove_with_too_many_values");
        List<String> attributesWithTooMany = new LinkedList();
        if (removeWithTooMany) {
            maxValueCount = this.getParameterAsInt("maximum_number_of_values");
            Iterator var7 = exampleSet.getAttributes().iterator();

            while(var7.hasNext()) {
                Attribute attribute = (Attribute)var7.next();
                if (!removeInAllCases.contains(attribute.getName()) && attribute.isNominal() && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), 5)) {
                    int numberOfValues = statistics.getNominalValueCountHandler().getValueCounts(attribute).size();
                    if (numberOfValues > maxValueCount) {
                        attributesWithTooMany.add(attribute.getName());
                    }
                }
            }
        }

        boolean performEncoding = this.getParameterAsBoolean("perform_encoding");
        Map<String, Double> sourceAttributeToComparisonGroupMap = this.getSourceAttributeToComparisonGroupMap(exampleSet, statistics, removeInAllCases);
        Map<String, Double> attributeTo1ValueMap = this.getAttributeTo1ValueMap(exampleSet, statistics, removeInAllCases);
        return new OneHotEncodingModel(exampleSet, sourceAttributeToComparisonGroupMap, attributeTo1ValueMap, removeWithTooMany, attributesWithTooMany, performEncoding, removeInAllCases);
    }

    @Override
    public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
        return OneHotEncodingModel.class;
    }

    @Override
    protected int[] getFilterValueTypes() {
        return new int[]{1};
    }

    @Override
    public boolean writesIntoExistingData() {
        return false;
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type = new ParameterTypeBoolean("remove_with_too_many_values", "Should nominal attributes with too many values be removed?", false);
        types.add(type);
        type = new ParameterTypeInt("maximum_number_of_values", "Attributes with more values than this will be removed from the example set.", 1, 2147483647, 100);
        type.registerDependencyCondition(new BooleanParameterCondition(this, "remove with too many values", false, true));
        types.add(type);
        type = new ParameterTypeBoolean("perform_encoding", "Indicates if the actual one-hot encoding should be performed or not.", true);
        types.add(type);
        return types;
    }

}
