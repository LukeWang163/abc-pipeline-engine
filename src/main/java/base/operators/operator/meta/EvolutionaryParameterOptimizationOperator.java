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
package base.operators.operator.meta;

import java.util.Iterator;
import java.util.List;

import base.operators.operator.performance.PerformanceVector;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeDouble;
import base.operators.parameter.ParameterTypeInt;
import base.operators.parameter.UndefinedParameterError;
import base.operators.parameter.value.ParameterValueRange;
import base.operators.tools.RandomGenerator;
import base.operators.tools.math.optimization.ec.es.ESOptimization;
import base.operators.tools.math.optimization.ec.es.Individual;
import base.operators.tools.math.optimization.ec.es.OptimizationValueType;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.ValueDouble;
import base.operators.parameter.value.ParameterValues;


/**
 * This operator finds the optimal values for a set of parameters using an evolutionary strategies
 * approach which is often more appropriate than a grid search or a greedy search like the quadratic
 * programming approach and leads to better results. The parameter <var>parameters</var> is a list
 * of key value pairs where the keys are of the form <code>operator_name.parameter_name</code> and
 * the value for each parameter must be a semicolon separated pair of a minimum and a maximum value
 * in squared parantheses, e.g. [10;100] for a range of 10 until 100. <br/>
 * The operator returns an optimal {@link ParameterSet} which can as well be written to a file with
 * a {@link base.operators.extension.legacy.operator.io.ParameterSetWriter}. This parameter set can be read in
 * another process using a {@link base.operators.extension.legacy.operator.io.ParameterSetLoader}. <br/>
 * The file format of the parameter set file is straightforward and can easily be generated by
 * external applications. Each line is of the form <center>
 * <code>operator_name.parameter_name = value</code></center> <br/>
 * Please refer to section {@rapidminer.ref sec:parameter_optimization|Advanced Processes/Parameter
 * and performance analysis} for an example application.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class EvolutionaryParameterOptimizationOperator extends ParameterOptimizationOperator {

	public static final String PARAMETER_MAX_GENERATIONS = ESOptimization.PARAMETER_MAX_GENERATIONS;
	public static final String PARAMETER_GENERATIONS_WITHOUT_IMPROVAL = ESOptimization.PARAMETER_GENERATIONS_WITHOUT_IMPROVAL;
	public static final String PARAMETER_POPULATION_SIZE = ESOptimization.PARAMETER_POPULATION_SIZE;
	public static final String PARAMETER_TOURNAMENT_FRACTION = ESOptimization.PARAMETER_TOURNAMENT_FRACTION;
	public static final String PARAMETER_KEEP_BEST = ESOptimization.PARAMETER_KEEP_BEST;
	public static final String PARAMETER_MUTATION_TYPE = ESOptimization.PARAMETER_MUTATION_TYPE;
	public static final String PARAMETER_SELECTION_TYPE = ESOptimization.PARAMETER_SELECTION_TYPE;
	public static final String PARAMETER_CROSSOVER_PROB = ESOptimization.PARAMETER_CROSSOVER_PROB;
	public static final String PARAMETER_SHOW_CONVERGENCE_PLOT = ESOptimization.PARAMETER_SHOW_CONVERGENCE_PLOT;
	public static final String PARAMETER_SPECIFIY_POPULATION_SIZE = ESOptimization.PARAMETER_SPECIFIY_POPULATION_SIZE;

	// private IOContainer input;

	/** The actual optimizer. */
	private ESOptimization optimizer;
	private double bestFitnessEver = Double.NaN;
	private double lastGenerationsPerformance = Double.NaN;

	/** The operators for which parameters should be optimized. */
	private Operator[] operators;

	/** The names of the parameters which should be optimized. */
	private String[] parameters;

	/** The parameter types. */
	private OptimizationValueType[] types;

	public EvolutionaryParameterOptimizationOperator(OperatorDescription description) {
		super(description);
		addValue(new ValueDouble("best", "best performance ever") {

			@Override
			public double getDoubleValue() {
				return bestFitnessEver;
			}
		});
	}

	public Operator[] getOptimizationOperators() {
		return this.operators;
	}

	public String[] getOptimizationParameters() {
		return this.parameters;
	}

	public OptimizationValueType[] getOptimizationValueTypes() {
		return this.types;
	}

	@Override
	public int getParameterValueMode() {
		return ParameterConfigurator.VALUE_MODE_CONTINUOUS;
	}

	@Override
	public double getCurrentBestPerformance() {
		// must make this check, because optimizer will be set null to tidy up after execution
		if (optimizer != null) {
			return optimizer.getBestFitnessInGeneration();
		} else {
			return lastGenerationsPerformance;
		}
	}

	@Override
	public void doWork() throws OperatorException {

		// check parameter values list
		List<ParameterValues> parameterValuesList = parseParameterValues(getParameterList("parameters"));
		if (parameterValuesList == null) {
			throw new UserError(this, 922);
		}
		for (Iterator<ParameterValues> iterator = parameterValuesList.iterator(); iterator.hasNext();) {
			ParameterValues parameterValues = iterator.next();
			if (!(parameterValues instanceof ParameterValueRange)) {
				getLogger().warning(
						"Found (and deleted) unsupported parameter value definition. Parameters have to be given as range (e.g. as [2;5.7]).");
				iterator.remove();
			}
		}
		if (parameterValuesList.size() == 0) {
			throw new UserError(this, 922);
		}

		// get parameters to optimize
		this.operators = new Operator[parameterValuesList.size()];
		this.parameters = new String[parameterValuesList.size()];
		double[] min = new double[parameterValuesList.size()];
		double[] max = new double[parameterValuesList.size()];
		this.types = new OptimizationValueType[parameterValuesList.size()];

		int index = 0;
		for (Iterator<ParameterValues> iterator = parameterValuesList.iterator(); iterator.hasNext();) {
			ParameterValueRange parameterValueRange = (ParameterValueRange) iterator.next();
			operators[index] = parameterValueRange.getOperator();
			parameters[index] = parameterValueRange.getParameterType().getKey();
			min[index] = Double.valueOf(parameterValueRange.getMin());
			max[index] = Double.valueOf(parameterValueRange.getMax());

			ParameterType targetType = parameterValueRange.getParameterType();
			if (targetType == null) {
				throw new UserError(this, 906, parameterValueRange.getOperator() + "." + parameterValueRange.getKey());
			}
			if (targetType instanceof ParameterTypeDouble) {
				types[index] = OptimizationValueType.VALUE_TYPE_DOUBLE;
				getLogger().fine("Parameter type of parameter " + targetType.getKey() + ": double");
			} else if (targetType instanceof ParameterTypeInt) {
				types[index] = OptimizationValueType.VALUE_TYPE_INT;
				getLogger().fine("Parameter type of parameter " + targetType.getKey() + ": int");
			} else {
				throw new UserError(this, 909, targetType.getKey());
			}
			index++;
		}

		// create and start optimizer
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		this.optimizer = createOptimizer(random);

		for (int i = 0; i < min.length; i++) {
			this.optimizer.setMin(i, min[i]);
			this.optimizer.setMax(i, max[i]);
			this.optimizer.setValueType(i, types[i]);
			this.optimizer.setExecutingOperator(this);
		}

		optimizer.optimize();

		// create result and return it
		double[] bestParameters = optimizer.getBestValuesEver();
		String[] bestValues = null;
		if (bestParameters != null) {
			bestValues = new String[bestParameters.length];
			for (int i = 0; i < bestParameters.length; i++) {
				if (types[i].equals(OptimizationValueType.VALUE_TYPE_DOUBLE)) {
					bestValues[i] = bestParameters[i] + "";
				} else {
					bestValues[i] = (int) Math.round(bestParameters[i]) + "";
				}
			}
		} else {
			bestValues = new String[operators.length];
			for (int i = 0; i < bestValues.length; i++) {
				bestValues[i] = "unknown";
			}
		}
		ParameterSet bestSet = new ParameterSet(operators, parameters, bestValues, optimizer.getBestPerformanceEver());

		// freeing memory, but saving best value before
		this.bestFitnessEver = optimizer.getBestFitnessEver();
		this.lastGenerationsPerformance = optimizer.getBestFitnessInGeneration();
		this.optimizer = null;
		deliver(bestSet);
	}

	/** This method creates a apropriate optimizer */
	protected ESOptimization createOptimizer(RandomGenerator random) throws UndefinedParameterError {
		return new ESParameterOptimization(this, operators.length, ESOptimization.INIT_TYPE_RANDOM,
				getParameterAsInt(PARAMETER_MAX_GENERATIONS), getParameterAsInt(PARAMETER_GENERATIONS_WITHOUT_IMPROVAL),
				getParameterAsInt(PARAMETER_POPULATION_SIZE), getParameterAsInt(PARAMETER_SELECTION_TYPE),
				getParameterAsDouble(PARAMETER_TOURNAMENT_FRACTION), getParameterAsBoolean(PARAMETER_KEEP_BEST),
				getParameterAsInt(PARAMETER_MUTATION_TYPE), getParameterAsDouble(PARAMETER_CROSSOVER_PROB),
				getParameterAsBoolean(PARAMETER_SHOW_CONVERGENCE_PLOT), random, this);
	}

	public ESOptimization getOptimization() {
		return optimizer;
	}

	public PerformanceVector setParametersAndEvaluate(Individual individual) throws OperatorException {
		double[] currentValues = individual.getValues();
		for (int j = 0; j < currentValues.length; j++) {
			String value;
			if (types[j].equals(OptimizationValueType.VALUE_TYPE_DOUBLE)) {
				value = currentValues[j] + "";
			} else {
				value = (int) Math.round(currentValues[j]) + "";
			}
			operators[j].getParameters().setParameter(parameters[j], value);
			getLogger().fine(operators[j] + "." + parameters[j] + " = " + value);
		}
		return getPerformanceVector();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(ESOptimization.getParameterTypes(this));
		return types;
	}

	public int getNumberOfOptimizationParameters() {
		return this.parameters.length;
	}
}
