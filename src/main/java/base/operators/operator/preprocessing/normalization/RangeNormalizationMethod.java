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
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.UndefinedParameterError;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


/**
 * This is the transformation method for transforming the data of an attribute into a certain
 * interval.
 * 
 * @author Sebastian Land
 */
public class RangeNormalizationMethod extends AbstractNormalizationMethod {

	/** The parameter name for &quot;The minimum value after normalization&quot; */
	public static final String PARAMETER_MIN = "min";

	/** The parameter name for &quot;The maximum value after normalization&quot; */
	public static final String PARAMETER_MAX = "max";

	@Override
	public Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd,
			InputPort exampleSetInputPort, ParameterHandler parameterHandler) throws UndefinedParameterError {
		double min = parameterHandler.getParameterAsDouble(PARAMETER_MIN);
		double max = parameterHandler.getParameterAsDouble(PARAMETER_MAX);
		amd.setMean(new MDReal());
		amd.setValueRange(new Range(min, max), SetRelation.EQUAL);
		return Collections.singleton(amd);
	}

	@Override
	public AbstractNormalizationModel getNormalizationModel(ExampleSet exampleSet, Operator operator) throws UserError {
		// Range Normalization
		double min = operator.getParameterAsDouble(PARAMETER_MIN);
		double max = operator.getParameterAsDouble(PARAMETER_MAX);
		if (max <= min) {
			throw new UserError(operator, 116, "max", "Must be greater than 'min'");
		}

		// calculating attribute ranges
		HashMap<String, Tupel<Double, Double>> attributeRanges = new HashMap<String, Tupel<Double, Double>>();
		exampleSet.recalculateAllAttributeStatistics();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				double minA = exampleSet.getStatistics(attribute, Statistics.MINIMUM);
				double maxA = exampleSet.getStatistics(attribute, Statistics.MAXIMUM);
				if (!Double.isFinite(minA) || !Double.isFinite(maxA)) {
					nonFiniteValueWarning(operator, attribute.getName(), minA, maxA);
				}
				attributeRanges.put(attribute.getName(), new Tupel<Double, Double>(minA, maxA));
			}
		}
		return new MinMaxNormalizationModel(exampleSet, min, max, attributeRanges);
	}

	@Override
	public String getName() {
		return "range transformation";
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler handler) {
		List<ParameterType> types = super.getParameterTypes(handler);
		types.add(new ParameterTypeDouble(PARAMETER_MIN, "The minimum value after normalization", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 0.0d));
		types.add(new ParameterTypeDouble(PARAMETER_MAX, "The maximum value after normalization", Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 1.0d));
		return types;
	}
}
