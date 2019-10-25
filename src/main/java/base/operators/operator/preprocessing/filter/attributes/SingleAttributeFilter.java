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
package base.operators.operator.preprocessing.filter.attributes;

import java.util.LinkedList;
import java.util.List;

import base.operators.example.Attribute;
import base.operators.example.set.ConditionCreationException;
import base.operators.operator.Operator;
import base.operators.operator.UserError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.MetaDataInfo;
import base.operators.parameter.ParameterHandler;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;


/**
 * @author Tobias Malbrecht
 */
public class SingleAttributeFilter extends AbstractAttributeFilterCondition {

	public static final String PARAMETER_ATTRIBUTE = "attribute";

	private String attributeName;

	@Override
	public void init(ParameterHandler operator) throws UserError, ConditionCreationException {
		attributeName = operator.getParameterAsString(PARAMETER_ATTRIBUTE);
		if (attributeName == null || attributeName.length() == 0) {
			throw new UserError(operator instanceof Operator ? (Operator) operator : null, 111);
		}
	}

	@Override
	public MetaDataInfo isFilteredOutMetaData(AttributeMetaData attribute, ParameterHandler handler)
			throws ConditionCreationException {
		if (attributeName == null || attributeName.length() == 0) {
			throw new ConditionCreationException(
					"The condition for a single attribute needs a non-empty attribute parameter string.");
		}
		return attribute.getName().equals(attributeName) ? MetaDataInfo.NO : MetaDataInfo.YES;
	}

	@Override
	public ScanResult beforeScanCheck(Attribute attribute) throws UserError {
		if (attribute.getName().equals(attributeName)) {
			return ScanResult.KEEP;
		} else {
			return ScanResult.REMOVE;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler operator, InputPort inPort, int... valueTypes) {
		List<ParameterType> types = new LinkedList<>();
		ParameterType type = new ParameterTypeAttribute(PARAMETER_ATTRIBUTE, "The attribute which should be chosen.",
				inPort, true, valueTypes);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
