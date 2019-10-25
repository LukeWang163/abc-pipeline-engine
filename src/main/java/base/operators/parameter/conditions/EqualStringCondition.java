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
package base.operators.parameter.conditions;

import base.operators.io.process.XMLTools;
import base.operators.parameter.ParameterHandler;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.XMLException;

import org.w3c.dom.Element;


/**
 * This condition checks if a string parameter (also string category) has a certain value.
 * 
 * @author Ingo Mierswa
 */
public class EqualStringCondition extends ParameterCondition {

	private static final String ELEMENT_VALUES = "Values";
	private static final String ELEMENT_VALUE = "Value";
	private String[] types;

	public EqualStringCondition(Element element) throws XMLException {
		super(element);
		Element valuesElement = XMLTools.getChildElement(element, ELEMENT_VALUES, true);
		types = XMLTools.getChildTagsContentAsStringArray(valuesElement, ELEMENT_VALUE);
	}

	public EqualStringCondition(ParameterHandler handler, String conditionParameter, boolean becomeMandatory,
                                String... types) {
		super(handler, conditionParameter, becomeMandatory);
		this.types = types;
	}

	@Override
	public boolean isConditionFullfilled() {
		boolean equals = false;
		String isType;
		try {
			isType = parameterHandler.getParameterAsString(conditionParameter);
		} catch (UndefinedParameterError e) {
			return false;
		}
		for (String type : types) {
			equals |= type.equals(isType);
		}
		return equals;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (types.length > 1) {
			builder.append(conditionParameter.replace('_', ' ') + " \u2208 {");
			for (int i = 0; i < types.length; i++) {
				builder.append(types[i]);
				if (i + 1 < types.length) {
					builder.append(", ");
				}
			}
			builder.append("}");
		} else {
			if (types.length > 0) {
				builder.append(conditionParameter.replace('_', ' ') + " = " + types[0]);
			}
		}
		return builder.toString();
	}

	//修改以输出参数
	@Override
	public void getDefinitionAsXML(Element element) {
		Element valuesElement = XMLTools.addTag(element, ELEMENT_VALUES);
		for (String value : types) {
			Element element1 =  XMLTools.addTag(valuesElement, ELEMENT_VALUE);
			element1.setAttribute("value", value);
		}
	}

//	@Override
//	public void getDefinitionAsXML(Element element) {
//		Element valuesElement = XMLTools.addTag(element, ELEMENT_VALUES);
//		for (String value : types) {
//			XMLTools.addTag(valuesElement, ELEMENT_VALUE, value);
//		}
//	}
}
