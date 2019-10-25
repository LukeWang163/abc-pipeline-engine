package base.operators.operator.nlp.autosummary.core.summary;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 搜索相关性评分算法
 * @author 
 */
public class BM25
{
    /**
     * 文档句子的个数
     */
    int senNum;

    /**
     * 文档句子的平均长度
     */
    double avgSenL;

    /**
     * 拆分为[句子[单词]]形式的文档
     */
    List<List<String>> sens;
    

    /**
     * 文档中每个句子中的每个词与词频
     */
    Map<String, Integer>[] f;

    /**
     * 文档中全部词语与出现在几个句子中
     */
    Map<String, Integer> df;

    /**
     * IDF
     */
    Map<String, Double> idf;

    /**
     * 调节因子
     */
    final static float k1 = 1.5f;

    /**
     * 调节因子
     */
    final static float b = 0.75f;

    public BM25(List<List<String>> sens)
    {
        this.sens = sens;
        senNum = sens.size();
        for (List<String> sentence : sens)
        {
            avgSenL += sentence.size();
        }
        avgSenL /= senNum;//句子平均长度（句子平均含有词的个数）
        f = new Map[senNum];//一个Map对应的是一个句子中所有的词、词频
        df = new TreeMap<String, Integer>();//词：包含该词的句子数
        idf = new TreeMap<String, Double>();//词的句子级idf
        init();
    }

    /**
     * 在构造时初始化自己的所有参数
     */
    private void init()
    {
        int index = 0;
        for (List<String> sentence : sens)//循环生成句子级的tf和df
        {
            Map<String, Integer> tf = new TreeMap<String, Integer>();
            for (String word : sentence)
            {
                Integer freq = tf.get(word);
                freq = (freq == null ? 0 : freq) + 1;
                tf.put(word, freq);
            }
            f[index] = tf;//f里一个元素存放的是一句中所有词的word frequency
            for (Map.Entry<String, Integer> entry : tf.entrySet())
            {
                String word = entry.getKey();
                Integer freq = df.get(word);
                freq = (freq == null ? 0 : freq) + 1;
                df.put(word, freq);//df里存放的是每一个word所对应的包含该word的文档数
            }
            ++index;
        }
        //计算句子级idf
        for (Map.Entry<String, Integer> entry : df.entrySet())
        {
            String word = entry.getKey();
            Integer freq = entry.getValue();
            idf.put(word, Math.log(senNum - freq + 0.5) - Math.log(freq + 0.5));//word的逆向文本频率？？
        }
    }

    /**
     * 计算两个句子之间的相似度：通过计算共同词之间的score和来获取相似度
     * @param sentence
     * @param index
     * @return
     */
    public double sim(List<String> sentence, int index)
    {
        double score = 0;
        for (String word : sentence)
        {
            if (!f[index].containsKey(word)) continue;
            int d = sens.get(index).size();
            Integer wf = f[index].get(word);
            score += (idf.get(word) * wf * (k1 + 1)
                    / (wf + k1 * (1 - b + b * d
                                                / avgSenL)));//共同词的权重和
        }

        return score;
    }
    
    public double[] simAll(List<String> sentence)
    {
        double[] scores = new double[senNum];
        for (int i = 0; i < senNum; ++i)
        {
            scores[i] = sim(sentence, i);
        }
        return scores;
    }
    
}
