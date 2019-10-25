package base.operators.operator.learner.associations.fpgrowth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BeltFPTree
{
    private Header[] headers;
    private int nNodes;

    static class Node
    {
        private Node parent;
        private int itemId;
        private int frequency;
        private Node sibling;

        Node(Node parent, int itemId, int frequency) {
            this.parent = parent;
            this.itemId = itemId;
            this.frequency = frequency;
        }

        Node getParent() { return this.parent; }

        Node getSibling() { return this.sibling; }

        int getItemId() { return this.itemId; }

        int getFrequency() { return this.frequency; }

        void updateFrequency(int frequency) { this.frequency += frequency; }

        private String getPath() { return ((this.parent == null) ? "null" : this.parent.getPath()) + " -> " + this.itemId; }

        @Override
        public String toString() { return "Node(" + getPath() + ":" + this.frequency + ")"; }
    }

    static final class Header
    {
        private NominalItem item;

        private Node node;

        private int sumFrequency;

        private Header(NominalItem item) { this.item = item; }

        private Header(NominalItem item, Node node) {
            this(item);
            this.node = node;
            this.sumFrequency = node.frequency;
        }

        void update(Node node) {
            node.sibling = this.node;
            this.node = node;
            this.sumFrequency += node.frequency;
        }

        NominalItem getItem() { return this.item; }

        Node getNode() { return this.node; }

        int getSumFrequency() { return this.sumFrequency; }

        void updateFrequency(int frequency) { this.sumFrequency += frequency; }

        @Override
        public String toString() { return "Header(item: " + this.item + ", node: " + this.node + ", sumFrequency: " + this.sumFrequency + ")"; }
    }

    private static final class ItemWithFrequency
            extends Object
            implements Comparable<ItemWithFrequency>
    {
        private NominalItem item;
        private int frequency;

        private ItemWithFrequency(NominalItem item, int frequency) {
            this.item = item;
            this.frequency = frequency;
        }

        @Override
        public int compareTo(ItemWithFrequency o) {
            int i = o.frequency - this.frequency;
            return (i == 0) ? this.item.compareTo(o.item) : i;
        }
    }

    private static final class ItemIdWithFrequency
            extends Object
            implements Comparable<ItemIdWithFrequency> {
        private int itemId;
        private int frequency;

        private ItemIdWithFrequency(int itemId, int frequency) {
            this.itemId = itemId;
            this.frequency = frequency;
        }

        @Override
        public int compareTo(ItemIdWithFrequency o) { return o.frequency - this.frequency; }
    }


    BeltFPTree(Map<NominalItem, Integer> itemFrequencies, int minFrequency) {
        List<ItemWithFrequency> orderedItemFrequencies = createItemsWithFrequencies(itemFrequencies, minFrequency);

        this.headers = new Header[orderedItemFrequencies.size()];
        if (orderedItemFrequencies.size() == 1) {
            ItemWithFrequency itemWithFrequency = (ItemWithFrequency)orderedItemFrequencies.get(0);
            this.headers[0] = new Header(itemWithFrequency.item, new Node(null, 0, itemWithFrequency.frequency));
            this.nNodes = 1;
        } else if (orderedItemFrequencies.size() > 1) {
            for (int i = 0; i < orderedItemFrequencies.size(); i++) {
                this.headers[i] = new Header(((ItemWithFrequency)orderedItemFrequencies.get(i)).item);
            }
        }
    }


    BeltFPTree(BeltFPTree base, Header header, int minFrequency) {
        Map<Integer, Integer> oldItemIdAndFrequencies = sumUpFrequencies(header);

        List<ItemIdWithFrequency> remainingOldItemIdAndFrequencies = createItemIdWithFrequencies(oldItemIdAndFrequencies, minFrequency);

        this.headers = new Header[remainingOldItemIdAndFrequencies.size()];
        if (remainingOldItemIdAndFrequencies.size() == 1) {
            ItemIdWithFrequency oldItemIdWithFrequency = (ItemIdWithFrequency)remainingOldItemIdAndFrequencies.get(0);
            this.headers[0] = new Header((base.headers[oldItemIdWithFrequency.itemId]).item, new Node((BeltFPTree.Node) null, 0, oldItemIdWithFrequency.frequency));
            this.nNodes = 1;
        } else if (remainingOldItemIdAndFrequencies.size() > 1) {
            Map<Integer, Integer> oldItemIdToNewItemIdMap = this.createIdRemapping(base, remainingOldItemIdAndFrequencies);

            this.nNodes = (new BeltFPTreeBuilder()).rebuiltTree(oldItemIdToNewItemIdMap, header.node, this.headers);
        }
    }

    Header[] getHeaders() { return this.headers; }

    int getNodeNumber() { return this.nNodes; }

    void updateNodeNumber(int number) { this.nNodes += number; }


    private Map<Integer, Integer> sumUpFrequencies(Header header) {
        Map<Integer, Integer> oldItemIdAndFrequencies = new HashMap<Integer, Integer>();
        Node node = header.node;
        while (node != null) {
            Node currentNode = node.parent;
            while (currentNode != null) {
                oldItemIdAndFrequencies.merge(Integer.valueOf(currentNode.itemId), Integer.valueOf(node.frequency), Integer::sum);

                currentNode = currentNode.parent;
            }

            node = node.sibling;
        }
        return oldItemIdAndFrequencies;
    }

    private Map<Integer, Integer> createIdRemapping(BeltFPTree base, List<ItemIdWithFrequency> oldItemIdAndFrequencies) {
        Collections.sort(oldItemIdAndFrequencies);
        Map<Integer, Integer> oldItemIdToNewItemIdMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < oldItemIdAndFrequencies.size(); i++) {
            int oldItemId = ((ItemIdWithFrequency)oldItemIdAndFrequencies.get(i)).itemId;
            this.headers[i] = new Header((base.headers[oldItemId]).item);
            oldItemIdToNewItemIdMap.put(Integer.valueOf(oldItemId), Integer.valueOf(i));
        }
        return oldItemIdToNewItemIdMap;
    }

    private static List<ItemWithFrequency> createItemsWithFrequencies(Map<NominalItem, Integer> itemFrequencies, int minFrequency) {
        List<ItemWithFrequency> orderedItemFrequencies = new ArrayList<ItemWithFrequency>();
        for (Map.Entry<NominalItem, Integer> itemAndFrequency : itemFrequencies.entrySet()) {
            int frequency = ((Integer)itemAndFrequency.getValue()).intValue();
            if (frequency >= minFrequency) {
                NominalItem item = (NominalItem)itemAndFrequency.getKey();
                orderedItemFrequencies.add(new ItemWithFrequency(item, frequency));
            }
        }
        Collections.sort(orderedItemFrequencies);
        return orderedItemFrequencies;
    }

    private static List<ItemIdWithFrequency> createItemIdWithFrequencies(Map<Integer, Integer> itemIdsAndFrequencies, int minFrequency) {
        List<ItemIdWithFrequency> itemIdAndFrequencies = new ArrayList<ItemIdWithFrequency>();
        for (Map.Entry<Integer, Integer> oldItemIdAndFrequency : itemIdsAndFrequencies.entrySet()) {
            int frequency = ((Integer)oldItemIdAndFrequency.getValue()).intValue();
            if (frequency >= minFrequency) {
                int itemId = ((Integer)oldItemIdAndFrequency.getKey()).intValue();
                itemIdAndFrequencies.add(new ItemIdWithFrequency(itemId, frequency));
            }
        }
        return itemIdAndFrequencies;
    }
}

