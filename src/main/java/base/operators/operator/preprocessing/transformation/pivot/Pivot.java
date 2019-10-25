package base.operators.operator.preprocessing.transformation.pivot;


import base.operators.adaption.belt.ContextAdapter;
import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;
import base.operators.belt.column.Dictionary;
import base.operators.belt.column.Column.Category;
import base.operators.belt.column.Column.TypeId;
import base.operators.belt.execution.Context;
import base.operators.belt.reader.MixedRow;
import base.operators.belt.table.Builders;
import base.operators.belt.table.Table;
import base.operators.belt.table.TableBuilder;
import base.operators.belt.transform.RowTransformer;
import base.operators.core.concurrency.ConcurrencyContext;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationCollector;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationFunction;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationManager;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationTreeNode;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.AggregationTreeNode.AggregationTreeLeaf;
import base.operators.operator.preprocessing.transformation.pivot.aggregation.manager.AggregationManagers;
import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.studio.concurrency.internal.BackgroudOperatorConcurrencyContext;
import base.operators.studio.concurrency.internal.BackgroundConcurrencyContext;
import base.operators.studio.internal.Resources;
import base.operators.tools.Tools;
import base.operators.tools.container.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

class Pivot {
    static final String FUNCTION_OPEN = "(";
    static final String FUNCTION_CLOSE = ")";
    static final String SEPARATOR_AFTER = "_";
    private static final String SEPARATOR_CLOSE = ")_";
    private static final String SEPARATOR_OPEN = "(";
    private static final Object AGGREGATION_PLACEHOLDER = new Object();
    private static final Pivot.ReadMixedRowAs AS_OBJECT = MixedRow::getObject;
    private static final Pivot.ReadMixedRowAs AS_NUMERIC = MixedRow::getNumeric;
    private final Operator operator;
    private final Set<Object> columnsValues;

    Pivot(Operator operator, Set<Object> columnsValues) {
        this.operator = (Operator)Objects.requireNonNull(operator, "Operator must not be null");
        this.columnsValues = columnsValues;
    }

    Pair<Table, Set<Object>> pivot(Table table, String groupByColumns, String columnGroupingColumnName, List<String[]> aggregationFunctionPairs, boolean defaultAggregation, String defaultAggregationFunction) throws OperatorException {
        String[] groupByColumnNames = groupByColumns.split("\\|");
        List<String> allColumnNames = new ArrayList();
        Map<String, Integer> allAttributesToIndex = new HashMap();
        int numberOfGroupAttributes = this.addColumnAndRowAttributes(groupByColumnNames, columnGroupingColumnName, allColumnNames, allAttributesToIndex);
        Iterator var11 = allColumnNames.iterator();

        String name;
        do {
            if (!var11.hasNext()) {
                List<AggregationManager> aggregationFunctions = this.addAggregations(aggregationFunctionPairs, table, allColumnNames, allAttributesToIndex, defaultAggregation, defaultAggregationFunction);
                Context context;
                try {
                    this.operator.getProcess();
                    context = ContextAdapter.adapt(Resources.getConcurrencyContext(this.operator));
                } catch (NullPointerException e) {
                    context = ContextAdapter.adapt(new BackgroudOperatorConcurrencyContext(this.operator));
                }
                if (allColumnNames.isEmpty()) {
                    return new Pair(Builders.newTableBuilder(1).build(context), Collections.emptySet());
                }

                Column[] columns = new Column[allColumnNames.size()];
                boolean mixedRowReader = this.fillInAndCheckColumns(table, allColumnNames, columns);
                Supplier<List<AggregationFunction>> allSupplier = () -> {
                    List<AggregationFunction> functions = new ArrayList(aggregationFunctions.size());
                    Iterator var2 = aggregationFunctions.iterator();

                    while(var2.hasNext()) {
                        AggregationManager manager = (AggregationManager)var2.next();
                        functions.add(manager.newFunction());
                    }

                    return functions;
                };
                boolean withColumnGrouping = columnGroupingColumnName != null;
                AggregationTreeNode root;
                if (mixedRowReader) {
                    root = this.buildWithMixedRows(columns, numberOfGroupAttributes, allSupplier, withColumnGrouping, context);
                } else {
                    root = this.buildWithNumericRows(columns, numberOfGroupAttributes, allSupplier, withColumnGrouping, context);
                }

                int newLength;
                if (numberOfGroupAttributes == 1) {
                    newLength = root.size();
                } else if (numberOfGroupAttributes == 0) {
                    newLength = 1;
                } else {
                    newLength = AggregationTreeNode.countLength(root);
                }

                Map<Object, List<AggregationCollector>> columnGroupingValueToCollector = new HashMap();
                this.ensureNumberOfColumns(this.columnsValues, aggregationFunctions, columnGroupingValueToCollector, newLength);
                int[] mapping = new int[newLength];
                AggregationTreeNode.treeToData(root, columnGroupingValueToCollector, aggregationFunctions, mapping, 0);
                Table subtableOfGroupByColumns = table.columns(allColumnNames.subList(0, numberOfGroupAttributes));
                Column columnGroupingColumn = withColumnGrouping ? columns[numberOfGroupAttributes] : null;
                Table pivotTable = this.buildNewTable(subtableOfGroupByColumns, columnGroupingColumn, allColumnNames, aggregationFunctions, columnGroupingValueToCollector, mapping, context);
                return new Pair(pivotTable, columnGroupingValueToCollector.keySet());
            }

            name = (String)var11.next();
        } while(table.contains(name));

        throw new UserError(this.operator, "no_such_attribute", new Object[]{name});
    }

