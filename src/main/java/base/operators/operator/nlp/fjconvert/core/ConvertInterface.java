package base.operators.operator.nlp.fjconvert.core;

import java.util.HashMap;
import java.util.Map;

/***
 * @author donglinkun
 * 接口整合
 */
public class ConvertInterface {
    public static final Integer OPTION_TW2S = 0;//台湾繁体转简体
    public static final Integer OPTION_T2S = 2;//大陆繁体转简体
    public static final Integer OPTION_S2T = 3; //简体转大陆繁体
    public static final Integer OPTION_S2TW = 4;//简体转台湾繁体
    public static final Integer OPTION_T2TW = 9;//大陆繁体转台湾繁体

    public static Map<String, Integer> map = new HashMap<>();
    static {
        map.put("台湾繁体转简体", 0);
        map.put("大陆繁体转简体", 2);
        map.put("简体转大陆繁体", 3);
        map.put("简体转台湾繁体", 4);
        map.put("大陆繁体转台湾繁体", 9);

    }

    public String convert(String sentence, int instance) {
        ChineseConvert convert;
        if (instance == OPTION_TW2S || instance == OPTION_T2S || instance == OPTION_S2T || instance == OPTION_S2TW || instance == OPTION_T2TW) {
            convert = ChineseConvert.getInstance(instance);
        } else {
            return "转换类型参数错误！"; // 转换类型输入参数错误
        }

        String convertedStr = convert.convert(sentence);
        return convertedStr;
    }

    public String convert(String sentence, String type) {
        ChineseConvert convert;
        try{
            convert = ChineseConvert.getInstance(map.get(type));
        } catch (Exception e){
            e.printStackTrace();
            return "转换类型参数错误！"; // 转换类型输入参数错误
        }

        String convertedStr = convert.convert(sentence);
        return convertedStr;
    }

    public static void main(String[] args) {
        String s = "閱讀說話";
        String result = new ConvertInterface().convert(s, 2);
        System.out.println(result);
    }
}
