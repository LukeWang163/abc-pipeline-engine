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
package base.operators.operator.clustering.clusterer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.OperatorChain;
import base.operators.operator.clustering.ClusterModel;
import base.operators.operator.clustering.ClusterModel2ExampleSet;
import base.operators.operator.clustering.FlattenClusterModel;
import base.operators.tools.Ontology;
import base.operators.tools.OperatorService;
import base.operators.tools.metadata.MetaDataTools;
import base.operators.example.Tools;
import base.operators.example.set.Partition;
import base.operators.example.set.SplittedExampleSet;
import base.operators.operator.OperatorCreationException;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.OperatorProgress;
import base.operators.operator.clustering.HierarchicalClusterLeafNode;
import base.operators.operator.clustering.HierarchicalClusterModel;
import base.operators.operator.clustering.HierarchicalClusterNode;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeMetaData;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPassThroughRule;
import base.operators.operator.ports.metadata.GenerateNewMDRule;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.PassThroughRule;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.SimplePrecondition;
import base.operators.operator.ports.metadata.SubprocessTransformRule;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeBoolean;
import base.operators.parameter.ParameterTypeInt;


/**
 * A top-down generic clustering that can be used with any (flat) clustering as inner operator. Note
 * though, that the outer operator cannot set or get the maximal number of clusters, the inner
 * operator produces. These value has to be set in the inner operator. This operator will create a
 * cluster attribute if not present yet.
 *
 * @author Sebastian Land
 */
public class TopDownClustering extends OperatorChain {

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("cluster model");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("clustered set");

	private OutputPort exampleSetInnerSource = getSubprocess(0).getInnerSources().createPort("example set");
	private InputPort modelInnerSink = getSubprocess(0).getInnerSinks().createPort("cluster model");

	/** The parameter name for &quot;the maximal number of items in a cluster leaf&quot; */
	public static final String PARAMETER_MAX_LEAF_SIZE = "max_leaf_size";

	public static final String PARAMETER_MAX_DEPTH = "max_depth";

	public static final String PARAMETER_CREATE_CLUSTER_LABEL = "create_cluster_label";

	public TopDownClustering(OperatorDescription description) {
		super(description, "Clustering Process");

		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, new ExampleSetMetaData()));
		getTransformer().addRule(new PassThroughRule(exampleSetInput, exampleSetInnerSource, false));

		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		modelInnerSink.addPrecondition(new SimplePrecondition(modelInnerSink, new MetaData(ClusterModel.class)));

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				if (addsClusterAttribute()) {
					metaData.addAttribute(
							new AttributeMetaData(Attributes.CLUSTER_NAME, Ontology.NOMINAL, Attributes.CLUSTER_NAME));
				}
				MetaDataTools.checkAndCreateIds(metaData);
				return metaData;
			}
		});
		getTransformer().addRule(new GenerateNewMDRule(modelOutput, new MetaData(HierarchicalClusterModel.class)));
	}

	private boolean addsClusterAttribute() {
		return getParameterAsBoolean(PARAMETER_CREATE_CLUSTER_LABEL);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		int maxLeafSize = getParameterAsInt(PARAMETER_MAX_LEAF_SIZE);

		// additional checks
		Tools.checkAndCreateIds(exampleSet);
		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this, new String[0]);

		// recursively descend until leaf_size smaller than max_leaf_size
		HierarchicalClusterNode root = new HierarchicalClusterNode("root");
		HierarchicalClusterModel model = new HierarchicalClusterModel(root);
		int createdLeafs = descend(exampleSet, root, 0, maxLeafSize, getParameterAsInt(PARAMETER_MAX_DEPTH) - 1,
				getProgress());

		if (getParameterAsBoolean(PARAMETER_CREATE_CLUSTER_LABEL) && exampleSetOutput.isConnected()) {
			try {
				FlattenClusterModel flattener = OperatorService.createOperator(FlattenClusterModel.class);
				flattener.setParameter(FlattenClusterModel.PARAMETER_NUMBER_OF_CLUSTER, createdLeafs + "");
				ClusterModel flatModel = flattener.flatten(model, exampleSet);
				ClusterModel2ExampleSet applier = OperatorService.createOperator(ClusterModel2ExampleSet.class);
				ExampleSet labelledExampleSet = applier.addClusterAttribute(exampleSet, flatModel);
				exampleSetOutput.deliver(labelledExampleSet);
				modelOutput.deliver(model);
			} catch (OperatorCreationException e) {
				throw new OperatorException("Could not create FlattenClusterModel Operator: " + e, e);
			}
		} else {
			Attribute clusterAttribute = exampleSet.getAttributes().getCluster();
			if (clusterAttribute != null) {
				exampleSet.getAttributes().remove(clusterAttribute);
			}
			exampleSetOutput.deliver(exampleSet);
			modelOutput.deliver(model);
		}
	}

	private int descend(ExampleSet exampleSet, HierarchicalClusterNode elter, int depth, int maxLeafSize, int maxDepth,
			OperatorProgress progress) throws OperatorException {
		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		// applying inner clusterer
		exampleSetInnerSource.deliver(exampleSet);
		getSubprocess(0).execute();
		ClusterModel currentModel = modelInnerSink.getData(ClusterModel.class);
		int[] clusterAssignments = currentModel.getClusterAssignments(exampleSet);

		// creating splitted examples set with cluster results
		Partition partition = new Partition(clusterAssignments, currentModel.getNumberOfClusters());
		SplittedExampleSet splittedSet = new SplittedExampleSet(exampleSet, partition);
		int numberOfCreatedLeafs = 0;

		// initialize operator progress
		if (progress != null) {
			progress.setTotal(currentModel.getNumberOfClusters());
		}

		for (int i = 0; i < currentModel.getNumberOfClusters(); i++) {
			// testing if cluster is large enough to split again
			splittedSet.selectSingleSubset(i);
			if (splittedSet.size() > maxLeafSize && depth < maxDepth) {
				// create new node and descend again on split of examples
				HierarchicalClusterNode node = new HierarchicalClusterNode(depth + ":" + i);
				elter.addSubNode(node);
				numberOfCreatedLeafs += descend(splittedSet, node, depth + 1, maxLeafSize, maxDepth, null);
			} else {
				// create leaf node and add all examples
				Collection<Object> exampleIds = new LinkedList<Object>();
				Attribute id = splittedSet.getAttributes().getId();
				if (id.isNominal()) {
					for (Example example : splittedSet) {
						exampleIds.add(example.getValueAsString(id));
					}
				} else {
					for (Example example : splittedSet) {
						exampleIds.add(example.getValue(id));
					}
				}
				HierarchicalClusterLeafNode leaf = new HierarchicalClusterLeafNode(depth + ":" + i, exampleIds);
				elter.addSubNode(leaf);
				numberOfCreatedLeafs++;
			}
			if (progress != null) {
				progress.step();
			}
		}
		return numberOfCreatedLeafs;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_CREATE_CLUSTER_LABEL,
				"Specifies if a cluster label should be created.", true);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_MAX_DEPTH, "The maximal depth of cluster tree.", 1, Integer.MAX_VALUE, 5);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeInt(PARAMETER_MAX_LEAF_SIZE, "The maximal number of items in each cluster leaf.", 1,
				Integer.MAX_VALUE, 1));

		return types;
	}
}