    private int addColumnAndRowAttributes(String[] groupByColumnNames, String columnGroupingColumnName, List<String> allAttributes, Map<String, Integer> allAttributesToIndex) {
        boolean emptySelection = groupByColumnNames.length == 1 && groupByColumnNames[0].trim().isEmpty();
        int numberOfGroupByColumns;
        if (!emptySelection) {
            int index = 0;
            String[] var8 = groupByColumnNames;
            int var9 = groupByColumnNames.length;

            for(int var10 = 0; var10 < var9; ++var10) {
                String attribute = var8[var10];
                if (!attribute.isEmpty() && !allAttributesToIndex.containsKey(attribute)) {
                    allAttributes.add(attribute);
                    allAttributesToIndex.put(attribute, index++);
                }
            }

            numberOfGroupByColumns = allAttributes.size();
        } else {
            numberOfGroupByColumns = 0;
        }

        if (columnGroupingColumnName != null) {
            allAttributes.add(columnGroupingColumnName);
            allAttributesToIndex.put(columnGroupingColumnName, allAttributes.size() - 1);
        }

        return numberOfGroupByColumns;
    }

    private List<AggregationManager> addAggregations(List<String[]> aggregationFunctionPairs, Table table, List<String> allAttributes, Map<String, Integer> allAttributesToIndex, boolean defaultAggregation, String defaultAggregationFunction) throws UserError {
        int aggregationsSize = aggregationFunctionPairs.size();
        if (aggregationsSize == 0 && !defaultAggregation) {
            return Collections.emptyList();
        } else {
            List<AggregationManager> aggregationFunctions = new ArrayList(aggregationsSize);
            Iterator var9 = aggregationFunctionPairs.iterator();

            while(var9.hasNext()) {
                String[] aggregationFunctionPair = (String[])var9.next();
                AggregationManager manager = this.initializeAggregationManager(aggregationFunctionPair, table, allAttributes, allAttributesToIndex);
                aggregationFunctions.add(manager);
            }

            if (defaultAggregation) {
                Supplier<AggregationManager> managerSupplier = (Supplier)AggregationManagers.INSTANCE.getAggregationManagers().get(defaultAggregationFunction);
                if (managerSupplier == null) {
                    throw new UserError(this.operator, "aggregation.illegal_function_name", new Object[]{defaultAggregationFunction});
                }

                Iterator var16 = table.labels().iterator();

                while(var16.hasNext()) {
                    String label = (String)var16.next();
                    if (!allAttributesToIndex.containsKey(label)) {
                        AggregationManager manager = (AggregationManager)managerSupplier.get();
                        Column column = table.column(label);
                        if (manager.checkColumnType(column.type()) != null) {
                            allAttributes.add(label);
                            int index = allAttributes.size() - 1;
                            allAttributesToIndex.put(label, index);
                            manager.initialize(column, index);
                            aggregationFunctions.add(manager);
                        }
                    }
                }
            }

            return aggregationFunctions;
        }
    }

