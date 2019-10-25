package abc_pipeline_engine.executor.listener;

import abc_pipeline_engine.executor.dao.ExecutorMapper;
import abc_pipeline_engine.executor.data.Executor;
import abc_pipeline_engine.executor.service.IExecutorService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.net.InetAddress;
import java.net.UnknownHostException;

//@Component
//public class InitExecutor implements ServletContextListener {
//
//    @Override
//    public void contextInitialized(ServletContextEvent  sce) {
////        context = new ClassPathXmlApplicationContext("classpath*:/spring/spring-context.xml");
//        ApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext());
//        System.out.println("Starting up");
//        IExecutorService executorService = (IExecutorService) context.getBean("executorService");
//
//        executorService.init();
//    }
//
//    @Override
//    public void contextDestroyed(ServletContextEvent sce) {
//        // TODO Auto-generated method stub
//
//    }
//
//}

//@Service
//public class InitExecutor implements  ApplicationListener<ContextRefreshedEvent> {
//    @Override
//    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
//        if (contextRefreshedEvent.getApplicationContext().getParent() == null) {//保证只执行一次
//
//            ApplicationContext context = contextRefreshedEvent.getApplicationContext();
//            System.out.println("Starting up");
//            IExecutorService executorService = (IExecutorService) context.getBean("executorService");
//
//            executorService.init();
//        }
//    }
//}

@Service
public class InitExecutor implements InitializingBean, ServletContextAware {

    @Resource
    private IExecutorService executorService;

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