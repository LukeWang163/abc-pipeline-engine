package base.operators.operator.learner.associations.fpgrowth;

import base.operators.operator.Operator;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.learner.associations.FrequentItemSet;
import base.operators.operator.learner.associations.fpgrowth.BeltFPTree.Header;
import base.operators.operator.learner.associations.fpgrowth.BeltFPTree.Node;
import java.util.List;

class BeltFPTreeMiner
{
    private final Operator operator;
    private final int minItemSetSize;
    private final int maxItemSetSize;
    private int maxRemainingItemSets;
    private List<FrequentItemSet> accumulator;
    private int minFrequency;

    BeltFPTreeMiner(Operator operator, int minItemSetSize, int maxItemSetSize) {
        this.maxRemainingItemSets = Integer.MAX_VALUE;
        this.operator = operator;
        this.minItemSetSize = minItemSetSize;
        this.maxItemSetSize = maxItemSetSize;
    }

    void mine(BeltFPTree tree, FrequentItemSet baseItemSet, int minFrequency, List<FrequentItemSet> acc, int desiredItemSets, boolean showProgress) throws OperatorException {
        this.maxRemainingItemSets = desiredItemSets;
        this.accumulator = acc;
        this.minFrequency = minFrequency;
        if (isBelowMaximalSize(baseItemSet)) {
            mine(tree, baseItemSet, showProgress);
        }
    }

    private void mine(BeltFPTree tree, FrequentItemSet baseItemSet, boolean showProgress) throws OperatorException {
        Header[] arrayOfHeader = tree.getHeaders();
        int minItem = Math.max(0, this.minItemSetSize - baseItemSet.getNumberOfItems() - 1);
        FrequentItemSet[] itemSets = new FrequentItemSet[Math.max(0, arrayOfHeader.length - minItem)];
        for (int itemId = arrayOfHeader.length - 1; itemId >= minItem; itemId--) {
            Header header = arrayOfHeader[itemId];
            FrequentItemSet itemSet = (FrequentItemSet)baseItemSet.clone();
            itemSet.addItem(header.getItem(), header.getSumFrequency());

            this.accumulator.add(itemSet);
            if (itemSet.getNumberOfItems() >= this.minItemSetSize && --this.maxRemainingItemSets <= 0) {
                return;
            }
            itemSets[itemId - minItem] = itemSet;
        }

        traverseConditionalTrees(tree, arrayOfHeader, itemSets, minItem, showProgress);
    }

    private void traverseConditionalTrees(BeltFPTree tree, Header[] headers, FrequentItemSet[] itemSets, int minItem, boolean showProgress) throws OperatorException {
        for (int itemId = headers.length - 1; itemId >= minItem; itemId--) {
            Header header = headers[itemId];
            FrequentItemSet itemSet = itemSets[itemId - minItem];
            if (isBelowMaximalSize(itemSet))
            {
                if (header.getNode().getSibling() == null) {
                    mineSinglePath(header.getNode(), getHeight(header), headers, header.getSumFrequency(), itemSet);
                } else {
                    BeltFPTree conditionalTree = new BeltFPTree(tree, header, this.minFrequency);
                    mine(conditionalTree, itemSet, false);
                }
            }
            if (showProgress) {
                this.operator.getProgress().step(9000 / headers.length);
            }
            this.operator.checkForStop();
        }
    }

    private boolean isBelowMaximalSize(FrequentItemSet itemSet) { return (this.maxItemSetSize == 0 || itemSet.getNumberOfItems() < this.maxItemSetSize); }

    private int getHeight(Header header) {
        Node node = header.getNode().getParent();
        int height = 0;
        while (node != null) {
            node = node.getParent();
            height++;
        }
        return height;
    }

    private void mineSinglePath(Node node, int height, Header[] headers, int frequency, FrequentItemSet baseItemSet) throws ProcessStoppedException {
        int maxOffset = height - Math.max(0, this.minItemSetSize - baseItemSet.getNumberOfItems() - 1);
        FrequentItemSet[] itemSets = new FrequentItemSet[maxOffset];
        Node currentNode = node;
        for (int offset = 0; offset < maxOffset; offset++) {
            currentNode = currentNode.getParent();

            FrequentItemSet itemSet = (FrequentItemSet)baseItemSet.clone();
            itemSet.addItem(headers[currentNode.getItemId()].getItem(), frequency);

            this.accumulator.add(itemSet);
            if (itemSet.getNumberOfItems() >= this.minItemSetSize && --this.maxRemainingItemSets <= 0) {
                return;
            }

            itemSets[offset] = itemSet;
        }
        this.operator.checkForStop();
        traverseSubpaths(node, height, headers, frequency, maxOffset, itemSets);
    }

    private void traverseSubpaths(Node node, int height, Header[] headers, int frequency, int maxOffset, FrequentItemSet[] itemSets) throws ProcessStoppedException {
        Node currentNode = node;
        for (int offset = 0; offset < maxOffset; offset++) {
            currentNode = currentNode.getParent();
            int newHeight = height - 1 - offset;
            FrequentItemSet itemSet = itemSets[offset];
            if (isBelowMaximalSize(itemSet)) {
                mineSinglePath(currentNode, newHeight, headers, frequency, itemSet);
            }
        }
    }
}
