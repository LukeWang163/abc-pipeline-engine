package base.operators.operator.nlp.autosummary.core.summary;

import com.alibaba.fastjson.JSONObject;

import java.util.*;

/**
 * Created by zhangxian on 2017/11/15.
 */
public class SummaryImpl {
	
	static TextRankSentence textRank = null;
	public SummaryImpl(List<String> dicts) {
		textRank = TextRankSentence.getInstance(dicts);
	}
	
	public SummaryImpl() {
		textRank = TextRankSentence.getInstance(null);
	}

    public SummaryImpl(List<String> dicts,double d,int max_iter,double min_diff,String size,int lang,int type) {
        textRank = TextRankSentence.getInstance(dicts,d,max_iter,min_diff,size,lang, type);
    }

	/**
	 * text: 输入文本
	 */
	public JSONObject summaryJsonSeg(String text) {
		JSONObject object = new JSONObject();
		List<Map<String, Object>> objs = textRank.getTopSentenceList(text);
		Collections.sort(objs, new Comparator<Map<String, Object>>() {
	          public int compare(Map<String, Object> map_0, Map<String, Object> map_1) {
	        	  int index_0 = (Integer) map_0.get("index");
	        	  int index_1 = (Integer) map_1.get("index");
	              return index_0 - index_1;
	          }
	      });
		object.put("sentences", objs);
		return object;
	}

	/**
	 *
	 * @param text 分词且词性标注后的文本
	 * @return
	 */
	public List<Map<String, Object>> summary(String text) {
		List<Map<String, Object>> objs = textRank.getTopSentenceList(text);
		Collections.sort(objs, new Comparator<Map<String, Object>>() {
	          public int compare(Map<String, Object> map_0, Map<String, Object> map_1) {
	        	  Double weight_0 = (Double) map_0.get("weight");
                  Double weight_1 = (Double) map_1.get("weight");
//	              return index_0 - index_1;
                  return weight_1.compareTo(weight_0);
	          }
	      });
		return objs;
	}

	//Collections.sort(ag.state_list,new Comparator<State>(){ public int compare(State sta1, State sta2) {  return new Double(sta1.score).compareTo(new Double(sta2.score));}});

	
	/*public String summary(String text) {
		List<String> senList = new ArrayList<String>();
		List<Map<String, Object>> objs = textRank.getTopSentenceList(text);
		for(int i=0; i<objs.size(); i++) {
			Map<String, Object> map = objs.get(i);
			String sentence = (String) map.get("sentence");
			if(null != sentence && !"".equals(sentence) ) {
				senList.add(sentence);
			}
		}
		return String.join(",", senList);
	}*/

	public static void main(String[] args) {
		String yx = "油旋这一济南名小吃，外形似螺旋，表面油润金黄，一股浓郁的葱油香气从中空的旋纹当中散发出来，真的很香。济南人吃油旋多是趁热吃，再配一碗鸡丝馄饨，可谓物美价廉，妙不可言。油旋有圆形和椭圆形两种。更有精细者，在油旋成熟后捅一空洞，磕入一个鸡蛋，再入炉烘烤一会，鸡蛋与油旋成为一体，食之更美。"
				+ "在泉城济南的大观园、芙蓉街、县西巷等美食街上，每到就餐时间，你会看到会有不少人在一些摊点前排起长队买油旋。而其中比较有名的，估计第一家就要属大观园的“油旋张”了，这还是季羡林老先生当年题的字，不大的摊子前面，每天都有好多人在排队，可见其味道确实不错！";
		String text = "人民网/n 1月1日/nt 讯/g 据/p 《/w [/w 纽约/ns 时报/n ]/w 》/w 报道/v ,/w 美国/ns 华尔街/ns 股市/n 在/p 2013年/nt 的/u 最后/nd 一天/mq 继续/v 上涨/v 。/w\r\n 和/c [/w 全球/n 股市/n ]/w 一样/u ,/w 都/d 以/p [/w 最高/a 纪录/n ]/w 或/c 接近/v [/w 最高/a 纪录/n ]/w 结束/v 本/r 年/nt 的/u 交易/v 。/w";
        String text_seg = "油旋 这 一 济南 名小吃 ， 外形 似 螺旋 ， 表面 油润金黄 ， 一股 浓郁 的 葱油 香气 从 中空 的 旋纹 当 中 散发 出来 ， 真 的 很 香 。 济南人 吃 油旋 多 是 趁 热 吃 ， 再 配 一 碗 鸡丝 馄饨 ， 可 谓 物美价廉 ， 妙不可言 。 油旋 有 圆形 和 椭圆形 两种 。 更 有 精细 者 ， 在 油旋 成熟 后 捅 一 空洞 ， 磕 入 一 个 鸡蛋 ， 再 入 炉 烘烤 一会 ， 鸡蛋 与 油旋 成 为 一体 ， 食 之 更 美 。 "
                + "在 泉城 济南 的 大观园 、 芙蓉街 、 县西巷 等 美食街 上 ， 每 到 就餐 时间 ， 你 会 看 到 会 有 不少 人 在 一些 摊点 前 排起 长队 买 油旋 。 而 其中 比较 有名 的 ， 估计 第一家 就 要 属 大观园 的 “油旋张” 了 ， 这 还 是 季羡林 老先生 当年 题 的 字 ， 不 大 的 摊子 前 面 ， 每天 都 有 好多 人 在 排队 ， 可 见 其 味道 确实 不错 ！ ";
		String text_seg1 = "接警员/n 李佳桐/nh 接到/v 家住/v 历下区/ns 利农庄路/ns 上/nd 鸿苑雅士园/ns 小区/n 王风莲/nh 报警/v 称/v ,/wp 2018年3月12日晚6点半/nt 左右/nd ,/wp 她/r 从/p 山大南路/ns 附近/nd 一家超市/ns 出来/v ,/wp 准备/v 将/d 物品/n 放到/v 自行车/n 上/nd 。/wp 就/d 在/p 她/r 倒腾/v 刚/d 买/v 的/u 物品/n 时/nt ,/wp 一/m 男子/n 趁/p 其/r 不注意/v ,/wp 迅速/a 抢走/v 放在/v 购物车/n 上/nd 的/u 手提包/n ,/wp 转身/v 就/d 跑/v 。/wp 包/v 内有价值/n 4000/m 元/mq 手机/n 一部/mq 、/wp 现金/n 约/d 300/m 元/mq 。/wp 王风莲/nh 随即/d 向/p 历下区/ns 派出所/ni 报警/v 被抢劫/n 。/wp ";

		List<String> customStopDict = new ArrayList<String>();//自定义停用词
		String size = "2";// 输出摘要句子个数
		double d = 0.85;// 阻尼系数
		int max_iter = 200;// 迭代次数
		double min_diff = 0.0001;// 收敛系数
		int lang = 0;// 语言(Chinese/English)
		int type = 0; // 停用词表使用方式，0:系统停用词；1:自定义停用词;2:合并
		SummaryImpl summary = new SummaryImpl(customStopDict,d,max_iter,min_diff,size,lang,type);
		System.out.println(summary.summary(text_seg1));
	}
}
