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
package base.operators.operator.preprocessing.filter;

import base.operators.example.ExampleSet;
import base.operators.example.set.ReplaceMissingExampleSet;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.operator.preprocessing.AbstractDataProcessing;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MetaData;


/**
 * This operator simply creates a new view on the input data without changing the actual data or
 * creating a new data table. The new view will not contain any missing values for regular
 * attributes but the mean value (or mode) of the non-missing values instead.
 *
 * @author Ingo Mierswa
 */
public class MissingValueReplenishmentView extends AbstractDataProcessing {

	public MissingValueReplenishmentView(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		for (AttributeMetaData amd : metaData.getAllAttributes()) {
			if (!amd.isSpecial()) {
				amd.setNumberOfMissingValues(new MDInteger(0));
			}
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		ExampleSet result = ReplaceMissingExampleSet.create(exampleSet, null);
		return result;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				MissingValueReplenishmentView.class, null);
	}
}
