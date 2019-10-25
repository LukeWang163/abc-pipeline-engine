package abc_pipeline_engine.node.data;

import java.util.List;

public class NodeView {

	private String id;
	private String text;
	private int x;
	private int y;
	private int inputs;
	private int outputs;
	private String icon;
	private String expId;
	private String params;
	private String origin;
	
	private String name;
	private String nodeDefId;
	private String codeName;
	private String fullName;
	private String pagePath;
	private String executePath;
	private String description;
	
	private String isSubprocess;
	private String parentId;

	private String paramDom;
	private String paramJs;
	private String paramCss;
	
	
	private List<Port> inPorts;
	private List<Port> outPorts;
	private int subInputs;
	private int subOutputs;
	private List<Port> subInPorts;
	private List<Port> subOutPorts;
	
	private String status;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
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
	
	public String getParams() {
		return params;
	}
	public void setParams(String params) {
		this.params = params;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
	public String getNodeDefId() {
		return nodeDefId;
	}
	public void setNodeDefId(String nodeDefId) {
		this.nodeDefId = nodeDefId;
	}
	public String getCodeName() {
		return codeName;
	}
	public void setCodeName(String codeName) {
		this.codeName = codeName;
	}

	
	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getPagePath() {
		return pagePath;
	}
	public void setPagePath(String pagePath) {
		this.pagePath = pagePath;
	}
	public String getExecutePath() {
		return executePath;
	}
	public void setExecutePath(String executePath) {
		this.executePath = executePath;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<Port> getInPorts() {
		return inPorts;
	}
	public void setInPorts(List<Port> inPorts) {
		this.inPorts = inPorts;
	}
	public List<Port> getOutPorts() {
		return outPorts;
	}
	public void setOutPorts(List<Port> outPorts) {
		this.outPorts = outPorts;
	}
	
	public int getSubInputs() {
		return subInputs;
	}
	public void setSubInputs(int subInputs) {
		this.subInputs = subInputs;
	}
	public int getSubOutputs() {
		return subOutputs;
	}
	public void setSubOutputs(int subOutputs) {
		this.subOutputs = subOutputs;
	}
	public List<Port> getSubInPorts() {
		return subInPorts;
	}
	public void setSubInPorts(List<Port> subInPorts) {
		this.subInPorts = subInPorts;
	}
	public List<Port> getSubOutPorts() {
		return subOutPorts;
	}
	public void setSubOutPorts(List<Port> subOutPorts) {
		this.subOutPorts = subOutPorts;
	}
	public String getOrigin() {
		return origin;
	}
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getParamDom() {
		return paramDom;
	}
	public void setParamDom(String paramDom) {
		this.paramDom = paramDom;
	}
	public String getParamJs() {
		return paramJs;
	}
	public void setParamJs(String paramJs) {
		this.paramJs = paramJs;
	}
	public String getParamCss() {
		return paramCss;
	}
	public void setParamCss(String paramCss) {
		this.paramCss = paramCss;
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
}
