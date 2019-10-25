package base.operators.operator.nio.xml;

import base.operators.tools.container.Pair;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLDomHelper {
    public static class AttributeNamespaceValue {
        private String name = null;
        private String namespace = null;
        private String value = null;
        private String element = null;

        @Override
        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = 31 * result + ((this.element == null) ? 0 : this.element.hashCode());
            result = 31 * result + ((this.name == null) ? 0 : this.name.hashCode());
            result = 31 * result + ((this.namespace == null) ? 0 : this.namespace.hashCode());
            return 31 * result + ((this.value == null) ? 0 : this.value.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            AttributeNamespaceValue other = (AttributeNamespaceValue)obj;
            if (this.element == null) {
                if (other.element != null) {
                    return false;
                }
            } else if (!this.element.equals(other.element)) {
                return false;
            }
            if (this.name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!this.name.equals(other.name)) {
                return false;
            }
            if (this.namespace == null) {
                if (other.namespace != null) {
                    return false;
                }
            } else if (!this.namespace.equals(other.namespace)) {
                return false;
            }
            if (this.value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!this.value.equals(other.value)) {
                return false;
            }
            return true;
        }

        public String getName() { return this.name; }

        public String getNamespace() { return this.namespace; }

        public String getValue() { return this.value; }

        public String getElement() { return this.element; }

        public void setElement(String element) { this.element = element; }

        public void setName(String name) { this.name = name; }

        public void setNamespace(String namespace) { this.namespace = namespace; }

        public void setValue(String value) { this.value = value; }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (this.element != null) {
                builder.append(this.element);
            }
            builder.append("[@");
            if (this.namespace != null) {
                builder.append(this.namespace);
                builder.append(":");
            }
            builder.append(this.name);
            builder.append("=\"");
            if (this.value != null) {
                builder.append(this.value);
            }
            builder.append("\"]");
            return builder.toString();
        }
    }

    public static List<Pair<String, String>> getCommonAncestorNames(Set<Element> elements) {
        Iterator<Element> it = elements.iterator();
        if (!it.hasNext()) {
            return new LinkedList();
        }

        Element referenceElement = (Element)it.next();
        String referenceElementNS = referenceElement.getNamespaceURI();
        String referenceElementName = referenceElement.getLocalName();

        while (it.hasNext()) {
            Element currentElement = (Element)it.next();
            String currentElementNS = currentElement.getNamespaceURI();
            String currentElementName = currentElement.getLocalName();

            if (currentElementNS != null && referenceElementNS != null) {
                if (!currentElementNS.equals(referenceElementNS)) {
                    return new LinkedList();
                }
            }
            else if (referenceElementNS != currentElementNS) {
                return new LinkedList();
            }

            if (!currentElementName.equals(referenceElementName)) {
                return new LinkedList();
            }
        }

        LinkedList<Pair<String, String>> commonAncestors = new LinkedList<Pair<String, String>>();
        Set<Element> directAncestors = getDirectAncestors(elements);

        if (!directAncestors.isEmpty()) {
            commonAncestors.addAll(getCommonAncestorNames(directAncestors));
        }

        commonAncestors.add(new Pair(referenceElementNS, referenceElementName));
        return commonAncestors;
    }

    public static Set<AttributeNamespaceValue> getCommonAttributes(Set<Element> elements) {
        Set<AttributeNamespaceValue> commonAttributeValueSet = null;
        for (Element element : elements) {
            Set<AttributeNamespaceValue> elementAttributeValueSet = new HashSet<AttributeNamespaceValue>();
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attribute = (Attr)attributes.item(i);

                if (!attribute.getLocalName().equals("xmlns") && (attribute.getNamespaceURI() == null || !attribute.getNamespaceURI().equals("http://www.w3.org/2000/xmlns/"))) {
                    AttributeNamespaceValue attributeNSValue = new AttributeNamespaceValue();
                    attributeNSValue.setName(attribute.getLocalName());
                    attributeNSValue.setNamespace(attribute.getNamespaceURI());
                    attributeNSValue.setValue(attribute.getValue());
                    elementAttributeValueSet.add(attributeNSValue);
                }
            }

            if (commonAttributeValueSet == null) {
                commonAttributeValueSet = elementAttributeValueSet;
            } else {
                commonAttributeValueSet.retainAll(elementAttributeValueSet);
            }

            if (commonAttributeValueSet.isEmpty()) {
                return commonAttributeValueSet;
            }
        }
        return commonAttributeValueSet;
    }

    public static Set<Element> getDirectAncestors(Set<Element> elements) {
        Set<Element> directAncestors = new HashSet<Element>();
        for (Element element : elements) {
            if (element.getParentNode() != null && element.getParentNode() instanceof Element) {
                directAncestors.add((Element)element.getParentNode()); continue;
            }
            return new HashSet();
        }
        return directAncestors;
    }

    public static String getXPath(Element rootElement, Element targetElement, boolean includeElementIndex, Map<String, String> namespacesMap) {
        Stack<Element> elementPath = new Stack<Element>();
        Map<String, String> namespaceUriToIdMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : namespacesMap.entrySet()) {
            namespaceUriToIdMap.put(entry.getValue(), entry.getKey());
        }

        Element currentElement = targetElement;
        while (currentElement != null && currentElement != rootElement) {
            elementPath.push(currentElement);
            Node parent = currentElement.getParentNode();
            if (parent instanceof Element) {
                currentElement = (Element)currentElement.getParentNode(); continue;
            }
            currentElement = null;
        }

        StringBuilder builder = new StringBuilder();
        while (!elementPath.isEmpty()) {
            currentElement = (Element)elementPath.pop();
            if (builder.length() > 0)
            {
                builder.append("/");
            }

            if (namespacesMap != null) {
                String namespace = currentElement.getNamespaceURI();
                if (namespace != null) {
                    namespace = (String)namespaceUriToIdMap.get(namespace);
                    builder.append(namespace);
                    builder.append(":");
                }
            }
            builder.append(currentElement.getLocalName());
            if (includeElementIndex) {
                int index = getElementIndex(currentElement);
                builder.append("[");
                builder.append(index);
                builder.append("]");
            }
        }
        return builder.toString();
    }

    public static int getElementIndex(Element element) {
        int index = 1;
        Node sibling = element;
        while ((sibling = sibling.getPreviousSibling()) != null) {
            if (sibling instanceof Element) {
                Element siblingElement = (Element)sibling;
                if (element.getLocalName().equals(siblingElement.getLocalName()) && (
                        (element.getNamespaceURI() == null) ? (siblingElement.getNamespaceURI() == null) : element
                                .getNamespaceURI().equals(siblingElement.getNamespaceURI()))) {
                    index++;
                }
            }
        }
        return index;
    }

    public static String nodeListToString(NodeList nodeList) throws TransformerException {
        StringWriter stringWriter = new StringWriter();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty("omit-xml-declaration", "yes");
                transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
            } else {
                stringWriter.append(node.getTextContent());
            }
        }
        return stringWriter.toString();
    }
}
