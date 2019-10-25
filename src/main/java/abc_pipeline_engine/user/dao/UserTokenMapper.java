package abc_pipeline_engine.user.dao;

import java.util.List;
import java.util.Map;

public interface UserTokenMapper {
	List<Map<String,String>> queryByParam(Map<String, String> params);
	
	void insert(Map<String, String> params);

	void remove(String userId);

	void update(Map<String, String> params);

	String getUserToken(String userId);
}
