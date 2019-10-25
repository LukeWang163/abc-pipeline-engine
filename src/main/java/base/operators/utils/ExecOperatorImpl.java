package base.operators.utils;

import base.operators.core.license.ProductConstraintManager;
import base.operators.example.ExampleSet;
import base.operators.operator.IOObject;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.OutputPortExtender;
import base.operators.operator.ports.PortExtender;
import base.operators.tools.GenericOperatorFactory;
import base.operators.tools.OperatorService;
import base.operators.tools.plugin.Plugin;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zls
 * create time:  2019.08.22.
 * description:
 */
public class ExecOperatorImpl {

    static List<IOObject> inputList = new ArrayList<>();  //输入
    static List<IOObject> outputList = new ArrayList<>();  //输出

    //static String fullName;
    static JSONArray inputsJa = new JSONArray();
    static JSONArray outputsJa = new JSONArray();
    static JSONObject methodJo = new JSONObject();
    static Operator operator = null;



    private static void init(JSONObject opParamsPairs) throws Exception{
     String fullName = opParamsPairs.getString("fullName");// 入口类路径
     inputsJa = opParamsPairs.getJSONArray("inputs");// 输入参数
     outputsJa = opParamsPairs.getJSONArray("outputs");// 输出参数
     methodJo = opParamsPairs.getJSONObject("params");// 算子运算参数


    // 2. init —>getLicense, getLoader
    //ExampleSet exampleSet = null;


        if (!ProductConstraintManager.INSTANCE.isInitialized()) {
            ProductConstraintManager.INSTANCE.initialize(null, null);
        }
        ProductConstraintManager.INSTANCE.getActiveLicense();
        //OperatorService.init();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();


        //System.out.println("test_print----------------------------------------------------------");
        // 3. Reflects the currently executing Operator
        if(fullName.startsWith("WekaExt")){
            //to be test,看是否能获取jar路径//或者查看方法是否直接获取
            Class<?> factoryClass = Class.forName("base.operators.tools.WekaOperatorFactory", true, loader);
            GenericOperatorFactory factory = (GenericOperatorFactory) factoryClass.newInstance();
            URL url =  factoryClass.getProtectionDomain().getCodeSource().getLocation();
            //System.out.println("jar--"+ url);
            String jarFile = String.valueOf(url).substring(5);
            System.out.println("jarFile:"+ jarFile);
            Plugin plugin = new Plugin(new File(jarFile));

            factory.registerOperators(loader, plugin);
            operator = OperatorService.getOperatorDescription(fullName).createOperatorInstance();

        }
        else {
            Class<? extends Operator> clazz = (Class<? extends Operator>) Class.forName(fullName, true, loader);
            OperatorDescription description = new OperatorDescription(null, fullName, clazz, null, null, null);
            Constructor constructor = clazz.getConstructor(OperatorDescription.class);
            operator = (Operator) constructor.newInstance(description);
        }

        // 4. Read InputParams for operator
        readInputPorts(inputsJa);

        // 5. setParameter
        for (String str : methodJo.keySet()) {
            operator.setParameter(str, (String) methodJo.get(str));
        }

    }


    public static void execOperator (JSONObject opParamsPairs){

        try {

            HDFSUtil.initFileSystem();
            init(opParamsPairs);

            List<PortExtender> extenders = operator.getInputPorts().getPortsExtenders();
            if(extenders != null && extenders.size()>0){
                //暂时还未发现有多余1个的算子，如有，需进一步测试，主要是个数问题
                PortExtender extender = extenders.get(0);
                extender.ensureMinimumNumberOfPorts(inputsJa.size());
            }
            List<PortExtender> extenders2 = operator.getOutputPorts().getPortsExtenders();
            if(extenders2 != null && extenders2.size()>0){
                //暂时还未发现有多余1个的算子，如有，需进一步测试
                PortExtender extender = extenders2.get(0);
                if(extender instanceof OutputPortExtender) {
                    extender.ensureMinimumNumberOfPorts(outputsJa.size());
                }
            }

            setInputData(operator);

            // Execut operator
            operator.doWork();

            getOutputData(operator);

            writeToRemote(outputsJa);

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            HDFSUtil.closeFileSystem();
        }



    }


