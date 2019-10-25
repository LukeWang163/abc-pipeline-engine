package base.operators.operator.nlp.ner.location;

import idsw.nlp.read.ReadFileAsStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.util.*;

/**
 * 行政区划地址文件加载 王建华在张娴2017/6/22日版本上修改
 */
public class NSDictionary {
	//private String nsDictPath = "/nlp/location/config/location/dict-location.json";//dict-location-all.json行政区划地址文件路径

	public DoubleArrayTrie locTrie = new DoubleArrayTrie();

	public static final String TOPCODE = "TOPCODE";
	/**
	 * 存储整个词典内容，key为地址名称，如果一个地址名称对应多个行政区划，那么Set里存多个
	 */
	public Map<String, Set<Location>> locationWordMap = new HashMap<>();

	public Map<String, Set<Location>> getLocationWordMap() {
		return locationWordMap;
	}

	/**
	 * 存储整个词典中的词（只存放词），用与构建双数组树
	 */
	public List<String> locationWords = new ArrayList<String>();
	/**
	 * 编码地址详细信息映射
	 */
	public Map<String, Location> locMap = new TreeMap();

	private NSDictionary(int type, InputStream templateModelStream) {
		loadTrie(type, templateModelStream);
	}

	// 定义一个静态私有变量(不初始化，不使用final关键字，使用volatile保证了多线程访问时instance变量的可见性，避免了instance初始化时其他变量属性还没赋值完时，被另外线程调用)
	private static volatile NSDictionary defaultInstance;

	// 定义一个共有的静态方法，返回该类型实例
	public static NSDictionary getIstance(int type, InputStream templateModelStream) {
		// 对象实例化时与否判断（不使用同步代码块，instance不等于null时，直接返回对象，提高运行效率）
		if (defaultInstance == null) {
			// 同步代码块（对象未初始化时，使用同步代码块，保证多线程访问时对象在第一次创建后，不再重复被创建）
			synchronized (NSDictionary.class) {
				// 未初始化，则初始instance变量
				if (defaultInstance == null) {
					defaultInstance = new NSDictionary(type, templateModelStream);
				}
			}
		}
		return defaultInstance;
	}

	public Map<String, Location> getLocMap() {
		return locMap;
	}

	/**
	 * 获取双数组树
	 * 
	 * @return
	 */
	public DoubleArrayTrie getLocTrie() {
		return locTrie;
	}

	/**
	 * 获取整个词典中的词
	 * 
	 * @return
	 */
	public List<String> getLocationWords() {
		return locationWords;
	}

