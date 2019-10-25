package base.operators.operator.preprocessing.transformation.pivot.aggregation;

import base.operators.tools.container.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

public class AggregationTreeNode {
    private Map<Object, AggregationTreeNode> nextMap = null;
    private Pair<Integer, Map<Object, AggregationTreeNode.AggregationTreeLeaf>> leafMapPair = null;

    public AggregationTreeNode() {
    }

    public AggregationTreeNode getOrCreateNext(Object value) {
        if (this.nextMap == null) {
            this.nextMap = new LinkedHashMap();
        }

        return (AggregationTreeNode)this.nextMap.computeIfAbsent(value, (k) -> {
            return new AggregationTreeNode();
        });
    }

    public AggregationTreeNode.AggregationTreeLeaf getOrCreateLeaf(Object value, Supplier<List<AggregationFunction>> functions, int rowIndex) {
        if (this.leafMapPair == null) {
            this.leafMapPair = new Pair(rowIndex, new HashMap());
        }

        AggregationTreeNode.AggregationTreeLeaf leaf = (AggregationTreeNode.AggregationTreeLeaf)((Map)this.leafMapPair.getSecond()).get(value);
        if (leaf == null) {
            leaf = new AggregationTreeNode.AggregationTreeLeaf((List)functions.get());
            ((Map)this.leafMapPair.getSecond()).put(value, leaf);
        }

        return leaf;
    }

    public int size() {
        return this.nextMap == null ? 0 : this.nextMap.size();
    }

    public static int countLength(AggregationTreeNode node) {
        int count = 0;
        AggregationTreeNode next;
        if (node.nextMap != null) {
            for(Iterator var2 = node.nextMap.values().iterator(); var2.hasNext(); count += countLength(next)) {
                next = (AggregationTreeNode)var2.next();
            }
        } else {
            ++count;
        }

        return count;
    }

    public static void merge(AggregationTreeNode node1, AggregationTreeNode node2) {
        if (node2.nextMap != null) {
            Iterator var2 = node2.nextMap.entrySet().iterator();

            while(var2.hasNext()) {
                Entry<Object, AggregationTreeNode> entry = (Entry)var2.next();
                AggregationTreeNode from1 = (AggregationTreeNode)node1.nextMap.get(entry.getKey());
                if (from1 == null) {
                    node1.nextMap.put(entry.getKey(), entry.getValue());
                } else {
                    merge(from1, (AggregationTreeNode)entry.getValue());
                }
            }
        } else {
            Map<Object, AggregationTreeNode.AggregationTreeLeaf> from1 = (Map)node1.leafMapPair.getSecond();
            mergeLeaves(from1, (Map)node2.leafMapPair.getSecond());
        }

    }

    private static void mergeLeaves(Map<Object, AggregationTreeNode.AggregationTreeLeaf> from1, Map<Object, AggregationTreeNode.AggregationTreeLeaf> from2) {
        Iterator var2 = from2.entrySet().iterator();

        while(true) {
            while(var2.hasNext()) {
                Entry<Object, AggregationTreeNode.AggregationTreeLeaf> entry = (Entry)var2.next();
                AggregationTreeNode.AggregationTreeLeaf leaf1 = (AggregationTreeNode.AggregationTreeLeaf)from1.get(entry.getKey());
                if (leaf1 == null) {
                    from1.put(entry.getKey(), entry.getValue());
                } else {
                    List<AggregationFunction> list1 = leaf1.getFunctions();
                    List<AggregationFunction> list2 = ((AggregationTreeNode.AggregationTreeLeaf)entry.getValue()).getFunctions();
                    int functionIndex = 0;
                    Iterator var8 = list2.iterator();

                    while(var8.hasNext()) {
                        AggregationFunction function = (AggregationFunction)var8.next();
                        ((AggregationFunction)list1.get(functionIndex++)).merge(function);
                    }
                }
            }

            return;
        }
    }

    public static int treeToData(AggregationTreeNode node, Map<Object, List<AggregationCollector>> indexValueToCollector, List<AggregationManager> managers, int[] mapping, int index) {
        int lastIndex = index;
        Iterator var6;
        AggregationTreeNode nextNode;
        if (node.nextMap != null) {
            for(var6 = node.nextMap.values().iterator(); var6.hasNext(); lastIndex = treeToData(nextNode, indexValueToCollector, managers, mapping, lastIndex)) {
                nextNode = (AggregationTreeNode)var6.next();
            }
        } else if (node.leafMapPair != null) {
            mapping[index] = (Integer)node.leafMapPair.getFirst();
            var6 = ((Map)node.leafMapPair.getSecond()).entrySet().iterator();

            while(var6.hasNext()) {
                Entry<Object, AggregationTreeNode.AggregationTreeLeaf> entry = (Entry)var6.next();
                Object key = entry.getKey();
                AggregationTreeNode.AggregationTreeLeaf leaf = (AggregationTreeNode.AggregationTreeLeaf)entry.getValue();
                List<AggregationCollector> collectors = (List)indexValueToCollector.computeIfAbsent(key, (k) -> {
                    return createCollectors(managers, mapping.length);
                });
                int functionIndex = 0;
                Iterator var12 = leaf.functions.iterator();

                while(var12.hasNext()) {
                    AggregationFunction function = (AggregationFunction)var12.next();
                    ((AggregationCollector)collectors.get(functionIndex++)).set(lastIndex, function);
                }
            }

            ++lastIndex;
        }

        return lastIndex;
    }

    private static List<AggregationCollector> createCollectors(List<AggregationManager> managers, int length) {
        List<AggregationCollector> collectors = new ArrayList(managers.size());
        Iterator var3 = managers.iterator();

        while(var3.hasNext()) {
            AggregationManager manager = (AggregationManager)var3.next();
            collectors.add(manager.getCollector(length));
        }

        return collectors;
    }

    public static class AggregationTreeLeaf {
        private final List<AggregationFunction> functions;

        AggregationTreeLeaf(List<AggregationFunction> functions) {
            this.functions = functions;
        }

        public List<AggregationFunction> getFunctions() {
            return this.functions;
        }
    }
}
