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

import base.operators.operator.*;
import base.operators.example.Attribute;
import base.operators.example.AttributeWeights;
import base.operators.operator.learner.tree.*;
import base.operators.operator.ports.metadata.*;
import base.operators.operator.*;
import base.operators.operator.learner.meta.MetaModel;
import base.operators.operator.learner.tree.*;
import base.operators.operator.learner.tree.criterions.AbstractCriterion;
import base.operators.operator.learner.tree.criterions.Criterion;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.*;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeStringCategory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static base.operators.operator.features.weighting.AbstractWeighting.PARAMETER_NORMALIZE_WEIGHTS;
import static base.operators.operator.learner.tree.AbstractTreeLearner.CRITERIA_NAMES;
import static base.operators.operator.learner.tree.AbstractTreeLearner.CRITERION_GAIN_RATIO;


/**
 * This weighting schema will use a given random forest to extract the implicit importance of the
 * used attributes. Therefore each node is visited and the benefit created by the respective split
 * is aggregated for the attribute the split was performed on. The mean benefit over all nodes in
 * all trees is used as importance.
 *
 * @author Sebastian Land
 */
@SuppressWarnings("deprecation")
public class ForestBasedWeighting extends Operator {

	/**
	 * The parameter name for &quot;Specifies the used criterion for selecting attributes and
	 * numerical splits.&quot;
	 */
	public static final String PARAMETER_CRITERION = "criterion";

	private InputPort forestInput = getInputPorts().createPort("random forest");
	private OutputPort weightsOutput = getOutputPorts().createPort("weights");
	private OutputPort forestOutput = getOutputPorts().createPort("random forest");

	/**
	 * {@link ModelMetaData} that accepts both {@link RandomForestModel}s and
	 * {@link ConfigurableRandomForestModel}s.
	 *
	 * @author Michael Knopf
	 * @since 7.0.0
	 */
	public static class RandomForestModelMetaData extends ModelMetaData {

		private static final long serialVersionUID = 1L;

		public RandomForestModelMetaData() {
			super(ConfigurableRandomForestModel.class, new ExampleSetMetaData());
		}

		@Override
		public boolean isCompatible(MetaData isData, CompatibilityLevel level) {
			if (RandomForestModel.class.isAssignableFrom(isData.getObjectClass())) {
				return true;
			}

			return super.isCompatible(isData, level);
		}
	}

	public ForestBasedWeighting(OperatorDescription description) {
		super(description);
		forestInput.addPrecondition(new SimplePrecondition(forestInput, new RandomForestModelMetaData(), true));
		getTransformer().addPassThroughRule(forestInput, forestOutput);
		getTransformer().addGenerationRule(weightsOutput, AttributeWeights.class);
	}

	@Override
	public void doWork() throws OperatorException {
		// The old and new random forest model implementations are not related (class hierarchy).
		// Thus, Port#getData() would fail for one or the other. For this reason, the implementation
		// below request the common super-type Model and performs the compatibility check manually.
		Model forest = forestInput.getData(Model.class);
		//不确定
//		if (!(forest instanceof MetaModel)
//				|| !(forest instanceof ConfigurableRandomForestModel || forest instanceof RandomForestModel)) {
//			PortUserError error = new PortUserError(forestInput, 156, RendererService.getName(forest.getClass()),
//					forestInput.getName(), RendererService.getName(ConfigurableRandomForestModel.class));
//			error.setExpectedType(ConfigurableRandomForestModel.class);
//			error.setActualType(forest.getClass());
//			throw error;
//		}

		Attribute label = forest.getTrainingHeader().getAttributes().getLabel();
		if (!label.isNominal()) {
			throw new UserError(this, "weight_extraction.regression_forest");
		}
		String[] labelValues = label.getMapping().getValues().toArray(new String[0]);

		// now start measuring weights
		Criterion criterion = AbstractCriterion.createCriterion(this, 0);
		HashMap<String, Double> attributeBenefitMap = new HashMap<>();
		for (Model model : ((MetaModel) forest).getModels()) {
			TreeModel treeModel = (TreeModel) model;
			extractWeights(attributeBenefitMap, criterion, treeModel.getRoot(), labelValues);
		}

		AttributeWeights weights = new AttributeWeights();
		int numberOfModels = ((MetaModel) forest).getModels().size();
		for (Entry<String, Double> entry : attributeBenefitMap.entrySet()) {
			weights.setWeight(entry.getKey(), entry.getValue() / numberOfModels);
		}

		if (getParameterAsBoolean(PARAMETER_NORMALIZE_WEIGHTS)) {
			weights.normalize();
		}

		weightsOutput.deliver(weights);
		forestOutput.deliver(forest);
	}

	private void extractWeights(HashMap<String, Double> attributeBenefitMap, Criterion criterion, Tree root,
			String[] labelValues) {
		if (!root.isLeaf()) {
			int numberOfChildren = root.getNumberOfChildren();
			double[][] weights = new double[numberOfChildren][];

			String attributeName = null;
			Iterator<Edge> childIterator = root.childIterator();
			int i = 0;
			while (childIterator.hasNext()) {
				Edge edge = childIterator.next();
				// retrieve attributeName: On each edge the same
				attributeName = edge.getCondition().getAttributeName();

				// retrieve weights after split: Weight in child
				Map<String, Integer> subtreeCounterMap = edge.getChild().getSubtreeCounterMap();
				weights[i] = new double[labelValues.length];
				for (int j = 0; j < labelValues.length; j++) {
					Integer weight = subtreeCounterMap.get(labelValues[j]);
					double weightValue = 0;
					if (weight != null) {
						weightValue = weight;
					}
					weights[i][j] = weightValue;
				}
				i++;
			}

			// calculate benefit and add to map
			double benefit = criterion.getBenefit(weights);
			Double knownBenefit = attributeBenefitMap.get(attributeName);
			if (knownBenefit != null) {
				benefit += knownBenefit;
			}
			attributeBenefitMap.put(attributeName, benefit);

			// recursively descent to children
			childIterator = root.childIterator();
			while (childIterator.hasNext()) {
				Tree child = childIterator.next().getChild();
				extractWeights(attributeBenefitMap, criterion, child, labelValues);
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeStringCategory type = new ParameterTypeStringCategory(PARAMETER_CRITERION,
				"Specifies the used criterion for weighting attributes.", CRITERIA_NAMES,
				CRITERIA_NAMES[CRITERION_GAIN_RATIO], false);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_NORMALIZE_WEIGHTS, "Activates the normalization of all weights.", false,
				false));
		return types;
	}
}
