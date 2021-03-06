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

package base.operators.belt.table;

import base.operators.belt.buffer.NumericBuffer;
import base.operators.belt.column.Column;
import base.operators.belt.column.ColumnType;

import java.util.Arrays;


/**
 * Temporary random access buffer for row-wise writing that can be used to define a
 * {@link base.operators.belt.column.ObjectColumn}.
 *
 * @author Gisa Meier
 */
class ObjectWriter<T> implements ComplexWriter {

	private final ColumnType<T> type;
	private Object[] data;
	private boolean frozen = false;
	private int size;


	/**
	 * Creates a buffer of the given length.
	 *
	 * @param length
	 * 		the length of the buffer
	 */
	ObjectWriter(ColumnType<T> type, int length) {
		data = new Object[length];
		if (type.id() != Column.TypeId.CUSTOM || type.category() != Column.Category.OBJECT) {
			throw new IllegalArgumentException("Column type must be custom and of category object");
		}
		this.type = type;
		size = length;
	}


	ObjectWriter(ColumnType<T> type) {
		data = new Object[0];
		if (type.id() != Column.TypeId.CUSTOM || type.category() != Column.Category.OBJECT) {
			throw new IllegalArgumentException("Column type must be custom and of category object");
		}
		this.type = type;
		size = 0;
	}


	/**
	 * Freezes this column buffer and copies the data to the final size.
	 */
	private void freeze() {
		frozen = true;
		if (data.length > size) {
			data = Arrays.copyOf(data, size);
		}
	}

	@Override
	public Column toColumn() {
		freeze();
		return ColumnAccessor.get().newObjectColumn(type, data);
	}

	/**
	 * Ensures that the buffer has the capacity for the given length.
	 */
	private void resize(int length) {
		if (frozen) {
			throw new IllegalStateException(NumericBuffer.BUFFER_FROZEN_MESSAGE);
		}
		size = length;
		if (length <= data.length) {
			return;
		}
		int oldLength = data.length;
		int newLength = Math.max(Math.max(NumericRowWriter.MIN_NON_EMPTY_SIZE, length), oldLength + (oldLength >>> 1));
		data = Arrays.copyOf(data, newLength);
	}

	@Override
	public void fill(Object[] buffer, int startIndex, int bufferOffset, int bufferStepSize, int height) {
		resize(height);
		int remainder = buffer.length % bufferStepSize > bufferOffset ? 1 : 0;
		int max = Math.min(startIndex + buffer.length / bufferStepSize + remainder, height);
		int copyIndex = startIndex;
		int bufferIndex = bufferOffset;
		while (copyIndex < max) {
			data[copyIndex] = buffer[bufferIndex];
			bufferIndex += bufferStepSize;
			copyIndex++;
		}
	}

}