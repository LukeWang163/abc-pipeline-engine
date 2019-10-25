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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import base.operators.example.*;
import base.operators.operator.Model;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.operator.learner.AbstractLearner;
import base.operators.operator.learner.PredictionModel;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.example.Attribute;
import base.operators.example.AttributeRole;
import base.operators.example.Example;
import base.operators.example.ExampleSet;

import Jama.Matrix;


/**
 * This operator performs a vector linear regression. It regresses all regular attributes upon a
 * vector of labels. The attributes forming the vector have to be marked as special, the special
 * role names of all label attributes have to start with <code>label</code>.
 *
 * TODO: Adapt meta data of model, but needs change of complete construction...
 *
 * @author Tobias Malbrecht
 */
public class VectorLinearRegression extends AbstractLearner {

	public static final String PARAMETER_USE_BIAS = "use_bias";

	public static final String PARAMETER_RIDGE = "ridge";

	public VectorLinearRegression(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {

		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this);

		boolean useBias = getParameterAsBoolean(PARAMETER_USE_BIAS);
		double ridge = getParameterAsDouble(PARAMETER_RIDGE);

		List<Attribute> labels = new LinkedList<>();
		for (Iterator<AttributeRole> roleIterator = exampleSet.getAttributes().allAttributeRoles(); roleIterator
				.hasNext();) {
			AttributeRole role = roleIterator.next();
			if (role.getSpecialName() != null && role.getSpecialName().startsWith("label")) {
				labels.add(role.getAttribute());
			}
		}
		int biasOffset = useBias ? 1 : 0;
		int width = exampleSet.getAttributes().size() + 1;

		Matrix x = new Matrix(exampleSet.size(), width);
		Matrix y = new Matrix(exampleSet.size(), labels.size());
		int j = 0;
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		for (Example example : exampleSet) {
			if (useBias) {
				x.set(j, 0, 1);
			}
			int i = biasOffset;
			for (Attribute attribute : regularAttributes) {
				x.set(j, i, example.getValue(attribute));
				i++;
			}
			int k = 0;
			for (Attribute label : labels) {
				y.set(j, k, example.getValue(label));
				k++;
			}
			j++;
		}

		int numberOfColumns = x.getColumnDimension();
		Matrix xTransposed = x.transpose();
		Matrix result = null;
		boolean finished = false;
		while (!finished) {
			Matrix xTx = xTransposed.times(x);

			for (int i = 0; i < numberOfColumns; i++) {
				xTx.set(i, i, xTx.get(i, i) + ridge);
			}

			Matrix xTy = xTransposed.times(y);
			try {
				result = xTx.solve(xTy);
				finished = true;
			} catch (Exception ex) {
				ridge *= 10;
				finished = false;
			}
		}

		String[] labelNames = new String[labels.size()];
		for (int i = 0; i < labels.size(); i++) {
			labelNames[i] = labels.get(i).getName();
		}
		return new VectorRegressionModel(exampleSet, labelNames, result, useBias);
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return VectorRegressionModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc.equals(OperatorCapability.NUMERICAL_ATTRIBUTES)) {
			return true;
		}
		if (lc.equals(OperatorCapability.NUMERICAL_LABEL)) {
			return true;
		}
		if (lc == OperatorCapability.WEIGHTED_EXAMPLES) {
			return false;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_USE_BIAS, "Indicates if an intercept value should be calculated.", true));
		types.add(new ParameterTypeDouble(PARAMETER_RIDGE, "The ridge parameter.", 0, Double.POSITIVE_INFINITY, 1.0E-8));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(),
				VectorLinearRegression.class, null);
	}
}
