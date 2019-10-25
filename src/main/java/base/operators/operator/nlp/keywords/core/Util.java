package base.operators.operator.nlp.keywords.core;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Util {
    //取目标map中值最大的前size个，若size<0,则取所有的。
    public static Map<String, Double> top(int size, Map<String, Double> map) {
        Map<String, Double> result = new LinkedHashMap<String, Double>();
        int size_new = size < 0 ? map.size() : size;
        for (Map.Entry<String, Double> entry : new MaxHeap<Map.Entry<String, Double>>(size_new, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        }).addAll(map.entrySet()).toList()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

}
