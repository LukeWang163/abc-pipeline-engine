/**
 * This file is part of the RapidMiner Belt project.
 * Copyright (C) 2017-2019 RapidMiner GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see 
 * https://www.gnu.org/licenses/.
 */

package base.operators.belt.column;

import base.operators.belt.reader.CategoricalReader;
import base.operators.belt.reader.NumericReader;
import base.operators.belt.reader.ObjectReader;
import base.operators.belt.util.Mapping;
import base.operators.belt.util.Order;
import base.operators.belt.util.Sorting;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

import static base.operators.belt.column.MappedDoubleArrayColumn.MAPPING_THRESHOLD;
import static base.operators.belt.util.IntegerFormats.*;

/**
 * Column with data associated to integer categories twisted by a mapping that selects a (reordered) subset of the
 * columns. Data can be accessed via a {@link CategoricalReader}, an {@link ObjectReader}, or a {@link NumericReader}
 * together with access to the dictionary by {@link #getDictionary(Class)}.
 *
 * @author Gisa Meier, Michael Knopf
 */
class MappedCategoricalColumn<R> extends CategoricalColumn<R> implements CacheMappedColumn {

	private static final String NULL_DATA = "Data must not be null";
	private static final String NULL_CATEGORY_MAPPING = "Categorical mapping must not be null";

	private final int[] mapping;

	private final Format format;
	private final PackedIntegers byteData;
	private final short[] shortData;
	private final int[] intData;

	private final Dictionary<R> dictionary;

	MappedCategoricalColumn(ColumnType<R> type, PackedIntegers data, Dictionary<R> dictionary, int[] mapping) {
		super(type, mapping.length);
		if (!BYTE_BACKED_FORMATS.contains(data.format())) {
			throw new IllegalArgumentException("Given data management not backed by byte array");
		}
		this.format = data.format();
		this.byteData = Objects.requireNonNull(data, NULL_DATA);
		this.shortData = null;
		this.intData = null;
		this.mapping = mapping;
		this.dictionary = Objects.requireNonNull(dictionary, NULL_CATEGORY_MAPPING);
	}

	MappedCategoricalColumn(ColumnType<R> type, short[] data, Dictionary<R> dictionary, int[] mapping) {
		super(type, mapping.length);
		this.format = Format.UNSIGNED_INT16;
		this.byteData = null;
		this.shortData = Objects.requireNonNull(data, NULL_DATA);
		this.intData = null;
		this.mapping = mapping;
		this.dictionary = Objects.requireNonNull(dictionary, NULL_CATEGORY_MAPPING);
	}

	MappedCategoricalColumn(ColumnType<R> type, int[] data, Dictionary<R> dictionary, int[] mapping) {
		super(type, mapping.length);
		this.format = Format.SIGNED_INT32;
		this.byteData = null;
		this.shortData = null;
		this.intData = data;
		this.mapping = mapping;
		this.dictionary = Objects.requireNonNull(dictionary, NULL_CATEGORY_MAPPING);
	}

	@Override
	void fillFromUInt2(double[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpNumericUInt2(j);
		}
	}

	@Override
	void fillFromUInt4(double[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpNumericUInt4(j);
		}
	}

	@Override
	void fillFromUInt8(double[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpNumericUInt8(j);
		}
	}

	@Override
	void fillFromUInt16(double[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpNumericUInt16(j);
		}
	}

	@Override
	void fillFromInt32(double[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpNumericInt32(j);
		}
	}