	/**
	 * 加载词典
	 */
	private void loadTrie(int type, InputStream templateModelStream) {
		JSONParser jsonParser = new JSONParser();
		if(type==0 || type==2) {
			List<String> dictFileNames = new ArrayList<>();
			try {
				InputStream inStream = null;
				inStream = ReadFileAsStream.readLocationJson("dict-location.json");
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						inStream, "UTF-8"));
				Object object = jsonParser.parse(reader);
				JSONObject dictRoot = (JSONObject) object;
				String configName = (String) dictRoot.get("name");
				JSONArray jsonArray = (JSONArray) dictRoot.get("dict_set");
				// System.out.println("dict_set中含有" + jsonArray.size() + "组字典");
				for (Object obj : jsonArray) {
					JSONObject dictObj = (JSONObject) ((JSONObject) obj)
							.get("dict");
					// System.out.println("dictObj：" + dictObj);
					dictFileNames.addAll(getDictFileNames(dictObj));
				}
			} catch (IOException | ParseException e) {
				e.printStackTrace();
			}
			// System.out.println("词典名称：" + dictFileNames);
			for (String fileName : dictFileNames) {
				System.out.println("词典名称：" + fileName);
				try {
					// System.out.println("词典名称：" + fileName);
					//fileName = "/nlp/location/dictionary/ns/" + fileName;
					InputStream inStream = null;
					inStream = ReadFileAsStream.readLocationDict(fileName);
					load(inStream);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} 
		if(type==1 || type==2) {
			try {
				load(templateModelStream);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		locationWords = new ArrayList<String>(locationWordMap.keySet());
		Collections.sort(locationWords);
		int load_error = locTrie.build(locationWords);
		System.out.println("地址行政区划词典加载是否错误: " + load_error);
		System.out.println("词总数: " + locationWords.size());
		System.out.println("词记录行总数: " + locMap.size());
		
/*
		Iterator it =locationWordMap.keySet().iterator();
		System.out.println("单字地名打印：");
		while(it.hasNext()){
			String word=(String)it.next();
			if(word.length()==1){
				System.out.println(word+"\t"+locationWordMap.get(word));
			}
		}

		Iterator itLoc =locMap.keySet().iterator();
		System.out.println("上级编码异常打印：");
		while(itLoc.hasNext()){
			String code=(String)itLoc.next();
			Location loc =locMap.get(code);
			String parentCode=loc.getParentCode();
			if(code.equals(parentCode)){
				System.out.println("====error parentcode this:\t"+code+"\t"+parentCode);
			}
			if(!parentCode.equals(TOPCODE)){
				Location parent=locMap.get(parentCode);
				if(parent==null) System.out.println("this\t"+parentCode+"\tparent\t"+code);
			}
		}
*/		
		
		
	}

	private void load(InputStream inputStream) throws IOException {
		try {
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
						String code = getXmlElement(startElement, "code");
			
						String name = getXmlElement(startElement, "name");
						String parentCode = getXmlElement(startElement,
								"parentCode");
						String shortName = getXmlElement(startElement,
								"shortName");
						String alias = getXmlElement(startElement, "alias");
						String level = getXmlElement(startElement, "level");
						String type = getXmlElement(startElement, "type");
						String longtitudeGCJ = getXmlElement(startElement,
								"longtitudeGCJ");
						String latitudeGCJ = getXmlElement(startElement,
								"latitudeGCJ");
						String longtitudeWGS = getXmlElement(startElement,
								"longtitudeWGS");
						String latitudeWGS = getXmlElement(startElement,
								"latitudeWGS");

						if (code == null || code.equals("")
								|| parentCode == null || parentCode.equals("")
								|| name == null || name.equals("")) {
							String errorString = "Dict load error!Row number:"
									+ rowNumber + " code:" + code
									+ ",parentCode:" + parentCode + ",name:"
									+ name;
							System.out.println(errorString);
						} else {
							Location location = new Location(code, parentCode,
									name, shortName, alias, level, type,
									longtitudeGCJ, latitudeGCJ, longtitudeWGS,
									latitudeWGS);
				
							locMap.put(location.getCode(), location);
							
						
							//
							String locName = location.getName();
							if (!"".equals(location.getShortName())) {
								locName += "," + location.getShortName();
							}
							if (!"".equals(location.getAlias())) {
								locName += "," + location.getAlias();
							}
							String locNameArray[] = locName.split(",");
							for (int locIndex = 0; locIndex < locNameArray.length; locIndex++) {
								String nameStr = locNameArray[locIndex];
								if (locationWordMap.get(nameStr) == null) {
									HashSet<Location> locSet = new HashSet();
									locationWordMap.put(nameStr, locSet);
								}
								locationWordMap.get(nameStr).add(location);
							}
						}
					}
				}
			}
			// System.out.println("Load file rownumber:" + rowNumber);
			inputStream.close();
		} catch (Exception e) {
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

	private List<String> getDictFileNames(JSONObject dictObject) {
		List<String> fileNames = new ArrayList<>();

		String type = (String) dictObject.get("type");
		// System.out.println("type:" + type);
		if (type.equals("xml")) {
			fileNames.add((String) dictObject.get("file"));
			// System.out.println((String) dictObject.get("file"));
		} else if (type.equals("group")) {
			JSONArray dictGroup = (JSONArray) dictObject.get("dicts");
			for (Object obj : dictGroup) {
				fileNames.addAll(getDictFileNames((JSONObject) obj));
			}
		}
		return fileNames;
	}

}