    private AggregationManager initializeAggregationManager(String[] aggregationFunctionPair, Table table, List<String> allAttributes, Map<String, Integer> allAttributesToIndex) throws UserError {
        String aggregationAttributeName = aggregationFunctionPair[0];
        if (!table.contains(aggregationAttributeName)) {
            throw new UserError(this.operator, "aggregation.aggregation_attribute_not_present", new Object[]{aggregationAttributeName});
        } else {
            Integer index = (Integer)allAttributesToIndex.computeIfAbsent(aggregationAttributeName, (k) -> {
                allAttributes.add(k);
                return allAttributes.size() - 1;
            });
            Supplier<AggregationManager> managerSupplier = (Supplier)AggregationManagers.INSTANCE.getAggregationManagers().get(aggregationFunctionPair[1]);
            if (managerSupplier == null) {
                throw new UserError(this.operator, "aggregation.illegal_function_name", new Object[]{aggregationFunctionPair[1]});
            } else {
                AggregationManager manager = (AggregationManager)managerSupplier.get();
                Column column = table.column(aggregationAttributeName);
                if (manager.checkColumnType(column.type()) == null) {
                    throw new UserError(this.operator, "aggregation.incompatible_attribute_type", new Object[]{aggregationAttributeName, aggregationFunctionPair[1]});
                } else {
                    manager.initialize(column, index);
                    return manager;
                }
            }
        }
    }

    private boolean fillInAndCheckColumns(Table table, List<String> allColumnNames, Column[] columns) {
        boolean generalRowReader = false;
        int columnsArrayIndex = 0;
        Iterator var6 = allColumnNames.iterator();

        while(var6.hasNext()) {
            String label = (String)var6.next();
            Column column = table.column(label);
            columns[columnsArrayIndex++] = column;
            if (column.type().category() == Category.OBJECT) {
                generalRowReader = true;
            }
        }

        return generalRowReader;
    }

    private AggregationTreeNode buildWithMixedRows(Column[] columns, int numberOfGroupByColumns, Supplier<List<AggregationFunction>> allSupplier, boolean withColumnGrouping, Context context) {
        Pivot.ReadMixedRowAs[] readAs = new Pivot.ReadMixedRowAs[numberOfGroupByColumns + 1];
        int lastIndex = withColumnGrouping ? numberOfGroupByColumns + 1 : numberOfGroupByColumns;

        for(int i = 0; i < lastIndex; ++i) {
            if (columns[i].type().category() == Category.OBJECT) {
                readAs[i] = AS_OBJECT;
            } else {
                readAs[i] = AS_NUMERIC;
            }
        }

        if (withColumnGrouping) {
            return (AggregationTreeNode)(new RowTransformer(Arrays.asList(columns))).reduceMixed(AggregationTreeNode::new, (node, row) -> {
                AggregationTreeNode current = node;

                for(int i = 0; i < numberOfGroupByColumns; ++i) {
                    current = current.getOrCreateNext(readAs[i].read(row, i));
                }

                AggregationTreeLeaf leaf = current.getOrCreateLeaf(readAs[numberOfGroupByColumns].read(row, numberOfGroupByColumns), allSupplier, row.position());
                Iterator var7 = leaf.getFunctions().iterator();

                while(var7.hasNext()) {
                    AggregationFunction function = (AggregationFunction)var7.next();
                    function.accept(row);
                }

            }, AggregationTreeNode::merge, context);
        } else {
            return (AggregationTreeNode)(new RowTransformer(Arrays.asList(columns))).reduceMixed(AggregationTreeNode::new, (node, row) -> {
                AggregationTreeNode current = node;

                for(int i = 0; i < numberOfGroupByColumns; ++i) {
                    current = current.getOrCreateNext(readAs[i].read(row, i));
                }

                AggregationTreeLeaf leaf = current.getOrCreateLeaf(AGGREGATION_PLACEHOLDER, allSupplier, row.position());
                Iterator var7 = leaf.getFunctions().iterator();

                while(var7.hasNext()) {
                    AggregationFunction function = (AggregationFunction)var7.next();
                    function.accept(row);
                }

            }, AggregationTreeNode::merge, context);
        }
    }

