package base.operators.operator.nlp.feature.core.assist;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TextRank {
	
	public Map<String, Float> getWordTextRank(Map<String, Set<String>> relationWords){
        float d = 0.85f;//阻尼系数
        float min_diff = 0.001f; //差值最小
        int max_iter = 200;//最大迭代次数
        Map<String, Float> score = new HashMap<>();
        //迭代
        for (int i = 0; i < max_iter; i++) {
            Map<String, Float> m = new HashMap<>();
            float max_diff = 0;
            for (Map.Entry<String, Set<String>> entry : relationWords.entrySet()) {
                Set<String> value = entry.getValue();
                //先给每个关键词一个默认rank值
                m.put(entry.getKey(), 1 - d);
                //一个关键词的TextRank由其它成员投票出来
                for (String other : value) {
                    int size = relationWords.get(other).size();
                    if (entry.getKey().equals(other) || size == 0) {
                        continue;
                    } else {
                        m.put(entry.getKey(), m.get(entry.getKey()) + d / size * (score.get(other) == null ? 0 : score.get(other)));
                    }
                }
                max_diff = Math.max(max_diff, Math.abs(m.get(entry.getKey()) - (score.get(entry.getKey()) == null ? 0 : score.get(entry.getKey()))));
            }
            score = m;
            if (max_diff <= min_diff) {
            	break;
            }
        } 
        return score;
	}
}     

   