	@Override
	void fillFromUInt2(double[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpNumericUInt2(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromUInt4(double[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpNumericUInt4(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromUInt8(double[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpNumericUInt8(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromUInt16(double[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpNumericUInt16(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromInt32(double[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpNumericInt32(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromUInt2(int[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpIndexUInt2(j);
		}
	}

	@Override
	void fillFromUInt4(int[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpIndexUInt4(j);
		}
	}

	@Override
	void fillFromUInt8(int[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpIndexUInt8(j);
		}
	}

	@Override
	void fillFromUInt16(int[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpIndexUInt16(j);
		}
	}

	@Override
	void fillFromInt32(int[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpIndexInt32(j);
		}
	}

	@Override
	void fillFromUInt2(int[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpIndexUInt2(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromUInt4(int[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpIndexUInt4(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromUInt8(int[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpIndexUInt8(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromUInt16(int[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpIndexUInt16(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromInt32(int[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpIndexInt32(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromUInt2(Object[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpUInt2(j);
		}
	}

	@Override
	void fillFromUInt4(Object[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpUInt4(j);
		}
	}

	@Override
	void fillFromUInt8(Object[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpUInt8(j);
		}
	}

	@Override
	void fillFromUInt16(Object[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpUInt16(j);
		}
	}

	@Override
	void fillFromInt32(Object[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			array[i] = lookUpInt32(j);
		}
	}

	@Override
	void fillFromUInt2(Object[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpUInt2(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromUInt4(Object[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpUInt4(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromUInt8(Object[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpUInt8(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromUInt16(Object[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpUInt16(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	void fillFromInt32(Object[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			array[arrayIndex] = lookUpInt32(rowIndex);
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	/**
	 * Returns the categorical mapping associated with this column.
	 *
	 * @return the categorical mapping
	 */
	@Override
	protected Dictionary<R> getDictionary() {
		return dictionary;
	}

	/**
	 * The Row mapping.
	 *
	 * @return the mapping
	 */
	protected int[] getRowMapping() {
		return mapping;
	}

	@Override
	public Format getFormat() {
		return format;
	}

	@Override
	protected PackedIntegers getByteData() {
		return byteData;
	}

	@Override
	protected short[] getShortData() {
		return shortData;
	}

	@Override
	protected int[] getIntData() {
		return intData;
	}

	@Override
	public int[] sort(Order order) {
		Comparator<R> comparator = type().comparator();
		if(comparator==null){
			throw new UnsupportedOperationException();
		}
		Comparator<R> comparatorWithNull = Comparator.nullsLast(comparator);
		switch (format) {
			case UNSIGNED_INT2:
				return Sorting.sort(mapping.length,
						(a, b) -> comparatorWithNull.compare(lookUpUInt2(a), lookUpUInt2(b)),
						order);
			case UNSIGNED_INT4:
				return Sorting.sort(mapping.length,
						(a, b) -> comparatorWithNull.compare(lookUpUInt4(a), lookUpUInt4(b)),
						order);
			case UNSIGNED_INT8:
				return Sorting.sort(mapping.length,
						(a, b) -> comparatorWithNull.compare(lookUpUInt8(a), lookUpUInt8(b)),
						order);
			case UNSIGNED_INT16:
				return Sorting.sort(mapping.length,
						(a, b) -> comparatorWithNull.compare(lookUpUInt16(a), lookUpUInt16(b)),
						order);
			case SIGNED_INT32:
				return Sorting.sort(mapping.length,
						(a, b) -> comparatorWithNull.compare(lookUpInt32(a), lookUpInt32(b)),
						order);
			default:
				throw new IllegalStateException("Unknown data format");
		}
	}

	@Override
	public Column map(int[] remapping, boolean preferView, Map<int[], int[]> cache) {
		int[] mergedMapping = cache.computeIfAbsent(getRowMapping(), k -> Mapping.merge(remapping, mapping));
		boolean keepView = preferView || mergedMapping.length > size() * MAPPING_THRESHOLD;
		return keepView ? deriveMappedColumn(mergedMapping) : deriveSimpleColumn(mergedMapping);
	}

	@Override
    Column map(int[] remapping, boolean preferView) {
		int[] mergedMapping = Mapping.merge(remapping, mapping);
		boolean keepView = preferView || mergedMapping.length > size() * MAPPING_THRESHOLD;
		return keepView ? deriveMappedColumn(mergedMapping) : deriveSimpleColumn(mergedMapping);
	}

	private Column deriveMappedColumn(int[] mapping) {
		return getCategoricalColumn(dictionary, mapping);
	}

	private Column deriveSimpleColumn(int[] mapping) {
		switch (format) {
			case UNSIGNED_INT2:
			case UNSIGNED_INT4:
			case UNSIGNED_INT8:
				PackedIntegers mappedBytes = Mapping.apply(byteData, mapping);
				return new SimpleCategoricalColumn<>(type(), mappedBytes, dictionary);
			case UNSIGNED_INT16:
				short[] mappedShorts = Mapping.apply(shortData, mapping);
				return new SimpleCategoricalColumn<>(type(), mappedShorts, dictionary);
			case SIGNED_INT32:
				int[] mappedIntegers = Mapping.apply(intData, mapping);
				return new SimpleCategoricalColumn<>(type(), mappedIntegers, dictionary);
			default:
				throw new IllegalStateException();
		}
	}

	@Override
	protected CategoricalColumn<R> swapDictionary(Dictionary<R> newDictionary) {
		return getCategoricalColumn(newDictionary, mapping);
	}

	private CategoricalColumn<R> getCategoricalColumn(Dictionary<R> dictionary, int[] mapping) {
		switch (format) {
			case UNSIGNED_INT2:
			case UNSIGNED_INT4:
			case UNSIGNED_INT8:
				return new MappedCategoricalColumn<>(type(), byteData, dictionary, mapping);
			case UNSIGNED_INT16:
				return new MappedCategoricalColumn<>(type(), shortData, dictionary, mapping);
			case SIGNED_INT32:
				return new MappedCategoricalColumn<>(type(), intData, dictionary, mapping);
			default:
				throw new IllegalStateException();
		}
	}

	@Override
    CategoricalColumn<R> remap(Dictionary<R> newDictionary, int[] remapping) {
		switch (format) {
			case UNSIGNED_INT2:
			case UNSIGNED_INT4:
			case UNSIGNED_INT8:
				return new RemappedMappedCategoricalColumn<>(type(), byteData, newDictionary, remapping, mapping);
			case UNSIGNED_INT16:
				return new RemappedMappedCategoricalColumn<>(type(), shortData, newDictionary, remapping, mapping);
			case SIGNED_INT32:
				return new RemappedMappedCategoricalColumn<>(type(), intData, newDictionary, remapping, mapping);
			default:
				throw new IllegalStateException();
		}
	}

	private int lookUpIndexUInt2(int i) {
		int position = mapping[i];
		if (position >= 0 && position < byteData.size()) {
			return readUInt2(byteData.data(), position);
		} else {
			return 0;
		}
	}

	private int lookUpIndexUInt4(int i) {
		int position = mapping[i];
		if (position >= 0 && position < byteData.size()) {
			return readUInt4(byteData.data(), position);
		} else {
			return 0;
		}
	}

	private int lookUpIndexUInt8(int i) {
		int position = mapping[i];
		if (position >= 0 && position < byteData.size()) {
			return Byte.toUnsignedInt(byteData.data()[position]);
		} else {
			return 0;
		}
	}

	private int lookUpIndexUInt16(int i) {
		int position = mapping[i];
		if (position >= 0 && position < shortData.length) {
			return Short.toUnsignedInt(shortData[position]);
		} else {
			return 0;
		}
	}

	private int lookUpIndexInt32(int i) {
		int position = mapping[i];
		if (position >= 0 && position < intData.length) {
			return intData[position];
		} else {
			return 0;
		}
	}

	private double lookUpNumericUInt2(int i) {
		int position = mapping[i];
		if (position >= 0 && position < byteData.size()) {
			int datum = readUInt2(byteData.data(), position);
			return datum == CategoricalReader.MISSING_CATEGORY ? Double.NaN : datum;
		} else {
			return Double.NaN;
		}
	}

	private double lookUpNumericUInt4(int i) {
		int position = mapping[i];
		if (position >= 0 && position < byteData.size()) {
			int datum = readUInt4(byteData.data(), position);
			return datum == CategoricalReader.MISSING_CATEGORY ? Double.NaN : datum;
		} else {
			return Double.NaN;
		}
	}

	private double lookUpNumericUInt8(int i) {
		int position = mapping[i];
		if (position >= 0 && position < byteData.size()) {
			int datum = Byte.toUnsignedInt(byteData.data()[position]);
			return datum == CategoricalReader.MISSING_CATEGORY ? Double.NaN : datum;
		} else {
			return Double.NaN;
		}
	}

	private double lookUpNumericUInt16(int i) {
		int position = mapping[i];
		if (position >= 0 && position < shortData.length) {
			int datum = Short.toUnsignedInt(shortData[position]);
			return datum == CategoricalReader.MISSING_CATEGORY ? Double.NaN : datum;
		} else {
			return Double.NaN;
		}
	}

	private double lookUpNumericInt32(int i) {
		int position = mapping[i];
		if (position >= 0 && position < intData.length) {
			int datum = intData[position];
			return datum == CategoricalReader.MISSING_CATEGORY ? Double.NaN : datum;
		} else {
			return Double.NaN;
		}
	}

	private R lookUpUInt2(int i) {
		int position = mapping[i];
		if (position >= 0 && position < byteData.size()) {
			int datum = readUInt2(byteData.data(), position);
			if (datum == CategoricalReader.MISSING_CATEGORY) {
				return null;
			} else {
				return dictionary.get(datum);
			}
		} else {
			return null;
		}
	}

	private R lookUpUInt4(int i) {
		int position = mapping[i];
		if (position >= 0 && position < byteData.size()) {
			int datum = readUInt4(byteData.data(), position);
			if (datum == CategoricalReader.MISSING_CATEGORY) {
				return null;
			} else {
				return dictionary.get(datum);
			}
		} else {
			return null;
		}
	}

	private R lookUpUInt8(int i) {
		int position = mapping[i];
		if (position >= 0 && position < byteData.size()) {
			int datum = Byte.toUnsignedInt(byteData.data()[position]);
			if (datum == CategoricalReader.MISSING_CATEGORY) {
				return null;
			} else {
				return dictionary.get(datum);
			}
		} else {
			return null;
		}
	}

	private R lookUpUInt16(int i) {
		int position = mapping[i];
		if (position >= 0 && position < shortData.length) {
			int datum = Short.toUnsignedInt(shortData[position]);
			if (datum == CategoricalReader.MISSING_CATEGORY) {
				return null;
			} else {
				return dictionary.get(datum);
			}
		} else {
			return null;
		}
	}

	private R lookUpInt32(int i) {
		int position = mapping[i];
		if (position >= 0 && position < intData.length) {
			int datum = intData[position];
			if (datum == CategoricalReader.MISSING_CATEGORY) {
				return null;
			} else {
				return dictionary.get(datum);
			}
		} else {
			return null;
		}
	}

}
