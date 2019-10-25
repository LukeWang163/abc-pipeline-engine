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
package base.operators.operator.validation.clustering.exampledistribution;

import java.util.List;

import base.operators.operator.Operator;
import base.operators.operator.clustering.ClusterModel;
import base.operators.operator.performance.PerformanceVector;
import base.operators.tools.ClassNameMapper;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ValueDouble;
import base.operators.operator.performance.EstimatedPerformance;
import base.operators.operator.performance.PerformanceCriterion;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.PassThroughOrGenerateRule;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeStringCategory;


/**
 * Evaluates flat cluster models on how well the examples are distributed over the clusters.
 * 
 * @author Michael Wurst, Sebastian Land
 * 
 */
public class ExampleDistributionEvaluator extends Operator {

	public static final String PARAMETER_MEASURE = "measure";

	private static final String[] DEFAULT_MEASURES = {
			SumOfSquares.class.getName(),
			GiniCoefficient.class.getName()};

	private static final ClassNameMapper MEASURE_MAP = new ClassNameMapper(DEFAULT_MEASURES);

	private double distribution = 0;

	private InputPort clusterModelInput = getInputPorts().createPort("cluster model", ClusterModel.class);
	private InputPort performanceInput = getInputPorts().createPort("performance vector");

	private OutputPort clusterModelOutput = getOutputPorts().createPort("cluster model");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance vector");

	public ExampleDistributionEvaluator(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(clusterModelInput, clusterModelOutput);
		getTransformer().addRule(
				new PassThroughOrGenerateRule(performanceInput, performanceOutput, new MetaData(PerformanceVector.class)));

		addValue(new ValueDouble("item_distribution", "The distribution of items over clusters.", false) {

			@Override
			public double getDoubleValue() {
				return distribution;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {

		ClusterModel model = clusterModelInput.getData(ClusterModel.class);

		ExampleDistributionMeasure measure = (ExampleDistributionMeasure) MEASURE_MAP
				.getInstantiation(getParameterAsString(PARAMETER_MEASURE));

		int totalNumberOfItems = 0;
		int[] count = new int[model.getNumberOfClusters()];
		for (int i = 0; i < model.getNumberOfClusters(); i++) {

			int numItemsInCluster = model.getCluster(i).getNumberOfExamples();
			totalNumberOfItems = totalNumberOfItems + numItemsInCluster;
			count[i] = numItemsInCluster;
		}

		PerformanceVector performance = performanceInput.getDataOrNull(PerformanceVector.class);
		if (performance == null) {
			// If no performance vector is available create a new one
			performance = new PerformanceVector();
		}

		distribution = measure.evaluate(count, totalNumberOfItems);

		PerformanceCriterion criterion = new EstimatedPerformance("Example distribution", distribution, 1, false);
		performance.addCriterion(criterion);

		clusterModelOutput.deliver(model);
		performanceOutput.deliver(performance);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		String[] shortClassNames = MEASURE_MAP.getShortClassNames();
		String defaultValue = shortClassNames.length > 0 ? shortClassNames[0] : SumOfSquares.class.getSimpleName();
		ParameterType type = new ParameterTypeStringCategory(PARAMETER_MEASURE, "the item distribution measure to apply",
				shortClassNames, defaultValue, true);
		type.setExpert(false);
		types.add(type);

		return types;
	}

}
