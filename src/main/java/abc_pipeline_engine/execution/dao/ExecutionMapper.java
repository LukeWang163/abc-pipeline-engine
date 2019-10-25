package abc_pipeline_engine.execution.dao;

import abc_pipeline_engine.execution.data.Execution;

public interface ExecutionMapper {
    int deleteByPrimaryKey(Integer ID);

    int insert(Execution record);

    int insertSelective(Execution record);

    Execution selectByPrimaryKey(Integer ID);

    int updateByPrimaryKeySelective(Execution record);

    int updateByPrimaryKey(Execution record);
}