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

import java.util.Iterator;
import java.util.List;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.operator.learner.PredictionModel;
import base.operators.operator.learner.lazy.DefaultLearner;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.tools.OperatorService;
import base.operators.operator.Model;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorCreationException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;


/**
 * <p>
 * This operator uses regression learner as a base learner. The learner starts with a default model
 * (mean or mode) as a first prediction model. In each iteration it learns a new base model and
 * applies it to the example set. Then, the residuals of the labels are calculated and the next base
 * model is learned. The learned meta model predicts the label by adding all base model predictions.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class AdditiveRegression extends AbstractMetaLearner {

	/** The parameter name for &quot;The number of iterations.&quot; */
	public static final String PARAMETER_ITERATIONS = "iterations";

	/**
	 * The parameter name for &quot;Reducing this learning rate prevent overfitting but increases
	 * the learning time.&quot;
	 */
	public static final String PARAMETER_SHRINKAGE = "shrinkage";

	public AdditiveRegression(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		// create temporary label attribute
		ExampleSet workingExampleSet = (ExampleSet) exampleSet.clone();
		Attribute originalLabel = workingExampleSet.getAttributes().getLabel();
		Attribute workingLabel = AttributeFactory.createAttribute(originalLabel, "working_label");
		workingExampleSet.getExampleTable().addAttribute(workingLabel);
		workingExampleSet.getAttributes().addRegular(workingLabel);
		for (Example example : workingExampleSet) {
			example.setValue(workingLabel, example.getValue(originalLabel));
		}
		workingExampleSet.getAttributes().remove(workingLabel);
		workingExampleSet.getAttributes().setLabel(workingLabel);

		// apply default model and calculate residuals
		DefaultLearner defaultLearner = null;
		try {
			defaultLearner = OperatorService.createOperator(DefaultLearner.class);
		} catch (OperatorCreationException e) {
			throw new OperatorException(getName() + ": not able to create default classifier!", e);
		}
		Model defaultModel = defaultLearner.doWork(workingExampleSet);
		residualReplace(workingExampleSet, defaultModel, false);

		// create residual models
		Model[] residualModels = new Model[getParameterAsInt(PARAMETER_ITERATIONS)];
		for (int iteration = 0; iteration < residualModels.length; iteration++) {
			residualModels[iteration] = applyInnerLearner(workingExampleSet);
			residualReplace(workingExampleSet, residualModels[iteration], true);
		}

		// clean up working label
		workingExampleSet.getAttributes().remove(workingLabel);
		workingExampleSet.getExampleTable().removeAttribute(workingLabel);

		// create and return model
		return new AdditiveRegressionModel(exampleSet, defaultModel, residualModels,
				getParameterAsDouble(PARAMETER_SHRINKAGE));
	}

	/**
	 * This methods replaces the labels of the given example set with the label residuals after
	 * using the given model. Please note that the label column will be overwritten and the original
	 * label should be stored!
	 */
	private void residualReplace(ExampleSet exampleSet, Model model, boolean shrinkage) throws OperatorException {
		ExampleSet resultSet = model.apply(exampleSet);
		Attribute label = exampleSet.getAttributes().getLabel();
		Iterator<Example> originalReader = exampleSet.iterator();
		Iterator<Example> predictionReader = resultSet.iterator();
		double shrinkageValue = 0;
		if (shrinkage) {
			shrinkageValue = getParameterAsDouble(PARAMETER_SHRINKAGE);
		}
		while (originalReader.hasNext() && predictionReader.hasNext()) {
			Example originalExample = originalReader.next();
			Example predictionExample = predictionReader.next();
			double prediction = predictionExample.getPredictedLabel();
			if (shrinkage) {
				prediction *= shrinkageValue;
			}
			double residual = originalExample.getLabel() - prediction;
			originalExample.setValue(label, residual);
		}
		PredictionModel.removePredictedLabel(resultSet);
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_LABEL:
			case POLYNOMINAL_LABEL:
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
				return false;
			default:
				return true;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_ITERATIONS, "The number of iterations.", 1, Integer.MAX_VALUE,
				10);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeDouble(PARAMETER_SHRINKAGE,
				"Reducing this learning rate prevent overfitting but increases the learning time.", 0.0d, 1.0d, 1.0d));
		return types;
	}
}
