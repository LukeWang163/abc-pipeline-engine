package abc_pipeline_engine.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONUtil {

	public static Map<String, Object> json2map(String json) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(json, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyMap();
	}

	public static Map<String, String> json2mapstring(String json) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(json, new TypeReference<HashMap<String, String>>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyMap();
	}

	public static String bean2json(Object obj) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<Map<String, String>> json2list(String json) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(json, new TypeReference<List<Map<String, String>>>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}
	
	public static List<Map<String,Object>> json2ObjectList(String json){
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

}
