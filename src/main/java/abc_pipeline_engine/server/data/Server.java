package abc_pipeline_engine.server.data;

public class Server {
    /**
     * 执行节点id
     */
    private Integer EXECUTOR_ID;

    /**
     * 执行节点ip
     */
    private String EXECUTOR_IP;

    /**
     * 执行节点端口
     */
    private String EXECUTOR_PORT;

    /**
     * 该节点机器是否可用（0：否，1：是）
     */
    private String IS_ACTIVE;

    /**
     * 执行节点id
     * @return EXECUTOR_ID 执行节点id
     */
    public Integer getEXECUTOR_ID() {
        return EXECUTOR_ID;
    }

    /**
     * 执行节点id
     * @param EXECUTOR_ID 执行节点id
     */
    public void setEXECUTOR_ID(Integer EXECUTOR_ID) {
        this.EXECUTOR_ID = EXECUTOR_ID;
    }

    /**
     * 执行节点ip
     * @return EXECUTOR_IP 执行节点ip
     */
    public String getEXECUTOR_IP() {
        return EXECUTOR_IP;
    }

    /**
     * 执行节点ip
     * @param EXECUTOR_IP 执行节点ip
     */
    public void setEXECUTOR_IP(String EXECUTOR_IP) {
        this.EXECUTOR_IP = EXECUTOR_IP == null ? null : EXECUTOR_IP.trim();
    }

    /**
     * 执行节点端口
     * @return EXECUTOR_PORT 执行节点端口
     */
    public String getEXECUTOR_PORT() {
        return EXECUTOR_PORT;
    }

    /**
     * 执行节点端口
     * @param EXECUTOR_PORT 执行节点端口
     */
    public void setEXECUTOR_PORT(String EXECUTOR_PORT) {
        this.EXECUTOR_PORT = EXECUTOR_PORT == null ? null : EXECUTOR_PORT.trim();
    }

    /**
     * 该节点机器是否可用（0：否，1：是）
     * @return IS_ACTIVE 该节点机器是否可用（0：否，1：是）
     */
    public String getIS_ACTIVE() {
        return IS_ACTIVE;
    }

    /**
     * 该节点机器是否可用（0：否，1：是）
     * @param IS_ACTIVE 该节点机器是否可用（0：否，1：是）
     */
    public void setIS_ACTIVE(String IS_ACTIVE) {
        this.IS_ACTIVE = IS_ACTIVE == null ? null : IS_ACTIVE.trim();
    }
}