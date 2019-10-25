package base.operators.operator.nlp.ner.location;
/**
 * 地址消歧的特征项定义
 * @author 王建华
 *
 */
public class FV {
	/**
	 * 歧义词存在唯一最高层地址编码特征标志
	 */
	public static final String TOP_CODE="TOP_CODE";
	/**
	 * 歧义词存在唯一同地址编码特征标志
	 */
	public static final String SAME_CODE="SAME_CODE";
	/**
	 * 歧义词上级在无歧义地址涵盖的字面编码上下范围
	 */
	public static final String PARENT_CODE="PARENT_CODE";
	/**
	 * 无歧义词上级在歧义地址涵盖的字面编码上下范围
	 */
	public static final String AM_PARENT_CODE="AM_PARENT_CODE";
	/**
	 * 两个地址存在共同的上级编码
	 */
	public static final String SAME_FATHER="SAME_FATHER";
	/**
	 * 词与确定无歧义词所有词间的最短路径（只通过根节点以及直接上下级路径）
	 */
	public static final String MIN_DISTANCE="MIN_DISTANCE";
	/**
	 * 到全局最短路径的上级节点的距离
	 */
	public static final String MIN_PARENT_DISTANCE="MIN_PARENT_DISTANCE";
	



}
