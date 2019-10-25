
    /**  
    * @Title: SpinOffMap.java
    * @Package nlp.fjconvert
    * @Description: TODO(用一句话描述该文件做什么)
    * @author Hubery
    * @date 2016年12月27日
    * @version V1.0  
    */
    
package base.operators.operator.nlp.fjconvert.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

    /**
        * @ClassName: SpinOffMap
        * @Description: TODO(依据字符串长度将原大MAP中的各字符串分拆至指定小MAP)
        * @author Hubery
        * @date 2016年12月27日
        *
        */

    public class SpinOffMap {
        static int lengthMax=0;
        static String phraseMax="";
        static ArrayList<String> lengthSet=new ArrayList<String>();

        public static Map<String,TreeMap<String,String>> spinOffMap(TreeMap<String,String> map)
        {
            String length="";
            Map<String,TreeMap<String,String>> all=new HashMap<String,TreeMap<String,String>>();
            for(String key:map.keySet())
            {
                int keyLength=key.length();
                if(lengthMax<keyLength)
                {
                    phraseMax=key;
                }
                lengthMax=(lengthMax>=keyLength)?lengthMax:keyLength;   //获取原词库中字词典左列最大长度
                length=""+keyLength;
                if(all.containsKey(length))
                {
                    all.get(length).put(key, map.get(key));
                }
                else
                {
                    lengthSet.add(length);
                    TreeMap<String,String> newMap=new TreeMap<String,String>();
                    newMap.put(key, map.get(key));
                    all.put(length,newMap);
                }
            }
            // System.out.println("phraseMax="+phraseMax);
            // System.out.println("lengthMax="+lengthMax);
            return all;


        }

    }
