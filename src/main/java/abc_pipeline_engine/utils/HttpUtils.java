package abc_pipeline_engine.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {
	private static final Log log = LogFactory.getLog(HttpUtils.class);
	public static Map<String, Object> httpGet(String url, String token) {
		Map<String, Object> result = new HashMap<String, Object>();
		CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            // 通过址默认配置创建一个httpClient实例
            httpClient = HttpClients.createDefault();
            // 创建httpGet远程连接实例
            HttpGet httpGet = new HttpGet(url);
            // 设置请求头信息，鉴权
            if(token != null) {
            	httpGet.setHeader("token", token);
            }
            // 设置配置请求参数
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000)// 连接主机服务超时时间
                    .setConnectionRequestTimeout(35000)// 请求超时时间
                    .setSocketTimeout(60000)// 数据读取超时时间
                    .build();
            // 为httpGet实例设置配置
            httpGet.setConfig(requestConfig);
            // 执行get请求得到返回对象
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                StringWriter writer = new StringWriter();
                IOUtils.copy(response.getEntity().getContent(), writer, "utf-8");
                String str = writer.toString();
                result.put("content", str);
            } else if(response.getStatusLine().getStatusCode() == 401) {
            	result.put("content", "401");
            }
            System.out.println(response.getEntity().getContent());
        } catch (ClientProtocolException e) {
        	log.error(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
        	log.error(e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
	}
	
	public static Map<String, Object> httpPost(String url, JSONObject param, String token) {
		Map<String, Object> result = new HashMap<String, Object>();
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            if(token != null) {
            	httpPost.setHeader("token", token);
            }
            StringEntity entity = new StringEntity(param.toString(), "utf-8");
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
            CloseableHttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                StringWriter writer = new StringWriter();
                IOUtils.copy(response.getEntity().getContent(), writer, "utf-8");
                String str = writer.toString();
                result.put("content", str);
            }
            System.out.println(response.getEntity().getContent());
		} catch(RuntimeException e) {
			log.error(e.getMessage());
		} catch (Exception e) {
			log.error(e.getMessage());
        }
		return result;
	}
	
	
	
	public static String httpDelete(String url, String token) {
		String result = "failed";
		CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            // 通过址默认配置创建一个httpClient实例
            httpClient = HttpClients.createDefault();
            // 创建httpGet远程连接实例
            HttpDelete httpDelete = new HttpDelete(url);
            // 设置请求头信息，鉴权
            if(token != null) {
            	httpDelete.setHeader("token", token);
            }
            // 设置配置请求参数
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000)// 连接主机服务超时时间
                    .setConnectionRequestTimeout(35000)// 请求超时时间
                    .setSocketTimeout(60000)// 数据读取超时时间
                    .build();
            // 为httpGet实例设置配置
            httpDelete.setConfig(requestConfig);
            // 执行get请求得到返回对象
            response = httpClient.execute(httpDelete);
            if (response.getStatusLine().getStatusCode() == 200) {
                result = "success";
            } 
        } catch (ClientProtocolException e) {
        	log.error(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
        	log.error(e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
	}
}
