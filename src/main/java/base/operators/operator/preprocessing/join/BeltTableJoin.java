package base.operators.operator.preprocessing.join;

import base.operators.adaption.belt.ContextAdapter;
import base.operators.adaption.belt.IOTable;
import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.ColumnTypes;
import base.operators.belt.column.Dictionary;
import base.operators.belt.execution.Context;
import base.operators.belt.execution.ExecutionAbortedException;
import base.operators.belt.execution.Workload;
import base.operators.belt.reader.MixedRow;
import base.operators.belt.reader.MixedRowReader;
import base.operators.belt.reader.NumericRow;
import base.operators.belt.reader.NumericRowReader;
import base.operators.belt.reader.ObjectRow;
import base.operators.belt.reader.Readers;
import base.operators.belt.table.BeltConverter;
import base.operators.belt.table.Builders;
import base.operators.belt.table.Table;
import base.operators.belt.table.TableBuilder;
import base.operators.belt.transform.RowTransformer;
import base.operators.belt.util.ColumnMetaData;
import base.operators.belt.util.ColumnReference;
import base.operators.belt.util.ColumnRole;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessSetupError;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.UserError;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;
import base.operators.operator.ports.metadata.ExampleSetUnionRule;
import base.operators.operator.ports.metadata.ParameterConditionedPrecondition;
import base.operators.operator.ports.metadata.SimpleMetaDataError;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeList;
import base.operators.parameter.conditions.BooleanParameterCondition;
import base.operators.studio.concurrency.internal.BackgroudOperatorConcurrencyContext;
import base.operators.studio.internal.Resources;
import base.operators.tools.Ontology;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.tools.container.Pair;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeltTableJoin
        extends Operator
{
    public static final String PARAMETER_JOIN_TYPE = "join_type";
    public static final String PARAMETER_LEFT_ATTRIBUTE_FOR_JOIN = "left_key_attributes";
    public static final String PARAMETER_RIGHT_ATTRIBUTE_FOR_JOIN = "right_key_attributes";
    public static final String PARAMETER_JOIN_ATTRIBUTES = "key_attributes";
    public static final String PARAMETER_USE_ID = "use_id_attribute_as_key";
    public static final String PARAMETER_KEEP_BOTH_JOIN_ATTRIBUTES = "keep_both_join_attributes";
    public static final String PARAMETER_REMOVE_DOUBLE_ATTRIBUTES = "remove_double_attributes";
    public static final String[] JOIN_TYPES = { "inner", "left", "right", "outer" };

    public static final int JOIN_TYPE_INNER = 0;

    public static final int JOIN_TYPE_LEFT = 1;

    public static final int JOIN_TYPE_RIGHT = 2;

    public static final int JOIN_TYPE_OUTER = 3;

    private static final String FROM_SECOND_SET = "_from_ES2";

    private static class DoubleArrayWrapper
    {
        private double[] data;

        private DoubleArrayWrapper(double[] data) { this.data = data; }

        @Override
        public boolean equals(Object other) { return (other instanceof DoubleArrayWrapper && equals(this.data, ((DoubleArrayWrapper)other).data)); }

        @Override
        public int hashCode() {
            if (this.data == null) {
                return 0;
            }
            int result = 1;
            for (double element : this.data) {
                long bits = Double.doubleToLongBits(element);

                if (bits != Float.MIN_VALUE) {
                    result = 31 * result + (int)(bits ^ bits >>> 32);
                }
            }
            return result;
        }

        @Override
        public String toString() { return Arrays.toString(this.data); }

        private static boolean equals(double[] a, double[] a2) {
            if (a == a2) {
                return true;
            }
            if (a == null || a2 == null) {
                return false;
            }

            int length = a.length;
            if (a2.length != length) {
                return false;
            }

            for (int i = 0; i < length; i++) {
                if (a[i] != a2[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final DoubleArrayWrapper NO_MATCH_KEY = new DoubleArrayWrapper(new double[] {0.0D/0.0 });

    private static final String LEFT_EXAMPLE_SET_INPUT = "left";

    private static final String RIGHT_EXAMPLE_SET_INPUT = "right";
    private InputPort leftInput = (InputPort)getInputPorts().createPort("left");
    private InputPort rightInput = (InputPort)getInputPorts().createPort("right");
    private OutputPort joinOutput = (OutputPort)getOutputPorts().createPort("join");
    private BackgroudOperatorConcurrencyContext context = new BackgroudOperatorConcurrencyContext(this);

    public BeltTableJoin(OperatorDescription description) {
        super(description);
        getTransformer().addRule(new ExampleSetUnionRule(this.leftInput, this.rightInput, this.joinOutput, "_from_ES2")
        {
            @Override
            protected String getPrefix()
            {
                return BeltTableJoin.this.getParameterAsBoolean("remove_double_attributes") ? null : "_from_ES2";
            }

            @Override
            protected ExampleSetMetaData modifyMetaData(ExampleSetMetaData leftEMD, ExampleSetMetaData rightEMD) {
                List<AttributeMetaData> joinedAttributesMetaData = BeltTableJoin.this.getUnionAttributesMetaData(leftEMD, rightEMD);
                ExampleSetMetaData joinedEMD = new ExampleSetMetaData();
                joinedEMD.addAllAttributes(joinedAttributesMetaData);
                return joinedEMD;
            }
        });
        this.leftInput.addPrecondition(new ParameterConditionedPrecondition(this.leftInput, new ExampleSetPrecondition(this.leftInput, 0, new String[] { "id" }), this, "use_id_attribute_as_key", "true"));
        this.leftInput.addPrecondition(new ParameterConditionedPrecondition(this.leftInput, new ExampleSetPrecondition(this.leftInput), this, "use_id_attribute_as_key", "false"));
        this.rightInput.addPrecondition(new ParameterConditionedPrecondition(this.rightInput, new ExampleSetPrecondition(this.rightInput, 0, new String[] { "id" }), this, "use_id_attribute_as_key", "true"));
        this.rightInput.addPrecondition(new ParameterConditionedPrecondition(this.rightInput, new ExampleSetPrecondition(this.rightInput), this, "use_id_attribute_as_key", "false"));
    }

    private List<AttributeMetaData> getUnionAttributesMetaData(ExampleSetMetaData leftMd, ExampleSetMetaData rightMd) {
        if (!this.leftInput.isConnected() || !this.rightInput.isConnected())
        {
            return Collections.emptyList();
        }
        if (isIdNeeded()) {
            AttributeMetaData id1 = leftMd.getSpecial("id");
            AttributeMetaData id2 = rightMd.getSpecial("id");

            if (id1 == null || id2 == null)
            {
                return Collections.emptyList();
            }
            if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(id1.getValueType(), id2.getValueType()) &&
                    !Ontology.ATTRIBUTE_VALUE_TYPE.isA(id2.getValueType(), id1.getValueType())) {
                addError(new SimpleProcessSetupError(ProcessSetupError.Severity.ERROR, getPortOwner(), "attributes_type_mismatch",
                        new Object[] { id1.getName(), "left", id2.getName(), "right" }));
                return Collections.emptyList();
            }
        }
        Set<String> rightKeyAttributes;
        try {
            rightKeyAttributes = this.getKeyAttributesMD(leftMd, rightMd);
        } catch (OperatorException e) {
            return Collections.emptyList();
        }

        Set<String> leftRoles = new HashSet<String>();
        Set<String> leftNames = new HashSet<String>();
        List<AttributeMetaData> unionAttributeList = new ArrayList<AttributeMetaData>();
        for (AttributeMetaData attributeMD : leftMd.getAllAttributes()) {
            leftNames.add(attributeMD.getName());
            unionAttributeList.add(attributeMD.clone());
            if (attributeMD.isSpecial()) {
                leftRoles.add(attributeMD.getRole());
            }
        }
        handleDoubleAttributes(leftNames, rightMd, unionAttributeList, leftRoles, rightKeyAttributes);
        return unionAttributeList;
    }

    private void handleDoubleAttributes(Set<String> leftNames, ExampleSetMetaData rightExampleSetMD, List<AttributeMetaData> unionList, Set<String> leftRoles, Set<String> rightKeyAttributes) {
        boolean removeDoubleAttributes = getParameterAsBoolean("remove_double_attributes");
        boolean keepBothKeyAttributes = getParameterAsBoolean("keep_both_join_attributes");
        Set<String> alreadyContained = leftNames;
        for (AttributeMetaData attribute : rightExampleSetMD.getAllAttributes()) {
            AttributeMetaData cloneAttribute = attribute.clone();
            if (rightKeyAttributes.contains(attribute.getName()) && keepBothKeyAttributes) {
                String role = attribute.getRole();
                if (role != null && leftRoles.contains(role))
                {
                    cloneAttribute.setRegular();
                }
                if (alreadyContained.contains(attribute.getName())) {
                    String newName = rename(attribute.getName(), alreadyContained);
                    cloneAttribute.setName(newName);
                    alreadyContained.add(newName);
                }
                unionList.add(cloneAttribute); continue;
            }
            boolean removed = false;
            if (alreadyContained.contains(attribute.getName())) {
                if (removeDoubleAttributes || attribute.getRole() != null) {

                    removed = true;
                } else {
                    String newName = rename(attribute.getName(), alreadyContained);
                    cloneAttribute.setName(newName);
                    alreadyContained.add(newName);
                }
            }
            if (!removed) {
                String role = attribute.getRole();
                if (role != null && leftRoles.contains(role)) {

                    this.rightInput.addError(
                            new SimpleMetaDataError(ProcessSetupError.Severity.WARNING, this.rightInput,
                                    "already_contains_role", new Object[] { attribute.getRole() }));
                    continue;
                }
                unionList.add(cloneAttribute);
            }
        }
    }

    private Set<String> getKeyAttributesMD(ExampleSetMetaData leftEMD, ExampleSetMetaData rightEMD) throws OperatorException {
        boolean useIdForJoin = getParameterAsBoolean("use_id_attribute_as_key");
        Set<String> rightKeyAttributes = new HashSet<String>();

        if (!useIdForJoin) {

            List<String[]> parKeyAttributes = getParameterList("key_attributes");
            int numKeyAttributes = parKeyAttributes.size();
            if (numKeyAttributes == 0) {
                addError(new SimpleProcessSetupError(ProcessSetupError.Severity.ERROR, getPortOwner(), "parameter.required_missing", new Object[] { "key attributes" }));

                throw new UserError(this, "join.no_key_attribute");
            }


            for (String[] attributePair : parKeyAttributes) {

                AttributeMetaData amdLeft = leftEMD.getAttributeByName(attributePair[0]);
                AttributeMetaData amdRight = rightEMD.getAttributeByName(attributePair[1]);


                if (amdLeft == null) {
                    this.leftInput.addError(new SimpleMetaDataError(ProcessSetupError.Severity.ERROR, this.leftInput, "missing_attribute", new Object[] { attributePair[0] }));

                    throw new UserError(this, "join.illegal_key_attribute", new Object[] { attributePair[0], "left", attributePair[1], "right" });
                }
                if (amdRight == null) {
                    this.rightInput.addError(new SimpleMetaDataError(ProcessSetupError.Severity.ERROR, this.rightInput, "missing_attribute", new Object[] { attributePair[1] }));

                    throw new UserError(this, "join.illegal_key_attribute", new Object[] { attributePair[1], "right", attributePair[0], "left" });
                }



                if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(amdLeft.getValueType(), amdRight.getValueType()) &&
                        !Ontology.ATTRIBUTE_VALUE_TYPE.isA(amdRight.getValueType(), amdLeft.getValueType())) {
                    addError(new SimpleProcessSetupError(ProcessSetupError.Severity.ERROR, getPortOwner(), "attributes_type_mismatch", new Object[] { attributePair[0], "left", attributePair[1], "right" }));

                    throw new UserError(this, "join.illegal_key_attribute", new Object[] { attributePair[1], "right", attributePair[0], "left" });
                }

                rightKeyAttributes.add(attributePair[1]);
            }
        } else {
            rightKeyAttributes.add(rightEMD.getSpecial("id").getName());
        }
        return rightKeyAttributes;
    }

    @Override
    public void doWork() throws OperatorException {
        IOTable leftTableObject = (IOTable)this.leftInput.getData(IOTable.class);
        Table leftTable = leftTableObject.getTable();
        Table rightTable = ((IOTable)this.rightInput.getData(IOTable.class)).getTable();

        Table joinTable = join(leftTable, rightTable);

        IOTable result = new IOTable(joinTable);
        result.getAnnotations().addAll(leftTableObject.getAnnotations());
        this.joinOutput.deliver(result);
    }

    private Pair<List<String>, List<String>> getIdColumns(Table leftTable, Table rightTable) throws UserError {
        List<String> leftIds = leftTable.select().withMetaData(ColumnRole.ID).labels();
        List<String> rightIds = rightTable.select().withMetaData(ColumnRole.ID).labels();

        if (leftIds.isEmpty() || rightIds.isEmpty()) {
            throw new UserError(this, '?');
        }

        if (leftIds.size() > 1 || rightIds.size() > 1) {
            throw new UserError(this, "join.id_key_attribute");
        }

        String leftIdLabel = (String)leftIds.get(0);
        String rightIdLabel = (String)rightIds.get(0);

        checkCompatibility(leftTable, leftIdLabel, rightTable, rightIdLabel);

        return new Pair(Collections.singletonList(leftIdLabel), Collections.singletonList(rightIdLabel));
    }

    private Table join(Table leftTable, Table rightTable) throws OperatorException {
        try {
            Pair<List<String>, List<String>> keyAttributes;
            if (isIdNeeded()) {
                keyAttributes = getIdColumns(leftTable, rightTable);
            } else {
                keyAttributes = getKeyAttributes(leftTable, rightTable);
            }
            Set<String> rightKeyAttributes = new HashSet<String>((Collection) keyAttributes.getSecond());

            List<String> leftWithRoles = leftTable.select().withMetaData(ColumnRole.class).labels();
            Set<String> leftRoles = new HashSet<String>();
            for (String leftLabel : leftWithRoles) {
                leftRoles.add(BeltConverter.convertRole(leftTable, leftLabel));
            }

            Table adjustedRightTable = handleDoubleAttributes(leftTable, rightTable, leftRoles, rightKeyAttributes);


            return joinData(leftTable, adjustedRightTable, keyAttributes);
        } catch (ExecutionAbortedException e) {
            throw new ProcessStoppedException(this);
        }
    }

    private Table handleDoubleAttributes(Table leftTable, Table rightTable, Set<String> leftRoles, Set<String> rightKeyAttributes) {
        boolean removeDoubleAttributes = getParameterAsBoolean("remove_double_attributes");
        TableBuilder rightBuilder = Builders.newTableBuilder(rightTable);

        Map<String, List<Pair<String, ColumnReference>>> rightBuilderReferences = calculateReferenceMap(rightTable);
        Set<String> alreadyContained = new HashSet<String>(leftTable.labels());

        for (String name : rightTable.labels()) {
            if (rightKeyAttributes.contains(name)) {
                String role = BeltConverter.convertRole(rightTable, name);
                if (role != null && leftRoles.contains(role))
                {
                    rightBuilder.removeMetaData(name, ColumnRole.class);
                }
                if (alreadyContained.contains(name)) {

                    String newName = rename(name, alreadyContained);
                    alreadyContained.add(newName);

                    updateColumnReference(rightBuilder, rightBuilderReferences, name, newName);
                }  continue;
            }
            boolean removed = false;
            if (alreadyContained.contains(name)) {
                if (removeDoubleAttributes || rightTable.getFirstMetaData(name, ColumnRole.class) != null) {

                    rightBuilder.remove(name);
                    removed = true;

                    updateColumnReference(rightBuilder, rightBuilderReferences, name, null);
                } else {

                    String newName = rename(name, alreadyContained);
                    alreadyContained.add(newName);

                    updateColumnReference(rightBuilder, rightBuilderReferences, name, newName);
                }
            }
            if (!removed) {
                String role = BeltConverter.convertRole(rightTable, name);
                if (role != null && leftRoles.contains(role)) {

                    rightBuilder.remove(name);
                    logWarning("Special attribute '" + role + "' already exist, skipping!");
                    updateColumnReference(rightBuilder, rightBuilderReferences, name, null);
                }
            }
        }

        return rightBuilder.build(ContextAdapter.adapt(this.context));
    }

    private Map<String, List<Pair<String, ColumnReference>>> calculateReferenceMap(Table rightTable) {
        Map<String, List<Pair<String, ColumnReference>>> rightBuilderReferences = new HashMap<String, List<Pair<String, ColumnReference>>>();
        for (String name : rightTable.labels()) {
            ColumnReference reference = (ColumnReference)rightTable.getFirstMetaData(name, ColumnReference.class);
            if (reference != null && reference.getColumn() != null) {
                ((List)rightBuilderReferences.computeIfAbsent(reference.getColumn(), k -> new ArrayList(1)))
                        .add(new Pair(name, reference));
            }
        }
        return rightBuilderReferences;
    }

    private void updateColumnReference(TableBuilder rightBuilder, Map<String, List<Pair<String, ColumnReference>>> rightBuilderReferences, String name, String newName) {
        List<Pair<String, ColumnReference>> pairList = (List)rightBuilderReferences.get(name);
        if (pairList != null) {
            for (Pair<String, ColumnReference> pair : pairList) {
                rightBuilder.removeMetaData((String)pair.getFirst(), (ColumnMetaData)pair.getSecond());
                rightBuilder.addMetaData((String)pair.getFirst(), new ColumnReference(newName, ((ColumnReference)pair
                        .getSecond()).getValue()));
            }
        }
    }

    private Table joinData(Table leftTable, Table rightTable, Pair<List<String>, List<String>> keyAttributes) throws OperatorException {
        int joinType = getParameterAsInt("join_type");
        boolean keepBothJoinAttributes = getParameterAsBoolean("keep_both_join_attributes");

        switch (joinType) {
            case 0:
                return performTableJoin(leftTable, rightTable, keyAttributes, keepBothJoinAttributes, true, false, false);
            case 1:
                return performTableJoin(leftTable, rightTable, keyAttributes, keepBothJoinAttributes, false, false, false);
            case 2:
                return performTableJoin(rightTable, leftTable, new Pair(keyAttributes
                        .getSecond(), keyAttributes.getFirst()), keepBothJoinAttributes, false, false, true);
            case 3:
                return performTableJoin(leftTable, rightTable, keyAttributes, keepBothJoinAttributes, false, true, false);
        }
        assert false;
        return null;
    }

    private Table performTableJoin(Table leftTable, Table rightTable, Pair<List<String>, List<String>> keyAttributes, boolean keepBothJoinAttributes, boolean inner, boolean fullOuter, boolean rightFirst) throws UserError {
        Pair<int[][], Integer> rowsAndIndex;
        List<String> leftKeyAttributeNames = (List)keyAttributes.getFirst();
        List<String> rightKeyAttributeNames = (List)keyAttributes.getSecond();

        Table leftKeyTable = leftTable.columns(leftKeyAttributeNames);
        Table rightKeyTable = rightTable.columns(rightKeyAttributeNames);

        Set<Integer> dateTimeColumns = findDateTimeColumns(leftKeyTable);


        if (!dateTimeColumns.isEmpty()) {

            Map<DoubleArrayWrapper, List<Integer>> rightKeyMapping = createDateTimeKeyMapping(rightKeyTable, leftKeyTable, dateTimeColumns, fullOuter);

            rowsAndIndex = getRowsWithDateTime(leftKeyTable, rightKeyMapping, dateTimeColumns, inner, fullOuter);
        }
        else {

            Map<DoubleArrayWrapper, List<Integer>> rightKeyMapping = createKeyMapping(rightKeyTable, leftKeyTable, fullOuter);

            rowsAndIndex = getRows(leftKeyTable, rightKeyMapping, inner, fullOuter);
        }

        int[][] rows = (int[][])rowsAndIndex.getFirst();

        Context context = ContextAdapter.adapt(this.context);
        Table leftMatchingTable = leftTable.rows(rows[0], context);
        Table rightMatchingTable = rightTable.rows(rows[1], context);

        Table firstTable = leftMatchingTable;
        Table secondTable = rightMatchingTable;
        if (rightFirst) {
            firstTable = rightMatchingTable;
            secondTable = leftMatchingTable;
        }

        if (!keepBothJoinAttributes) {

            List<String> toRemove = rightFirst ? leftKeyAttributeNames : rightKeyAttributeNames;
            TableBuilder builder = Builders.newTableBuilder(secondTable);
            for (String name : toRemove) {
                builder.remove(name);
            }
            secondTable = builder.build(context);
        }

        TableBuilder builder = Builders.newTableBuilder(firstTable);
        Set<String> alreadyAdded = new HashSet<String>(firstTable.labels());

        for (int i = 0; i < secondTable.width(); i++) {
            String label = secondTable.label(i);
            if (alreadyAdded.contains(label)) {
                label = rename(label, alreadyAdded);
            }
            builder.add(label, secondTable.column(i));
            builder.addMetaData(label, secondTable.getMetaData(secondTable.label(i)));
            alreadyAdded.add(label);
        }

        if (rightFirst && !keepBothJoinAttributes)
        {
            for (int i = 0; i < leftKeyAttributeNames.size(); i++) {
                builder.replace((String)rightKeyAttributeNames.get(i), leftMatchingTable.column((String)leftKeyAttributeNames.get(i)));
            }
        }

        if (fullOuter && !keepBothJoinAttributes && ((Integer)rowsAndIndex.getSecond()).intValue() < leftMatchingTable.height()) {
            replaceByNewColumns(leftMatchingTable, rightMatchingTable, leftKeyAttributeNames, rightKeyAttributeNames, builder, ((Integer)rowsAndIndex
                    .getSecond()).intValue(), context);
        }

        return builder.build(context);
    }

    private Set<Integer> findDateTimeColumns(Table leftKeyTable) {
        Set<Integer> notNumericReadable = new HashSet<Integer>();
        for (int i = 0; i < leftKeyTable.width(); i++) {
            if (leftKeyTable.column(i).type().equals(ColumnTypes.DATETIME)) {
                notNumericReadable.add(Integer.valueOf(i));
            }
        }
        return notNumericReadable;
    }

    private void replaceByNewColumns(Table leftMatchingTable, Table rightMatchingTable, List<String> leftKeyAttributeNames, List<String> rightKeyAttributeNames, TableBuilder builder, int firstOnlyRight, Context context) {
        for (int i = 0; i < leftKeyAttributeNames.size(); i++) {
            Column newColumn, leftColumn = leftMatchingTable.column((String)leftKeyAttributeNames.get(i));

            Column rightColumn = rightMatchingTable.column((String)rightKeyAttributeNames.get(i));

            RowTransformer transformer = (new RowTransformer(Arrays.asList(new Column[] { leftColumn, rightColumn }))).workload(Workload.SMALL);


            if (leftColumn.type().category() == Column.Category.CATEGORICAL) {
                newColumn = mergeCategorical(transformer, leftColumn.type(), firstOnlyRight, context);
            }
            else if (leftColumn.type().id() == Column.TypeId.TIME) {


                newColumn = transformer.applyObjectToTime(LocalTime.class, row -> (row.position() < firstOnlyRight) ? (LocalTime)row.get(0) : (LocalTime)row.get(1), context).toColumn();
            } else if (leftColumn.type().id() == Column.TypeId.DATE_TIME) {


                newColumn = transformer.applyObjectToDateTime(Instant.class, row -> (row.position() < firstOnlyRight) ? (Instant)row.get(0) : (Instant)row.get(1), context).toColumn();

            }
            else if (rightColumn.type().id() == Column.TypeId.INTEGER && leftColumn.type().id() == Column.TypeId.INTEGER) {

                newColumn = transformer.applyNumericToInteger(row -> (row.position() < firstOnlyRight) ? row.get(0) : row.get(1), context).toColumn();
            }
            else {
                newColumn = transformer.applyNumericToReal(row -> (row.position() < firstOnlyRight) ? row.get(0) : row.get(1), context).toColumn();
            }
            builder.replace((String)leftKeyAttributeNames.get(i), newColumn);
        }
    }

    private <T> Column mergeCategorical(RowTransformer transformer, ColumnType<T> type, int firstOnlyRight, Context context) {
        return transformer.applyObjectToCategorical(type.elementType(), row ->
                (row.position() < firstOnlyRight) ? row.get(0) : row.get(1), context)
                .toColumn(type);
    }

    private Map<DoubleArrayWrapper, List<Integer>> createKeyMapping(Table keyTable, Table remapKeyTable, boolean fullOuter) {
        Map<Integer, Map<Integer, Integer>> valueMapping = createCategoricalRemapping(keyTable, remapKeyTable);

        Map<DoubleArrayWrapper, List<Integer>> keyMapping = new HashMap<DoubleArrayWrapper, List<Integer>>();

        boolean hasRemapping = !valueMapping.isEmpty();
        NumericRowReader rowReader = Readers.numericRowReader(keyTable);
        int r = 0;
        while (rowReader.hasRemaining()) {
            rowReader.move();
            double[] key = getAsDoubleArray(rowReader, new double[rowReader.width()]);
            boolean remapComplete = true;
            if (hasRemapping) {
                remapComplete = remap(key, valueMapping);
            }
            storeDoubleArray(key, keyMapping, r, fullOuter, remapComplete);
            r++;
        }

        return keyMapping;
    }

    private boolean remap(double[] key, Map<Integer, Map<Integer, Integer>> valueMapping) {
        boolean remapComplete = true;
        for (int c = 0; c < key.length; c++) {
            Map<Integer, Integer> valueMap = (Map)valueMapping.get(Integer.valueOf(c));
            if (valueMap != null) {
                Integer remap = (Integer)valueMap.get(Integer.valueOf((int)key[c]));
                if (remap != null) {
                    if (remap.intValue() != 0) {
                        key[c] = remap.intValue();
                    }
                } else {
                    remapComplete = false;
                    break;
                }
            }
        }
        return remapComplete;
    }

    private Map<DoubleArrayWrapper, List<Integer>> createDateTimeKeyMapping(Table keyTable, Table remapKeyTable, Set<Integer> dateTimeColumns, boolean fullOuter) {
        Map<Integer, Map<Integer, Integer>> valueMapping = createCategoricalRemapping(keyTable, remapKeyTable);

        Map<DoubleArrayWrapper, List<Integer>> keyMapping = new HashMap<DoubleArrayWrapper, List<Integer>>();

        boolean hasRemapping = !valueMapping.isEmpty();

        int doubledCount = dateTimeColumns.size();
        int keySize = keyTable.width() + doubledCount;
        MixedRowReader rowReader = Readers.mixedRowReader(keyTable);
        int r = 0;
        while (rowReader.hasRemaining()) {
            rowReader.move();
            double[] key = getAsDoubleArray(rowReader, dateTimeColumns, new double[keySize]);

            boolean remapComplete = true;
            if (hasRemapping) {
                remapComplete = remapWithDateTime(key, valueMapping, keyTable.width(), dateTimeColumns);
            }
            storeDoubleArray(key, keyMapping, r, fullOuter, remapComplete);
            r++;
        }

        return keyMapping;
    }

    private boolean remapWithDateTime(double[] key, Map<Integer, Map<Integer, Integer>> valueMapping, int tableWidth, Set<Integer> dateTimeColumns) {
        boolean remapComplete = true;
        int keyIndex = 0;
        for (int columnIndex = 0; columnIndex < tableWidth; columnIndex++) {
            if (dateTimeColumns.contains(Integer.valueOf(columnIndex))) {
                keyIndex += 2;
            } else {
                Map<Integer, Integer> valueMap = (Map)valueMapping.get(Integer.valueOf(columnIndex));
                if (valueMap != null) {
                    Integer remap = (Integer)valueMap.get(Integer.valueOf((int)key[keyIndex]));
                    if (remap != null) {
                        if (remap.intValue() != 0) {
                            key[keyIndex] = remap.intValue();
                        }
                    } else {
                        remapComplete = false;
                        break;
                    }
                }
                keyIndex++;
            }
        }
        return remapComplete;
    }

    private double[] getAsDoubleArray(MixedRow row, Set<Integer> dateTimeColumns, double[] key) {
        int keyIndex = 0;
        for (int columnIndex = 0; columnIndex < row.width(); columnIndex++) {
            if (dateTimeColumns.contains(Integer.valueOf(columnIndex))) {


                Instant instant = (Instant)row.getObject(columnIndex);
                if (instant != null) {
                    long epochSecond = instant.getEpochSecond();
                    if (epochSecond < 0L) {
                        epochSecond += -4503599627370496L;
                    }
                    key[keyIndex++] = Double.longBitsToDouble(epochSecond);

                    key[keyIndex++] = instant.getNano();
                } else {
                    key[keyIndex++] = 0.0D / 0.0;
                    key[keyIndex++] = 0.0D / 0.0;
                }
            } else {

                key[keyIndex++] = row.getNumeric(columnIndex);
            }
        }
        return key;
    }

    private void storeDoubleArray(double[] key, Map<DoubleArrayWrapper, List<Integer>> keyMapping, int row, boolean fullOuter, boolean remapComplete) {
        if (remapComplete) {
            DoubleArrayWrapper wrappedKey = new DoubleArrayWrapper(key);
            List<Integer> indices = (List)keyMapping.get(wrappedKey);
            if (indices == null) {
                indices = new ArrayList<Integer>(1);
                keyMapping.put(wrappedKey, indices);
            }
            indices.add(Integer.valueOf(row));
        } else if (fullOuter) {

            List<Integer> indices = (List)keyMapping.get(NO_MATCH_KEY);
            if (indices == null) {
                indices = new ArrayList<Integer>(1);
                keyMapping.put(NO_MATCH_KEY, indices);
            }
            indices.add(Integer.valueOf(row));
        }
    }

    private Map<Integer, Map<Integer, Integer>> createCategoricalRemapping(Table keyTable, Table remapKeyTable) {
        Map<Integer, Map<Integer, Integer>> valueMapping = new HashMap<Integer, Map<Integer, Integer>>();
        for (int c = 0; c < keyTable.width(); c++) {
            Column keyColumn = keyTable.column(c);
            Column remapKeyColumn = remapKeyTable.column(c);

            if (keyColumn.type().category() == Column.Category.CATEGORICAL) {
                Dictionary<Object> indexToCategoryMap = keyColumn.getDictionary(Object.class);

                Map<Object, Integer> remapCategoryToIndexMap = remapKeyColumn.getDictionary(Object.class).createInverse();

                Map<Integer, Integer> valueMap = new HashMap<Integer, Integer>();
                for (int j = 0; j <= indexToCategoryMap.maximalIndex(); j++) {
                    Object category = indexToCategoryMap.get(j);
                    Integer remapIndex = (Integer)remapCategoryToIndexMap.get(category);
                    if (remapIndex != null) {
                        valueMap.put(Integer.valueOf(j), remapIndex);
                    }
                }

                valueMapping.put(Integer.valueOf(c), valueMap);
            }
        }
        return valueMapping;
    }

    private Pair<int[][], Integer> getRows(Table leftKeyTable, Map<DoubleArrayWrapper, List<Integer>> rightKeyMapping, boolean inner, boolean fullOuter) throws UserError {
        long matchingRowCount = 0L;
        int missingLeftCount = 0;
        List<Integer> missingRightIndices = new ArrayList<Integer>();
        Map<DoubleArrayWrapper, List<Integer>> rightCopy = null;
        if (fullOuter) {
            rightCopy = new HashMap<DoubleArrayWrapper, List<Integer>>(rightKeyMapping);
        }

        NumericRowReader leftReader = Readers.numericRowReader(leftKeyTable);
        while (leftReader.hasRemaining()) {
            leftReader.move();
            DoubleArrayWrapper wrappedKey = new DoubleArrayWrapper(getAsDoubleArray(leftReader, new double[leftReader.width()]));

            List<Integer> rightIndices = (List)rightKeyMapping.get(wrappedKey);
            if (fullOuter) {
                rightCopy.remove(wrappedKey);
            }
            if (rightIndices != null) {
                matchingRowCount += rightIndices.size(); continue;
            }
            missingLeftCount++;
        }

        if (fullOuter) {
            for (List<Integer> indices : rightCopy.values()) {
                missingRightIndices.addAll(indices);
            }
        }

        long total = matchingRowCount + (long) (fullOuter ? missingRightIndices.size() : 0) + (inner ? 0 : missingLeftCount);
        if (total > 2147483647L) {
            throw new UserError(this, "join_too_big");
        }
        int[][] rows = new int[2][(int)total];


        leftReader.setPosition(-1);
        int joinIndex = 0;
        int leftIndex = 0;
        while (leftReader.hasRemaining()) {
            leftReader.move();
            DoubleArrayWrapper wrappedKey = new DoubleArrayWrapper(getAsDoubleArray(leftReader, new double[leftReader.width()]));

            List<Integer> rightIndices = (List)rightKeyMapping.get(wrappedKey);
            if (rightIndices != null) {
                for (Iterator iterator = rightIndices.iterator(); iterator.hasNext(); ) { int rightIndex = ((Integer)iterator.next()).intValue();
                    rows[0][joinIndex] = leftIndex;
                    rows[1][joinIndex] = rightIndex;
                    joinIndex++; }

            } else if (!inner) {
                rows[0][joinIndex] = leftIndex;
                rows[1][joinIndex] = -1;
                joinIndex++;
            }
            leftIndex++;
        }
        int firstFromRight = joinIndex;
        if (fullOuter) {

            Collections.sort(missingRightIndices);
            for (Iterator iterator = missingRightIndices.iterator(); iterator.hasNext(); ) { int rightIndex = ((Integer)iterator.next()).intValue();
                rows[0][joinIndex] = -1;
                rows[1][joinIndex] = rightIndex;
                joinIndex++; }

        }

        return new Pair(rows, Integer.valueOf(firstFromRight));
    }

    private Pair<int[][], Integer> getRowsWithDateTime(Table leftKeyTable, Map<DoubleArrayWrapper, List<Integer>> rightKeyMapping, Set<Integer> dateTimeColumns, boolean inner, boolean fullOuter) throws UserError {
        long matchingRowCount = 0L;
        int missingLeftCount = 0;
        List<Integer> missingRightIndices = new ArrayList<Integer>();
        Map<DoubleArrayWrapper, List<Integer>> rightCopy = null;
        if (fullOuter) {
            rightCopy = new HashMap<DoubleArrayWrapper, List<Integer>>(rightKeyMapping);
        }

        int doubledCount = dateTimeColumns.size();
        int keySize = leftKeyTable.width() + doubledCount;
        MixedRowReader leftReader = Readers.mixedRowReader(leftKeyTable);

        while (leftReader.hasRemaining()) {
            leftReader.move();

            DoubleArrayWrapper wrappedKey = new DoubleArrayWrapper(getAsDoubleArray(leftReader, dateTimeColumns, new double[keySize]));
            List<Integer> rightIndices = (List)rightKeyMapping.get(wrappedKey);
            if (fullOuter) {
                rightCopy.remove(wrappedKey);
            }
            if (rightIndices != null) {
                matchingRowCount += rightIndices.size(); continue;
            }
            missingLeftCount++;
        }

        if (fullOuter) {
            for (List<Integer> indices : rightCopy.values()) {
                missingRightIndices.addAll(indices);
            }
        }

        long total = matchingRowCount + (long) (fullOuter ? missingRightIndices.size() : 0) + (inner ? 0 : missingLeftCount);
        if (total > 2147483647L) {
            throw new UserError(this, "join_too_big");
        }
        int[][] rows = new int[2][(int)total];


        leftReader.setPosition(-1);
        int joinIndex = 0;
        int leftIndex = 0;
        while (leftReader.hasRemaining()) {
            leftReader.move();

            DoubleArrayWrapper wrappedKey = new DoubleArrayWrapper(getAsDoubleArray(leftReader, dateTimeColumns, new double[keySize]));

            List<Integer> rightIndices = (List)rightKeyMapping.get(wrappedKey);
            if (rightIndices != null) {
                for (Iterator iterator = rightIndices.iterator(); iterator.hasNext(); ) { int rightIndex = ((Integer)iterator.next()).intValue();
                    rows[0][joinIndex] = leftIndex;
                    rows[1][joinIndex] = rightIndex;
                    joinIndex++; }

            } else if (!inner) {
                rows[0][joinIndex] = leftIndex;
                rows[1][joinIndex] = -1;
                joinIndex++;
            }
            leftIndex++;
        }
        int firstFromRight = joinIndex;
        if (fullOuter) {

            Collections.sort(missingRightIndices);
            for (Iterator iterator = missingRightIndices.iterator(); iterator.hasNext(); ) { int rightIndex = ((Integer)iterator.next()).intValue();
                rows[0][joinIndex] = -1;
                rows[1][joinIndex] = rightIndex;
                joinIndex++; }

        }

        return new Pair(rows, Integer.valueOf(firstFromRight));
    }

    private Pair<List<String>, List<String>> getKeyAttributes(Table leftTable, Table rightTable) throws UserError {
        List<String[]> parKeyAttributes = getParameterList("key_attributes");
        int numKeyAttributes = parKeyAttributes.size();
        if (numKeyAttributes == 0) {
            throw new UserError(this, "join.no_key_attributes");
        }
        Pair<List<String>, List<String>> keyAttributes = new Pair<List<String>, List<String>>(new ArrayList(numKeyAttributes), new ArrayList(numKeyAttributes));

        for (String[] attributePair : parKeyAttributes) {
            if (!leftTable.contains(attributePair[0])) {
                throw new UserError(this, "join.illegal_key_attribute", new Object[] { attributePair[0], "left", attributePair[1], "right" });
            }
            if (!rightTable.contains(attributePair[1])) {
                throw new UserError(this, "join.illegal_key_attribute", new Object[] { attributePair[1], "right", attributePair[0], "left" });
            }
            checkCompatibility(leftTable, attributePair[0], rightTable, attributePair[1]);

            ((List)keyAttributes.getFirst()).add(attributePair[0]);
            ((List)keyAttributes.getSecond()).add(attributePair[1]);
        }

        return keyAttributes;
    }

    private void checkCompatibility(Table leftTable, String leftName, Table rightTable, String rightName) throws UserError {
        Column.TypeId leftType = leftTable.column(leftName).type().id();
        Column.TypeId rightType = rightTable.column(rightName).type().id();
        if (leftType != rightType && (leftType != Column.TypeId.INTEGER || rightType != Column.TypeId.REAL) && (leftType != Column.TypeId.REAL || rightType != Column.TypeId.INTEGER))
        {
            throw new UserError(this, "join.illegal_key_attribute", new Object[] { rightName, "right", leftName, "left" });
        }

        if (leftType == Column.TypeId.CUSTOM) {
            if (!leftTable.column(leftName).type().equals(rightTable.column(rightName).type())) {
                throw new UserError(this, "join.illegal_key_attribute", new Object[] { rightName, "right", leftName, "left" });
            }

            if (leftTable.column(leftName).type().category() != Column.Category.CATEGORICAL) {
                throw new UserError(this, "join.unsupported_key_attribute", new Object[] { leftName });
            }
        }
    }

    private String rename(String old, Set<String> notAllowed) {
        String name = old;
        do {
            name = name + "_from_ES2";
        } while (notAllowed.contains(name));
        return name;
    }

    private double[] getAsDoubleArray(NumericRowReader rowReader, double[] values) {
        for (int i = 0; i < rowReader.width(); i++) {
            values[i] = rowReader.get(i);
        }
        return values;
    }

    private boolean isIdNeeded() { return getParameterAsBoolean("use_id_attribute_as_key"); }


    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeBoolean("remove_double_attributes", "Indicates if double attributes should be removed or renamed", true));

        types.add(new ParameterTypeCategory("join_type", "Specifies which join should be executed.", JOIN_TYPES, 0, false));

        types.add(new ParameterTypeBoolean("use_id_attribute_as_key", "Indicates if the id attribute is used for join.", false, false));

        ParameterTypeList parameterTypeList = new ParameterTypeList("key_attributes", "The attributes which shall be used for join. Attributes which shall be matched must be of the same type.", new ParameterTypeAttribute("left_key_attributes", "The attribute in the left example set to be used for the join.", (InputPort)getInputPorts().getPortByName("left"), true), new ParameterTypeAttribute("right_key_attributes", "The attribute in the left example set to be used for the join.", (InputPort)getInputPorts().getPortByName("right"), true), false);

        parameterTypeList.registerDependencyCondition(new BooleanParameterCondition(this, "use_id_attribute_as_key", true, false));

        types.add(parameterTypeList);

        ParameterTypeBoolean parameterTypeBoolean = new ParameterTypeBoolean("keep_both_join_attributes", "If checked, both columns of a join pair will be kept. Usually this is unneccessary since both attributes are identical.", false, true);

        types.add(parameterTypeBoolean);

        return types;
    }

    @Override
    public ResourceConsumptionEstimator getResourceConsumptionEstimator() { return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator((InputPort)getInputPorts().getPortByIndex(0), BeltTableJoin.class, null); }
}

