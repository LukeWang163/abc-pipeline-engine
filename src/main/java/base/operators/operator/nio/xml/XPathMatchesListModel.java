package base.operators.operator.nio.xml;

import base.operators.tools.I18N;
import base.operators.tools.xml.MapBasedNamespaceContext;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class XPathMatchesListModel extends AbstractListModel<Object>{
    private static final long serialVersionUID = 5596412058073512745L;
    private Document document;
    private XPath xpath;
    private NodeList exampleNodes;
    private List<XPathMatchesResultListener> listeners;
    private int maxElements;

    public XPathMatchesListModel(Document document, Map<String, String> namespaceMap, String defaultNamespaceURI, int maxElements) {
        this.listeners = new LinkedList();
        this.document = document;
        this.xpath = XPathFactory.newInstance().newXPath();
        this.maxElements = maxElements;
        this.xpath.setNamespaceContext(new MapBasedNamespaceContext(namespaceMap, defaultNamespaceURI));
    }

    public void setXPathExpression(String expression) {
        XPathExpression exampleExpression = null;
        try {
            exampleExpression = this.xpath.compile(expression);
        } catch (XPathExpressionException e1) {
            fireStateChange(I18N.getGUILabel("xml_reader.wizard.illegal_xpath", new Object[] { e1 }), true);
        }
        if (exampleExpression != null) {
            try {
                final int oldSize = getSize();
                this.exampleNodes = (NodeList)exampleExpression.evaluate(this.document, XPathConstants.NODESET);
                List<String> illegalElements = new LinkedList<String>();
                for (int i = 0; i < this.exampleNodes.getLength(); i++) {
                    if (!(this.exampleNodes.item(i) instanceof org.w3c.dom.Element)) {
                        illegalElements.add(this.exampleNodes.item(i).getNodeName());
                    }
                }
                if (!illegalElements.isEmpty()) {
                    fireStateChange(
                            I18N.getGUILabel("xml_reader.wizard.xpath_non_element_nodes", new Object[] { illegalElements.toString() }), true);
                    this.exampleNodes = null;
                    return;
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        XPathMatchesListModel.this.fireContentsChanged(this, 0, Math.min(oldSize, XPathMatchesListModel.this.exampleNodes.getLength()));
                        if (oldSize > XPathMatchesListModel.this.exampleNodes.getLength()) {
                            XPathMatchesListModel.this.fireIntervalRemoved(this, XPathMatchesListModel.this.exampleNodes.getLength(), oldSize - 1);
                        } else if (oldSize < XPathMatchesListModel.this.exampleNodes.getLength()) {
                            XPathMatchesListModel.this.fireIntervalAdded(this, oldSize, XPathMatchesListModel.this.exampleNodes.getLength() - 1);
                        }
                        XPathMatchesListModel.this.fireStateChange(I18N.getGUILabel("xml_reader.wizard.xpath_result", new Object[] {XPathMatchesListModel.this.exampleNodes.getLength()}), (XPathMatchesListModel.this.exampleNodes.getLength() == 0));
                    }
                });
            } catch (XPathExpressionException e) {
                this.exampleNodes = null;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run()
                    {
                        XPathMatchesListModel.this.fireStateChange(I18N.getGUILabel("xml_reader.wizard.illegal_xpath", new Object[] { e.getMessage() }), true);
                    }
                });
            }
        } else {
            this.exampleNodes = null;
        }
    }

    @Override
    public int getSize() {
        if (this.exampleNodes == null) {
            return 0;
        }
        if (this.exampleNodes.getLength() > this.maxElements) {
            return this.maxElements;
        }
        return this.exampleNodes.getLength();
    }

    @Override
    public Object getElementAt(int index) { return this.exampleNodes.item(index); }

    public void addListener(XPathMatchesResultListener listener) { this.listeners.add(listener); }

    public void removeListener(XPathMatchesResultListener listener) { this.listeners.remove(listener); }

    private void fireStateChange(String message, boolean error) {
        for (XPathMatchesResultListener listener : this.listeners) {
            listener.informStateChange(message, error);
        }
    }

    public static interface XPathMatchesResultListener {
        void informStateChange(String param1String, boolean param1Boolean);
    }
}
