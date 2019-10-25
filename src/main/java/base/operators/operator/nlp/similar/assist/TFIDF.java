package base.operators.operator.nlp.similar.assist;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TFIDF {

    public Map<String, Double> tf1 = new HashMap<>();
    public Map<String, Double> tf2 = new HashMap<>();
    public Map<String, Integer> df = new HashMap<>();

    public TFIDF(List<String> list1, List<String> list2){
        for (int i = 0; i < list1.size(); i++) {
            tf1.put(list1.get(i), tf1.containsKey(list1.get(i)) ? (tf1.get(list1.get(i)) + 1)/(double)list1.size() : 1/(double)list1.size());
            df.put(list1.get(i), list2.contains(list1.get(i)) ? 2 : 1);
        }
        for (int j = 0; j < list2.size(); j++) {
            tf2.put(list2.get(j), tf2.containsKey(list2.get(j)) ? (tf2.get(list2.get(j)) + 1)/(double)list2.size() : 1/(double)list2.size());
            if(!df.containsKey(list2.get(j))){
                df.put(list2.get(j), 1);
            }
        }
    }
    public TFIDF(String s1, String s2){
        this(Arrays.asList(s1.split("")), Arrays.asList(s2.split("")));
    }

    public TFIDF(String[] arr1, String[] arr2){
        this(Arrays.asList(arr1), Arrays.asList(arr2));
    }

    public double getTfidfSim(){
        double v_x = 0, v_y = 0, v_x_y = 0, v_x_2 = 0, v_y_2 = 0;
        for (Map.Entry<String, Integer> entry : df.entrySet()){
            if(entry.getValue() == 0){
                continue;
            }else{
                double idf = (2+1) / (double)(entry.getValue()+1);
                v_x = tf1.containsKey(entry.getKey()) ? (Math.log(idf)+1) * Math.log(tf1.get(entry.getKey())): 0;
                v_y = tf2.containsKey(entry.getKey()) ? (Math.log(idf)+1) * Math.log(tf2.get(entry.getKey())): 0;
                v_x_y += v_x * v_y;
                v_x_2 += v_x * v_x;
                v_y_2 += v_y * v_y;
            }

        }
        return v_x_y == 0 ? 0 : v_x_y / (Math.sqrt(v_x_2) * Math.sqrt(v_y_2)) ;
    }
//    public static void main(String[] args){
//        String[] s1 = "wo fn hi ha ha ha ha".split(" ");
//        String[] s2 = "wo wo wo hi hi".split(" ");
//        TFIDF tfidf = new TFIDF(s1,s2);
//        System.out.println(tfidf.getTfidfSim());
//    }
}
