package base.operators.operator.preprocessing.transformation.pivot;

import base.operators.adaption.belt.IOTable;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.ColumnTypes;
import base.operators.belt.table.Table;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationManager;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.AggregationManagers;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.UserError;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.PassThroughRule;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.SimpleMetaDataError;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeAttributes;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.ParameterTypeStringCategory;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.tools.Ontology;
import base.operators.tools.Tools;
import base.operators.tools.container.Pair;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class PivotOperator extends Operator {
    public static final String PARAMETER_GROUP_ATTRIBUTES = "group_by_attributes";
    public static final String PARAMETER_COLUMN_ATTRIBUTE = "column_grouping_attribute";
    public static final String PARAMETER_AGGREGATION_ATTRIBUTES = "aggregation_attributes";
    public static final String PARAMETER_AGGREGATION_FUNCTION = "aggregation_function";
    public static final String PARAMETER_DEFAULT_AGGREGATION = "use_default_aggregation";
    public static final String PARAMETER_DEFAULT_AGGREGATION_FUNCTION = "default_aggregation_function";
    private static final String PARAMETER_AGGREGATION_ATTRIBUTE = "aggregation_attribute";
    private static final String ERROR_ATTRIBUTE_UNKNOWN = "aggregation.attribute_unknown";
    static final OperatorVersion NON_FINITE_ATTRIBUTE_NAME_FIX = new OperatorVersion(9, 2, 1);
    private final InputPort tableInput = (InputPort)this.getInputPorts().createPort("input");
    private final OutputPort tableOutput = (OutputPort)this.getOutputPorts().createPort("output");
    private final OutputPort originalOutput = (OutputPort)this.getOutputPorts().createPort("original");

    public PivotOperator(OperatorDescription description) {
        super(description);
        this.tableInput.addPrecondition(new AttributeSetPrecondition(this.tableInput, AttributeSetPrecondition.getAttributesByParameter(this, new String[]{"column_grouping_attribute"}), new String[0]));
        this.getTransformer().addRule(new PassThroughRule(this.tableInput, this.tableOutput, false) {
            public MetaData modifyMetaData(MetaData metaData) {
                if (metaData instanceof ExampleSetMetaData) {
                    try {
                        return PivotOperator.this.modifyMetaData((ExampleSetMetaData)metaData);
                    } catch (UserError var3) {
                        return new ExampleSetMetaData();
                    }
                } else {
                    return metaData;
                }
            }
        });
        this.getTransformer().addPassThroughRule(this.tableInput, this.originalOutput);
    }

    protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UserError {
        String groupByAttributesTogether = this.getParameterAsString("group_by_attributes");
        String columnGroupingAttribute = this.getParameterAsString("column_grouping_attribute");
        AttributeMetaData indexAttribute;
        if (columnGroupingAttribute != null && !columnGroupingAttribute.isEmpty()) {
            indexAttribute = metaData.getAttributeByName(columnGroupingAttribute);
            if (indexAttribute == null) {
                this.tableInput.addError(new SimpleMetaDataError(Severity.WARNING, this.tableInput, "aggregation.attribute_unknown", new Object[]{columnGroupingAttribute}));
                return new ExampleSetMetaData();
            }
        } else {
            indexAttribute = null;
        }

        ExampleSetMetaData emd = new ExampleSetMetaData();
        Set<AttributeMetaData> groupByAttributes = this.calculateNumberOfExamples(metaData, groupByAttributesTogether, emd);
        Iterator var7 = groupByAttributes.iterator();

        while(var7.hasNext()) {
            AttributeMetaData amd = (AttributeMetaData)var7.next();
            emd.addAttribute(amd.clone());
        }

        if (indexAttribute != null) {
            groupByAttributes.add(indexAttribute);
        }

        this.addNewAttributes(metaData, indexAttribute, emd, groupByAttributes);
        return emd;
    }

    private void addNewAttributes(ExampleSetMetaData originalMetaData, AttributeMetaData indexAttribute, ExampleSetMetaData emd, Set<AttributeMetaData> allAttributes) throws UserError {
        List<String[]> aggregationPairs = this.getParameterList("aggregation_attributes");
        if (aggregationPairs.isEmpty() && !this.getParameterAsBoolean("use_default_aggregation")) {
            this.addError(new SimpleProcessSetupError(Severity.WARNING, this.getPortOwner(), Collections.singletonList(new ParameterSettingQuickFix(this, "aggregation_attributes")), "pivot.no_aggregation_attributes", new Object[0]));
        }

        if (indexAttribute == null) {
            this.addAggregation(originalMetaData, aggregationPairs, emd, allAttributes);
        } else if (indexAttribute.isNominal()) {
            this.addNominal(originalMetaData, indexAttribute, aggregationPairs, emd, allAttributes);
        } else {
            this.addNonNominal(originalMetaData, aggregationPairs, indexAttribute, emd, allAttributes);
        }

    }

    private void addAggregation(ExampleSetMetaData originalMetaData, List<String[]> aggregationPairs, ExampleSetMetaData emd, Set<AttributeMetaData> allAttributes) throws UserError {
        Iterator var5 = aggregationPairs.iterator();

        AttributeMetaData amd;
        AttributeMetaData aggregatedMetaData;
        while(var5.hasNext()) {
            String[] pair = (String[])var5.next();
            amd = originalMetaData.getAttributeByName(pair[0]);
            allAttributes.add(amd);
            if (amd == null) {
                this.tableInput.addError(new SimpleMetaDataError(Severity.WARNING, this.tableInput, "aggregation.attribute_unknown", new Object[]{pair[0]}));
                amd = new AttributeMetaData(pair[0], 0);
            }

            aggregatedMetaData = this.getAttributeMetaData(pair[1], amd, true);
            aggregatedMetaData.getNumberOfMissingValues().increaseByUnknownAmount();
            emd.addAttribute(aggregatedMetaData);
        }

        if (this.getParameterAsBoolean("use_default_aggregation")) {
            String defaultAggregationFunction = this.getParameterAsString("default_aggregation_function");
            Iterator var10 = originalMetaData.getAllAttributes().iterator();

            while(var10.hasNext()) {
                amd = (AttributeMetaData)var10.next();
                if (!allAttributes.contains(amd)) {
                    aggregatedMetaData = this.getAttributeMetaData(defaultAggregationFunction, amd, false);
                    if (aggregatedMetaData != null) {
                        aggregatedMetaData.getNumberOfMissingValues().increaseByUnknownAmount();
                        emd.addAttribute(aggregatedMetaData);
                    }
                }
            }
        }

    }

    private void addNominal(ExampleSetMetaData originalMetaData, AttributeMetaData indexAttribute, List<String[]> aggregationPairs, ExampleSetMetaData emd, Set<AttributeMetaData> allAttributes) throws UserError {
        String[] pair;
        AttributeMetaData amd;
        for(Iterator var6 = aggregationPairs.iterator(); var6.hasNext(); this.addNewNominal(indexAttribute, emd, amd, pair[1], true)) {
            pair = (String[])var6.next();
            amd = originalMetaData.getAttributeByName(pair[0]);
            allAttributes.add(amd);
            if (amd == null) {
                this.tableInput.addError(new SimpleMetaDataError(Severity.WARNING, this.tableInput, "aggregation.attribute_unknown", new Object[]{pair[0]}));
                amd = new AttributeMetaData(pair[0], 0);
            }
        }

        if (this.getParameterAsBoolean("use_default_aggregation")) {
            String defaultAggregationFunction = this.getParameterAsString("default_aggregation_function");
            Iterator var10 = originalMetaData.getAllAttributes().iterator();

            while(var10.hasNext()) {
                amd = (AttributeMetaData)var10.next();
                if (!allAttributes.contains(amd)) {
                    this.addNewNominal(indexAttribute, emd, amd, defaultAggregationFunction, false);
                }
            }
        }

        emd.mergeSetRelation(indexAttribute.getValueSetRelation());
    }

    private void addNewNominal(AttributeMetaData indexAttribute, ExampleSetMetaData emd, AttributeMetaData amd, String aggregationFunction, boolean addIncompatible) throws UserError {
        AttributeMetaData aggregatedMetaData = this.getAttributeMetaData(aggregationFunction, amd, addIncompatible);
        if (aggregatedMetaData != null) {
            Set<String> valueSet = indexAttribute.getValueSet();
            if (valueSet.isEmpty()) {
                valueSet = Collections.singleton("?");
            }

            Iterator var8 = valueSet.iterator();

            while(var8.hasNext()) {
                String value = (String)var8.next();
                AttributeMetaData newIndexedAttribute = aggregatedMetaData.clone();
                newIndexedAttribute.setName(aggregatedMetaData.getName() + "_" + value);
                newIndexedAttribute.getNumberOfMissingValues().increaseByUnknownAmount();
                emd.addAttribute(newIndexedAttribute);
            }
        }

    }

    private void addNonNominal(ExampleSetMetaData originalMetaData, List<String[]> aggregationPairs, AttributeMetaData indexAttribute, ExampleSetMetaData emd, Set<AttributeMetaData> allAttributes) throws UserError {
        String[] pair;
        AttributeMetaData amd;
        for(Iterator var6 = aggregationPairs.iterator(); var6.hasNext(); this.addNewNonNominal(indexAttribute, emd, amd, pair[1], true)) {
            pair = (String[])var6.next();
            amd = originalMetaData.getAttributeByName(pair[0]);
            if (amd == null) {
                this.tableInput.addError(new SimpleMetaDataError(Severity.WARNING, this.tableInput, "aggregation.attribute_unknown", new Object[]{pair[0]}));
                amd = new AttributeMetaData(pair[0], 0);
            }
        }

        if (this.getParameterAsBoolean("use_default_aggregation")) {
            String defaultAggregationFunction = this.getParameterAsString("default_aggregation_function");
            Iterator var10 = originalMetaData.getAllAttributes().iterator();

            while(var10.hasNext()) {
                amd = (AttributeMetaData)var10.next();
                if (!allAttributes.contains(amd)) {
                    this.addNewNonNominal(indexAttribute, emd, amd, defaultAggregationFunction, false);
                }
            }
        }

        emd.mergeSetRelation(SetRelation.SUPERSET);
    }

    private void addNewNonNominal(AttributeMetaData indexAttribute, ExampleSetMetaData emd, AttributeMetaData amd, String aggregationFunction, boolean addIncompatible) throws UserError {
        AttributeMetaData aggregatedMetaData = this.getAttributeMetaData(aggregationFunction, amd, addIncompatible);
        if (aggregatedMetaData != null) {
            AttributeMetaData newIndexedAttribute = aggregatedMetaData.clone();
            String lowerString = this.getValueString(indexAttribute, indexAttribute.getValueRange().getLower());
            newIndexedAttribute.setName(newIndexedAttribute.getName() + "_" + lowerString);
            newIndexedAttribute.getNumberOfMissingValues().increaseByUnknownAmount();
            newIndexedAttribute.setValueSetRelation(SetRelation.SUBSET);
            emd.addAttribute(newIndexedAttribute);
            double upper = indexAttribute.getValueRange().getUpper();
            if (!indexAttribute.isDateTime() || !Double.isInfinite(indexAttribute.getValueRange().getLower()) || !Double.isInfinite(upper)) {
                String upperString = this.getValueString(indexAttribute, upper);
                aggregatedMetaData.setName(aggregatedMetaData.getName() + "_" + upperString);
                aggregatedMetaData.getNumberOfMissingValues().increaseByUnknownAmount();
                aggregatedMetaData.setValueSetRelation(SetRelation.SUBSET);
                emd.addAttribute(aggregatedMetaData);
            }
        }

    }

    private String getValueString(AttributeMetaData indexAttribute, double value) {
        String valueString;
        if (indexAttribute.isDateTime()) {
            if (Double.isInfinite(value) && Double.isInfinite(indexAttribute.getValueRange().getUpper())) {
                valueString = "?";
            } else {
                valueString = Instant.ofEpochMilli((long)value).toString();
            }
        } else if (indexAttribute.getValueType() == 3) {
            if (!this.getCompatibilityLevel().isAtMost(NON_FINITE_ATTRIBUTE_NAME_FIX) && Double.isInfinite(value)) {
                valueString = Double.toString(value);
            } else {
                valueString = Tools.formatIntegerIfPossible(value);
            }
        } else if (!this.getCompatibilityLevel().isAtMost(NON_FINITE_ATTRIBUTE_NAME_FIX) && Double.isNaN(value)) {
            valueString = Tools.formatIntegerIfPossible(value);
        } else {
            valueString = Double.toString(value);
        }

        return valueString;
    }

    private AttributeMetaData getAttributeMetaData(String aggregationFunctionName, AttributeMetaData sourceAttributeMetaData, boolean createIncompatible) throws UserError {
        Supplier<AggregationManager> function = (Supplier)AggregationManagers.INSTANCE.getAggregationManagers().get(aggregationFunctionName);
        if (function != null) {
            AggregationManager manager = (AggregationManager)function.get();
            ColumnType<?> resultType = manager.checkColumnType(getTypeForMetaData(sourceAttributeMetaData.getValueType()));
            int ontologyForType;
            if (resultType == null) {
                if (!createIncompatible) {
                    this.tableInput.addError(new SimpleMetaDataError(Severity.WARNING, this.tableInput, "pivot_aggregation.incompatible_value_type", new Object[]{sourceAttributeMetaData.getName(), aggregationFunctionName, Ontology.VALUE_TYPE_NAMES[sourceAttributeMetaData.getValueType()]}));
                    return null;
                }

                this.tableInput.addError(new SimpleMetaDataError(Severity.ERROR, this.tableInput, "pivot_aggregation.incompatible_value_type", new Object[]{sourceAttributeMetaData.getName(), aggregationFunctionName, Ontology.VALUE_TYPE_NAMES[sourceAttributeMetaData.getValueType()]}));
                ontologyForType = 0;
            } else {
                ontologyForType = getOntologyForType(resultType);
            }

            return new AttributeMetaData(aggregationFunctionName + "(" + sourceAttributeMetaData.getName() + ")", ontologyForType);
        } else {
            this.tableInput.addError(new SimpleMetaDataError(Severity.ERROR, this.tableInput, "aggregation.unknown_aggregation_function", new Object[]{aggregationFunctionName}));
            throw new UserError(this, "aggregation.illegal_function_name", new Object[]{aggregationFunctionName});
        }
    }

    private static ColumnType<?> getTypeForMetaData(int ontology) {
        switch(ontology) {
            case 2:
            case 4:
                return ColumnTypes.REAL;
            case 3:
                return ColumnTypes.INTEGER;
            case 5:
            case 6:
            case 7:
            case 8:
            default:
                return ColumnTypes.NOMINAL;
            case 9:
            case 10:
            case 11:
                return ColumnTypes.DATETIME;
        }
    }

    private static int getOntologyForType(ColumnType<?> type) {
        switch(type.id()) {
            case INTEGER:
                return 3;
            case REAL:
                return 4;
            case DATE_TIME:
                return 9;
            default:
                return 1;
        }
    }

    private Set<AttributeMetaData> calculateNumberOfExamples(ExampleSetMetaData originalMetaData, String groupByAttributesTogether, ExampleSetMetaData emd) {
        Set<AttributeMetaData> groupByAttributes = new LinkedHashSet();
        String[] groupByAttributeNames = groupByAttributesTogether.split("\\|");
        if (groupByAttributesTogether.length() != 0 && groupByAttributeNames.length != 0) {
            this.examplesWithGroupAttributes(originalMetaData, emd, groupByAttributes, groupByAttributeNames);
        } else {
            emd.setNumberOfExamples(1);
        }

        return groupByAttributes;
    }

    private void examplesWithGroupAttributes(ExampleSetMetaData originalMetaData, ExampleSetMetaData emd, Set<AttributeMetaData> groupByAttributes, String[] groupByAttributeNames) {
        long numberOfExamples = -1L;
        boolean stopMultiplying = false;
        String[] var8 = groupByAttributeNames;
        int var9 = groupByAttributeNames.length;

        for(int var10 = 0; var10 < var9; ++var10) {
            String name = var8[var10];
            if (!name.isEmpty()) {
                AttributeMetaData groupAttribute = this.getMetaData(originalMetaData, name, groupByAttributes);
                if (!stopMultiplying && groupAttribute != null) {
                    if (groupAttribute.isNominal()) {
                        int size = Math.max(groupAttribute.getValueSet().size(), 1);
                        if (numberOfExamples < 0L) {
                            numberOfExamples = (long)size;
                        } else {
                            numberOfExamples *= (long)size;
                        }

                        if (groupAttribute.getValueSetRelation() == SetRelation.SUPERSET) {
                            numberOfExamples = -1L;
                            stopMultiplying = true;
                        }
                    } else {
                        numberOfExamples = -1L;
                        stopMultiplying = true;
                    }
                }
            }
        }

        this.setNumberOfExamples(emd, numberOfExamples);
    }

    private AttributeMetaData getMetaData(ExampleSetMetaData originalMetaData, String name, Set<AttributeMetaData> groupByAttributes) {
        AttributeMetaData groupAttribute = originalMetaData.getAttributeByName(name);
        if (groupAttribute == null) {
            this.tableInput.addError(new SimpleMetaDataError(Severity.WARNING, this.tableInput, "aggregation.attribute_unknown", new Object[]{name}));
        } else {
            groupByAttributes.add(groupAttribute);
        }

        return groupAttribute;
    }

    private void setNumberOfExamples(ExampleSetMetaData emd, long numberOfExamples) {
        if (numberOfExamples > 0L) {
            if (numberOfExamples < 2147483647L) {
                emd.setNumberOfExamples((int)numberOfExamples);
            } else {
                emd.setNumberOfExamples(2147483647);
            }

            emd.getNumberOfExamples().reduceByUnknownAmount();
        } else {
            emd.setNumberOfExamples(new MDInteger());
            emd.getNumberOfExamples().increaseByUnknownAmount();
        }

    }

    public void doWork() throws OperatorException {
        IOTable wrapper = (IOTable)this.tableInput.getData(IOTable.class);
        Table table = wrapper.getTable();
        String groupAttributes = this.getParameterAsString("group_by_attributes");
        String columnAttribute = this.getParameterAsString("column_grouping_attribute");
        if (columnAttribute != null && columnAttribute.isEmpty()) {
            columnAttribute = null;
        }

        List<String[]> aggregationPairs = this.getParameterList("aggregation_attributes");
        boolean defaultAggregation = this.getParameterAsBoolean("use_default_aggregation");
        String defaultAggregationFunctionName = null;
        if (defaultAggregation) {
            defaultAggregationFunctionName = this.getParameterAsString("default_aggregation_function");
        }

        Pivot pivoter = new Pivot(this, (Set)null);
        Pair<Table, Set<Object>> result = pivoter.pivot(table, groupAttributes, columnAttribute, aggregationPairs, defaultAggregation, defaultAggregationFunctionName);
        Table resultTable = (Table)result.getFirst();
        this.tableOutput.deliver(new IOTable(resultTable));
        this.originalOutput.deliver(this.tableInput.getAnyDataOrNull());
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterTypeAttributes groupbyAttributes = new ParameterTypeAttributes("group_by_attributes", "Attributes that groups the examples which form one row after pivoting.", this.tableInput, true);
        groupbyAttributes.setExpert(false);
        types.add(groupbyAttributes);
        types.add(new ParameterTypeAttribute("column_grouping_attribute", "Attribute that decides the new attributes after the pivot.", this.tableInput, true, false));
        ParameterTypeList aggregationAttributes = new ParameterTypeList("aggregation_attributes", "The attributes which should be aggregated.", new ParameterTypeAttribute("aggregation_attribute", "Specifies the attribute which is aggregated.", this.tableInput, false), new ParameterTypeStringCategory("aggregation_function", "The type of the used aggregation function.", (String[])AggregationManagers.INSTANCE.getAggregationFunctionNames().toArray(new String[0]), "count", false), false);
        aggregationAttributes.setOptional(true);
        aggregationAttributes.setPrimary(true);
        types.add(aggregationAttributes);
        types.add(new ParameterTypeBoolean("use_default_aggregation", "If checked you can select a default aggregation function for a all attributes that are not used in the other parameters.", false, true));
        ParameterType type = new ParameterTypeStringCategory("default_aggregation_function", "The type of the used aggregation function for all default attributes.", (String[])AggregationManagers.INSTANCE.getAggregationFunctionNames().toArray(new String[0]), "first");
        type.registerDependencyCondition(new BooleanParameterCondition(this, "use_default_aggregation", false, true));
        type.setExpert(true);
        types.add(type);
        return types;
    }

    public OperatorVersion[] getIncompatibleVersionChanges() {
        OperatorVersion[] old = super.getIncompatibleVersionChanges();
        OperatorVersion[] versions = (OperatorVersion[])Arrays.copyOf(old, old.length + 1);
        versions[old.length] = NON_FINITE_ATTRIBUTE_NAME_FIX;
        return versions;
    }
}
