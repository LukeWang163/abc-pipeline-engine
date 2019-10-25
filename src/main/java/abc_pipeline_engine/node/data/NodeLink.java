package abc_pipeline_engine.node.data;

/**
 * NodeLink represents the link between two nodes.
 * @author zhang_xurj
 *
 */
public class NodeLink {
	
	private String id;
	private String expId;
	private String sourceId;
	private String outputPortId;
	private String targetId;
	private String inputPortId;
	private String parentId;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getExpId() {
		return expId;
	}
	public void setExpId(String expId) {
		this.expId = expId;
	}
	public String getSourceId() {
		return sourceId;
	}
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}
	public String getOutputPortId() {
		return outputPortId;
	}
	public void setOutputPortId(String outputPortId) {
		this.outputPortId = outputPortId;
	}
	public String getTargetId() {
		return targetId;
	}
	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}
	public String getInputPortId() {
		return inputPortId;
	}
	public void setInputPortId(String inputPortId) {
		this.inputPortId = inputPortId;
	}
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	
	
}
