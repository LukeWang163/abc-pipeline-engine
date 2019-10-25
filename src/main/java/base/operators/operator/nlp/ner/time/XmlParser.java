package base.operators.operator.nlp.ner.time;

import idsw.nlp.read.ReadFileAsStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class XmlParser {
	//private static final String FILE_NAME = "/nlp/time/TimeRule.xml";

	// 定义一个静态私有变量(不初始化，不使用final关键字，使用volatile保证了多线程访问时instance变量的可见性，避免了instance初始化时其他变量属性还没赋值完时，被另外线程调用)
	private static volatile XmlParser defaultInstance;

	// 定义一个共有的静态方法，返回该类型实例
	public static XmlParser getIstance() {
		// 对象实例化时与否判断（不使用同步代码块，instance不等于null时，直接返回对象，提高运行效率）
		if (defaultInstance == null) {
			// 同步代码块（对象未初始化时，使用同步代码块，保证多线程访问时对象在第一次创建后，不再重复被创建）
			synchronized (XmlParser.class) {
				// 未初始化，则初始instance变量
				if (defaultInstance == null) {
					defaultInstance = new XmlParser();
				}
			}
		}
		return defaultInstance;
	}

	private List<String> getRules() {
		List<String> ruleList = null;
		try {
			InputStream inStream = null;
			inStream = ReadFileAsStream.readTimeDict();
			ruleList = load(inStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ruleList;
	}

	public static List<String> getTimePatterns() {
		return getIstance().getRules();
	}

	private List<String> load(InputStream inputStream) throws IOException {
		try {
			List<String> ruleList = new ArrayList<String>();
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			XMLEventReader xmlEventReader = inputFactory
					.createXMLEventReader(inputStream);
			long rowNumber = 0;
			while (xmlEventReader.hasNext()) {
				XMLEvent event = xmlEventReader.nextEvent();
				// rowNumber++;
				if (event.isStartElement()) {
					StartElement startElement = event.asStartElement();
					if (startElement.getName().toString().equals("c")) {
						String rule = getXmlElement(startElement, "rule");
						//System.out.println("rule:"+rule);
						ruleList.add(rule.trim());
					}
				}
			}
			// System.out.println("Load file rownumber:" + rowNumber);
			inputStream.close();
			return ruleList;
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}

	private String getXmlElement(StartElement startElement, String element) {
		QName e = QName.valueOf(element);
		if (e == null)
			return "";
		Attribute attr = startElement.getAttributeByName(e);
		if (attr == null)
			return "";
		String v = attr.getValue();
		if (v == null)
			return "";
		return v;

	}

}
