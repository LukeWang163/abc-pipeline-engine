package base.operators.operator.nlp.fjconvert.core;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * OpenCC converts Simplified Chinese to Traditional Chinese and vice versa
 */
public class ChineseConvert
{

	public static final Map<String, String> CONVERSIONS = new HashMap<>();
	/**
	 * Traditional Chinese (Taiwan standard) to Simplified Chinese
	 */
	public static final int tw2s = 0;
	/**
	 * "Traditional Chinese (Taiwan standard) to Simplified Chinese (with
	 * phrases)
	 */
	public static final int tw2sp = 1;
	/**
	 * Traditional Chinese to Simplified Chinese
	 */
	public static final int t2s = 2;
	/**
	 * Simplified Chinese to Traditional Chinese
	 */
	public static final int s2t = 3;
	/**
	 * Simplified Chinese to Traditional Chinese (Taiwan standard)
	 */
	public static final int s2tw = 4;
	/**
	 * Simplified Chinese to Traditional Chinese (Taiwan standard, with phrases)
	 */
	public static final int s2twp = 5;
	/**
	 * Simplified Chinese to Traditional Chinese (Hong Kong standard)
	 */
	public static final int s2hk = 6;
	/**
	 * Traditional Chinese to Traditional Chinese (Hong Kong standard)
	 */
	public static final int t2hk = 7;
	/**
	 * Traditional Chinese (Hong Kong standard) to Simplified Chinese
	 */
	public static final int hk2s = 8;
	/**
	 * Traditional Chinese to Traditional Chinese (Taiwan standard)
	 */
	public static final int t2tw = 9;
	private static final String CONVERTER_TYPE[] = new String[10];
	static
	{
		CONVERTER_TYPE[tw2s] = "tw2s";
		CONVERTER_TYPE[tw2sp] = "tw2sp";
		CONVERTER_TYPE[t2s] = "t2s";
		CONVERTER_TYPE[s2t] = "s2t";
		CONVERTER_TYPE[s2tw] = "s2tw";
		CONVERTER_TYPE[s2twp] = "s2twp";
		CONVERTER_TYPE[s2hk] = "s2hk";
		CONVERTER_TYPE[t2hk] = "t2hk";
		CONVERTER_TYPE[hk2s] = "hk2s";
		CONVERTER_TYPE[t2tw] = "t2tw";
	}

	private static final int NUM_OF_CONVERTERS = 10;
	private static final ChineseConvert[] CONVERTERS = new ChineseConvert[NUM_OF_CONVERTERS];
	static Map<String, TreeMap<String, String>> allDictMap = new HashMap<String, TreeMap<String, String>>();
	// 定义存放10个对象的数组

	DictManager dictManager;
	public static int type;

	/**
	 *
	 * @param converterType
	 *            0 for tw2s and 4 for s2tw
	 * @return
	 */
	public static ChineseConvert getInstance(int converterType)
	{
		type = converterType;
		if (converterType >= 0 && converterType < NUM_OF_CONVERTERS)
		{

			if (CONVERTERS[converterType] == null)
			{
				synchronized (ChineseConvert.class)
				{
					if (CONVERTERS[converterType] == null)
					{
						CONVERTERS[converterType] = new ChineseConvert(CONVERTER_TYPE[converterType]);
					}
				}
			}
			return CONVERTERS[converterType];

		} else
		{
			return null;
		}
	}

	/**
	 * Convert API
	 *
	 * @param text
	 * @param converterType
	 * @return
	 */
	/*
	 * public static String convert(String text, int converterType) {
	 * ChineseConvert instance = getInstance(converterType); return
	 * instance.convert(text); }
	 */

	/**
	 * construct OpenCC with conversion
	 *
	 * @param config
	 *            options are "hk2s", "s2hk", "s2t", "s2tw", "s2twp", "t2hk",
	 *            "t2s", "t2tw", "tw2s", and "tw2sp"
	 */
	private ChineseConvert(String config)
	{
		dictManager = new DictManager(config);

		load(); // 装载字词典，并将大字词典拆分为小字词典
	}

	/**
	 *
	 * @return dict name
	 */
	public String getDictName()
	{
		return dictManager.getDictName();
	}

	/**
	 * set OpenCC a new conversion
	 *
	 * @param conversion
	 *            options are "hk2s", "s2hk", "s2t", "s2tw", "s2twp", "t2hk",
	 *            "t2s", "t2tw", "tw2s", and "tw2sp"
	 */
	private void setConversion(String conversion)
	{
		dictManager.setConfig(conversion);
	}

	/**
	 * Convert method
	 *
	 * @param in
	 * @return
	 */
	public String convert(String in)
	{
		int codePoint = in.codePointCount(0, in.length());
		// Check CodePoint 检查指定文本范围内Unicode代码点的数量
		/**
		 * Java中并非所有unicode字符都可以用一个char字符存储
		 * char采用UCS-2编码是一种淘汰的UTF-16编码，最多65536种形态，远少于当今unicode拥有11万字符的需求
		 * Java对后来新增的Unicode字符采用2个char字符拼出一个Unicode字符的方式表示
		 * 因而若字符串中存在新增的Unicode字符则令另返回的codePointCount<length
		 */
		int strLen = in.length();
		return convertNormalString(in);

	}

	/**
	 * Check a char
	 *
	 * @param codePoint
	 * @return
	 */
	private boolean isExtendCodePoint(int codePoint)
	{
		char c[] = Character.toChars(codePoint);
		if (c.length == 1)
			return false;
		return true;
	}

	private void load()
	{
		TreeMap<String, String> allDict = dictManager.getAllDictMap(); // allDictMap中配置的是根据转换类型加载的相应字典与词典
		SpinOffMap M1 = new SpinOffMap();
		allDictMap = M1.spinOffMap(allDict);
	}

	private String convertNormalString(String in)
	{
		return dealString(in, allDictMap);

	}

	public static String dealString(String st, Map<String, TreeMap<String, String>> allDictMap) // String
	{
		String part = st;
		while (part.length() > 0)
		{
			String partLength = "" + part.length();
			if (allDictMap.containsKey(partLength))
			{
				if (allDictMap.get(partLength).containsKey(part)
						|| (part.length() == 1 && !allDictMap.get(partLength).containsKey(part)))
				{
					String replace = part;
					// 对于上方的符合“或”if判断中的后一种，即单字符且字词典中无转换关系的，则保留原样输出；对于前一种，则按照字词典的对应关系转换后输出
					if (allDictMap.get(partLength).containsKey(part))
					{
						String[] right_value = allDictMap.get(partLength).get(replace).trim().split(" ");
						replace = right_value[0];
					}

					String partNew = st.substring(part.length()); // 去除前部被转换的词,
					// 对余下后部继续处理
					return replace + dealString(partNew, allDictMap);

				} else
				{
					part = part.substring(0, part.length() - 1); // 当长字符串匹配不成功时，从尾部减去一个字符，继续匹配
				}
			} else
			{
				part = part.substring(0, part.length() - 1); // 当长字符串匹配不成功时，从尾部减去一个字符，继续匹配
			}
		}
		return "";
	}
}
