package abc_pipeline_engine.executor.dao;

import abc_pipeline_engine.executor.data.Executor;

public interface ExecutorMapper {
    int deleteByPrimaryKey(Integer EXECUTOR_ID);

    int insert(Executor record);

    Executor selectByExecuteIP(String EXECUTOR_IP);

    int insertSelective(Executor record);

    Executor selectByPrimaryKey(Integer EXECUTOR_ID);

    int updateByPrimaryKeySelective(Executor record);

    int updateByPrimaryKey(Executor record);
}