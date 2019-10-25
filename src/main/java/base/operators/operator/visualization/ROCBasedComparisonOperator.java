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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.Tools;
import base.operators.example.set.SplittedExampleSet;
import base.operators.tools.RandomGenerator;
import base.operators.tools.math.ROCBias;
import base.operators.tools.math.ROCData;
import base.operators.tools.math.ROCDataGenerator;
import base.operators.operator.Model;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorChain;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.UserError;
import base.operators.operator.learner.CapabilityProvider;
import base.operators.operator.learner.PredictionModel;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.InputPortExtender;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.OutputPortExtender;
import base.operators.operator.ports.metadata.CapabilityPrecondition;
import base.operators.operator.ports.metadata.GenerateNewMDRule;
import base.operators.operator.ports.metadata.Precondition;
import base.operators.operator.ports.metadata.PredictionModelMetaData;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.operator.ports.metadata.SubprocessTransformRule;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;


/**
 * This operator uses its inner operators (each of those must produce a model) and calculates the
 * ROC curve for each of them. All ROC curves together are plotted in the same plotter. The
 * comparison is based on the average values of a k-fold cross validation. Alternatively, this
 * operator can use an internal split into a test and a training set from the given data set.
 *
 * Please note that a former predicted label of the given example set will be removed during the
 * application of this operator.
 *
 * @author Ingo Mierswa
 */
public class ROCBasedComparisonOperator extends OperatorChain implements CapabilityProvider {

	/** The parameter name for the number of folds. */
	public static final String PARAMETER_NUMBER_OF_FOLDS = "number_of_folds";

	/** The parameter name for &quot;Relative size of the training set&quot; */
	public static final String PARAMETER_SPLIT_RATIO = "split_ratio";

	/**
	 * The parameter name for &quot;Defines the sampling type of the cross validation (linear =
	 * consecutive subsets, shuffled = random subsets, stratified = random subsets with class
	 * distribution kept constant)&quot;
	 */
	public static final String PARAMETER_SAMPLING_TYPE = "sampling_type";

	/** Indicates if example weights should be used. */
	public static final String PARAMETER_USE_EXAMPLE_WEIGHTS = "use_example_weights";

	private final InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("exampleSet");
	private final OutputPort rocComparisonOutput = getOutputPorts().createPort("rocComparison");
	private final OutputPortExtender trainingSetExtender = new OutputPortExtender("train",
			getSubprocess(0).getInnerSources());
	private final InputPortExtender modelExtender = new InputPortExtender("model", getSubprocess(0).getInnerSinks()) {

		@Override
		public Precondition makePrecondition(InputPort inputPort) {
			return new SimplePrecondition(inputPort, new PredictionModelMetaData(PredictionModel.class), false);
		}
	};

