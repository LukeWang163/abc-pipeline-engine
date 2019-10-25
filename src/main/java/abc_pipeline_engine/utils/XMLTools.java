/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package abc_pipeline_engine.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;


/**
 * This class offers several convenience methods for treating XML documents-
 *
 * @author Sebastian Land, Simon Fischer
 */
public class XMLTools {


	/**
	 * Util class, no instance.
	 */
	private XMLTools() {
		throw new UnsupportedOperationException("Not to be instantiated");
	}

	private static final Map<URI, Validator> VALIDATORS = new HashMap<>();

	private static final DocumentBuilderFactory BUILDER_FACTORY;

	public static final String SCHEMA_URL_PROCESS = "http://www.rapidminer.com/xml/schema/RapidMinerProcess";

	private static final String FEATURE_FAILED_LOG_MSG = "ParserConfigurationException was thrown. The feature '{0}' is probably not supported by your XML processor.";
	private static final String CONTENTS_OF_TAG_0_MUST_BE_1_BUT_FOUND_2_LOG_MSG = "Contents of tag <{0}> must be {1}, but found '{2}'.";
	private static final String INTEGER_STRING = "integer";


	/**
	 * Security guideline from OWASP https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#Java
	 */
	static {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		String feature = null;
		try {
			// This is the PRIMARY defense. If DTDs (doctypes) are disallowed, almost all XML entity attacks are prevented
			// Xerces 2 only - http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
			feature = "http://apache.org/xml/features/disallow-doctype-decl";
			documentBuilderFactory.setFeature(feature, true);
		} catch (ParserConfigurationException e) {
			// This should catch a failed setFeature feature
			System.out.println(MessageFormat.format(FEATURE_FAILED_LOG_MSG, feature));
		}
		try {
			// If you can't completely disable DTDs, then at least do the following:
			// Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
			// Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
			// JDK7+ - http://xml.org/sax/features/external-general-entities
			feature = "http://xml.org/sax/features/external-general-entities";
			documentBuilderFactory.setFeature(feature, false);
		} catch (ParserConfigurationException e) {
			// This should catch a failed setFeature feature
			System.out.println(MessageFormat.format(FEATURE_FAILED_LOG_MSG, feature));
		}
		try {
			// Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
			// Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
			// JDK7+ - http://xml.org/sax/features/external-parameter-entities
			feature = "http://xml.org/sax/features/external-parameter-entities";
			documentBuilderFactory.setFeature(feature, false);
		} catch (ParserConfigurationException e) {
			// This should catch a failed setFeature feature
			System.out.println(MessageFormat.format(FEATURE_FAILED_LOG_MSG, feature));
		}
		try {
			// Disable external DTDs as well
			feature = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
			documentBuilderFactory.setFeature(feature, false);
		} catch (ParserConfigurationException e) {
			// This should catch a failed setFeature feature
			System.out.println(MessageFormat.format(FEATURE_FAILED_LOG_MSG, feature));
		}
		// and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
		documentBuilderFactory.setXIncludeAware(false);
		documentBuilderFactory.setExpandEntityReferences(false);

		// And, per Timothy Morgan: "If for some reason support for inline DOCTYPEs are a requirement, then
		// ensure the entity settings are disabled (as shown above) and beware that SSRF attacks
		// (http://cwe.mitre.org/data/definitions/918.html) and denial
		// of service attacks (such as billion laughs or decompression bombs via "jar:") are a risk."

		BUILDER_FACTORY = documentBuilderFactory;
	}

	/**
	 * Creates a new {@link DocumentBuilder} instance that is secured against XXE attacks.
	 *
	 * Needed because DocumentBuilder is not thread-safe and crashes when different threads try to
	 * parse at the same time.
	 *
	 * @return
	 * @throws IOException
	 *             if it fails to create a {@link DocumentBuilder}
	 */
	public static DocumentBuilder createDocumentBuilder(){
		try {
			synchronized (BUILDER_FACTORY) {
				return BUILDER_FACTORY.newDocumentBuilder();
			}
		} catch (ParserConfigurationException e) {
			System.out.println("Unable to create document builder" + e);
		}
		return null;
	}

	private static Validator getValidator(URI schemaURI){
		if (schemaURI == null) {
			throw new NullPointerException("SchemaURL is null!");
		}
		synchronized (VALIDATORS) {
			if (VALIDATORS.containsKey(schemaURI)) {
				return VALIDATORS.get(schemaURI);
			} else {
				SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
				Validator validator = null;
				try {
					validator = factory.newSchema(schemaURI.toURL()).newValidator();
				} catch (SAXException | MalformedURLException e) {
					System.out.println("Cannot parse XML schema: " + e.getMessage());
				}
				VALIDATORS.put(schemaURI, validator);
				return validator;
			}
		}
	}


