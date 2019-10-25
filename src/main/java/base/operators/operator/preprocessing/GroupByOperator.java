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
package base.operators.operator.preprocessing;

import java.util.List;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.operator.Operator;
import base.operators.tools.Ontology;
import base.operators.example.set.SplittedExampleSet;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.UserError;
import base.operators.operator.error.AttributeNotFoundError;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.metadata.AttributeSetPrecondition;
import base.operators.parameter.ParameterType;
import base.operators.parameter.ParameterTypeAttribute;


/**
 * <p>
 * This operator creates a SplittedExampleSet from an arbitrary example set. The partitions of the
 * resulting example set are created according to the values of the specified attribute. This works
 * similar to the <code>GROUP BY</code> clause in SQL.
 * </p>
 *
 * <p>
 * Please note that the resulting example set is simply a splitted example set where no subset is
 * selected. Following operators might decide to select one or several of the subsets, e.g. one of
 * the aggregation operators.
 * </p>
 *
 * @author Christian Bockermann, Ingo Mierswa
 */
public class GroupByOperator extends Operator {

	public final static String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public GroupByOperator(OperatorDescription desc) {
		super(desc);

		exampleSetInput.addPrecondition(new AttributeSetPrecondition(exampleSetInput, AttributeSetPrecondition
				.getAttributesByParameter(this, PARAMETER_ATTRIBUTE_NAME), Ontology.NOMINAL));
		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Attribute attribute = exampleSet.getAttributes().get(this.getParameterAsString(PARAMETER_ATTRIBUTE_NAME));

		if (attribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_ATTRIBUTE_NAME, getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		}

		if (!attribute.isNominal()) {
			throw new UserError(this, 103, new Object[] { this.getParameterAsString(PARAMETER_ATTRIBUTE_NAME),
			"grouping by attribute." });
		}

		SplittedExampleSet grouped = SplittedExampleSet.splitByAttribute(exampleSet, attribute);

		exampleSetOutput.deliver(grouped);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME,
				"Name of the nominal attribute which is used to create partitions.", exampleSetInput, false,
				Ontology.NOMINAL));
		return types;
	}
}
