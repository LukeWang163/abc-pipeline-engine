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

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.MappedExampleSet;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.operator.preprocessing.AbstractDataProcessing;
import base.operators.tools.Ontology;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.parameter.UndefinedParameterError;

import java.util.LinkedList;
import java.util.List;


/**
 * This operator performs a set intersection on two example sets, i.e., the resulting example set
 * contains all the examples of the first example set whose IDs appear also in the second example
 * set. As compared to SQL, both example sets neither need to have neither the same number of
 * columns nor the same data types. The operation does only depend on the ID columns of the example
 * sets.
 * 
 * @author Tobias Malbrecht
 */
public class ExampleSetIntersect extends AbstractDataProcessing {

	private InputPort secondInput = getInputPorts().createPort("second");

	public ExampleSetIntersect(OperatorDescription description) {
		super(description);
		secondInput.addPrecondition(new ExampleSetPrecondition(secondInput, Ontology.ATTRIBUTE_VALUE, Attributes.ID_NAME));
		((InputPort) getInputPort().getPorts().getPortByIndex(0)).addPrecondition(new ExampleSetPrecondition(
				(InputPort) getInputPort().getPorts().getPortByIndex(0), Ontology.ATTRIBUTE_VALUE, Attributes.ID_NAME));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		metaData.getNumberOfExamples().reduceByUnknownAmount();
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		ExampleSet secondSet = secondInput.getData(ExampleSet.class);
		ExampleSet firstSet = exampleSet;

		secondSet.remapIds();
		firstSet.remapIds();

		Attribute firstId = firstSet.getAttributes().getId();
		Attribute secondId = secondSet.getAttributes().getId();

		// sanity checks
		if ((firstId == null) || (secondId == null)) {
			throw new UserError(this, 129);
		}
		if (firstId.getValueType() != secondId.getValueType()) {
			throw new UserError(this, 120, new Object[] { secondId.getName(),
					Ontology.VALUE_TYPE_NAMES[secondId.getValueType()], Ontology.VALUE_TYPE_NAMES[firstId.getValueType()] });
		}

		List<Integer> indices = new LinkedList<>();
		{
			int i = 0;
			for (Example firstExample : firstSet) {
				checkForStop();
				double id = firstExample.getValue(firstId);
				Example secondExample = null;
				if (firstId.isNominal()) {
					secondExample = secondSet.getExampleFromId(secondId.getMapping().getIndex(
							firstId.getMapping().mapIndex((int) id)));
				} else {
					secondExample = secondSet.getExampleFromId(id);
				}
				if (secondExample != null) {
					indices.add(i);
				}
				i++;
			}
		}

		int[] indexArray = new int[indices.size()];
		for (int i = 0; i < indices.size(); i++) {
			checkForStop();
			indexArray[i] = indices.get(i);
		}

		return new MappedExampleSet(firstSet, indexArray);
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), ExampleSetIntersect.class,
				null);
	}

}
