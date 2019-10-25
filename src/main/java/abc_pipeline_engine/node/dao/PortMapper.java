package abc_pipeline_engine.node.dao;

import abc_pipeline_engine.node.data.Port;

import java.util.List;
import java.util.Map;


public interface PortMapper {
	
	public List<Port> queryByParam(Map<String, Object> param);

	public Port getOne(String id);

	public void add(Port instance);

	public void update(Port instance);

	public void remove(String id);

	public void removeByExperiment(String id);

	public void removeByNode(Map<String, Object> param);
	
}
