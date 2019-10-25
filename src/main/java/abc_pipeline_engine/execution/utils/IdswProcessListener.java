package abc_pipeline_engine.execution.utils;

import abc_pipeline_engine.experiment.common.ExperimentConstants;
import abc_pipeline_engine.experiment.dao.ExperimentMapper;
import abc_pipeline_engine.experiment.data.Experiment;
import abc_pipeline_engine.node.dao.NodeMapper;
import abc_pipeline_engine.node.dao.PortMapper;
import abc_pipeline_engine.node.data.Node;
import abc_pipeline_engine.node.data.Port;
import abc_pipeline_engine.utils.DateTimeUtil;
import abc_pipeline_engine.utils.ExampleSetUtil;
import abc_pipeline_engine.utils.forRP.ObjectHdfsSource;
import base.operators.OperatorLogMapper;
import base.operators.Process;
import base.operators.ProcessStateListener;
import base.operators.example.ExampleSet;
import base.operators.operator.IOObject;
import org.loushang.framework.util.SpringContextHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zls
 * create time:  2019.09.25.
 * description:
 */
public class IdswProcessListener<enableCache> implements ProcessStateListener {
	
	public String processStatus = ExperimentConstants.EXP_STAUTS_KILLED;

	private ExperimentMapper experimentMapper = SpringContextHolder.getBean(ExperimentMapper.class);
    private NodeMapper nodeMapper = SpringContextHolder.getBean(NodeMapper.class);
	private PortMapper portMapper = SpringContextHolder.getBean(PortMapper.class);
	public String id = null ;

    public  IdswProcessListener(String id){
        this.id = id;
    }

    @Override
    public void started(Process process) {
    	//TODO
    }

    @Override
    public void paused(Process process) {
    	// TODO
    }

    @Override
    public void resumed(Process process) {
    	//TODO
    }

    @Override
    public void stopped(Process process) {
    	if(process.isSuccessful()) {
    		processStatus = ExperimentConstants.EXP_STAUTS_SUCCESS;
    	}
    	String expId = this.id;
    	
        if(process.isSuccessful()) {
            Experiment experiment = experimentMapper.getOne(expId);
            experiment.setStatus(ExperimentConstants.EXP_STAUTS_SUCCESS);
            experiment.setUpdateTime(DateTimeUtil.getCurrentTime());
            experimentMapper.update(experiment);
        }else {
            Experiment experiment = experimentMapper.getOne(expId);
            experiment.setStatus(ExperimentConstants.EXP_STAUTS_KILLED);
            experiment.setUpdateTime(DateTimeUtil.getCurrentTime());
            experimentMapper.update(experiment);
        }
        ProcessUtil.cachedThreadPool.execute(new Runnable() {
			@Override
			public void run() {


				process.getRootOperator().getAllInnerOperators().forEach(operator -> {
                    //TODO：会不会死循环

                    synchronized (operator.writeCount){
                        if(operator.outPaths.size() > 0) {
                            while (operator.writeCount.get() < 1) {
                                try {
                                    operator.writeCount.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

				    if(operator.getApplyCount() <= 1){
                        operator.freeMemory();
                    }else {
                        System.out.println(operator.getName() + ":" + operator.getApplyCount());
                        if (operator.getProcess().getStoreType() == 0) {
                            Node node = nodeMapper.getOne(operator.getId());
                            node.setStatus(ExperimentConstants.OP_STAUTS_OK);
                            node.setLog(((OperatorLogMapper) operator.myLog).logString);
                            nodeMapper.update(node);


                            operator.outPaths.forEach((k, v) -> {
                                try {
                                    // 更新port表
                                    Map<String, Object> params = new HashMap<String, Object>();
                                    params.put("portType", "output");
                                    params.put("sequence", Integer.parseInt(k) + 1);
                                    params.put("nodeId", operator.getId());
                                    Port port = portMapper.queryByParam(params).get(0);
                                    port.setViewPath(v);
                                    portMapper.update(port);
                                    // 写入数据
                                    IOObject object = operator.getOutputPorts().getPortByIndex(Integer.parseInt(k)).getAnyDataOrNull();
                                    if (object != null) {
                                        if (object instanceof ExampleSet) {
                                            ExampleSetUtil.writeExampleSetToHDFS((ExampleSet) object, v);
                                        } else {
                                            ObjectHdfsSource.writeToHDFS(object, v);
                                        }
                                    }

                                } catch (Exception e) {
                                    System.err.println("写入错误" + operator.getName());
                                    e.printStackTrace();
                                }
                            });
                            operator.freeMemory();

                        }
                    }
				});
			}
		});
    }

}
