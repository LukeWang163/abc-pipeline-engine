package abc_pipeline_engine.execution.controller;

import abc_pipeline_engine.execution.dao.ExecutionMapper;
import abc_pipeline_engine.execution.service.IExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
@RequestMapping("/execute")
public class ExecutionController {

	@Autowired
	private ExecutionMapper executionMapper;

	@Autowired
	private IExecutionService executionService;

	@ResponseBody
	@RequestMapping(method = RequestMethod.POST)
	public Map<String, Object> execute(@RequestParam Map<String, Object> params) {

		String expID = params.get("expID").toString();
		Map<String,Object> resultMap = executionService.executeProcess(expID);
		return resultMap;
	}
}