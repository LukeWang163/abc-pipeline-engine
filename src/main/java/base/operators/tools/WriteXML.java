package base.operators.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author zls
 * create time:  2019.08.20.
 * description:
 */
public class WriteXML {

    public static void write(String name, String xml){
        File file = new File("E:\\new_parameters\\" + name + ".xml");

        try {

            FileOutputStream out = new FileOutputStream(file);

            // size  为字串的长度 ，这里一次性写完

            byte[] buffer=xml.getBytes();


            out.write(buffer);

            out.close();

        } catch (IOException e) {

            // TODO Auto-generated catch block


            e.printStackTrace();

        }

    }
}
