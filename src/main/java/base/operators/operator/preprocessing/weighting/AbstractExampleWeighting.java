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
package base.operators.operator.preprocessing.weighting;

import base.operators.example.Attributes;
import base.operators.operator.preprocessing.AbstractDataProcessing;
import base.operators.tools.Ontology;
import base.operators.tools.math.container.Range;
import base.operators.operator.OperatorDescription;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SetRelation;


/**
 * Abstract superclass of operators adding a weight attribute.
 * 
 * @author Simon Fischer
 * 
 */
public abstract class AbstractExampleWeighting extends AbstractDataProcessing {

	public AbstractExampleWeighting(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		AttributeMetaData weightAttribute = new AttributeMetaData(Attributes.WEIGHT_NAME, Ontology.REAL,
				Attributes.WEIGHT_NAME);
		weightAttribute.setValueRange(getWeightAttributeRange(), getWeightAttributeValueRelation());
		metaData.addAttribute(weightAttribute);
		return metaData;
	}

	protected Range getWeightAttributeRange() {
		return new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	protected SetRelation getWeightAttributeValueRelation() {
		return SetRelation.UNKNOWN;
	}

}
