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
package base.operators.operator.features.construction;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.tools.Ontology;
import base.operators.tools.ProcessTools;
import base.operators.tools.math.function.aggregation.AbstractAggregationFunction;
import base.operators.tools.math.function.aggregation.AggregationFunction;
import base.operators.tools.math.function.aggregation.ConcatenationFunction;
import base.operators.example.Example;
import base.operators.example.table.AttributeFactory;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.UserError;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.SimpleMetaDataError;
import base.operators.operator.tools.AttributeSubsetSelector;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeString;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.AboveOperatorVersionCondition;
import base.operators.parameter.conditions.EqualTypeCondition;


/**
 * Allows to generate a new attribute which consists of a function of several other attributes. As
 * functions, several aggregation attributes are available.
 *
 * @author Tobias Malbrecht
 */
public class AttributeAggregationOperator extends AbstractFeatureConstruction {

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	public static final String PARAMETER_AGGREGATION_FUNCTION = "aggregation_function";

	public static final String PARAMETER_CONCATENATION_SEPARATOR = "concatenation_separator";

	public static final String PARAMETER_IGNORE_MISSINGS = "ignore_missings";

	public static final String PARAMETER_IGNORE_MISSING_ATTRIBUTES = "ignore_missing_attributes";

	/** The parameter name for &quot;Indicates if the all old attributes should be kept.&quot; */
	public static final String PARAMETER_KEEP_ALL = "keep_all";

	private static final OperatorVersion VERSION_BEFORE_HANDLING_EMPTY_ATTRIBUTE_SETS = new OperatorVersion(6, 5, 2);

