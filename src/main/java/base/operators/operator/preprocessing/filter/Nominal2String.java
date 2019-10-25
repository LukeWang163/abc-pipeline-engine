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
import base.operators.tools.Ontology;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.example.table.AttributeFactory;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;


/**
 * Converts all nominal attributes to string attributes. Each nominal value is simply used as string
 * value of the new attribute. If the value is missing, the new value will be missing.
 * 
 * @author Ingo Mierswa
 */
public class Nominal2String extends AbstractFilteredDataProcessing {

	public Nominal2String(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNominal()) {
				Attribute newAttribute = AttributeFactory.changeValueType(attribute, Ontology.STRING);
				exampleSet.getAttributes().replace(attribute, newAttribute);
			}
		}
		return exampleSet;
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) {
		for (AttributeMetaData amd : emd.getAllAttributes()) {
			if (amd.isNominal()) {
				amd.setType(Ontology.STRING);
			}
		}
		return emd;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NOMINAL };
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler
				.getResourceConsumptionEstimator(getInputPort(), Nominal2String.class, null);
	}
}
