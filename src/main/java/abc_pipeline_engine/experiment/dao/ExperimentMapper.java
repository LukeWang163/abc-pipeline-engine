package abc_pipeline_engine.experiment.dao;

import abc_pipeline_engine.experiment.data.Experiment;

import java.util.List;
import java.util.Map;


/**
 * The interface is to operate the node.
 * @author zhang_xurj
 *
 */
public interface ExperimentMapper {
	
	public List<Experiment> queryByParam(Map<String, Object> param);

	public void add(Experiment experiment);

	public void update(Experiment experiment);

	public void remove(String id);

	public Experiment getOne(String id);

	public Experiment getOneByName(Map<String, Object> param);

}
