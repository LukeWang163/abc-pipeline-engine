package base.operators.operator.nlp.segment.kshortsegment.predict.segment;

public class Define {

	/**
	 * 句子的开始
	 */
	public final static String TAG_BIGIN = "始##始";
	/**
	 * 句子的结束
	 */
	public final static String TAG_END = "末##末";
	/**
	 * 其它
	 */
	public final static String TAG_OTHER = "未##它";
	/**
	 * 数词 m
	 */
	public final static String TAG_NUMBER = "未##数";
	/**
	 * 时间 t
	 */
	public final static String TAG_TIME = "未##时";
	/**
	 * 字符串 x
	 */
	public final static String TAG_LETTER = "未##串";
	/**
	 * 人名 nh
	 */
	public final static String TAG_PERSON = "未##人";
	/**
	 * 地址 ns
	 */
	public final static String TAG_PLACE = "未##地";
	/**
	 * 机构ni
	 */
	public final static String TAG_ORG = "未##机";

	/**
	 *专有名词nz
	 */
	public final static String TAG_SPECIAL = "未##专";

	public static  int MAX_FREQUENCY = 30000000;

}
