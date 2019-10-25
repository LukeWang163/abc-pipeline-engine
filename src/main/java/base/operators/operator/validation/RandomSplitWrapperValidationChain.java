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
package base.operators.operator.validation;

import java.util.List;

import base.operators.example.ExampleSet;
import base.operators.operator.OperatorCapability;
import base.operators.tools.RandomGenerator;
import base.operators.example.AttributeWeights;
import base.operators.example.set.AttributeWeightedExampleSet;
import base.operators.example.set.SplittedExampleSet;
import base.operators.operator.Model;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorVersion;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.CapabilityPrecondition;
import base.operators.operator.ports.metadata.Precondition;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.operator.ports.quickfix.QuickFix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.NonEqualTypeCondition;


/**
 * This operator evaluates the performance of feature weighting algorithms including feature
 * selection. The first inner operator is the weighting algorithm to be evaluated itself. It must
 * return an attribute weights vector which is applied on the data. Then a new model is created
 * using the second inner operator and a performance is retrieved using the third inner operator.
 * This performance vector serves as a performance indicator for the actual algorithm.
 *
 * This implementation is described for the {@link RandomSplitValidationChain}.
 *
 * @author Ingo Mierswa
 */
public class RandomSplitWrapperValidationChain extends WrapperValidationChain {

	public static final String PARAMETER_SPLIT_RATIO = "split_ratio";

	public static final String PARAMETER_SAMPLING_TYPE = "sampling_type";

	public RandomSplitWrapperValidationChain(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Precondition getCapabilityPrecondition() {
		return new CapabilityPrecondition(this, exampleSetInput) {

			@Override
			protected List<QuickFix> getFixesForRegressionWhenClassificationSupported(AttributeMetaData labelMD) {
				List<QuickFix> fixes = super.getFixesForRegressionWhenClassificationSupported(labelMD);
				fixes.add(0, new ParameterSettingQuickFix(RandomSplitWrapperValidationChain.this, PARAMETER_SAMPLING_TYPE,
						SplittedExampleSet.SHUFFLED_SAMPLING + "", "switch_to_shuffled_sampling"));
				return fixes;
			}
		};
	}

	@Override
	public void doWork() throws OperatorException {
		double splitRatio = getParameterAsDouble(PARAMETER_SPLIT_RATIO);
		ExampleSet inputSet = exampleSetInput.getData(ExampleSet.class);
		SplittedExampleSet eSet = new SplittedExampleSet(inputSet, splitRatio, getParameterAsInt(PARAMETER_SAMPLING_TYPE),
				getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
				getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED),
				getCompatibilityLevel().isAtMost(SplittedExampleSet.VERSION_SAMPLING_CHANGED));

		eSet.selectSingleSubset(0);
		AttributeWeights weights = useWeightingMethod(eSet);
		SplittedExampleSet newInputSet = new SplittedExampleSet(eSet);

		// learn on the same data
		Model model = learn(new AttributeWeightedExampleSet(newInputSet, weights, 0.0d).createCleanClone());

		// testing
		newInputSet.selectSingleSubset(1);
		PerformanceVector pv = evaluate(new AttributeWeightedExampleSet(newInputSet, weights, 0.0d).createCleanClone(),
				model);
		setResult(pv.getMainCriterion());

		performanceOutput.deliver(pv);
		attributeWeightsOutput.deliver(weights);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_SPLIT_RATIO, "Relative size of the training set", 0, 1, 0.7);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeCategory(PARAMETER_SAMPLING_TYPE,
				"Defines the sampling type of the cross validation (linear = consecutive subsets, shuffled = random subsets, stratified = random subsets with class distribution kept constant, automatic = primary stratified or secondary shuffled)",
				SplittedExampleSet.SAMPLING_NAMES, SplittedExampleSet.AUTOMATIC, false));
		List<ParameterType> randomTypes = RandomGenerator.getRandomGeneratorParameters(this);
		for (ParameterType randomType : randomTypes) {
			// Don't show random seed when linear sampling is selected
			randomType.registerDependencyCondition(new NonEqualTypeCondition(this, PARAMETER_SAMPLING_TYPE,
					SplittedExampleSet.SAMPLING_NAMES, false, SplittedExampleSet.LINEAR_SAMPLING));
		}
		types.addAll(randomTypes);
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
