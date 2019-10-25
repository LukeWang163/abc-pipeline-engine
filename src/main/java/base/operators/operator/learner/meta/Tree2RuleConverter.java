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
import base.operators.operator.ports.metadata.PredictionModelMetaData;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.operator.Model;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.learner.rules.Rule;
import base.operators.operator.learner.rules.RuleModel;
import base.operators.operator.learner.tree.Edge;
import base.operators.operator.learner.tree.SplitCondition;
import base.operators.operator.learner.tree.Tree;
import base.operators.operator.learner.tree.TreeModel;

import java.util.Iterator;


/**
 * This meta learner uses an inner tree learner and creates a rule model from the learned decision
 * tree.
 * 
 * @author Ingo Mierswa
 */
public class Tree2RuleConverter extends AbstractMetaLearner {

	public Tree2RuleConverter(OperatorDescription description) {
		super(description);

		innerModelSink.addPrecondition(new SimplePrecondition(innerModelSink, new PredictionModelMetaData(TreeModel.class)));
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		Model innerModel = applyInnerLearner(exampleSet);

		TreeModel treeModel = null;
		if (innerModel instanceof TreeModel) {
			treeModel = (TreeModel) innerModel;
		} else {
			throw new UserError(this, 127, "the inner learner must produce a tree model.");
		}

		Tree tree = treeModel.getRoot();
		RuleModel ruleModel = new RuleModel(exampleSet);

		addRules(ruleModel, new Rule(), tree);

		return ruleModel;
	}

	private void addRules(RuleModel ruleModel, Rule currentRule, Tree tree) {
		if (tree.isLeaf()) {
			currentRule.setLabel(tree.getLabel());
			int[] frequencies = new int[ruleModel.getLabel().getMapping().size()];
			int index = 0;
			for (String labelValue : ruleModel.getLabel().getMapping().getValues()) {
				frequencies[index++] = tree.getCount(labelValue);
			}
			currentRule.setFrequencies(frequencies);
			ruleModel.addRule(currentRule);
		} else {
			Iterator<Edge> e = tree.childIterator();
			while (e.hasNext()) {
				Edge edge = e.next();
				SplitCondition condition = edge.getCondition();
				Tree child = edge.getChild();
				Rule clonedRule = (Rule) currentRule.clone();
				clonedRule.addTerm(condition);
				addRules(ruleModel, clonedRule, child);
			}
		}
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_ATTRIBUTES:
			case POLYNOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
			case POLYNOMINAL_LABEL:
			case BINOMINAL_LABEL:
			case WEIGHTED_EXAMPLES:
			case MISSING_VALUES:
				return true;
			default:
				return false;
		}
	}
}
