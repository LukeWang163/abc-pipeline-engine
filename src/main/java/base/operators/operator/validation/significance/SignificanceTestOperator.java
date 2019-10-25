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
package base.operators.operator.validation.significance;

import base.operators.operator.Operator;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPortExtender;
import base.operators.operator.ports.OutputPortExtender;
import base.operators.tools.math.SignificanceTestResult;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.GenerateNewMDRule;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;

import java.util.List;


/**
 * Determines if the null hypothesis (all actual mean values are the same) holds for the input
 * performance vectors.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public abstract class SignificanceTestOperator extends Operator {

	public static final String PARAMETER_ALPHA = "alpha";

	private PortPairExtender performanceExtender = new PortPairExtender("performance", getInputPorts(), getOutputPorts(),
			new MetaData(PerformanceVector.class));
	private OutputPort significanceOutput = getOutputPorts().createPort("significance");

	public SignificanceTestOperator(OperatorDescription description) {
		super(description);
		performanceExtender.start();
		getTransformer().addRule(new GenerateNewMDRule(significanceOutput, SignificanceTestResult.class));
		getTransformer().addRule(performanceExtender.makePassThroughRule());
	}

	public PortPairExtender getInputPortExtender() {
		return performanceExtender;
	}
	/**
	 * Returns the result of the significance test for the given performance vector collection.
	 */
	public abstract SignificanceTestResult performSignificanceTest(PerformanceVector[] allVectors, double alpha)
			throws OperatorException;

	/**
	 * Returns the minimum number of performance vectors which can be compared by this significance
	 * test.
	 */
	public abstract int getMinSize();

	/**
	 * Returns the maximum number of performance vectors which can be compared by this significance
	 * test.
	 */
	public abstract int getMaxSize();

	/** Writes the attribute set to a file. */
	@Override
	public void doWork() throws OperatorException {
		List<PerformanceVector> allVectors = performanceExtender.getData(PerformanceVector.class);

		if (allVectors.size() < getMinSize()) {
			throw new UserError(this, 123, PerformanceVector.class, getMinSize() + "");
		}

		if (allVectors.size() > getMaxSize()) {
			throw new UserError(this, 124, PerformanceVector.class, getMaxSize() + "");
		}

		PerformanceVector[] allVectorsArray = new PerformanceVector[allVectors.size()];
		allVectors.toArray(allVectorsArray);
		// // create result array
		// IOObject[] resultArray = new IOObject[allVectors.size() + 1];
		// System.arraycopy(allVectorsArray, 0, resultArray, 0, allVectorsArray.length);

		SignificanceTestResult result = performSignificanceTest(allVectorsArray, getParameterAsDouble(PARAMETER_ALPHA));

		performanceExtender.passDataThrough();
		significanceOutput.deliver(result);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeDouble(PARAMETER_ALPHA,
				"The probability threshold which determines if differences are considered as significant.", 0.0d, 1.0d,
				0.05d));
		return types;
	}
}
