package base.operators.utils;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.operator.Model;
import base.operators.operator.ResultObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ObjectHdfsSource extends HDFSUtil {

    /**
     * 上传model到hdfs
     * @param object
     * @param basePath
     */
    public static void writeToHDFS(Object object, String basePath) {
 //       FileSystem fs = null;
        try {
            fs.getStatus();
        } catch (Exception e){
            initFileSystem();
        }
        FSDataOutputStream outputStream = null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
  //          fs = HDFSUtil.getFileSystem();
            Path path = new Path(basePath);
            if (fs.exists(path)) {
                fs.delete(path, true);
            }
            if (!fs.exists(path)){
                fs.mkdirs(path);
            }
            Path objectPath = new Path(basePath +  Path.SEPARATOR + "data");
            outputStream = fs.create(objectPath);
//            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(outputStream);
            oos.writeObject(object);
            oos.writeObject(null);
//            outputStream.write(baos.toByteArray());
            oos.flush();
            oos.close();

            Path metaPath = new Path(basePath +  Path.SEPARATOR + "metadata");
            outputStream = fs.create(metaPath);
            if(object instanceof ResultObject){
                String objectInfo = ((ResultObject)object).toResultString();
                if (object instanceof Model){
                    Model model = (Model) object;
                    String modelName = model.getName();
                    Attribute labelAttribute = model.getTrainingHeader().getAttributes().getLabel();
                    String label;
                    Attributes trainingAttributes1 = model.getTrainingHeader().getAttributes();
                    Attributes trainingAttributes = (Attributes)trainingAttributes1.clone();
                    if (labelAttribute != null) {
                        label = labelAttribute.getName();
                        trainingAttributes.remove(labelAttribute);
                    } else {
                        label = "";
                    }
                    String featureNames;
                    Iterator<Attribute> iter = trainingAttributes.iterator();
                    StringBuilder sb = new StringBuilder();
                    while (iter.hasNext()) {
                        sb.append(iter.next().getName());
                        sb.append(",");
                    }
                    featureNames = sb.toString();
                    Map<String, String> objectMetadata = new HashMap<>();
                    objectMetadata.put("model_name", modelName);
                    objectMetadata.put("feature_names", featureNames);
                    objectMetadata.put("label_name", label);
                    objectMetadata.put("object_info", objectInfo);
                    ObjectMapper mapper = new ObjectMapper();
                    outputStream.write(mapper.writeValueAsString(objectMetadata).getBytes(StandardCharsets.UTF_8));
                } else {
                    Map<String, String> objectMetadata = new HashMap<>();
                    objectMetadata.put("object_info", objectInfo);
                    ObjectMapper mapper = new ObjectMapper();
                    outputStream.write(mapper.writeValueAsString(objectMetadata).getBytes(StandardCharsets.UTF_8));
                }
            }else {
                Map<String, String> objectMetadata = new HashMap<>();
                objectMetadata.put("object_info", object.toString());
                ObjectMapper mapper = new ObjectMapper();
                outputStream.write(mapper.writeValueAsString(objectMetadata).getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                oos.close();
//                baos.close();
                outputStream.close();
   //             fs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static Object readFromHDFS(String inputPath) {
  //      FileSystem fs = null;
        try {
            fs.getStatus();
        } catch (IOException | NullPointerException  e){
            initFileSystem();
        }
        InputStream inputStream = null;
        ByteArrayInputStream bi =null;
        ObjectInputStream oi = null;
//        byte[] byteArray = null;
        Object object = null;
        try {
            Path path = new Path(inputPath + Path.SEPARATOR + "data.parquet");
            if(fs.exists(path)){
                return null;
            }
   //         fs = HDFSUtil.getFileSystem();
            inputStream =fs.open(new Path(inputPath + Path.SEPARATOR + "data"));
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            byte[] b = new byte[1024];
//            int numBytes = 0;
//            while ((numBytes = inputStream.read(b, 0, 1024)) > 0) {
//                baos.write(b, 0, numBytes);
//            }
//            byteArray = baos.toByteArray();
//            bi = new ByteArrayInputStream(byteArray);
            oi= new ObjectInputStream(inputStream);
            object = oi.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            try {
                oi.close();
//                bi.close();
                inputStream.close();
   //             fs.close();
            } catch (Exception e) {
            }
        }
        return object;
    }
}
