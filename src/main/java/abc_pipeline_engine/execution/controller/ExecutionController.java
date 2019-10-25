package abc_pipeline_engine.execution.controller;

import abc_pipeline_engine.execution.utils.ProcessUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import org.apache.commons.codec.binary.Base64;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/execute")
public class ExecutionController {

	@ResponseBody
	@RequestMapping(method = RequestMethod.POST)
	public Map<String, Object> execute(@RequestBody Map<String, Object> params) {
		Map<String,Object> resultMap=new LinkedHashMap<String,Object>();
		String expId = params.get("expID").toString();
		//TODO: get from database
		String base64ProcessXml = params.get("processXML").toString();
		String processXml = Base64.decodeBase64(base64ProcessXml).toString();
		// 提交Process Job
		String processStatus = ProcessUtil.runProcessString(processXml, expId);

		resultMap.put("processStatus", processStatus);

		return resultMap;
	}
}