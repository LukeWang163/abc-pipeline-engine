package base.operators.operator.nlp.ner.location;

public class ItemFeature {
	/**
	 * 词串
	 */
	private String word;
	/**
	 * 地址编码
	 */
	private String code;
	/**
	 * 对应的地址详细信息
	 */
	private LocationWrapper location;

	/**
	 * 匹配成功的特征标签
	 */
	private String featureLable;

	private String ruleCode;

	/**
	 * 构造函数
	 * 
	 * @param word
	 * @param location
	 * @param level
	 * @param featureLable
	 */
	public ItemFeature(String word, LocationWrapper location,
			String featureLable, String ruleCode) {
		this.word = word;
		this.location = location;
		this.featureLable = featureLable;
		this.code = location.getLocation().getCode();
		this.ruleCode = ruleCode;
	}

	public String getWord() {
		return word;
	}

	public String getCode() {
		return code;
	}

	public LocationWrapper getLocation() {
		return location;
	}

	public String getFeatureLable() {
		return featureLable;
	}

	public String toString() {
		return "[word:" + word + ",code:" + code + ",featureLable:"
				+ featureLable + ",ruleCode:" + ruleCode + "]";
	}

}
