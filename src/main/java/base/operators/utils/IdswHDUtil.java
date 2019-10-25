package base.operators.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数仓RestAPI的工具类
 *
 */
public class IdswHDUtil {

	/**
	 *  查看 指定集群的用户的HDFS访问权限
	 * @param clusterId 集群Id
	 * @param userId	用户Id
	 * @return			指定集群的指定用户有权访问的路径集合
	 */
	public static List<String> getFilesQuotasByUserId(String clusterId, String userId, String keyCloakRealm) {
		// 1.创建HTTPClient
		HttpClient client = new HttpClient();
		List<String> paths = new ArrayList<String>();
		
		/*clusterId = UrlEncoded.encodeString(clusterId, "UTF-8");
		userId = UrlEncoded.encodeString(userId, "UTF-8");*/
		String path = "/manage-store";
		String bathPath = ConfigUtil.getString(Constants.HD_ADMIN_URl) + path;
		String userIdwithRealm = userId +"-"+keyCloakRealm;
		StringBuffer sb = new StringBuffer();
		sb.append(bathPath)
		    .append("/service/hdfs/rest/getFilesQuotasByUserId?clusterId=")
		    .append(clusterId)
		    .append("&userId=")
		    .append(userIdwithRealm);
		String url = sb.toString();
		/*String url = bathPath +"/service/hdfs/rest/getFilesQuotasByUserId?clusterId="+"cluster5013"+"&userId=" + "zhangsan-realm5013";	*/	
		try {
			//2.构造GetMethod的实例
			GetMethod getMethod = new GetMethod(url);
			getMethod.addRequestHeader("Content-Type", "text/html; charset=UTF-8");
	        client.executeMethod(getMethod);
	        
	        JSONArray pathJa = JSONArray.parseArray(getMethod.getResponseBodyAsString());
	        for (int i = 0; i < pathJa.size(); i++) {
	        	JSONObject pathJo = pathJa.getJSONObject(i);
	        	if(pathJo!=null) {
	        		paths.add(pathJo.getString("path"));
	        	}
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return paths;
	}
	
	/**
	 * 创建目录
	 * @param clusterId	集群Id
	 * @param userId	用户Id
	 * @param newDir	待创建目录
	 * @return			创建成功与否的状态，“true”创建成功，“false”创建失败
	 */
	public static String createDirByUserId(String clusterId, String userId, String newDir, String keyCloakRealm) {
		String createStatus = "false";
		// 1.创建HTTPClient
		HttpClient client = new HttpClient();
		List<String> paths = new ArrayList<String>();

		String path = "/manage-store";
		String bathPath = ConfigUtil.getString(Constants.HD_ADMIN_URl) + path;
		String userIdwithRealm = userId +"-"+keyCloakRealm;
		StringBuffer sb = new StringBuffer();
		sb.append(bathPath)
		    .append("/service/hdfs/rest/makeDir?clusterId=")
		    .append(clusterId)
		    .append("&userId=")
		    .append(userIdwithRealm)
		    .append("&path=")
		    .append(newDir);
		String url = sb.toString();
		/*String url = bathPath + "/service/hdfs/rest/makeDir?clusterId=" + "cluster5013" + "&userId=" + "zhangsan-realm5013" + "&path=" + newDir;*/
		try {
			// 2.构造GetMethod的实例
			GetMethod getMethod = new GetMethod(url);
			getMethod.addRequestHeader("Content-Type", "text/html; charset=UTF-8");
			client.executeMethod(getMethod);

			createStatus = getMethod.getResponseBodyAsString();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return createStatus;
	}
	
	/**
	 * 获取指定用户的krb5的Principal
	 * @param userId			指定用户
	 * @param keyCloakRealm		用户所在的域
	 * @return					用户对应的Principal
	 */
	public static String getKrb5Principal(String userId, String keyCloakRealm) {
		
		String krb5Principal = null;
		HttpClient client = new HttpClient();
		String path = "/manage";
		String bathPath = ConfigUtil.getString(Constants.HD_ADMIN_URl) + path;
		String userIdwithRealm = userId +"-"+keyCloakRealm;
		StringBuffer sb = new StringBuffer();
		sb.append(bathPath)
		    .append("/service/api/manage/tenants/getKerberosInfo?userId=")
		    .append(userIdwithRealm);
		String url = sb.toString();
		try {
			GetMethod getMethod = new GetMethod(url);
			getMethod.addRequestHeader("Content-Type", "text/html; charset=UTF-8");
			client.executeMethod(getMethod);
			JSONObject krb5InfoJo = JSONObject.parseObject(getMethod.getResponseBodyAsString());
			krb5Principal = krb5InfoJo.getString("principal");
		} catch (IOException e) {
			e.printStackTrace();
		}

		return krb5Principal;
	}
	
	/**
	 * 下载keytab文件到KEYTAB_DIR目录
	 * @param keytabDir			keytab存储的目录
	 * @param keytabPath		keytab存储的路径
	 * @param userId			用户ID
	 * @param keyCloakRealm		用户所在的域
	 */
	public static void getkeytabFile(String keytabDir, String keytabPath, String userId, String keyCloakRealm) {
		FileOutputStream fileOut = null;
		HttpClient client = new HttpClient();
		String path = "/manage";
		String bathPath = ConfigUtil.getString(Constants.HD_ADMIN_URl) + path;
		String userIdwithRealm = userId +"-"+keyCloakRealm;
		StringBuffer sb = new StringBuffer();
		sb.append(bathPath)
		    .append("/service/api/manage/tenants/getKeytabFile?userId=")
		    .append(userIdwithRealm);
		String url = sb.toString();
		try {
			GetMethod getMethod = new GetMethod(url);
			getMethod.addRequestHeader("Content-Type", "text/html; charset=UTF-8");
			client.executeMethod(getMethod);
			InputStream inputStream = getMethod.getResponseBodyAsStream();
			BufferedInputStream keytabBis = new BufferedInputStream(inputStream); 
			//判断文件的保存路径后面是否以/结尾  
	        if (!keytabDir.endsWith("/")) {  
	        	keytabDir += "/";  
	        }  
			//写入到文件（注意文件保存路径的后面一定要加上文件的名称）  
			fileOut = new FileOutputStream(keytabPath);
			BufferedOutputStream keytabBos = new BufferedOutputStream(fileOut);  
			  
			byte[] buf = new byte[4096];  
			int length = keytabBis.read(buf);  
			//保存文件  
			while(length != -1)  
			{  
				keytabBos.write(buf, 0, length);  
			    length = keytabBis.read(buf);  
			}  
			keytabBos.close();  
			keytabBis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 给用户授权某路径
	 * @param clusterId			集群Id
	 * @param userId			带授权的用户Id
	 * @param userDir			带授权的目录
	 * @param keyCloakRealm		带授权用户所在的realm域
	 * @return					success/failure
	 */
	public static String saveHDFSAuth(String clusterId, String userId, String userDir, String keyCloakRealm) {
		String result = null;
		HttpClient client = new HttpClient();
		String path = "/manage";
		String bathPath = ConfigUtil.getString(Constants.HD_ADMIN_URl) + path;
		String userIdwithRealm = userId +"-"+keyCloakRealm;
		StringBuffer sb = new StringBuffer();
		sb.append(bathPath)
		    .append("/service/security/rest/saveHDFSAuth?clusterId=").append(clusterId)
		    .append("&clusterName=").append(clusterId)
		    .append("&authUser=").append(userIdwithRealm)
		    .append("&path=").append(userDir);
		String url = sb.toString();
		try {
			GetMethod getMethod = new GetMethod(url);
			getMethod.addRequestHeader("Content-Type", "text/html; charset=UTF-8");
			client.executeMethod(getMethod);
			result = getMethod.getResponseBodyAsString();
			if ("success".equals(result)) {
				// 由于Ranger的定期轮询机制，设置轮询时间为1秒
				Thread.sleep(1000);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result;
	}	
	
}
