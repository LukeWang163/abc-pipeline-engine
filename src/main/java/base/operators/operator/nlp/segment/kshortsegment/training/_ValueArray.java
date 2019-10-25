package base.operators.operator.nlp.segment.kshortsegment.training;

public class _ValueArray<V> {
	V[] value;
	int offset;

	public _ValueArray(V[] value) {
		this.value = value;
	}

	public V nextValue() {
		return value[offset++];
	}

	/**
	 * 仅仅给子类用，不要用
	 */
	protected _ValueArray() {
	}

	public _ValueArray setValue(V[] value) {
		this.value = value;
		return this;
	}
}
