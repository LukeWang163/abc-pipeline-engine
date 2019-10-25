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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import base.operators.example.table.AttributeFactory;
import base.operators.example.table.NominalMapping;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.tools.Ontology;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.tools.math.container.Range;
import org.apache.commons.lang.ArrayUtils;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.Statistics;
import base.operators.example.Tools;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.ProcessSetupError;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.SimpleMetaDataError;
import base.operators.operator.ports.quickfix.AttributeToNominalQuickFixProvider;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.UndefinedParameterError;


/**
 * This operator distributes example weights so that all example weights of labels sum up equally.
 *
 * @author Sebastian Land
 */
public class EqualLabelWeighting extends AbstractExampleWeighting {

	private static final String PARAMETER_TOTAL_WEIGHT = "total_weight";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public EqualLabelWeighting(OperatorDescription description) {
		super(description);
		// add set role quick fix
	 	getExampleSetInputPort().addPrecondition(new ExampleSetPrecondition(getExampleSetInputPort(), Ontology.ATTRIBUTE_VALUE, Attributes.LABEL_NAME));
	}

	@Override
	protected Range getWeightAttributeRange() {
		try {
			return new Range(0, getParameterAsDouble(PARAMETER_TOTAL_WEIGHT));
		} catch (UndefinedParameterError e) {
			return new Range(0, Double.POSITIVE_INFINITY);
		}
	}

	@Override
	protected SetRelation getWeightAttributeValueRelation() {
		return SetRelation.SUPERSET;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		if (exampleSet.getAttributes().getWeight() == null) {
			Tools.hasNominalLabels(exampleSet, getOperatorClassName());
			Attribute weight = AttributeFactory.createAttribute(Attributes.WEIGHT_NAME, Ontology.NUMERICAL);
			exampleSet.getExampleTable().addAttribute(weight);
			exampleSet.getAttributes().addRegular(weight);
			exampleSet.getAttributes().setWeight(weight);

			Attribute label = exampleSet.getAttributes().getLabel();
			exampleSet.recalculateAttributeStatistics(label);
			NominalMapping labelMapping = label.getMapping();
			Map<String, Double> labelFrequencies = new HashMap<String, Double>();
			for (String labelName : labelMapping.getValues()) {
				labelFrequencies.put(labelName, exampleSet.getStatistics(label, Statistics.COUNT, labelName));
			}
			double numberOfLabels = labelFrequencies.size();
			double perLabelWeight = getParameterAsDouble(PARAMETER_TOTAL_WEIGHT) / numberOfLabels;
			for (Example example : exampleSet) {
				double exampleWeight = perLabelWeight
						/ labelFrequencies.get(labelMapping.mapIndex((int) example.getValue(label)));
				example.setValue(weight, exampleWeight);
			}
		}
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_TOTAL_WEIGHT,
				"The total weight distributed over all examples.", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1);
		type.setExpert(false);
		types.add(type);
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			return true;
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected();
		}
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), EqualLabelWeighting.class,
				null);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		// Add label to nominal quick fix
		AttributeMetaData label = metaData.getLabelMetaData();
		if (label != null && !label.isNominal()) {
			getExampleSetInputPort().addError(
					new SimpleMetaDataError(ProcessSetupError.Severity.WARNING, getExampleSetInputPort(), AttributeToNominalQuickFixProvider.labelToNominal(getExampleSetInputPort(), label), "special_attribute_has_wrong_type", label.getName(), Attributes.LABEL_NAME, Ontology.VALUE_TYPE_NAMES[Ontology.NOMINAL]));
		}
		return super.modifyMetaData(metaData);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}
}
