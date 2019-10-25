package abc_pipeline_engine.server.dao;

import abc_pipeline_engine.server.data.Server;

public interface ServerMapper {
    int deleteByPrimaryKey(Integer EXECUTOR_ID);

    int insert(Server record);

    Server selectByExecuteIP(String EXECUTOR_IP);

    int insertSelective(Server record);

    Server selectByPrimaryKey(Integer EXECUTOR_ID);

    int updateByPrimaryKeySelective(Server record);

    int updateByPrimaryKey(Server record);
}