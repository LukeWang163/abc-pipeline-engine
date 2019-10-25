package abc_pipeline_engine.sysinfo.service.impl;

import abc_pipeline_engine.sysinfo.service.ISysInfoService;
import abc_pipeline_engine.utils.OsCpuUtil;
import abc_pipeline_engine.utils.OsMemoryUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service("SysInfoService")
public class SysInfoServiceImpl implements ISysInfoService {

	@Override
	public Map<String, Object> getSysInfo() {
		Map<String, Object> sysInfo = new HashMap<>();
		final OsCpuUtil osCpuUtil = new OsCpuUtil(1);
		final OsMemoryUtil osMemoryUtil = new OsMemoryUtil();
		long freeMem = osMemoryUtil.getOsTotalFreeMemorySize();
		double freeCpu = 100.0 - osCpuUtil.getCpuLoad();
		sysInfo.put("freeCpu", freeCpu);
		sysInfo.put("freeMem", freeMem);
		return sysInfo;
	}
}
