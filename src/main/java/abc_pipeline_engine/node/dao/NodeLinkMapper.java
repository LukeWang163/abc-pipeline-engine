package abc_pipeline_engine.node.dao;

import abc_pipeline_engine.node.data.NodeLink;

import java.util.List;
import java.util.Map;

/**
 * The interface is to operate NodeLink.
 * @author zhang_xurj
 *
 */
public interface NodeLinkMapper {
	
	public NodeLink getOne(String id);
	
	public List<NodeLink> queryByParam(Map<String, Object> param);

	public void add(NodeLink node);

	public void update(NodeLink node);

	public void remove(String id);

	public void removeByNode(Map<String, Object> param);
	
	public void removeNodeLink(NodeLink link);
	
	public void removeByExperiment(String expId);

}
