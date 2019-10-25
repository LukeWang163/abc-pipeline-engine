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
package base.operators.operator.meta;

import java.util.LinkedList;
import java.util.List;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.Statistics;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeString;
import base.operators.tools.Ontology;
import base.operators.operator.IOObject;
import base.operators.operator.OperatorChain;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ValueString;
import base.operators.operator.error.AttributeNotFoundError;
import base.operators.operator.ports.CollectingPortPairExtender;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.AttributeParameterPrecondition;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.SubprocessTransformRule;


/**
 * <p>
 * In each iteration step, this meta operator executes its inner process to the input example set.
 * This will happen for each possible attribute value of the specified attributes if
 * <code>all</code> is selected for the <code>values</code> parameter. If <code>above p</code> is
 * selected, an iteration is only performed for those values which exhibit an occurrence ratio of at
 * least p. This may be helpful, if only large subgroups should be considered.
 * </p>
 *
 * <p>
 * The current value of the loop can be accessed with the specified macro name.
 * </p>
 *
 * @author Tobias Malbrecht, Ingo Mierswa
 * @deprecated since 7.4, replaced by the LoopValuesOperator in the Concurrency extension
 */
@Deprecated
public class ValueIteration extends OperatorChain {

	public static final String PARAMETER_ATTRIBUTE = "attribute";

	public static final String PARAMETER_ITERATION_MACRO = "iteration_macro";

	public static final String DEFAULT_ITERATION_MACRO_NAME = "loop_value";

	private String currentValue = null; // for logging

	private final InputPort exampleSetInput = getInputPorts().createPort("example set", new ExampleSetMetaData());
	private final OutputPort exampleInnerSource = getSubprocess(0).getInnerSources().createPort("example set");
	private final CollectingPortPairExtender outExtender = new CollectingPortPairExtender("out", getSubprocess(0)
			.getInnerSinks(), getOutputPorts());

	public ValueIteration(OperatorDescription description) {
		super(description, "Iteration");

		outExtender.start();
		exampleSetInput.addPrecondition(new AttributeParameterPrecondition(exampleSetInput, this, PARAMETER_ATTRIBUTE,
				Ontology.NOMINAL));

		getTransformer().addPassThroughRule(exampleSetInput, exampleInnerSource);
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(outExtender.makePassThroughRule());

		addValue(new ValueString("current_value", "The nominal value of the current loop.") {

			@Override
			public String getStringValue() {
				return currentValue;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		exampleSet.recalculateAllAttributeStatistics();
		outExtender.reset();

		String attributeName = getParameterAsString(PARAMETER_ATTRIBUTE);
		Attribute attribute = exampleSet.getAttributes().get(attributeName);
		if (attribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE, attributeName);
		}
		if (!attribute.isNominal()) {
			throw new UserError(this, 119, attributeName, getName());
		}

		String iterationMacro = getParameterAsString(PARAMETER_ITERATION_MACRO);

		List<String> values = new LinkedList<>(attribute.getMapping().getValues());

		// init Operator progress
		getProgress().setTotal(values.size());
		getProgress().setCheckForStop(false);

		for (String value : values) {
			if (exampleSet.getStatistics(attribute, Statistics.COUNT, value) > 0) {
				if (iterationMacro != null) {
					// getProcess().getMacroHandler().addMacro(iterationMacro, value.replace(' ',
					// '_'));
					getProcess().getMacroHandler().addMacro(iterationMacro, value);
				}

				// store for logging
				this.currentValue = value;

				exampleInnerSource.deliver((ExampleSet) exampleSet.clone());

				getSubprocess(0).execute();

				for (PortPairExtender.PortPair pair : outExtender.getManagedPairs()) {
					IOObject result = pair.getInputPort().getDataOrNull(IOObject.class);
					if (result != null) {
						result.setSource(this.getName() + ":" + value);
					}
				}
				outExtender.collect();
			}
			inApplyLoop();
			getProgress().step();
		}

		if (iterationMacro != null) {
			getProcess().getMacroHandler().addMacro(iterationMacro, null);
		}
		getProgress().complete();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeAttribute(PARAMETER_ATTRIBUTE,
				"The nominal attribute for which the iteration should be defined", exampleSetInput, false);
		types.add(type);

		types.add(new ParameterTypeString(PARAMETER_ITERATION_MACRO, "Name of macro which is set in each iteration.",
				DEFAULT_ITERATION_MACRO_NAME, false));

		return types;
	}
}
