package abc_pipeline_engine.experiment.dao;

import abc_pipeline_engine.experiment.data.ExperimentRun;

import java.util.List;
import java.util.Map;

public interface ExperimentRunMapper {
	
	public List<ExperimentRun> queryByParam(Map<String, Object> params);
	
	public ExperimentRun getOne(String runId);
	
	public void save(ExperimentRun run);
	
	public void update(ExperimentRun run);
	
	public void remove(String runId);

}
