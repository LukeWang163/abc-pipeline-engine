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
package base.operators.operator.learner.functions;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.table.NominalMapping;
import base.operators.operator.Model;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.learner.AbstractLearner;
import base.operators.operator.learner.PredictionModel;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.tools.math.kernels.DotKernel;
import base.operators.tools.math.kernels.Kernel;

import java.util.List;


/**
 * The perceptron is a type of artificial neural network invented in 1957 by Frank Rosenblatt. It
 * can be seen as the simplest kind of feedforward neural network: a linear classifier. Beside all
 * biological analogies, the single layer perceptron is simply a linear classifier which is
 * efficiently trained by a simple update rule: for all wrongly classified data points, the weight
 * vector is either increased or decreased by the corresponding example values.
 * 
 * @author Sebastian Land
 */
public class Perceptron extends AbstractLearner {

	public static final String PARAMETER_ROUNDS = "rounds";

	public static final String PARAMETER_LEARNING_RATE = "learning_rate";

	public Perceptron(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		Kernel kernel = getKernel();

		double initLearnRate = getParameterAsDouble(PARAMETER_LEARNING_RATE);
		NominalMapping labelMapping = exampleSet.getAttributes().getLabel().getMapping();
		String classNeg = labelMapping.getNegativeString();
		String classPos = labelMapping.getPositiveString();
		double classValueNeg = labelMapping.getNegativeIndex();
		int numberOfAttributes = exampleSet.getAttributes().size();
		HyperplaneModel model = new HyperplaneModel(exampleSet, classNeg, classPos, kernel);
		model.init(new double[numberOfAttributes], 0);
		int rounds = getParameterAsInt(PARAMETER_ROUNDS);
		for (int round = 0; round <= rounds; round++) {
			double learnRate = getLearnRate(round, rounds, initLearnRate);
			Attributes attributes = exampleSet.getAttributes();
			for (Example example : exampleSet) {
				double prediction = model.predict(example);
				if (prediction != example.getLabel()) {
					double direction = (example.getLabel() == classValueNeg) ? -1 : 1;
					// adapting intercept
					model.setIntercept(model.getIntercept() + learnRate * direction);
					// adapting coefficients
					double coefficients[] = model.getCoefficients();
					int i = 0;
					for (Attribute attribute : attributes) {
						coefficients[i] += learnRate * direction * example.getValue(attribute);
						i++;
					}
				}
			}
		}
		return model;
	}

	protected Kernel getKernel() throws UndefinedParameterError {
		return new DotKernel();
	}

	public double getLearnRate(int time, int maxtime, double initLearnRate) {
		return initLearnRate * Math.pow(((initLearnRate * 0.1d) / initLearnRate), (((double) time) / ((double) maxtime)));
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return HyperplaneModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc == OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
		if (lc == OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (lc == OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_ROUNDS, "The number of datascans used to adapt the hyperplane.", 0,
				Integer.MAX_VALUE, 3, false));
		types.add(new ParameterTypeDouble(PARAMETER_LEARNING_RATE,
				"The hyperplane will adapt with this rate to each example.", 0.0d, 1.0d, 0.05d, false));
		return types;
	}
}
