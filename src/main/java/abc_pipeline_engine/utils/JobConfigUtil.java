package abc_pipeline_engine.utils;


import abc_pipeline_engine.common.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class JobConfigUtil {
	private static Properties prop = new Properties();
	private static boolean hasLoad = false;

	private static void loadProperties() {
		InputStream is = JobConfigUtil.class.getClassLoader().getResourceAsStream(Constants.JOB_CONFIG_FILE);
		try {
			prop.load(is);
			hasLoad = true;
		} catch (IOException e) {
			e.printStackTrace();
			hasLoad = false;
		}
	}

	public static String getString(String key) {
		// 优先加载环境变量
		String value = System.getenv(key);
		if (null == value) {
			if (!hasLoad) {
				loadProperties();
			}
			value = prop.getProperty(key);
		}
		return value == null ? "" : value.trim();
	}

	public static String getString(String key, String defaultStr) {
		// 优先加载环境变量
		String value = System.getenv(key);
		if (null == value) {
			if (!hasLoad) {
				loadProperties();
			}
			value = prop.getProperty(key);
		}
		return null == value ? defaultStr : value;
	}
}
