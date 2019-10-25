package base.operators.utils;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author zls
 * create time:  2019.08.20.
 * description:
 */
public class ReadJson {

    public static String read(String name){
        File file = new File("./jsons/" + name);
        String str="";

        try {

            FileInputStream in = new FileInputStream(file);

            // size  为字串的长度 ，这里一次性读完

            int size=in.available();

            byte[] buffer=new byte[size];

            in.read(buffer);

            in.close();

            str=new String(buffer,"UTF-8");

        } catch (IOException e) {

            // TODO Auto-generated catch block


            e.printStackTrace();

        }

        return str;
    }

    public static String readHDFS(String xmlPath){
        String xml = null;
        try(FileSystem fs = HDFSUtil.getFileSystem();
            InputStream inputStream =fs.open(new Path(xmlPath))){

        }catch (Exception e){
            e.printStackTrace();
        }
        return xml;
    }

}
