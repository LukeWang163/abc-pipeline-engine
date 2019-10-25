package abc_pipeline_engine.node.data;

public class Node {
	private String id;
	private String operatorId;
	private String params;
	private int x;
	private int y;
	private String text;
	private int inputs;
	private int outputs;
	private String icon;
	private String expId;
	private String status;
	private String log;
	private String origin;
	private String isSubprocess;
	private String parentId;
	private String nodeGraph;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getOperatorId() {
		return operatorId;
	}
	public void setOperatorId(String operatorId) {
		this.operatorId = operatorId;
	}
	public String getParams() {
		return params;
	}
	public void setParams(String params) {
		this.params = params;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public int getInputs() {
		return inputs;
	}
	public void setInputs(int inputs) {
		this.inputs = inputs;
	}
	public int getOutputs() {
		return outputs;
	}
	public void setOutputs(int outputs) {
		this.outputs = outputs;
	}
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public String getExpId() {
		return expId;
	}
	public void setExpId(String expId) {
		this.expId = expId;
	}
	
	
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getLog() {
		return log;
	}
	public void setLog(String log) {
		this.log = log;
	}
	public String getOrigin() {
		return origin;
	}
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	public String getIsSubprocess() {
		return isSubprocess;
	}
	public void setIsSubprocess(String isSubprocess) {
		this.isSubprocess = isSubprocess;
	}
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	public String getNodeGraph() {
		return nodeGraph;
	}
	public void setNodeGraph(String nodeGraph) {
		this.nodeGraph = nodeGraph;
	}
	
	
	
	
}
