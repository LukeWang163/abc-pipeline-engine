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
package base.operators.operator.visualization.dependencies;

import Jama.Matrix;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ProcessSetupError;
import base.operators.tools.math.matrix.CovarianceMatrix;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;
import base.operators.operator.ports.metadata.SimpleMetaDataError;
import base.operators.parameter.UndefinedParameterError;


/**
 * This operator calculates the covariances between all attributes of the input example set and
 * returns a covariance matrix object which can be visualized.
 *
 * @author Ingo Mierswa
 */
public class CovarianceMatrixOperator extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set");

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort covarianceOutput = getOutputPorts().createPort("covariance");

	public CovarianceMatrixOperator(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput) {

			@Override
			public void makeAdditionalChecks(ExampleSetMetaData emd) throws UndefinedParameterError {
				for (AttributeMetaData amd : emd.getAllAttributes()) {
					if (!amd.isSpecial() && !amd.isNumerical()) {
						exampleSetInput.addError(new SimpleMetaDataError(ProcessSetupError.Severity.WARNING, exampleSetInput,
								"not_defined_on_nominal", "Covariance"));
						break;
					}
				}
				super.makeAdditionalChecks(emd);
			}
		});

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addGenerationRule(covarianceOutput, NumericalMatrix.class);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		String[] columnNames = new String[exampleSet.getAttributes().size()];
		boolean[] isNominal = new boolean[columnNames.length];
		int counter = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			columnNames[counter] = attribute.getName();
			if (attribute.isNominal()) {
				isNominal[counter] = true;
			}
			counter++;
		}
		Matrix covarianceMatrix = CovarianceMatrix.getCovarianceMatrix(exampleSet, this);

		// setting all nominal colums on NaN
		double[][] matrix = covarianceMatrix.getArray();
		for (int i = 0; i < covarianceMatrix.getColumnDimension(); i++) {
			for (int j = 0; j < covarianceMatrix.getRowDimension(); j++) {
				if (isNominal[i] || isNominal[j]) {
					matrix[i][j] = Double.NaN;
				}
			}
		}

		exampleSetOutput.deliver(exampleSet);
		covarianceOutput.deliver(new NumericalMatrix("Covariance", columnNames, covarianceMatrix, true));
	}
}
