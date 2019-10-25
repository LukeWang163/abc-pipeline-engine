
package base.operators.operator.nlp.ner.time;

/**
 * <p>
 * 范围时间的默认时间点
 * <p>
 * 
 */
public enum RangeTimeEnum {
	
	/**
	 * add by Binson
	 * 添加上旬、中旬、下旬的枚举
	 */
	month_early(1),
	month_mid(15),
	month_late(28),
	
	day_break(3),
	early_morning(8), //早
	morning(10), //上午
	noon(12), //中午、午间
	afternoon(15), //下午、午后
	night(18), //晚上、傍晚
	lateNight(20), //晚、晚间
	midNight(23);  //深夜
	
	private int time = 0;

	/**
	 * @param time
	 */
	private RangeTimeEnum(int time) {
		this.setTime(time);
	}

	/**
	 * @return the time
	 */
	public int getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(int time) {
		this.time = time;
	}
	
}
