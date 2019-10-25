package base.operators.operator.nlp.keywords.core;

import java.util.*;

public class TfIdf {
    public static Map<String, List<String>> text_split = new HashMap<>();
    public static Set<String> vocab = new HashSet<>();
    public static Map<String, Double> idf = new HashMap<>();
    public static Map<String, Map<String, Double>> tf = new HashMap<>();
    public static Map<String, Map<String, Double>> tfidf = new HashMap<>();

    public TfIdf(List<String> docId, List<String> content){
        getVocab(docId, content);
        getIdf();
        gettf();
        getTfidf();
    }
    public void getVocab(List<String> docId, List<String> content) {
        for (int i = 0; i < docId.size(); i++) {
            text_split.put(docId.get(i), Arrays.asList(content.get(i).split("\\s+|\\t")));
        }
        text_split.values().stream().forEach(item -> {vocab.addAll(item);});
    }
    /**
     * 对于输入的文件内容列表，计算输入文档中所有词的IDF值
     */
    public void getIdf() {
        int len = text_split.size();
        for(String word : vocab){
            int count = 0;
            for(Map.Entry<String, List<String>> entry : text_split.entrySet()){
                if (entry.getValue().contains(word)){
                    count+=1;
                }
            }
            idf.put(word, (double)Math.log(len/(double)(count + 1)));
        }
    }
    /**
     * 计算每个文本的单词的TF值
     */
    public void gettf() {
        for (Map.Entry<String, List<String>> idEntry : text_split.entrySet()) {
            Map<String, Double> per_content_tf = new HashMap<>();
            for (String word : idEntry.getValue()) {
                per_content_tf.put(word, per_content_tf.containsKey(word) ? per_content_tf.get(word) + 1 / (double)idEntry.getValue().size() : 1 / (double)idEntry.getValue().size());
            }
            tf.put(idEntry.getKey() ,per_content_tf);
        }
    }

    /**
     * 计算每个文本中单词的TFIDF值
     */
    public void getTfidf() {
        for (Map.Entry<String, Map<String, Double>> idEntry : tf.entrySet()){
            Map<String, Double> per_content_tfidf = new HashMap<>();
            for(Map.Entry<String, Double> wordEntry : idEntry.getValue().entrySet()){
                per_content_tfidf.put(wordEntry.getKey(), wordEntry.getValue() * idf.get(wordEntry.getKey()));
            }
            tfidf.put(idEntry.getKey(), per_content_tfidf);
        }
    }
//    public static void main(String[] args){
//        List<String> content = new ArrayList<>();
//        content.add("各项工作 牵头 此次 整治 活动 总结报告 领导小组 成员 单位 工作职责 互联网 金融各业态 监管分工 负责 领域 专项整治工作 制定工作方案 成立 组织 区 财政安排 专项整治工作 经费 保障工作 顺利开展");
//        content.add("互联网金融业务 专项整治 具体工作 2 区市场 监督管理局 负责 互联网金融 广告 专项整治 牵头 投资理财 名义 金融 活动 专项整治 准入管理 做好 相关 企业 准入管理");
//        content.add("警方 采取强制措施 记者 今日上午 洛阳市人大 获悉 洛阳市 十三 届 人大常委会 昨日 第四次会议 市人大常委会 主任 常振 义 主持会议 据介绍");
//        content.add("福州市 社会信用体系建设规划 2016 -2020 年 加快 推进 福州市 社会信用体系建设 进一步规范 政府采购 招投标活动 现将 福州市 政府采购供应商 黑名单制度 管理办法");
//        content.add("安全风险管控 隐患排查治理 双重机制 建设 深入开展 安全生产大检查 重点行业领域 安全生产 专项整治 全市 安全生产形势 平稳 两 节 在即 市 安委会 ");
//        List<String> ids = new ArrayList<>();
//        ids.add("1");
//        ids.add("2");
//        ids.add("3");
//        ids.add("4");
//        ids.add("5");
//        TfIdf tfidf = new TfIdf(ids, content);
//        System.out.println(tfidf.vocab);
//        System.out.println(tfidf.tf);
//        System.out.println(tfidf.idf);
//        System.out.println(tfidf.tfidf);
//    }

}
