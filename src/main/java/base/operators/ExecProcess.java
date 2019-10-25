package base.operators;

import base.operators.example.ExampleSet;
import base.operators.operator.IOContainer;
import base.operators.operator.IOObject;

import java.io.File;

/**
 * @author zls
 * create time:  2019.09.02.
 * description:
 */
public class ExecProcess {

    public static void main(String[] args) {
        //RapidMiner.init();

        try {
            Process process = new Process(new File("D:\\Users\\zhanglsh01\\Documents\\Tencent Files\\1026160609\\FileRecv\\process.xml"));
            //IOObject input1 = ExecOperatorImpl.readFromParquet("/data/rapidminerTest/read_csv/golf", true);
            //IOContainer container = new IOContainer(input1);

            IOContainer out = process.run();

            IOObject object = out.getElementAt(0);

            if (object instanceof ExampleSet){
                System.out.println(true);
                System.out.println(object);
                System.out.println(((ExampleSet)object).getExample(0));
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
