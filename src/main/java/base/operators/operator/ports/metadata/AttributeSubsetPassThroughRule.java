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
package base.operators.operator.ports.metadata;

import base.operators.example.set.ConditionCreationException;
import base.operators.operator.Operator;
import base.operators.operator.ProcessSetupError;
import base.operators.operator.SimpleProcessSetupError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.parameter.UndefinedParameterError;
import base.operators.operator.tools.AttributeSubsetSelector;


/**
 * 
 * @author Sebastian Land
 * 
 */
public class AttributeSubsetPassThroughRule extends ExampleSetPassThroughRule {

	protected final Operator operator;
	protected final boolean keepSpecialIfNotIncluded;
	protected final AttributeSubsetSelector selector;

	public AttributeSubsetPassThroughRule(InputPort inputPort, OutputPort outputPort, Operator operator,
                                          boolean keepSpecialIfNotIncluded) {
		super(inputPort, outputPort, SetRelation.EQUAL);
		this.operator = operator;
		this.keepSpecialIfNotIncluded = keepSpecialIfNotIncluded;
		this.selector = new AttributeSubsetSelector(operator, inputPort);
	}

	@Override
	public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
		// checking if condition creation works
		try {
			AttributeSubsetSelector.createCondition(
					operator.getParameterAsString(AttributeSubsetSelector.PARAMETER_FILTER_TYPE), operator);
		} catch (UndefinedParameterError e) {
			// a standard error is already thrown
		} catch (ConditionCreationException e) {
			try {
				operator.addError(new SimpleProcessSetupError(ProcessSetupError.Severity.ERROR, operator.getPortOwner(),
						"attribute_filter_condition_error", operator
								.getParameterAsString(AttributeSubsetSelector.PARAMETER_FILTER_TYPE)));
			} catch (UndefinedParameterError e1) {
				// a standard error is already thrown
			}
		}

		ExampleSetMetaData result = selector.getMetaDataSubset(metaData, keepSpecialIfNotIncluded);

		return result;
	}

}
