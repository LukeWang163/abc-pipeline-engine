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

import base.operators.belt.util.Mapping;
import base.operators.belt.util.Order;
import base.operators.belt.util.Sorting;

import java.util.Map;
import java.util.Objects;


/**
 * A {@link Column} implementation backed by a double array with remapped indices. The remapping is evaluated lazily.
 *
 * @author Gisa Schaefer
 */
class MappedDoubleArrayColumn extends NumericColumn implements CacheMappedColumn {

	/** Mappings of a relative size smaller than this threshold are implemented via a deep copy. */
	static final double MAPPING_THRESHOLD = 0.1;

	private final ColumnType<Void> columnType;
	private final double[] data;
	private final int[] mapping;

	/**
	 * Creates a new column where the data from src is reordered by the given mapping.
	 *
	 * @param src
	 *            the data source
	 * @param mapping
	 *            the new order
	 * @param type the column type id
	 * @throws NullPointerException
	 *             if source array or mapping is {@code null}
	 */
	MappedDoubleArrayColumn(TypeId type, double[] src, int[] mapping) {
		super(Objects.requireNonNull(mapping, "Mapping must not be null").length);
		this.columnType = type == TypeId.INTEGER ? ColumnTypes.INTEGER : ColumnTypes.REAL;
		this.data = Objects.requireNonNull(src, "Source array must not be null");
		this.mapping = mapping;
	}

	@Override
	public void fill(double[] array, int rowIndex) {
		int max = Math.min(rowIndex + array.length, mapping.length);
		for (int i = 0, j = rowIndex; j < max; i++, j++) {
			int position = mapping[j];
			if (position < 0 || position >= data.length) {
				array[i] = Double.NaN;
			} else {
				array[i] = data[position];
			}
		}
	}

	@Override
	public void fill(double[] array, int startIndex, int arrayOffset, int arrayStepSize) {
		if (arrayStepSize < 1) {
			throw new IllegalArgumentException("step size must not be smaller than 1");
		}
		int max = Math.min(startIndex + (array.length - arrayOffset - 1) / arrayStepSize + 1, mapping.length);
		int rowIndex = startIndex;
		int arrayIndex = arrayOffset;
		while (rowIndex < max) {
			int position = mapping[rowIndex];
			if (position < 0 || position >= data.length) {
				array[arrayIndex] = Double.NaN;
			} else {
				array[arrayIndex] = data[position];
			}
			arrayIndex += arrayStepSize;
			rowIndex++;
		}
	}

	@Override
	public ColumnType<Void> type() {
		return columnType;
	}

	@Override
	Column map(int[] mapping, boolean preferView) {
		return mapMerged(Mapping.merge(mapping, this.mapping), preferView);
	}

	@Override
	public Column map(int[] mapping, boolean preferView, Map<int[], int[]> cache) {
		int[] merged = cache.computeIfAbsent(this.mapping, k -> Mapping.merge(mapping, this.mapping));
		return mapMerged(merged, preferView);
	}

	@Override
	public int[] sort(Order order) {
		return Sorting.sort(Mapping.apply(data, mapping), order);
	}

	private Column mapMerged(int[] mergedMapping, boolean preferView) {
		if (preferView || mergedMapping.length > data.length * MAPPING_THRESHOLD) {
			return new MappedDoubleArrayColumn(columnType.id(), data, mergedMapping);
		} else {
			return new DoubleArrayColumn(columnType.id(), Mapping.apply(data, mergedMapping));
		}
	}

}
