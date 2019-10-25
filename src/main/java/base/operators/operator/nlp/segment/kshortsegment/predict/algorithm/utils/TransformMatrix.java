package base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils;

public class TransformMatrix<E extends Enum<E>> {
	Class<E> enumType;
	/**
	 * 储存转移矩阵
	 */
	int matrix[][];

	/**
	 * 储存每个标签出现的次数
	 */
	double total[];

	/**
	 * 所有标签出现的总次数
	 */
	double totalFrequency;

	// HMM的五元组
	/**
	 * 隐状态
	 */
	public int[] states;
	// int[] observations;
	/**
	 * 初始概率
	 */
	public double[] start_probability;
	/**
	 * 转移概率
	 */
	public double[][] transititon_probability;

	String[] labels;

	public TransformMatrix(Class<E> enumType, int matrix[][], int[] states, double[] start_probability,
                           double[][] transititon_probability, double total[], double totalFrequency, String labels[]) {
		this.enumType = enumType;
		this.matrix = matrix;
		this.states = states;
		this.start_probability = start_probability;
		this.transititon_probability = transititon_probability;
		this.total = total;
		this.totalFrequency = totalFrequency;
		this.labels = labels;
	}

	public int[][] getMatrix() {
		return matrix;
	}

	public void setMatrix(int[][] matrix) {
		this.matrix = matrix;
	}

	public double[] getTotal() {
		return total;
	}

	public double getTotalFrequency() {
		return totalFrequency;
	}

	public int[] getStates() {
		return states;
	}

	public double[] getStart_probability() {
		return start_probability;
	}

	public double[][] getTransititon_probability() {
		return transititon_probability;
	}

	public String[] getLabels() {
		return labels;
	}

	protected E convert(String label) {
		return Enum.valueOf(enumType, label);
	}

	/**
	 * 获取转移频次
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public int getFrequency(String from, String to) {
		return getFrequency(convert(from), convert(to));
	}

	/**
	 * 获取转移频次
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public int getFrequency(E from, E to) {
		return matrix[from.ordinal()][to.ordinal()];
	}

	/**
	 * 获取e的总频次
	 *
	 * @param e
	 * @return
	 */
	public double getTotalFrequency(E e) {
		return total[e.ordinal()];
	}

}
