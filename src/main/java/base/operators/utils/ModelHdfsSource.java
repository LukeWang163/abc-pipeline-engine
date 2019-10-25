package base.operators.utils;

import base.operators.operator.Model;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.*;

public class ModelHdfsSource {

    /**
     * 上传model到hdfs
     * @param model
     * @param modelPath
     */
    public static void writeToHDFS(Model model, String modelPath) {
        FileSystem fs = null;
        FSDataOutputStream outputStream = null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            fs = HDFSUtil.getFileSystem();
            Path path = new Path(modelPath);
            if (fs.exists(path)) {
                fs.delete(path, true);
            }
            outputStream = fs.create(path);
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(model);
            outputStream.write(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                oos.close();
                baos.close();
                outputStream.close();
                fs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static Model readFromHDFS(String inputsPath) {
        FileSystem fs = null;
        InputStream inputStream = null;
        ByteArrayInputStream bi =null;
        ObjectInputStream oi = null;
        byte[] byteArray = null;
        Model model = null;
        try {
            fs = HDFSUtil.getFileSystem();
            inputStream =fs.open(new Path(inputsPath));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int numBytes = 0;
            while ((numBytes = inputStream.read(b, 0, 1024)) > 0) {
                baos.write(b, 0, numBytes);
            }
            byteArray = baos.toByteArray();
            bi = new ByteArrayInputStream(byteArray);
            oi= new ObjectInputStream(bi);
            model = (Model)oi.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            try {
                oi.close();
                bi.close();
                inputStream.close();
                fs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return model;
    }
}
