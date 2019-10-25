package base.operators.operator.nlp.fjconvert.core;

/**
 * Created by zhangxian on 2019/3/12.
 */
public class TestZX {

    public static void main(String[] args){
/*        ConvertInterface convertInterface = new ConvertInterface();
        String T2TW_String = convertInterface.convert("閱讀說話",0);//閱讀說話 出租車
        System.out.println(T2TW_String);*/


        String regex = "(\r\n)+";
        String sBefore = "1234567\r\n\r\n\r\n\r\n12345678";
        String sAfter = sBefore.replaceAll(regex,"\r\n");
        System.out.println("sBefore=="+sBefore);
        System.out.println("sAfter=="+sAfter);
    }



}
