package abc_pipeline_engine.execution.utils;

import abc_pipeline_engine.utils.ExperimentConfigUtil;
import abc_pipeline_engine.utils.forRP.HDFSUtil;
import base.operators.Process;
import base.operators.operator.Operator;
import base.operators.tools.Observer;
import base.operators.utils.PluginUtil;
import org.jboss.netty.util.internal.ConcurrentHashMap;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProcessUtil {
	public static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
	
	static Map<String, ProcessThread> map = new ConcurrentHashMap<>();
	
	/**
	 * @param xmlPath
	 * @param expId
	 * @return processStatus
	 */
	public static String runProcess(String xmlPath, String expId) {
		String processTmp = ExperimentConfigUtil.getString("process.local.xmlDir");
		String libpath = ExperimentConfigUtil.getString("operators.libpath");
		
		// 下载依赖的jar包到本地
//		userInfoService = SpringContextHolder.getBean(IUserInfoService.class);
//		String userId = userInfoService.getLoginUserId();
		// TODO: get userId from ABC_PIPELINE
		String userId = "";
		String jarLocalPath = processTmp + "/" + userId; 
		PluginUtil.init(libpath,jarLocalPath);
		
        // 下载算子流程文件到本地
		String xmlLocalPath = processTmp + "/" +expId;
		HDFSUtil.copyToLocalFile(xmlPath, xmlLocalPath);
        
        //生成进程类
		File processFile = new File(xmlLocalPath+"/process.xml");
        Process processTest;
        String processStatus = null;
		try {
			ProcessThread thread = new ProcessThread();
			map.put(expId, thread);
			processTest = new Process(processFile, true);
			for(Operator operator : processTest.getAllOperators()){
				for(int i=0; i<operator.getOutputPorts().getNumberOfPorts(); ++i){
					operator.getOutputPorts().getPortByIndex(i).setConnectForDoWork();
				}
			}
			Observer<Operator> observer = new OperatorObserver();
			processTest.idswObservers.add(observer);
			IdswProcessListener processListener = new IdswProcessListener(expId); 
			processTest.addProcessStateListener(processListener);
			thread.setProcess(processTest);
			thread.start();
			thread.join();
			processStatus = processListener.processStatus;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}finally {
			map.remove(expId);
			return processStatus;
		}
		
	}

	/**
	 * @param xmlString
	 * @param expId
	 * @return processStatus
	 */
	public static String runProcessString(String xmlString, String expId) {
		String processTmp = ExperimentConfigUtil.getString("process.local.xmlDir");
		String libpath = ExperimentConfigUtil.getString("operators.libpath");

		// 下载依赖的jar包到本地
//		userInfoService = SpringContextHolder.getBean(IUserInfoService.class);
//		String userId = userInfoService.getLoginUserId();
		// TODO: get userId from ABC_PIPELINE
		String userId = "";
		String jarLocalPath = processTmp + "/" + userId;
		PluginUtil.init(libpath, jarLocalPath);

		//生成进程类
		Process processTest;
		String processStatus = null;
		try {
			ProcessThread thread = new ProcessThread();
			map.put(expId, thread);
			processTest = new Process(xmlString, true);
			for (Operator operator : processTest.getAllOperators()) {
				for (int i=0; i<operator.getOutputPorts().getNumberOfPorts(); ++i) {
					operator.getOutputPorts().getPortByIndex(i).setConnectForDoWork();
				}
			}
			Observer<Operator> observer = new OperatorObserver();
			processTest.idswObservers.add(observer);
			IdswProcessListener processListener = new IdswProcessListener(expId);
			processTest.addProcessStateListener(processListener);
			thread.setProcess(processTest);
			thread.start();
			thread.join();
			processStatus = processListener.processStatus;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}finally {
			map.remove(expId);
			return processStatus;
		}

	}
	
	public static void killProcess( String expId) {
		
		try {
			System.out.println("进入kKILL");
			ProcessThread thread = map.get(expId);
			System.out.println("222222222222"+thread.getId());
			thread.stopProcess();
			
		}catch (Exception e) {
			// TODO: handle exception
			System.out.println("33333333"+expId);
	//		e.printStackTrace();
		}
	}

}