	public ROCBasedComparisonOperator(OperatorDescription description) {
		super(description, "Model Generation");
		trainingSetExtender.start();
		modelExtender.start();

		exampleSetInput.addPrecondition(new CapabilityPrecondition(this, exampleSetInput));

		getTransformer().addRule(trainingSetExtender.makePassThroughRule(exampleSetInput));
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new GenerateNewMDRule(rocComparisonOutput, ROCComparison.class));
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		Tools.hasNominalLabels(exampleSet, getOperatorClassName());
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label.getMapping().size() != 2) {
			throw new UserError(this, 114, "ROC Comparison", label.getName());
		}

		Map<String, List<ROCData>> rocData = new HashMap<String, List<ROCData>>();

		int numberOfFolds = getParameterAsInt(PARAMETER_NUMBER_OF_FOLDS);
		boolean useExampleWeights = getParameterAsBoolean(PARAMETER_USE_EXAMPLE_WEIGHTS);
		if (numberOfFolds < 0) {
			double splitRatio = getParameterAsDouble(PARAMETER_SPLIT_RATIO);
			SplittedExampleSet eSet = new SplittedExampleSet(exampleSet, splitRatio,
					getParameterAsInt(PARAMETER_SAMPLING_TYPE),
					getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
					getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED),
					getCompatibilityLevel().isAtMost(SplittedExampleSet.VERSION_SAMPLING_CHANGED));

			// apply subprocess to generate all models
			eSet.selectSingleSubset(0);
			trainingSetExtender.deliverToAll(eSet, false);
			getSubprocess(0).execute();
			List<Model> models = modelExtender.getData(Model.class, true);
			// apply models on test set
			eSet.selectSingleSubset(1);
			for (Model model : models) {
				ExampleSet resultSet = model.apply(eSet);
				if (resultSet.getAttributes().getPredictedLabel() == null) {
					throw new UserError(this, 107);
				}

				// calculate ROC values
				ROCDataGenerator rocDataGenerator = new ROCDataGenerator(1.0d, 1.0d);
				ROCData rocPoints = rocDataGenerator.createROCData(resultSet, useExampleWeights,
						ROCBias.getROCBiasParameter(this));
				List<ROCData> dataList = new LinkedList<ROCData>();
				dataList.add(rocPoints);
				rocData.put(model.getSource(), dataList);

				// remove predicted label
				PredictionModel.removePredictedLabel(resultSet);
			}
		} else {
			SplittedExampleSet eSet = new SplittedExampleSet(exampleSet, numberOfFolds,
					getParameterAsInt(PARAMETER_SAMPLING_TYPE),
					getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
					getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED),
					getCompatibilityLevel().isAtMost(SplittedExampleSet.VERSION_SAMPLING_CHANGED));

			PredictionModel.removePredictedLabel(eSet);

			for (int iteration = 0; iteration < numberOfFolds; iteration++) {
				eSet.selectAllSubsetsBut(iteration);
				trainingSetExtender.deliverToAll(eSet, false);
				getSubprocess(0).execute();
				// apply all models
				List<Model> models = modelExtender.getData(Model.class, true);
				for (Model model : models) {
					eSet.selectSingleSubset(iteration);
					ExampleSet resultSet = model.apply(eSet);
					if (resultSet.getAttributes().getPredictedLabel() == null) {
						throw new UserError(this, 107);
					}

					// calculate ROC values
					ROCDataGenerator rocDataGenerator = new ROCDataGenerator(1.0d, 1.0d);
					ROCData rocPoints = rocDataGenerator.createROCData(resultSet, useExampleWeights,
							ROCBias.getROCBiasParameter(this));
					List<ROCData> dataList = rocData.get(model.getSource());
					if (dataList == null) {
						dataList = new LinkedList<ROCData>();
						rocData.put(model.getSource(), dataList);
					}
					dataList.add(rocPoints);

					// remove predicted label
					PredictionModel.removePredictedLabel(resultSet);
				}
				inApplyLoop();
			}
		}

		exampleSetOutput.deliver(exampleSet);
		rocComparisonOutput.deliver(new ROCComparison(rocData));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_OF_FOLDS,
				"The number of folds used for a cross validation evaluation (-1: use simple split ratio).", -1,
				Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeDouble(PARAMETER_SPLIT_RATIO, "Relative size of the training set", 0.0d, 1.0d, 0.7d));
		types.add(new ParameterTypeCategory(PARAMETER_SAMPLING_TYPE,
				"Defines the sampling type of the cross validation (linear = consecutive subsets, shuffled = random subsets, stratified = random subsets with class distribution kept constant)",
				SplittedExampleSet.SAMPLING_NAMES, SplittedExampleSet.STRATIFIED_SAMPLING));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		types.add(new ParameterTypeBoolean(PARAMETER_USE_EXAMPLE_WEIGHTS,
				"Indicates if example weights should be regarded (use weight 1 for each example otherwise).", true));

		types.add(ROCBias.makeParameterType());
		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NO_LABEL:
				return false;
			case NUMERICAL_LABEL:
				try {
					return getParameterAsInt(PARAMETER_SAMPLING_TYPE) != SplittedExampleSet.STRATIFIED_SAMPLING;
				} catch (UndefinedParameterError e) {
					return false;
				}
			default:
				return true;
		}
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] { SplittedExampleSet.VERSION_SAMPLING_CHANGED };
	}
}
