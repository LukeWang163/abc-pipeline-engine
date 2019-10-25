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
package base.operators.operator.learner.tree;

import java.util.List;

import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.ExampleSetUtilities;
import base.operators.operator.Model;
import base.operators.operator.OperatorException;
import base.operators.operator.learner.SimplePredictionModel;
import base.operators.operator.learner.meta.MetaModel;
import base.operators.operator.learner.meta.SimpleVoteModel;
import base.operators.operator.learner.meta.ConfidenceVoteModel;


/**
 * Random forest that can be configured to either use majority voting or confidence based voting for
 * its prediction.
 *
 * @author Michael Knopf
 * @since 7.0.0
 */
public class ConfigurableRandomForestModel extends SimplePredictionModel implements MetaModel {

	public enum VotingStrategy {
		MAJORITY_VOTE("majority vote"), CONFIDENCE_VOTE("confidence vote");

		private final String value;

		private VotingStrategy(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	private static final long serialVersionUID = 1L;

	/** The wrapped voting meta model. */
	private final SimplePredictionModel model;

	public ConfigurableRandomForestModel(ExampleSet exampleSet, List<? extends TreePredictionModel> models,
                                         VotingStrategy strategy) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.EQUAL,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		switch (strategy) {
			case MAJORITY_VOTE:
				model = new SimpleVoteModel(exampleSet, models);
				break;
			default:
				model = new ConfidenceVoteModel(exampleSet, models);
		}
	}

	@Override
	public List<? extends Model> getModels() {
		return ((MetaModel) model).getModels();
	}

	@Override
	public List<String> getModelNames() {
		return ((MetaModel) model).getModelNames();
	}

	@Override
	public String getName() {
		return "Random Forest Model";
	}

	@Override
	public double predict(Example example) throws OperatorException {
		return model.predict(example);
	}

	@Override
	public String toString() {
		return model.toString();
	}

}
