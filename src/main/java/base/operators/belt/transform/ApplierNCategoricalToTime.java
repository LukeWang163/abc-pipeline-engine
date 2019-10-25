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
import base.operators.belt.reader.CategoricalRow;
import base.operators.belt.reader.CategoricalRowReader;
import base.operators.belt.reader.Readers;

import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;


/**
 * Maps {@link Column.Category#CATEGORICAL} {@link Column}s to a {@link TimeBuffer} using a given mapping
 * operator.
 *
 * @author Gisa Meier
 */
final class ApplierNCategoricalToTime implements Calculator<TimeBuffer> {

	private TimeBuffer target;
	private final List<Column> sources;
	private final Function<CategoricalRow, LocalTime> operator;

	ApplierNCategoricalToTime(List<Column> sources, Function<CategoricalRow, LocalTime> operator) {
		this.sources = sources;
		this.operator = operator;
	}

	@Override
	public void init(int numberOfBatches) {
		target = Buffers.timeBuffer(sources.get(0).size(), false);
	}

	@Override
	public int getNumberOfOperations() {
		return sources.get(0).size();
	}

	@Override
	public void doPart(int from, int to, int batchIndex) {
		mapPart(sources, operator, target, from, to);
	}

	@Override
	public TimeBuffer getResult() {
		return target;
	}

	/**
	 * Maps every index between from (inclusive) and to (exclusive) of the sources columns using the operator and
	 * stores the result in target.
	 */
	private static void mapPart(final List<Column> sources, final Function<CategoricalRow, LocalTime> operator,
                                final TimeBuffer target, int from, int to) {
		final CategoricalRowReader reader = Readers.categoricalRowReader(sources);
		reader.setPosition(from - 1);
		for (int i = from; i < to; i++) {
			reader.move();
			target.set(i, operator.apply(reader));
		}
	}


}