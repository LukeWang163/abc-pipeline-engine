package base.operators.utils;

import base.operators.Process;
import base.operators.example.ExampleSet;
import base.operators.operator.IOContainer;
import base.operators.operator.IOObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zls
 * create time:  2019.09.04.
 * description:
 */
public class ExecProcessImpl {

    static List<IOObject> inputList = new ArrayList<>();  //输入
    static List<IOObject> outputList = new ArrayList<>();  //输出

    static String xmlPath = null;

    static JSONArray inputsJa = new JSONArray();
    static JSONArray outputsJa = new JSONArray();
    static JSONObject methodJo = new JSONObject();

    public static void execute(JSONObject opParamsPairs){

        xmlPath = opParamsPairs.getString("xmlPath");// xml路径
        inputsJa = opParamsPairs.getJSONArray("inputs");// 输入参数
        outputsJa = opParamsPairs.getJSONArray("outputs");// 输出参数
        //methodJo = opParamsPairs.getJSONObject("params");// 算子运算参数


        HDFSUtil.initFileSystem();
        for (int i = 0; i < inputsJa.size(); i++) {

            String inputsPath = inputsJa.getJSONObject(i).getString("path");
            //String type = inputsJa.getJSONObject(i).getString("type");
            if("".equals(inputsPath)){
                inputList.add(null);
            } else {
                inputList.add((IOObject)readFromRemote(inputsPath));
            }

        }


        try(
            InputStream inputStream = HDFSUtil.getFileSystem().open(new Path(xmlPath))){

            Process process = new Process(inputStream, true);
            IOContainer inContainer = new IOContainer(inputList);
            IOContainer outContainer = process.run(inContainer);

            for(int i=0; i<outContainer.size(); ++i) {
                String outputsPath = outputsJa.getJSONObject(i).getString("path");
                IOObject out = outContainer.getElementAt(i);
                if (out != null) {
                    if (out instanceof ExampleSet) {
                        ParquetExampleSourceUtil.writeToParquet((ExampleSet) out, outputsPath, false);
                    } else {
                        ObjectHdfsSource.writeToHDFS(out, outputsPath);
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            HDFSUtil.closeFileSystem();
        }


    }

    private static void writeToRemote(JSONArray outputsJa) {
        String outputsPath;
        HDFSUtil.initFileSystem();
        for (int i = 0; i < outputsJa.size(); i++) {
            if (outputsJa.getJSONObject(i).size() != 0) {
                outputsPath = outputsJa.getJSONObject(i).getString("path");
                IOObject object = outputList.get(i);
                if (object!=null && object instanceof ExampleSet) {
                    try {

                        System.out.println("out_data:" + ((ExampleSet)object).getAttributes() + "\n" + "examples:" + ((ExampleSet)object).size() + "   " + ((ExampleSet)object).getExample(0));
                        ParquetExampleSourceUtil.writeToParquet((ExampleSet)object, outputsPath, false);
                        //测试时输出，使用时删除
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else{

                    if (object != null) {
                        ObjectHdfsSource.writeToHDFS(object, outputsPath);
                    }
                    //测试时输出，使用时删除
                    System.out.println("out_others:" + object);

                }
            }
        }
        HDFSUtil.closeFileSystem();
    }

    public static Object readFromRemote(String filePath){

        Object result = ObjectHdfsSource.readFromHDFS(filePath);
        if(result == null){
            result = ParquetExampleSourceUtil.readFromParquet(filePath, true, false);
        }
        return result;
    }

}
