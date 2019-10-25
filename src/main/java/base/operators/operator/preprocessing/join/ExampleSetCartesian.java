/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package base.operators.operator.preprocessing.join;

import java.util.Iterator;
import java.util.List;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.studio.internal.ProcessStoppedRuntimeException;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorProgress;
import base.operators.operator.ProcessStoppedException;
import base.operators.operator.UserError;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;


/**
 * <p>
 * Build the cartesian product of two example sets. In contrast to the {@link ExampleSetJoin}
 * operator, this operator does not depend on Id attributes. The result example set will consist of
 * the union set or the union list (depending on parameter setting double attributes will be removed
 * or renamed) of both feature sets. In case of removing double attribute the attribute values must
 * be the same for the examples of both example set, otherwise an exception will be thrown.
 * </p>
 *
 * <p>
 * Please note that this check for double attributes will only be applied for regular attributes.
 * Special attributes of the second input example set which do not exist in the first example set
 * will simply be added. If they already exist they are simply skipped.
 * </p>
 *
 * @author Peter B. Volk
 */
public class ExampleSetCartesian extends AbstractExampleSetJoin {

	public ExampleSetCartesian(OperatorDescription description) {
		super(description);
		getLeftInput().addPrecondition(new ExampleSetPrecondition(getLeftInput()));
		getRightInput().addPrecondition(new ExampleSetPrecondition(getRightInput()));
	}

	/**
	 * Joins the data WITHOUT a WHERE criteria.
	 *
	 * @param es1
	 * @param es2
	 * @param originalAttributeSources
	 * @param unionAttributeList
	 * @return the table with the joined data
	 * @throws OperatorException
	 */
	@Override
	protected ExampleSetBuilder joinData(ExampleSet es1, ExampleSet es2, List<AttributeSource> originalAttributeSources,
                                         List<Attribute> unionAttributeList) throws OperatorException {
		ExampleSetBuilder builder = ExampleSets.from(unionAttributeList);

		long total = (long) es1.size() * es2.size();
		if (total > Integer.MAX_VALUE) {
			throw new UserError(this, "cartesian_product_too_big");
		}
		int intTotal = (int) total;
		builder.withExpectedSize(intTotal);

		builder.withBlankSize(intTotal);
		Iterator<Attribute> unionIterator = unionAttributeList.iterator();
		final int unionSize = unionAttributeList.size();

		OperatorProgress progress = getProgress();
		// report progress 10 times per attribute or if that is too much once per attribute
		long steps = 10L * unionSize;
		int batchSize;
		if (steps > Integer.MAX_VALUE) {
			progress.setTotal(unionSize);
			batchSize = intTotal;
		} else {
			progress.setTotal((int) steps);
			batchSize = Math.max(1, intTotal / 10);
		}

		for (AttributeSource source : originalAttributeSources) {
			if (source.getSource() == AttributeSource.FIRST_SOURCE) {
				builder.withColumnFiller(unionIterator.next(), i -> {
					if ((i + 1) % batchSize == 0) {
						try {
							progress.step();
						} catch (ProcessStoppedException e) {
							throw new ProcessStoppedRuntimeException();
						}
					}
					// every value of the first source attribute is repeated es2.size() times
					return es1.getExample(i / es2.size()).getValue(source.getAttribute());
				});
			} else if (source.getSource() == AttributeSource.SECOND_SOURCE) {
				builder.withColumnFiller(unionIterator.next(), i -> {
					if ((i + 1) % batchSize == 0) {
						try {
							progress.step();
						} catch (ProcessStoppedException e) {
							throw new ProcessStoppedRuntimeException();
						}
					}
					// the values of the second source attributes are repeated in es2.size() large
					// blocks
					return es2.getExample(i % es2.size()).getValue(source.getAttribute());
				});
			}
		}

		return builder;
	}

	@Override
	protected boolean isIdNeeded() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0),
				ExampleSetCartesian.class, null);
	}
}
