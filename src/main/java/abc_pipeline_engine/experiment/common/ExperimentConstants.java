package abc_pipeline_engine.experiment.common;

public class ExperimentConstants {

	// Experiment_INST status
	public static final String EXP_STAUTS_DRAFT = "DRAFT";
	public static final String EXP_STAUTS_SUCCESS = "SUCCESS";
	public static final String EXP_STAUTS_RUNNING = "RUNNING";
	public static final String EXP_STAUTS_KILLED = "KILLED";

	// Operator status
	public static final String OP_STAUTS_DRAFT = "DRAFT";
	public static final String OP_STAUTS_OK = "OK";
	public static final String OP_STAUTS_RUNNING = "RUNNING";
	public static final String OP_STAUTS_ERROR = "ERROR";

	// 实验中Multiply的相关信息
	public static final String MULTIPLY_CLASS = "base.operators.operator.IOMultiplier";
	public static final String MULTIPLY_NAME = "Multiply";
	public static final String MULTIPLY_OUTPUT = "output";
	public static final String MULTIPLY_INPUT = "input";
	
	public static final String FROM_OP_PORTTYPE = "output";
	public static final String TO_OP_PORTTYPE = "input";
	public static final String RESULT_OP_NAME = "result";
	public static final String RESULT_OP_PORTTYPE = "output";

	// 子流程算子的标志位 1： 子算子 0：普通算子
	public static final String SUBPROCESS_NODE_FLAG = "1";
	
	// 子流程中开始结束的逻辑节点ID
	public static final String SUBPROCESS_LOGIC_START_NODE_ID = "1567219780574";
	public static final String SUBPROCESS_LOGIC_END_NODE_ID = "1567219794308";
	
	// 主页面算子的PARENT_ID
	public static final String MAIN_PARENT_ID = "root";

}
