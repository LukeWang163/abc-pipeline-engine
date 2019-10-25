package abc_pipeline_engine.server.listener;

import abc_pipeline_engine.server.service.IServerService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.Resource;
import javax.servlet.ServletContext;

@Service
public class InitServer implements InitializingBean, ServletContextAware {

    @Resource
    private IServerService executorService;

    @Override
    public void setServletContext(ServletContext servletContext) {
        try {
            System.out.println("开机启动一次-----------------------");
            executorService.init();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //开机后需要执行的业务逻辑

    }

}