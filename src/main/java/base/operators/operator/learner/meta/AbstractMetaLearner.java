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

import base.operators.example.AttributeWeights;
import base.operators.example.ExampleSet;
import base.operators.operator.learner.CapabilityCheck;
import base.operators.operator.learner.CapabilityProvider;
import base.operators.operator.learner.Learner;
import base.operators.operator.learner.PredictionModel;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.tools.ParameterService;
import base.operators.tools.Tools;
import base.operators.operator.Model;
import base.operators.operator.OperatorChain;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.LearnerPrecondition;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.PassThroughRule;
import base.operators.operator.ports.metadata.PredictionModelMetaData;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.operator.ports.metadata.SubprocessTransformRule;


/**
 * A <tt>MetaLearner</tt> is an operator that encapsulates one or more learning steps to build its
 * model. New meta learning schemes should extend this class to support the same parameters as other
 * learners. The main purpose of this class is to perform some compatibility checks.
 * 
 * @author Ingo Mierswa
 */
public abstract class AbstractMetaLearner extends OperatorChain implements Learner {

	protected final InputPort exampleSetInput = getInputPorts().createPort("training set");
	private final OutputPort modelOutput = getOutputPorts().createPort("model");
	private final OutputPort innerExampleSource = getSubprocess(0).getInnerSources().createPort("training set");
	protected final InputPort innerModelSink = getSubprocess(0).getInnerSinks().createPort("model");
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public AbstractMetaLearner(OperatorDescription description) {
		super(description, "Learning Process");
		exampleSetInput.addPrecondition(new LearnerPrecondition(this, exampleSetInput));
		innerModelSink.addPrecondition(new SimplePrecondition(innerModelSink, new PredictionModelMetaData(
				PredictionModel.class, new ExampleSetMetaData())));
		getTransformer().addRule(new PassThroughRule(exampleSetInput, innerExampleSource, true) {

			@Override
			public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
				if (unmodifiedMetaData instanceof ExampleSetMetaData) {
					return modifyExampleSetMetaData((ExampleSetMetaData) unmodifiedMetaData);
				}
				return unmodifiedMetaData;
			}
		});
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new PassThroughRule(innerModelSink, modelOutput, true) {

			@Override
			public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
				if (unmodifiedMetaData instanceof PredictionModelMetaData) {
					PredictionModelMetaData pmd = (PredictionModelMetaData) unmodifiedMetaData.clone();
					return modifyGeneratedModelMetaData(pmd);
				} else {
					return super.modifyMetaData(unmodifiedMetaData);
				}
			}
		});
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
	}

	/** Modifies the meta data of the generated model. */
	protected MetaData modifyGeneratedModelMetaData(PredictionModelMetaData unmodifiedMetaData) {
		return unmodifiedMetaData;
	}

	/**
	 * This method can be used by subclasses to additionally change the example set meta data
	 * delivered to the inner learner
	 */
	protected MetaData modifyExampleSetMetaData(ExampleSetMetaData unmodifiedMetaData) {
		return unmodifiedMetaData;
	}

	public InputPort getTrainingSetInputPort() {
		return exampleSetInput;
	}

	public OutputPort getModelOutputPort() {
		return modelOutput;
	}

	public InputPort getInnerModelSink() {
		return innerModelSink;
	}

	/**
	 * Trains a model using an ExampleSet from the input. Uses the method learn(ExampleSet).
	 */
	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		// some checks
		if (exampleSet.getAttributes().getLabel() == null) {
			throw new UserError(this, 105, new Object[0]);
		}
		if (exampleSet.getAttributes().size() == 0) {
			throw new UserError(this, 106, new Object[0]);
		}

		// check capabilities and produce errors if they are not fulfilled
		CapabilityCheck check = new CapabilityCheck(this, Tools.booleanValue(
				ParameterService.getParameterValue(CapabilityProvider.PROPERTY_RAPIDMINER_GENERAL_CAPABILITIES_WARN), true));
		check.checkLearnerCapabilities(this, exampleSet);

		Model model = learn(exampleSet);

		modelOutput.deliver(model);
		exampleSetOutput.deliver(exampleSet);
	}

	/**
	 * This is a convenience method to apply the inner operators and return the model which must be
	 * output of the last operator.
	 */
	protected Model applyInnerLearner(ExampleSet exampleSet) throws OperatorException {
		innerExampleSource.deliver(exampleSet);
		executeInnerLearner();
		return innerModelSink.getData(Model.class);
	}

	protected void executeInnerLearner() throws OperatorException {
		getSubprocess(0).execute();
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == exampleSetOutput) {
			return getParameterAsBoolean("keep_example_set");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	/**
	 * Returns true if the user wants to estimate the performance (depending on a parameter). In
	 * this case the method getEstimatedPerformance() must also be overridden and deliver the
	 * estimated performance. The default implementation returns false.
	 */
	@Override
	public boolean shouldEstimatePerformance() {
		return false;
	}

	/**
	 * Returns true if the user wants to calculate feature weights (depending on a parameter). In
	 * this case the method getWeights() must also be overriden and deliver the calculated weights.
	 * The default implementation returns false.
	 */
	@Override
	public boolean shouldCalculateWeights() {
		return false;
	}

	/** The default implementation throws an exception. */
	@Override
	public PerformanceVector getEstimatedPerformance() throws OperatorException {
		throw new UserError(this, 912, getName(), "estimation of performance not supported.");
	}

	/**
	 * Returns the calculated weight vectors. The default implementation throws an exception.
	 */
	@Override
	public AttributeWeights getWeights(ExampleSet exampleSet) throws OperatorException {
		throw new UserError(this, 916, getName(), "calculation of weights not supported.");
	}
}
