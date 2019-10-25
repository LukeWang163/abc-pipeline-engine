package abc_pipeline_engine.executor.service.impl;

import abc_pipeline_engine.executor.dao.ExecutorMapper;
import abc_pipeline_engine.executor.data.Executor;
import abc_pipeline_engine.executor.service.IExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Service("executorService")
public class ExecutorServiceImpl implements IExecutorService {

    @Autowired
    private ExecutorMapper executorMapper;

    @Autowired
    private IExecutorService executorService;

    @Override
    public void init() {
        String hostAddress;
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
            Executor checkExecutor = executorMapper.selectByExecuteIP(hostAddress);
            if (checkExecutor != null) {
                checkExecutor.setIS_ACTIVE("1");
                executorMapper.updateByPrimaryKey(checkExecutor);
            } else {
                Executor executor = new Executor();
                executor.setEXECUTOR_IP(hostAddress);
                executor.setEXECUTOR_PORT("8080");
                executor.setIS_ACTIVE("1");
                executorMapper.insert(executor);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

}
