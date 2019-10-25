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

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.operator.preprocessing.AbstractDataProcessing;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.parameter.UndefinedParameterError;


/**
 * <p>
 * This operator replaces the names of the regular attributes by the corresponding construction
 * descriptions if the attribute was constructed at all.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class Construction2Names extends AbstractDataProcessing {

	public Construction2Names(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		metaData.clear();
		metaData.attributesAreSuperset();
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		for (Attribute attribute : exampleSet.getAttributes()) {
			String construction = attribute.getConstruction();
			if (construction != null) {
				attribute.setName(construction);
			}
		}

		return exampleSet;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), Construction2Names.class,
				null);
	}
}
