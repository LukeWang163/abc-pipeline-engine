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
package base.operators.operator.preprocessing.normalization;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.Statistics;
import base.operators.operator.Operator;
import base.operators.tools.container.Tupel;
import base.operators.tools.math.container.Range;
import base.operators.operator.UserError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDReal;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.parameter.ParameterHandler;
import base.operators.parameter.UndefinedParameterError;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;


/**
 * The normalization method for the Z-Transformation
 * 
 * @author Sebastian Land
 */
public class ZTransformationNormalizationMethod extends AbstractNormalizationMethod {

	@Override
	public Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd,
			InputPort exampleSetInputPort, ParameterHandler parameterHandler) throws UndefinedParameterError {
		amd.setMean(new MDReal((double) 0));
		amd.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
		return Collections.singleton(amd);
	}

	@Override
	public AbstractNormalizationModel getNormalizationModel(ExampleSet exampleSet, Operator operator) throws UserError {
		// Z-Transformation
		exampleSet.recalculateAllAttributeStatistics();
		HashMap<String, Tupel<Double, Double>> attributeMeanVarianceMap = new HashMap<String, Tupel<Double, Double>>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				double average = exampleSet.getStatistics(attribute, Statistics.AVERAGE);
				double variance = exampleSet.getStatistics(attribute, Statistics.VARIANCE);
				if (!Double.isFinite(average) || !Double.isFinite(variance)) {
					nonFiniteValueWarning(operator, attribute.getName(), average, variance);
				}
				attributeMeanVarianceMap.put(attribute.getName(), new Tupel<Double, Double>(average, variance));
			}
		}
		ZTransformationModel model = new ZTransformationModel(exampleSet, attributeMeanVarianceMap);
		return model;
	}

	@Override
	public String getName() {
		return "Z-transformation";
	}

}
