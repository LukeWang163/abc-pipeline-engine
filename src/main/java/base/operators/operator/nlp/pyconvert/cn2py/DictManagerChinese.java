package base.operators.operator.nlp.pyconvert.cn2py;

import idsw.nlp.read.ReadFileAsStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Dictionary holds the mappings for converting Chinese characters
 */
public class DictManagerChinese {
	/**
	 * Dict Name
	 */
	protected String name;
	/**
	 * Dict List Chain
	 */
	protected List<SortedMap<String, String>> dictChain;
	/**
	 * Dict Map of All Files
	 */
	protected TreeMap<String, String> allDictMap = new TreeMap<>();
	/**
	 * Conflict Mappings
	 */
	private Set confSets = new HashSet();

/**
 * dictChain
 */
	public DictManagerChinese() {

		dictChain = new ArrayList<>();

		name = "";

		setConfig();
	}

	/**
	 *
	 * @return dict name
	 */
	public String getDictName() {
		return name;
	}

	/**
	 * get Dict Name
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * get All Dict Mappings
	 * 
	 * @return
	 */
	public TreeMap<String, String> getAllDictMap() {
		return allDictMap;
	}

	/**
	 * get Conflict Mappings
	 * 
	 * @return
	 */
	public Set getConfSets() {
		return confSets;
	}


	public void setConfig() {

		dictChain.clear();
		allDictMap.clear();
		confSets.clear();
		
		// load Dict
		loadDict();
	}

	/**
	 * load dictionary files into dictChain
	 */
	private void loadDict() {
		// dict Reset
		dictChain.clear();
		allDictMap.clear();
		confSets.clear();

		JSONParser jsonParser = new JSONParser();

		List<String> dictFileNames = new ArrayList<>();
		// Load Config File Start
		try {
			//String configFile = "/nlp/cn2py/config/" + "ChineseConvertPinyin" + ".json";
			InputStream inStream = ReadFileAsStream.readChineseConvertPinyinJson();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inStream,"utf-8"));
			Object object = jsonParser.parse(reader);
			JSONObject dictRoot = (JSONObject) object;
			name = (String) dictRoot.get("name");
			JSONArray jsonArray = (JSONArray) dictRoot.get("conversion_chain");
			for (Object obj : jsonArray) {
				JSONObject dictObj = (JSONObject) ((JSONObject) obj).get("dict");
				dictFileNames.addAll(getDictFileNames(dictObj));
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		// Begin Load Dicts
		for (String fileName : dictFileNames) {
			TreeMap<String, String> dict = new TreeMap<>();
			//fileName = "/nlp/cn2py/dictionary/" + fileName;
			InputStream inStream = null;
			Scanner sc = null;
			try {
				inStream = ReadFileAsStream.readChineseConvertPinyinDict(fileName);
				BufferedReader in = new BufferedReader(new InputStreamReader(inStream,"utf-8"));
				String row;
				while ((row = in.readLine()) != null) {
					String[] words = row.trim().split("\t");
					if (words.length >= 2)
						dict.put(words[0], words[1]);
				}
				dictChain.add(Collections.unmodifiableSortedMap(dict));

			} catch (Exception e) {

			} finally {
				if (inStream != null) {
					try {
						inStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (sc != null) {
					sc.close();
				}
			}

		}
		// Init Mappings and check Conflictings
		initHelper();
	}

	private void initHelper() {
		// Merge Mappings
		for (SortedMap<String, String> dictMap : getDictChain()) {
			Iterator it = dictMap.keySet().iterator();
			while (it.hasNext()) {
				String word = (String) it.next();
				String mappingWord = dictMap.get(word);
				allDictMap.put(word, mappingWord);
			}
		}
		// Check Confilct
		Map posibleConflictMap = new HashMap();
		Iterator iter = allDictMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			if (key.length() >= 1) {
				for (int i = 0; i < (key.length()); i++) {
					String keySubstring = key.substring(0, i + 1);
					if (posibleConflictMap.containsKey(keySubstring)) {
						Integer integer = (Integer) (posibleConflictMap.get(keySubstring));
						posibleConflictMap.put(keySubstring, new Integer(integer.intValue() + 1));
					} else {
						posibleConflictMap.put(keySubstring, new Integer(1));
					}

				}
			}
		}
		iter = posibleConflictMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			if (((Integer) (posibleConflictMap.get(key))).intValue() > 1) {
				confSets.add(key);
			}
		}
	}

	private List<String> getDictFileNames(JSONObject dictObject) {
		List<String> fileNames = new ArrayList<>();

		String type = (String) dictObject.get("type");

		if (type.equals("txt")) {
			fileNames.add((String) dictObject.get("file"));
		} else if (type.equals("group")) {
			JSONArray dictGroup = (JSONArray) dictObject.get("dicts");
			for (Object obj : dictGroup) {
				fileNames.addAll(getDictFileNames((JSONObject) obj));
			}
		}

		return fileNames;
	}

	/**
	 *
	 * @return dictChain
	 */
	public List<SortedMap<String, String>> getDictChain() {
		return dictChain;
	}

}