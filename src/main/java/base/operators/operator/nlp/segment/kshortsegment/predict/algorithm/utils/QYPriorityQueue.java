package base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils;

import java.util.LinkedList;
import java.util.List;

public class QYPriorityQueue<E extends BaseElementWithWeight> {
	private List<E> elementWeightPairList = new LinkedList<E>();
	private int limitSize = -1;
	private boolean isIncremental = false;

	/**
	 * Default constructor.
	 */
	public QYPriorityQueue() {
	}

	/**
	 * Constructor.
	 * 
	 * @param pLimitSize
	 */
	public QYPriorityQueue(int pLimitSize, boolean pIsIncremental) {
		limitSize = pLimitSize;
		isIncremental = pIsIncremental;
	}

	@Override
	public String toString() {
		return elementWeightPairList.toString();
	}

	/**
	 * Binary search is exploited to find the right position of the new element.
	 * 
	 * @param weight
	 * @return the position of the new element
	 */
	private int binLocatePos(double weight, boolean isIncremental) {
		int mid = 0;
		int low = 0;
		int high = elementWeightPairList.size() - 1;
		//
		while (low <= high) {
			mid = (low + high) / 2;
			if (elementWeightPairList.get(mid).getWeight() == weight) {
				return mid + 1;
			}

			if (isIncremental) {
				if (elementWeightPairList.get(mid).getWeight() < weight) {
					high = mid - 1;
				} else {
					low = mid + 1;
				}
			} else {
				if (elementWeightPairList.get(mid).getWeight() > weight) {
					high = mid - 1;
				} else {
					low = mid + 1;
				}
			}
		}
		return low;
	}

	/**
	 * Add a new element in the queue.
	 * 
	 * @param element
	 */
	public void add(E element) {
		elementWeightPairList.add(binLocatePos(element.getWeight(), isIncremental), element);

		if (limitSize > 0 && elementWeightPairList.size() > limitSize) {
			int sizeOfResults = elementWeightPairList.size();
			elementWeightPairList.remove(sizeOfResults - 1);
		}
	}

	/**
	 * It only reflects the size of the current results.
	 * 
	 * @return
	 */
	public int size() {
		return elementWeightPairList.size();
	}

	/**
	 * Get the i th element.
	 * 
	 * @param i
	 * @return
	 */
	public E get(int i) {
		if (i >= elementWeightPairList.size()) {
			System.err.println("The result :" + i + " doesn't exist!!!");
		}
		return elementWeightPairList.get(i);
	}

	/**
	 * Get the first element, and then remove it from the queue.
	 * 
	 * @return
	 */
	public E poll() {
		E ret = elementWeightPairList.get(0);
		elementWeightPairList.remove(0);
		return ret;
	}

	/**
	 * Check if it's empty.
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return elementWeightPairList.isEmpty();
	}

}