    private AggregationTreeNode buildWithNumericRows(Column[] columns, int numberOfGroupByColumns, Supplier<List<AggregationFunction>> allSupplier, boolean withColumnGrouping, Context context) {
        return withColumnGrouping ? (AggregationTreeNode)(new RowTransformer(Arrays.asList(columns))).reduceNumeric(AggregationTreeNode::new, (node, row) -> {
            AggregationTreeNode current = node;

            for(int i = 0; i < numberOfGroupByColumns; ++i) {
                current = current.getOrCreateNext(row.get(i));
            }

            AggregationTreeLeaf leaf = current.getOrCreateLeaf(row.get(numberOfGroupByColumns), allSupplier, row.position());
            Iterator var6 = leaf.getFunctions().iterator();

            while(var6.hasNext()) {
                AggregationFunction function = (AggregationFunction)var6.next();
                function.accept(row);
            }

        }, AggregationTreeNode::merge, context) : (AggregationTreeNode)(new RowTransformer(Arrays.asList(columns))).reduceNumeric(AggregationTreeNode::new, (node, row) -> {
            AggregationTreeNode current = node;

            for(int i = 0; i < numberOfGroupByColumns; ++i) {
                current = current.getOrCreateNext(row.get(i));
            }

            AggregationTreeLeaf leaf = current.getOrCreateLeaf(AGGREGATION_PLACEHOLDER, allSupplier, row.position());
            Iterator var6 = leaf.getFunctions().iterator();

            while(var6.hasNext()) {
                AggregationFunction function = (AggregationFunction)var6.next();
                function.accept(row);
            }

        }, AggregationTreeNode::merge, context);
    }

    private void ensureNumberOfColumns(Set<Object> indexValues, List<AggregationManager> aggregationManagers, Map<Object, List<AggregationCollector>> columnGroupingValueToCollector, int newLength) {
        if (indexValues != null) {
            Iterator var5 = indexValues.iterator();

            while(var5.hasNext()) {
                Object key = var5.next();
                List<AggregationCollector> collectors = new ArrayList(aggregationManagers.size());
                Iterator var8 = aggregationManagers.iterator();

                while(var8.hasNext()) {
                    AggregationManager manager = (AggregationManager)var8.next();
                    collectors.add(manager.getCollector(newLength));
                }

                columnGroupingValueToCollector.put(key, collectors);
            }
        }

    }

    private Table buildNewTable(Table groupBySubTable, Column columnGroupingColumn, List<String> allAttributes, List<AggregationManager> aggregationFunctions, Map<Object, List<AggregationCollector>> columnGroupingValueToCollector, int[] mapping, Context context) {
        TableBuilder builder = Builders.newTableBuilder(mapping.length);
        Table mappedGroupByColumns = groupBySubTable.rows(mapping, context);

        for(int i = 0; i < mappedGroupByColumns.width(); ++i) {
            builder.add(mappedGroupByColumns.label(i), mappedGroupByColumns.column(i));
            builder.addMetaData(mappedGroupByColumns.label(i), mappedGroupByColumns.getMetaData(mappedGroupByColumns.label(i)));
        }

        if (columnGroupingColumn != null) {
            Category category = columnGroupingColumn.type().category();
            switch(category) {
                case CATEGORICAL:
                    this.addAggregationsSortedCategorical(columnGroupingColumn, columnGroupingColumn.type(), aggregationFunctions, columnGroupingValueToCollector, allAttributes, builder, context);
                    break;
                case NUMERIC:
                    this.addAggregationsSortedNumeric(columnGroupingColumn.type().id(), aggregationFunctions, columnGroupingValueToCollector, allAttributes, builder, context);
                    break;
                case OBJECT:
                default:
                    this.addAggregationsSortedObjects(columnGroupingColumn.type(), aggregationFunctions, columnGroupingValueToCollector, allAttributes, builder, context);
            }
        } else {
            this.addToBuilder(allAttributes, Collections.singletonList(AGGREGATION_PLACEHOLDER), aggregationFunctions, columnGroupingValueToCollector, (o) -> {
                return "";
            }, builder, context);
        }

        return builder.build(context);
    }

    private <T> void addAggregationsSortedCategorical(Column columnGroupingColumn, ColumnType<T> type, List<AggregationManager> aggregationFunctions, Map<Object, List<AggregationCollector>> columnGroupingValueToCollector, List<String> allAttributes, TableBuilder builder, Context context) {
        List<Object> objects = this.getDesiredColumns(columnGroupingValueToCollector);
        Comparator<T> comparator = type.comparator();
        Dictionary<T> dictionary = columnGroupingColumn.getDictionary(type.elementType());
        if (comparator != null) {
            objects.sort(Comparator.comparing((o) -> {
                return dictionary.get(((Double)o).intValue());
            }, Comparator.nullsLast(comparator)));
        } else {
            objects.sort(Comparator.comparing((o) -> {
                return Objects.toString(dictionary.get(((Double)o).intValue()));
            }, Comparator.nullsLast(String::compareTo)));
        }

        Function<Object, String> toStringFunction = (object) -> {
            return Objects.toString(dictionary.get(((Double)object).intValue()), "?");
        };
        this.addToBuilder(allAttributes, objects, aggregationFunctions, columnGroupingValueToCollector, toStringFunction, builder, context);
    }

