package base.operators.operator.nlp.segment.kshortsegment.training;

import java.util.*;

/**
 * 转移矩阵词典制作工具
 */
public class TMDictionaryMaker {

	public Map<String, Map<String, Integer>> transferMatrix;

	public TMDictionaryMaker() {
		transferMatrix = new TreeMap<String, Map<String, Integer>>();
	}

	/**
	 * 添加一个转移例子，会在内部完成统计
	 *
	 * @param first
	 * @param second
	 */
	public void addPair(String first, String second) {
		Nature firstPos=Nature.fromString(first);
		Nature secondPos=Nature.fromString(second);
		if(firstPos!=null  && secondPos!=null){
			Map<String, Integer> firstMatrix = transferMatrix.get(first);
			if (firstMatrix == null) {
				firstMatrix = new TreeMap<String, Integer>();
				transferMatrix.put(first, firstMatrix);
			}
			Integer frequency = firstMatrix.get(second);
			if (frequency == null)
				frequency = 0;
			firstMatrix.put(second, frequency + 1);
		}
	}

	@Override
	public String toString() {
		Set<String> labelSet = new TreeSet<String>();
		for (Map.Entry<String, Map<String, Integer>> first : transferMatrix.entrySet()) {
			labelSet.add(first.getKey());
			labelSet.addAll(first.getValue().keySet());
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(' ');
		for (String key : labelSet) {
			sb.append(',');
			sb.append(key);
		}
		sb.append('\n');
		for (String first : labelSet) {
			Map<String, Integer> firstMatrix = transferMatrix.get(first);
			if (firstMatrix == null)
				firstMatrix = new TreeMap<String, Integer>();
			sb.append(first);
			for (String second : labelSet) {
				sb.append(',');
				Integer frequency = firstMatrix.get(second);
				if (frequency == null)
					frequency = 0;
				sb.append(frequency);
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	public List<String> toList() {
		List<String> result = new ArrayList<>();
		Set<String> labelSet = new TreeSet<String>();
		for (Map.Entry<String, Map<String, Integer>> first : transferMatrix.entrySet()) {
			labelSet.add(first.getKey());
			labelSet.addAll(first.getValue().keySet());
		}
		StringBuilder sb = new StringBuilder();
		sb.append(' ');
		for (String key : labelSet) {
			sb.append(',');
			sb.append(key);
		}
		result.add(sb.toString());

		for (String first : labelSet) {
			StringBuilder sbLabel = new StringBuilder();
			Map<String, Integer> firstMatrix = transferMatrix.get(first);
			if (firstMatrix == null)
				firstMatrix = new TreeMap<String, Integer>();
			sbLabel.append(first);
			for (String second : labelSet) {
				sbLabel.append(',');
				Integer frequency = firstMatrix.get(second);
				if (frequency == null)
					frequency = 0;
				sbLabel.append(frequency);
			}
			result.add(sbLabel.toString());
		}
		return result;
	}

}
