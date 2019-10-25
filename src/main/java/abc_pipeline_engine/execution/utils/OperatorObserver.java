package abc_pipeline_engine.execution.utils;

import abc_pipeline_engine.experiment.common.ExperimentConstants;
import abc_pipeline_engine.node.dao.NodeMapper;
import abc_pipeline_engine.node.dao.PortMapper;
import abc_pipeline_engine.node.data.Node;
import abc_pipeline_engine.node.data.Port;
import abc_pipeline_engine.utils.ExampleSetUtil;
import abc_pipeline_engine.utils.forRP.ObjectHdfsSource;
import base.operators.OperatorLogMapper;
import base.operators.example.ExampleSet;
import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.tools.Observable;
import base.operators.tools.Observer;
import org.loushang.framework.util.SpringContextHolder;

import java.util.HashMap;
import java.util.Map;

public class OperatorObserver<A extends Operator> implements Observer<A> {
	
	private NodeMapper nodeMapper=SpringContextHolder.getBean(NodeMapper.class);
	private PortMapper portMapper=SpringContextHolder.getBean(PortMapper.class);

    @Override
    public void update(Observable<A> observable, A arg){

        Operator operator = (Operator)observable;

        if(operator.isError()){
        	if(operator.getId()==null) {
        		return;
        	}
        	Node node = nodeMapper.getOne(operator.getId());
        	node.setStatus(ExperimentConstants.OP_STAUTS_ERROR);
        	node.setLog(((OperatorLogMapper) operator.myLog).logString);
        	nodeMapper.update(node);
        	
            return;
        }

        if(operator.isRunning()) {
        	
        	if(operator.getId()==null) {
        		return;
        	}
        	Node node = nodeMapper.getOne(operator.getId());
        	node.setStatus(ExperimentConstants.OP_STAUTS_RUNNING);
        	nodeMapper.update(node);

        	
            return;
        }

        if(operator.isCompleted()) {

        	if(operator.getId()==null) {
        		return;
        	}
        	if(operator.getApplyCount() > 1){
        	    return;
            }

            System.out.println(operator.getName() + ":" + operator.getApplyCount());
        	Node node = nodeMapper.getOne(operator.getId());
        	node.setStatus(ExperimentConstants.OP_STAUTS_OK);
        	node.setLog(((OperatorLogMapper) operator.myLog).logString);
        	nodeMapper.update(node);

            ProcessUtil.cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                	// 写入中间数据
                    try{
                    if (operator.getProcess().getStoreType() == 0) {
                            operator.outPaths.forEach((k, v) -> {
                                // 更新port表
                                Map<String, Object> params = new HashMap<String,Object>();
                                params.put("portType", "output");
                                params.put("sequence", Integer.parseInt(k)+1);
                                params.put("nodeId", operator.getId());
                                Port port = portMapper.queryByParam(params).get(0);
                                port.setViewPath(v);
                                portMapper.update(port);
                                // 写入数据
                                IOObject object = operator.getOutputPorts().getPortByIndex(Integer.parseInt(k)).getAnyDataOrNull();
                                if (object != null) {
                                    if (object instanceof ExampleSet) {
                                        try {
//                                            ParquetExampleSourceUtil.writeToParquet((ExampleSet) object, v, false);
                                            ExampleSetUtil.writeExampleSetToHDFS((ExampleSet) object, v);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                    else {
                                        ObjectHdfsSource.writeToHDFS(object, v);
                                    }
                                }
                            });

                         }

                    }catch (Exception e){

                    }finally {
                        synchronized (operator.writeCount) {
                            operator.writeCount.incrementAndGet();
                            operator.writeCount.notifyAll();
                        }

                    }
//                    operator.freeMemory();
                }
            });


        }

    }

}
