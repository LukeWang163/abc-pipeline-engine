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

import java.util.LinkedList;
import java.util.List;

import base.operators.example.Attribute;
import base.operators.tools.Ontology;
import base.operators.tools.ProcessTools;
import base.operators.tools.math.container.Range;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDReal;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.tools.AttributeSubsetSelector;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.UndefinedParameterError;


/**
 * Creates a gaussian function based on a given attribute and a specified mean and standard
 * deviation sigma.
 * 
 * @author Ingo Mierswa
 */
public class GaussFeatureConstructionOperator extends AbstractFeatureConstruction {

	public static final String PARAMETER_MEAN = "mean";

	public static final String PARAMETER_SIGMA = "sigma";

	public GaussFeatureConstructionOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		AttributeSubsetSelector selector = getSubsetSelector();
		double mean = getParameterAsDouble(PARAMETER_MEAN);
		double sigma = getParameterAsDouble(PARAMETER_SIGMA);

		for (AttributeMetaData amd : selector.getMetaDataSubset(metaData, false).getAllAttributes()) {
			AttributeMetaData newAttribute = amd.clone();
			newAttribute.setName("gauss(" + amd.getName() + ", " + mean + ", " + sigma + ")");
			newAttribute.setMean(new MDReal());
			newAttribute.setValueRange(new Range(0, 1), SetRelation.SUBSET);
			metaData.addAttribute(newAttribute);
		}

		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		AttributeSubsetSelector selector = getSubsetSelector();
		double mean = getParameterAsDouble(PARAMETER_MEAN);
		double sigma = getParameterAsDouble(PARAMETER_SIGMA);
		List<Attribute> newAttributes = new LinkedList<Attribute>();

		for (Attribute attribute : selector.getAttributeSubset(exampleSet, false)) {
			newAttributes.add(createAttribute(exampleSet, attribute, mean, sigma));
		}

		// adding constructed attributes
		for (Attribute attribute : newAttributes) {
			exampleSet.getAttributes().addRegular(attribute);
		}

		return exampleSet;
	}

	private Attribute createAttribute(ExampleSet exampleSet, Attribute base, double mean, double sigma) {
		Attribute newAttribute = AttributeFactory.createAttribute("gauss(" + base.getName() + ", " + mean + ", " + sigma
				+ ")", Ontology.REAL);
		exampleSet.getExampleTable().addAttribute(newAttribute);

		for (Example example : exampleSet) {
			double value = example.getValue(base);
			double gaussValue = Math.exp((-1) * ((value - mean) * (value - mean)) / (sigma * sigma));
			example.setValue(newAttribute, gaussValue);
		}

		return newAttribute;
	}

	private AttributeSubsetSelector getSubsetSelector() {
		return new AttributeSubsetSelector(this, getExampleSetInputPort(), Ontology.NUMERICAL);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(getSubsetSelector().getParameterTypes(), true));

		types.add(new ParameterTypeDouble(PARAMETER_MEAN, "The mean value for the gaussian function.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0d));
		types.add(new ParameterTypeDouble(PARAMETER_SIGMA, "The sigma value for the gaussian function.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0d));
		return types;
	}
}
