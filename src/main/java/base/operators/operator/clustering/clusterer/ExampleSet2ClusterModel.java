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

import java.util.HashMap;
import java.util.List;

import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.operator.clustering.ClusterModel;
import base.operators.tools.metadata.MetaDataTools;
import base.operators.example.Tools;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.error.AttributeNotFoundError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ExampleSetPassThroughRule;
import base.operators.operator.ports.metadata.ExampleSetPrecondition;
import base.operators.operator.ports.metadata.SetRelation;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;
import base.operators.parameter.ParameterTypeBoolean;


/**
 * This operator creates a flat cluster model using a nominal attribute and dividing the exampleset
 * by this attribute over the clusters. Every value is mapped onto a cluster, including the unkown
 * value. This operator will create a cluster attribute if not present yet.
 *
 * @author Sebastian Land
 */
public class ExampleSet2ClusterModel extends Operator {

	public static final String PARAMETER_ATTRIBUTE = "attribute";
	public static final String PARAMETER_REMOVE_UNLABELED = "remove_unlabeled";
	public static final String PARAMETER_ADD_AS_LABEL = "add_as_label";

	private InputPort exampleSetInput = getInputPorts().createPort("example set");

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("cluster model");

	public ExampleSet2ClusterModel(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput));
		exampleSetInput.addPrecondition(new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition
				.getAttributesByParameter(this, PARAMETER_ATTRIBUTE)));

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				MetaDataTools.checkAndCreateIds(metaData);
				return metaData;
			}
		});
		getTransformer().addGenerationRule(modelOutput, ClusterModel.class);

	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		Attribute attribute = exampleSet.getAttributes().get(getParameterAsString(PARAMETER_ATTRIBUTE));
		if (attribute != null) {
			if (attribute.isNominal()) {
				// search all possible values
				HashMap<Double, Integer> valueMap = new HashMap<>();
				int[] clusterAssignments = new int[exampleSet.size()];
				int i = 0;
				for (Example example : exampleSet) {
					double value = example.getValue(attribute);
					if (valueMap.containsKey(value)) {
						clusterAssignments[i] = valueMap.get(value).intValue();
					} else {
						clusterAssignments[i] = valueMap.size();
						valueMap.put(value, valueMap.size());
					}
					i++;
				}
				ClusterModel model = new ClusterModel(exampleSet, valueMap.size(),
						getParameterAsBoolean(RMAbstractClusterer.PARAMETER_ADD_AS_LABEL),
						getParameterAsBoolean(RMAbstractClusterer.PARAMETER_REMOVE_UNLABELED));
				// assign examples to clusters
				model.setClusterAssignments(clusterAssignments, exampleSet);

				modelOutput.deliver(model);
				exampleSetOutput.deliver(exampleSet);
			} else {
				throw new UserError(this, 119, getParameterAsString(PARAMETER_ATTRIBUTE), "ExampleSet2ClusterModel");
			}
		} else {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE, getParameterAsString(PARAMETER_ATTRIBUTE));
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE,
				"Specifies the nominal attribute used to create the cluster", exampleSetInput, false));

		ParameterType type = new ParameterTypeBoolean(PARAMETER_ADD_AS_LABEL,
				"Should the cluster values be added as label.", false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_REMOVE_UNLABELED, "Delete the unlabeled examples.", false);
		type.setExpert(false);
		types.add(type);

		return types;
	}

}
