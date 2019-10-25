package base.operators.utils;

import base.operators.Process;
import base.operators.example.ExampleSet;
import base.operators.operator.IOContainer;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.tools.GenericOperatorFactory;
import base.operators.tools.OperatorService;
import base.operators.tools.Tools;
import base.operators.tools.plugin.Plugin;
import base.operators.tools.plugin.PluginClassLoader;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class PluginUtil {
	private static boolean isInit = false;
	private static boolean hasLoad = false;
	private static Properties prop = new Properties();

	public static String[] pluginList = new String[]{"WEKA", "NLP", "ADAPTER_WEKA", "TEST_PLUGIN", "JDBC"};

	public static List<String> pluginNames = new ArrayList<>();

	//public static String BASE_PATH = getString("BASE_PATH");


	public static Map<String, String> pluginPath = new HashMap<>();

	public static String parentPath;
	public static String basePath;

	static {
		for(int i=0; i<pluginList.length; ++i){
			pluginNames.add(getString(pluginList[i]));
		}
	}


	public static void setParentPath(String path){
		parentPath = path;
	}
	public static void setBasePath(String path){
		basePath = path;
	}

	public static void init(String basePath, String parentPath){
		setBasePath(basePath);
		setParentPath(parentPath);
		isInit = true;
	}

	private static void loadProperties() {
		InputStream is = null;
		try {
			is = Tools.getResource(Constants.VARIABLE_FILE).openStream();
//			int size=is.available();
//
//			byte[] buffer=new byte[size];
//
//			is.read(buffer);
//
//			is.close();
//
//			String str=new String(buffer,"UTF-8");
//			System.out.println(str);
			prop.load(is);
			hasLoad = true;
		} catch (IOException e) {
			e.printStackTrace();
			hasLoad = false;
		}
	}

	public static String getString(String key) {
		String value;
		if (!hasLoad) {
			loadProperties();
		}
		value = prop.getProperty(key);
		return value == null ? "" : value.trim();
	}

	public static String getString(String key, String defaultStr) {
		String value;
		if (!hasLoad) {
			loadProperties();
		}
		value = prop.getProperty(key);
		return value == null ? defaultStr : value.trim();
	}

	public static OperatorDescription getDescription(Element opElement, String className) throws Exception {
		if (OperatorService.getOperatorMap().containsKey(className)){
			return OperatorService.getOperatorMap().get(className);
		}
		OperatorDescription opDescr = null;
		Class<? extends Operator> clazz = null;
		String path = opElement.getAttribute("jarPath");
		String type = "0";
		if(opElement.hasAttribute("storeType")){
			type = opElement.getAttribute("storeType");
		}
		if (!"".equals(path)) {
			if(isInit) {
				String fullPath = path;

				if("0".equals(type)) {
					String lastPath = path.substring(basePath.length() + 1);

					String pluginName = lastPath.substring(0, lastPath.indexOf("/"));

					String dir = parentPath + File.separator + lastPath.substring(0, lastPath.lastIndexOf("/"));

					String name = lastPath.substring(lastPath.lastIndexOf("/") + 1);

					fullPath = dir + File.separator + name;

					//if (pluginNames.contains(pluginName) && (new File(fullPath)).exists()) {
					//先不判断是否内置，后续需完善
					if ( (new File(fullPath)).exists()) {

					} else {
						HDFSUtil.copyToLocalFile(path, dir);
					}
				}

				if (className.startsWith("WekaExt")) {


					List<File> files = new ArrayList<>();
					files.add(new File(fullPath));
					Plugin.registerPlugins(files, true, true);
					Plugin.registerAllPluginOperators();


					return OperatorService.getOperatorMap().get(className);


				} else {
					File file = new File(fullPath);
					URL url = file.toURI().toURL();
					PluginClassLoader urlclassLoader = new PluginClassLoader(new URL[]{url});
					clazz = (Class<? extends Operator>) Class.forName(className, true, urlclassLoader);
					opDescr = new OperatorDescription(null, null, clazz, urlclassLoader, null, null);
				}
			}else {
				if(className.startsWith("WekaExt")){
					ClassLoader loader = Thread.currentThread().getContextClassLoader();
					Class<?> factoryClass = Class.forName("base.operators.tools.WekaOperatorFactory", true, loader);
					GenericOperatorFactory factory = (GenericOperatorFactory) factoryClass.newInstance();
					URL url =  factoryClass.getProtectionDomain().getCodeSource().getLocation();
					//System.out.println("jar--"+ url);
					String jarFile = String.valueOf(url).substring(5);
					Plugin plugin = new Plugin(new File(jarFile));
					factory.registerOperators(loader, plugin);

					return OperatorService.getOperatorMap().get(className);

				}else {
					clazz = (Class<? extends Operator>) Class.forName(className, true, Thread.currentThread().getContextClassLoader());
					opDescr = new OperatorDescription(null, null, clazz, null, null, null);
				}

			}


		} else {
			clazz = (Class<? extends Operator>) Class.forName(className, true, Thread.currentThread().getContextClassLoader());
			opDescr = new OperatorDescription(null, null, clazz, null, null, null);
		}
		OperatorService.getOperatorMap().put(className, opDescr);
		return opDescr;
	}

	public static void main(String[] args) {
		PluginUtil.init("/user/root/examples/lib","D:\\MyCode\\java\\IDSW\\idsw-portal\\tmp\\process\\superadmin");

		try {

			//Process process = new Process();
			Process process = new Process(new File("D:\\MyCode\\java\\IDSW\\idsw-portal\\tmp\\process\\8eeb9b43b05544c38470b35a576dcdfb\\process.xml"), true);
			System.out.println(process);
			IOContainer container = process.run();
			System.out.println(((ExampleSet) container.getElementAt(0)).getAttributes());
			System.out.println(container.getElementAt(1));

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

}
