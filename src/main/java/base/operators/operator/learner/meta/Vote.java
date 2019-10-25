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

import base.operators.example.ExampleSet;
import base.operators.example.Statistics;
import base.operators.operator.ExecutionUnit;
import base.operators.operator.Model;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.learner.lazy.AttributeBasedVotingModel;
import base.operators.operator.ports.InputPortExtender;
import base.operators.operator.ports.OutputPortExtender;


/**
 * This class uses n+1 inner learners and generates n different models by using the last n learners.
 * The predictions of these n models are taken to create n new features for the example set, which
 * is finally used to serve as an input of the first inner learner.
 * 
 * @author Ingo Mierswa, Helge Homburg
 */
public class Vote extends AbstractStacking {

	public Vote(OperatorDescription description) {
		super(description, "Base Learner");
	}

	@Override
	public String getModelName() {
		return "Vote Model";
	}

	public OutputPortExtender getBaseInputExtender() {
		return super.baseInputExtender;
	}

	public InputPortExtender getBaseModelExtender() {
		return super.baseModelExtender;
	}


	@Override
	public boolean keepOldAttributes() {
		return false;
	}

	@Override
	protected ExecutionUnit getBaseModelLearnerProcess() {
		return getSubprocess(0);
	}

	@Override
	protected Model getStackingModel(ExampleSet stackingLearningSet) throws OperatorException {
		stackingLearningSet.recalculateAttributeStatistics(stackingLearningSet.getAttributes().getLabel());
		double majorityPrediction;
		if (stackingLearningSet.getAttributes().getLabel().isNominal()) {
			majorityPrediction = stackingLearningSet.getStatistics(stackingLearningSet.getAttributes().getLabel(),
					Statistics.MODE);
		} else {
			majorityPrediction = stackingLearningSet.getStatistics(stackingLearningSet.getAttributes().getLabel(),
					Statistics.AVERAGE);
		}
		return new AttributeBasedVotingModel(stackingLearningSet, majorityPrediction);
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc == OperatorCapability.POLYNOMINAL_ATTRIBUTES) {
			return true;
		}
		if (lc == OperatorCapability.BINOMINAL_ATTRIBUTES) {
			return true;
		}
		if (lc == OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}

		if (lc == OperatorCapability.POLYNOMINAL_LABEL) {
			return true;
		}
		if (lc == OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (lc == OperatorCapability.NUMERICAL_LABEL) {
			return true;
		}

		return false;
	}
}
