package base.operators.operator.nlp.synonym;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import idsw.nlp.read.ReadFileAsStream;

import java.io.*;
import java.util.*;

public class Synonym {

	public static Map<String, List<Map<String, Object>>> synonymMap = new HashMap<String, List<Map<String, Object>>>();
	
	
	private static volatile Synonym defaultInstance;
	
	//private String dictPath = "/nlp/synonym/dict/synonym.txt";
	
	private static int type = 0; // 词典加载方式，0：系统词典；1：自定义词典；2合并
	
	//定义一个共有的静态方法，返回该类型实例
	public static Synonym getInstance(List<Map<String, String>> dicts, int dictType) {
		// 对象实例化时与否判断（不使用同步代码块，instance不等于null时，直接返回对象，提高运行效率）
		if (defaultInstance == null) {
			//同步代码块（对象未初始化时，使用同步代码块，保证多线程访问时对象在第一次创建后，不再重复被创建）
			synchronized (Synonym.class) {
				//未初始化，则初始instance变量
				if (defaultInstance == null) {
					type = dictType;
					defaultInstance = new Synonym(dicts);
				}
			}
		}
		return defaultInstance;
	}
	
	private Synonym(List<Map<String, String>> dicts) {
		if(type == 0 || type == 2) {
			initDict();
		}
		if(type == 1 || type == 2) {
			if(null != dicts && dicts.size() > 0) {
				for(int i=0; i<dicts.size(); i++) {
					Map<String, String> item = dicts.get(i);
					String bigCode = item.get("bigcode");
					String bigCat = item.get("bigcat");
					String midCode = item.get("midcode");
					String midCat = item.get("midcat");
					String smallCode = item.get("smallcode");
					String smallCat = item.get("smallcat");
					String pos = item.get("pos");
					String words = item.get("synonym");
					String items[] = words.split(" ");
					List<String> itemList=Arrays.asList(items);
					Map<String, Object> term = new HashMap<String, Object>();
					term.put("pos", pos);
					term.put("bigCat", bigCat);
					term.put("midCat", midCat);
					term.put("smallCat", smallCat);
					term.put(bigCode, bigCat);
					term.put(midCode, midCat);
					term.put(smallCode, smallCat);
					for(int j=0; j<itemList.size(); j++) {
						List<String> wordList = new ArrayList<String>();
						for(int k = 0; k<itemList.size(); k++) {
							if(j != k) {
								wordList.add(itemList.get(k));
							}
						}
						if(synonymMap.containsKey(itemList.get(j))) {
							List<Map<String, Object>> map = synonymMap.get(itemList.get(j));
							term.remove("synonym");
							term.put("synonym", wordList);
							map.add(term);
							synonymMap.put(itemList.get(j), map);
						} else {
							List<Map<String, Object>> synList = new ArrayList<Map<String, Object>>();
							term.remove("synonym");
							term.put("synonym", wordList);
							synList.add(term);
							synonymMap.put(itemList.get(j), synList);
						}
					}
				}
			}
		}
	}

	private void initDict() {
		InputStream inStream = ReadFileAsStream.readSynonymDict();
		try {
			BufferedReader dictReader = new BufferedReader(new InputStreamReader(inStream,"UTF-8"));
			String row;
			while ((row = dictReader.readLine()) != null) {
				String[] params = row.split("\t");
				if(params.length < 14) {
					System.out.println(row);
				}
				String bigCode = params[0];
				String bigCat = params[1];
				String midCode = params[3];
				String midCat = params[4];
				String smallCode = params[6];
				String smallCat = params[7];
				String pos = params[13];
				String words = params[12];
				String items[] = words.split(" ");
				Map<String, Object> term = new HashMap<String, Object>(); 
				//term.put("code", code);
				term.put("pos", pos);
				term.put("bigCat", bigCat);
				term.put("midCat", midCat);
				term.put("smallCat", smallCat);
				term.put(bigCode, bigCat);
				term.put(midCode, midCat);
				term.put(smallCode, smallCat);
				List<String> itemList=Arrays.asList(items);
				
				for(int j=0; j<itemList.size(); j++) {
					List<String> wordList = new ArrayList<String>();
					for(int k = 0; k<itemList.size(); k++) {
						if(j != k) {
							wordList.add(itemList.get(k));
						}
					}
					if(synonymMap.containsKey(itemList.get(j))) {
						List<Map<String, Object>> map = synonymMap.get(itemList.get(j));
						term.remove("synonym");
						term.put("synonym", wordList);
						map.add(term);
						synonymMap.put(itemList.get(j), map);
					} else {
						List<Map<String, Object>> synList = new ArrayList<Map<String, Object>>();
						term.remove("synonym");
						term.put("synonym", wordList);
						synList.add(term);
						synonymMap.put(itemList.get(j), synList);
					}
				}
			}
			dictReader.close();
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		
	}
	
	public JSONObject searchSynonym(String word) {
		JSONObject obj = new JSONObject();
		JSONArray list = new JSONArray();
		List<Map<String, Object>> termList = synonymMap.get(word);
		List<String> catList = new ArrayList<String>();
		if(termList != null) {
			for(int i=0; i<termList.size(); i++) {
				JSONObject item = new JSONObject();
				Map<String, Object> term = termList.get(i);
				item.put("bigCat", term.get("bigCat"));
				item.put("midCat", term.get("midCat"));
				item.put("smallCat", term.get("smallCat"));
				JSONArray synonymList = new JSONArray();
				item.put("synonyms", synonymList);
				String pos = (String) term.get("pos");
				String cat = (String) term.get("bigCat") + term.get("midCat") + term.get("smallCat");
				List<String> w =  (List<String>) term.get("synonym");
				List<String> w_copy = deepCopy(w);
				w_copy.remove(word);
				if(catList.contains(cat)) {
					int index = catList.indexOf(cat);
					JSONObject temp = list.getJSONObject(index);
					JSONArray synonyms = temp.getJSONArray("synonyms");
					List<String> posList = new ArrayList<String>();
					for(int j=0; j<synonyms.size(); j++) {
						String po = synonyms.getJSONObject(j).getString("pos");
						posList.add(po);
					}
					if(posList.contains(pos)) {
						int num = posList.indexOf(pos);
						JSONObject json = synonyms.getJSONObject(num);
						List<String> words = (List<String>) json.get("words");
						words.addAll(w_copy);
						json.remove("words");
						json.put("words", words);
						
					} else {
						JSONObject json = new JSONObject();
						json.put("pos", pos);
						List<String> words = new ArrayList<String>();
						words.addAll(w_copy);
						json.put("words", words);
						synonyms.add(json);
						posList.add(pos);
						item.put("synonyms", synonyms);
						
					}
				} else {
					JSONObject synonym = new JSONObject();
					synonym.put("pos", pos);
					List<String> words = new ArrayList<String>();
					words.addAll(w_copy);
					synonym.put("words", words);
					JSONArray synonyms = item.getJSONArray("synonyms");
					synonyms.add(synonym);
					item.put("synonyms", synonymList);
					list.add(item);
					catList.add(cat);
				}
				
			}
		}
		obj.put("wordList", list);
		return obj;
	}
	
	private static <T> List<T> deepCopy(List<T> src) {  
	    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();  
	    ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(byteOut);
			out.writeObject(src);  
			  
		    ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());  
		    ObjectInputStream in = new ObjectInputStream(byteIn);  
		    @SuppressWarnings("unchecked")  
		    List<T> dest = (List<T>) in.readObject();  
		    return dest;  
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		return null;
	}

}
