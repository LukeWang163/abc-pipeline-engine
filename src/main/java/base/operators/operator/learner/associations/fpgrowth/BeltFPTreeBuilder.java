package base.operators.operator.learner.associations.fpgrowth;

import base.operators.belt.column.Column;
import base.operators.belt.reader.ObjectReader;
import base.operators.belt.reader.Readers;
import base.operators.operator.learner.associations.fpgrowth.BeltFPTree.Header;
import base.operators.operator.learner.associations.fpgrowth.BeltFPTree.Node;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class BeltFPTreeBuilder
{
    private Node node;
    private Map<Integer, BeltFPTreeBuilder> children;

    BeltFPTreeBuilder() {}

    private BeltFPTreeBuilder(Node node) { this.node = node; }

    int rebuiltTree(Map<Integer, Integer> oldItemIdToNewItemIdMap, Node headerNode, Header[] headers) {
        int numberOfNodes = 0;
        Node sibling = headerNode;
        while (sibling != null) {
            List<Integer> newItemIdSet = new ArrayList<Integer>();
            Node currentNode = sibling.getParent();
            while (currentNode != null) {
                Integer newItemId = (Integer)oldItemIdToNewItemIdMap.get(Integer.valueOf(currentNode.getItemId()));
                if (newItemId != null) {
                    newItemIdSet.add(newItemId);
                }

                currentNode = currentNode.getParent();
            }
            Collections.sort(newItemIdSet);

            numberOfNodes += addIdItemSet(newItemIdSet, headers, sibling.getFrequency());
            sibling = sibling.getSibling();
        }
        return numberOfNodes;
    }

    private int addIdItemSet(List<Integer> itemIdSet, Header[] headers, int frequency) {
        int sizeIncrement = 0;
        BeltFPTreeBuilder parent = this;
        for (Iterator iterator = itemIdSet.iterator(); iterator.hasNext(); ) { int itemId = ((Integer)iterator.next()).intValue();
            BeltFPTreeBuilder child = null;
            if (parent.children == null) {
                parent.children = new HashMap();
            } else {
                child = (BeltFPTreeBuilder)parent.children.get(Integer.valueOf(itemId));
            }

            if (child == null) {
                Node newNode = new Node(parent.node, itemId, frequency);
                child = new BeltFPTreeBuilder(newNode);
                parent.children.put(Integer.valueOf(itemId), child);
                headers[itemId].update(newNode);
                sizeIncrement++;
            } else {
                BeltFPTree.Node newNode = child.node;
                newNode.updateFrequency(frequency);
                headers[itemId].updateFrequency(frequency);
                sizeIncrement += 0;
            }
            parent = child; }

        return sizeIncrement;
    }

    static BeltFPTree buildFromItemsColumns(Column column, List<NominalItem> mandatoryItems, Map<NominalItem, Integer> itemFrequencies, int minFrequency) {
        BeltFPTree BeltFPTree = new BeltFPTree(itemFrequencies, minFrequency);
        if (BeltFPTree.getHeaders().length > 1) {
            BeltFPTreeBuilder beltFpTreeBuilder = new BeltFPTreeBuilder();
            ObjectReader<ItemSet> reader = Readers.objectReader(column, ItemSet.class);
            Map<String, Integer> nameToHeaderId = new HashMap<String, Integer>();
            BeltFPTree.Header[] arrayOfHeader = BeltFPTree.getHeaders();
            for (int i = 0; i < arrayOfHeader.length; i++) {
                nameToHeaderId.put(arrayOfHeader[i].getItem().getName(), Integer.valueOf(i));
            }
            boolean mandatoryEmpty = mandatoryItems.isEmpty();
            while (reader.hasRemaining()) {
                ItemSet itemSet = (ItemSet)reader.read();
                if (mandatoryEmpty || allItemsPresent(itemSet, mandatoryItems)) {
                    int newNodes = processItemSet(itemSet, BeltFPTree.getHeaders(), nameToHeaderId, beltFpTreeBuilder);
                    BeltFPTree.updateNodeNumber(newNodes);
                }
            }
        }

        return BeltFPTree;
    }

    private static int processItemSet(ItemSet itemSet, Header[] headers, Map<String, Integer> nameToHeaderId, BeltFPTreeBuilder beltFpTreeBuilder) {
        List<Integer> itemIdSet = new ArrayList<Integer>();
        for (String name : itemSet) {
            Integer headerIndex = (Integer)nameToHeaderId.get(name);
            if (headerIndex != null) {
                itemIdSet.add(headerIndex);
            }
        }
        Collections.sort(itemIdSet);
        return beltFpTreeBuilder.addIdItemSet(itemIdSet, headers, 1);
    }

    private static boolean allItemsPresent(ItemSet itemSet, List<NominalItem> mandatoryItems) {
        for (NominalItem item : mandatoryItems) {
            if (!item.isPresentInItemSet(itemSet)) {
                return false;
            }
        }
        return true;
    }
}
