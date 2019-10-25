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
package base.operators.operator.clustering;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.tools.Ontology;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.NominalMapping;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.GenerateNewMDRule;
import base.operators.operator.ports.metadata.MDInteger;
import base.operators.operator.ports.metadata.MetaData;
import base.operators.operator.ports.metadata.ModelMetaData;


/**
 * This operator extracts the cluster prototypes from a flat clustermodel and builds an example set
 * containing them.
 *
 * @author Sebastian Land
 *
 */
public class ExtractClusterPrototypes extends Operator {

	private InputPort modelInput = getInputPorts().createPort("model", CentroidClusterModel.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("model");

	public ExtractClusterPrototypes(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(modelInput, modelOutput);
		getTransformer().addRule(new GenerateNewMDRule(exampleSetOutput, ExampleSet.class) {

			@Override
			public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
				if (modelInput.getMetaData() instanceof ModelMetaData) {
					ModelMetaData modelMetaData = (ModelMetaData) modelInput.getMetaData();
					ExampleSetMetaData emd = modelMetaData.getTrainingSetMetaData();
					emd.setNumberOfExamples(new MDInteger());
					return emd;
				}
				return super.modifyMetaData(unmodifiedMetaData);
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		CentroidClusterModel model = modelInput.getData(CentroidClusterModel.class);

		Attributes trainAttributes = model.getTrainingHeader().getAttributes();
		String[] attributeNames = model.getAttributeNames();
		Attribute[] attributes = new Attribute[attributeNames.length + 1];

		for (int i = 0; i < attributeNames.length; i++) {
			Attribute originalAttribute = trainAttributes.get(attributeNames[i]);
			attributes[i] = AttributeFactory.createAttribute(attributeNames[i], originalAttribute.getValueType());
			if (originalAttribute.isNominal()) {
				attributes[i].setMapping((NominalMapping) originalAttribute.getMapping().clone());
			}
		}
		Attribute clusterAttribute = AttributeFactory.createAttribute("cluster", Ontology.NOMINAL);
		attributes[attributes.length - 1] = clusterAttribute;

		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(model.getNumberOfClusters());
		for (int i = 0; i < model.getNumberOfClusters(); i++) {
			double[] data = new double[attributeNames.length + 1];
			System.arraycopy(model.getCentroidCoordinates(i), 0, data, 0, attributeNames.length);
			data[attributeNames.length] = clusterAttribute.getMapping().mapString("cluster_" + i);
			builder.addRow(data);
		}

		ExampleSet resultSet = builder.withRole(clusterAttribute, Attributes.CLUSTER_NAME).build();

		modelOutput.deliver(model);
		exampleSetOutput.deliver(resultSet);
	}

}
