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

package base.operators.belt.transform;


import base.operators.belt.buffer.Buffers;
import base.operators.belt.buffer.TimeBuffer;
import base.operators.belt.column.Column;
import base.operators.belt.reader.NumericReader;
import base.operators.belt.reader.Readers;

import java.time.LocalTime;
import java.util.function.DoubleFunction;


/**
 * Maps a {@link Column.Capability#NUMERIC_READABLE} {@link Column} to a {@link TimeBuffer} using a given mapping
 * operator.
 *
 * @author Gisa Meier
 */
final class ApplierNumericToTime implements Calculator<TimeBuffer> {


	private TimeBuffer target;
	private final Column source;
	private final DoubleFunction<LocalTime> operator;

	ApplierNumericToTime(Column source, DoubleFunction<LocalTime> operator) {
		this.source = source;
		this.operator = operator;
	}


	@Override
	public void init(int numberOfBatches) {
		target = Buffers.timeBuffer(source.size(), false);
	}

	@Override
	public int getNumberOfOperations() {
		return source.size();
	}

	@Override
	public void doPart(int from, int to, int batchIndex) {
		mapPart(source, operator, target, from, to);
	}

	@Override
	public TimeBuffer getResult() {
		return target;
	}

	/**
	 * Maps every index between from (inclusive) and to (exclusive) of the source column using the operator and stores
	 * the result in target.
	 */
	private static void mapPart(Column source, DoubleFunction<LocalTime> operator, TimeBuffer target, int from,
                                int to) {
		final NumericReader reader = Readers.numericReader(source, to);
		reader.setPosition(from - 1);
		for (int i = from; i < to; i++) {
			double value = reader.read();
			target.set(i, operator.apply(value));
		}
	}


}