	public static Document parse(String string) throws SAXException, IOException {
		return createDocumentBuilder().parse(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
	}

	public static Document parse(InputStream in) throws SAXException, IOException {
		return createDocumentBuilder().parse(in);
	}

	public static Document parse(File file) throws SAXException, IOException {
		return createDocumentBuilder().parse(file);
	}

	public static String toString(Document document){
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		stream(document, buf, StandardCharsets.UTF_8);
		return new String(buf.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * @param document
	 * @param encoding
	 * @return
	 * @throws XMLException
	 * @deprecated use {@link #toString(Document)} instead
	 */
	@Deprecated
	public static String toString(Document document, Charset encoding){
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		stream(document, buf, encoding);
		return new String(buf.toByteArray(), encoding);
	}

	public static void stream(Document document, File file, Charset encoding){
		try(OutputStream out = new FileOutputStream(file)) {
			stream(document, out, encoding);
		} catch (IOException e) {
			System.out.print("Cannot save XML to " + file + ": " + e);
		}
	}

	public static void stream(Document document, OutputStream out, Charset encoding){
		stream(new DOMSource(document), out, encoding);
	}

	public static void stream(DOMSource source, OutputStream out, Charset encoding){
		// we wrap this in a Writer to fix a Java bug
		// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
		if (encoding == null) {
			encoding = StandardCharsets.UTF_8;
		}
		stream(source, new StreamResult(new OutputStreamWriter(out, encoding)), encoding);
	}

	public static void stream(Document document, Result result, Charset encoding){
		stream(new DOMSource(document), result, encoding);
	}

	public static void stream(DOMSource source, Result result, Charset encoding){
		stream(source, result, encoding, null);
	}

	public static void stream(DOMSource source, Result result, Charset encoding, Properties outputProperties) {
		Transformer transformer = null;
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			try {
				tf.setAttribute("indent-number", Integer.valueOf(2));
			} catch (IllegalArgumentException e) {
				// ignore, may not be supported by implementation
			}
			transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			try {
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			} catch (IllegalArgumentException e) {
				// ignore, may not be supported by implementation
			}
			if (outputProperties != null) {
				transformer.setOutputProperties(outputProperties);
			}

			if (encoding != null) {
				transformer.setOutputProperty(OutputKeys.ENCODING, encoding.name());
			}
		} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
			System.out.println("Cannot transform XML: " + e);
		}
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			System.out.println("Cannot transform XML: " + e);
		}
	}

	/**
	 * As {@link #getTagContents(Element, String, boolean)}, but never throws an exception. Returns
	 * null if can't retrieve string.
	 */
	public static String getTagContents(Element element, String tag) {
		try {
			return getTagContents(element, tag, false);
		} catch (Exception e) {
			// cannot happen
			return null;
		}
	}

	public static String getTagContents(Element element, String tag, String deflt) {
		String result = getTagContents(element, tag);
		if (result == null) {
			return deflt;
		} else {
			return result;
		}
	}

	/**
	 * For a tag <parent> <tagName>content</tagName> <something>else</something> ... </parent>
	 *
	 * returns "content". This will return the content of the first occurring child element with
	 * name tagName. If no such tag exists and {@link XMLException} is thrown if
	 * throwExceptionOnError is true. Otherwise null is returned.
	 */
	public static String getTagContents(Element parent, String tagName, boolean throwExceptionOnError){
		NodeList nodeList = parent.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node instanceof Element && ((Element) node).getTagName().equals(tagName)) {
				Element child = (Element) node;
				return child.getTextContent();
			}
		}
		if (throwExceptionOnError) {
			System.out.println("Missing tag: <" + tagName + "> in <" + parent.getTagName() + ">.");
		} 
		return null;
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as integer. If no such child element can be found an XMLException is thrown. If more
	 * than one exists, the first is used. A {@link XMLException} is thrown if the text content is
	 * not a valid integer.
	 */
	public static int getTagContentsAsInt(Element element, String tag){
		final String string = getTagContents(element, tag, true);
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			System.out.println(MessageFormat.format(CONTENTS_OF_TAG_0_MUST_BE_1_BUT_FOUND_2_LOG_MSG, tag, INTEGER_STRING, string));
		}
		return -1;
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as integer. If no such child element can be found, the given default value is
	 * returned. If more than one exists, the first is used. A {@link XMLException} is thrown if the
	 * text content is not a valid integer.
	 */
	public static int getTagContentsAsInt(Element element, String tag, int dfltValue){
		final String string = getTagContents(element, tag, false);
		if (string == null) {
			return dfltValue;
		}
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			System.out.println(MessageFormat.format(CONTENTS_OF_TAG_0_MUST_BE_1_BUT_FOUND_2_LOG_MSG, tag, INTEGER_STRING, string));
		}
		return -1;
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as long. If no such child element can be found an XMLException is thrown. If more
	 * than one exists, the first is used. A {@link XMLException} is thrown if the text content is
	 * not a valid long.
	 */
	public static long getTagContentsAsLong(Element element, String tag){
		final String string = getTagContents(element, tag, true);
		try {
			return Long.parseLong(string);
		} catch (NumberFormatException e) {
			System.out.println(MessageFormat.format(CONTENTS_OF_TAG_0_MUST_BE_1_BUT_FOUND_2_LOG_MSG, tag, INTEGER_STRING, string));
		}
		return 1l;
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as long. If no such child element can be found, the given default value is returned.
	 * If more than one exists, the first is used. A {@link XMLException} is thrown if the text
	 * content is not a valid long.
	 */
	public static long getTagContentsAsLong(Element element, String tag, int dfltValue){
		final String string = getTagContents(element, tag, false);
		if (string == null) {
			return dfltValue;
		}
		try {
			return Long.parseLong(string);
		} catch (NumberFormatException e) {
			System.out.println(MessageFormat.format(CONTENTS_OF_TAG_0_MUST_BE_1_BUT_FOUND_2_LOG_MSG, tag, INTEGER_STRING, string));
		}
		return 0l;
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as double. If no such child element can be found, the given default value is
	 * returned. If more than one exists, the first is used. A {@link XMLException} is thrown if the
	 * text content is not a valid integer.
	 */
	public static double getTagContentsAsDouble(Element element, String tag, double dfltValue){
		final String string = getTagContents(element, tag, false);
		if (string == null) {
			return dfltValue;
		}
		try {
			return Double.parseDouble(string);
		} catch (NumberFormatException e) {
			System.out.println(MessageFormat.format(CONTENTS_OF_TAG_0_MUST_BE_1_BUT_FOUND_2_LOG_MSG, tag, "double", string));
		}
		return 0l;
	}

	/**
	 * This will parse the text contents of an child element of element parent with the given
	 * tagName as boolean. If no such child element can be found the default is returned. If more
	 * than one exists, the first is used. A {@link NumberFormatException} is thrown if the text
	 * content is not a valid integer.
	 */
	public static boolean getTagContentsAsBoolean(Element parent, String tagName, boolean dflt){
		String string = getTagContents(parent, tagName, false);
		if (string == null) {
			return dflt;
		}
		try {
			return Boolean.parseBoolean(string);
		} catch (NumberFormatException e) {
			System.out.println(MessageFormat.format(CONTENTS_OF_TAG_0_MUST_BE_1_BUT_FOUND_2_LOG_MSG, tagName, "true or false", string));
		}
		return true;
	}

	/**
	 * If parent has a direct child with the given name, the child's children are removed and are
	 * replaced by a single text node with the given text. If no direct child of parent with the
	 * given tag name exists, a new one is created.
	 */
	public static void setTagContents(Element parent, String tagName, String value) {
		if (value == null) {
			value = "";
		}
		Element child = null;
		NodeList list = parent.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node instanceof Element) {
				if (((Element) node).getTagName().equals(tagName)) {
					child = (Element) node;
					break;
				}
			}
		}
		if (child == null) {
			child = parent.getOwnerDocument().createElement(tagName);
			parent.appendChild(child);
		} else {
			while (child.hasChildNodes()) {
				child.removeChild(child.getFirstChild());
			}
		}
		child.appendChild(parent.getOwnerDocument().createTextNode(value));
	}

