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

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.example.set.SplittedExampleSet;
import base.operators.operator.OperatorChain;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.tools.Ontology;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.PortPairExtender;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPassThroughRule;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.operator.ports.metadata.SubprocessTransformRule;
import base.operators.parameter.UndefinedParameterError;


/**
 * This operator splits up the input example set according to the clusters and applies its inner
 * operators <var>number_of_clusters</var> time on copies of its own input. This requires the
 * example set to have a special cluster attribute which can be either created by a
 * {@link base.operators.operator.clustering.AbstractClusterer} or might be declared in the
 * attribute description file that was used when the data was loaded.
 *
 * @author Ingo Mierswa
 */
public class ClusterIterator extends OperatorChain {

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort subsetInnerSource = getSubprocess(0).getInnerSources().createPort("cluster subset");
	private PortPairExtender inputExtender = new PortPairExtender("in", getInputPorts(), getSubprocess(0).getInnerSources());
	private PortPairExtender outputExtender = new PortPairExtender("out", getSubprocess(0).getInnerSinks(), getOutputPorts());

	private int numberOfClusters = 0;

	public ClusterIterator(OperatorDescription description) {
		super(description, "Cluster Iteration");
		inputExtender.start();
		outputExtender.start();
		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.VALUE_TYPE,
				new String[] { Attributes.CLUSTER_NAME }));

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, subsetInnerSource, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				metaData.getNumberOfExamples().reduceByUnknownAmount();
				return super.modifyExampleSet(metaData);
			}
		});
		getTransformer().addRule(inputExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(outputExtender.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		Attribute clusterAttribute = exampleSet.getAttributes().getCluster();
		if (clusterAttribute == null) {
			throw new UserError(this, 113, Attributes.CLUSTER_NAME);
		}

		SplittedExampleSet splitted = SplittedExampleSet.splitByAttribute(exampleSet, clusterAttribute);
		numberOfClusters = splitted.getNumberOfSubsets();

		// init Operator progress
		getProgress().setTotal(numberOfClusters);

		for (int i = 0; i < numberOfClusters; i++) {
			splitted.selectSingleSubset(i);

			clearAllInnerSinks();
			subsetInnerSource.deliver(splitted);
			inputExtender.passCloneThrough();
			super.doWork();
			inApplyLoop();
			getProgress().step();
		}

		outputExtender.passDataThrough();
		getProgress().complete();
	}
}
