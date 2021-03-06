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


import base.operators.belt.column.Column;
import base.operators.belt.reader.ObjectRow;
import base.operators.belt.reader.ObjectRowReader;
import base.operators.belt.reader.Readers;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;


/**
 * Filters by several {@link Column.Capability#OBJECT_READABLE} {@link Column}s using a given filter operator.
 *
 * @author Gisa Meier
 */
final class ObjectColumnsFilterer<T> implements Calculator<int[]> {

	private final List<Column> sources;
	private final Class<T> sourceType;
	private final Predicate<ObjectRow<T>> operator;
	private boolean[] target;
	private AtomicInteger found = new AtomicInteger();

	ObjectColumnsFilterer(List<Column> sources, Class<T> sourceType, Predicate<ObjectRow<T>> operator) {
		this.sources = sources;
		this.sourceType = sourceType;
		this.operator = operator;
	}


	@Override
	public void init(int numberOfBatches) {
		target = new boolean[sources.get(0).size()];
	}

	@Override
	public int getNumberOfOperations() {
		return sources.get(0).size();
	}

	@Override
	public void doPart(int from, int to, int batchIndex) {
		int filtered = filterPart(sources, sourceType, target, operator, from, to);
		found.addAndGet(filtered);
	}

	@Override
	public int[] getResult() {
		return NumericColumnFilterer.getMapping(found.get(), target);
	}

	/**
	 * Writes the result of the filter into the target array for every index between from (inclusive) and to (exclusive)
	 * of the source column using the operator.
	 */
	private static <T> int filterPart(List<Column> sources, Class<T> type, boolean[] target,
                                      Predicate<ObjectRow<T>> operator, int from, int to) {
		int found = 0;
		final ObjectRowReader<T> reader = Readers.objectRowReader(sources, type);
		reader.setPosition(from - 1);
		for (int i = from; i < to; i++) {
			reader.move();
			boolean testResult = operator.test(reader);
			target[i] = testResult;
			if (testResult) {
				found++;
			}
		}
		return found;
	}


}