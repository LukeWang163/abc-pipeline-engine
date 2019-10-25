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

import java.util.List;

import base.operators.example.ExampleSet;
import base.operators.operator.OperatorChain;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ValueDouble;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeString;
import base.operators.operator.ports.CollectingPortPairExtender;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.PassThroughRule;
import base.operators.operator.ports.metadata.SubprocessTransformRule;


/**
 * <p>
 * This operator takes an input data set and applies its inner operators as often as the number of
 * examples of the input data is. Inner operators can access the current example number (begins with
 * 0) by a macro, whose name can be specified via the parameter <code>iteration_macro</code>.
 * </p>
 *
 * <p>
 * As input example will be used either the {@link ExampleSet} delivered on the {@link OutputPort}
 * of this operator or if not connected the initial unmodified {@link ExampleSet}.
 * </p>
 *
 * @author Marcin Skirzynski, Tobias Malbrecht
 */
public class ExampleIterator extends OperatorChain {

	public static final String PARAMETER_ITERATION_MACRO = "iteration_macro";
	public static final String DEFAULT_ITERATION_MACRO_NAME = "example";

	private final InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private final OutputPort exampleSetInnerSource = getSubprocess(0).getInnerSources().createPort("example set");
	private final InputPort exampleSetInnerSink = getSubprocess(0).getInnerSinks().createPort("example set");
	private final CollectingPortPairExtender outExtender = new CollectingPortPairExtender("output", getSubprocess(0)
			.getInnerSinks(), getOutputPorts());

	/**
	 * Indicates the current iteration respectively example.
	 */
	private int iteration;

	public ExampleIterator(OperatorDescription description) {
		super(description, "Example Process");
		outExtender.start();

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetInnerSource);
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new PassThroughRule(exampleSetInput, exampleSetOutput, false) {

			@Override
			public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
				if (exampleSetInnerSink.isConnected()) {
					return exampleSetInnerSink.getMetaData();
				} else {
					// due to side effects, we cannot make any guarantee about the output.
					return new ExampleSetMetaData();
				}
			}
		});
		getTransformer().addRule(outExtender.makePassThroughRule());

		addValue(new ValueDouble("iteration", "The number of the current iteration / loop / example.") {

			@Override
			public double getDoubleValue() {
				return iteration;
			}
		});

	}

	/**
	 * Gets the input data and macro name and iterates over the example set while updating the
	 * current iteration in the given macro.
	 */
	@Override
	public void doWork() throws OperatorException {
		outExtender.reset();
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		String iterationMacroName = getParameterAsString(PARAMETER_ITERATION_MACRO);
		boolean innerSinkIsConnected = exampleSetInnerSink.isConnected();

		// init Operator progress
		getProgress().setTotal(exampleSet.size());

		// disable call to checkForStop as inApplyLoop will call it anyway
		getProgress().setCheckForStop(false);

		for (iteration = 1; iteration <= exampleSet.size(); iteration++) {

			getProcess().getMacroHandler().addMacro(iterationMacroName, String.valueOf(iteration));

			// passing in clone or if connected the result from last iteration
			exampleSetInnerSource.deliver(innerSinkIsConnected ? exampleSet : (ExampleSet) exampleSet.clone());
			getSubprocess(0).execute();
			inApplyLoop();
			getProgress().step();

			if (innerSinkIsConnected) {
				exampleSet = exampleSetInnerSink.getData(ExampleSet.class);
			}

			outExtender.collect();
		}

		getProcess().getMacroHandler().removeMacro(iterationMacroName);
		exampleSetOutput.deliver(exampleSet);
		getProgress().complete();
	}

	/**
	 * Provides the iteration macro name.
	 */
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_ITERATION_MACRO,
				"The name of the macro which holds the index of the current example in each iteration.",
				DEFAULT_ITERATION_MACRO_NAME, false));
		return types;
	}
}
