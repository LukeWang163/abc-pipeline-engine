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

import java.util.List;

import base.operators.example.AttributeRole;
import base.operators.example.ExampleSet;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.operator.preprocessing.AbstractDataProcessing;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.error.AttributeNotFoundError;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.UndefinedParameterError;


/**
 * This operator changes the attribute roles of two input attributes. This could for example be
 * useful to exchange the roles of a label with a regular attribute (and vice versa), or a label
 * with a batch attribute, a label with a cluster etc.
 *
 * @author Ingo Mierswa
 */
public class ExchangeAttributeRoles extends AbstractDataProcessing {

	public static final String PARAMETER_FIRST_ATTRIBUTE = "first_attribute";

	public static final String PARAMETER_SECOND_ATTRIBUTE = "second_attribute";

	public ExchangeAttributeRoles(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(
						this, PARAMETER_FIRST_ATTRIBUTE, PARAMETER_SECOND_ATTRIBUTE)));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		try {
			AttributeMetaData amd1 = metaData.getAttributeByName(getParameterAsString(PARAMETER_FIRST_ATTRIBUTE));
			AttributeMetaData amd2 = metaData.getAttributeByName(getParameterAsString(PARAMETER_SECOND_ATTRIBUTE));

			if (amd1 != null && amd2 != null) {
				String role1 = amd1.getRole();
				amd1.setRole(amd2.getRole());
				amd2.setRole(role1);
			}
		} catch (UndefinedParameterError e) {
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String firstName = getParameterAsString(PARAMETER_FIRST_ATTRIBUTE);
		String secondName = getParameterAsString(PARAMETER_SECOND_ATTRIBUTE);

		AttributeRole firstRole = exampleSet.getAttributes().getRole(firstName);
		AttributeRole secondRole = exampleSet.getAttributes().getRole(secondName);

		if (firstRole == null) {
			throw new AttributeNotFoundError(this, PARAMETER_FIRST_ATTRIBUTE, firstName);
		}

		if (secondRole == null) {
			throw new AttributeNotFoundError(this, PARAMETER_SECOND_ATTRIBUTE, secondName);
		}

		String firstRoleName = firstRole.getSpecialName();
		String secondRoleName = secondRole.getSpecialName();

		firstRole.changeToRegular();
		secondRole.changeToRegular();

		firstRole.setSpecial(secondRoleName);
		secondRole.setSpecial(firstRoleName);

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_FIRST_ATTRIBUTE,
				"The name of the first attribute for the attribute role exchange.", getExampleSetInputPort(), false));
		types.add(new ParameterTypeAttribute(PARAMETER_SECOND_ATTRIBUTE,
				"The name of the first attribute for the attribute role exchange.", getExampleSetInputPort(), false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				ExchangeAttributeRoles.class, null);
	}
}
