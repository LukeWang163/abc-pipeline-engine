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
package base.operators.operator.features.weighting;

import base.operators.example.Attribute;
import base.operators.operator.OperatorCapability;
import base.operators.tools.OperatorService;
import base.operators.example.AttributeWeights;
import base.operators.example.ExampleSet;
import base.operators.example.Tools;
import base.operators.example.set.AttributeSelectionExampleSet;
import base.operators.operator.Model;
import base.operators.operator.OperatorCreationException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.learner.AbstractLearner;
import base.operators.operator.learner.rules.SingleRuleLearner;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.performance.SimplePerformanceEvaluator;


/**
 * This operator calculates the relevance of a feature by computing the error rate of a OneR Model
 * on the exampleSet without this feature.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class OneRErrorWeighting extends AbstractWeighting {

	private static final int PROGRESS_UPDATE_STEPS = 500;

	public OneRErrorWeighting(OperatorDescription description) {
		super(description, true);
	}

	@Override
	protected AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {
		Tools.hasNominalLabels(exampleSet, getOperatorClassName());

		// calculate the actual chi-squared values and assign them to weights
		AttributeWeights weights = new AttributeWeights(exampleSet);
		AbstractLearner learner;
		try {
			learner = OperatorService.createOperator(SingleRuleLearner.class);
		} catch (OperatorCreationException e) {
			throw new UserError(this, 904, "inner operator", e.getMessage());
		}
		SimplePerformanceEvaluator performanceEvaluator;
		try {
			performanceEvaluator = OperatorService.createOperator(SimplePerformanceEvaluator.class);
		} catch (OperatorCreationException e) {
			throw new UserError(this, 904, "performance evaluation operator", e.getMessage());
		}

		int attributesSize = exampleSet.getAttributes().size();
		boolean[] mask = new boolean[attributesSize];
		int i = 0;
		int progressCounter = 0;
		int exampleSetSize = exampleSet.size();
		getProgress().setTotal(100);
		for (Attribute attribute : exampleSet.getAttributes()) {
			mask[i] = true;
			if (i > 0) {
				mask[i - 1] = false;
			}
			ExampleSet singleAttributeSet = AttributeSelectionExampleSet.create(exampleSet, mask);
			// calculating model
			Model model = learner.doWork(singleAttributeSet);
			progressCounter += exampleSetSize;
			if (progressCounter > PROGRESS_UPDATE_STEPS) {
				progressCounter = 0;
				getProgress().setCompleted((int) (100 * (i + 0.33F) / attributesSize));
			}

			// applying model
			singleAttributeSet = model.apply(singleAttributeSet);
			progressCounter += exampleSetSize;
			if (progressCounter > PROGRESS_UPDATE_STEPS) {
				progressCounter = 0;
				getProgress().setCompleted((int) (100 * (i + 0.67F) / attributesSize));
			}

			// applying performance evaluator
			PerformanceVector performance = performanceEvaluator.doWork(singleAttributeSet);
			double weight = performance.getCriterion(0).getAverage();

			weights.setWeight(attribute.getName(), weight);
			i++;

			progressCounter += exampleSetSize;
			if (progressCounter > PROGRESS_UPDATE_STEPS) {
				progressCounter = 0;
				getProgress().setCompleted((int) (100F * i / attributesSize));
			}
		}
		return weights;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_LABEL:
			case POLYNOMINAL_LABEL:
			case BINOMINAL_ATTRIBUTES:
			case POLYNOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
				return true;
			default:
				return false;
		}
	}
}