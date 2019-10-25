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
package base.operators.operator.macros;

import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.DummyPortPairExtender;
import base.operators.operator.ports.PortPairExtender;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeEnumeration;
import base.operators.parameter.ParameterTypeString;

import java.util.List;


/**
 * This operator allows to unset a previously defined macro. This might be needed for branch checks
 * with the if macro is defined condition
 * 
 * @author Sebastian Land
 */
public class UnsetMacroOperator extends Operator {

	public static final String PARAMETER_MACRO = "macro";
	public static final String PARAMETER_MACRO_ENUM = "macros";

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public UnsetMacroOperator(OperatorDescription description) {
		super(description);

		dummyPorts.start();

		getTransformer().addRule(dummyPorts.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		if (isParameterSet(PARAMETER_MACRO)) {
			String macroName = getParameterAsString(PARAMETER_MACRO);
			getProcess().getMacroHandler().removeMacro(macroName);
		}
		if (isParameterSet(PARAMETER_MACRO_ENUM)) {
			String[] macroNames = ParameterTypeEnumeration
					.transformString2Enumeration(getParameterAsString(PARAMETER_MACRO_ENUM));
			for (String macroName : macroNames) {
				getProcess().getMacroHandler().removeMacro(macroName);
			}
		}

		dummyPorts.passDataThrough();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_MACRO, "A single macro which should be unset and removed.", true, false));
		types.add(new ParameterTypeEnumeration(
				PARAMETER_MACRO_ENUM,
				"A list of parameter types to unset and remove. Does the same as the macro parameter, but allows to remove macros on batch.",
				new ParameterTypeString(PARAMETER_MACRO, "The name of a macro which should be unset and removed"), false));
		return types;
	}
}
