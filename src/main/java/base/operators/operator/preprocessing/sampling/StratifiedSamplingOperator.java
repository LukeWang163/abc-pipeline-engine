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
package base.operators.operator.preprocessing.sampling;

import base.operators.example.ExampleSet;
import base.operators.example.set.SplittedExampleSet;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.tools.RandomGenerator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessSetupError.Severity;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MetaDataInfo;
import base.operators.operator.ports.metadata.SimpleMetaDataError;
import base.operators.operator.ports.quickfix.ParameterSettingQuickFix;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeCategory;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.conditions.EqualTypeCondition;

import java.util.Collections;
import java.util.List;


/**
 * Stratified sampling.
 * 
 * @author Ingo Mierswa, Sebastian Land, Tobias Malbrecht
 */
public class StratifiedSamplingOperator extends AbstractSamplingOperator {

	public static final String PARAMETER_SAMPLE = "sample";

	public static final String[] SAMPLE_MODES = { "absolute", "relative" };

	public static final int SAMPLE_ABSOLUTE = 0;

	public static final int SAMPLE_RELATIVE = 1;

	/** The parameter name for &quot;The fraction of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";

	/** The parameter name for &quot;The fraction of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	public StratifiedSamplingOperator(OperatorDescription description) {
		super(description);
	}

	/**
	 * This method should return the ratio used for stratifiedSampling
	 */
	public double getRatio(ExampleSet exampleSet) throws OperatorException {
		switch (getParameterAsInt(PARAMETER_SAMPLE)) {
			case SAMPLE_ABSOLUTE:
				double targetSize = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
				if (targetSize > exampleSet.size()) {
					return 1d;
				} else {
					return targetSize / (exampleSet.size());
				}
			case SAMPLE_RELATIVE:
				return getParameterAsDouble(PARAMETER_SAMPLE_RATIO);
			default:
				return 1;
		}
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) throws UndefinedParameterError {
		switch (getParameterAsInt(PARAMETER_SAMPLE)) {
			case SAMPLE_ABSOLUTE:
				int absoluteNumber = getParameterAsInt(PARAMETER_SAMPLE_SIZE);

				if (emd.getNumberOfExamples().isAtLeast(absoluteNumber) == MetaDataInfo.NO) {
					getExampleSetInputPort().addError(
							new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), Collections
									.singletonList(new ParameterSettingQuickFix(this, PARAMETER_SAMPLE_SIZE, emd
											.getNumberOfExamples().getValue().toString())), "exampleset.need_more_examples",
									absoluteNumber + ""));
				}
				return new MDInteger(absoluteNumber);
			case SAMPLE_RELATIVE:
				MDInteger number = emd.getNumberOfExamples();
				number.multiply(getParameterAsDouble(PARAMETER_SAMPLE_RATIO));
				return number;
			default:
				return new MDInteger();
		}
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// perform stratified sampling
		SplittedExampleSet splittedExampleSet = new SplittedExampleSet(exampleSet, getRatio(exampleSet),
				SplittedExampleSet.STRATIFIED_SAMPLING,
				getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED),
				getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED));
		splittedExampleSet.selectSingleSubset(0);

		return splittedExampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_SAMPLE, "Determines how the amount of data is specified.",
				SAMPLE_MODES, SAMPLE_ABSOLUTE);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_SAMPLE_SIZE, "The number of examples which should be sampled", 1,
				Integer.MAX_VALUE, 100);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_ABSOLUTE));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO, "The fraction of examples which should be sampled", 0.0d,
				1.0d, 0.1d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_RELATIVE));
		type.setExpert(false);
		types.add(type);
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				StratifiedSamplingOperator.class, null);
	}
}