    private void addAggregationsSortedNumeric(TypeId typeId, List<AggregationManager> aggregationFunctions, Map<Object, List<AggregationCollector>> columnGroupingValueToCollector, List<String> allAttributes, TableBuilder builder, Context context) {
        List<Object> objects = this.getDesiredColumns(columnGroupingValueToCollector);
        objects.sort(Comparator.comparing((o) -> {
            return (Double)o;
        }, Double::compare));
        boolean keepOldNames = this.operator.getCompatibilityLevel().isAtMost(PivotOperator.NON_FINITE_ATTRIBUTE_NAME_FIX);
        Function toStringFunction;
        if (typeId == TypeId.INTEGER) {
            if (keepOldNames) {
                toStringFunction = (object) -> {
                    return Tools.formatIntegerIfPossible((Double)object);
                };
            } else {
                toStringFunction = (object) -> {
                    double d = (Double)object;
                    return Double.isInfinite(d) ? Double.toString(d) : Tools.formatIntegerIfPossible(d);
                };
            }
        } else if (keepOldNames) {
            toStringFunction = (object) -> {
                return Objects.toString(object, "?");
            };
        } else {
            toStringFunction = (object) -> {
                return Double.isNaN((Double)object) ? "?" : Double.toString((Double)object);
            };
        }

        this.addToBuilder(allAttributes, objects, aggregationFunctions, columnGroupingValueToCollector, toStringFunction, builder, context);
    }

    private <T> void addAggregationsSortedObjects(ColumnType<T> type, List<AggregationManager> aggregationFunctions, Map<Object, List<AggregationCollector>> columnGroupingValueToCollector, List<String> allAttributes, TableBuilder builder, Context context) {
        List<Object> objects = this.getDesiredColumns(columnGroupingValueToCollector);
        Comparator<T> comparator = type.comparator();
        if (comparator != null) {
            objects.sort(Comparator.comparing(o -> (T) o, Comparator.nullsLast(comparator)));
        } else {
            objects.sort(Comparator.nullsLast(Comparator.comparing(Objects::toString, String::compareTo)));
        }

        Function<Object, String> toStringFunction = (object) -> {
            return Objects.toString(object, "?");
        };
        this.addToBuilder(allAttributes, objects, aggregationFunctions, columnGroupingValueToCollector, toStringFunction, builder, context);
    }

    private ArrayList<Object> getDesiredColumns(Map<Object, List<AggregationCollector>> columnGroupingValueToCollector) {
        return this.columnsValues == null ? new ArrayList(columnGroupingValueToCollector.keySet()) : new ArrayList(this.columnsValues);
    }

    private void addToBuilder(List<String> allAttributes, List<Object> objects, List<AggregationManager> aggregationFunctions, Map<Object, List<AggregationCollector>> columnGroupingValueToCollector, Function<Object, String> toStringFunction, TableBuilder builder, Context context) {
        for(int i = 0; i < aggregationFunctions.size(); ++i) {
            AggregationManager aggregationManager = (AggregationManager)aggregationFunctions.get(i);
            String aggregationName = aggregationManager.getAggregationName();
            int indexInAllAttributes = aggregationManager.getIndex();
            Iterator var12 = objects.iterator();

            while(var12.hasNext()) {
                Object object = var12.next();
                List collector;
                String label;
                if (object == AGGREGATION_PLACEHOLDER) {
                    collector = (List)columnGroupingValueToCollector.get(object);
                    if (collector != null) {
                        label = aggregationName + "(" + (String)allAttributes.get(indexInAllAttributes) + ")";
                        builder.add(label, ((AggregationCollector)collector.get(i)).getResult(context));
                    }
                } else {
                    collector = (List)columnGroupingValueToCollector.get(object);
                    if (collector != null) {
                        label = aggregationName + "(" + (String)allAttributes.get(indexInAllAttributes) + ")_" + (String)toStringFunction.apply(object);
                        builder.add(label, ((AggregationCollector)collector.get(i)).getResult(context));
                    }
                }
            }
        }

    }

    interface ReadMixedRowAs {
        Object read(MixedRow var1, int var2);
    }
}

