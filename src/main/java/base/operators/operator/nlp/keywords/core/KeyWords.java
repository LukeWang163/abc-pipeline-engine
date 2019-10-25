package base.operators.operator.nlp.keywords.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyWords {
    /**
     * 多个文档的关键词tfidf
     *
     * @param docId 文档id
     * @param content 文档内容
     * @param size 希望提取几个关键词,如果size<0,则返回所有的单词
     * @return 结果map
     */
    public static Map<String, Map<String, Double>> getKeyWordsTFIDF(List<String> docId, List<String> content, int size){
        //关键词结果，键为文档id,值为提取的关键词与权重值
        Map<String, Map<String, Double>> keywords = new HashMap<>();
        TfIdf tfidf = new TfIdf(docId, content);
        Map<String, Map<String, Double>> tfidfResult = tfidf.tfidf;
        for(Map.Entry<String, Map<String, Double>> entry : tfidfResult.entrySet()){
            keywords.put(entry.getKey(), Util.top(size, entry.getValue()));
        }
        return keywords;
    }

    /**
     * 单个文档的关键词textrank
     *
     * @param text 单个文档内容
     * @param size 希望提取几个关键词,如果size<0,则返回所有的单词
     * @param d 阻尼系数
     * @param max_iter 最大迭代次数
     * @param min_diff 迭代结束误差
     * @param windows 前后窗口大小
     * @return 一个列表
     */
    public static Map<String, Double> getKeyWordsTextRank(String text, int size, float d, int max_iter, float min_diff, int windows){
        List<String> termList = Arrays.asList(text.split(" "));
        TextRank textRank = new TextRank(d,  max_iter,  min_diff, windows);
        //提取size个关键词
        Map<String, Double> entrySet = Util.top(size, textRank.getTermAndRank(termList));
        return entrySet;
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
//        Map<String, Map<String, Double>> res = KeyWordsTfIdf.getKeyWords(ids, content, 2);
//        System.out.println(res);
//    }

}
