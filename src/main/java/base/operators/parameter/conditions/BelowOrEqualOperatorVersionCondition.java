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

import base.operators.gui.tools.VersionNumber;
import base.operators.io.process.XMLTools;
import base.operators.operator.Operator;
import base.operators.operator.OperatorVersion;
import base.operators.tools.XMLException;
import org.w3c.dom.Element;


/**
 * This {@link ParameterCondition} implementation checks whether the currently selected
 * {@link OperatorVersion} is at most a predefined one by checking
 * {@link VersionNumber#isAtMost(VersionNumber)} for the provided version number.
 *
 * @author Nils Woehler
 * @since 6.5.0
 */
public class BelowOrEqualOperatorVersionCondition extends ParameterCondition {

	private static final String ELEMENT_VERSION = "BelowOrEqualOperatorVersion";
	private VersionNumber isAtMost;
	private Operator operator;

	public BelowOrEqualOperatorVersionCondition(Element element) throws XMLException {
		super(element);
		isAtMost = new VersionNumber(XMLTools.getTagContents(element, ELEMENT_VERSION, true));
	}

	public BelowOrEqualOperatorVersionCondition(Operator operator, VersionNumber isAtMost) {
		super(operator, false);
		this.operator = operator;
		this.isAtMost = isAtMost;

	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
		super.setOperator(operator);
	}

	@Override
	public boolean isConditionFullfilled() {
		if (operator == null) {
			return true;
		}
		return operator.getCompatibilityLevel().isAtMost(isAtMost);
	}

	//为生成参数修改
	@Override
	public void getDefinitionAsXML(Element element) {
		Element element1 =  XMLTools.addTag(element, ELEMENT_VERSION);
		element1.setAttribute("value" , isAtMost.getLongVersion());
	}

//	@Override
//	public void getDefinitionAsXML(Element element) {
//		XMLTools.addTag(element, ELEMENT_VERSION, isAtMost.getLongVersion());
//	}

}