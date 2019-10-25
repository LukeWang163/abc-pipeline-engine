package base.operators.operator.nlp.pyconvert.py2cn;

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
public class DictManagerPinyin {
	/**
	 * Dict Name
	 */
	protected String name;
	/**
	 * Dict List Chain
	 */
	protected List<IdentityHashMap<String, String>> dictChain;
	/**
	 * Dict Map of All Files
	 */
	protected IdentityHashMap<String, String> allDictMap = new IdentityHashMap<>();
	
	protected List<String> dat_dict = new ArrayList<String>();
	protected DoubleArrayTrie DOUBLE_ARRAY_TRIE = new DoubleArrayTrie();


	/**
	 * dictChain
	 */
	public DictManagerPinyin() {
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
	public IdentityHashMap<String, String> getAllDictMap() {
		return allDictMap;
	}
	
	public DoubleArrayTrie getDat() {
		return DOUBLE_ARRAY_TRIE;
	}
	public List<String> getDict() {
		return dat_dict;
	}
	
	public void setConfig() {

		dictChain.clear();
		allDictMap.clear();
		
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
		JSONParser jsonParser = new JSONParser();

		List<String> dictFileNames = new ArrayList<>();
		// Load Config File Start
		try {
			//String configFile = "/nlp/py2cn/config/" + "PinyinConvertChinese" + ".json";;
			InputStream inStream = ReadFileAsStream.readPinyinConvertChineseJson();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
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
			IdentityHashMap<String, String> dict = new IdentityHashMap<>();
			//fileName = "/nlp/py2cn/dictionary/" + fileName;
			InputStream inStream = null;
			Scanner sc = null;
			try {
				inStream = ReadFileAsStream.readPinyinConvertChineseDict(fileName);
				BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
				String row;
				while ((row = in.readLine()) != null) {
										
					int tabCount = 0;
					int len = row.length();
					for(int j = 0;j < len;j++){
						char tem = row.charAt(j);						
						 if(tem == '\t'){
							 tabCount++;
						 }						
					}			
					if(tabCount >= 2){
						String[] words = row.trim().split("\t");					
						if (words.length >= 2)
							dict.put(words[0], words[1]+"\t"+words[2]+" ");
						}
				}
				dictChain.add(dict);
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
		for (IdentityHashMap<String, String> dictMap : getDictChain()) {
			Iterator it = dictMap.keySet().iterator();
			
			while (it.hasNext()) {
				String word = (String) it.next();
				String mappingWord = dictMap.get(word);
				allDictMap.put(word, mappingWord);										
			}	
			
		}	
		

		for(String dat : allDictMap.keySet()){
			dat_dict.add(dat);
		}
		Collections.sort(dat_dict); 
		DOUBLE_ARRAY_TRIE.build(dat_dict);

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
	public List<IdentityHashMap<String, String>> getDictChain() {
		return dictChain;
	}	

}