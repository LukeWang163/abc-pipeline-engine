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
package base.operators.operator.visualization;

import java.util.List;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.Value;
import base.operators.tools.Ontology;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.error.AttributeNotFoundError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.operator.ports.metadata.ExampleSetSizePrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeInt;


/**
 * This operator can be used to log a specific value of a given example set into the provided log
 * value &quot;data_value&quot; which can then be logged by the operator {@link ProcessLogOperator}.
 *
 * @author Ingo Mierswa
 */
public class Data2Log extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";
	public static final String PARAMETER_EXAMPLE_INDEX = "example_index";

	private Object currentValue = null;

	private boolean isNominal = false;

	public Data2Log(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition
				.getAttributesByParameter(this, PARAMETER_ATTRIBUTE_NAME)));
		exampleSetInput.addPrecondition(new ExampleSetSizePrecondition(exampleSetInput, this, PARAMETER_EXAMPLE_INDEX));

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);

		addValue(new Value("data_value", "The value from the data which should be logged.") {

			@Override
			public Object getValue() {
				return currentValue;
			}

			@Override
			public boolean isNominal() {
				return isNominal;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		Attribute attribute = exampleSet.getAttributes().get(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		if (attribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME, getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		}

		int index = getParameterAsInt(PARAMETER_EXAMPLE_INDEX);
		if (index == 0) {
			throw new UserError(this, 207, "0", PARAMETER_EXAMPLE_INDEX, "only positive or negative indices are allowed");
		}

		if (index < 0) {
			index = exampleSet.size() + index;
		} else {
			index--;
		}

		if (index >= exampleSet.size()) {
			throw new UserError(this, 110, index);
		}

		Example example = exampleSet.getExample(index);
		if (attribute.isNominal() || Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
			currentValue = example.getValueAsString(attribute);
			isNominal = true;
		} else {
			currentValue = Double.valueOf(example.getValue(attribute));
			isNominal = false;
		}

		exampleSetOutput.deliver(exampleSet);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME,
				"The attribute from which the value should be taken.", exampleSetInput, false));
		types.add(new ParameterTypeInt(
				PARAMETER_EXAMPLE_INDEX,
				"The index of the example from which the value should be taken. Negative indices are counted from the end of the data set. Positive counting starts with 1, negative counting with -1.",
				-Integer.MAX_VALUE, Integer.MAX_VALUE, false));
		return types;
	}
}
