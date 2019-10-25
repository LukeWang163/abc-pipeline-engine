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
package base.operators.operator.preprocessing.transformation;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.SplittedExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.annotation.ResourceConsumptionEstimator;
import base.operators.tools.Ontology;
import base.operators.tools.OperatorResourceConsumptionHandler;
import base.operators.tools.math.AnovaCalculator;
import base.operators.tools.math.SignificanceCalculationException;
import base.operators.tools.math.SignificanceTestResult;
import base.operators.tools.math.function.aggregation.AggregationFunction;
import base.operators.tools.math.function.aggregation.AverageFunction;
import base.operators.tools.math.function.aggregation.VarianceFunction;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.error.AttributeNotFoundError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.GenerateNewMDRule;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeDouble;


/**
 * <p>
 * This operator creates groups of the input example set based on the defined grouping attribute.
 * For each of the groups the mean and variance of another attribute (the anova attribute) is
 * calculated and an ANalysis Of VAriance (ANOVA) is performed. The result will be a significance
 * test result for the specified significance level indicating if the values for the attribute are
 * significantly different between the groups defined by the grouping attribute.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class GroupedANOVAOperator extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", new ExampleSetMetaData());
	private OutputPort significanceOutput = getOutputPorts().createPort("significance");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public static final String PARAMETER_ANOVA_ATTRIBUTE = "anova_attribute";
	public static final String PARAMETER_GROUP_BY_ATTRIBUTE = "group_by_attribute";
	public static final String PARAMETER_SIGNIFICANCE_LEVEL = "significance_level";
	public static final String PARAMETER_ONLY_DISTINCT = "only_distinct";

	public GroupedANOVAOperator(OperatorDescription desc) {
		super(desc);
		getTransformer().addRule(new GenerateNewMDRule(significanceOutput, SignificanceTestResult.class));
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		exampleSetInput.addPrecondition(new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition
				.getAttributesByParameter(this, PARAMETER_ANOVA_ATTRIBUTE), Ontology.NUMERICAL));
		exampleSetInput.addPrecondition(new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition
				.getAttributesByParameter(this, PARAMETER_GROUP_BY_ATTRIBUTE), Ontology.NOMINAL));
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		significanceOutput.deliver(apply(exampleSet));
	}

	public SignificanceTestResult apply(ExampleSet exampleSet) throws OperatorException {
		// init and checks
		String attributeName = getParameterAsString(PARAMETER_ANOVA_ATTRIBUTE);
		String groupByAttributeName = getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTE);
		boolean onlyDistinct = getParameterAsBoolean(PARAMETER_ONLY_DISTINCT);

		Attribute anovaAttribute = exampleSet.getAttributes().get(attributeName);
		if (anovaAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ANOVA_ATTRIBUTE,
					getParameterAsString(PARAMETER_ANOVA_ATTRIBUTE));
		}
		if (anovaAttribute.isNominal()) {
			throw new UserError(this, 104, new Object[] { "anova calculation",
					this.getParameterAsString(PARAMETER_ANOVA_ATTRIBUTE) });
		}

		Attribute groupByAttribute = exampleSet.getAttributes().get(groupByAttributeName);
		if (groupByAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_GROUP_BY_ATTRIBUTE,
					getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTE));
		}
		if (!groupByAttribute.isNominal()) {
			throw new UserError(this, 103, new Object[] { "the parameter grouping by",
					this.getParameterAsString(PARAMETER_GROUP_BY_ATTRIBUTE) });
		}

		// create anova calculator
		AnovaCalculator anovaCalculator = new AnovaCalculator();
		double alpha = getParameterAsDouble(PARAMETER_SIGNIFICANCE_LEVEL);
		anovaCalculator.setAlpha(alpha);

		// add groups
		SplittedExampleSet grouped = SplittedExampleSet.splitByAttribute(exampleSet, groupByAttribute);
		AggregationFunction meanFunction = new AverageFunction();
		AggregationFunction varianceFunction = new VarianceFunction();
		for (int i = 0; i < grouped.getNumberOfSubsets(); i++) {
			grouped.selectSingleSubset(i);
			double[] values = getValues(grouped, anovaAttribute, onlyDistinct);
			double mean = meanFunction.calculate(values);
			double variance = varianceFunction.calculate(values);
			anovaCalculator.addGroup(grouped.size(), mean, variance);
		}

		// calculate and return result
		SignificanceTestResult result = null;
		try {
			result = anovaCalculator.performSignificanceTest();
		} catch (SignificanceCalculationException e) {
			throw new UserError(this, 920, e.getMessage());
		}

		exampleSetOutput.deliver(exampleSet);

		return result;
	}

	private double[] getValues(ExampleSet exampleSet, Attribute attribute, boolean onlyDistinct) {
		Collection<Double> valueCollection = new LinkedList<>();
		if (onlyDistinct) {
			valueCollection = new TreeSet<>();
		}

		for (Example e : exampleSet) {
			valueCollection.add(e.getValue(attribute));
		}

		double[] result = new double[valueCollection.size()];
		int counter = 0;
		for (double d : valueCollection) {
			result[counter++] = d;
		}
		return result;
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == exampleSetOutput) {
			return getParameterAsBoolean("keep_example_set");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ANOVA_ATTRIBUTE,
				"Calculate the ANOVA for this attribute based on the groups defines by " + PARAMETER_GROUP_BY_ATTRIBUTE
						+ ".", exampleSetInput, false));
		types.add(new ParameterTypeAttribute(PARAMETER_GROUP_BY_ATTRIBUTE,
				"Performs a grouping by the values of the attribute with this name.", exampleSetInput, false));
		types.add(new ParameterTypeDouble(PARAMETER_SIGNIFICANCE_LEVEL, "The significance level for the ANOVA calculation.",
				0.0d, 1.0d, 0.05d, false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_ONLY_DISTINCT,
				"Indicates if only rows with distinct values for the aggregation attribute should be used for the calculation of the aggregation function.",
				false));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0),
				GroupedANOVAOperator.class, null);
	}
}
