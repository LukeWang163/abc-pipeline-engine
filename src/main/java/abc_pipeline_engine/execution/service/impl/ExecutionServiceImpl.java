package abc_pipeline_engine.execution.service.impl;

import abc_pipeline_engine.execution.dao.ExecutionMapper;
import abc_pipeline_engine.execution.service.IExecutionService;
import abc_pipeline_engine.execution.utils.ProcessUtil;
import abc_pipeline_engine.experiment.common.ExperimentConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service("executionService")
public class ExecutionServiceImpl implements IExecutionService {

    @Autowired
    private ExecutionMapper executionMapper;

    @Override
    public Map<String, Object> executeProcess(String expID) {
        Map<String,Object> resultMap=new LinkedHashMap<>();

        // TODO: get prpocessXml from database
//        String processXml = "";
//        String processStatus = ProcessUtil.runProcessString(processXml, expID);

        // TODO: put real status
        resultMap.put("processStatus", ExperimentConstants.EXP_STAUTS_SUCCESS);

        return resultMap;
    }
}