    // 3. Read data from remote
    private static void readInputPorts(JSONArray inputsJa) {

        if (inputsJa.size() != 0) {
            // TODO 开始增加类型
            String inputsPath = null;
            //String type = null;
            for (int i = 0; i < inputsJa.size(); i++) {

                inputsPath = inputsJa.getJSONObject(i).getString("path");
                //type = inputsJa.getJSONObject(i).getString("type");
                if ("".equals(inputsPath)) {
                    inputList.add(null);
                } else{
                    inputList.add((IOObject)readFromRemote(inputsPath));
                }

            }
        }
    }



    // 6. set inputData for operator
    private static void setInputData(Operator operator) {
        if (inputList.size() != 0) {
            for (int i = 0; i < inputList.size(); i++) {
                if (inputList.get(i) != null) {
                    operator.getInputPorts().getPortByIndex(i).receive(inputList.get(i));
                    System.out.println("in:" + inputList.get(i));

                }
            }
        }

        for (int i = 0; i < operator.getOutputPorts().getNumberOfPorts(); i++){
            operator.getOutputPorts().getPortByIndex(i).setConnectForDoWork();
        }
    }

//get outputdata from operator
    private static void getOutputData(Operator operator) throws OperatorException {

        for (int i = 0; i < operator.getOutputPorts().getNumberOfPorts(); i++) {

            IOObject object = operator.getOutputPorts().getPortByIndex(i).getAnyDataOrNull();

                outputList.add(object);

        }
    }

    // writeToParquet
    private static void writeToRemote(JSONArray outputsJa) {
        String outputsPath;
        for (int i = 0; i < outputsJa.size(); i++) {
            if (outputsJa.getJSONObject(i).size() != 0) {
                outputsPath = outputsJa.getJSONObject(i).getString("path");
                IOObject object = outputList.get(i);
                if (object != null) {
                    if (object instanceof ExampleSet) {
                        try {

                            System.out.println("out_data:" + ((ExampleSet) object).getAttributes() + "\n" + "examples:" + ((ExampleSet) object).size() + "   " + ((ExampleSet) object).getExample(0));
                            ParquetExampleSourceUtil.writeToParquet((ExampleSet) object, outputsPath, false);
                            //测试时输出，使用时删除
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        ObjectHdfsSource.writeToHDFS(object, outputsPath);
                        //测试时输出，使用时删除
                        System.out.println("out_others:" + object);
                    }
                }
            }
        }
    }

    public static Object readFromRemote(String filePath){

        Object result = ObjectHdfsSource.readFromHDFS(filePath);
        if(result == null){
            result = ParquetExampleSourceUtil.readFromParquet(filePath, true, false);
        }
        return result;
    }


    /**
     * Write exampleset to Parquet
     *
     * @param exampleSet
     * @throws IOException
     */
    public static void writeToParquet(ExampleSet exampleSet, String pqFileName) throws IOException {
        ParquetExampleSourceUtil.writeToParquet(exampleSet, pqFileName, false);
    }

    /**
     * test read from parquet to exampleset
     */
    public static ExampleSet readFromParquet(String pqFileName, Boolean inferMetaData) {
        return ParquetExampleSourceUtil.readFromParquet(pqFileName, inferMetaData, false);
    }



    public static void writeToHDFS(Object object, String modelPath) {
        if (object != null) {
            ObjectHdfsSource.writeToHDFS(object, modelPath);
        }
    }




    public static Object readFromHDFS(String inputsPath) {
        return ObjectHdfsSource.readFromHDFS(inputsPath);
    }

    public static void main(String[] args) throws ClassNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class<?> factoryClass = Class.forName("org.apache.commons.lang.StringUtils", true, loader);
        URL url =  factoryClass.getProtectionDomain().getCodeSource().getLocation();
        System.out.println(url);
        System.out.println(String.valueOf(url).substring(6));
    }
}

