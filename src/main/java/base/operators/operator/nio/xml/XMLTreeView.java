package base.operators.operator.nio.xml;

import base.operators.tools.FontTools;
import java.awt.Component;
import java.awt.Font;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

public class XMLTreeView extends JTree{
    private static final long serialVersionUID = 1L;
    private Map<String, String> namespaceUriToIdMap;
    private Set<Object> highlightedNodes;
    private boolean showElementIndices;

    public XMLTreeView(Map<String, String> namespacesMap) {
        this.showElementIndices = false;
        setNamespacesMap(namespacesMap);
        setCellRenderer(new DefaultTreeCellRenderer() {
            private static final long serialVersionUID = 1L;
            private JPanel emptyPanel = new JPanel();

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                StringBuilder builder = new StringBuilder();
                if (value instanceof Element) {
                    Element element = (Element)value;
                    builder.append("<");
                    if (element.getNamespaceURI() != null) {
                        builder.append((String)XMLTreeView.this.namespaceUriToIdMap.get(element.getNamespaceURI())).append(":");
                    }
                    builder.append(element.getLocalName());
                    if (XMLTreeView.this.showElementIndices) {
                        int index = XMLDomHelper.getElementIndex(element);
                        builder.append("[");
                        builder.append(index);
                        builder.append("]");
                    }
                    builder.append(">");
                }
                else if (value instanceof Attr) {
                    builder.append("@");
                    Attr attribute = (Attr)value;

                    if (attribute.getNamespaceURI() != null) {
                        builder.append((String)XMLTreeView.this.namespaceUriToIdMap.get(attribute.getNamespaceURI())).append(":");
                    }
                    builder.append(attribute.getLocalName());
                } else {

                    return this.emptyPanel;
                }

                JLabel treeCellRendererComponent = (JLabel)super.getTreeCellRendererComponent(tree, builder.toString(), selected, expanded, leaf, row, hasFocus);
                treeCellRendererComponent.setIcon(null);
                if (XMLTreeView.this.highlightedNodes.contains(value)) {
                    Font font = treeCellRendererComponent.getFont();
                    treeCellRendererComponent.setFont(FontTools.getFont(font.getName(), 1, font.getSize()));
                }
                return treeCellRendererComponent;
            }
        });
    }

    @Override
    public void setModel(TreeModel model) {
        this.highlightedNodes = new HashSet();
        super.setModel(model);
    }

    public void setNamespacesMap(Map<String, String> namespacesMap) {
        this.namespaceUriToIdMap = new HashMap();
        for (Map.Entry<String, String> entry : namespacesMap.entrySet()) {
            this.namespaceUriToIdMap.put(entry.getValue(), entry.getKey());
        }
    }

    public Set<Element> getElementsFromSelection() {
        Set<Element> selection = new HashSet<Element>();
        TreePath[] selectionPaths = getSelectionPaths();
        if (selectionPaths == null) {
            return selection;
        }
        for (TreePath path : selectionPaths) {
            Object lastComponent = path.getLastPathComponent();
            if (lastComponent instanceof Element) {
                selection.add((Element)lastComponent);
            }
        }
        return selection;
    }

    public Set<Attr> getAttributesFromSelection() {
        Set<Attr> selection = new HashSet<Attr>();
        TreePath[] selectionPaths = getSelectionPaths();
        if (selectionPaths == null) {
            return selection;
        }
        for (TreePath path : selectionPaths) {
            Object lastComponent = path.getLastPathComponent();
            if (lastComponent instanceof Attr) {
                selection.add((Attr)lastComponent);
            }
        }
        return selection;
    }

    public void setHighlighted(Object node, boolean highlighted) {
        if (highlighted) {
            this.highlightedNodes.add(node);
        } else {
            this.highlightedNodes.remove(node);
        }
    }

    public void setShowElementIndices(boolean yes) { this.showElementIndices = yes; }
}