	/**
	 * This method removes all child elements with the given name of the given element.
	 */
	public static void deleteTagContents(Element parentElement, String name) {
		NodeList children = parentElement.getElementsByTagName(name);
		for (int i = children.getLength() - 1; i >= 0; i--) {
			Element child = (Element) children.item(i);
			parentElement.removeChild(child);
		}
	}

	public static XMLGregorianCalendar getXMLGregorianCalendar(Date date) {
		if (date == null) {
			return null;
		}
		// Calendar calendar = Calendar.getInstance();
		// calendar.setTimeInMillis(date.getTime());
		DatatypeFactory datatypeFactory;
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("Failed to create XMLGregorianCalendar: " + e, e);
		}
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(date);
		return datatypeFactory.newXMLGregorianCalendar(c);
		//
		// XMLGregorianCalendar xmlGregorianCalendar = datatypeFactory.newXMLGregorianCalendar();
		// xmlGregorianCalendar.setYear(calendar.get(Calendar.YEAR));
		// xmlGregorianCalendar.setMonth(calendar.get(Calendar.MONTH) + 1);
		// xmlGregorianCalendar.setDay(calendar.get(Calendar.DAY_OF_MONTH));
		// xmlGregorianCalendar.setHour(calendar.get(Calendar.HOUR_OF_DAY));
		// xmlGregorianCalendar.setMinute(calendar.get(Calendar.MINUTE));
		// xmlGregorianCalendar.setSecond(calendar.get(Calendar.SECOND));
		// xmlGregorianCalendar.setMillisecond(calendar.get(Calendar.MILLISECOND));
		// //
		// xmlGregorianCalendar.setTimezone(calendar.get(((Calendar.DST_OFFSET)+calendar.get(Calendar.ZONE_OFFSET))/(60*1000)));
		// return xmlGregorianCalendar;
	}

	/**
	 * This will return the inner tag of the given element with the given tagName. If no such
	 * element can be found, or if there are more than one, an {@link XMLException} is thrown.
	 */
	public static Element getUniqueInnerTag(Element element, String tagName){
		return getUniqueInnerTag(element, tagName, true);
	}

	/**
	 * This method will return null if the element doesn't exist if obligatory is false. Otherwise
	 * an exception is thrown. If the element is not unique, an exception is thrown in any cases.
	 */
	public static Element getUniqueInnerTag(Element element, String tagName, boolean obligatory){
		NodeList children = element.getChildNodes();
		Collection<Element> elements = new ArrayList<Element>();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i) instanceof Element) {
				Element child = (Element) children.item(i);
				if (tagName.equals(child.getTagName())) {
					elements.add(child);
				}
			}
		}
		switch (elements.size()) {
			case 0:
				if (obligatory) {
					System.out.println("Missing inner tag <" + tagName + "> inside <" + element.getTagName() + ">.");
				} else {
					return null;
				}
			case 1:
				return elements.iterator().next();
			default:
				System.out.println("Inner tag <" + tagName + "> inside <" + element.getTagName()
						+ "> must be unique, but found " + children.getLength() + ".");
		}
		return null;
	}

	/**
	 * This method will return a Collection of all Elements that are direct child elements of the
	 * given element and have the given tag name.
	 */
	public static Collection<Element> getChildElements(Element father, String tagName) {
		List<Element> elements = new LinkedList<>();
		NodeList list = father.getChildNodes();
		if(list != null) {
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node instanceof Element && node.getNodeName().equals(tagName)) {
					elements.add((Element) node);
				}
			}
		}
		
		return elements;
	}

	/**
	 * This method will return a Collection of all Elements that are direct child elements of the
	 * given element.
	 */
	public static Collection<Element> getChildElements(Element father) {
		List<Element> elements = new LinkedList<>();
		NodeList list = father.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node instanceof Element) {
				elements.add((Element) node);
			}
		}
		return elements;
	}

	/**
	 * This method will return the single inner child with the given name of the given father
	 * element. If obligatory is true, an Exception is thrown if the element is not present. If it's
	 * ambiguous, an execption is thrown in any case.
	 */
	public static Element getChildElement(Element father, String tagName, boolean mandatory){
		Collection<Element> children = getChildElements(father, tagName);
		switch (children.size()) {
			case 0:
				if (mandatory) {
					System.out.println("Missing child tag <" + tagName + "> inside <" + father.getTagName() + ">.");
				} else {
					return null;
				}
			case 1:
				return children.iterator().next();
			default:
				System.out.println("Child tag <" + tagName + "> inside <" + father.getTagName()
						+ "> must be unique, but found " + children.size() + ".");
		}
		return null;
	}

	/**
	 * This is the same as {@link #getChildElement(Element, String, boolean)}, but its always
	 * obligatory to have the child element.
	 *
	 * @throws XMLException
	 */
	public static Element getUniqueChildElement(Element father, String tagName){
		return getChildElement(father, tagName, true);
	}

	/**
	 * This adds a single tag with the given content to the given parent element. The new tag is
	 * automatically appended.
	 */
	public static void addTag(Element parent, String name, String textValue) {
		Element child = parent.getOwnerDocument().createElement(name);
		child.setTextContent(textValue);
		parent.appendChild(child);
	}

	/**
	 * Creates a new, empty document.
	 */
	public static Document createDocument() {
		try {
			DocumentBuilder builder = createDocumentBuilder();
			return builder.newDocument();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * This will add an empty new tag to the given fatherElement with the given name.
	 */
	public static Element addTag(Element fatherElement, String tagName) {
		Element createElement = fatherElement.getOwnerDocument().createElement(tagName);
		fatherElement.appendChild(createElement);
		return createElement;
	}

	/**
	 * Returns the unique child of the given element with the given tag name. This child tag must be
	 * unique, or an exception will be raised. If optional is false and the tag is missing, this
	 * method also raises an exception. Otherwise it returns null.
	 */
	public static Element getChildTag(Element element, String xmlTagName, boolean optional){
		NodeList children = element.getChildNodes();
		Element found = null;
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n instanceof Element && ((Element) n).getTagName().equals(xmlTagName)) {
				if (found != null) {
					System.out.println("Tag <" + xmlTagName + "> in <" + element.getTagName() + "> must be unique.");
				} else {
					found = (Element) n;
				}
			}
		}
		if (!optional && found == null) {
			System.out.println("Tag <" + xmlTagName + "> in <" + element.getTagName() + "> is missing.");
		} 
		return found;
	}

	/**
	 * Returns the contents of the inner tags with the given name as String array.
	 */
	public static String[] getChildTagsContentAsStringArray(Element father, String childElementName) {
		Collection<Element> valueElements = XMLTools.getChildElements(father, childElementName);
		String[] values = new String[valueElements.size()];
		int i = 0;
		for (Element valueElement : valueElements) {
			values[i] = valueElement.getTextContent();
			i++;
		}

		return values;
	}
	// 获取属性value中的值
	public static String[] getChildTagsValueAsStringArray(Element father, String childElementName) {
		Collection<Element> valueElements = XMLTools.getChildElements(father, childElementName);
		String[] values = new String[valueElements.size()];
		int i = 0;
		for (Element valueElement : valueElements) {
			values[i] = valueElement.getAttribute("value") != null?valueElement.getAttribute("value"):valueElement.getAttribute("Value");
			i++;
		}

		return values;
	}

	/**
	 * Returns the contents of the inner tags with the given name as int array.
	 *
	 * @throws XMLException
	 */
	public static int[] getChildTagsContentAsIntArray(Element father, String childElementName){
		Collection<Element> valueElements = XMLTools.getChildElements(father, childElementName);
		int[] values = new int[valueElements.size()];
		int i = 0;
		for (Element valueElement : valueElements) {
			try {
				values[i] = Integer.valueOf(valueElement.getTextContent());
			} catch (NumberFormatException e) {
				System.out.println("Invalid format for element content of type " + childElementName);
			}
			i++;
		}

		return values;
	}
	// 获取属性value中的值
	public static int[] getChildTagsValueAsIntArray(Element father, String childElementName){
		if(father != null) {
			Collection<Element> valueElements = XMLTools.getChildElements(father, childElementName);
			if(valueElements.size() > 0) {
				int[] values = new int[valueElements.size()];
				int i = 0;
				for (Element valueElement : valueElements) {
					try {
						String value = valueElement.getAttribute("value") != null?valueElement.getAttribute("value"):valueElement.getAttribute("Value");
						if(value != null && !value.equals("")) {
							values[i] = Integer.valueOf(value);
						} else {
							values[i] = -1;
						}
					} catch (NumberFormatException e) {
						System.out.println("Invalid format for element content of type " + childElementName);
					}
					i++;
				}
				return values;
			}
		}
		return null;
	}

	/**
	 * This method will get a XPath expression matching all elements given. This works by following
	 * this algorithm: 1. Check whether the last element is of same type Yes: if paths of elements
	 * are of same structure, keep it, but remove counters where necessary if not,
	 */
	public static String getXPath( Element... elements) {
		Map<String, List<Element>> elementTypeElementsMap = new HashMap<>();
		for (Element element : elements) {
			List<Element> typeElements = elementTypeElementsMap.get(element.getTagName());
			if (typeElements == null) {
				typeElements = new LinkedList<>();
				elementTypeElementsMap.put(element.getTagName(), typeElements);
			}
			typeElements.add(element);
		}

		// for each single type of element build single longest common path of all elements

		Element[] parentElements = new Element[elements.length];

		for (int i = 0; i < elements.length; i++) {
			parentElements[i] = (Element) elements[i].getParentNode();
		}

		return "";
	}
}
