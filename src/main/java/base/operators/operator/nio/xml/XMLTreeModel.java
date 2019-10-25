package base.operators.operator.nio.xml;

import java.util.LinkedList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class XMLTreeModel implements TreeModel{
    private LinkedList<TreeModelListener> listeners;
    private Element rootElement;
    private boolean provideAttributes;

    public XMLTreeModel(Element rootElement, boolean provideAttributes) {
        this.listeners = new LinkedList();
        this.rootElement = rootElement;
        this.provideAttributes = provideAttributes;
    }

    @Override
    public Object getRoot() { return this.rootElement; }

    @Override
    public Object getChild(Object parent, int index) {
        Element element = (Element)parent;
        NodeList childNodes = element.getChildNodes();
        int elementIndex = 0;
        if (this.provideAttributes) {
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                if (elementIndex == index) {
                    return attributes.item(i);
                }
                elementIndex++;
            }
        }

        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element) {
                if (elementIndex == index) {
                    return childNodes.item(i);
                }
                elementIndex++;
            }
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof org.w3c.dom.Attr) {
            return 0;
        }
        Element element = (Element)parent;
        NodeList childNodes = element.getChildNodes();
        int childCount = 0;
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element) {
                childCount++;
            }
        }
        if (this.provideAttributes) {
            childCount += element.getAttributes().getLength();
        }
        return childCount;
    }

    @Override
    public boolean isLeaf(Object node) { return (getChildCount(node) == 0); }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        for (TreeModelListener listener : this.listeners) {
            listener.treeNodesChanged(new TreeModelEvent(this, path));
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        int elementIndex = 0;
        if (this.provideAttributes) {
            if (parent instanceof org.w3c.dom.Attr) {
                return -1;
            }
            Element element = (Element)parent;

            if (child instanceof org.w3c.dom.Attr) {
                NamedNodeMap attributes = element.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (child == attributes.item(i)) {
                        return elementIndex;
                    }
                    elementIndex++;
                }
            }
        }

        Element element = (Element)parent;
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element) {
                if (child == childNodes.item(i)) {
                    return elementIndex;
                }
                elementIndex++;
            }
        }
        return -1;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) { this.listeners.add(l); }

    @Override
    public void removeTreeModelListener(TreeModelListener l) { this.listeners.remove(l); }
}

