package abc_pipeline_engine.node.dao;

import abc_pipeline_engine.node.data.Node;

import java.util.List;
import java.util.Map;


public interface NodeMapper {
	
	public List<Node> queryByParam(Map<String, Object> param);
	
	public void add(Node node);
	
	public void update(Node node);

	public void remove(String id);
	
	public void removeByExperiment(String expId);
	
	public Node getOne(String id);
	
	public void updateParam(Node node);
	
	public void updatePosition(Node node);
	
	public void updateLog(Node node);

	public void updateGraph(Node node);
}
