package abc_pipeline_engine.server.service.impl;

import abc_pipeline_engine.server.dao.ServerMapper;
import abc_pipeline_engine.server.data.Server;
import abc_pipeline_engine.server.service.IServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Service("serverService")
public class ServerServiceImpl implements IServerService {

    @Autowired
    private ServerMapper serverMapper;

    @Autowired
    private IServerService executorService;

    @Override
    public void init() {
        String hostAddress;
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
            Server checkServer = serverMapper.selectByExecuteIP(hostAddress);
            if (checkServer != null) {
                checkServer.setIS_ACTIVE("1");
                serverMapper.updateByPrimaryKey(checkServer);
            } else {
                Server server = new Server();
                server.setEXECUTOR_IP(hostAddress);
                server.setEXECUTOR_PORT("8080");
                server.setIS_ACTIVE("1");
                serverMapper.insert(server);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

}
