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
package base.operators.operator.learner.meta;

import java.util.List;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.operator.error.AttributeNotFoundError;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.tools.Ontology;
import base.operators.operator.Model;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;


/**
 * This meta regression learner transforms the label on-the-fly relative to the value of the
 * specified attribute. This is done right before the inner regression learner is applied. This can
 * be useful in order to allow time series predictions on data sets with large trends.
 *
 * @author Ingo Mierswa
 */
public class RelativeRegression extends AbstractMetaLearner {

	public static final String PARAMETER_RELATIVE_ATTRIBUTE = "relative_attribute";

	public RelativeRegression(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		Attribute relativeAttribute = exampleSet.getAttributes().get(getParameter(PARAMETER_RELATIVE_ATTRIBUTE));

		// checks 2
		if (relativeAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_RELATIVE_ATTRIBUTE, getParameter(PARAMETER_RELATIVE_ATTRIBUTE));
		}

		if (!relativeAttribute.isNumerical()) {
			throw new UserError(this, 120, new Object[] { relativeAttribute.getName(),
					Ontology.VALUE_TYPE_NAMES[relativeAttribute.getValueType()],
					Ontology.VALUE_TYPE_NAMES[Ontology.NUMERICAL] });
		}

		String relativeAttributeName = relativeAttribute.getName();

		// create transformed label
		Attribute originalLabel = exampleSet.getAttributes().getLabel();
		Attribute transformedLabel = AttributeFactory.createAttribute(originalLabel, "Relative");
		exampleSet.getExampleTable().addAttribute(transformedLabel);
		exampleSet.getAttributes().addRegular(transformedLabel);

		for (Example e : exampleSet) {
			double originalLabelValue = e.getValue(originalLabel);
			double relativeValue = e.getValue(relativeAttribute);
			e.setValue(transformedLabel, originalLabelValue - relativeValue);
		}

		exampleSet.getAttributes().remove(originalLabel);
		exampleSet.getAttributes().setLabel(transformedLabel);

		// base model learning
		Model baseModel = applyInnerLearner(exampleSet);

		// clean up
		exampleSet.getAttributes().remove(transformedLabel);
		exampleSet.getExampleTable().removeAttribute(transformedLabel);
		exampleSet.getAttributes().setLabel(originalLabel);

		return new RelativeRegressionModel(exampleSet, baseModel, relativeAttributeName);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeAttribute(PARAMETER_RELATIVE_ATTRIBUTE,
				"Select the attribute which should be used as a base for the relative comparison.", exampleSetInput,
				Ontology.NUMERICAL) {

			private static final long serialVersionUID = 384379555037475293L;

			@Override
			protected boolean isFilteredOut(AttributeMetaData amd) {
				String role = amd.getRole();
				if (role != null) {
					return amd.getRole().equals(Attributes.LABEL_NAME);
				}
				return false;
			}
		};
		type.setExpert(false);
		types.add(type);
		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case POLYNOMINAL_LABEL:
			case BINOMINAL_LABEL:
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
				return false;
			default:
				return true;
		}
	}
}
