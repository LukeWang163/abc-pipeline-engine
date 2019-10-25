package base.operators.operator.nlp.ner.location;

public class ArrayUtils {
	public static final int INDEX_NOT_FOUND = -1;

	public static int indexOf(final Object[] array, final Object objectToFind) {

		if (array == null) {
			return INDEX_NOT_FOUND;
		}
		int startIndex = 0;

		if (objectToFind == null) {
			for (int i = startIndex; i < array.length; i++) {
				if (array[i] == null) {
					return i;
				}
			}
		} else {
			for (int i = startIndex; i < array.length; i++) {
				if (objectToFind.equals(array[i])) {
					return i;
				}
			}
		}
		return INDEX_NOT_FOUND;
	}

}
