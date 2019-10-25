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
package base.operators.operator.validation.clustering;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.clustering.ClusterModel;
import base.operators.tools.Ontology;
import base.operators.tools.math.similarity.DistanceMeasure;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ValueDouble;
import base.operators.operator.performance.EstimatedPerformance;
import base.operators.operator.performance.PerformanceCriterion;
import base.operators.operator.performance.PerformanceVector;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.PassThroughOrGenerateRule;
import base.operators.operator.similarity.SimilarityMeasureObject;


/**
 * This operator is used to evaluate a non-hierarchical cluster model based on the average within
 * cluster similarity/distance. It is computed by averaging all similarities / distances between
 * each pair of examples of a cluster.
 * 
 * @author Michael Wurst, Ingo Mierswa, Sebastian Land
 */
public class ClusterDensityEvaluator extends Operator {

	private double avgClusterSim = 0.0;

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private InputPort distanceInput = getInputPorts().createPort("distance measure", SimilarityMeasureObject.class);
	private InputPort performanceInput = getInputPorts().createPort("performance vector");
	private InputPort clusterModelInput = getInputPorts().createPort("cluster model", ClusterModel.class);

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance vector");

	/**
	 * Constructor for ClusterDensityEvaluator.
	 */
	public ClusterDensityEvaluator(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.ATTRIBUTE_VALUE,
				Attributes.ID_NAME));
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addRule(
				new PassThroughOrGenerateRule(performanceInput, performanceOutput, new MetaData(PerformanceVector.class)));

		addValue(new ValueDouble("clusterdensity", "Avg. within cluster similarity/distance", false) {

			@Override
			public double getDoubleValue() {
				return avgClusterSim;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		SimilarityMeasureObject simMeasure = distanceInput.getData(SimilarityMeasureObject.class);
		DistanceMeasure measure = simMeasure.getDistanceMeasure();
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		ClusterModel clusterModel = clusterModelInput.getData(ClusterModel.class);
		PerformanceVector performance = performanceInput.getDataOrNull(PerformanceVector.class);

		if (performance == null) {
			performance = new PerformanceVector();
		}

		double[] avgWithinClusterSims = withinClusterAvgSim(clusterModel, exampleSet, measure);

		avgClusterSim = avgWithinClusterSims[clusterModel.getNumberOfClusters()];

		PerformanceCriterion withinClusterSim = null;

		if (measure.isDistance()) {
			withinClusterSim = new EstimatedPerformance("Avg. within cluster distance", avgClusterSim, 1, true);
		} else {
			withinClusterSim = new EstimatedPerformance("Avg. within cluster similarity", avgClusterSim, 1, false);
		}

		performance.addCriterion(withinClusterSim);

		for (int i = 0; i < clusterModel.getNumberOfClusters(); i++) {
			PerformanceCriterion withinSingleClusterSim = null;

			if (measure.isDistance()) {
				withinSingleClusterSim = new EstimatedPerformance("Avg. within cluster distance for cluster "
						+ clusterModel.getCluster(i).getClusterId(), avgWithinClusterSims[i], 1, true);
			} else {
				withinSingleClusterSim = new EstimatedPerformance("Avg. within cluster similarity for cluster "
						+ clusterModel.getCluster(i).getClusterId(), avgWithinClusterSims[i], 1, false);
			}

			performance.addCriterion(withinSingleClusterSim);

		}

		exampleSetOutput.deliver(exampleSet);
		performanceOutput.deliver(performance);
	}

	private double[] withinClusterAvgSim(ClusterModel clusterModel, ExampleSet exampleSet, DistanceMeasure measure) {
		Attribute id = exampleSet.getAttributes().getId();

		double[] result = new double[clusterModel.getNumberOfClusters() + 1];

		for (Example example : exampleSet) {
			int clusterIndex = id.isNominal() ? clusterModel.getClusterIndexOfId(example.getValueAsString(id))
					: clusterModel.getClusterIndexOfId(example.getValue(id));
			for (Example compExample : exampleSet) {
				int compClusterIndex = id.isNominal() ? clusterModel.getClusterIndexOfId(compExample.getValueAsString(id))
						: clusterModel.getClusterIndexOfId(compExample.getValue(id));
				if (clusterIndex == compClusterIndex) {
					double v = measure.calculateSimilarity(example, compExample);
					result[clusterIndex] += v;
				}
			}
		}

		double sum = 0.0;
		int totalCount = 0;
		for (int i = 0; i < clusterModel.getNumberOfClusters(); i++) {
			sum += result[i];
			int clusterSize = clusterModel.getCluster(i).getNumberOfExamples();
			result[i] = result[i] /= clusterSize;
			totalCount += clusterSize;
			;
		}
		result[clusterModel.getNumberOfClusters()] = sum / totalCount;
		return result;
	}
}