	public AttributeAggregationOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		try {
			AttributeMetaData newAMD = new AttributeMetaData(getParameterAsString(PARAMETER_ATTRIBUTE_NAME), Ontology.REAL);
			AttributeSubsetSelector selector = new AttributeSubsetSelector(this, getExampleSetInputPort());
			int functionIndex = getParameterAsInt(PARAMETER_AGGREGATION_FUNCTION);
			String functionName;
			if (functionIndex < AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES.length) {
				functionName = AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[functionIndex];
			} else {
				functionName = getParameterAsString(PARAMETER_AGGREGATION_FUNCTION);
			}
			boolean ignoreMissings = getParameterAsBoolean(PARAMETER_IGNORE_MISSINGS);
			AggregationFunction aggregationFunction;
			try {
				if (ConcatenationFunction.CONCATENATION_NAME.equals(functionName)) {
					aggregationFunction = new ConcatenationFunction();
					int resultType = aggregationFunction.getValueTypeOfResult(newAMD.getValueType());
					newAMD.setType(resultType);
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(resultType, Ontology.NOMINAL)) {
						newAMD.setValueSetRelation(SetRelation.UNKNOWN);
					}
				} else {
					aggregationFunction = AbstractAggregationFunction.createAggregationFunction(functionName, ignoreMissings);
				}
				int numberOfMissings = 0;
				for (AttributeMetaData amd : selector.getMetaDataSubset(metaData, false).getAllAttributes()) {
					if (!aggregationFunction.supportsAttribute(amd)) {
						getExampleSetInputPort().addError(new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(),
								"exampleset.parameters.attribute_must_be_numerical", amd.getName(),
								PARAMETER_AGGREGATION_FUNCTION, functionName));
					}
					if (amd.getNumberOfMissingValues().isKnown()) {
						numberOfMissings = Math.max(numberOfMissings, amd.getNumberOfMissingValues().getValue());
					}
				}
				newAMD.setNumberOfMissingValues(new MDInteger(numberOfMissings));
			} catch (Exception e) {
				// ignore
			}

			metaData.addAttribute(newAMD);
		} catch (UndefinedParameterError e) {
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		AttributeSubsetSelector selector = new AttributeSubsetSelector(this, getExampleSetInputPort());
		Set<Attribute> attributes;
		boolean ignoreMissingAttributes = getCompatibilityLevel().isAtMost(VERSION_BEFORE_HANDLING_EMPTY_ATTRIBUTE_SETS)
				|| getParameterAsBoolean(PARAMETER_IGNORE_MISSING_ATTRIBUTES);
		if (ignoreMissingAttributes) {
			attributes = selector.getAttributeSubset(exampleSet, false, false);
		} else {
			attributes = selector.getAttributeSubset(exampleSet, false, true);
		}

		// cannot do anything with no attributes
		if (attributes.isEmpty()) {
			return exampleSet;
		}
		int functionIndex = getParameterAsInt(PARAMETER_AGGREGATION_FUNCTION);
		String functionName;
		if (functionIndex < AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES.length) {
			functionName = AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[functionIndex];
		} else {
			functionName = getParameterAsString(PARAMETER_AGGREGATION_FUNCTION);
		}
		boolean ignoreMissings = getParameterAsBoolean(PARAMETER_IGNORE_MISSINGS);
		AggregationFunction aggregationFunction;
		if (ConcatenationFunction.CONCATENATION_NAME.equals(functionName)) {
			aggregationFunction = new ConcatenationFunction();
			String separator = getParameterAsString(PARAMETER_CONCATENATION_SEPARATOR);
			((ConcatenationFunction) aggregationFunction).setSeparator(separator);
		} else {
			try {
				aggregationFunction = AbstractAggregationFunction.createAggregationFunction(functionName, ignoreMissings);
			} catch (Exception e) {
				throw new UserError(this, 904, functionName, e.getMessage());
			}
		}

		int valueType;
		if (attributes.isEmpty()) {
			// If there is no attribute present for the aggregation
			// function, the function may have to work on an empty set of
			// attributes. As the function still checks what attribute type
			// is being served to them, we tell it to assume it's dealing
			// with numerical values.
			valueType = Ontology.NUMERICAL;
		} else {
			valueType = Ontology.ATTRIBUTE_VALUE;
			for (Attribute attribute : attributes) {
				if (valueType == Ontology.ATTRIBUTE_VALUE) {
					if (attribute.isNominal() || attribute.isNumerical()) {
						valueType = Ontology.NUMERICAL;
					} else {
						valueType = attribute.getValueType();
					}
				}
				if (!aggregationFunction.supportsAttribute(attribute)) {
					throw new UserError(this, 136, attribute.getName());
				}
			}
		}

		valueType = aggregationFunction.getValueTypeOfResult(valueType);

		// create aggregation attribute
		Attribute newAttribute = AttributeFactory.createAttribute(getParameterAsString(PARAMETER_ATTRIBUTE_NAME), valueType);
		aggregationFunction.setTargetAttribute(newAttribute);

		exampleSet.getExampleTable().addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);

		// iterate over examples and aggregate values
		double[] values = new double[attributes.size()];
		for (Example example : exampleSet) {
			int i = 0;
			for (Attribute attribute : attributes) {
				values[i] = example.getValue(attribute);
				i++;
			}
			example.setValue(newAttribute, aggregationFunction.calculate(values));
		}

		// remove old attributes
		if (!getParameterAsBoolean(PARAMETER_KEEP_ALL)) {
			for (Attribute attribute : attributes) {
				exampleSet.getAttributes().remove(attribute);
			}
		}

		return exampleSet;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] old = super.getIncompatibleVersionChanges();
		OperatorVersion[] updatedVersions = Arrays.copyOf(old, old.length + 1);
		updatedVersions[updatedVersions.length - 1] = VERSION_BEFORE_HANDLING_EMPTY_ATTRIBUTE_SETS;
		return updatedVersions;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_ATTRIBUTE_NAME, "Name of the resulting attributes.", false));
		types.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(new AttributeSubsetSelector(this, getExampleSetInputPort()).getParameterTypes(), true));
		String[] functionNames = AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES;
		functionNames = Arrays.copyOf(functionNames, functionNames.length + 1);
		functionNames[functionNames.length - 1] = ConcatenationFunction.CONCATENATION_NAME;
		ParameterType type = new ParameterTypeCategory(PARAMETER_AGGREGATION_FUNCTION,
				"Function for aggregating the attribute values.",
				functionNames, AbstractAggregationFunction.SUM);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeString(PARAMETER_CONCATENATION_SEPARATOR, "The separator string between values. Is '|' by default.", "|", true);
		type.setOptional(true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_AGGREGATION_FUNCTION, functionNames, false, functionNames.length - 1));
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_KEEP_ALL, "Indicates if the all old attributes should be kept.", true);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_IGNORE_MISSINGS,
				"Indicates if missings should be ignored and aggregation should be based only on existing values or not. In the latter case the aggregated value will be missing in the presence of missing values.",
				true));

		type = new ParameterTypeBoolean(PARAMETER_IGNORE_MISSING_ATTRIBUTES,
				"If checked, no error will be shown when the attribute filter doesn't return any attributes.", false);
		type.setExpert(true);
		type.registerDependencyCondition(
				new AboveOperatorVersionCondition(this, VERSION_BEFORE_HANDLING_EMPTY_ATTRIBUTE_SETS));
		types.add(type);
		return types;
	}
}
