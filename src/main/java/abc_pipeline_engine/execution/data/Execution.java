package abc_pipeline_engine.execution.data;

public class Execution {
    /**
     * 该条记录的id
     */
    private Integer ID;

    /**
     * 流程id
     */
    private String PIPELINE_ID;

    /**
     * 运行该流程的executor
     */
    private Integer EXECUTOR_ID;

    /**
     * 流程状态（0：准备，1：执行中，2：成功，3：失败）
     */
    private String PIPELINE_STATUS;

    /**
     * 流程分发到执行节点的时间
     */
    private String CREATE_TIME;

    /**
     * 状态更新时间
     */
    private String UPDATE_TIME;

    /**
     * 该条记录的id
     * @return ID 该条记录的id
     */
    public Integer getID() {
        return ID;
    }

    /**
     * 该条记录的id
     * @param ID 该条记录的id
     */
    public void setID(Integer ID) {
        this.ID = ID;
    }

    /**
     * 流程id
     * @return PIPELINE_ID 流程id
     */
    public String getPIPELINE_ID() {
        return PIPELINE_ID;
    }

    /**
     * 流程id
     * @param PIPELINE_ID 流程id
     */
    public void setPIPELINE_ID(String PIPELINE_ID) {
        this.PIPELINE_ID = PIPELINE_ID == null ? null : PIPELINE_ID.trim();
    }

    /**
     * 运行该流程的executor
     * @return EXECUTOR_ID 运行该流程的executor
     */
    public Integer getEXECUTOR_ID() {
        return EXECUTOR_ID;
    }

    /**
     * 运行该流程的executor
     * @param EXECUTOR_ID 运行该流程的executor
     */
    public void setEXECUTOR_ID(Integer EXECUTOR_ID) {
        this.EXECUTOR_ID = EXECUTOR_ID;
    }

    /**
     * 流程状态（0：准备，1：执行中，2：成功，3：失败）
     * @return PIPELINE_STATUS 流程状态（0：准备，1：执行中，2：成功，3：失败）
     */
    public String getPIPELINE_STATUS() {
        return PIPELINE_STATUS;
    }

    /**
     * 流程状态（0：准备，1：执行中，2：成功，3：失败）
     * @param PIPELINE_STATUS 流程状态（0：准备，1：执行中，2：成功，3：失败）
     */
    public void setPIPELINE_STATUS(String PIPELINE_STATUS) {
        this.PIPELINE_STATUS = PIPELINE_STATUS == null ? null : PIPELINE_STATUS.trim();
    }

    /**
     * 流程分发到执行节点的时间
     * @return CREATE_TIME 流程分发到执行节点的时间
     */
    public String getCREATE_TIME() {
        return CREATE_TIME;
    }

    /**
     * 流程分发到执行节点的时间
     * @param CREATE_TIME 流程分发到执行节点的时间
     */
    public void setCREATE_TIME(String CREATE_TIME) {
        this.CREATE_TIME = CREATE_TIME == null ? null : CREATE_TIME.trim();
    }

    /**
     * 状态更新时间
     * @return UPDATE_TIME 状态更新时间
     */
    public String getUPDATE_TIME() {
        return UPDATE_TIME;
    }

    /**
     * 状态更新时间
     * @param UPDATE_TIME 状态更新时间
     */
    public void setUPDATE_TIME(String UPDATE_TIME) {
        this.UPDATE_TIME = UPDATE_TIME == null ? null : UPDATE_TIME.trim();
    }
}