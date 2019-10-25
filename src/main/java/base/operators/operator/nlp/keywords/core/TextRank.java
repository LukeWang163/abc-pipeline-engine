package base.operators.operator.nlp.keywords.core;

import java.math.BigDecimal;
import java.util.*;

public class TextRank {
    /**
     * 阻尼系数（DampingFactor），一般取值为0.85
     */
    public float d = 0.85f;
    /**
     * 最大迭代次数
     */
    public int max_iter = 200;
    /**
     * 迭代结束误差
     */
    public float min_diff = 0.001f;
    /**
     * 前后窗口大小
     */
    public int windows = 5;

    public TextRank(float d, int max_iter, float min_diff, int windows){
        this.d = d;
        this.max_iter = max_iter;
        this.min_diff = min_diff;
        this.windows = windows;
    }
    /**
     * 单个文本内容已经分词完毕，形式是List<String>,使用该List<String>来计算rank
     *
     * @param termList
     * @return
     */
    public Map<String, Double> getTermAndRank(List<String> termList) {
        Map<String, Set<String>> words = new TreeMap<String, Set<String>>();
        Queue<String> que = new LinkedList<String>();
        for (String w : termList) {
            if (!words.containsKey(w)) {
                words.put(w, new TreeSet<String>());
            }
            // 复杂度O(n-1)
            if (que.size() >= windows) {
                que.poll();
            }
            for (String qWord : que) {
                if (w.equals(qWord)) {
                    continue;
                }
                //既然是邻居,那么关系是相互的,遍历一遍即可
                words.get(w).add(qWord);
                words.get(qWord).add(w);
            }
            que.offer(w);
        }
        Map<String, Double> score = new HashMap<String, Double>();
        //依据TF来设置初值
        for (Map.Entry<String, Set<String>> entry : words.entrySet()) {
            BigDecimal b = new BigDecimal(String.valueOf(sigMoid(entry.getValue().size())));
            score.put(entry.getKey(), b.doubleValue());
        }
        for (int i = 0; i < max_iter; ++i) {
            Map<String, Double> m = new HashMap();
            float max_diff = 0;
            for (Map.Entry<String, Set<String>> entry : words.entrySet()) {
                String key = entry.getKey();
                Set<String> value = entry.getValue();
                m.put(key, (new BigDecimal(String.valueOf(1 - d))).doubleValue());
                for (String element : value) {
                    int size = words.get(element).size();
                    if (key.equals(element) || size == 0) continue;
                    m.put(key, m.get(key) + d / size * (score.get(element) == null ? 0 : score.get(element)));
                }
                max_diff = (float)Math.max(max_diff, Math.abs(m.get(key) - (score.get(key) == null ? 0 : score.get(key))));
            }
            score = m;
            if (max_diff <= min_diff) break;
        }

        return score;
    }
    /**
     * sigmoid函数
     *
     * @param value
     * @return
     */
    public static float sigMoid(float value) {
        return (float) (1d / (1d + Math.exp(-value)));
    }
}
