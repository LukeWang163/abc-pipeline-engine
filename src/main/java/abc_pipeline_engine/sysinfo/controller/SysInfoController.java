package abc_pipeline_engine.sysinfo.controller;

import abc_pipeline_engine.sysinfo.service.ISysInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/sysinfo")
public class SysInfoController {

    @Autowired
    private ISysInfoService sysInfoService;

    /**
     * 获取实验相关项目
     * @return
     */
    @ResponseBody
    @RequestMapping(method=RequestMethod.GET,params={"method=getSysInfo"})
    public Map<String,Object> getSysInfo(){
        return sysInfoService.getSysInfo();
    }
}